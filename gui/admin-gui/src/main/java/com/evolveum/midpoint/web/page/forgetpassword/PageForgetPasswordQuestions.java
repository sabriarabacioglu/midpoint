package com.evolveum.midpoint.web.page.forgetpassword;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;
import com.evolveum.midpoint.web.component.menu.top.TopMenuBar;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

@PageDescriptor(url = "/forgetpasswordquestions")
public class PageForgetPasswordQuestions extends PageBase {
	
	private UserType userTypeObject;
	
	private static final Trace LOGGER = TraceManager.getTrace(PageForgetPassword.class);
	  private static final String DOT_CLASS = PageForgetPasswordQuestions.class.getName() + ".";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
	private static final String ID_PWDRESETQUESTIONFORM = "pwdResetQuestionForm";
	private static final String ID_LABELQUESTION1 = "labelQuestion1";
	private static final String ID_LABELQUESTION2 = "labelQuestion2";
	private static final String ID_LABELQUESTION3 = "labelQuestion3";
	
	
	 private static final String TASK_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";
	 
	

	public PageForgetPasswordQuestions() {

	//	  model = new LoadableModel<MyPasswordsDto>(false) {

	  //          @Override
	    //        protected MyPasswordsDto load() {
	      //          return loadPageModel();
	      //      }
	     //   };

	  //      initLayout();
	  //  }

		TopMenuBar menuBar = getTopMenuBar();
		menuBar.addOrReplace(new LocalePanel(TopMenuBar.ID_RIGHT_PANEL));
		 
		
		 Task task = createSimpleTask(OPERATION_LOAD_USER);

         StringValue userOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
         Collection options1 = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
                 GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
         OperationResult result = new OperationResult(OPERATION_LOAD_USER);
         try {
			PrismObject<UserType> user = getModelService().getObject(UserType.class, userOid.toString(), options1, task, result);
		} catch (ObjectNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SchemaException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityViolationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CommunicationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    
         
         List<SecurityQuestionAnswerType> userAnswer= userTypeObject.getCredentials().getSecurityQuestions().getQuestionAnswer();
	        List<String> questionIdentifiers = new ArrayList<String>();
	        for (Iterator iterator = userAnswer.iterator(); iterator.hasNext();) {
				SecurityQuestionAnswerType securityQuestionAnswerType = (SecurityQuestionAnswerType) iterator
						.next();
			 	questionIdentifiers.add(securityQuestionAnswerType.getQuestionIdentifier());
				
				
			}
	      
	        
	        Collection<SelectorOptions<GetOperationOptions>> options =
	                SelectorOptions.createCollection(GetOperationOptions.createResolve(),
	                        SystemConfigurationType.F_DEFAULT_USER_TEMPLATE ,SystemConfigurationType.F_GLOBAL_SECURITY_POLICY_REF);
	        Task systemTask = createSimpleTask(TASK_GET_SYSTEM_CONFIG);
	        OperationResult result2 = new OperationResult(TASK_GET_SYSTEM_CONFIG);
	        
	        try {
				PrismObject<SystemConfigurationType> systemConfig = getModelService().getObject(SystemConfigurationType.class,
				        SystemObjectsType.SYSTEM_CONFIGURATION.value(), options, systemTask, result2);
				
				System.out.println(systemConfig.asObjectable().getGlobalSecurityPolicyRef());
				PrismObject<SecurityPolicyType> secPolicy = getModelService().getObject(SecurityPolicyType.class, systemConfig.asObjectable().getGlobalSecurityPolicyRef().getOid(), options, task, result);
				SecurityQuestionsCredentialsPolicyType secQuCre= secPolicy.asObjectable().getCredentials().getSecurityQuestions();
				List<SecurityQuestionDefinitionType> questionList=secQuCre.getQuestion() ;
				 
				for  (int i = 0; i < questionIdentifiers.size(); i++)  {
					  String item   = questionIdentifiers.get(i);
					 	
						
						
					}
				  for (int i = 0; i < questionList.size(); i++) {
					
				 
					  SecurityQuestionDefinitionType item   = questionList.get(i);
					  
					  item.getIdentifier();
					  
				  }	  
				  
				  
				
			} catch (ObjectNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SchemaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityViolationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CommunicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        
	        
	        
	     
	        
	        
	        
	      
		
		
		 Form form = new Form(ID_PWDRESETQUESTIONFORM) {

	            @Override
	            protected void onSubmit() {
	            	
	            	
	            
	                 
	               
	              LOGGER.info("Reset Password user info form submitted.");
	          
	                //Check if the email and the uid exists and matches in the idm
	                
	           
	             
	              
	            }
	        };
		
		

	}

	public UserType getUserTypeObject() {
		return userTypeObject;
	}

	public void setUserTypeObject(UserType userTypeObject) {
		this.userTypeObject = userTypeObject;
	}

}
