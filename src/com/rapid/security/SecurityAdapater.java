package com.rapid.security;

import java.util.List;

import javax.servlet.ServletContext;

import com.rapid.core.Application;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidHttpServlet.RapidRequest;

/*

RapidAdmin and RapidDesign roles are required in the rapid application security provider to use the
Admin and Design screens

In addition, to administrate or design particular applications RapidAdmin and RapidDesign roles 
are also required in that application's security provider

Finally, to administrate or design the rapid application itself the roles RAPIDADMIN and RAPIDDESIGN 
are required in the rapid application security provider

*/

public abstract class SecurityAdapater {

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
	
	public SecurityAdapater(ServletContext servletContext, Application application) {
		_servletContext = servletContext;
		_application = application;
	}
	
	// abstract methods
	
	public abstract void addRole(RapidRequest rapidRequest, String role) throws SecurityAdapaterException;
	
	public abstract void addUser(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException;
			
	public abstract void addUserRole(RapidRequest rapidRequest, String userName, String role) throws SecurityAdapaterException;
	
	public abstract void deleteRole(RapidRequest rapidRequest, String role) throws SecurityAdapaterException;
	
	public abstract void deleteUser(RapidRequest rapidRequest, String userName) throws SecurityAdapaterException;
	
	public abstract void deleteUserRole(RapidRequest rapidRequest, String userName, String role) throws SecurityAdapaterException;
	
	public abstract void updateUserPassword(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException;
	
	public abstract boolean checkUserPassword(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException;
		
	public abstract boolean checkUserRole(RapidRequest rapidRequest, String userName, String role) throws SecurityAdapaterException;
	
	public abstract List<String> getUserRoles(RapidRequest rapidRequest, String userName) throws SecurityAdapaterException;
	
	public abstract List<String> getRoles(RapidRequest rapidRequest) throws SecurityAdapaterException;
	
	public abstract List<String> getUsers(RapidRequest rapidRequest) throws SecurityAdapaterException;	
		
}
