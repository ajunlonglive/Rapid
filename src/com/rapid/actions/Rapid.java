/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

package com.rapid.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.Parameter;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Application.Resource;
import com.rapid.core.Application.Resources;
import com.rapid.core.Applications;
import com.rapid.core.Applications.Versions;
import com.rapid.core.Control;
import com.rapid.core.Device;
import com.rapid.core.Device.Devices;
import com.rapid.core.Email;
import com.rapid.core.Page;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Process;
import com.rapid.core.Settings;
import com.rapid.core.Theme;
import com.rapid.core.Workflow;
import com.rapid.core.Workflows;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DatabaseConnection;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.Role;
import com.rapid.security.SecurityAdapter.Roles;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.security.SecurityAdapter.UserRoles;
import com.rapid.security.SecurityAdapter.Users;
import com.rapid.server.Monitor;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.server.RapidServletContextListener;
import com.rapid.server.RapidSessionListener;
import com.rapid.server.filter.RapidFilter;
import com.rapid.soa.JavaWebservice;
import com.rapid.soa.SOAElementRestriction;
import com.rapid.soa.SOAElementRestriction.EnumerationRestriction;
import com.rapid.soa.SOAElementRestriction.MaxLengthRestriction;
import com.rapid.soa.SOAElementRestriction.MaxOccursRestriction;
import com.rapid.soa.SOAElementRestriction.MinLengthRestriction;
import com.rapid.soa.SOAElementRestriction.MinOccursRestriction;
import com.rapid.soa.SOASchema;
import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.soa.SQLWebservice;
import com.rapid.soa.Webservice;
import com.rapid.utils.Comparators;
import com.rapid.utils.Files;
import com.rapid.utils.XML.XMLAttribute;
import com.rapid.utils.XML.XMLGroup;
import com.rapid.utils.XML.XMLValue;

public class Rapid extends Action {

	// private static finals
	private static final String TEST_EMAIL_TO = "test@dev.rapid-is.co.uk";
	private static final String TEST_EMAIL_FROM = "test@dev.rapid-is.co.uk";

	// static variables
	private static Logger _logger = LogManager.getLogger(Rapid.class);

	// instance variables

	private List<Action> _successActions, _errorActions, _childActions;

