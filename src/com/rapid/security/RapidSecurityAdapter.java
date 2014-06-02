package com.rapid.security;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.rapid.core.Application;
import com.rapid.server.RapidHttpServlet.RapidRequest;
import com.rapid.utils.Files;

public class RapidSecurityAdapter extends SecurityAdapater {
		
	public static class User {
		
		private String _password;
		private ArrayList<String> _roles;
		
		public String getPassword() { return _password; }
		public void setPassword(String password) { _password = password; }
		
		public ArrayList<String> getRoles() { return _roles; }
		public void setRoles(ArrayList<String> roles) { _roles = roles; }
		
		public User(){};
		
		public User(String password) {
			_password = password;
			_roles = new ArrayList<String>();
		}
		
		public User(String password, ArrayList<String> roles) {
			_password = password;
			_roles = roles;
		}
		
		public void addRole(String role) {
			if (_roles == null) _roles = new ArrayList<String>();
			if (!_roles.contains(role)) _roles.add(role);
		}
		
		public void deleteRole(String role) {
			_roles.remove(role);
		}
		
	}
	
	@XmlRootElement
	public static class Security {
		
		// instance variables
		
		private List<String> _roles;
		private HashMap<String,User> _users;
		
		// properties
		
		public List<String> getRoles() { return _roles; }		
		public void setRoles(List<String> roles) { _roles = roles; } 
		
		public HashMap<String,User> getUsers() { return _users; }		
		public void setUsers(HashMap<String,User> users) { _users = users; } 
		
		// constructor
		
		public Security() {
			_roles = new ArrayList<String>();
			_users = new HashMap<String,User>();
		}
		
		// instance methods
		
		public void addRole(String role) {
			_roles.add(role);
		}
				
		public void removeRole(String role) {
			_roles.remove(role);
		}
		
		public void addUser(String userName, User user) {
			_users.put(userName, user);
		}
		
		public User getUser(String userName) {
			return _users.get(userName);
		}
		
		public void removeUser(String userName) {
			_users.remove(userName);
		}
		
	}
	
	// instance variables
	private static Logger _logger = Logger.getLogger(RapidSecurityAdapter.class);	
	private Security _security;
	private Marshaller _marshaller;
	private Unmarshaller _unmarshaller;
			
	// constructors
	
	public RapidSecurityAdapter(ServletContext servletContext, Application application) {
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
		// load the users
		load();
	}
	
	// instance methods
	
	private void load() {
		
		try {
			
			// create a file object for the security.xml file
			File securityFile = new File(_servletContext.getRealPath("/WEB-INF/applications/" + _application.getId() + "/security.xml"));
			
			// check the file exists
			if (securityFile.exists()) {
				
				// marshal the users object to the temp file
				_security = (Security) _unmarshaller.unmarshal(securityFile);
					
			} else {
				
				_security = new Security();
				
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
	public void addRole(RapidRequest rapidRequest, String role) throws SecurityAdapaterException {
		_security.addRole(role);
		save();
	}

	@Override
	public void deleteRole(RapidRequest rapidRequest, String role) throws SecurityAdapaterException {
		_security.removeRole(role);
		save();
	}
	
	@Override
	public void addUser(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException {
		_security.addUser(userName.toLowerCase(), new User(password));
		save();
	}
		
	@Override
	public void deleteUser(RapidRequest rapidRequest, String userName) throws SecurityAdapaterException {
		if (_security.getUser(userName.toLowerCase()) == null) throw new SecurityAdapaterException("No such user");
		_security.removeUser(userName.toLowerCase());
		save();
	}
	
	@Override
	public void addUserRole(RapidRequest rapidRequest, String userName, String role) throws SecurityAdapaterException {
		User user = _security.getUser(userName.toLowerCase());
		if (user == null) throw new SecurityAdapaterException("No such user");
		user.addRole(role);
		save();
	}
	
	@Override
	public void deleteUserRole(RapidRequest rapidRequest, String userName, String role) throws SecurityAdapaterException {
		User user = _security.getUser(userName.toLowerCase());
		if (user == null) throw new SecurityAdapaterException("No such user");
		user.deleteRole(role);
		save();
	}
	
	@Override
	public void updateUserPassword(RapidRequest rapidRequest, String userName, String password)	throws SecurityAdapaterException {
		User user = _security.getUser(userName.toLowerCase());
		if (user == null) throw new SecurityAdapaterException("No such user");
		user.setPassword(password);
		save();
	}

	@Override
	public boolean checkUserPassword(RapidRequest rapidRequest, String userName, String password)	throws SecurityAdapaterException {
		User user = _security.getUser(userName.toLowerCase());
		if (user != null) {
			if (password.equals(user.getPassword())) return true;
		}
		return false;		
	}
	
	@Override
	public boolean checkUserRole(RapidRequest rapidRequest, String userName,String role) throws SecurityAdapaterException {
		User user = _security.getUser(userName.toLowerCase());
		if (user != null) {			
			List<String> roles = user.getRoles();
			if (roles != null) {
				for (String userRole : user.getRoles()) {
					if (userRole.equals(role)) return true;
				}
			}							
		}
		return false;
	}
	
	@Override
	public List<String> getUserRoles(RapidRequest rapidRequest, String userName) throws SecurityAdapaterException {
		User user = _security.getUser(userName.toLowerCase());
		ArrayList<String> roles = new ArrayList<String>();
		if (user != null) {		
			ArrayList<String> userRoles =  user.getRoles();
			if (userRoles != null) {
				for (String role : userRoles) {
					roles.add(role);
				}
			}
			Collections.sort(roles);
		}
		return roles;
	}

	@Override
	public List<String> getRoles(RapidRequest rapidRequest) throws SecurityAdapaterException {
		return _security.getRoles();
	}
	
	@Override
	public List<String> getUsers(RapidRequest rapidRequest) throws SecurityAdapaterException {
		ArrayList<String> users = new ArrayList<String>();
		for (String key : _security.getUsers().keySet()) {
			users.add(key);
		}
		Collections.sort(users);
		return users;
	}	
	
}
