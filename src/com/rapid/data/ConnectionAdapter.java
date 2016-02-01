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

package com.rapid.data;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;

import com.rapid.server.RapidRequest;

/*

The purpose of the connection adapter is to be able to allow overrides for how database connections
are aquired, and managed. For example some connection adapters may provide pooling, others might
lookup passwords when making connections  

*/

public abstract class ConnectionAdapter {
	
	// this exception class can be extended for more meaningful exceptions that may occur within the adapters
	public static class ConnectionAdapterException extends Exception {
		
		private String _message;
		private Throwable _cause;
		
		public ConnectionAdapterException(String message) {
			_message = message;
		}
		
		public ConnectionAdapterException(String message, Throwable cause) {
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
	protected String _driverClassName, _connectionString, _userName, _password;
	
	// super class properties
	
	public String getDriverClass() { return _driverClassName; }
	public void setDriverClass(String driverClassName) { _driverClassName = driverClassName; }
	
	public String getConnectionString() { return _connectionString; }
	public void setConnectionString(String connectionString) { _connectionString = connectionString; }
	
	public String getUserName() { return _userName; }
	public void setUserName(String userName) { _userName = userName; }
	
	public String getPassword() { return _password; }
	public void setPassword(String password) { _password = password; }
	
	// constructor
	
	public ConnectionAdapter(ServletContext servletContext, String driverClassName, String connectionString, String userName, String password) {
		_servletContext = servletContext;
		_driverClassName = driverClassName;
		_connectionString = connectionString; 
		_userName = userName;
		_password = password;
	}
	
	// abstract methods
	
	// this method fetches connections for the adapter
	public abstract Connection getConnection(RapidRequest rapidRequest) throws ConnectionAdapterException, SQLException, ClassNotFoundException;
	
	// this method closes connection for the adapter
	public abstract void closeConnection(Connection connection) throws SQLException;
	
	// close any objects we used
	public abstract void close() throws SQLException;
				
}
