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

package com.rapid.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.Parameter;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Applications;
import com.rapid.core.Control;
import com.rapid.core.Device;
import com.rapid.core.Device.Devices;
import com.rapid.core.Page;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.core.Applications.Versions;
import com.rapid.core.Pages.PageHeader;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.Role;
import com.rapid.security.SecurityAdapter.Roles;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.security.SecurityAdapter.Users;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.server.RapidServletContextListener;
import com.rapid.soa.JavaWebservice;
import com.rapid.soa.SOAElementRestriction;
import com.rapid.soa.SOASchema;
import com.rapid.soa.SQLWebservice;
import com.rapid.soa.Webservice;
import com.rapid.soa.SOAElementRestriction.*;
import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.utils.Files;

public class Rapid extends Action {
	
	// instance variables
	
	private ArrayList<Action> _successActions, _errorActions, _childActions;
	
	// properties
	
	public ArrayList<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(ArrayList<Action> successActions) { _successActions = successActions; }
	
	public ArrayList<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(ArrayList<Action> errorActions) { _errorActions = errorActions; }
	
	// constructors
	
	//jaxb
	public Rapid() { super(); }	
	// designer
	public Rapid(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		
		// call the super constructor to the set the xml version
		super();
		
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for success, error, and child actions
			if (!"successActions".equals(key) && !"errorActions".equals(key) && !"childActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		} 
		
		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some
		if (jsonSuccessActions != null) {
			_successActions = Control.getActions(rapidServlet, jsonSuccessActions);
		}
		
		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some
		if (jsonErrorActions != null) {
			_errorActions = Control.getActions(rapidServlet, jsonErrorActions);
		}
						
	}	
	
	// internal methods
	
	private Application createApplication(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String name, String version, String title, String description) throws IllegalArgumentException, SecurityException, JAXBException, IOException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, SecurityAdapaterException, ParserConfigurationException, XPathExpressionException, RapidLoadingException, SAXException {
		
		String newAppId = Files.safeName(name).toLowerCase();
		String newAppVersion = Files.safeName(version);
		
		Application newApp = new Application();
		
		// populate the bare-minimum of properties
		newApp.setId(newAppId);
		newApp.setVersion(newAppVersion);
		newApp.setName(name);
		newApp.setTitle(title);
		newApp.setDescription(description);
		newApp.setCreatedBy(rapidRequest.getUserName());
		newApp.setCreatedDate(new Date());
		newApp.setSecurityAdapterType("rapid");
						 								
		// initialise the application
		newApp.initialise(rapidServlet.getServletContext(), true);
		
		// initialise the list of action
		List<String> actionTypes = new ArrayList<String>();
		
		// get the JSONArray of controls
		JSONArray jsonActionTypes = rapidServlet.getJsonActions();
		
		// if there were some
		if (jsonActionTypes != null) {
			// loop them 
			for (int i = 0; i < jsonActionTypes.length(); i++) {
				// get the action
				JSONObject jsonActionType = jsonActionTypes.getJSONObject(i);
				// add to list if addToNewApplications is set
				if (jsonActionType.optBoolean("addToNewApplications")) actionTypes.add(jsonActionType.getString("type"));
			}
		}
							
		// assign the list to the application
		newApp.setActionTypes(actionTypes);
		
		// initialise the list of controls
		List<String> controlTypes = new ArrayList<String>();
		
		// get the JSONArray of controls
		JSONArray jsonControlTypes = rapidServlet.getJsonControls();
		
		// if there were some
		if (jsonControlTypes != null) {
			// loop them 
			for (int i = 0; i < jsonControlTypes.length(); i++) {
				// get the control
				JSONObject jsonControlType = jsonControlTypes.getJSONObject(i);
				// add to list if addToNewApplications is set
				if (jsonControlType.optBoolean("addToNewApplications")) controlTypes.add(jsonControlType.getString("type"));
			}
		}
							
		// assign the list to the application
		newApp.setControlTypes(controlTypes);
		
		// save the application to file
		newApp.save(rapidServlet, rapidRequest, false);
		
		// get the security 
		SecurityAdapter security = newApp.getSecurity();
		
		// check there is one
		if (security != null) {
			
			// get the current user's name
			String userName = rapidRequest.getUserName();
			
			// get the current user's record from the adapter
			User user = security.getUser(rapidRequest);
			
			// get the rapid application
			Application rapidApplication = rapidServlet.getApplications().get("rapid");
			
			// check the current user is present in the app's security adapter
			if (user == null) {
				// get the Rapid user object
				User rapidUser = rapidApplication.getSecurity().getUser(rapidRequest);
				// create a new user based on the current user
				user = new User(userName, rapidUser.getDescription(), rapidUser.getPassword());
				// add the new user 
				security.addUser(rapidRequest, user);
			}
			
			// add Admin and Design roles for the new user if required
			if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE)) security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
			if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE)) security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
		}
						
		return newApp;
		
	}
	
	
	
	private List<SOAElementRestriction> getRestrictions(JSONArray jsonRestrictions) throws JSONException {
		// check we have something
		if (jsonRestrictions == null) {
			return null;
		} else {
			// instantiate the list we're making
			List<SOAElementRestriction> restrictions = new ArrayList<SOAElementRestriction>();
			// loop what we got
			for (int i = 0; i < jsonRestrictions.length(); i++) {
				// fetch this item
				JSONObject jsonRestriction = jsonRestrictions.getJSONObject(i);
				// get the type
				String type = jsonRestriction.getString("type").trim();
				// get the value
				String value = jsonRestriction.optString("value");
				
				// check the type and construct appropriate restriction				
				if ("MinOccursRestriction".equals(type)) restrictions.add(new MinOccursRestriction(Integer.parseInt(value)));
				if ("MaxOccursRestriction".equals(type)) restrictions.add(new MaxOccursRestriction(Integer.parseInt(value)));
				if ("MinLengthRestriction".equals(type)) restrictions.add(new MinLengthRestriction(Integer.parseInt(value)));
				if ("MaxLengthRestriction".equals(type)) restrictions.add(new MaxLengthRestriction(Integer.parseInt(value)));
				if ("EnumerationRestriction".equals(type)) restrictions.add(new EnumerationRestriction(value));
				
			}
			return restrictions;
		}
	}
		
	// overrides
	
	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<Action>();
			// add child success actions
			if (_successActions != null) {
				for (Action action : _successActions) _childActions.add(action);			
			}
			// add child error actions
			if (_errorActions != null) {
				for (Action action : _errorActions) _childActions.add(action);			
			}
		}
		return _childActions;		
	}
	
	@Override
	public String getJavaScript(RapidHttpServlet rapidServlet, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		
		// the javascript we're about to build
		String js = "";
		
		// write success actions variable
		js += "  var successCallback = function(data) {\n";
		// check success actions
		if (_successActions != null) {
			if (_successActions.size() > 0) {
				for (Action action : _successActions) {
					js += "    " + action.getJavaScript(rapidServlet, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}				
			}
		}
		js += "  };\n";
		
		// write error actions variable
		js += "  var errorCallback = function(server, status, error) {\n";
		// check whether is an error handling routin
		boolean gotErrorHandler = false;
		// check error actions
		if (_errorActions != null) {
			if (_errorActions.size() > 0) {
				gotErrorHandler = true;
				for (Action action : _errorActions) {
					js += "    " + action.getJavaScript(rapidServlet, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}				
			}
		}
		// if there is no error hadling routine insert our own
		if (!gotErrorHandler) js += "    alert('Rapid action failed : ' + ((server && server.responseText) || error));\n";
		// close the error handler
		js += "  };\n";
		
		// assume this action was called from the page and there is no control id nor details
		String controlId = "null";
		if (control != null) controlId = "'" + control.getId() + "'";
		
		// return the JavaScript
		js += "  Action_rapid(ev, '" + application.getId() + "','" + page.getId() + "'," + controlId + ",'" + getId() + "','" + getProperty("actionType") + "', " + getProperty("rapidApp") + ", successCallback, errorCallback);";
		
		return js;
	}
	
	@Override
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonAction) throws Exception {
		
		JSONObject result = new JSONObject();
		
		String action = jsonAction.getString("actionType");
		
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();
		
		String newAppId = null;
		
		// get the id of the app we're about to manipulate
		String appId = jsonAction.getString("appId");	
		// get the version of the app we're about to manipulate
		String appVersion = jsonAction.optString("version", null);	
		// get the application we're about to manipulate
		Application app = rapidServlet.getApplications().get(appId, appVersion);
		
		// only if we had an application
		if (app != null) {
			
			// recreate the rapid request using the application we wish to manipulate
			RapidRequest rapidActionRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app); 
			
			// check the action
			if ("GETAPPS".equals(action)) {
				
				// create a json array for holding our apps
				JSONArray jsonApps = new JSONArray();
				
				// get a sorted list of the applications
				for (String id : rapidServlet.getApplications().getIds()) {
					
					// loop the versions
					for (String version : rapidServlet.getApplications().getVersions(id).keySet()) {
						
						// get the this application version
						Application application = rapidServlet.getApplications().get(id, version);
						
						// get the security
						SecurityAdapter security = application.getSecurity();
						
						// check the user password
						if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
							
							// check the users permission to design this application
							boolean designPermission = security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
							
							// if app is rapid do a further check
							if (designPermission && "rapid".equals(application.getId())) designPermission = application.getSecurity().checkUserRole(rapidRequest, com.rapid.server.Rapid.SUPER_ROLE);
							
							// if we got permssion - add this application to the list 
							if (designPermission) {
								// create a json object
								JSONObject jsonApplication = new JSONObject();
								// add the details we want
								jsonApplication.put("value", application.getId());
								jsonApplication.put("text", application.getName() + " - " + application.getTitle());
								// add the object to the collection
								jsonApps.put(jsonApplication);
								// no need to check any further versions
								break;
							}
							
							
						} // password check

					} // version loop
																						
				} // app loop
				
				// add the actions to the result
				result.put("applications", jsonApps);	
				
				// if there was at least one app
				if (jsonApps.length() > 0) {
				
					// fetch the database drivers
					JSONArray jsonDatabaseDrivers = rapidServlet.getJsonDatabaseDrivers();				
					// check we have some database drivers
					if (jsonDatabaseDrivers != null) {
						// prepare the database driver collection we'll send
						JSONArray jsonSendDatabaseDrivers = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonDatabaseDrivers.length(); i++) {
							// get the item
							JSONObject jsonDatabaseDriver = jsonDatabaseDrivers.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonSendDatabaseDriver = new JSONObject();
							// add type
							jsonSendDatabaseDriver.put("value", jsonDatabaseDriver.get("class"));
							// add name
							jsonSendDatabaseDriver.put("text", jsonDatabaseDriver.get("name"));
							// add to collection
							jsonSendDatabaseDrivers.put(jsonSendDatabaseDriver);
						}
						// add the database drivers to the result
						result.put("databaseDrivers", jsonSendDatabaseDrivers);
					}
					
					// fetch the connection adapters
					JSONArray jsonConnectionAdapters = rapidServlet.getJsonConnectionAdapters();				
					// check we have some database drivers
					if (jsonConnectionAdapters != null) {
						// prepare the database driver collection we'll send
						JSONArray jsonSendConnectionAdapters = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonConnectionAdapters.length(); i++) {
							// get the item
							JSONObject jsonConnectionAdapter = jsonConnectionAdapters.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonSendConnectionAdapter = new JSONObject();
							// add type
							jsonSendConnectionAdapter.put("value", jsonConnectionAdapter.get("class"));
							// add name
							jsonSendConnectionAdapter.put("text", jsonConnectionAdapter.get("name"));
							// add to collection
							jsonSendConnectionAdapters.put(jsonSendConnectionAdapter);
						}
						// add the database drivers to the result
						result.put("connectionAdapters", jsonSendConnectionAdapters);
					}	
					
					// fetch the security adapters
					JSONArray jsonSecurityAdapters = rapidServlet.getJsonSecurityAdapters();				
					// check we have some security adapters
					if (jsonSecurityAdapters != null) {
						// prepare the security adapter collection we'll send
						JSONArray jsonSendSecurityAdapters = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
							// get the item
							JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonSendSecurityAdapter = new JSONObject();
							// add type
							jsonSendSecurityAdapter.put("value", jsonSecurityAdapter.get("type"));
							// add name
							jsonSendSecurityAdapter.put("text", jsonSecurityAdapter.get("name"));
							// add canManageRoles
							jsonSendSecurityAdapter.put("canManageRoles", jsonSecurityAdapter.get("canManageRoles"));
							// add canManageUsers
							jsonSendSecurityAdapter.put("canManageUsers", jsonSecurityAdapter.get("canManageUsers"));
							// add canManageUserRoles
							jsonSendSecurityAdapter.put("canManageUserRoles", jsonSecurityAdapter.get("canManageUserRoles"));
							// add to collection
							jsonSendSecurityAdapters.put(jsonSendSecurityAdapter);
						}
						// add the security adapters to the result
						result.put("securityAdapters", jsonSendSecurityAdapters);
					}									
					
					// process the actions and only send the name and type
					JSONArray jsonSendActions = new JSONArray();
					JSONArray jsonActions = rapidServlet.getJsonActions();
					for (int i = 0; i < jsonActions.length(); i++) {
						JSONObject jsonSysAction = jsonActions.getJSONObject(i);
						// do not send the rapid action
						if (!"rapid".equals(jsonSysAction.getString("type"))) {
							JSONObject jsonSendAction = new JSONObject();
							jsonSendAction.put("name", jsonSysAction.get("name"));
							jsonSendAction.put("type", jsonSysAction.get("type"));
							jsonSendActions.put(jsonSendAction);
						}					
					}				
					// add the actions to the result
					result.put("actions", jsonSendActions);	
					
					// process the controls and only send the name and type for canUserAdd
					JSONArray jsonSendControls = new JSONArray();
					JSONArray jsonControls = rapidServlet.getJsonControls();
					for (int i = 0; i < jsonControls.length(); i++) {
						JSONObject jsonSysControl = jsonControls.getJSONObject(i);
						// only present controls users can add
						if (jsonSysControl.optBoolean("canUserAdd")) {
							JSONObject jsonSendControl = new JSONObject();
							jsonSendControl.put("name", jsonSysControl.get("name"));
							jsonSendControl.put("type", jsonSysControl.get("type"));
							jsonSendControls.put(jsonSendControl);
						}					
					}				
					// add the controls to the result
					result.put("controls", jsonSendControls);			
					
					// add the devices
					result.put("devices", rapidServlet.getDevices());
					
				} // at least one app check
				
				// add the current userName to the result
				result.put("userName", rapidRequest.getUserName());
				
			} else if ("GETVERSIONS".equals(action)) {
				
				// prepare a json array we're going to include in the result
				JSONArray jsonVersions = new JSONArray();
											
				// get the versions
				Versions versions = rapidServlet.getApplications().getVersions(appId);
				
				// if there are any
				if (versions != null) {
																										
					// loop the list of applications sorted by id (with rapid last)
					for (Application application : versions.sort()) {
						
						// get the security
						SecurityAdapter security = application.getSecurity();
						
						// check the user password
						if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
							
							// check the users permission to design this application
							boolean designPermission = security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
							
							// if app is rapid do a further check
							if (designPermission && "rapid".equals(application.getId())) designPermission = application.getSecurity().checkUserRole(rapidRequest, com.rapid.server.Rapid.SUPER_ROLE);
							
							// check the RapidDesign role is present in the users roles for this application
							if (designPermission) {												
								
								// make a json object for this version
								JSONObject jsonVersion = new JSONObject();
								// add the version
								jsonVersion.put("value", application.getVersion());
								// derive the text
								String text = application.getVersion();
								// if live add some
								if (application.getStatus() == 1) text += " - (Live)";
								// add the title
								jsonVersion.put("text", text);																					
								// put the entry into the collection
								jsonVersions.put(jsonVersion);	
								
							} // design permission
							
						} // password check
																																												
					} // versions loop
						
				} // got versions check
				
				// add the versions to the result
				result.put("versions", jsonVersions);
																												
			} else if ("GETVERSION".equals(action)) {
				
				// get the security
				SecurityAdapter security = app.getSecurity();
				
				// password check
				if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
					
					// check the users permission to design this application
					boolean designPermission = security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
					
					// if app is rapid do a further check
					if (designPermission && "rapid".equals(app.getId())) designPermission = app.getSecurity().checkUserRole(rapidRequest, com.rapid.server.Rapid.SUPER_ROLE);
					
					if (designPermission) {
						
						// add the name
						result.put("name", app.getName());
						// add the version
						result.put("version", app.getVersion());
						// add the status
						result.put("status", app.getStatus());
						// add the title
						result.put("title", app.getTitle());
						// add the description
						result.put("description", app.getDescription());
						// add whether to show control ids
						result.put("showControlIds", app.getShowControlIds());
						// add whether to show action ids
						result.put("showActionIds", app.getShowActionIds());
						
						// create a simplified array to hold the pages
						JSONArray jsonPages = new JSONArray();
						// retrieve the pages
						List<PageHeader> pages = app.getPages().getSortedPages();
						// check we have some
						if (pages != null) {
							for (PageHeader page : pages) {
								JSONObject jsonPage = new JSONObject();						
								jsonPage.put("text", page.getName() + " - " + page.getTitle());
								jsonPage.put("value", page.getId());
								jsonPages.put(jsonPage);
							}
						}				
						// add the pages
						result.put("pages", jsonPages);
						
						// add the start page Id
						result.put("startPageId", app.getStartPageId());
						
						// add the styles
						result.put("styles", app.getStyles());
						// add the functions
						result.put("functions", app.getFunctions());
						// add the security adapter
						result.put("securityAdapter", app.getSecurityAdapterType());	
						// add action types
						result.put("actionTypes", app.getActionTypes());				
						// add control types
						result.put("controlTypes", app.getControlTypes());
						
						// create an array for the database connections
						JSONArray jsonDatabaseConnections = new JSONArray();
						
						
						// check we have some database connections
						if (app.getDatabaseConnections() != null) {
							// remember the index
							int index = 0;
							// loop and add to jsonArray
							for (DatabaseConnection dbConnection : app.getDatabaseConnections()) {
								// create an object for the database connection
								JSONObject jsonDBConnection = new JSONObject();
								// set the index as the value
								jsonDBConnection.put("value", index);
								// set the name as the text
								jsonDBConnection.put("text", dbConnection.getName());
								// add to our collection
								jsonDatabaseConnections.put(jsonDBConnection);
								// inc the index
								index ++;
							}					
						}
						// add database connections
						result.put("databaseConnections", jsonDatabaseConnections);
						
						
						// create an array for the soa webservices
						JSONArray jsonWebservices = new JSONArray();
										
						// check we have some webservices
						if (app.getWebservices() != null) {
							// loop and add to jsonArray
							for (Webservice webservice : app.getWebservices()) {
								jsonWebservices.put(webservice.getName());
							}					
						}	
						// add webservices connections
						result.put("webservices", jsonWebservices);
						
						// create an array for the parameters
						JSONArray jsonParameters = new JSONArray();
										
						// check we have some webservices
						if (app.getParameters() != null) {
							// loop and add to jsonArray
							for (Parameter parameter : app.getParameters()) {
								jsonParameters.put(parameter.getName());
							}					
						}	
						// add webservices connections
						result.put("parameters", jsonParameters);
						
						
						// create an array for the app backups
						JSONArray jsonAppBackups = new JSONArray();
						
						// check we have some app backups
						if (app.getApplicationBackups(rapidServlet) != null) {
							// loop and add to jsonArray
							for (Application.Backup appBackup : app.getApplicationBackups(rapidServlet)) {
								// create the backup json object
								JSONObject jsonBackup = new JSONObject();
								// create a date formatter
								//SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
								// populate it
								jsonBackup.append("id", appBackup.getId());
								jsonBackup.append("date", rapidServlet.getLocalDateTimeFormatter().format(appBackup.getDate()));
								jsonBackup.append("user", appBackup.getUser());
								jsonBackup.append("size", appBackup.getSize());
								// add it
								jsonAppBackups.put(jsonBackup);
							}					
						}	
						// add webservices connections
						result.put("appbackups", jsonAppBackups);
						
						// add the max number of application backups
						result.put("appBackupsMaxSize", app.getApplicationBackupsMaxSize());
										
						// create an array for the page backups
						JSONArray jsonPageBackups = new JSONArray();
						
						// check we have some app backups
						if (app.getPageBackups(rapidServlet) != null) {
							// loop and add to jsonArray
							for (Application.Backup appBackup : app.getPageBackups(rapidServlet)) {
								// create the backup json object
								JSONObject jsonBackup = new JSONObject();						
								// populate it
								jsonBackup.append("id", appBackup.getId());
								jsonBackup.append("page", appBackup.getName());
								jsonBackup.append("date", rapidServlet.getLocalDateTimeFormatter().format(appBackup.getDate()));
								jsonBackup.append("user", appBackup.getUser());
								jsonBackup.append("size", appBackup.getSize());
								// add it
								jsonPageBackups.put(jsonBackup);
							}					
						}	
						// add webservices connections
						result.put("pagebackups", jsonPageBackups);

						// add the max number of page backups
						result.put("pageBackupsMaxSize", app.getPageBackupsMaxSize());
							
					} // permission check
					
				} // password check
							
			} else if ("GETDBCONN".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();
				
				// check we have database connections
				if (dbConns != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < dbConns.size()) {
						// get the database connection
						DatabaseConnection dbConn = dbConns.get(index);
						// add the name
						result.put("name", dbConn.getName());
						// add the driver type
						result.put("driver", dbConn.getDriverClass());
						// add the connection adapter class
						result.put("connectionString", dbConn.getConnectionString());
						// add the connection adapter class
						result.put("connectionAdapter", dbConn.getConnectionAdapterClass());
						// add the user name
						result.put("userName", dbConn.getUserName());
						// add the password
						if ("".equals(dbConn.getPassword())) {
							result.put("password", "");
						} else {
							result.put("password", "********");
						}
					}
				}
				
			} else if ("GETSOA".equals(action)) {
				
				// retain the JSON object which we will return
				JSONObject jsonWebservice;
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the database connections
				List<Webservice> webservices = app.getWebservices();
				
				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {
						// get the webservice from the collection
						Webservice webservice = webservices.get(index);
						// convert it into a json object
						jsonWebservice = new JSONObject(webservice);
						// add the type
						jsonWebservice.put("type", webservice.getClass().getSimpleName());
						// add the user to the response
						result.put("webservice", jsonWebservice);
					}
				}
												
			} else if ("GETSEC".equals(action)) {
				
				// get the securityAdapter type from the jsonAction
				String securityAdapterType = jsonAction.getString("securityAdapter");
				
				// assume the current class has not been set				
				String securityAdapterClass = "";
				
				// get all of the available security adapters
				JSONArray jsonSecurityAdapters = rapidServlet.getJsonSecurityAdapters();				
				// check we have some security adapters
				if (jsonSecurityAdapters != null) {
						// loop what we have
					for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
						// get the item
						JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
						// if this is the type that came in
						if (securityAdapterType.equals(jsonSecurityAdapter.getString("type"))) {
							// retain the name
							securityAdapterClass = jsonSecurityAdapter.getString("class");
							// we're done
							break;
						}
					}
				}	 
												
				// get the current app security adapter
				SecurityAdapter security = app.getSecurity();
												
				// if we got one
				if (security != null) {
					
					// if it's different from what came in
					if (!securityAdapterClass.equals(security.getClass().getCanonicalName())) {						
						// set the new security adapter
						app.setSecurity(rapidServlet.getServletContext(), securityAdapterType);
						// read it back again
						security = app.getSecurity();
					}
					
					// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
					rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
					
					// get the roles
					Roles roles = security.getRoles(rapidRequest);
											
					// add the entire roles collection to the response
					result.put("roles", roles);
					
					// if we had some roles
					if (roles != null) {
						// prepapre a list of just the role names (not descriptions)
						List<String> roleNames = new ArrayList<String>();
						// loop the roles
						for (Role role : roles) {
							roleNames.add(role.getName());
						}
						// add the rolenames
						result.put("roleNames", roleNames);
					}
					
					// add the users to the response
					result.put("users", security.getUsers(rapidRequest));					
																				
				} // got security
				
			} else if ("GETUSER".equals(action)) { 
										
				// get the userName from the incoming json
				String userName = jsonAction.getString("userName");
				
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				
				// derive whether this is the current user
				boolean currentUser = userName.toLowerCase().equals(rapidRequest.getUserName().toLowerCase());
				
				// now set the rapid request user to the user we want
				rapidRequest.setUserName(userName);
				
				// get the app security
				SecurityAdapter security = app.getSecurity();
				
				// get the user
				User user = security.getUser(rapidRequest);
				
				// add the user name
				result.put("userName", userName);
				
				// add the user description
				result.put("description", user.getDescription());
				
				// set the default password mask
				String password = "********";
				
				// if the password is blank reflect this in what we send
				if ("".equals(user.getPassword())) password = "";
				
				// add a masked password
				result.put("password", password);
			
				// if we got one
				if (security != null) {
									
					// get the users roles
					List<String> roles = security.getUser(rapidRequest).getRoles();
					
					// add the users to the response
					result.put("roles", roles);											
												
				} // got security
				
				// if this user record is for the logged in user
				result.put("currentUser", currentUser);
								
			} else if ("GETUSERS".equals(action)) { 
							
				// get the app security
				SecurityAdapter security = app.getSecurity();
				
				// if we got one
				if (security != null) {
					
					// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
					rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				
					// get the users
					Users users = security.getUsers(rapidRequest);
					
					// add the users
					result.put("users", users);
					
					// add the current user
					result.put("currentUser", rapidRequest.getUserName());
																						
				} // got security
				
			} else if ("GETPARAM".equals(action)) {
				
				// retrieve the index
				int index = jsonAction.getInt("index");
				
				// create the json object
				JSONObject jsonParameter = new JSONObject();
				
				// check the parameters
				if (app.getParameters() != null) {
					
					// check we have the one requested
					if (index >= 0 && index < app.getParameters().size()) {
						
						// get the parameter
						Parameter parameter = app.getParameters().get(index);
																		
						// add the name and value
						jsonParameter.put("name", parameter.getName());
						jsonParameter.put("value", parameter.getValue());
																		
					}
				}
				
				// add the parameter to the result
				result.put("parameter", jsonParameter);
						
			} else if ("GETDEVICE".equals(action)) {
				
				// retrieve the index
				int index = jsonAction.getInt("index");
				
				// create the json object
				JSONObject jsonDevice = new JSONObject();
				
				// reference to all devices
				Devices devices = rapidServlet.getDevices();
				
				// check we have devices				
				if (devices != null) {
					// check the index is ok
					if (index >= 0 && index < devices.size()) {
						
						// get the device
						Device device = rapidServlet.getDevices().get(index);
						
						// add the name and value
						jsonDevice.put("name", device.getName());
						jsonDevice.put("width", device.getWidth());
						jsonDevice.put("height", device.getHeight());
						jsonDevice.put("ppi", device.getPPI());
						jsonDevice.put("scale", device.getScale());
						
					}
				}
				
				// add the parameter to the result
				result.put("device", jsonDevice);
						
			} else if ("RELOADACTIONS".equals(action)) {
							
				// load actions and set the result message
				result.put("message", RapidServletContextListener.loadActions(rapidServlet.getServletContext()) + " actions reloaded");  
								
			} else if ("RELOADCONTROLS".equals(action)) {
				
				// load controls and set the result message
				result.put("message", RapidServletContextListener.loadControls(rapidServlet.getServletContext()) + " controls reloaded");  				
				
			} else if ("RELOADAPPLICATIONS".equals(action)) {
				
				// load applications and set the result message
				result.put("message", RapidServletContextListener.loadApplications(rapidServlet.getServletContext()) + " applications reloaded");  
								
			} else if ("RELOADADAPTERS".equals(action)) {
								
				// load adapters and set the result message
				int databaseDrivers = 0;
				int connectionAdapters = 0;
				int securityAdapters = 0;
													
				databaseDrivers = RapidServletContextListener.loadDatabaseDrivers(rapidServlet.getServletContext());
				
				connectionAdapters = RapidServletContextListener.loadConnectionAdapters(rapidServlet.getServletContext());
				
				securityAdapters = RapidServletContextListener.loadSecurityAdapters(rapidServlet.getServletContext());
									
				result.put("message", databaseDrivers + " database drivers, " + connectionAdapters + " connection adapters, " + securityAdapters + " security adapters");
								
			} else if ("RELOADVERSION".equals(action)) {
				
				// look for an application file in the application folder
				File applicationFile = new File(app.getConfigFolder(rapidServlet.getServletContext()) + "/application.xml");
				
				// reload the application from file
				Application reloadedApplication = Application.load(rapidServlet.getServletContext(), applicationFile);
				
				// replace it into the applications collection
				rapidServlet.getApplications().put(reloadedApplication);
				
				// load applications and set the result message
				result.put("message", "Version reloaded");  
								
			} else if ("SAVEAPP".equals(action)) {
			
				// get the new values
				String id = Files.safeName(jsonAction.getString("name")).toLowerCase();
				String version = Files.safeName(jsonAction.getString("saveVersion"));
				int status = jsonAction.optInt("status");
				String name = jsonAction.getString("name");
				String title = jsonAction.getString("title");
				String description = jsonAction.getString("description");
				boolean showControlIds = jsonAction.optBoolean("showControlIds");
				boolean showActionIds = jsonAction.optBoolean("showActionIds");
				String startPageId = jsonAction.optString("startPageId","");
				
				// assume we do not need to update the applications drop down
				boolean appUpdated = false;				
				
				// if the id or version is now different we need to move it, rebuilding all the resources as we go
				if (!app.getId().equals(id) || !app.getVersion().equals(version)) {
					// copy the app to the id/version, returning the new one for saving
					app = app.copy(rapidServlet, rapidRequest, id, version, true, true);
					// mark that it has been updated
					appUpdated = true;
				}
				
				// update the values
				app.setName(name);
				app.setStatus(status);
				app.setTitle(title);
				app.setDescription(description);
				app.setShowControlIds(showControlIds);
				app.setShowActionIds(showActionIds);
				app.setStartPageId(startPageId);
				
				// save
				app.save(rapidServlet, rapidRequest, true); 
											
				// add the application to the response
				result.put("message", "Application details saved");
				result.put("update", appUpdated);

				
			} else if ("SAVESTYLES".equals(action)) {
							
				String styles = jsonAction.getString("styles");
				
				app.setStyles(styles);
				
				app.save(rapidServlet, rapidRequest, true);
				
				// add the application to the response
				result.put("message", "Styles saved");
				
			} else if ("SAVEFUNCTIONS".equals(action)) {
				
				String functions = jsonAction.getString("functions");
				
				app.setFunctions(functions);
				
				app.save(rapidServlet, rapidRequest, true);
				
				// add the application to the response
				result.put("message", "Functions saved");
				
			} else if ("SAVEDBCONN".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();
				
				// remeber whether we found the connection
				boolean foundConnection = false;
				
				// check we have database connections
				if (dbConns != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < dbConns.size()) {
						// get the database connection
						DatabaseConnection dbConn = dbConns.get(index);
						
						// set the databse connection properties
						dbConn.setName(jsonAction.getString("name"));
						dbConn.setDriverClass(jsonAction.getString("driver"));
						dbConn.setConnectionString(jsonAction.getString("connectionString"));
						dbConn.setConnectionAdapterClass(jsonAction.getString("connectionAdapter"));
						dbConn.setUserName(jsonAction.getString("userName"));
						String password = jsonAction.getString("password");
						// only set the password if it's different from the default
						if (!"********".equals(password)) dbConn.setPassword(password);
						
						// reset the dbconn so the adapter is re-initialised with any changes
						dbConn.reset();
						
						// save the app
						app.save(rapidServlet, rapidRequest, true);
						
						foundConnection = true;
						
						// add the application to the response
						result.put("message", "Database connection saved");
						
					}
				}
				
				if (!foundConnection) result.put("message", "Database connection could not be found");
				
			} else if ("SAVESOASQL".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the webservices
				List<Webservice> webservices = app.getWebservices();
				
				// remeber whether we found the connection
				boolean foundWebservice = false;
				
				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {
						// get the web service connection
						Webservice webservice = webservices.get(index);
						// check the type
						if (webservice.getClass() == SQLWebservice.class) {
							// cast to our type
							SQLWebservice sqlWebservice = (SQLWebservice) webservice;
							
							// set the webservice properties
							sqlWebservice.setName(jsonAction.getString("name").trim());
							sqlWebservice.setDatabaseConnectionIndex(jsonAction.getInt("databaseConnectionIndex"));
							
							// get the rest of the complex details
							JSONObject jsonDetails = jsonAction.getJSONObject("details");
							
							// set the sql
							sqlWebservice.setSQL(jsonDetails.getString("SQL").trim());
							
							// get the json request
							JSONObject jsonRequestSchmea = jsonDetails.optJSONObject("requestSchema");
							// check it
							if (jsonRequestSchmea != null) {
								// get the root element
								JSONObject jsonElement = jsonRequestSchmea.getJSONObject("rootElement");
								// get its name
								String elementName = jsonElement.optString("name").trim();								
								// create the schema
								SOASchema requestSchema = new SOASchema(elementName);
								// get any child elements
								JSONArray jsonChildElements = jsonElement.optJSONArray("childElements");
								// check
								if (jsonChildElements != null) {
									// loop
									for (int i = 0; i < jsonChildElements.length(); i++) {
										// get child element
										JSONObject jsonChildElement = jsonChildElements.getJSONObject(i);
										// get child element name
										String childElementName = jsonChildElement.getString("name").trim();
										// get its data type
										int childElementDataType = jsonChildElement.optInt("dataType",1);
										// add child element to schema (and get a reference)
										SOASchemaElement soaChildElement = requestSchema.addChildElement(childElementName);
										// set the data type
										soaChildElement.setDataType(childElementDataType);
										// add any restrictions
										soaChildElement.setRestrictions(getRestrictions(jsonChildElement.optJSONArray("restrictions")));
									}
								}
								// set the schema property
								sqlWebservice.setRequestSchema(requestSchema);
							}
							
							// get the json response
							JSONObject jsonResponseSchema = jsonDetails.optJSONObject("responseSchema");
							// check it
							if (jsonResponseSchema != null) {
								// get the root element
								JSONObject jsonElement = jsonResponseSchema.getJSONObject("rootElement");
								// get its name
								String elementName = jsonElement.optString("name");
								// get if array
								boolean isArray = Boolean.parseBoolean(jsonElement.optString("isArray"));
								// create the schema
								SOASchema responseSchema = new SOASchema(elementName, isArray);
								// get any child elements
								JSONArray jsonChildElements = jsonElement.optJSONArray("childElements");
								// check
								if (jsonChildElements != null) {
									// loop
									for (int i = 0; i < jsonChildElements.length(); i++) {
										// get child element
										JSONObject jsonChildElement = jsonChildElements.getJSONObject(i);
										// get child element name
										String childElementName = jsonChildElement.getString("name").trim();
										// get child element field
										String childElementField = jsonChildElement.optString("field","");
										// get its data type
										int childElementDataType = jsonChildElement.optInt("dataType",1);
										// add child element to schema (and get reference)
										SOASchemaElement soaChildElement = responseSchema.addChildElement(childElementName);
										// set field
										soaChildElement.setField(childElementField);
										// set data type
										soaChildElement.setDataType(childElementDataType);
										// add any restrictions
										soaChildElement.setRestrictions(getRestrictions(jsonChildElement.optJSONArray("restrictions")));
									}
								}
								// set the schema property
								sqlWebservice.setResponseSchema(responseSchema);
							}
														
							// save the app
							app.save(rapidServlet, rapidRequest, true);
							
							foundWebservice = true;
							
							// add the application to the response
							result.put("message", "SQL webservice saved");
						}	
					}
				}
				
				if (!foundWebservice) result.put("message", "SQL webservice could not be found");
				
			} else if ("SAVESOAJAVA".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the webservices
				List<Webservice> webservices = app.getWebservices();
				
				// remeber whether we found the connection
				boolean foundWebservice = false;
				
				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {
						// get the web service connection
						Webservice webservice = webservices.get(index);
						// check the type
						if (webservice.getClass() == JavaWebservice.class) {
							
							// cast to our type
							JavaWebservice javaWebservice = (JavaWebservice) webservice;
							
							// set the webservice properties
							javaWebservice.setName(jsonAction.getString("name").trim());						
							javaWebservice.setClassName(jsonAction.getString("className").trim());
												
							// save the app
							app.save(rapidServlet, rapidRequest, true);
							
							foundWebservice = true;
							
							// add the application to the response
							result.put("message", "Java webservice saved");
						}	
					}
				}
				
				if (!foundWebservice) result.put("message", "Java webservice could not be found");
				
			} else if ("SAVESECURITYADAPT".equals(action)) { 
				
				String securityAdapter = jsonAction.getString("securityAdapter").trim();
				
				app.setSecurityAdapterType(securityAdapter);
				
				app.save(rapidServlet, rapidRequest, true);
				
				// add the application to the response
				result.put("message", "Security adapter saved");
				
			} else if ("SAVEACTIONS".equals(action)) {
				
				JSONArray jsonActionTypes = jsonAction.getJSONArray("actionTypes");
				
				ArrayList<String> actionTypes = new ArrayList<String>();
				
				for (int i =0; i < jsonActionTypes.length(); i++) {
					actionTypes.add(jsonActionTypes.getString(i).trim());
				}
				
				// make sure some required actions are there if this is the rapid app
				if ("rapid".equals(appId)) {					
					String [] requiredActionTypes = {"rapid", "ajax", "control", "custom", "dataCopy", "existing", "validation"};
					for (String actionType : requiredActionTypes) {
						if (!actionTypes.contains(actionType)) actionTypes.add(actionType);
					}					
				}
				
				// sort the types
				Collections.sort(actionTypes);
				
				// put the list into the app
				app.setActionTypes(actionTypes);
				
				// save it
				app.save(rapidServlet, rapidRequest, true);
				
				// add the message to the response
				result.put("message", actionTypes.size() + " actions");
				
			} else if ("SAVECONTROLS".equals(action)) {
				
				JSONArray jsonControlTypes = jsonAction.getJSONArray("controlTypes");
				
				ArrayList<String> controlTypes = new ArrayList<String>();
				
				for (int i =0; i < jsonControlTypes.length(); i++) {
					controlTypes.add(jsonControlTypes.getString(i).trim());
				}
				
				// make sure some required controls are there if this is the rapid app
				if ("rapid".equals(appId)) {					
					String [] requiredControlTypes = {"button", "dataStore", "dropdown", "grid", "image", "input", "page", "table", "tabGroup", "text"};
					for (String controlType : requiredControlTypes) {
						if (!controlTypes.contains(controlType)) controlTypes.add(controlType);
					}					
				}
				
				// sort the types
				Collections.sort(controlTypes);
				
				// add the controls to the app
				app.setControlTypes(controlTypes);
				
				// save
				app.save(rapidServlet, rapidRequest, true);
				
				// add the message to the response
				result.put("message", controlTypes.size() + " controls");
				
			} else if ("REBUILDPAGES".equals(action)) {
			
				// add the application to the response
				result.put("message", "This feature is not supported");
				
			} else if ("NEWAPP".equals(action)) {
				
				// retrieve the inputs from the json
				String name = jsonAction.getString("name").trim();
				String version = jsonAction.getString("version").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();
				
				// create a new application with our reusable, private method
				Application newApp = createApplication(rapidServlet, rapidRequest, name, version, title, description);
											
				// set the result message
				result.put("message", "Application " + app.getTitle() + " created");
				
				// set the result appId
				result.put("appId", newApp.getId());
				
				// set the result version
				result.put("version", newApp.getVersion());
				
			} else if ("DELAPP".equals(action)) {
						
				// check we have an app
				if (app != null)  {						
					// get the collection of applications and versions
					Applications applications = rapidServlet.getApplications();
					// get all versions of this application
					Versions versions = applications.getVersions(app.getId());
					// get the number of version
					int versionCount = versions.size();
					// make a list of versions
					ArrayList<String> versionNumbers = new ArrayList<String>();
					// loop the versions
					for (String version : versions.keySet()) {
						versionNumbers.add(version);						
					}
					// loop the versionNumbers
					for (String versionNumber: versionNumbers) {
						// get this version
						Application v = applications.get(app.getId(), versionNumber);
						// delete it
						v.delete(rapidServlet, rapidActionRequest, true);
					}
					// set the result message
					result.put("message", versionCount + " application version" + (versionCount == 1 ? "" : "s") + " deleted for " + app.getName());					
				}
								
			} else if ("DUPAPP".equals(action)) {
				
				String version = jsonAction.getString("newVersion").trim();			
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();
				
				// use the application.copy routine
				app.copy(rapidServlet, rapidRequest, app.getId(), version, false, false);
				
				// set the new title
				app.setTitle(title);
				// set the new description
				app.setDescription(description);
				
				// save
				app.save(rapidServlet, rapidRequest, false);
																		
				// set the result message
				result.put("message", "Application " + app.getTitle() + " duplicated");
				
			} else if ("NEWVERSION".equals(action)) {
				
				// retrieve the inputs from the json
				String id = jsonAction.getString("appId").trim();
				String version = jsonAction.getString("newVersion").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();
				
				// create a new application with our reusable, private method
				Application newApp = createApplication(rapidServlet, rapidRequest, id, version, title, description);
											
				// set the result message
				result.put("message", "Version " + newApp.getVersion() + " created for " + newApp.getTitle());
				
				// set the result appId
				result.put("appId", newApp.getId());
				
				// set the result version
				result.put("version", newApp.getVersion());
				
			} else if ("DELVERSION".equals(action)) {
				
				// delete the application version
				if (app != null) app.delete(rapidServlet, rapidActionRequest, false);
				// set the result message
				result.put("message", "Version " + app.getVersion() + " deleted");
				
			} else if ("NEWPAGE".equals(action)) {
				
				String id = jsonAction.getString("id").trim();
				String name = jsonAction.getString("name").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();
				
				Page newPage = new Page();
				newPage.setId(id);
				newPage.setName(name);
				newPage.setTitle(title);
				newPage.setDescription(description);
				newPage.setCreatedBy(rapidRequest.getUserName());
				newPage.setCreatedDate(new Date());
				
				// save the page to file
				newPage.save(rapidServlet, rapidActionRequest, app, false);
								
				// put the id in the result
				result.put("id", id);
				
				// set the result message
				result.put("message", "Page " + newPage.getTitle() + " created");
				
			} else if ("DELPAGE".equals(action)) {
				
				// get the id
				String id = jsonAction.getString("id").trim();			
				// retrieve the page
				Page delPage = app.getPages().getPage(rapidRequest.getRapidServlet().getServletContext(), id);
				// delete it if we got one
				if (delPage != null) delPage.delete(rapidServlet, rapidActionRequest, app);
				// set the result message
				result.put("message", "Page " + delPage.getName() + " delete");
				
			} else if ("NEWDBCONN".equals(action)) {
				
				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();
				// instantiate if null
				if (dbConns == null) dbConns = new ArrayList<DatabaseConnection>();
				
				// make the new database connection
				DatabaseConnection dbConn = new DatabaseConnection(
					jsonAction.getString("name").trim(),
					jsonAction.getString("driver").trim(),
					jsonAction.getString("connectionString").trim(),
					jsonAction.getString("connectionAdapter").trim(),
					jsonAction.getString("userName").trim(),
					jsonAction.getString("password")
				); 
				
				// add it to the collection
				dbConns.add(dbConn);
				
				// save the app
				app.save(rapidServlet, rapidRequest, true);				
				
				// add the application to the response
				result.put("message", "Database connection added");
												
			} else if ("DELDBCONN".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();
				
				// remeber whether we found the connection
				boolean foundConnection = false;
				
				// check we have database connections
				if (dbConns != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < dbConns.size()) {
						
						// remove the database connection
						dbConns.remove(index);
						
						// save the app
						try { app.save(rapidServlet, rapidRequest, true);	} 
						catch (Exception ex) { throw new JSONException(ex);	}
						
						// add the application to the response
						result.put("message", "Database connection deleted");
						
					}
				}
				
				if (!foundConnection) result.put("message", "Database connection could not be found");
				
			} else if ("NEWSOA".equals(action)) {
				
				// the webservice we are about to make
				Webservice webservice = null;
				
				// get the type
				String type = jsonAction.getString("type");
				
				if ("SQLWebservice".equals(type)) {
					// make the new database connection
					webservice = new SQLWebservice( 
						jsonAction.getString("name").trim()
					); 
				} else if ("JavaWebservice".equals(type)) {
					webservice = new JavaWebservice(
						jsonAction.getString("name").trim()
					);
				}
				
				// if one was made
				if (webservice != null) {
					
					// add it to the collection
					app.getWebservices().add(webservice);
				
					// save the app
					app.save(rapidServlet, rapidRequest, true);			
				
					// add the application to the response
					result.put("message", "SOA webservice added");
					
				} else {
					// send message
					result.put("message", "Webservice type not recognised");
				}
												
			} else if ("DELSOA".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the webservices
				List<Webservice> webservices = app.getWebservices();
				
				// remeber whether we found the webservice
				boolean foundWebservice = false;
				
				// check we have database connections
				if (webservices != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < webservices.size()) {
						
						// remove the database connection
						webservices.remove(index);
						
						// save the app
						app.save(rapidServlet, rapidRequest, true);
						
						// add the application to the response
						result.put("message", "SOA webservice deleted");
						
					}
				}
				
				if (!foundWebservice) result.put("message", "SOA webservice could not be found");
				
			} else if ("NEWROLE".equals(action)) {
													
				// get the role name
				String roleName = jsonAction.getString("role").trim();
				// get the role descrition
				String description = jsonAction.getString("description").trim();
				
				// add the role
				app.getSecurity().addRole(rapidRequest, new Role(roleName, description));
				// set the result message
				result.put("message", "Role added");
																	
			} else if ("DELROLE".equals(action)) {
				
				// get the role
				String role = jsonAction.getString("role").trim();
				// delete the role
				app.getSecurity().deleteRole(rapidRequest, role);
				// set the result message
				result.put("message", "Role deleted");					
								
			} else if ("NEWUSER".equals(action)) {
				
				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// get the userDescription
				String description = jsonAction.optString("description","").trim();
				// get the password
				String password = jsonAction.getString("password");
				
				// get the security
				SecurityAdapter security = app.getSecurity();
				
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
												
				// add the user
				security.addUser(rapidRequest, new User(userName, description, password));
				
				// update the Rapid Request to have the new user name
				rapidRequest.setUserName(userName);
				
				// if this is the rapid app
				if ("rapid".equals(app.getId())) {
					// check for useAdmin
					boolean useAdmin = jsonAction.optBoolean("useAdmin");
					// add role if we were given one
					if (useAdmin) security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
					// check for useDesign
					boolean useDesign = jsonAction.optBoolean("useDesign");
					// add role if we were given one
					if (useDesign) security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
				}
				
				// set the result message
				result.put("message", "User added");					
								
			} else if ("DELUSER".equals(action)) {
				
				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);				
				// delete the user
				app.getSecurity().deleteUser(rapidRequest);
				// remove any of their page locks
				app.removeUserPageLocks(rapidServlet.getServletContext(), userName);
				// set the result message
				result.put("message", "User deleted");
													
			} else if ("SAVEROLE".equals(action)) {
				
				// get the role
				String roleName = jsonAction.getString("role").trim();
				// get the description
				String roleDescription = jsonAction.getString("description").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// update the role
				app.getSecurity().updateRole(rapidRequest, new Role(roleName, roleDescription));
				// set the result message
				result.put("message", "Role details saved");
													
			} else if ("NEWUSERROLE".equals(action)) {
				
				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);				
				// get the role
				String role = jsonAction.getString("role").trim();
				// add the user role
				app.getSecurity().addUserRole(rapidRequest, role);
				// set the result message
				result.put("message", "User role added");
									
			} else if ("DELUSERROLE".equals(action)) {
				
				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);
				// get the role
				String role = jsonAction.getString("role").trim();
				// add the user role
				app.getSecurity().deleteUserRole(rapidRequest, role);
				// set the result message
				result.put("message", "User role deleted");
													
			} else if ("SAVEUSER".equals(action)) {
			
				// get the userName
				String userName = jsonAction.getString("userName").trim();
				// recreate the rapidRequest with the selected app (so app parameters etc are available from the app in the rapidRequest)
				rapidRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);
				// override the standard request user
				rapidRequest.setUserName(userName);				
				// get the description
				String description = jsonAction.getString("description").trim();
				// get the password
				String password = jsonAction.getString("password");
				
				// get the security
				SecurityAdapter security = app.getSecurity();
				// get the user
				User user = security.getUser(rapidRequest);
				// update the description
				user.setDescription(description);
				// update the password if different from the mask
				if (!"********".equals(password)) user.setPassword(password);
				// update the user
				security.updateUser(rapidRequest, user);
				
				// if we are updating the rapid application we have used checkboxes for the Rapid Admin and Rapid Designer roles
				if ("rapid".equals(app.getId())) {
					// get the valud of rapidAdmin
					String useAdmin = jsonAction.optString("useAdmin");
					// check useAdmin was sent
					if (useAdmin != null) {
						// check the user was given the role
						if ("true".equals(useAdmin)) {
							// add the role if the user doesn't have it already
							if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE))
								security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
						} else {
							// remove the role
							security.deleteUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
						}
					}
					// get the valud of rapidDesign
					String useDesign = jsonAction.optString("useDesign");
					// check useAdmin was sent
					if (useDesign != null) {
						// check the user was given the role
						if ("true".equals(useDesign)) {
							// add the role if the user doesn't have it already
							if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE))
								security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
						} else {
							// remove the role
							security.deleteUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
						}
					}
				}
				
				// set the result message
				result.put("message", "User details saved");					
			
			} else if ("NEWPARAM".equals(action)) {
				
				// add a new parameter to the collection
				app.getParameters().add(new Parameter());
				
			} else if ("DELPARAM".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// remove the parameter
				app.getParameters().remove(index);

				// save the app
				app.save(rapidServlet, rapidRequest, true);
								
				// set the result message
				result.put("message", "Parameter deleted");
				
			} else if ("SAVEPARAM".equals(action)) {	
				
				int index = jsonAction.getInt("index");
				String name = jsonAction.getString("name");
				String value = jsonAction.getString("value");
				
				// fetch the parameter
				Parameter parameter = app.getParameters().get(index);
				
				// update it
				parameter.setName(name);
				parameter.setValue(value);
				
				// save the app
				app.save(rapidServlet, rapidRequest, true);
								
				// set the result message
				result.put("message", "Parameter details saved");
												
			} else if ("NEWDEVICE".equals(action)) {
				
				// get the devices
				Devices devices = rapidServlet.getDevices();
				
				// add a new device to the collection
				devices.add(new Device("New device", 500, 500, 200, 1d));
				
				// save it
				devices.save(rapidServlet.getServletContext());
				
			} else if ("DELDEVICE".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the devices
				Devices devices = rapidServlet.getDevices();
				
				// remove the device
				devices.remove(index);

				// save the devices
				devices.save(rapidServlet.getServletContext());
								
				// set the result message
				result.put("message", "Device deleted");
				
			} else if ("SAVEDEVICE".equals(action)) {	
				
				int index = jsonAction.getInt("index");
				String name = jsonAction.getString("name");
				int width = jsonAction.getInt("width");
				int height = jsonAction.getInt("height");
				int ppi = jsonAction.getInt("ppi");
				double scale = jsonAction.getDouble("scale");
				
				// fetch the devices
				Devices devices = rapidServlet.getDevices();
				// fetch the device
				Device device = devices.get(index);
				
				// update the device
				device.setName(name);
				device.setWidth(width);
				device.setHeight(height);
				device.setPPI(ppi);
				device.setScale(scale);
				
				// save the devices
				devices.save(rapidServlet.getServletContext());
								
				// set the result message
				result.put("message", "Device details saved");
												
			} else if ("TESTDBCONN".equals(action)) {
				
				// get the index
				int index = jsonAction.getInt("index");
				
				// get the database connections
				List<DatabaseConnection> dbConns = app.getDatabaseConnections();
				
				// remeber whether we found the connection
				boolean foundConnection = false;
				
				// check we have database connections
				if (dbConns != null) {
					// check the index we where given will retieve a database connection
					if (index > -1 && index < dbConns.size()) {
						
						// retrieve the details from the json
						String driverClass = jsonAction.getString("driver").trim();
						String connectionString = jsonAction.getString("connectionString").trim();
						String connectionAdapterClass = jsonAction.getString("connectionAdapter").trim();
						String userName = jsonAction.getString("userName").trim();
						String password = jsonAction.getString("password");
						
						// if the password wasn't set retrieve it via the connection index
						if ("********".equals(password)) password = dbConns.get(index).getPassword();
						
						// get our class
						Class classClass = Class.forName(connectionAdapterClass);
						// initialise a constructor
						Constructor constructor = classClass.getConstructor(ServletContext.class, String.class, String.class, String.class, String.class);
						// initialise the class
						ConnectionAdapter connectionAdapter = (ConnectionAdapter) constructor.newInstance(rapidServlet.getServletContext(), driverClass, connectionString, userName, password) ;
						
						// get a data factory
						DataFactory dataFactory = new DataFactory(connectionAdapter);
						// get a connection
						Connection connection = dataFactory.getConnection(rapidRequest);
						// close it
						dataFactory.close();
												
						// add the application to the response
						result.put("message", "Database connection OK");
						
						// retain that a connection was found
						foundConnection = true;
						
					}
				}
				
				if (!foundConnection) result.put("message", "Database connection could not be found");
					
			} else if ("DELAPPBACKUP".equals(action)) {
				
				// get the id
				String backupId = jsonAction.getString("backupId");
				
				// get the folder into a file object
				File backup = new File (app.getBackupFolder(rapidServlet.getServletContext(), false) + "/" + backupId);
				// delete it
				Files.deleteRecurring(backup);
				
				// set the result message
				result.put("message", "Application backup " + appId + "/" + appVersion + "/" + backupId + " deleted");
				// pass back a control id from in the dialogue with which to close it
				result.put("controlId", "#rapid_P12_C13_");
													
			} else if ("DELPAGEBACKUP".equals(action)) {
				
				// get the id
				String backupId = jsonAction.getString("backupId");
				
				// get the folder into a file object
				File backup = new File (app.getBackupFolder(rapidServlet.getServletContext(), false) + "/" + backupId);
				// delete it
				Files.deleteRecurring(backup);
				
				// set the result message
				result.put("message", "Page backup " + appId + "/" + backupId + " deleted");
				// pass back a control id from in  the dialogue with which to close it
				result.put("controlId", "#rapid_P13_C13_");
				
			} else if ("RESTOREAPPBACKUP".equals(action)) {
				
				// get the id
				String backupId = jsonAction.getString("backupId");
				
				// get this backup folder
				File backupFolder = new File(app.getBackupFolder(rapidServlet.getServletContext(), false) + "/" + backupId);
				
				// check it exists
				if (backupFolder.exists()) {
					
					// back up the current state of the application
					app.backup(rapidServlet, rapidRequest, false);
					
					
					// get the config folder
					File configFolder = new File(app.getConfigFolder(rapidServlet.getServletContext()));
					
					// get the web folder
					File webFolder = new File(app.getWebFolder(rapidServlet.getServletContext()));
					
					// get the backups folder
					File backupsFolder = new File(app.getBackupFolder(rapidServlet.getServletContext(), false));
					
															
					
					// create a file object for restoring the config folder
				 	File configRestoreFolder = new File(Application.getConfigFolder(rapidServlet.getServletContext(), app.getId(), "_restore"));
				 	
				 	List<String> ignoreList = new ArrayList<String>();
				 	ignoreList.add("WebContent");
				 	
				 	// copy the backup into the application restore folder
					Files.copyFolder(backupFolder, configRestoreFolder, ignoreList);
				 			 					
				 				 	
				 				 				 	
				 	// create a file object for the web content backup folder (which is currently sitting under the application)
					File webBackupFolder = new File(backupFolder + "/WebContent");
					
					// create a file object for the web content restore folder
					File webRestoreFolder = new File(Application.getWebFolder(rapidServlet.getServletContext(), app.getId(), "_restore"));
					
					// copy the web contents backup folder to the webcontent restore folder
					Files.copyFolder(webBackupFolder, webRestoreFolder);
					
									
					
					// get the backups destination folder
					File backupsRestoreFolder = new File(Application.getBackupFolder(rapidServlet.getServletContext(), app.getId(), "_restore", false));
					
					// copy in the backups
					Files.copyFolder(backupsFolder, backupsRestoreFolder);

					
					
					// delete the application config folder (this removes the webcontent and backups too so we do it here)
				 	Files.deleteRecurring(configFolder);
				 	
				 	// rename the restore folder to the application folder
				 	configRestoreFolder.renameTo(configFolder);
					
					
					// delete the webcontent folder
					Files.deleteRecurring(webFolder);
					
					// rename the restore folder to the webconten folder
					webRestoreFolder.renameTo(webFolder);
					

									
					// get the application file
					File applicationFile = new File(configFolder + "/application.xml");
					
					// reload the application
					app = Application.load(rapidServlet.getServletContext(), applicationFile);
					
					// add it back to the collection
					rapidServlet.getApplications().put(app);
					

					// set the result message
					result.put("message", "Application " + backupId + " restored");
					// pass back a control id from in  the dialogue with which to close it
					result.put("controlId", "#rapid_P14_C13_");
										
				} else {
					
					// set the result message
					result.put("message", "Application backup " + backupId + " not found");
					
				}
														
				
								
			} else if ("RESTOREPAGEBACKUP".equals(action)) {
				
				// get the id
				String backupId = jsonAction.getString("backupId");
				
				// turn the id into parts
				String[] idParts = backupId.split("_");
				
				// start the page name
				String pageName = idParts[0];
				// loop the remaining parts and build
				for (int i = 1; i < idParts.length - 3; i++) {
					pageName += "_" + idParts[i];
				}
				
				// get the page
				Page page = app.getPages().getPageByName(rapidServlet.getServletContext(), pageName);
				
				// create a file object for the page
			 	File pageFile = new File(page.getFile(rapidServlet.getServletContext(), app));
				
			 	// create a backup for the current state
				page.backup(rapidServlet, rapidRequest, app, pageFile, false);
				
				// get this backup file
				File backupFile = new File(app.getBackupFolder(rapidServlet.getServletContext(), false) + "/" + backupId);
				
				// copy it over the current page file
				Files.copyFile(backupFile, pageFile);
				
				// load the page from the backup 
				page = Page.load(rapidServlet.getServletContext(), backupFile);
				
				// replace the current entry
				app.getPages().addPage(page, pageFile);
									
				// set the result message
				result.put("message", "Page backup " + appId + "/" + backupId + " restored");
				// pass back a control id from in  the dialogue with which to close it
				result.put("controlId", "#rapid_P15_C13_");
								
			} else if ("SAVEAPPBACKUPSIZE".equals(action)) {
								
				// get the max backup size
				int backupMaxSize = jsonAction.getInt("backupMaxSize");
				
				// pass it to the application
				app.setApplicationBackupMaxSize(backupMaxSize);
									
				// save the application
				app.save(rapidServlet, rapidRequest, false);
				
				// set the result message
				result.put("message", "Application backup max size updated to " + backupMaxSize);
					
			} else if ("SAVEPAGEBACKUPSIZE".equals(action)) {
				
				// get the max backup size
				int backupMaxSize = jsonAction.getInt("backupMaxSize");
				
				// pass it to the application
				app.setPageBackupsMaxSize(backupMaxSize);
				
				// save the application
				app.save(rapidServlet, rapidRequest, false);
				
				// set the result message
				result.put("message", "Page backup max size updated to " + backupMaxSize);
									
			}
								
			// sent back the new app id for the callback load
			if (newAppId != null) result.put("id", newAppId);
			
		} else {
			
			// send back an error
			result.put("error", "Application not found");
			
		}
		
		return result;
		
	}
	
}
