/**
 * Returns custom landing path provided in Role's custom attribute for a Role current user belongs
 * to
 */

package com.liferay.opensourceforlife.landingpage;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liferay.opensourceforlife.util.CustomLandingPageUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.UniqueList;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.model.UserGroupGroupRole;
import com.liferay.portal.model.UserGroupRole;
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserGroupGroupRoleLocalServiceUtil;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

/**
 * @author Tejas Kanani
 */
public class RolePageType implements LandingPageType {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.liferay.opensourceforlife.landingpage.LandingPageType#getLandingPagePath
	 * (javax.servlet .http.HttpServletRequest)
	 */
	public String getLandingPagePath(final HttpServletRequest request)
			throws PortalException, SystemException {
		String rolePath = StringPool.BLANK;
		
		User currentUser = PortalUtil.getUser(request);

		List<Role>  userRoles = getUserRoles(currentUser,
				CustomLandingPageUtil.getIncludeSystemRole());
		if (userRoles==null &&  (LOG.isDebugEnabled())){
			LOG.debug("No Roles assigned to user :  "
					+ currentUser.getFullName());
		}
		for (Role role: userRoles){							
				String path = CustomLandingPageUtil.getLandingPageFriendlyURL(role,
						PortalUtil.getCompanyId(request));
				if (LOG.isDebugEnabled())
					LOG.debug("role: "+role.getName()+" -  rolePath: "+path);
				if (Validator.isNotNull(path)){
					rolePath = path;
					break;
				}				
		}
		
		return rolePath;
	}

	/**
	 * @param user
	 * @param includeSystemRole
	 * @return
	 */
	private List<Role>  getUserRoles(final User user, final boolean includeSystemRole) {		
		List<Role> userRoles = new UniqueList<Role>();
		try {
			List<Role> roles = new UniqueList<Role>();
			roles.addAll(user.getRoles());
			roles.addAll(getRolesByUserGroup(user));
			roles.addAll(getUserGroupRolesOfUser(user));
			roles.addAll(getUserExplicitRoles(user));

			if (Validator.isNotNull(roles) && !roles.isEmpty()) {				
				for (Role role : roles) {					
					if (includeSystemRole || !includeSystemRole && !PortalUtil.isSystemRole(role.getName())) {
						userRoles.add(role);							
					}
				}
				
			}
		} catch (PortalException e) {
			LOG.error(e.getMessage(), e);

		} catch (SystemException se) {
			LOG.error(se.getMessage(), se);
		}

		return userRoles;
	}
	
	private static List<Role> getRolesByUserGroup(User user) throws SystemException, PortalException{
		List<Role> roles = new UniqueList<Role>();
		List<UserGroup> userGroups =user.getUserGroups();
		
		for(UserGroup group: userGroups){			
			List<Role> userGroupRoles = RoleLocalServiceUtil.getGroupRoles(group.getGroupId());		
			if (userGroupRoles!=null){
				roles.addAll(userGroupRoles);
			}					
		}		

		return roles; 
	}

	private static List<Role> getUserExplicitRoles(User user)
			throws SystemException, PortalException {
		List<Role> roles = new UniqueList<Role>();
		
		List<UserGroupRole> userGroupRoles = UserGroupRoleLocalServiceUtil
				.getUserGroupRoles(user.getUserId());
		for (UserGroupRole userGroupRole : userGroupRoles) {
			roles.add(userGroupRole.getRole());
		}
		return roles;
	}

	private static List<Role> getUserGroupRolesOfUser(User user)
			throws SystemException, PortalException {
		List<Role> roles = new UniqueList<Role>();
		List<UserGroup> userGroupList = UserGroupLocalServiceUtil
				.getUserUserGroups(user.getUserId());
		List<UserGroupGroupRole> userGroupGroupRoles = new ArrayList<UserGroupGroupRole>();
		for (UserGroup userGroup : userGroupList) {
			userGroupGroupRoles.addAll(UserGroupGroupRoleLocalServiceUtil
					.getUserGroupGroupRoles(userGroup.getUserGroupId()));
		}
		for (UserGroupGroupRole userGroupGroupRole : userGroupGroupRoles) {
			Role role = RoleLocalServiceUtil.getRole(userGroupGroupRole
					.getRoleId());
			roles.add(role);
		}
		return roles;
	}

	private static final Log LOG = LogFactory.getLog(RolePageType.class);
}