	// properties

	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }

	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }

	// enterprise monitor
	protected Monitor _monitor = new Monitor();

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

	private Application createApplication(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String name, String version, String title, String type, boolean responsive, String themeType, String description) throws IllegalArgumentException, SecurityException, JAXBException, IOException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, SecurityAdapaterException, ParserConfigurationException, XPathExpressionException, RapidLoadingException, SAXException, NoSuchAlgorithmException {

		String newAppId = Files.safeName(name).toLowerCase();
		String newAppVersion = Files.safeName(version);

		Application newApp = new Application();

		// populate the bare-minimum of properties
		newApp.setId(newAppId);
		newApp.setVersion(newAppVersion);
		newApp.setName(name);
		newApp.setTitle(title);
		newApp.setThemeType(themeType);
		newApp.setDescription(description);
		newApp.setCreatedBy(rapidRequest.getUserName());
		newApp.setCreatedDate(new Date());

		// get the context
		ServletContext servletContext = rapidServlet.getServletContext();

		// look for the new app security adapter parameter
		String newAppSecurityAdapter = servletContext.getInitParameter("newapp.securityAdpter");
		// check we got one
		if (newAppSecurityAdapter == null) {
			// no default so go for Rapid
			newApp.setSecurityAdapterType("rapid");
		} else {
			// set the one given
			newApp.setSecurityAdapterType(newAppSecurityAdapter);
		}

		// look for any new app parameters parameter
		String newAppParameters = servletContext.getInitParameter("newapp.parameters");
		// check we got one
		if (newAppParameters != null) {
			// trim it
			newAppParameters = newAppParameters.trim();
			// if it looks like an object
			if (newAppParameters.startsWith("{")) {
				// read to object
				JSONObject jsonNewAppParameterObject = new JSONObject(newAppParameters);
				// get keys iterator
				Iterator<String> keys = jsonNewAppParameterObject.keys();
				// get app parameters
				List<Parameter> parameters = newApp.getParameters();
				// loop keys
				while (keys.hasNext()) {
					// get key
					String key = keys.next();
					// get value
					String value = jsonNewAppParameterObject.getString(key);
					// add to app parameters
					parameters.add(new Parameter(key, value));
				}
			}
		}

		// if this is a form
		if ("F".equals(type)) {
			// set the isForm flag
			newApp.setIsForm(true);
			// add standard form adapter
			newApp.setFormAdapterType("rapid");
			// add form security
			newApp.setSecurityAdapterType("form");
		}

		// initialise the application
		newApp.initialise(servletContext, true);

		// initialise the list of actions
		List<String> actionTypes = new ArrayList<>();

		// get the JSONArray of actions
		JSONArray jsonActionTypes = rapidServlet.getJsonActions();

		// if there were some
		if (jsonActionTypes != null) {
			// loop them
			for (int i = 0; i < jsonActionTypes.length(); i++) {
				// get the action
				JSONObject jsonActionType = jsonActionTypes.getJSONObject(i);
				// get it's type
				String actionType = jsonActionType.getString("type");

				// assume we should not add this action
				boolean addAction = false;

				// get the default add
				boolean addDefault= jsonActionType.optBoolean("addToNewApplications");

				// check the application type, for when to avoid the action if default but explicit app type is false;
				if ("D".equals(type)) {
					// add to list if app type is D and action has addToNewDesktopApplications
					if (jsonActionType.optBoolean("addToNewDesktopApplications", addDefault)) addAction = true;
				} else if ("M".equals(type)) {
					// add to list if app type is M and action has addToNewDesktopApplications
					if (jsonActionType.optBoolean("addToNewMobileApplications", addDefault)) addAction = true;
				} else if ("F".equals(type)) {
					// add to list if app type is F and action has addToNewDesktopApplications
					if (jsonActionType.optBoolean("addToNewFormApplications", addDefault)) addAction = true;
				} else {
					// not a special type so just use the default
					addAction = addDefault;
				}

				// add it if we should
				if (addAction) actionTypes.add(actionType);

			}
		}

		// sort them again, just to be sure
		Collections.sort(actionTypes);

		// assign the list to the application
		newApp.setActionTypes(actionTypes);

		// initialise the list of controls
		List<String> controlTypes = new ArrayList<>();

		// get the JSONArray of controls
		JSONArray jsonControlTypes = rapidServlet.getJsonControls();

		// if there were some
		if (jsonControlTypes != null) {
			// loop them
			for (int i = 0; i < jsonControlTypes.length(); i++) {
				// get the control
				JSONObject jsonControlType = jsonControlTypes.getJSONObject(i);
				// get the type
				String controlType = jsonControlType.getString("type");

				// assume we should not add this control
				boolean addControl = false;

				// get whether to add to new applications by default
				boolean addDefault = jsonControlType.optBoolean("addToNewApplications");
				// if this is a responsive app
				if (responsive) addDefault = jsonControlType.optBoolean("addToNewResponsiveApplications", addDefault);

				// check the application type
				if ("D".equals(type)) {
					// add to list if app type is D and action has addToNewDesktopApplications
					if (jsonControlType.optBoolean("addToNewDesktopApplications", addDefault)) addControl = true;
				} else if ("M".equals(type)) {
					// add to list if app type is M and action has addToNewDesktopApplications
					if (jsonControlType.optBoolean("addToNewMobileApplications", addDefault)) addControl = true;
				} else if ("F".equals(type)) {
					// add to list if app type is F and action has addToNewFormApplications
					if (jsonControlType.optBoolean("addToNewFormApplications", addDefault)) addControl = true;
				} else {
					// not one of the special types use the default
					addControl = addDefault;
				}

				// add he control if we're supposed to
				if (addControl) controlTypes.add(controlType);

			}
		}

		// sort them again, just to be sure
		Collections.sort(controlTypes);

		// assign the list to the application
		newApp.setControlTypes(controlTypes);

		// save the application to file
		newApp.save(rapidServlet, rapidRequest, false);

		// get the security
		SecurityAdapter security = newApp.getSecurityAdapter();

		// check there is one
		if (security != null) {

			// update the request action to stop the default form adapter creating the wrong user
			rapidRequest.setActionName("newapp");

			// get the current user's record from the adapter (a centralised adapter may have them already)
			User user = security.getUser(rapidRequest);

			// if user is null - for example the default
			if (user == null) {
				// get the rapid application
				Application rapidApplication = rapidServlet.getApplications().get("rapid");
				// get the user object from rapid application
				User rapidUser = rapidApplication.getSecurityAdapter().getUser(rapidRequest);
				// create a new user based on the current user
				user = new User(rapidUser);
				// add the new user to the new application
				security.addUser(rapidRequest, user);
			}

			// add Admin and Design roles for the new user if required
			if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE)) security.addUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
			if (!security.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE)) security.addUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
		}

		return newApp;

	}

	private Workflow createWorkflow(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String name, String title, String description) throws IllegalArgumentException, SecurityException, JAXBException, IOException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, SecurityAdapaterException, ParserConfigurationException, XPathExpressionException, RapidLoadingException, SAXException {

		String newId = Files.safeName(name).toLowerCase();

		Workflow newWorkflow = new Workflow();

		// populate the bare-minimum of properties
		newWorkflow.setId(newId);
		newWorkflow.setName(name);
		newWorkflow.setTitle(title);
		newWorkflow.setDescription(description);
		newWorkflow.setCreatedBy(rapidRequest.getUserName());
		newWorkflow.setCreatedDate(new Date());

		// initialise the list of actions
		List<String> actionTypes = new ArrayList<>();

		// get the JSONArray of actions
		JSONArray jsonActionTypes = rapidServlet.getJsonActions();

		// if there were some
		if (jsonActionTypes != null) {
			// loop them
			for (int i = 0; i < jsonActionTypes.length(); i++) {
				// get the action
				JSONObject jsonActionType = jsonActionTypes.getJSONObject(i);
				// add to list if addToNewApplications is set
				if (jsonActionType.optBoolean("canUseWorkflow")) actionTypes.add(jsonActionType.getString("type"));
			}
		}

		// sort them again, just to be sure
		Collections.sort(actionTypes);

		// assign the list to the application
		newWorkflow.setActionTypes(actionTypes);

		// save the application to file
		newWorkflow.save(rapidServlet, rapidRequest, false);

		return newWorkflow;

	}

	private JSONArray getSafeUsersJSON(RapidRequest rapidRequest, SecurityAdapter security, boolean gotRapidAdmin, Users users) throws JSONException, SecurityAdapaterException {
		// prepare a JSON array to send them in
		JSONArray jsonUsers = new JSONArray();
		// loop them
		for (User user : users) {
			// assume user is safe to add
			boolean addUser = true;

			// if we don't have the high-level Rapid Admin role
			if (!gotRapidAdmin) {
				// update the rapidRequest to this user
				rapidRequest.setUserName(user.getName());
				// if they have the admin role we are not allowed to se them
				if (security.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE)) {
					// don't add them
					addUser = false;
				}
			}

			// if user is still safe to add
			if (addUser) {
				// create a JSON object for them
				JSONObject jsonUser = new JSONObject();
				// add the details of this user
				jsonUser.put("name", user.getName());
				jsonUser.put("description", user.getDescription());
				// add the object to the collection
				jsonUsers.put(jsonUser);
			}
		}
		return jsonUsers;
	}

	private List<SOAElementRestriction> getRestrictions(JSONArray jsonRestrictions) throws JSONException {
		// check we have something
		if (jsonRestrictions == null) {
			return null;
		} else {
			// instantiate the list we're making
			List<SOAElementRestriction> restrictions = new ArrayList<>();
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
			_childActions = new ArrayList<>();
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
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		// the javascript we're about to build
		String js = "";

		// if we are getting the versions for an application remove the appId and version parameters from the url so we don't end up with a version from an app we were previously looking at
		if ("GETVERSIONS".equals(getProperty("actionType"))) js += "if ($('#rapid_P0_C43').val() != $.getUrlVar('appId') && window.history && window.history.replaceState) window.history.replaceState(\"rapid\", \"\", \"~?a=rapid\");\n";

		// write success actions variable
		js += "  var successCallback = function(data) {\n";
		// check success actions
		if (_successActions != null) {
			if (_successActions.size() > 0) {
				for (Action action : _successActions) {
					js += "    " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}
			}
		}
		js += "  };\n";

		// write error actions variable
		js += "  var errorCallback = function(server, status, message) {\n";
		// check whether is an error handling routin
		boolean gotErrorHandler = false;
		// check error actions
		if (_errorActions != null) {
			if (_errorActions.size() > 0) {
				gotErrorHandler = true;
				for (Action action : _errorActions) {
					js += "    " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}
			}
		}
		// if there is no error handling routine insert our own
		if (!gotErrorHandler) js += "    alert('Rapid action failed : ' + ((server && server.responseText) || message));\n";
		// close the error handler
		js += "  };\n";

		// assume this action was called from the page and there is no control id nor details
		String controlId = "null";
		if (control != null) controlId = "'" + control.getId() + "'";

		// return the JavaScript
		js += "  Action_rapid(ev, '" + application.getId() + "','" + page.getId() + "'," + controlId + ",'" + getId() + "','" + getProperty("actionType") + "', " + getProperty("rapidApp") + ", successCallback, errorCallback);";

		return js;
	}

	private void recordMonitorEvent(RapidRequest rapidRequest, JSONObject jsonAction, String actionType) throws JSONException {

		// if monitor not alive or we are not recording everything then do nothing
		if(_monitor==null || !_monitor.isAlive(rapidRequest.getRapidServlet().getServletContext()) || !_monitor.isLoggingAll())
			return;

		// if the action is not one of the types being recorded then do nothing
		if(!"NEWAPP".equals(actionType) && !"DELAPP".equals(actionType) || !"DUPAPP".equals(actionType) || !"GETAPPS".equals(actionType))
			return;

		// get the data required for recording the event
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();
		String appId = jsonAction.getString("appId");
		String appVersion = jsonAction.optString("version", null);
		Application app = rapidServlet.getApplications().get(appId, appVersion);

		// record the event
		if("NEWAPP".equals(actionType)) {
			recordMonitorEvent(rapidRequest, actionType, jsonAction.getString("name").trim(), jsonAction.getString("newVersion").trim());
		} else if("DELAPP".equals(actionType)) {
			recordMonitorEvent(rapidRequest, actionType, app.getName(), app.getVersion());
		} else if("DUPAPP".equals(actionType)) {
			recordMonitorEvent(rapidRequest, actionType, app.getName(), jsonAction.getString("newVersion").trim());
		} else if("GETAPPS".equals(actionType)) {
			recordMonitorEventGetApps(rapidRequest, rapidServlet);
		}
	}

	private void recordMonitorEventGetApps(RapidRequest rapidRequest, RapidHttpServlet rapidServlet) {
		for (String id : rapidServlet.getApplications().getIds()) {
			for (String version : rapidServlet.getApplications().getVersions(id).keySet()) {
				Application application = rapidServlet.getApplications().get(id, version);
				recordMonitorEvent(rapidRequest, "", application.getName(), application.getVersion());
			}
		}
	}

	private void recordMonitorEvent(RapidRequest rapidRequest, String actionName, String appId, String appVersion) {
		_monitor.openEntry();
		_monitor.setActionName(actionName);
		_monitor.setAppId(appId);
		_monitor.setAppVersion(appVersion);
		_monitor.commitEntry(rapidRequest, null, 0);
	}

	@Override
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonAction) throws Exception {

		// get the rapid servlet for easy reference
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

		// get the servlet context for easy reference
		ServletContext servletContext = rapidServlet.getServletContext();

		// prepare our result object
		JSONObject result = new JSONObject();

		// get the action type
		String action = jsonAction.getString("actionType");

		// assume no new app id
		String newAppId = null;

		// assume we have no special rapid roles
		boolean rapidAdmin = false;
		boolean rapidDesign = false;
		boolean rapidUsers = false;
		boolean rapidMaster = false;

		// get the rapid security for the current user
		SecurityAdapter rapidSecurity = rapidRequest.getApplication().getSecurityAdapter();

		// if we got one
		if (rapidSecurity != null) {
			// check the admin role
			rapidAdmin = rapidSecurity.checkUserRole(rapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);
			// if no admin check designer
			if (!rapidAdmin) rapidDesign = rapidSecurity.checkUserRole(rapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);
			// if no admin check users
			if (!rapidAdmin) rapidUsers = rapidSecurity.checkUserRole(rapidRequest, com.rapid.server.Rapid.USERS_ROLE);
			// if we have admin check admin master
			if (rapidAdmin) rapidMaster = rapidSecurity.checkUserRole(rapidRequest, com.rapid.server.Rapid.MASTER_ROLE);
		}

		// get the id of the app we're about to manipulate
		String appId = jsonAction.getString("appId");
		// get the version of the app we're about to manipulate
		String appVersion = jsonAction.optString("version", null);
		// get the application we're about to manipulate
		Application app = rapidServlet.getApplications().get(appId, appVersion);

		// record the event to the monitor database table
		recordMonitorEvent(rapidRequest, jsonAction, action);

		// only if we had an application and one of the special Rapid roles
		if (app != null && (rapidAdmin || rapidDesign || rapidUsers)) {

			// recreate the rapid request using the application we wish to manipulate
			RapidRequest rapidActionRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), app);

			// get the application security
			SecurityAdapter security = app.getSecurityAdapter();

			// check the action
			if ("GETAPPS".equals(action)) {

				// create a json array for holding our apps
				JSONArray jsonApps = new JSONArray();

				// get a sorted list of the application ids
				for (String id : rapidServlet.getApplications().getIds()) {

					// loop the versions
					for (String version : rapidServlet.getApplications().getVersions(id).keySet()) {

						// get the this application version
						Application application = rapidServlet.getApplications().get(id, version);

						// assume permission is the same as rapid master
						boolean permission = rapidMaster;

						// if we don't have permission, yet
						if (!permission) {

							// get the security for this application
							security = application.getSecurityAdapter();

							// now emulate the app we are looping
							RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

							// check app permission
							permission = security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword());

							// if we passed the password check
							if (permission) {
								// if we have rapid admin
								if (rapidAdmin) {
									// we need it in the app too
									permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);
								} else {
									// if we have rapid design we need it in the app too
									if (rapidDesign) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.DESIGN_ROLE);
									// if we have rapid users we need it in the app too
									if (rapidUsers) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.USERS_ROLE);
								}
							}

						}

						// if app is rapid do a further check
						if (permission && "rapid".equals(application.getId())) permission = security.checkUserRole(rapidRequest, com.rapid.server.Rapid.SUPER_ROLE);

						// if we got permission - add this application to the list
						if (permission) {
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

					} // version loop

				} // app loop

				// add the applications to the result
				result.put("applications", jsonApps);

				// fetch the workflows
				JSONArray jsonWorkflows = new JSONArray();
				// loop the workflows
				for (String id : rapidServlet.getWorkflows().getIds()) {
					// get the workflow
					Workflow workflow = rapidServlet.getWorkflows().get(id);
					// create a json object
					JSONObject jsonWorkflow = new JSONObject();
					// add the details we want
					jsonWorkflow.put("value", workflow.getId());
					jsonWorkflow.put("text", workflow.getName() + " - " + workflow.getTitle());
					// add the object to the collection
					jsonWorkflows.put(jsonWorkflow);
				}

				// add the workflows to the result
				result.put("workflows", jsonWorkflows);

				// only send these if we have rapid admin
				if (rapidAdmin) {

					// fetch the database drivers
					JSONArray jsonDatabaseDrivers = rapidServlet.getJsonDatabaseDrivers();
					// check we have some database drivers
					if (jsonDatabaseDrivers != null) {
						// prepare the database driver collection we'll send
						JSONArray jsonDrivers = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonDatabaseDrivers.length(); i++) {
							// get the item
							JSONObject jsonDatabaseDriver = jsonDatabaseDrivers.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonDriver = new JSONObject();
							// add type
							jsonDriver.put("value", jsonDatabaseDriver.get("class"));
							// add name
							jsonDriver.put("text", jsonDatabaseDriver.get("name"));
							// add optional jdbc
							jsonDriver.put("jdbc", jsonDatabaseDriver.opt("jdbc"));
							// add to collection
							jsonDrivers.put(jsonDriver);
						}
						// add the database drivers to the result
						result.put("databaseDrivers", jsonDrivers);
					}

					// fetch the connection adapters
					JSONArray jsonConnectionAdapters = rapidServlet.getJsonConnectionAdapters();
					// check we have some database drivers
					if (jsonConnectionAdapters != null) {
						// prepare the database driver collection we'll send
						JSONArray jsonAdapters = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonConnectionAdapters.length(); i++) {
							// get the item
							JSONObject jsonConnectionAdapter = jsonConnectionAdapters.getJSONObject(i);
							// make a simpler send item
							JSONObject jsonSendAdapter = new JSONObject();
							// add type
							jsonSendAdapter.put("value", jsonConnectionAdapter.get("class"));
							// add name
							jsonSendAdapter.put("text", jsonConnectionAdapter.get("name"));
							// add to collection
							jsonAdapters.put(jsonSendAdapter);
						}
						// add the database drivers to the result
						result.put("connectionAdapters", jsonAdapters);
					}

					// security adapter are in rapidAdmin or rapidUsers block further down

					// fetch the form adapters
					JSONArray jsonFormAdapters = rapidServlet.getJsonFormAdapters();
					// prepare the collection we'll send
					JSONArray jsonAdapters = new JSONArray();
					// create an entry for no form adapter
					JSONObject jsonSendAdapter = new JSONObject();
					// no value
					jsonSendAdapter.put("value", "");
					// None as text
					jsonSendAdapter.put("text", "Please select...");
					// add the None member first
					jsonAdapters.put(jsonSendAdapter);
					// check we have some database drivers
					if (jsonFormAdapters != null) {
						// loop what we have
						for (int i = 0; i < jsonFormAdapters.length(); i++) {
							// get the item
							JSONObject jsonAdapter = jsonFormAdapters.getJSONObject(i);
							// make a simpler send item
							 jsonSendAdapter = new JSONObject();
							// add type
							jsonSendAdapter.put("value", jsonAdapter.get("type"));
							// add name
							jsonSendAdapter.put("text", jsonAdapter.get("name"));
							// add to collection
							jsonAdapters.put(jsonSendAdapter);
						}
						// add the database drivers to the result
						result.put("formAdapters", jsonAdapters);
					}

					// create a json object for our email settings
					JSONObject jsonEmail = new JSONObject();
					// get the email settings object
					Email email = Email.getEmailSettings();
					// if not null
					if (email != null) {
						// add email settings properties
						jsonEmail.put("enabled", email.getEnabled());
						jsonEmail.put("host", email.getHost());
						jsonEmail.put("port", email.getPort());
						jsonEmail.put("security", email.getSecurity());
						jsonEmail.put("userName", email.getUserName());
						// for password use ******** or empty string if not set
						if (email.getPassword() == null) {
							jsonEmail.put("password", "");
						} else {
							if ("".equals(email.getPassword())) {
								jsonEmail.put("password", "");
							} else {
								jsonEmail.put("password", "********");
							}
						}
					}
					// add the email settings
					result.put("email", jsonEmail);

					// create a json object for our processes
					JSONArray jsonProcesses = new JSONArray();
					// get the processes
					List<Process> processes = rapidServlet.getProcesses();
					// if not null
					if (processes != null) {
						// loop them
						for (Process process : processes) {
							// if visible
							if (process.isVisible()) {
								// make a json object for this process from it's config
								JSONObject jsonProcess = new JSONObject();
								// add the properties we need
								jsonProcess.put("name", process.getProcessName());
								jsonProcess.put("class", process.getClass());
								jsonProcess.put("visible", process.isVisible());
								jsonProcess.put("interval", process.getInterval());
								jsonProcess.put("days", process.getDays());
								jsonProcess.put("parameters", process.getParameters());
								jsonProcess.put("fileName", process.getFileName());
								// add the json process object to our collection
								jsonProcesses.put(jsonProcess);
							}
						}
					}
					// add the email settings
					result.put("processes", jsonProcesses);

					// add the Rapid version
					result.put("version", com.rapid.server.Rapid.VERSION);

				} // rapid admin check

				// either rapidAdmin or rapidDesign
				if (rapidAdmin || rapidDesign) {

					// prepare the collection we'll send
					JSONArray jsonThemes = new JSONArray();
					// create an entry for no template
					JSONObject jsonTheme = new JSONObject();
					// no value
					jsonTheme.put("value", "");
					// None as text
					jsonTheme.put("text", "None");
					// add the None member first
					jsonThemes.put(jsonTheme);
					// get the themes
					List<Theme> themes = rapidServlet.getThemes();
					// check we have some
					if (themes != null) {
						// loop what we have
						for (Theme theme : themes) {
							// only if visible
							if (theme.getVisible()) {
								// make a simpler send item
								jsonTheme = new JSONObject();
								// add type
								jsonTheme.put("value", theme.getType());
								// add name
								jsonTheme.put("text", theme.getName());
								// add to collection
								jsonThemes.put(jsonTheme);
							}
						}
						// add the database drivers to the result
						result.put("themes", jsonThemes);
					}

					// process the actions and only send the name and type
					JSONArray jsonSendActions = new JSONArray();
					JSONArray jsonActions = rapidServlet.getJsonActions();
					for (int i = 0; i < jsonActions.length(); i++) {
						// get this action
						JSONObject jsonSysAction = jsonActions.getJSONObject(i);
						// only if visible
						if (jsonSysAction.optBoolean("visible",true)) {
							JSONObject jsonSendAction = new JSONObject();
							jsonSendAction.put("name", jsonSysAction.getString("name"));
							jsonSendAction.put("type", jsonSysAction.getString("type"));
							jsonSendAction.put("helpHtml", jsonSysAction.optString("helpHtml"));
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
						// only present controls users can add, that aren't hidden
						if (jsonSysControl.optBoolean("canUserAdd") && jsonSysControl.optBoolean("visible",true)) {
							JSONObject jsonSendControl = new JSONObject();
							jsonSendControl.put("name", jsonSysControl.getString("name"));
							jsonSendControl.put("type", jsonSysControl.getString("type"));
							jsonSendControl.put("helpHtml", jsonSysControl.optString("helpHtml"));
							jsonSendControls.put(jsonSendControl);
						}
					}
					// add the controls to the result
					result.put("controls", jsonSendControls);

					// add the devices
					result.put("devices", rapidServlet.getDevices());

				}

				// either rapidAdmin or rapidUsers
				if (rapidAdmin || rapidUsers) {

					// fetch the security adapters
					JSONArray jsonSecurityAdapters = rapidServlet.getJsonSecurityAdapters();
					// check we have some security adapters
					if (jsonSecurityAdapters != null) {
						// prepare the security adapter collection we'll send
						JSONArray jsonAdapters = new JSONArray();
						// loop what we have
						for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
							// get the item
							JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
							// determine whether visible
							boolean visible = jsonSecurityAdapter.optBoolean("visible", true);
							// only if visible
							if (visible) {
								// make a simpler send item
								JSONObject jsonSendAdapter = new JSONObject();
								// get the type
								String type = jsonSecurityAdapter.getString("type");
								// add type
								jsonSendAdapter.put("value", type);
								// add name
								jsonSendAdapter.put("text", jsonSecurityAdapter.getString("name"));
								// add canManageRoles
								jsonSendAdapter.put("canManageRoles", jsonSecurityAdapter.optBoolean("canManageRoles"));
								// add canManageUsers
								jsonSendAdapter.put("canManageUsers", jsonSecurityAdapter.optBoolean("canManageUsers"));
								// add canManageUserRoles
								jsonSendAdapter.put("canManageUserRoles", jsonSecurityAdapter.optBoolean("canManageUserRoles"));
								// add to collection
								jsonAdapters.put(jsonSendAdapter);
								// if this is the one being used by the Rapid Admin app
								if (type.equals(rapidRequest.getApplication().getSecurityAdapterType())) {
									// add can manager roles
									result.put("canManageRoles", jsonSecurityAdapter.optBoolean("canManageRoles"));
									// add can manager users
									result.put("canManageUsers", jsonSecurityAdapter.optBoolean("canManageUsers"));
									// add can manager user roles
									result.put("canManageUserRoles", jsonSecurityAdapter.optBoolean("canManageUserRoles"));
								}
							} // visible check
						}
						// add the security adapters to the result
						result.put("securityAdapters", jsonAdapters);
					}

					// if we also have Rapid Master
					if (rapidMaster) result.put("useMaster", true);

				}

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

						// assume permission is the same as master
						boolean permission = rapidMaster;

						// if we don't have permission
						if (!permission) {

							// get the application security
							security = application.getSecurityAdapter();

							// now emulate the app we are looping
							RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

							// check permission
							permission = security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword());

							// if we passed the password check
							if (permission) {
								// if we have rapid admin
								if (rapidAdmin) {
									// we need it in the app too
									permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);
								} else {
									// if we have rapid design we need it in the app too
									if (rapidDesign) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.DESIGN_ROLE);
									// if we have rapid users we need it in the app too
									if (rapidUsers) permission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.USERS_ROLE);
								}
							}

						}

						// if app is rapid do a further check
						if (permission && "rapid".equals(application.getId())) permission = application.getSecurityAdapter().checkUserRole(rapidRequest, com.rapid.server.Rapid.SUPER_ROLE);

						// check the user password
						if (permission) {

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

						}

					} // versions loop

				} // got versions check

				// add the versions to the result
				result.put("versions", jsonVersions);

			} else if ("GETVERSION".equals(action)) {

				// assume permission is the same as master
				boolean permission = rapidMaster;

				// if we don't have permission
				if (!permission) {

					// password check
					if (security.checkUserPassword(rapidActionRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

						// check the users permission to admin this application
						permission = security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE);

						// if app is rapid do a further check
						if (permission && "rapid".equals(app.getId())) permission = security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.SUPER_ROLE);

					}

				}

				if (permission) {

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

					// date time formatter to use on created / modified date times
					SimpleDateFormat format = rapidServlet.getLocalDateTimeFormatter();

					// assume no data for app created value
					String created = "";
					// get any created date
					Date createdDate = app.getCreatedDate();
					// if we had a created date format as created
					if (createdDate != null) created = format.format(createdDate);
					// get any created user
					String createdUser = app.getCreatedBy();
					// if we had one
					if (createdUser != null) {
						// add by to go after date if we had that
						if (createdDate != null) created += " by ";
						// append the user name
						created += createdUser;
					}
					// add to to result
					result.put("createdBy", created);

					// assume no data for app modified value
					String modified = "";
					// get any created date
					Date modifiedDate = app.getModifiedDate();
					// if we had a modifiedDate date, format as modified
					if (modifiedDate != null) modified = format.format(modifiedDate);
					// get any modified user
					String modifiedUser = app.getModifiedBy();
					// if we had one
					if (modifiedUser != null) {
						// add by to go after date if we had that
						if (modifiedDate != null) modified += " by ";
						// append the user name
						modified += modifiedUser;
					}
					// add to result
					result.put("modifiedBy", modified);

					// add whether to show control ids
					result.put("pageNameIds", app.getPageNameIds());
					// add whether to show control ids
					result.put("showControlIds", app.getShowControlIds());
					// add whether to show action ids
					result.put("showActionIds", app.getShowActionIds());
					// add the is hidden
					result.put("isHidden", app.getIsHidden());
					// add the is get seetings id
					result.put("settingsId", app.getSettingsId());

					// add the form settings
					result.put("isForm", app.getIsForm());
					// add the form adapter
					result.put("formAdapterType", app.getFormAdapterType());
					// add if we're showing the form summary
					result.put("formShowSummary", app.getFormShowSummary());
					// add if auto complete is disabled
					result.put("formDisableAutoComplete", app.getFormDisableAutoComplete());

					// add forms email setting
					result.put("formEmail", app.getFormEmail());
					// add forms from address
					result.put("formEmailFrom", app.getFormEmailFrom());
					// add forms email to address
					result.put("formEmailTo", app.getFormEmailTo());
					// add forms attachment type
					result.put("formEmailAttachmentType", app.getFormEmailAttachmentType());

					// add whether to email the customer
					result.put("formEmailCustomer", app.getFormEmailCustomer());
					// get app input controls
					List<Control> inputControls = app.getAllControls(servletContext, "input", "responsiveinput");
					// make a json array for them
					JSONArray jsonInputControls = new JSONArray();
					// if there were controls
					if (inputControls != null && inputControls.size() > 0) {
						// loop them
						for (Control control : inputControls) {
							// get the name
							String controlName = control.getName();
							// if there is one
							if (controlName != null && controlName.trim().length() > 0) {
								// make a json object for them
								JSONObject jsonControl = new JSONObject();
								// add text
								jsonControl.put("text", controlName);
								// add value
								jsonControl.put("value", control.getId());
								// add control to collection
								jsonInputControls.put(jsonControl);
							}
						}
					}
					// add the json control collection to the result
					result.put("inputControls", jsonInputControls);

					// add the customer email control id
					result.put("formEmailCustomerControlId", app.getFormEmailCustomerControlId());
					// add the customer email subject
					result.put("formEmailCustomerSubject", app.getFormEmailCustomerSubject());
					// add the customer email type, T or H
					result.put("formEmailCustomerType", app.getFormEmailCustomerType());
					// add the customer email body
					result.put("formEmailCustomerBody", app.getFormEmailCustomerBody());
					// add the customer email attachment type
					result.put("formEmailCustomerAttachmentType", app.getFormEmailCustomerAttachmentType());

					// add form file details
					result.put("formFile", app.getFormFile());
					// add form file type
					result.put("formFileType", app.getFormFileType());
					// add form file path
					result.put("formFilePath", app.getFormFilePath());
					// add form file username
					result.put("formFileUserName", app.getFormFileUserName());
					// add form file password
					result.put("formFilePassword", app.getFormFilePassword());
					// add form webservice
					result.put("formWebservice", app.getFormWebservice());
					// add form webservice URL
					result.put("formWebserviceURL", app.getFormWebserviceURL());
					// add form webservice type
					result.put("formWebserviceType", app.getFormWebserviceType());
					// add form webservice SOAP action
					result.put("formWebserviceSOAPAction", app.getFormWebserviceSOAPAction());

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
					result.put("themeType", app.getThemeType());
					result.put("styles", app.getStyles());
					result.put("statusBarColour", app.getStatusBarColour());
					result.put("statusBarHighlightColour", app.getStatusBarHighlightColour());
					result.put("statusBarTextColour", app.getStatusBarTextColour());
					result.put("statusBarIconColour", app.getStatusBarIconColour());

					// add the security adapter
					result.put("securityAdapter", app.getSecurityAdapterType());
					// add whether there is device security
					result.put("deviceSecurity", app.getDeviceSecurity());
					// add whether password is retained on Rapid Mobile
					result.put("storePasswordDuration", app.getStorePasswordDuration());
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
						// get a synchronised list for multithreaded sorting
						List<Webservice> webservices = Collections.synchronizedList(app.getWebservices());
						// synchronise this block for thread-safety
						synchronized (this) {
							// sort them by their name
							Collections.sort(webservices, new Comparator<Webservice>() {
								@Override
								public int compare(Webservice o1, Webservice o2) {
									if (o1 == null) {
										return -1;
									} else if (o2 == null) {
										return 1;
									} else {
										return Comparators.AsciiCompare(o1.getName(), o2.getName(), false);
									}
								}
							});
						}
						// loop and add to jsonArray
						for (Webservice webservice : webservices) {
							jsonWebservices.put(webservice.getName());
						}
					}
					// add webservices connections
					result.put("webservices", jsonWebservices);

					// create an array for the parameters
					JSONArray jsonParameters = new JSONArray();

					// check we have some webservices
					if (app.getParameters() != null) {
						// get a synchronised list for multithreaded sorting
						List<Parameter> parameters = Collections.synchronizedList(app.getParameters());
						// synchronize this block
						synchronized (this) {
							// sort them by their name
							Collections.sort(parameters, new Comparator<Parameter>() {
								@Override
								public int compare(Parameter o1, Parameter o2) {
									if (o1 == null) {
										return -1;
									} else if (o2 == null) {
										return 1;
									} else {
										return Comparators.AsciiCompare(o1.getName(), o2.getName(), false);
									}
								}
							});
						}
						// loop and add to jsonArray
						for (Parameter parameter : parameters) {
							jsonParameters.put(parameter.getName());
						}
					}
					// add webservices connections
					result.put("parameters", jsonParameters);

					// create an array for the resources
					JSONArray jsonResources = new JSONArray();

					// check we have some resources
					if (app.getAppResources() != null) {
						// get a synchronised list for multithreaded sorting
						List<Resource> resources = Collections.synchronizedList(app.getAppResources());
						// synchronize this block
						synchronized (this) {
							// sort them by their name
							Collections.sort(resources, new Comparator<Resource>() {
								@Override
								public int compare(Resource o1, Resource o2) {
									if (o1 == null) {
										return -1;
									} else if (o2 == null) {
										return 1;
									} else {
										return Comparators.AsciiCompare(o1.getName(), o2.getName(), false);
									}
								}
							});
						}
						// loop and adds2 to jsonArray
						for (Resource resource : resources) {
							jsonResources.put(resource.getName());
						}
					}
					// add webservices connections
					result.put("resources", jsonResources);

					// create an array for the app backups
					JSONArray jsonAppBackups = new JSONArray();

					// check we have some app backups
					if (app.getApplicationBackups(rapidServlet) != null) {
						// loop and add to jsonArray
						for (Application.Backup appBackup : app.getApplicationBackups(rapidServlet)) {
							// create the backup json object
							JSONObject jsonBackup = new JSONObject();
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

			} else if ("GETWORKFLOWS".equals(action)) {

				// create a json array for holding our workflows
				JSONArray jsonWorkflows = new JSONArray();

				// get a sorted list of the workflows
				for (String id : rapidServlet.getWorkflows().getIds()) {

						// get the this workflow
						Workflow workflow = rapidServlet.getWorkflows().get(id);

						/*

						// get the security
						SecurityAdapter security = application.getSecurityAdapter();

						// now emulate the app we are looping
						RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

						// check the user password
						if (security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

							// check the users permission to admin this application
							boolean adminPermission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);

							// if app is rapid do a further check
							if (adminPermission && "rapid".equals(application.getId())) adminPermission = application.getSecurityAdapter().checkUserRole(appSecurityRequest, com.rapid.server.Rapid.SUPER_ROLE);

							// if we got permssion - add this application to the list
							if (adminPermission) {
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

						*/

						// create a json object
						JSONObject jsonWorkflow = new JSONObject();
						// add the details we want
						jsonWorkflow.put("value", workflow.getId());
						jsonWorkflow.put("text", workflow.getName() + " - " + workflow.getTitle());
						// add to workflows
						jsonWorkflows.put(jsonWorkflow);

				} // app loop

				// add the actions to the result
				result.put("workflows", jsonWorkflows);

			} else if ("GETWORKFLOW".equals(action)) {

				// get the id
				String id = jsonAction.getString("wfId");

				// get the this workflow
				Workflow workflow = rapidServlet.getWorkflows().get(id);

				/*

				// get the security
				SecurityAdapter security = application.getSecurityAdapter();

				// now emulate the app we are looping
				RapidRequest appSecurityRequest = new RapidRequest(rapidServlet, rapidRequest.getRequest(), application);

				// check the user password
				if (security.checkUserPassword(appSecurityRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

					// check the users permission to admin this application
					boolean adminPermission = security.checkUserRole(appSecurityRequest, com.rapid.server.Rapid.ADMIN_ROLE);

					// if app is rapid do a further check
					if (adminPermission && "rapid".equals(application.getId())) adminPermission = application.getSecurityAdapter().checkUserRole(appSecurityRequest, com.rapid.server.Rapid.SUPER_ROLE);

					// if we got permssion - add this application to the list
					if (adminPermission) {
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

				} // pass word check

				*/

				// add the details we want
				result.put("id", workflow.getId());
				result.put("name", workflow.getName());
				result.put("title", workflow.getTitle());
				result.put("description", workflow.getDescription());

			} // action type check

			// if we don't have a result yet
			if (result.length() == 0) {

				// if we have rapid admin role
				if (rapidAdmin) {

					if ("GETDEVICE".equals(action)) {

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

					} else if ("GETSESSIONS".equals(action)) {

						// create the json object
						JSONObject jsonDetails= new JSONObject();

						// create a json array
						JSONArray jsonSessions = new JSONArray();

						// get the sessions
						Map<String, HttpSession> sessions = RapidSessionListener.getSessions();

						// check we got some
						if (sessions != null) {

							// get a date formatter
							SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

							// loop them
							for (String key : sessions.keySet()) {
								// get the session
								HttpSession httpSession = sessions.get(key);
								// create object
								JSONObject jsonSession = new JSONObject();
								// add name
								jsonSession.put("name", httpSession.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME));
								// add device
								jsonSession.put("device", httpSession.getAttribute(RapidFilter.SESSION_VARIABLE_USER_DEVICE));
								// add last resource
								jsonSession.put("resource", httpSession.getAttribute(RapidFilter.SESSION_VARIABLE_USER_RESOURCE));
								// get a new date from the created time
								Date createTime = new Date(httpSession.getCreationTime());
								// add created date
								jsonSession.put("created", df.format(createTime));
								// get a new date from the last access time
								Date accessTime = new Date(httpSession.getLastAccessedTime());
								// add last access
								jsonSession.put("access", df.format(accessTime));
								// add to collections
								jsonSessions.put(jsonSession);
							}

						}

						// add sessions
						jsonDetails.put("sessions", jsonSessions);

						return jsonDetails;

					} else if ("GETPROCESSES".equals(action)) {

						// to return
						JSONObject jsonDetails = new JSONObject();
						// create a json object for our processes
						JSONArray jsonProcesses = new JSONArray();
						// get the processes
						List<Process> processes = rapidServlet.getProcesses();

						// if there are some
						if (processes != null) {
							// loop them
							for (Process process : processes) {
								// if its visible put its name in the list to be returned
								if (process.isVisible()) {
									jsonProcesses.put(process.getProcessName());
								}
							}
						}

						// add the processes to our response
						jsonDetails.put("processes", jsonProcesses);
						return jsonDetails;

					} else if ("GETPROCESS".equals(action)) {

						// to return
						JSONObject jsonDetails = new JSONObject();
						// get the processes
						List<Process> processes = rapidServlet.getProcesses();

						String processName = jsonAction.getString("processName");

						if (processes != null) {
							for (Process process : processes) {
								if (process.getProcessName().equals(processName)) {
									// add the properties we need
									jsonDetails.put("className", process.getClassName());
									jsonDetails.put("interval", process.getInterval());
									jsonDetails.put("duration", process.getDuration());
									jsonDetails.put("days", process.getDays());
									jsonDetails.put("parameters", process.getParameters());
									jsonDetails.put("fileName", process.getFileName());
									break;
								}
							}
						}

						return jsonDetails;

					} else if ("RELOADACTIONS".equals(action)) {

						// load actions and set the result message
						result.put("message", RapidServletContextListener.loadActions(servletContext) + " actions reloaded");

					} else if ("RELOADCONTROLS".equals(action)) {

						// load controls and set the result message
						result.put("message", RapidServletContextListener.loadControls(servletContext) + " controls reloaded");

					} else if ("RELOADAPPLICATIONS".equals(action)) {

						// load applications and set the result message
						result.put("message", RapidServletContextListener.loadApplications(servletContext) + " applications reloaded");

					} else if ("RELOADADAPTERS".equals(action)) {

						// ints for number of bits of each we're reloading
						int databaseDrivers = 0;
						int connectionAdapters = 0;
						int securityAdapters = 0;
						int forms = 0;
						int themes = 0;
						int devices = 0;

						// reload adapters and set the int for the result message
						databaseDrivers = RapidServletContextListener.loadDatabaseDrivers(servletContext);
						connectionAdapters = RapidServletContextListener.loadConnectionAdapters(servletContext);
						securityAdapters = RapidServletContextListener.loadSecurityAdapters(servletContext);
						forms =  RapidServletContextListener.loadFormAdapters(servletContext);
						themes = RapidServletContextListener.loadThemes(servletContext);
						devices = Devices.load(servletContext).size();

						// reset upload mime types so they are reloaded
						rapidServlet.resetMimeTypes();

						// show result message
						result.put("message",
							databaseDrivers + " database driver" + (databaseDrivers == 1 ? "" : "s") + ", " +
							connectionAdapters + " connection adapter" + (connectionAdapters == 1 ? "" : "s") + ", " +
							securityAdapters + " security adapter" + (securityAdapters == 1 ? "" : "s") + ", " +
							forms + " form adapter" + (forms == 1 ? "" : "s") + ", " +
							themes + " theme" + (themes == 1 ? "" : "s") + ", " +
							devices + " device" + (devices == 1 ? "" : "s") + " reloaded"
						);

					} else if ("RELOADPROCESSES".equals(action)) {

						// assume no processes
						int processes = 0;

						// (re)load the processes, this interrupts/stops all of the current process objects and then makes new ones
						processes = RapidServletContextListener.loadProcesses(servletContext);

						// load processes and set the result message
						result.put("message", processes + " process" + (processes == 1 ? "" : "es") + " reloaded");

					} else if ("RELOADVERSION".equals(action)) {

						// look for an application file in the application folder
						File applicationFile = new File(app.getConfigFolder(servletContext) + "/application.xml");

						// close the existing app
						app.close(servletContext);

						// reload the application from file
						Application reloadedApplication = Application.load(servletContext, applicationFile);

						// replace it into the applications collection
						rapidServlet.getApplications().put(reloadedApplication);

						// load applications and set the result message
						result.put("message", "Version reloaded");

					} else if ("REBUILDPAGES".equals(action)) {

						// add the message to the response
						result.put("message", "This feature is not supported");

					} else if ("NEWAPP".equals(action)) {

						// retrieve the inputs from the json
						String name = jsonAction.getString("name").trim();
						String version = jsonAction.getString("newVersion").trim();
						String title = jsonAction.optString("title").trim();
						String type = jsonAction.optString("type");
						String formAdapterType = jsonAction.optString("formAdapter", null);
						boolean responsive = jsonAction.optBoolean("responsive");
						String themeType = jsonAction.optString("themeType");
						String description = jsonAction.optString("description").trim();

						// create a new application with our reusable, private method
						Application newApp = createApplication(rapidServlet, rapidRequest, name, version, title, type, responsive, themeType, description);

						// if this is a form, add the adapter
						if ("F".equals(type)) newApp.setFormAdapter(servletContext, formAdapterType);

						// set the result message
						result.put("message", "Application " + newApp.getTitle() + " created");

						// set the result appId
						result.put("appId", newApp.getId());

						// set the result version
						result.put("version", newApp.getVersion());

					} else if ("NEWWORKFLOW".equals(action)) {

						// retrieve the inputs from the json
						String name = jsonAction.getString("name").trim();
						String title = jsonAction.optString("title").trim();
						String description = jsonAction.optString("description").trim();

						// create a new workflow with our reusable, private method
						Workflow newWorkflow = createWorkflow(rapidServlet, rapidRequest, name, title, description);

						// set the result message
						result.put("message", "Workflow " + app.getTitle() + " created");

						// set the result appId
						result.put("wfId", newWorkflow.getId());

					} else if ("DELWORKFLOW".equals(action)) {

						// get the id
						String id = jsonAction.getString("id").trim();
						// get the collection of workflows
						Workflows workflows = rapidServlet.getWorkflows();
						// get the workflow
						Workflow workflow = workflows.get(id);
						// delete it
						workflow.delete(rapidServlet, rapidRequest);
						// set the result message
						result.put("message", "Work flow " + workflow.getName() + " deleted");

					} else if ("NEWDEVICE".equals(action)) {

						// get the devices
						Devices devices = rapidServlet.getDevices();

						// add a new device to the collection
						devices.add(new Device("New device", 500, 500, 200, 1d));

						// save it
						devices.save(servletContext);

					} else if ("DELDEVICE".equals(action)) {

						// get the index
						int index = jsonAction.getInt("index");

						// get the devices
						Devices devices = rapidServlet.getDevices();

						// remove the device
						devices.remove(index);

						// save the devices
						devices.save(servletContext);

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
						devices.save(servletContext);

						// set the result message
						result.put("message", "Device details saved");

					} else  if ("TESTEMAIL".equals(action)) {

						String host = jsonAction.getString("host").trim();
						int port = jsonAction.getInt("port");
						String sec = jsonAction.getString("security").trim();
						String userName = jsonAction.getString("userName").trim();
						String password = jsonAction.getString("password");

						// if password is ********
						if ("********".equals(password)) {
							// get the current email settings
							Email emailSettings = Email.getEmailSettings();
							// if we got one use that password
							if (emailSettings != null) password = emailSettings.getPassword();
						}

						// set the properties we've just received
						Email.setProperties(host, port, sec, userName, password);

						// assume to email is the constant
						String from = TEST_EMAIL_FROM;
						// look for a setting in the web.xml
						String fromSetting = servletContext.getInitParameter("email.test.from");
						// if we got one use this value
						if (fromSetting != null) from = fromSetting;

						// assume to email is the constant
						String to = TEST_EMAIL_TO;
						// look for a setting in the web.xml
						String toSetting = servletContext.getInitParameter("email.test.to");
						// if we got one use this value
						if (toSetting != null) to = toSetting;

						try {

							// send a test email
					        Email.send(from, to, "Rapid test email", "It's working!");

						} catch (Exception ex) {

							// reload the saved values
					        Email.load(servletContext);

					        // rethrow
					        throw ex;

						}

				        // reload the saved values
				        Email.load(servletContext);

				        // add a meaningful response message which the success callback is expecting
				        result.put("message", "Test email sent OK");

					} else if ("SAVEEMAIL".equals(action)) {

						// get whether enabled
						boolean enabled = jsonAction.optBoolean("enabled");

						// get any current email settings
						Email email = Email.getEmailSettings();

						// check whether enabled
						if (enabled) {

							String host = jsonAction.getString("host").trim();
							int port = jsonAction.getInt("port");
							String sec = jsonAction.getString("security").trim();
							String userName = jsonAction.getString("userName").trim();
							String password = jsonAction.getString("password");
							// if we just got 8 *'s for the password
							if ("********".equals(password)) {
								// if we had an email object
								if (email == null) {
									// make it a blank space
									password = "";
								} else {
									// set it back to stop being overridden with 8 *'s
									password = email.getPassword();
								}
							}

							// set the properties we've just loaded
				            Email.setProperties(host, port, sec, userName, password);

				            // construct a new object
					        email = new Email(host, port, sec, userName, password);

					        // set enabled
					        email.setEnabled(true);

						} else {

							// for safety, make a new one if not yet set
							if (email == null) email = new Email();

							// set not enabled
					        email.setEnabled(false);

						}

				        // save it
				        email.save(servletContext);

					} else if ("NEWPROCESS".equals(action)) {

						// get our list of processes
						List<Process> processes = rapidServlet.getProcesses();

						// make our new process! ... (including un-packing the jsonAction JSON we got in, using the new process name to make a safe file name for WEB-INF/processes)
						// this will come from a new dialogue page called 20_ProcessNew - it should just ask for the new name, save below will be used to set all of the details

						// get the new process name from the front-end json
						String newProcessName = jsonAction.getString("processName");
						// derive a file name by safing the new process name
						String filename = Files.safeName(newProcessName) + ".process.xml";

						// assume the name is not being used
						boolean nameInUse = false;
						// loop all processes
						for (Process process : processes) {
							// if the name or file name is in use already
							if (process.getProcessName().equals(newProcessName) || process.getFileName().equals(filename)) {
								// retain that this process exists
								nameInUse = true;
								break;
							}
						}

						// if the name is used already
						if (nameInUse) {

							// throw exception to the front-end
							throw new Exception("Name is already used");

						} else {

							// get the class name from the front-end action json
							String className = jsonAction.getString("processClassName");


							String path = servletContext.getRealPath("/") + "/WEB-INF/processes/" + filename;
							File file = new File(path);
							if (file.exists()) throw new Exception("Name is already used");
							file.createNewFile();

							XMLGroup processXML = new XMLGroup("process")
							.add(new XMLAttribute("xmlVersion", "1"))
							.add(new XMLAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"))
							.add(new XMLAttribute("xsi:noNamespaceSchemaLocation", "../schemas/process.xsd"))
							.add(new XMLValue("name", newProcessName))
							.add(new XMLValue("class", "com.rapid.processes." + className))
							.add(new XMLValue("interval", "-1"))
							.add(new XMLGroup("duration")
								.add(new XMLValue("start", "0:00"))
								.add(new XMLValue("stop", "23:00"))
							)
							.add(new XMLGroup("days")
								.add(new XMLValue("monday", "false"))
								.add(new XMLValue("tuesday", "false"))
								.add(new XMLValue("wednesday", "false"))
								.add(new XMLValue("thursday", "false"))
								.add(new XMLValue("friday", "false"))
								.add(new XMLValue("saturday", "false"))
								.add(new XMLValue("sunday", "false"))
							);

							String newDocumentBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + processXML;

							Writer writer = new FileWriter(file);

							writer.write(newDocumentBody);
							writer.close();

							// assume no processes
							int processesCount = 0;

							// (re)load the processes, this interrupts/stops all of the current process objects and then makes new ones
							try {
								processesCount = RapidServletContextListener.loadProcesses(servletContext);
							} catch (ClassNotFoundException ex) {
								// delete the file we made above
								file.delete();
								// rethrow
								throw ex;
							}

							// load processes and set the result message
							result.put("message", processes + " process" + (processesCount == 1 ? "" : "es") + " reloaded");

						}

					} else if ("DELPROCESS".equals(action)) {

						// similar to above, but find and remove by name property

						String processName = jsonAction.getString("processName");

						// get the processes
						List<Process> processes = rapidServlet.getProcesses();

						// use the process name to get its process - to get its filename - to get its xml file
						if (processes != null) {

							// get the directory in which the process xml files are stored
							File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/processes/");

							// a reference for the process we want to delete (if we can find it)
							Process deleteProcess = null;

							// loop through all processes, matching on name to find the right one, then loop all files matching against the process file to find that
							searchForProcess:
							for (Process process : processes) {
								if (process.getProcessName().equals(processName)) {
									for (File xmlFile : dir.listFiles()) {
										if (process.getFileName().equals(xmlFile.getName())) {

											// retain that this is the process to delete
											deleteProcess = process;

											// stop the process
											process.interrupt();

											// delete the file
											xmlFile.delete();

											// exit both loops
											break searchForProcess;
										}
									}
								}
							}

							// remove the deleted process from our collection
							if (deleteProcess != null) processes.remove(deleteProcess);

						}

						// assume no processes
						int processesCount = 0;

						// (re)load the processes, this interrupts/stops all of the current process objects and then makes new ones
						processesCount = RapidServletContextListener.loadProcesses(servletContext);

						// load processes and set the result message
						result.put("message", processes + " process" + (processesCount == 1 ? "" : "es") + " reloaded");

					} else if ("SAVEPROCESS".equals(action)) {

						// similar to new, but will have all process details, but find old object, call its interrupt(), and replace a new one into collection by name/property index.
						// will have to (re)create the xml file manually (make a temp one first and then copy over/delete)
						// use com.rapid.utils.XML and created methods like addChildElement(Node node, String elementName, value) and addAtribute(Node node, String attributeName, String value)
						// add the namespace and schema attribues so the xml files you save are exactly like the existing ones! (particually that they validate against the schema)
						// processes and their info are added to the UI in the GETAPPS action type

						// get the name of the process from the front-end action json
						String processName = jsonAction.getString("processName");

						// get all processes
						List<Process> processes = rapidServlet.getProcesses();

						// use the process name to get its process - to get its filename - to get its xml file
						if (processes != null) {

							// get the directory in which the process xml files are stored
							File dir = new File(servletContext.getRealPath("/") + "/WEB-INF/processes/");

							searchForProcess:
							for (Process process : processes) {
								if (process.getProcessName().equals(processName)) {
									for (File xmlFile : dir.listFiles()) {
										if (process.getFileName().equals(xmlFile.getName())) {

											// get all of the process details sent to us in the action json
											JSONObject details = jsonAction.getJSONObject("details");

											// use those details to save the process to the correct file
											process.save(details, xmlFile);

											// now (re)load the process from the file we just saved
											RapidServletContextListener.loadProcess(xmlFile, servletContext);

											// exit both loops
											break searchForProcess;
										}
									}
								}
							}

						}

					} else if ("GETRAPIDLOG".equals(action)) {

							int nLines = jsonAction.optInt("nLines");

							JSONObject jsonDetails = new JSONObject();
							JSONArray lines = new JSONArray();
							jsonDetails.put("lines", lines);

							File logFile = new File(servletContext.getRealPath("/") + "/WEB-INF/logs/Rapid.log/");

							if (logFile.exists()) {

								Scanner logScanner = new Scanner(logFile);

								int lineIndex;
								for (lineIndex = 0; logScanner.hasNextLine(); lineIndex++) {
									lines.put(lineIndex % nLines, logScanner.nextLine());
								}
								logScanner.close();

								jsonDetails.put("firstLineIndex", lineIndex % nLines);

							}

							return jsonDetails;

					} // action type check

				}

				// if rapid admin or rapid users
				if (rapidAdmin || rapidUsers) {

					// check user app password, or master user
					if (rapidMaster || security.checkUserPassword(rapidActionRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

						// get whether user has the rapid master, or app admin role
						boolean appAdmin = rapidMaster || security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE);

						// assume the user does not have the app users role
						boolean appUsers = false;
						// assume the user does not have the app designers role
						boolean appDesign = false;

						try {

							// get whether user has the app users role
							appUsers = security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.USERS_ROLE);
							// get whether user has the app design role
							appDesign = security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.DESIGN_ROLE);

						} catch (SecurityAdapaterException ex) {

							// log
							_logger.error("Error checking users and design roles", ex);

						}

						// check user app admin role
						if (appAdmin) {

							// if master too
							if (rapidMaster) {

								if ("ADDADMINUSER".equals(action)) {

									// get the user name
									String userName = jsonAction.getString("user");

									// add this user name to the rapid request
									rapidRequest.setUserName(userName);

									// get this user from Rapid security
									User user = rapidSecurity.getUser(rapidRequest);

									// if we got one
									if (user != null) {

										// make a new user based on this one, but without any roles
										User newUser = new User(user.getName(), user.getDescription(), user.getEmail(), user.getPassword(), user.getDeviceDetails(), new UserRoles());

										// add the admin role to this user
										newUser.getRoles().add(com.rapid.server.Rapid.ADMIN_ROLE);

										// add this new user to the app
										security.addUser(rapidActionRequest, newUser);
									}

								} // master action check

							} // rapid master check

							if ("GETDBCONN".equals(action)) {

								// must have rapid admin
								if (rapidAdmin) {

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

							} else  if ("GETPARAM".equals(action)) {

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
										jsonParameter.put("description", parameter.getDescription());
										jsonParameter.put("value", parameter.getValue());

									}
								}

								// add the parameter to the result
								result.put("parameter", jsonParameter);

							} else if ("GETRESOURCE".equals(action)) {

								// retrieve the index
								int index = jsonAction.getInt("index");

								// create the json object
								JSONObject jsonParameter = new JSONObject();

								// check the resources
								if (app.getAppResources() != null) {

									// check we have the one requested
									if (index >= 0 && index < app.getAppResources().size()) {

										// get the parameter
										Resource resource = app.getAppResources().get(index);

										// add the name and value
										jsonParameter.put("name", resource.getName());
										jsonParameter.put("type", resource.getType());
										jsonParameter.put("value", resource.getContent());

									}
								}

								// add the parameter to the result
								result.put("resource", jsonParameter);

							} else if ("SAVEAPP".equals(action)) {

								// get the new values
								String id = Files.safeName(jsonAction.getString("name")).toLowerCase();
								String version = Files.safeName(jsonAction.getString("saveVersion"));
								int status = jsonAction.optInt("status");
								String name = jsonAction.getString("name");
								String title = jsonAction.getString("title");
								String description = jsonAction.getString("description");

								boolean isForm = jsonAction.optBoolean("isForm");
								String startPageId = jsonAction.optString("startPageId","");
								boolean isHidden = jsonAction.optBoolean("isHidden");
								boolean pageNameIds = jsonAction.optBoolean("pageNameIds");
								boolean showControlIds = jsonAction.optBoolean("showControlIds");
								boolean showActionIds = jsonAction.optBoolean("showActionIds");
								String settingsId = jsonAction.optString("settingsId", null);

								String formAdapter = jsonAction.optString("formAdapter");
								boolean formShowSummary = jsonAction.optBoolean("formShowSummary");
								boolean formDisableAutoComplete = jsonAction.optBoolean("formDisableAutoComplete");

								boolean formEmail = jsonAction.optBoolean("formEmail");
								String formEmailFrom = jsonAction.optString("formEmailFrom");
								String formEmailTo = jsonAction.optString("formEmailTo");
								String formEmailAttachmentType = jsonAction.optString("formEmailAttachmentType");

								boolean formEmailCustomer = jsonAction.optBoolean("formEmailCustomer");
								String formEmailCustomerControlId = jsonAction.optString("formEmailCustomerControlId");
								String formEmailCustomerSubject = jsonAction.optString("formEmailCustomerSubject");
								String formEmailCustomerType = jsonAction.optString("formEmailCustomerType");
								String formEmailCustomerBody = jsonAction.optString("formEmailCustomerBody");
								String formEmailCustomerAttachmentType = jsonAction.optString("formEmailCustomerAttachmentType");

								boolean formFile = jsonAction.optBoolean("formFile");
								String formFileType = jsonAction.optString("formFileType");
								String formFilePath = jsonAction.optString("formFilePath");
								String formFileUserName = jsonAction.optString("formFileUserName");
								String formFilePassword = jsonAction.optString("formFilePassword");

								boolean formWebservice = jsonAction.optBoolean("formWebservice");
								String formWebserviceURL = jsonAction.optString("formWebserviceURL");
								String formWebserviceType = jsonAction.optString("formWebserviceType");
								String formWebserviceSOAPAction = jsonAction.optString("formWebserviceSOAPAction");

								// assume we do not need to update the applications drop down
								boolean appUpdated = false;

								// if the id or version is now different we need to move it, rebuilding all the resources as we go
								if (!app.getId().equals(id) || !app.getVersion().equals(version)) {
									// copy the app to the id/version, returning the new one for saving
									app = app.copy(rapidServlet, rapidActionRequest, id, version, true, true);
									// mark that it has been updated
									appUpdated = true;
								}

								// assume we do not need to reload the application
								boolean appReload = false;

								// if the status has changed we do want to reload
								if (app.getStatus() != status) appReload = true;

								// update the values
								app.setName(name);
								app.setStatus(status);
								app.setTitle(title);
								app.setDescription(description);

								app.setIsHidden(isHidden);
								app.setIsForm(isForm);
								app.setStartPageId(startPageId);
								app.setPageNameIds(pageNameIds);
								app.setShowControlIds(showControlIds);
								app.setShowActionIds(showActionIds);
								app.setSettingsId(settingsId); // we'll load and set the settings object just before calling the app.save

								app.setFormAdapterType(formAdapter);
								app.setFormShowSummary(formShowSummary);
								app.setFormDisableAutoComplete(formDisableAutoComplete);

								app.setFormEmail(formEmail);
								app.setFormEmailFrom(formEmailFrom);
								app.setFormEmailTo(formEmailTo);
								app.setFormEmailAttachmentType(formEmailAttachmentType);

								app.setFormEmailCustomer(formEmailCustomer);
								app.setFormEmailCustomerControlId(formEmailCustomerControlId);
								app.setFormEmailCustomerSubject(formEmailCustomerSubject);
								app.setFormEmailCustomerType(formEmailCustomerType);
								app.setFormEmailCustomerBody(formEmailCustomerBody);
								app.setFormEmailCustomerAttachmentType(formEmailCustomerAttachmentType);

								app.setFormFile(formFile);
								app.setFormFileType(formFileType);
								app.setFormFilePath(formFilePath);
								app.setFormFileUserName(formFileUserName);
								app.setFormFilePassword(formFilePassword);

								app.setFormWebservice(formWebservice);
								app.setFormWebserviceURL(formWebserviceURL);
								app.setFormWebserviceType(formWebserviceType);
								app.setFormWebserviceSOAPAction(formWebserviceSOAPAction);

								// reload
								if (appReload) {

									// load the pages (actually clears down the pages collection and reloads the headers)
									app.getPages().loadpages(servletContext);

								}

								// save - this also saves the settings object to its file if one is being used
								app.save(rapidServlet, rapidActionRequest, true);

								// if there are settings
								if (settingsId == null || settingsId.length() == 0) {
									// no settings so empty
									app.setSettings(null);
								} else {
									// load and set them!
									app.setSettings(Settings.load(servletContext, app));
								}

								// add the application to the response
								result.put("message", "Application details saved");
								result.put("update", appUpdated);
								result.put("reload", appReload);

							} else if ("SAVESTYLES".equals(action)) {

								// get the values from the front-end json
								String themeType = jsonAction.getString("themeType");
								String styles = jsonAction.getString("styles");
								String statusBarColour = jsonAction.optString("statusBarColour");
								String statusBarHighlightColour = jsonAction.optString("statusBarHighlightColour");
								String statusBarTextColour = jsonAction.optString("statusBarTextColour");
								String statusBarIconColour = jsonAction.optString("statusBarIconColour");

								// put the values on the app (which will apply to the settings object if in use)
								app.setThemeType(themeType);
								app.setStyles(styles);
								app.setStatusBarColour(statusBarColour);
								app.setStatusBarHighlightColour(statusBarHighlightColour);
								app.setStatusBarTextColour(statusBarTextColour);
								app.setStatusBarIconColour(statusBarIconColour);

								// save the app (also saves the settings object if in use)
								app.save(rapidServlet, rapidActionRequest, true);

								// add the application to the response
								result.put("message", "Styles saved");

							} else if ("SAVEDBCONN".equals(action)) {

								// get the index
								int index = jsonAction.getInt("index");

								// get the database connections
								List<DatabaseConnection> dbConns = app.getDatabaseConnections();

								// if there weren't any
								if (dbConns == null) {
									// make some
									dbConns = new ArrayList<>();
									// add them in
									app.setDatabaseConnections(dbConns);
								}

								// remember whether we found the connection
								boolean foundConnection = false;

								// check we have database connections
								if (dbConns != null) {
									// check the index we where given will retrieve a database connection
									if (index > -1 && index < dbConns.size()) {
										// get the database connection
										DatabaseConnection dbConn = dbConns.get(index);

										// set the database connection properties
										dbConn.setName(jsonAction.getString("name"));
										dbConn.setDriverClass(jsonAction.getString("driver"));
										dbConn.setConnectionString(jsonAction.getString("connectionString").trim()); // note the trim
										dbConn.setConnectionAdapterClass(jsonAction.getString("connectionAdapter"));
										dbConn.setUserName(jsonAction.getString("userName"));
										String password = jsonAction.getString("password");
										// only set the password if it's different from the default
										if (!"********".equals(password)) dbConn.setPassword(password);

										// reset the dbconn so the adapter is re-initialised with any changes
										dbConn.reset();

										// save the app
										app.save(rapidServlet, rapidActionRequest, true);

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
											app.save(rapidServlet, rapidActionRequest, true);

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
											app.save(rapidServlet, rapidActionRequest, true);

											foundWebservice = true;

											// add the application to the response
											result.put("message", "Java webservice saved");
										}
									}
								}

								if (!foundWebservice) result.put("message", "Java webservice could not be found");

							} else if ("SAVESECURITYADAPT".equals(action)) {

								String securityAdapter = jsonAction.getString("securityAdapter").trim();

								boolean deviceSecurity = jsonAction.optBoolean("deviceSecurity");

								String storePasswordDuration = jsonAction.optString("storePasswordDuration");

								app.setSecurityAdapterType(securityAdapter);

								app.setDeviceSecurity(deviceSecurity);

								app.setStorePasswordDuration(storePasswordDuration);

								app.save(rapidServlet, rapidActionRequest, true);

								// add the application to the response
								result.put("message", "Security adapter saved");

							} else if ("SAVEACTIONS".equals(action)) {

								JSONArray jsonActionTypes = jsonAction.getJSONArray("actionTypes");

								ArrayList<String> actionTypes = new ArrayList<>();

								for (int i = 0; i < jsonActionTypes.length(); i++) {
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
								app.save(rapidServlet, rapidActionRequest, true);

								// add the message to the response
								result.put("message", actionTypes.size() + " actions");

							} else if ("SAVECONTROLS".equals(action)) {

								JSONArray jsonControlTypes = jsonAction.getJSONArray("controlTypes");

								ArrayList<String> controlTypes = new ArrayList<>();

								// loop the controls
								for (int i = 0; i < jsonControlTypes.length(); i++) {
									// get the control type
									String controlType = jsonControlTypes.getString(i).trim();
									// get the json for it
									JSONObject jsonControl = rapidServlet.getJsonControl(controlType);
									// if there was one
									if (jsonControl != null) {
										// add this type
										controlTypes.add(controlType);
										// look for any required action
										String requiredActionType = jsonControl.optString("requiredActionType", null);
										// if we got one
										if (requiredActionType != null) {
											// get the action types
											List<String> actionTypes = app.getActionTypes();
											// if it doesn't exist
											if (!actionTypes.contains(requiredActionType)) {
												// get the json for it
												JSONObject jsonActionType = rapidServlet.getJsonControl(requiredActionType);
												// if we got one
												if (jsonActionType != null) {
													// add it
													actionTypes.add(requiredActionType);
													// sort the list
													Collections.sort(actionTypes);
												}
											}
										}
									}
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
								app.save(rapidServlet, rapidActionRequest, true);

								// add the message to the response
								result.put("message", controlTypes.size() + " controls");

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
									ArrayList<String> versionNumbers = new ArrayList<>();
									// loop the versions
									for (String version : versions.keySet()) {
										versionNumbers.add(version);
									}
									// loop the versionNumbers
									for (String versionNumber: versionNumbers) {
										// get this version
										Application v = applications.get(app.getId(), versionNumber);
										// delete it
										v.delete(rapidServlet, rapidActionRequest);
									}
									// set the result message
									result.put("message", versionCount + " application version" + (versionCount == 1 ? "" : "s") + " deleted for " + app.getName());
								}

							} else if ("DUPAPP".equals(action)) {

								String version = jsonAction.getString("newVersion").trim();
								String title = jsonAction.optString("title").trim();
								String description = jsonAction.optString("description").trim();

								// use the application.copy routine (this updates the status and created time)
								Application dupApp = app.copy(rapidServlet, rapidActionRequest, app.getId(), version, false, false);

								// set the new title into the duplicate
								dupApp.setTitle(title);
								// set the new description
								dupApp.setDescription(description);

								// save the duplicate
								dupApp.save(rapidServlet, rapidActionRequest, false);

								// set the result message
								result.put("message", "Application " + app.getTitle() + " duplicated");
								result.put("id", dupApp.getId());
								result.put("version", dupApp.getVersion());

							} else if ("NEWVERSION".equals(action)) {

								// retrieve the inputs from the json
								String id = jsonAction.getString("appId").trim();
								String version = jsonAction.getString("newVersion").trim();
								String title = jsonAction.optString("title").trim();
								String description = jsonAction.optString("description").trim();

								// create a new application with our reusable, private method
								Application newApp = createApplication(rapidServlet, rapidActionRequest, id, version, title, "", false, "", description);

								// set the result message
								result.put("message", "Version " + newApp.getVersion() + " created for " + newApp.getTitle());

								// set the result appId
								result.put("appId", newApp.getId());

								// set the result version
								result.put("version", newApp.getVersion());

								// set the result message
								result.put("message", "Application " + app.getTitle() + " duplicated");
								result.put("id", newApp.getId());
								result.put("version", newApp.getVersion());

							} else if ("DELVERSION".equals(action)) {

								// delete the application version
								if (app != null) app.delete(rapidServlet, rapidActionRequest);
								// set the result message
								result.put("message", "Version " + app.getVersion() + " deleted");

							} else if ("NEWDBCONN".equals(action)) {

								// get the database connections
								List<DatabaseConnection> dbConns = app.getDatabaseConnections();
								// instantiate if null
								if (dbConns == null) dbConns = new ArrayList<>();

								// make the new database connection
								DatabaseConnection dbConn = new DatabaseConnection(
									servletContext,
									app,
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
								app.save(rapidServlet, rapidActionRequest, true);

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
										try { app.save(rapidServlet, rapidActionRequest, true); }
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
									// make the new SQL webservice
									webservice = new SQLWebservice(
										jsonAction.getString("name").trim()
									);
								} else if ("JavaWebservice".equals(type)) {
									// make the new Java class webservice
									webservice = new JavaWebservice(
										jsonAction.getString("name").trim()
									);
								}

								// if one was made
								if (webservice != null) {

									// add it to the collection
									app.getWebservices().add(webservice);

									// save the app
									app.save(rapidServlet, rapidActionRequest, true);

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
										app.save(rapidServlet, rapidActionRequest, true);

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
								security.addRole(rapidActionRequest, new Role(roleName, description));
								// set the result message
								result.put("message", "Role added");

							} else if ("DELROLE".equals(action)) {

								// get the role
								String role = jsonAction.getString("role").trim();
								// delete the role
								security.deleteRole(rapidActionRequest, role);
								// set the result message
								result.put("message", "Role deleted");

							} else if ("SAVEROLE".equals(action)) {

								// get the role
								String roleName = jsonAction.getString("role").trim();
								// get the description
								String roleDescription = jsonAction.getString("description").trim();
								// update the role
								security.updateRole(rapidActionRequest, new Role(roleName, roleDescription));
								// set the result message
								result.put("message", "Role details saved");

							} else if ("NEWPARAM".equals(action)) {

								// get the parameters
								List<Parameter> parameters = app.getParameters();

								// if there weren't any
								if (parameters == null) {
									// make some
									parameters = new ArrayList<>();
									// add them in
									app.setParameters(parameters);
								}

								// add a new parameter to the collection
								app.getParameters().add(new Parameter());

							} else if ("DELPARAM".equals(action)) {

								// get the index
								int index = jsonAction.getInt("index");

								// remove the parameter
								app.getParameters().remove(index);

								// save the app
								app.save(rapidServlet, rapidActionRequest, true);

								// set the result message
								result.put("message", "Parameter deleted");

							} else if ("SAVEPARAM".equals(action)) {

								int index = jsonAction.getInt("index");
								String name = jsonAction.getString("name");
								String description = jsonAction.getString("description");
								String value = jsonAction.getString("value");

								// fetch the parameter
								Parameter parameter = app.getParameters().get(index);

								// update it
								parameter.setName(name);
								parameter.setDescription(description);
								parameter.setValue(value);

								// save the app
								app.save(rapidServlet, rapidActionRequest, true);

								// set the result message
								result.put("message", "Parameter details saved");

							} else if ("NEWRESOURCE".equals(action)) {

								// get the resources
								Resources resources = app.getAppResources();
								// if null (could be from a previous version)
								if (resources == null) {
									// instantiate here
									resources = new Resources();
									// assign to the application
									app.setAppResources(resources);
								}

								// add a new parameter to the collection
								resources.add(new Resource());

							} else if ("DELRESOURCE".equals(action)) {

								// get the index
								int index = jsonAction.getInt("index");

								// remove the parameter
								app.getAppResources().remove(index);

								// save the app
								app.save(rapidServlet, rapidActionRequest, true);

								// set the result message
								result.put("message", "Resource deleted");

							} else if ("SAVERESOURCE".equals(action)) {

								int index = jsonAction.getInt("index");
								String name = jsonAction.getString("name");
								int type = jsonAction.getInt("type");
								String value = jsonAction.getString("value");

								// fetch the resource
								Resource resource = app.getAppResources().get(index);

								// update it
								resource.setName(name);
								resource.setType(type);
								resource.setContent(value);

								// save the app
								app.save(rapidServlet, rapidActionRequest, true);

								// set the result message
								result.put("message", "Resource details saved");

							} else if ("TESTDBCONN".equals(action)) {

								// get the index
								int index = jsonAction.getInt("index");

								// get the database connections
								List<DatabaseConnection> dbConns = app.getDatabaseConnections();

								// remember whether we found the connection
								boolean foundConnection = false;

								// check we have database connections
								if (dbConns != null) {
									// check the index we where given will retrieve a database connection
									if (index > -1 && index < dbConns.size()) {

										// retrieve the details from the json
										String driverClass = jsonAction.getString("driver").trim();
										String connectionString =app.insertParameters(servletContext, jsonAction.getString("connectionString").trim());
										String connectionAdapterClass = jsonAction.getString("connectionAdapter").trim();
										String userName = jsonAction.getString("userName").trim();
										String password = jsonAction.getString("password");

										// if the password wasn't set retrieve it via the connection index
										if ("********".equals(password)) password = dbConns.get(index).getPassword();

										// instantiate a DatabaseConnection object for this test
										DatabaseConnection dbconnection = new DatabaseConnection(
											servletContext,
											app,
											"test",
											driverClass,
											connectionString,
											connectionAdapterClass,
											userName,
											password
										);
										// get the adapter
										ConnectionAdapter connectionAdapter = dbconnection.getConnectionAdapter(servletContext, app);
										// get a data factory
										DataFactory dataFactory = new DataFactory(connectionAdapter);
										// get a connection
										Connection connection = dataFactory.getConnection(rapidActionRequest);
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
								File backup = new File (app.getBackupFolder(servletContext, false) + "/" + backupId);
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
								File backup = new File (app.getBackupFolder(servletContext, false) + "/" + backupId);
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
								File backupFolder = new File(app.getBackupFolder(servletContext, false) + "/" + backupId);

								// check it exists
								if (backupFolder.exists()) {

									// back up the current state of the application
									app.backup(rapidServlet, rapidActionRequest, false);


									// get the config folder
									File configFolder = new File(app.getConfigFolder(servletContext));

									// get the web folder
									File webFolder = new File(app.getWebFolder(servletContext));

									// get the backups folder
									File backupsFolder = new File(app.getBackupFolder(servletContext, false));



									// create a file object for restoring the config folder
								 	File configRestoreFolder = new File(Application.getConfigFolder(servletContext, app.getId(), "_restore"));

								 	List<String> ignoreList = new ArrayList<>();
								 	ignoreList.add("WebContent");

								 	// copy the backup into the application restore folder
									Files.copyFolder(backupFolder, configRestoreFolder, ignoreList);



								 	// create a file object for the web content backup folder (which is currently sitting under the application)
									File webBackupFolder = new File(backupFolder + "/WebContent");

									// create a file object for the web content restore folder
									File webRestoreFolder = new File(Application.getWebFolder(servletContext, app.getId(), "_restore"));

									// copy the web contents backup folder to the webcontent restore folder
									Files.copyFolder(webBackupFolder, webRestoreFolder);



									// get the backups destination folder
									File backupsRestoreFolder = new File(Application.getBackupFolder(servletContext, app.getId(), "_restore", false));

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
									app = Application.load(servletContext, applicationFile);

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

								String[] nameParts = backupId.split("_");

								// we'll try and find the page id (before the date) in newer backup files
								String pageId = "";
								// we'l also build the full name and look for like, like we used to
								String fullName = "";

								// there must be at least 3 bits: name, id (newer), date, user
								if (nameParts.length >= 3) {

									// retain i so we know where the date came in
									int i;

									// loop through the parts - we need them all, as we make the file name back
									for (i = 0; i < nameParts.length; i++) {

										// if this part is a date
										if (nameParts[i].matches("^\\d{8}$")) break;

										// add this part back to the name as we look for the date, allowing for skipping just one occurrence of the id
										fullName += nameParts[i] + "_";

									}

									// get the id
									pageId = nameParts[i - 1];
									// remove the final _ from when we built it back
									fullName = fullName.substring(0, fullName.length() - 1);

								} // name parts > 3

								// get the page with the id
								Page page = app.getPages().getPage(pageId);

								// if we couldn't find it with the id, use the fullName (for older pages)
								if (page == null) page = app.getPages().getPageByName(servletContext, fullName);

								// create a file object for the page
							 	File pageFile = new File(page.getFile(servletContext, app));

							 	// create a backup for the current state
								page.backup(rapidServlet, rapidActionRequest, app, pageFile, false);

								// get this backup file
								File backupFile = new File(app.getBackupFolder(servletContext, false) + "/" + backupId);

								// copy it over the current page file
								Files.copyFile(backupFile, pageFile);

								// load the page from the backup
								page = Page.load(servletContext, backupFile);

								// replace the current entry
								app.getPages().addPage(page, pageFile, app.getIsForm());

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
								app.save(rapidServlet, rapidActionRequest, false);

								// set the result message
								result.put("message", "Application backup max size updated to " + backupMaxSize);

							} else if ("SAVEPAGEBACKUPSIZE".equals(action)) {

								// get the max backup size
								int backupMaxSize = jsonAction.getInt("backupMaxSize");

								// pass it to the application
								app.setPageBackupsMaxSize(backupMaxSize);

								// save the application
								app.save(rapidServlet, rapidActionRequest, false);

								// set the result message
								result.put("message", "Page backup max size updated to " + backupMaxSize);

							} else if ("GETSETTINGS".equals(action)) {

								// get the list of settings for this application
								List<Settings> settings = Settings.list(servletContext, app);

								// if we have any settings
								if (settings != null) {
									// add fields to results
									result.put("fields", new JSONArray("['text','value']"));
									// make a json rows object
									JSONArray jsonRows = new JSONArray();
									// add any empty first entry
									jsonRows.put(new JSONArray("['','']"));
									// loop them
									for (Settings setting : settings) {
										// make a new row object
										JSONArray jsonRow = new JSONArray();
										// populate it
										jsonRow.put(setting.getName());
										jsonRow.put(setting.getId());
										// add it to the collection
										jsonRows.put(jsonRow);
									}
									// return all settings
									result.put("rows", jsonRows);
								}

							} else if ("NEWSETTINGS".equals(action)) {

								// get the name
								String name = jsonAction.getString("name");

								// make a new settings
								Settings settings = new Settings();
								// set it's id as the safe version of the name
								settings.setId(Files.safeName(name));
								// set it's name
								settings.setName(name);

								// copy current properties to settings object
								settings.setThemeType(app.getThemeType());
								settings.setStyles(app.getStyles());
								settings.setStatusBarColour(app.getStatusBarColour());
								settings.setStatusBarHighlightColour(app.getStatusBarHighlightColour());
								settings.setStatusBarTextColour(app.getStatusBarTextColour());
								settings.setStatusBarIconColour(app.getStatusBarIconColour());
								settings.setDatabaseConnections(app.getDatabaseConnections());
								settings.setParameters(app.getParameters());
								settings.setSecurityAdapterType(app.getSecurityAdapterType());

								// save the settings
								settings.save(servletContext, app);

							} else if ("DELSETTINGS".equals(action)) {

								// get the id
								String id = jsonAction.getString("id");

								// get this settings
								Settings settings = Settings.load(servletContext, app, id);

								// delete this settings (and it's file - the success will reload the settings)
								settings.delete(servletContext, app);

							} // second action type check

						} // user app admin role


						// actions that require app admin *or* app user role
						if (appAdmin || appUsers) {

							// get the securityAdapter type from the jsonAction
							String securityAdapterType = jsonAction.optString("securityAdapter", null);

							// get all of the available security adapters
							JSONArray jsonSecurityAdapters = rapidServlet.getJsonSecurityAdapters();

							//  Users without Rapid Admin need adapterIndex setting in all user security action types!
							if (!rapidAdmin) {

								// check we have some security adapters
								if (security != null && jsonSecurityAdapters != null) {
									// loop what we have
									for (int i = 0; i < jsonSecurityAdapters.length(); i++) {
										// get the item
										JSONObject jsonSecurityAdapter = jsonSecurityAdapters.getJSONObject(i);
										// if this is the type that came in
										if (security.getClass().getCanonicalName().equals(jsonSecurityAdapter.getString("class"))) {
											// add the adapter index as we know we don't have a drop down
											result.put("adapterIndex", i);
											// we're done
											break;
										}
									}
								}

							}

							if ("GETSEC".equals(action)) {

								// check we got one, might not have if drop down is not visible
								if (securityAdapterType != null) {

									// assume the current class has not been set
									String securityAdapterClass = "";

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

									// if it's different from what came in
									if (!securityAdapterClass.equals(security.getClass().getCanonicalName())) {
										// set the new security adapter
										app.setSecurityAdapter(servletContext, securityAdapterType);
										// read it back again
										security = app.getSecurityAdapter();
									}

								} // got security adapter type

								// if we got the security
								if (security != null) {

									// if we are a high-level Rapid Admin or high-level Rapid Users
									if (rapidAdmin || rapidUsers) {

										// get the roles
										Roles roles = security.getRoles(rapidActionRequest);

										// add the entire roles collection to the response
										result.put("roles", roles);

										// if we had some roles
										if (roles != null) {
											// prepapre a list of just the role names (not descriptions) - these go in the drop down for new roles that can be added
											List<String> roleNames = new ArrayList<>();
											// loop the roles
											for (Role role : roles) {
												// we need the RapidAdmin role to add RapidAdmin or RapidDesign
												if ((!com.rapid.server.Rapid.ADMIN_ROLE.equals(role.getName()) && !com.rapid.server.Rapid.DESIGN_ROLE.equals(role.getName())) || security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE))
													roleNames.add(role.getName());
											}
											// add the rolenames
											result.put("roleNames", roleNames);
										}

									} // high-level Rapid Admin check

									// get the users
									Users users = security.getUsers(rapidActionRequest);

									// if we got some add the users safely to the response (does not include password)
									if (users != null) result.put("users", getSafeUsersJSON(rapidRequest, security, rapidAdmin, users));

								} // got security

							} else if ("GETUSER".equals(action)) {

								// get the userName from the incoming json
								String userName = jsonAction.getString("userName");

								// derive whether this is the current user
								boolean currentUser = userName.toLowerCase().equals(rapidRequest.getUserName().toLowerCase());

								// now set the rapid request user to the user we want
								rapidActionRequest.setUserName(userName);

								// get the user
								User user = security.getUser(rapidActionRequest);

								// check user
								if (user != null) {

									// add the user name
									result.put("userName", userName);

									// add the user description
									result.put("description", user.getDescription());

									// add the user email
									result.put("email", user.getEmail());

									// set the default password mask
									String password = "********";

									// if the password is blank reflect this in what we send
									if ("".equals(user.getPassword())) password = "";

									// add a masked password
									result.put("password", password);

									// add isLocked status
									result.put("isLocked", user.getIsLocked());

									// add the device details
									result.put("deviceDetails", user.getDeviceDetails());

									// if we got one
									if (security != null) {

										// get the users roles
										List<String> roles = security.getUser(rapidActionRequest).getRoles();

										// add the users to the response
										result.put("roles", roles);

									} // got security

									// if this user record is for the logged in user
									result.put("currentUser", currentUser);

								} // user check

							} else if ("GETUSERS".equals(action)) {

								// add the current user
								result.put("currentUser", rapidRequest.getUserName());

								// if we got one
								if (security != null) {

									// get the users of the selected app
									Users users = security.getUsers(rapidActionRequest);

									// if we got some
									if (users != null) {
										// put the list of users
										result.put("users", getSafeUsersJSON(rapidActionRequest, security, rapidAdmin, users));
									}

									// send Rapid Master for extra copy to Master Rapid Admin users drop down
									if (rapidMaster) result.put("hasMaster", true);

								} // got security

							} else if ("NEWUSER".equals(action)) {

								// get the userName
								String userName = jsonAction.getString("userName").trim();
								// get the userDescription
								String description = jsonAction.optString("description","").trim();
								// get the email
								String email = jsonAction.optString("email");
								// get the password
								String password = jsonAction.getString("password");
								// get locked status
								boolean isLocked = jsonAction.isNull("isLocked")?false:"true".equalsIgnoreCase(jsonAction.getString("isLocked"));
								// get the device details
								String deviceDetails = jsonAction.optString("deviceDetails");
								// check for useAdmin - must have Rapid Admin to grant
								boolean useAdmin = jsonAction.optBoolean("useAdmin") && rapidAdmin;
								// check for useAdmin - must have useAdmin, Rapid Admin, and, Rapid Master to grant
								boolean useMaster = jsonAction.optBoolean("useMaster") && useAdmin && rapidAdmin && rapidMaster;
								// check for useDesign - must have Rapid Admin to grant
								boolean useDesign = jsonAction.optBoolean("useDesign") && rapidAdmin;
								// check for useUsers - must have Rapid Admin to grant
								boolean useUsers = jsonAction.optBoolean("useUsers") && rapidAdmin;

								// add the user
								security.addUser(rapidActionRequest, new User(userName, description, email, password, isLocked, deviceDetails));

								// update the Rapid Request to have the new user name
								rapidActionRequest.setUserName(userName);

								// add role if we were given one
								if (useAdmin) security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE);

								// add role if we were given one
								if (useMaster) security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.MASTER_ROLE);

								// add role if we were given one
								if (useDesign) security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.DESIGN_ROLE);

								// add role if we were given one
								if (useUsers) security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.USERS_ROLE);

								// set the result message
								result.put("message", "User added");

							} else if ("DELUSER".equals(action)) {

								// get the userName
								String userName = jsonAction.getString("userName").trim();
								// override the standard request user
								rapidActionRequest.setUserName(userName);
								// delete the user
								security.deleteUser(rapidActionRequest);
								// remove any of their page locks
								app.removeUserPageLocks(servletContext, userName);
								// set the result message
								result.put("message", "User deleted");

							} else if ("NEWUSERROLE".equals(action)) {

								// get the userName
								String userName = jsonAction.getString("userName").trim();
								// override the standard request user
								rapidActionRequest.setUserName(userName);
								// get the role
								String role = jsonAction.getString("role").trim();
								// add the user role - if RapidAdmin or RapidDesign, must have rapidAdmin
								if ((!com.rapid.server.Rapid.ADMIN_ROLE.equals(role) && !com.rapid.server.Rapid.DESIGN_ROLE.equals(role)) || rapidAdmin) security.addUserRole(rapidActionRequest, role);
								// set the result message
								result.put("message", "User role added");

							} else if ("DELUSERROLE".equals(action)) {

								// get the userName
								String userName = jsonAction.getString("userName").trim();
								// override the standard request user
								rapidActionRequest.setUserName(userName);
								// get the role
								String role = jsonAction.getString("role").trim();
								// add the user role
								security.deleteUserRole(rapidActionRequest, role);
								// set the result message
								result.put("message", "User role deleted");

							} else if ("SAVEUSER".equals(action)) {

								// get the userName of the user being changed
								String userName = jsonAction.getString("userName").trim();
								// override the standard request user
								rapidActionRequest.setUserName(userName);
								// get the description
								String description = jsonAction.getString("description").trim();
								// get the email
								String email = jsonAction.optString("email");
								// get the password
								String password = jsonAction.getString("password");
								// get locked status
								String isLocked = jsonAction.isNull("isLocked")?"false":jsonAction.getString("isLocked");
								// get the device details
								String deviceDetails = jsonAction.getString("deviceDetails");

								// get the user
								User user = security.getUser(rapidActionRequest);
								// if there was one
								if (user == null) {

									// set the result message
									result.put("message", "User not found");

								} else {

									// update the email
									user.setEmail(email);
									// update the description
									user.setDescription(description);
									// update the device details
									user.setDeviceDetails(deviceDetails);

									// update the locked status
									user.setIsLocked("true".equalsIgnoreCase(isLocked));

									// if password is different from the mask
									if ("********".equals(password)) {
										// just update the user
										security.updateUser(rapidActionRequest, user);
									} else {
										// get the old password
										String oldPassword = user.getPassword();
										// update the password
										user.setPassword(password);
										// update the user
										security.updateUser(rapidActionRequest, user);
										// update the session password as well if we are changing our own password (this is required especially when changing the rapid app password)
										if (user.getName().equals(rapidActionRequest.getSessionAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME))) rapidActionRequest.setUserPassword(password);
										// if there is an old password - should always be
										if (oldPassword != null) {
											// only if the new password is different from the old one
											if (!password.equals(oldPassword)) {
												// unlock user in this app
												user.setIsLocked(false);
												// get all applications
												Applications applications = rapidActionRequest.getRapidServlet().getApplications();
												// loop them
												for (String id : applications.getIds()) {
													// get their versions
													Versions versions = applications.getVersions(id);
													// loop the versions
													for (String version : versions.keySet()) {
														// get this version
														Application v = applications.get(id, version);
														// we have updated the password in the selected app already so no need to do it again
														if (!(app.getId().equals(v.getId()) && app.getVersion().equals(v.getVersion()))) {
															// get this app versions security adapter
															SecurityAdapter s = v.getSecurityAdapter();
															// recreate the rapidRequest with the selected version (so app parameters etc are available from the app in the rapidRequest)
															rapidActionRequest = new RapidRequest(rapidServlet, rapidActionRequest.getRequest(), v);
															// override the standard request user
															rapidActionRequest.setUserName(userName);
															// check the user had permission to the app with their old password
															if (s.checkUserPassword(rapidActionRequest, userName, oldPassword)) {
																// get this user
																User u = s.getUser(rapidActionRequest);
																// safety check for if we found one
																if (u == null) {
																	// log that it passed password check but can't be found
																	rapidActionRequest.getRapidServlet().getLogger().debug("User " + userName + " passed password check for " + app.getId() + "/" + app.getVersion() + " but now can't be found for password update");
																} else {
																	// set new user password
																	u.setPassword(password);
																	// unlock user in this app
																	u.setIsLocked(false);
																	// update user
																	s.updateUser(rapidActionRequest, u);
																}
															} // password match check
														} // ignore app version updated already
													} // version loop
												} // app id loop
											} // old password different from new
										} // old password null check
									} // password provided

									// if we are updating the rapid application we have used checkboxes for the Rapid Admin, Rapid Designer, and user manager roles
									if ("rapid".equals(app.getId())) {
										// get the value of rapidAdmin
										boolean useAdmin = jsonAction.optBoolean("useAdmin");
										// check the user was given the role
										if (useAdmin) {
											// add the role if the user doesn't have it already
											if (!security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE))
												security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE);
										} else {
											// remove the role
											security.deleteUserRole(rapidActionRequest, com.rapid.server.Rapid.ADMIN_ROLE);
										}
										// get the value of rapidAdmin
										boolean useMaster = jsonAction.optBoolean("useMaster");
										// check the user was given the role, can only be done by users who are masters already, unless they already have it
										if (useMaster && useAdmin) {
											// add the role if the user doesn't have it already
											if (!security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.MASTER_ROLE))
												security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.MASTER_ROLE);
										} else {
											// remove the role
											security.deleteUserRole(rapidActionRequest, com.rapid.server.Rapid.MASTER_ROLE);
										}
										// get the value of rapidDesign
										boolean useDesign = jsonAction.optBoolean("useDesign");
										// check the user was given the role
										if (useDesign) {
											// add the role if the user doesn't have it already
											if (!security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.DESIGN_ROLE))
												security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.DESIGN_ROLE);
										} else {
											// remove the role
											security.deleteUserRole(rapidActionRequest, com.rapid.server.Rapid.DESIGN_ROLE);
										}
										// get the value of rapidUsers
										boolean useUsers = jsonAction.optBoolean("useUsers");
										// check the user was given the role
										if (useUsers) {
											// add the role if the user doesn't have it already
											if (!security.checkUserRole(rapidActionRequest, com.rapid.server.Rapid.USERS_ROLE))
												security.addUserRole(rapidActionRequest, com.rapid.server.Rapid.USERS_ROLE);
										} else {
											// remove the role
											security.deleteUserRole(rapidActionRequest, com.rapid.server.Rapid.USERS_ROLE);
										}
									}

									// set the result message
									result.put("message", "User details saved");

								} // user check

							} else if ("CHECKPASSWORDCOMPLEXITY".equals(action)) {

								// get the password
								String password = jsonAction.getString("password");

								// check complexity
								boolean complexPass = security.checkPasswordComplexity(rapidActionRequest, password);

								// add check result
								result.put("complexPass", complexPass);
								// add whether rapid app
								result.put("rapidApp", "rapid".equals(app.getId()));

								// check passed
								if (complexPass) {

									// set the result message
									result.put("message", "Password is sufficiently complex");

								} else {

									// set the result message
									result.put("message", security.getPasswordComplexityDescription(rapidActionRequest, password));

								}

							} // action type check

						} // admin or users action type check

						// actions that require app design role
						if (appDesign) {

							if ("NEWPAGE".equals(action)) {

								String id = jsonAction.getString("id").trim();
								String name = jsonAction.getString("name").trim();
								String title = jsonAction.optString("title").trim();
								String description = jsonAction.optString("description").trim();

								// assume designer set id
								boolean nameIds = false;

								// this parameter existed against the entire system for just a single version of Rapid 2.4.1 before being moved to each application from 2.4.2
								if (Boolean.parseBoolean(servletContext.getInitParameter("pageNameIds")) || rapidActionRequest.getApplication().getPageNameIds()) nameIds = true;

								// check nameIds and set accordingly
								if (nameIds) id = name;

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
								Page delPage = app.getPages().getPage(rapidActionRequest.getRapidServlet().getServletContext(), id);
								// delete it if we got one
								if (delPage != null) delPage.delete(rapidServlet, rapidActionRequest, app);
								// set the result message
								result.put("message", "Page " + delPage.getName() + " delete");

							} // action type check

						} // design role check

					} else {

						throw new SecurityAdapaterException("User must have correct password for application");

					} // user app password

				} else {

					throw new SecurityAdapaterException("User must have correct password for Rapid Admin");

				} // rapid admin role and password check

			} // result check

			// send back the new app id for the callback load
			if (newAppId != null) result.put("id", newAppId);

		} else {

			// send back an error
			result.put("error", "Application not found");

		}

		return result;

	}

}
