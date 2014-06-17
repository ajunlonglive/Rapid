package com.rapid.security;

import java.io.File;
import java.io.FileOutputStream;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.server.Rapid;
import com.rapid.server.RapidHttpServlet.RapidRequest;
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
			
	// constructors

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
			File securityFile = new File(_servletContext.getRealPath("/WEB-INF/applications/" + _application.getId() + "/security.xml"));
			
			// check the file exists
			if (securityFile.exists()) {
				
				// unmarshal the xml into an object
				_security = (Security) _unmarshaller.unmarshal(securityFile);
									
			} else {
				
				// initialise the security object
				_security = new Security();
				
				// add the default application roles
				_security.getRoles().add(new Role(Rapid.ADMIN_ROLE, "Rapid administrator"));
				_security.getRoles().add(new Role(Rapid.DESIGN_ROLE, "Rapid designer"));
				
				// create a list of roles we want the user to have
				UserRoles userRoles = new UserRoles();
				userRoles.add(Rapid.ADMIN_ROLE);
				userRoles.add(Rapid.DESIGN_ROLE);
				
				// initialise the admin user with all application roles
				User adminUser = new User("admin", "Admin user", "admin", userRoles);
				
				// add the admin user
				_security.getUsers().add(adminUser);
				
				// initialise the simple users with no application roles
				User simpleUser = new User("user", "Unprivileged user", "user");
				
				// add the simple user
				_security.getUsers().add(simpleUser);
								
				save();
				
			}
							    
		} catch (Exception ex) {
			_logger.error(ex);
		}
		
	}
	
	private void save() {
		
		try {
		
			// get the folder in which the file is saved
			File folderDir = new File(_servletContext.getRealPath("/WEB-INF/applications/" + _application.getId()));		
			// create the folder if it doesn't exist
			if (!folderDir.exists()) folderDir.mkdirs();
						
			// create a file object for the security.xml file
			File securityFile = new File(folderDir.getAbsolutePath() + "/security.xml");
	
			// create a temp file for saving the application to
			File tempFile = new File(folderDir.getAbsolutePath() + "/security-saving.xml");
			
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
		for (Role role : _security.getRoles()) if (role.getName().equals(roleName)) return role;
		return null;
	}
	
	@Override
	public User getUser(RapidRequest rapidRequest, String userName) throws SecurityAdapaterException {
		for (User user : _security.getUsers()) if (user.getName().equals(userName)) return user;
		return null;
	}
	
	@Override
	public void addRole(RapidRequest rapidRequest, Role role) throws SecurityAdapaterException {
		_security.getRoles().add(role);
		_security.getRoles().sort();
		save();
	}
	
	@Override
	public void deleteRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		for (Role role : _security.getRoles()) if (role.getName().equals(roleName)) {
			_security.getRoles().remove(role);
			_security.getRoles().sort();
			save();
			break;
		};	
	}
	
	@Override
	public void addUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException {
		_security.getUsers().add(user);
		_security.getUsers().sort();
	}
	
	@Override
	public void deleteUser(RapidRequest rapidRequest, String userName) throws SecurityAdapaterException {
		for (User user : _security.getUsers()) if (user.getName().equals(userName)){
			_security.getUsers().remove(user);
			_security.getUsers().sort();
			save();
			break;
		}			
	}
	
	@Override
	public void addUserRole(RapidRequest rapidRequest, String userName,	String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest, userName);
		if (user == null) throw new SecurityAdapaterException("User " + userName + " cannot be found");
		user.getRoles().add(roleName);
		user.getRoles().sort();
		save();		
	}
	
	@Override
	public void deleteUserRole(RapidRequest rapidRequest, String userName, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest, userName);
		if (user == null) throw new SecurityAdapaterException("User " + userName + " cannot be found");
		user.getRoles().remove(roleName);
		user.getRoles().sort();
		save();
	}
	
	
	@Override
	public boolean checkUserPassword(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException {
		User user = getUser(rapidRequest, userName);
		if (user == null) throw new SecurityAdapaterException("User " + userName + " cannot be found");
		if (password.equals(user.getPassword())) return true;
		return false;
	}
	
	@Override
	public boolean checkUserRole(RapidRequest rapidRequest, String userName, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest, userName);
		if (user == null) throw new SecurityAdapaterException("User " + userName + " cannot be found");
		return user.getRoles().contains(roleName);
	}
	
	@Override
	public void updateUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException {
		deleteUser(rapidRequest, user.getName());
		_security.getUsers().add(user);
		_security.getUsers().sort();
	}
						
}
