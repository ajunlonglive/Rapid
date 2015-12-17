/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as 
published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version. The terms require you 
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.rapid.core.Application;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Comparators;
import com.rapid.utils.JAXB.EncryptedXmlAdapter;

/*

RapidAdmin and RapidDesign roles are required in the rapid application security provider to use the
Admin and Design screens

In addition, to administrate or design particular applications RapidAdmin and RapidDesign roles 
are also required in that application's security provider

Finally, to administrate or design the rapid application itself the roles RAPIDADMIN and RAPIDDESIGN 
are required in the rapid application security provider

*/

public abstract class SecurityAdapter {

	// this class holds a roles details
	public static class Role {
		
		// private class variables
		protected String _name, _description;
		
		// properties
		public String getName() { return _name; }
		public void setName(String name) { _name = name; }
		
		public String getDescription() { return _description; }
		public void setDescription(String description) { _description = description; }
		
		// constructors
		public Role() {}
		
		public Role(String name, String description) {
			_name = name;
			_description = description;
		}
				
	}
		
	// this class is an overridden list of roles with some useful methods
	public static class Roles extends ArrayList<Role> {
		
		public List<String> getNames() {
			// a list of strings
			List<String> names = new ArrayList<String>();
			// loop entries and add names
			for (Role role : this) {
				// get the role name
				String roleName = role.getName();
				// add it to the collection we're returning if not already there
				if (!names.contains(roleName)) names.add(roleName);
			}
			// return
			return names;			
		}
		
		// remove a role by name
		public boolean remove(String roleName) {
			boolean removed = false;
			Role role = null;
			for (Role r : this) {
				if (r.getName().equals(roleName)) {
					role = r;
					break;
				}
				
			};	
			if (role != null) {
				this.remove(role);
				removed = true;
			}
			return removed;
		}
		
		// sort the roles case - not case sensitive
		public Roles sort() {
			// sort the roles alphabetically by the name
			Collections.sort(this, new Comparator<Role>() {
				@Override
				public int compare(Role r1, Role r2) {
					return Comparators.AsciiCompare(r1.getName(), r2.getName(), false);
				}				
			});			
			return this;			
		}
		
		// return true if the list of roles contains one by name
		public boolean contains(String roleName) {
			// loop the roles returning true as soon as there's a match
			for (Role role : this) if (role.getName().equals(roleName)) return true;
			// not found return false
			return false;
		}
						
	}
		
	// this class is an overridden list of role names with some useful methods
	public static class UserRoles extends ArrayList<String> {
		
		// sort the roles alphabetically
		public UserRoles sort() {			
			Collections.sort(this);			
			return this;			
		}
						
	}
	
	
	// this class holds a users details
	public static class User {
		
		// private class variables
		protected String _name, _description, _password, _deviceDetails;
		protected UserRoles _userRoles;
		
		// properties
		public String getName() { return _name; }
		public void setName(String name) { _name = name; }
		
		public String getDescription() { return _description; }
		public void setDescription(String description) { _description = description; }
		
		@XmlJavaTypeAdapter( EncryptedXmlAdapter.class )
		public String getPassword() { return _password; }
		public void setPassword(String password) { _password = password; }
		
		public String getDeviceDetails() { return _deviceDetails; }
		public void setDeviceDetails(String deviceDetails) { _deviceDetails = deviceDetails; }
		
		public UserRoles getRoles() { return _userRoles; }
		public void setRoles(UserRoles roles) { _userRoles = roles; }
		
		// constructors
		public User() {
			_userRoles = new UserRoles();
		}
				
		public User(String name, String description, String password, String deviceDetails, UserRoles roles) {
			_name = name;
			_description = description;
			_password = password;
			_deviceDetails = deviceDetails;
			_userRoles = roles;
		}
		
		public User(String name, String description, String password, UserRoles roles) {
			this(name, description, password, null, roles);
		}
		
