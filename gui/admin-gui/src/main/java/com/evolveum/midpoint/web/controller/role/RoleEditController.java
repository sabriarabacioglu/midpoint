/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.controller.role;

import java.io.Serializable;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("roleEdit")
@Scope("session")
public class RoleEditController implements Serializable {

	public static final String PAGE_NAVIGATION = "/role/roleEdit?faces-redirect=true";
	private static final long serialVersionUID = 6390559677870495118L;
	private static final Trace LOGGER = TraceManager.getTrace(RoleEditController.class);
	@Autowired(required = true)
	private transient ObjectTypeCatalog catalog;
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient RoleListController roleList;
	private boolean newRole = true;
	private RoleDto role;

	public RoleDto getRole() {
		return role;
	}

	/**
	 * True if this controller is used for creating new role, false if we're
	 * editing existing role
	 */
	public boolean isNewRole() {
		return newRole;
	}

	void setNewRole(boolean newRole) {
		this.newRole = newRole;
	}

	void setRole(RoleDto role) {
		Validate.notNull(role, "Role must not be null.");
		this.role = role;
		newRole = false;
		template.setSelectedLeftId("leftRoleEdit");
	}

	public String initController() {
		role = new RoleDto(new RoleType());
		newRole = true;
		template.setSelectedLeftId("leftRoleCreate");

		return PAGE_NAVIGATION;
	}

	public String save() {
		if (role == null) {
			FacesUtils.addErrorMessage("Role must not be null.");
			return null;
		}

		String nextPage = null;
		try {
			RoleManager manager = ControllerUtil.getRoleManager(catalog);
			manager.submit(getRole());

			roleList.initController();
			nextPage = RoleListController.PAGE_NAVIGATION_LIST;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't submit role {}", ex, role.getName());
			FacesUtils.addErrorMessage("Couldn't submit role '" + role.getName() + "'.", ex);
		}

		return nextPage;
	}
}
