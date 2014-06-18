package com.rapid.actions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.security.SecurityAdapater;
import com.rapid.security.SecurityAdapater.Role;
import com.rapid.security.SecurityAdapater.SecurityAdapaterException;
import com.rapid.security.SecurityAdapater.User;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidServletContextListener;
import com.rapid.server.RapidHttpServlet.RapidRequest;
import com.rapid.server.filter.RapidFilter;
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
	
	public Rapid() {}	
	public Rapid(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws JSONException { 
		
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for query
			if (!"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		} 
		
		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some
		if (jsonSuccessActions != null) {
			// instantiate our contols collection
			try {
				_successActions = Control.getActions(rapidServlet, jsonSuccessActions);
			} catch (Exception ex) {
				// rethrow as a JSON error
				throw new JSONException(ex);
			}
		}
		
		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some
		if (jsonErrorActions != null) {
			// instantiate our contols collection
			try {
				_errorActions = Control.getActions(rapidServlet, jsonErrorActions);
			} catch (Exception ex) {
				// rethrow as a JSON error
				throw new JSONException(ex);
			}
		}
						
	}	
	
	// internal method
	
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
	public String getJavaScript(Application application, Page page, Control control) {
		
		// the javascript we're about to build
		String js = "";
		
		// write success actions variable
		js += "  var successCallback = function(data) {\n";
		// check success actions
		if (_successActions != null) {
			if (_successActions.size() > 0) {
				for (Action action : _successActions) {
					js += "    " + action.getJavaScript(application, page, control).trim().replace("\n", "\n    ") + "\n";
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
					js += "    " + action.getJavaScript(application, page, control).trim().replace("\n", "\n    ") + "\n";
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
		js += "  Action_rapid(ev, '" + application.getId() + "','" + page.getId() + "'," + controlId + ",'" + getId() + "','" + getProperty("actionType") + "', successCallback, errorCallback);";
		
		return js;
	}
	
	@Override
	public JSONObject doAction(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, JSONObject jsonAction) throws JSONException, JAXBException, IOException {
		
		JSONObject result = new JSONObject();
		
		String action = jsonAction.getString("actionType");
		
		String newAppId = null;
		
		// get the if of the app we're about to manipulate
		String appId = jsonAction.getString("appId");	
		// get the application we're about to manipulate
		Application app = rapidServlet.getApplication(appId);
		
		// only if we had an application
		if (app != null) {
			
			// recreate the rapid request using the application we wish to manipulate
			RapidRequest rapidActionRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app); 
			
			// check the action
			if ("GETAPPS".equals(action)) {
				
				// prepare a json array we're going to include in the result
				JSONArray jsonApplications = new JSONArray();
				// fetch all the applications
				ArrayList<Application> applications = rapidServlet.getSortedApplications();
				
				// get the userName
				String userName = rapidRequest.getUserName();
				if (userName == null) userName = "";
				
				// loop the applications
				for (Application application : applications) {
					
					// get the id of this application
					String applicationId = application.getId();
					
					// try and check the permission
					try {
						
						// check permission for the user against the RapidAdmin role
						if (application.getSecurity().checkUserRole(rapidRequest, userName, "RapidAdmin")) {
							JSONObject jsonApplication = new JSONObject();
							jsonApplication.put("id", applicationId);
							jsonApplication.put("title", application.getTitle());
							jsonApplications.put(jsonApplication);
						}
						
					} catch (SecurityAdapaterException ex) {
						rapidServlet.getLogger().error("Rapid action error, failed to get permission : " + ex.getMessage(),ex);
					}
									
				}
				
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
								
				// add the applications to the result
				result.put("applications", jsonApplications);
												
			} else if ("GETAPP".equals(action)) {
							
				// add the name
				result.put("name", app.getName());
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
				List<Page> pages = app.getSortedPages();
				// check we have some
				if (pages != null) {
					for (Page page : pages) {
						JSONObject jsonPage = new JSONObject();						
						jsonPage.put("text", page.getTitle());
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
				
				
				// create an array for the app backups
				JSONArray jsonAppBackups = new JSONArray();
				
				// check we have some app backups
				if (app.getApplicationBackups(rapidServlet) != null) {
					// loop and add to jsonArray
					for (Application.Backup appBackup : app.getApplicationBackups(rapidServlet)) {
						// create the backup json object
						JSONObject jsonBackup = new JSONObject();
						// create a date formatter
						SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
						// populate it
						jsonBackup.append("id", appBackup.getId());
						jsonBackup.append("date", df.format(appBackup.getDate()));
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
						// create a date formatter
						SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
						// populate it
						jsonBackup.append("id", appBackup.getId());
						jsonBackup.append("page", appBackup.getName());
						jsonBackup.append("date", df.format(appBackup.getDate()));
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
				
				// get the app security
				SecurityAdapater security = app.getSecurity();
				
				// if we got one
				if (security != null) {
					
					try {
												
						// add the roles to the response
						result.put("roles", security.getRoles(rapidRequest));
						
						// add the users to the response
						result.put("users", security.getUsers(rapidRequest));					
						
					} catch (SecurityAdapaterException ex) {
						rapidServlet.getLogger().error("Rapid security adapter error : " + ex.getMessage(),ex);
						throw new JSONException(ex);
					}
														
				} // got security
				
			} else if ("GETUSER".equals(action)) { 
										
				try {
					
					// get the userName from the incoming json
					String userName = jsonAction.getString("userName");
					
					// get the app security
					SecurityAdapater security = app.getSecurity();
					
					// get the user
					User user = security.getUser(rapidRequest, userName);
					
					// add the user name
					result.put("userName", userName);
					
					// add the user description
					result.put("description", user.getDescription());
					
					// add a masked password
					result.put("password", "********");
				
					// if we got one
					if (security != null) {
										
						// get the users roles
						List<String> roles = security.getUser(rapidRequest, userName).getRoles();
						
						// add the users to the response
						result.put("roles", roles);											
													
					} // got security
					
				} catch (SecurityAdapaterException ex) {
					rapidServlet.getLogger().error("Rapid action error : " + ex.getMessage(),ex);
					throw new JSONException(ex);
				}
								
			} else if ("RELOADACTIONS".equals(action)) {
							
				// load actions and set the result message
				try { result.put("message", RapidServletContextListener.loadActions(rapidServlet.getServletContext()) + " actions reloaded"); } 
				catch (Exception ex) { throw new JSONException(ex);	}
				
			} else if ("RELOADCONTROLS".equals(action)) {
				
				// load controls and set the result message
				try { result.put("message", RapidServletContextListener.loadControls(rapidServlet.getServletContext()) + " controls reloaded"); } 
				catch (Exception ex) { throw new JSONException(ex);	}
				
			} else if ("RELOADAPPLICATIONS".equals(action)) {
				
				// load applications and set the result message
				try { result.put("message", RapidServletContextListener.loadApplications(rapidServlet.getServletContext()) + " applications reloaded"); } 
				catch (Exception ex) { throw new JSONException(ex);	}
				
			} else if ("RELOADADAPTERS".equals(action)) {
								
				// load adapters and set the result message
				int databaseDrivers = 0;
				int connectionAdapters = 0;
				int securityAdapters = 0;
				
				try { 
					
					databaseDrivers = RapidServletContextListener.loadDatabaseDrivers(rapidServlet.getServletContext());
					
					connectionAdapters = RapidServletContextListener.loadConnectionAdapters(rapidServlet.getServletContext());
					
					securityAdapters = RapidServletContextListener.loadSecurityAdapters(rapidServlet.getServletContext());
										
					result.put("message", databaseDrivers + " database drivers, " + connectionAdapters + " connection adapters, " + securityAdapters + " security adapters");
					
				} catch (Exception ex) { throw new JSONException(ex); }
								
			} else if ("SAVEAPP".equals(action)) {
			
				// get the new values
				String name = jsonAction.getString("name");
				String title = jsonAction.getString("title");
				String description = jsonAction.getString("description");
				boolean showControlIds = jsonAction.optBoolean("showControlIds");
				boolean showActionIds = jsonAction.optBoolean("showActionIds");
				String startPageId = jsonAction.optString("startPageId","");
				
				String id = Files.safeName(name).toLowerCase();
				
				// if the name is now different the derived id will be too
				if (!app.getName().equals(name)) {
					// archive the app as is
					app.backup(rapidServlet, rapidRequest);
					// copy the app to the id location
					app.copy(rapidServlet, rapidRequest, id);
					// delete it
					app.delete(rapidServlet, rapidRequest);
				}
				
				// update the values
				app.setName(name);
				app.setId(id);
				app.setTitle(title);
				app.setDescription(description);
				app.setShowControlIds(showControlIds);
				app.setShowActionIds(showActionIds);
				app.setStartPageId(startPageId);
				
				// save
				try { app.save(rapidServlet, rapidRequest);	} 
				catch (Exception ex) { throw new JSONException(ex);	} 
				
				// make sure it's in the collection of apps (may have been moved out if the name was changed)
				rapidServlet.getApplications().put(app.getId(), app);
							
				// add the application to the response
				result.put("message", "Application details saved");
				
			} else if ("SAVESTYLES".equals(action)) {
							
				String styles = jsonAction.getString("styles");
				
				app.setStyles(styles);
				
				try { app.save(rapidServlet, rapidRequest);	} 
				catch (Exception ex) { throw new JSONException(ex);	}
				
				// add the application to the response
				result.put("message", "Styles saved");
				
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
						
						// save the app
						try { app.save(rapidServlet, rapidRequest);	} 
						catch (Exception ex) { throw new JSONException(ex);	}
						
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
							try { app.save(rapidServlet, rapidRequest);	} 
							catch (Exception ex) { throw new JSONException(ex);	}
							
							foundWebservice = true;
							
							// add the application to the response
							result.put("message", "SQL webservice saved");
						}	
					}
				}
				
				if (!foundWebservice) result.put("message", "SQL webservice could not be found");
				
			} else if ("SAVESECURITYADAPT".equals(action)) { 
				
				String securityAdapter = jsonAction.getString("securityAdapter").trim();
				
				app.setSecurityAdapterType(securityAdapter);
				
				try { app.save(rapidServlet, rapidRequest);	} 
				catch (Exception ex) { throw new JSONException(ex);	}
				
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
				try { app.save(rapidServlet, rapidRequest);	} 
				catch (Exception ex) { throw new JSONException(ex);	}
				
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
				try { app.save(rapidServlet, rapidRequest);	} 
				catch (Exception ex) { throw new JSONException(ex);	}
				
				// add the message to the response
				result.put("message", controlTypes.size() + " controls");
				
			} else if ("REBUILDPAGES".equals(action)) {
			
				int pages = app.rebuildPages(rapidServlet, rapidRequest);
								
				// add the application to the response
				result.put("message", pages + " pages rebuilt");
				
			} else if ("NEWAPP".equals(action)) {
				
				String name = jsonAction.getString("name").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();
				
				// derive the new app id based on making the name safe and in lower case
				newAppId = Files.safeName(name).toLowerCase();
				
				// instantiate a new application
				Application newApp = new Application();
				// populate the bare-minimum of properties
				newApp.setId(newAppId);
				newApp.setName(name);
				newApp.setTitle(title);
				newApp.setDescription(description);
				newApp.setCreatedBy(rapidRequest.getUserName());
				newApp.setCreatedDate(new Date());
				newApp.setSecurityAdapterType("rapid");
								 				
				try {
					
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
					
					// get the security 
					SecurityAdapater security = newApp.getSecurity();
					
					// check there is one
					if (security != null) {
						
						// get the current user's name
						String userName = rapidRequest.getUserName();
						
						// get the current users record from the adapter
						User user = security.getUser(rapidRequest, userName);
						
						// get the rapid application
						Application rapidApplication = rapidRequest.getRapidServlet().getApplication("rapid");
						
						// check the current user is present in the app's security adapter
						if (user == null) {
							// get the Rapid user object
							User rapidUser = rapidApplication.getSecurity().getUser(rapidRequest, userName);
							// create a new user based on the Rapid user
							user = new User(userName, rapidUser.getDescription(), rapidUser.getPassword());
							// add the new user 
							security.addUser(rapidRequest, user);
						}
						
						// add Admin and Design roles for the new user if required
						if (!security.checkUserRole(rapidRequest, userName, com.rapid.server.Rapid.ADMIN_ROLE)) security.addUserRole(rapidRequest, userName, com.rapid.server.Rapid.ADMIN_ROLE);
						if (!security.checkUserRole(rapidRequest, userName, com.rapid.server.Rapid.DESIGN_ROLE)) security.addUserRole(rapidRequest, userName, com.rapid.server.Rapid.DESIGN_ROLE);
					}
					
					// save the application to file
					newApp.save(rapidServlet, rapidRequest);
					
				} catch (Exception ex) {
					Logger.getLogger(Rapid.class).error(ex);
					throw new JSONException(ex);
				}
								
				// set the result message
				result.put("message", "Application " + app.getTitle() + " created");
				
				// set the result appId
				result.put("appId", newApp.getId());
				
			} else if ("DELAPP".equals(action)) {
						
				// delete the application
				if (app != null) app.delete(rapidServlet, rapidActionRequest);
				// set the result message
				result.put("message", "Application " + app.getName() + " deleted");
				
			} else if ("DUPAPP".equals(action)) {
				
				String name = jsonAction.getString("name").trim();
				String title = jsonAction.optString("title").trim();
				String description = jsonAction.optString("description").trim();
				
				// derive the new app id based on making the name safe and in lower case
				newAppId = Files.safeName(name).toLowerCase();
																				 				
				try {
					
					// create a list of files to ignore
					List<String> ignoreFiles = new ArrayList<String>();
					ignoreFiles.add(com.rapid.server.Rapid.BACKUP_FOLDER);
					
					// copy any webcontent
					File oldFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + app.getId()));					
					File newFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + newAppId));					
					Files.copyFolder(oldFolder, newFolder, ignoreFiles);
					
					// copy application pages
					oldFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + app.getId()));					
					newFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + newAppId));					
					Files.copyFolder(oldFolder, newFolder, ignoreFiles);
															
					// look for page files
					File pagesFolder = new File(newFolder.getAbsolutePath() + "/pages");
					if (pagesFolder.exists()) {
						
						// create a filter for finding .page.xml files
						FilenameFilter xmlFilenameFilter = new FilenameFilter() {
					    	public boolean accept(File dir, String name) {
					    		return name.toLowerCase().endsWith(".page.xml");
					    	}
					    };
					    
					    // loop the .page.xml files 
					    for (File pageFile : pagesFolder.listFiles(xmlFilenameFilter)) {
					    	
					    	BufferedReader reader = new BufferedReader(new FileReader(pageFile));
					        String line = null;
					        StringBuilder stringBuilder = new StringBuilder();
					        
					        while ((line = reader.readLine()) != null ) {
					            stringBuilder.append(line);
					            stringBuilder.append("\n");
					        }
					        reader.close();
					        
					        // retrieve the xml into a string
					        String fileString = stringBuilder.toString();

					        // replace all properties that appear to have a url, and all created links
					        String newFileString = fileString
					        		.replace("applications/" + app.getId() + "/", "applications/" + newAppId + "/")
					        		.replace("~?a=" + app.getId() + "&amp;", "~?a=" + newAppId + "&amp;");
					        					        
					        PrintWriter newFileWriter = new PrintWriter(pageFile);
					        newFileWriter.print(newFileString);
					        newFileWriter.close();
					        								    	
					    }
						
					}
					
					// look for an archive folder
					File archiveFolder = new File(newFolder.getAbsolutePath() + "/" + com.rapid.server.Rapid.BACKUP_FOLDER);
					// delete the archive folder if present
					if (archiveFolder.exists()) Files.deleteRecurring(archiveFolder);
										
					// load the new application (but do not regenerate the resources files - this happens on the save)
					Application newApp = Application.load(rapidServlet.getServletContext(), new File(newFolder.getAbsolutePath() + "/application.xml"), false);
					
					// overwrite the properties
					newApp.setId(newAppId);
					newApp.setName(name);
					newApp.setTitle(title);
					newApp.setDescription(description);

					// save the application to file
					newApp.save(rapidServlet, rapidRequest);
															
				} catch (Exception ex) {
					Logger.getLogger(Rapid.class).error(ex);
					throw new JSONException(ex);
				}
								
				// set the result message
				result.put("message", "Application " + app.getTitle() + " duplicated");
				
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
				newPage.save(rapidServlet, rapidActionRequest);
				
				// put the id in the result
				result.put("id", id);
				
				// set the result message
				result.put("message", "Page " + newPage.getTitle() + " created");
				
			} else if ("DELPAGE".equals(action)) {
				
				// get the id
				String id = jsonAction.getString("id").trim();			
				// retrieve the page
				Page delPage = app.getPage(id);
				// delete it if we got one
				if (delPage != null) delPage.delete(rapidServlet, rapidActionRequest);
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
				try { app.save(rapidServlet, rapidRequest);	} 
				catch (Exception ex) { throw new JSONException(ex);	}				
				
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
						try { app.save(rapidServlet, rapidRequest);	} 
						catch (Exception ex) { throw new JSONException(ex);	}
						
						// add the application to the response
						result.put("message", "Database connection deleted");
						
					}
				}
				
				if (!foundConnection) result.put("message", "Database connection could not be found");
				
			} else if ("NEWSOA".equals(action)) {
				
				// get the webservices
				List<Webservice> webservices = app.getWebservices();
				
				// make the new database connection
				Webservice webservice = new SQLWebservice(
					jsonAction.getString("name").trim()
				); 
				
				// add it to the collection
				webservices.add(webservice);
				
				// save the app
				try { app.save(rapidServlet, rapidRequest);	} 
				catch (Exception ex) { throw new JSONException(ex);	}				
				
				// add the application to the response
				result.put("message", "SOA webservice added");
												
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
						try { app.save(rapidServlet, rapidRequest);	} 
						catch (Exception ex) { throw new JSONException(ex);	}
						
						// add the application to the response
						result.put("message", "SOA webservice deleted");
						
					}
				}
				
				if (!foundWebservice) result.put("message", "SOA webservice could not be found");
				
			} else if ("NEWROLE".equals(action)) {
				
				try {
					
					// get the role name
					String roleName = jsonAction.getString("role").trim();
					// get the role descrition
					String description = jsonAction.getString("description").trim();
					
					// add the role
					app.getSecurity().addRole(rapidRequest, new Role(roleName, description));
					// set the result message
					result.put("message", "Role added");
					
				} catch (SecurityAdapaterException ex) {
					throw new JSONException(ex);
				}
								
			} else if ("DELROLE".equals(action)) {
				
				try {
					
					// get the role
					String role = jsonAction.getString("role").trim();
					// delete the role
					app.getSecurity().deleteRole(rapidRequest, role);
					// set the result message
					result.put("message", "Role deleted");
					
				} catch (SecurityAdapaterException ex) {
					throw new JSONException(ex);
				}
								
			} else if ("NEWUSER".equals(action)) {
				
				try {
					
					// get the userName
					String userName = jsonAction.getString("userName").trim();
					// get the userDescription
					String description = jsonAction.optString("description","").trim();
					// get the password
					String password = jsonAction.getString("password");
					// add the role
					app.getSecurity().addUser(rapidRequest, new User(userName, description, password));
					// set the result message
					result.put("message", "User added");
					
				} catch (SecurityAdapaterException ex) {
					throw new JSONException(ex);
				}
								
			} else if ("DELUSER".equals(action)) {
				
				try {
					
					// get the userName
					String userName = jsonAction.getString("userName").trim();
					// delete the user
					app.getSecurity().deleteUser(rapidRequest, userName);
					// set the result message
					result.put("message", "User deleted");
					
				} catch (SecurityAdapaterException ex) {
					throw new JSONException(ex);
				}
								
			} else if ("NEWUSERROLE".equals(action)) {
				
				try {
					
					// get the userName
					String userName = jsonAction.getString("userName").trim();
					// get the role
					String role = jsonAction.getString("role").trim();
					// add the user role
					app.getSecurity().addUserRole(rapidRequest, userName, role);
					// set the result message
					result.put("message", "User role added");
					
				} catch (SecurityAdapaterException ex) {
					throw new JSONException(ex);
				}
								
			} else if ("DELUSERROLE".equals(action)) {
				
				try {
					
					// get the userName
					String userName = jsonAction.getString("userName").trim();
					// get the role
					String role = jsonAction.getString("role").trim();
					// add the user role
					app.getSecurity().deleteUserRole(rapidRequest, userName, role);
					// set the result message
					result.put("message", "User role deleted");
					
				} catch (SecurityAdapaterException ex) {
					throw new JSONException(ex);
				}
								
			} else if ("SAVEUSER".equals(action)) {
				
				try {
					
					// get the userName
					String userName = jsonAction.getString("userName").trim();
					// get the description
					String description = jsonAction.getString("description").trim();
					// get the password
					String password = jsonAction.getString("password");
					// get the user
					User user = app.getSecurity().getUser(rapidRequest, userName);
					// update the description
					user.setDescription(description);
					// update the password if different from the mask
					if (!"********".equals(password)) user.setPassword(password);
					// update the user
					app.getSecurity().updateUser(rapidRequest, user);
					// set the result message
					result.put("message", "User saved");
					
				} catch (SecurityAdapaterException ex) {
					throw new JSONException(ex);
				}
								
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
						
						try {
							
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
							
						} catch (Exception ex) { throw new JSONException(ex); }
												
						// add the application to the response
						result.put("message", "Database connection OK");
						
					}
				}
				
				if (!foundConnection) result.put("message", "Database connection could not be found");
					
			} else if ("DELAPPBACKUP".equals(action)) {
				
				try {
					
					// get the id
					String backupId = jsonAction.getString("backupId");
					
					// delete the backup
					Application.deleteBackup(rapidServlet, backupId);
					
					// set the result message
					result.put("message", "Application backup " + appId + "/" + backupId + " deleted");
					// pass back a control id from in  the dialogue with which to close it
					result.put("controlId", "#rapid_P12_C13_");
					
				} catch (Exception ex) {
					throw new JSONException(ex);
				}
								
			} else if ("DELPAGEBACKUP".equals(action)) {
				
				try {
					
					// get the id
					String backupId = jsonAction.getString("backupId");
					
					// delete the backup
					Page.deleteBackup(rapidServlet, appId, backupId);
					
					// set the result message
					result.put("message", "Page backup " + appId + "/" + backupId + " deleted");
					// pass back a control id from in  the dialogue with which to close it
					result.put("controlId", "#rapid_P13_C13_");
					
				} catch (Exception ex) {
					throw new JSONException(ex);
				}
								
			} else if ("RESTOREAPPBACKUP".equals(action)) {
				
				try {
					
					// get the id
					String backupId = jsonAction.getString("backupId");
															
					// back up the current state of the application
					app.backup(rapidServlet, rapidRequest);
					
					
					// get this backup folder
					File applicationBackupFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + com.rapid.server.Rapid.BACKUP_FOLDER + "/" + backupId));
					
					// create a file object for restoring the application folder
				 	File applicationRestoreFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/_" + app.getId() + "_restore"));
				 	
				 	// copy the backup into the application reatore folder
					Files.copyFolder(applicationBackupFolder, applicationRestoreFolder);
					
				 	// create a file object for the application folder
				 	File applicationFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + app.getId()));
					
				 	// delete the application folder
				 	Files.deleteRecurring(applicationFolder);
				 	
				 	// rename the restore folder to the application folder
				 	applicationRestoreFolder.renameTo(applicationFolder);
				 	
				 	
				 	// create a file object for the web content backup folder (which is currently sitting under the application)
					File webcontentBackupFolder = new File(applicationFolder.getAbsolutePath() + "/WebContent");
					
					// create a file object for the web content restore folder
					File webcontentRestoreFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/_") + app.getId() + "_restore");
					
					// copy the webcontent backup folder to the webcontent restore folder
					Files.copyFolder(webcontentBackupFolder, webcontentRestoreFolder);
					
					// create a file object for the webcontent folder
					File webcontentFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + app.getId()));
					
					// delete the webcontent folder
					Files.deleteRecurring(webcontentFolder);
					
					// rename the restore folder to the webconten folder
					webcontentRestoreFolder.renameTo(webcontentFolder);
					
					// delete the webcontent backup folder from under the application folder
					Files.deleteRecurring(webcontentBackupFolder);
					
					
					// get the application file
					File applicationFile = new File(applicationFolder.getAbsolutePath() + "/application.xml");
					
					// reload the application
					app = Application.load(rapidServlet.getServletContext(), applicationFile);
					
					// add it back to the collection
					rapidServlet.getApplications().put(app.getId(), app);
					
					// set the result message
					result.put("message", "Application backup " + appId + "/" + backupId + " restored");
					// pass back a control id from in  the dialogue with which to close it
					result.put("controlId", "#rapid_P14_C13_");
					
				} catch (Exception ex) {
					throw new JSONException(ex);
				}
								
			} else if ("RESTOREPAGEBACKUP".equals(action)) {
				
				try {
					
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
					Page page = app.getPageByName(pageName);
					
					// get the page path
					String pagePath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + app.getId() + "/pages");
					
					// create a file object for the page
				 	File pageFile = new File(pagePath + "/" + Files.safeName(page.getName()) + ".page.xml");
					
				 	// create a backup for the current state
					page.backup(rapidServlet, rapidRequest, pageFile);
					
					// get this backup file
					File backupFile = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + app.getId() + "/" + com.rapid.server.Rapid.BACKUP_FOLDER + "/" + backupId));
					
					// copy it over the current page file
					Files.copyFile(backupFile, pageFile);
					
					// load the page from the backup 
					page = Page.load(rapidServlet.getServletContext(), backupFile);
					
					// replace the current entry
					app.addPage(page);
										
					// set the result message
					result.put("message", "Page backup " + appId + "/" + backupId + " restored");
					// pass back a control id from in  the dialogue with which to close it
					result.put("controlId", "#rapid_P15_C13_");
					
				} catch (Exception ex) {
					throw new JSONException(ex);
				}
								
			} else if ("SAVEAPPBACKUPSIZE".equals(action)) {
								
				try {
					
					// get the max backup size
					int backupMaxSize = jsonAction.getInt("backupMaxSize");
					
					// pass it to the application
					app.setApplicationBackupMaxSize(backupMaxSize);
										
					// save the application
					app.save(rapidServlet, rapidRequest);
					
					// set the result message
					result.put("message", "Application backup max size updated to " + backupMaxSize);
					
				} catch (Exception ex) {
					throw new JSONException(ex);
				}
				
			} else if ("SAVEPAGEBACKUPSIZE".equals(action)) {
				
				try {
					
					// get the max backup size
					int backupMaxSize = jsonAction.getInt("backupMaxSize");
					
					// pass it to the application
					app.setPageBackupMaxSize(backupMaxSize);
					
					// save the application
					app.save(rapidServlet, rapidRequest);
					
					// set the result message
					result.put("message", "Page backup max size updated to " + backupMaxSize);
					
				} catch (Exception ex) {
					throw new JSONException(ex);
				}
				
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
