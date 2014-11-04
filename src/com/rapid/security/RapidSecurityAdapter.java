/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.security;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.server.Rapid;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Files;

public class RapidSecurityAdapter extends SecurityAdapater {
	
	// security object which is marshalled and unmarshalled
	
	@XmlRootElement
	public static class Security {
		
		private Roles _roles;
		private Users _users;
		
		// properties (these are mashalled in and out of the security.xml file)
		
		public Roles getRoles() { return _roles; }
		public void setRoles(Roles roles) { _roles = roles; }
		
		public Users getUsers() { return _users; }
		public void setUsers(Users users) { _users = users; }
		
		// constructors
		
		public Security() {
			_roles = new Roles();
			_users = new Users();
		}
		
	}
						
	// instance variables
	
	private static Logger _logger = Logger.getLogger(RapidSecurityAdapter.class);	
	private Marshaller _marshaller;
	private Unmarshaller _unmarshaller;
	private Security _security;
			
	// constructor

	public RapidSecurityAdapter(ServletContext servletContext, Application application) {
		// call the super method which retains the context and application
		super(servletContext, application);
		// init the marshaller and ununmarshaller
		try { 
			JAXBContext jaxb = JAXBContext.newInstance(Security.class);
			_marshaller = jaxb.createMarshaller(); 
			_marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			_unmarshaller = jaxb.createUnmarshaller();
		} catch (JAXBException ex) {
			_logger.error(ex);
		}
		// load the users and roles
		load();
	}
	
	// instance methods
	
	private void load() {
		
		try {
			
			// create a file object for the security.xml file
			File securityFile = new File(_application.getConfigFolder(_servletContext) + "/security.xml");
			
			// check the file exists
			if (securityFile.exists()) {
				
				// unmarshal the xml into an object
				_security = (Security) _unmarshaller.unmarshal(securityFile);
									
			} else {
				
				// initialise the security object
				_security = new Security();
				
			}
			
			// initialise roles if necessary
			if (_security.getRoles() == null) _security.setRoles(new Roles());
			
			// initialise users if necessary
			if (_security.getUsers() == null) _security.setUsers(new Users());
			
			// retain whether we added users or roles to the security object
			boolean modified = false;
			
			// check there are any roles, add defaults if not
			if (_security.getRoles().size() == 0) {
				
				// add the default application roles
				_security.getRoles().add(new Role(Rapid.ADMIN_ROLE, "Manage application in Rapid Admin"));
				_security.getRoles().add(new Role(Rapid.DESIGN_ROLE, "Design application in Rapid Designer"));
				
				// record that we modified
				modified = true;
				
			}
				
			if (_security.getUsers().size() == 0) {
				
				// create a list of roles we want the user to have
				UserRoles userRoles = new UserRoles();
				userRoles.add(Rapid.ADMIN_ROLE);
				userRoles.add(Rapid.DESIGN_ROLE);
				
				// initialise the current user with all application roles
				User adminUser = new User("admin", "Admin user", "admin", userRoles);
				
				// add the admin user
				_security.getUsers().add(adminUser);
				
				// initialise the simple users with no application roles
				User simpleUser = new User("user", "Unprivileged user", "user");
				
				// add the simple user
				_security.getUsers().add(simpleUser);
				
				// record that we modified
				modified = true;
				
			}
			
			// save the files if we modified it							
			if (modified) save();
														    
		} catch (Exception ex) {
			_logger.error(ex);
		}
		
	}
	
	private void save() {
		
		try {
								
			// create a file object for the security.xml file
			File securityFile = new File(_application.getConfigFolder(_servletContext) + "/security.xml");
	
			// create a temp file for saving the application to
			File tempFile = new File(_application.getConfigFolder(_servletContext) + "/security-saving.xml");
			
			// get a file output stream to write the data to
			FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());		
			
			// marshal the security object to the temp file
			_marshaller.marshal(_security, fos);
				    
			// close the stream
		    fos.close();
		    
		    // copy / overwrite the app file with the temp file	    	    	    
		    Files.copyFile(tempFile, securityFile);
		    
		    // delete the temp file
		    tempFile.delete();
	    
		} catch (Exception ex) {
			_logger.error(ex);
		}
	    
	}
	
	
	// overrides
	
	@Override
	public Roles getRoles(RapidRequest rapidRequest) throws SecurityAdapaterException {
		return _security.getRoles();
	}
	
	@Override
	public Users getUsers(RapidRequest rapidRequest) throws SecurityAdapaterException {
		return _security.getUsers();
	}
	
	@Override
	public Role getRole(RapidRequest rapidRequest, String roleName)	throws SecurityAdapaterException {
		for (Role role : _security.getRoles()) if (role.getName().toLowerCase().equals(roleName.toLowerCase())) return role;
		return null;
	}
	
	@Override
	public User getUser(RapidRequest rapidRequest) throws SecurityAdapaterException {
		for (User user : _security.getUsers()) if (user.getName().toLowerCase().equals(rapidRequest.getUserName().toLowerCase())) return user;
		return null;
	}
			
	@Override
	public void addRole(RapidRequest rapidRequest, Role role) throws SecurityAdapaterException {
		Roles roles = _security.getRoles();
		if (!roles.contains(role)) roles.add(role);
		roles.sort();
		save();
	}
	
	@Override
	public void deleteRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		Roles roles = _security.getRoles();
		roles.remove(roleName);		
		save();
	}
	
	@Override
	public void addUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException {
		_security.getUsers().add(user);
		_security.getUsers().sort();
		save();
	}
	
	@Override
	public void deleteUser(RapidRequest rapidRequest) throws SecurityAdapaterException {
		for (User user : _security.getUsers()) if (user.getName().equals(rapidRequest.getUserName())){
			_security.getUsers().remove(user);
			_security.getUsers().sort();
			save();
			break;
		}			
	}
	
	@Override
	public void addUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user == null) throw new SecurityAdapaterException("User " + rapidRequest.getUserName() + " cannot be found");
		user.getRoles().add(roleName);
		user.getRoles().sort();
		save();		
	}
	
	@Override
	public void deleteUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user == null) throw new SecurityAdapaterException("User " + rapidRequest.getUserName() + " cannot be found");
		user.getRoles().remove(roleName);
		user.getRoles().sort();
		save();
	}
				
	@Override
	public boolean checkUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user != null) {
			return user.getRoles().contains(roleName);
		}
		return false;
	}
	
	@Override
	public boolean checkUserRole(RapidRequest rapidRequest, List<String> roleNames) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user != null) {
			for (String roleName : roleNames) {
				if (user.getRoles().contains(roleName)) return true;
			}
		}
		return false;
	}
	
	@Override
	public void updateRole(RapidRequest rapidRequest, Role role) throws SecurityAdapaterException {
		Role currentRole = getRole(rapidRequest, role.getName());
		if (currentRole == null) throw new SecurityAdapaterException("Role " + role.getName() + " cannot be found");
		currentRole.setDescription(role.getDescription());
		save();
	}
	
	@Override
	public void updateUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException {
		deleteUser(rapidRequest);
		_security.getUsers().add(user);
		_security.getUsers().sort();
		save();
	}
	
	@Override
	public boolean checkUserPassword(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException {
		User user = null;
		for (User u : _security.getUsers()) {
			if (u.getName().toLowerCase().equals(userName.toLowerCase())) {
				user = u;
				break;
			}
		}
		if (user != null) {
			if (password.equals(user.getPassword())) return true;
		}
		return false;
	}
						
}
