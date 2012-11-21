/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.lens;

import static com.evolveum.midpoint.model.ModelCompiletimeConfig.CONSISTENCY_CHECKS;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

/**
 * @author semancik
 */
@Component
public class ChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    @Autowired(required = true)
    private transient TaskManager taskManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired(required = true)
    private ProvisioningService provisioning;
    
    @Autowired(required = true)
    private PrismContext prismContext;
    
    private PrismObjectDefinition<UserType> userDefinition = null;
    private PrismObjectDefinition<AccountShadowType> accountDefinition = null;
    
    @PostConstruct
    private void locateUserDefinition() {
    	userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    	accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
    }

    public <F extends ObjectType, P extends ObjectType> void executeChanges(LensContext<F,P> syncContext, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {
    	
    	OperationResult result = parentResult.createSubresult(ChangeExecutor.class+".executeChanges");
    	
    	try {
	    	LensFocusContext<F> focusContext = syncContext.getFocusContext();
	    	if (focusContext != null) {
		        ObjectDelta<F> userDelta = focusContext.getWaveDelta(syncContext.getExecutionWave());
		        if (userDelta != null) {
		
		            executeChange(userDelta, focusContext, syncContext, task, result);
		
		            // userDelta is composite, mixed from primary and secondary. The OID set into
		            // it will be lost ... unless we explicitly save it
		            focusContext.setOid(userDelta.getOid());
		        } else {
		            LOGGER.trace("Skipping change execute, because user delta is null");
		        }
	    	}
	
	        for (LensProjectionContext<P> accCtx : syncContext.getProjectionContexts()) {
	        	if (accCtx.getWave() != syncContext.getExecutionWave()) {
	        		continue;
	        	}
	            ObjectDelta<P> accDelta = accCtx.getExecutableDelta();
	            
	            if (accCtx.getSynchronizationPolicyDecision() != SynchronizationPolicyDecision.BROKEN) {
		            if (accDelta == null || accDelta.isEmpty()) {
		                if (LOGGER.isTraceEnabled()) {
		                	LOGGER.trace("No change for account " + accCtx.getResourceShadowDiscriminator());
		                	LOGGER.trace("Delta:\n{}", accDelta == null ? null : accDelta.dump());
		                }
		                if (focusContext != null) {
		                	updateAccountLinks(focusContext.getObjectNew(), focusContext, accCtx, task, result);
		                }
		                continue;
		            }
		            
		            executeChange(accDelta, accCtx, syncContext, task, result);
		            
		            // To make sure that the OID is set (e.g. after ADD operation)
		            accCtx.setOid(accDelta.getOid());
	            }
	            
	            if (focusContext != null) {
	            	updateAccountLinks(focusContext.getObjectNew(), focusContext, accCtx, task, result);
	            }
	        }
	        
	        result.computeStatus();
	        
    	} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			result.computeStatus();
			if (!result.isSuccess()) {
				result.recordFatalError(e);
			}
			throw e;
		} catch (CommunicationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ConfigurationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (SecurityViolationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			throw e;
    	}  
        
    }

    /**
     * Make sure that the account is linked (or unlinked) as needed.
     */
    private <F extends ObjectType, P extends ObjectType> void updateAccountLinks(PrismObject<F> prismObject,
    		LensFocusContext<F> focusContext, LensProjectionContext<P> accCtx,
    		Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, RewindException {
    	if (prismObject == null) {
    		return;
    	}
        F objectTypeNew = prismObject.asObjectable();
        if (!(objectTypeNew instanceof UserType)) {
        	return;
        }
        UserType userTypeNew = (UserType) objectTypeNew;
        String accountOid = accCtx.getOid();
        if (accountOid == null) {
            throw new IllegalStateException("Account has null OID, this should not happen");
        }

        if (accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK 
        		|| accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE
        		|| accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
            // Link should NOT exist
        	
        	PrismReference accountRef = userTypeNew.asPrismObject().findReference(UserType.F_ACCOUNT_REF);
        	if (accountRef != null) {
        		for (PrismReferenceValue accountRefVal: accountRef.getValues()) {
        			if (accountRefVal.getOid().equals(accountOid)) {
                        // Linked, need to unlink
                        unlinkAccount(userTypeNew.getOid(), accountRefVal, (LensFocusContext<UserType>) focusContext, task, result);
                    }
        		}
        		
        	}
            
            //update account situation only if the account was not deleted
//			if (accCtx.getDelta() != null ) {
				LOGGER.trace("Account {} unlinked from the user, updating also situation in account.", accountOid);
				updateSituationInAccount(task, null, accountOid, result);
				LOGGER.trace("Situation in the account was updated to {}.", "null");
//			}
            // Not linked, that's OK

        } else {
            // Link should exist
        	
            for (ObjectReferenceType accountRef : userTypeNew.getAccountRef()) {
                if (accountOid.equals(accountRef.getOid())) {
                    // Already linked, nothing to do, only be sure, the situation is set with the good value
                	LOGGER.trace("Updating situation in already linked account.");
                	updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountOid, result);
                	LOGGER.trace("Situation in account was updated to {}.", SynchronizationSituationType.LINKED);
                	return;
                }
            }
            // Not linked, need to link
            linkAccount(userTypeNew.getOid(), accountOid, (LensFocusContext<UserType>) focusContext, task, result);
            //be sure, that the situation is set correctly
            LOGGER.trace("Updating situation after account was linked.");
            updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountOid, result);
            LOGGER.trace("Situation in account was updated to {}.", SynchronizationSituationType.LINKED);
        }
    }

    private void linkAccount(String userOid, String accountOid, LensElementContext<UserType> userContext, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException {

        LOGGER.trace("Linking account " + accountOid + " to user " + userOid);
        PrismReferenceValue accountRef = new PrismReferenceValue();
        accountRef.setOid(accountOid);
        accountRef.setTargetType(AccountShadowType.COMPLEX_TYPE);

        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationAddCollection(
        		UserType.F_ACCOUNT_REF, getUserDefinition(), accountRef); 

        try {
            cacheRepositoryService.modifyObject(UserType.class, userOid, accountRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
//        updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountRef, result);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, accountRefDeltas, UserType.class, prismContext);
		userContext.addToExecutedDeltas(userDelta);
    }

	private PrismObjectDefinition<UserType> getUserDefinition() {
		return userDefinition;
	}

	private void unlinkAccount(String userOid, PrismReferenceValue accountRef, LensElementContext<UserType> userContext, Task task, OperationResult result) throws
            ObjectNotFoundException, SchemaException {

        LOGGER.trace("Deleting accountRef " + accountRef + " from user " + userOid);

        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationDeleteCollection(
        		UserType.F_ACCOUNT_REF, getUserDefinition(), accountRef.clone()); 
        
        try {
            cacheRepositoryService.modifyObject(UserType.class, userOid, accountRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
        
      //setting new situation to account
//        updateSituationInAccount(task, null, accountRef, result);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, accountRefDeltas, UserType.class, prismContext);
		userContext.addToExecutedDeltas(userDelta);

    }
	
    private void updateSituationInAccount(Task task, SynchronizationSituationType situation, String accountRef, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, RewindException{

    	OperationResult result = new OperationResult("Updating situation in account (Model)");
    	result.addParam("situation", situation);
    	result.addParam("accountRef", accountRef);
		List<PropertyDelta> syncSituationDeltas = new ArrayList<PropertyDelta>();

		PropertyDelta syncSituationDelta = PropertyDelta.createReplaceDelta(accountDefinition,
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION, situation);
		syncSituationDeltas.add(syncSituationDelta);

		// new situation description
		SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
		syncSituationDescription.setSituation(situation);
		syncSituationDescription.setChannel(task.getHandlerUri());
		syncSituationDescription.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
		
		syncSituationDelta = PropertyDelta.createDelta(new ItemPath(
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), accountDefinition);
		syncSituationDelta.addValueToAdd(new PrismPropertyValue(syncSituationDescription));
		syncSituationDeltas.add(syncSituationDelta);
		
		try {
			modifyProvisioningObject(AccountShadowType.class, accountRef, syncSituationDeltas, result);
		} catch (ObjectNotFoundException ex) {
			// if the object not found exception is thrown, it's ok..probably
			// the account was deleted by previous execution of changes..just
			// log in the trace the message for the user.. 
			LOGGER.trace("Situation in account could not be updated. Account not found on the resource. Skipping modifying situation in account");
			return;
		}
		// if everything is OK, add result of the situation modification to the
		// parent result
		result.recordSuccess();
		parentResult.addSubresult(result);
		
	}
    
	private <T extends ObjectType, F extends ObjectType, P extends ObjectType>
    	void executeChange(ObjectDelta<T> objectDelta, LensElementContext<T> objectContext, LensContext<F,P> context, Task task, OperationResult result) 
    			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException,
    			ConfigurationException, SecurityViolationException, RewindException {
    	
        if (objectDelta == null) {
            throw new IllegalArgumentException("Null change");
        }
        
        // Other types than user type may not be definition-complete (e.g. accounts and resources are completed in provisioning)
        if (UserType.class.isAssignableFrom(objectDelta.getObjectTypeClass())) {
        	objectDelta.assertDefinitions();
        }
        
    	if (LOGGER.isTraceEnabled()) {
    		logDeltaExecution(objectDelta, context);
    	}

        if (objectDelta.getChangeType() == ChangeType.ADD) {
            executeAddition(objectDelta, result);

        } else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
            executeModification(objectDelta, result);

        } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
            executeDeletion(objectDelta, result);
        }
        
        objectContext.addToExecutedDeltas(objectDelta.clone());
        
    }
	
	private <T extends ObjectType, F extends ObjectType, P extends ObjectType>
				void logDeltaExecution(ObjectDelta<T> objectDelta, LensContext<F,P> context) {
		StringBuilder sb = new StringBuilder();
		sb.append("---[ EXECUTE delta of ").append(objectDelta.getObjectTypeClass().getSimpleName());
		sb.append(" ]---------------------\n");
		DebugUtil.debugDumpLabel(sb, "Channel", 0);
		sb.append(" ").append(context.getChannel()).append("\n");
		DebugUtil.debugDumpLabel(sb, "Wave", 0);
		sb.append(" ").append(context.getExecutionWave()).append("\n");
		sb.append(objectDelta.dump());
		sb.append("\n--------------------------------------------------");
		
		LOGGER.trace("\n{}", sb);
	}

    private <T extends ObjectType> void executeAddition(ObjectDelta<T> change, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {

        PrismObject<T> objectToAdd = change.getObjectToAdd();

        if (change.getModifications() != null) {
            for (ItemDelta delta : change.getModifications()) {
                delta.applyTo(objectToAdd);
            }
            change.getModifications().clear();
        }

        T objectTypeToAdd = objectToAdd.asObjectable();

        String oid = null;
        if (objectTypeToAdd instanceof TaskType) {
            oid = addTask((TaskType) objectTypeToAdd, result);
        } else if (ObjectTypes.isManagedByProvisioning(objectTypeToAdd)) {
            oid = addProvisioningObject(objectToAdd, result);
            if (oid == null) {
            	throw new SystemException("Provisioning addObject returned null OID while adding " + objectToAdd);
            }
            result.addReturn("createdAccountOid", oid);
        } else {
            oid = cacheRepositoryService.addObject(objectToAdd, result);
            if (oid == null) {
            	throw new SystemException("Repository addObject returned null OID while adding " + objectToAdd);
            }
        }
        change.setOid(oid);
    }

    private <T extends ObjectType> void executeDeletion(ObjectDelta<T> change, OperationResult result) throws
            ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {

        String oid = change.getOid();
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.deleteTask(oid, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            deleteProvisioningObject(objectTypeClass, oid, result);
        } else {
            cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
        }
    }

    private <T extends ObjectType> void executeModification(ObjectDelta<T> change, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, RewindException {
        if (change.isEmpty()) {
            // Nothing to do
            return;
        }
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.modifyTask(change.getOid(), change.getModifications(), result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            String oid = modifyProvisioningObject(objectTypeClass, change.getOid(), change.getModifications(), result);
            if (!oid.equals(change.getOid())){
            	change.setOid(oid);
            }
        } else {
            cacheRepositoryService.modifyObject(objectTypeClass, change.getOid(), change.getModifications(), result);
        }
    }

    private String addTask(TaskType task, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException {
        try {
            return taskManager.addTask(task.asPrismObject(), result);
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't add object {} to task manager", ex, task.getName());
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private String addProvisioningObject(PrismObject<? extends ObjectType> object, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, RewindException {

        if (object.canRepresent(ResourceObjectShadowType.class)) {
            ResourceObjectShadowType shadow = (ResourceObjectShadowType) object.asObjectable();
            String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadow);
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is null in shadow");
            }
        }

        try {
            ProvisioningScriptsType scripts = getScripts(object.asObjectable(), result);
            String oid = provisioning.addObject(object, scripts, result);
            return oid;
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (ObjectAlreadyExistsException ex) {
            throw new RewindException(ex);
        } catch (CommunicationException ex) {
            throw ex;
        } catch (ConfigurationException e) {
			throw e;
        } catch (RuntimeException ex) {
            throw new SystemException(ex.getMessage(), ex);
		}
    }

    private void deleteProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
            OperationResult result) throws ObjectNotFoundException, ObjectAlreadyExistsException,
            SchemaException {

        try {
            // TODO: scripts
            provisioning.deleteObject(objectTypeClass, oid, null, null, result);
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private String modifyProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
            Collection<? extends ItemDelta> modifications, OperationResult result) throws ObjectNotFoundException, RewindException {

        try {
            // TODO: scripts
            String changedOid = provisioning.modifyObject(objectTypeClass, oid, modifications, null, result);
            return changedOid;
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private ProvisioningScriptsType getScripts(ObjectType object, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
    	ProvisioningScriptsType scripts = null;
        if (object instanceof ResourceType) {
            ResourceType resource = (ResourceType) object;
            scripts = resource.getScripts();
        } else if (object instanceof ResourceObjectShadowType) {
            ResourceObjectShadowType resourceObject = (ResourceObjectShadowType) object;
            if (resourceObject.getResource() != null) {
                scripts = resourceObject.getResource().getScripts();
            } else {
                String resourceOid = ResourceObjectShadowUtil.getResourceOid(resourceObject);
                ResourceType resObject = provisioning.getObject(ResourceType.class, resourceOid, null, result).asObjectable();
                scripts = resObject.getScripts();
            }
        }

        if (scripts == null) {
            scripts = new ProvisioningScriptsType();
        }

        return scripts;
    }

}