		public User(String name, String description, String password, String deviceDetails) {
			this(name, description, password, deviceDetails, new UserRoles());
		}
		
		public User(String name, String description, String password) { 
			this(name, description, password, null, new UserRoles());
		}
								
	}
	
		
	// this class is an overridden list of users with some useful methods
	public static class Users extends ArrayList<User> {
		
		public List<String> getNames() {
			// a list of strings
			List<String> names = new ArrayList<String>();
			// loop entries and add names
			for (User user : this) names.add(user.getName());
			// return
			return names;			
		}
		
		public Users sort() {
			// sort the users alphabetically by the name
			Collections.sort(this, new Comparator<User>() {
				@Override
				public int compare(User u1, User u2) {
					return Comparators.AsciiCompare(u1.getName(), u2.getName(), false);
				}				
			});			
			return this;			
		}
		
		// return true if the list of users contains one by name
		public boolean contains(String userName) {
			// loop the roles returning true as soon as there's a match
			for (User user : this) if (user.getName().equals(userName)) return true;
			// not found return false
			return false;
		}
						
	}
	
	// this exception class can be extended for more meaningful exceptions that may occur within the adapters
	public static class SecurityAdapaterException extends Exception {
		
		private String _message;
		private Throwable _cause;
		
		public SecurityAdapaterException(String message) {
			_message = message;
		}
		
		public SecurityAdapaterException(String message, Throwable cause) {
			_message = message;
			_cause = cause;
		}

		@Override
		public String getMessage() {
			return _message;
		}

		@Override
		public Throwable getCause() {
			if (_cause == null) return super.getCause();
			return _cause;			
		}
		
		@Override
		public StackTraceElement[] getStackTrace() {
			if (_cause == null) return super.getStackTrace();
			return _cause.getStackTrace();			
		}
		
	}
	
	// instance variables
		
	protected ServletContext _servletContext;
	protected Application _application;
	
	// properties
	
	public ServletContext getServletContext() { return _servletContext; }
	public Application getApplication() { return _application; }
		
	// constructor
	
	public SecurityAdapter(ServletContext servletContext, Application application) {
		_servletContext = servletContext;
		_application = application;
	}
	
	// abstract methods
			
	// all roles available to the application
	public abstract Roles getRoles(RapidRequest rapidRequest) throws SecurityAdapaterException;
	
	// all users of the application
	public abstract Users getUsers(RapidRequest rapidRequest) throws SecurityAdapaterException;
	
	
	// details of a single role
	public abstract Role getRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException;
		
	// details of a single user
	public abstract User getUser(RapidRequest rapidRequest) throws SecurityAdapaterException;
	
		
		
	// add a role to the application (the adapter will need to ensure that the role is not present already)
	public abstract void addRole(RapidRequest rapidRequest, Role role) throws SecurityAdapaterException;
						
	// delete a role from the application
	public abstract void deleteRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException;
	
	
	// add a user to the application (the adapter will need to ensure that the user is not present already)
	public abstract void addUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException;
		
	// delete a user from the application
	public abstract void deleteUser(RapidRequest rapidRequest) throws SecurityAdapaterException;
	
	
	// add a named role to a named user (the adapter will need to ensure that the role is not present already)
	public abstract void addUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException;
	
	// remove a named role from a named user
	public abstract void deleteUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException;
	
	// check a named userName/roleName combination
	public abstract boolean checkUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException;
	
	// check a named userName for any of a list of roles (faster then looping them all)
	public abstract boolean checkUserRole(RapidRequest rapidRequest, List<String> roleNames) throws SecurityAdapaterException;
	
	// update a role description
	public abstract void updateRole(RapidRequest rapidRequest, Role role) throws SecurityAdapaterException;
		
	// update a user's details
	public abstract void updateUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException;
	
	// check a named userName/password combination
	public abstract boolean checkUserPassword(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException;
		
}
