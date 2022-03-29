/*

Copyright (C) 2022 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapid.actions.Database;
import com.rapid.actions.Logic;
import com.rapid.actions.Logic.Condition;
import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.ValueList;
import com.rapid.core.Applications.Versions;
import com.rapid.core.Control;
import com.rapid.core.Event;
import com.rapid.core.Page;
import com.rapid.core.Page.Lock;
import com.rapid.core.Page.RoleControlHtml;
import com.rapid.core.Page.Variable;
import com.rapid.core.Page.Variables;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Pages.PageHeaders;
import com.rapid.core.Theme;
import com.rapid.core.Workflow;
import com.rapid.core.Workflows;
import com.rapid.data.DataFactory;
import com.rapid.data.DataFactory.Parameters;
import com.rapid.data.DatabaseConnection;
import com.rapid.forms.FormAdapter;
import com.rapid.security.RapidSecurityAdapter;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.Role;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.security.SecurityAdapter.Users;
import com.rapid.utils.Bytes;
import com.rapid.utils.Files;
import com.rapid.utils.Strings;
import com.rapid.utils.XML;
import com.rapid.utils.ZipFile;

public class Designer extends RapidHttpServlet {

	private static final long serialVersionUID = 2L;

	// this byte buffer is used for reading the post data
	byte[] _byteBuffer = new byte[1024];

    public Designer() { super(); }

    // helper method to set the content type, write, and close the stream for common JSON output
    private void sendJsonOutput(HttpServletResponse response, String output) throws IOException {

    	// set response as json
		response.setContentType("application/json");

		// get a writer from the response
		PrintWriter out = response.getWriter();

		// write the output into the response
		out.print(output);

		// close the writer
		out.close();

		// send it immediately
		out.flush();

    }

    // print indentation
    private void printIndentation(PrintWriter out, int level) {
    	// loop level
    	for (int i = 0; i < level; i++) {
    		out.print("\t");
    	}
    }

    // print details of an action
    private void printAction(Action action, PrintWriter out, boolean details, int level) {

    	// print any indentation
    	printIndentation(out, level);

    	// retain the level at this point
    	int thislevel = level + 2;

    	// print the action
    	out.print("\t\tAction:\t" + action.getId() + "\t" + action.getType() + "\r\n");

		// create a sorted list
		List<String> sortedKeys = new ArrayList<>();
		// a map for the properties we're going to print
		Map<String, String> keyValues = new HashMap<>();

		// only required for details
		if (details) {
			// get the object properties
			Map<String, String> objectProperties = action.getProperties();

			// loop them
			for (String key : objectProperties.keySet()) {
				// add the key
				sortedKeys.add(key);
				// add the value
				keyValues.put(key, objectProperties.get(key));
			}
		}

		// get a JSONObject for this action which will turn the get/set properties into keys
		JSONObject jsonAction = new JSONObject(action);
		// get it's properties
		Iterator<String> keys = jsonAction.keys();

		// a map of child actions
		Map<String, List<Action>> keyChildActions = new HashMap<>();

		// loop them
		while (keys.hasNext()) {
			// get the next one
			String key = keys.next();
			// if not there already and details or actions
			if (!sortedKeys.contains(key) && (details || key.endsWith("Actions") || "actions".equals(key))) {
				// add the key
				sortedKeys.add(key);
				try {
					// if the key ends with actions
					if ((key.endsWith("Actions") || "actions".equals(key)) && action.getChildActions() != null && action.getChildActions().size() > 0) {
						// get the child actions
						JSONArray jsonChildActions = jsonAction.optJSONArray(key);
						// if we got some
						if (jsonChildActions != null) {

							// list of child actions for this key
							List<Action> childActions = new ArrayList<>();

							// loop the child actions
							for (int i = 0; i < jsonChildActions.length(); i++) {
								// get this one
								JSONObject jsonChildAction = jsonChildActions.getJSONObject(i);
								// get its id
								String childId = jsonChildAction.optString("id", null);
								// if there was one
								if (childId != null) {
									// loop them
									List<Action> as = action.getChildActions();
									for (Action childAction : as) {
										// print the child actions
										if (childId.equals(childAction.getId())) {
											// add the child action
											childActions.add(childAction);
											// we're done
											break;
										}
									}
								}
							}

							// add the child actions for this key
							keyChildActions.put(key, childActions);

						}
					} else {

						String str = JSONObject.valueToString(jsonAction.get(key));

						if (looksLikeJSONObject(str)) {

							JSONObject jsonObject = jsonAction.getJSONObject(key);
							str =  printJSONObject(jsonObject, thislevel).replaceAll("\r\n\t*\r\n", "\r\n");

						} else if (looksLikeJSONArray(str)) {

							JSONArray jsonArray = jsonAction.getJSONArray(key);
							str = printJSONArray(jsonArray, thislevel).replaceAll("\r\n\t*\r\n", "\r\n");
						}

						// add the value
						keyValues.put(key, str);
					}
				} catch (JSONException e) {}
			}
		}

		// sort the keys
		Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);

		// loop the sorted keys
		for (String key : sortedKeys) {
			// print it if not id, nor properties itself
			if (!"id".equals(key) && !"properties".equals(key) && !"type".equals(key) && !"childActions".equals(key)) {
				// print any indentation
		    	printIndentation(out, level);
		    	// print the key
				out.print("\t\t\t" + key);
				// get any child actions
				List<Action> childActions = keyChildActions.get(key);
				// if there are child actions for this key
				if (childActions != null) {
					// loop the child actions
					for (Action childAction : childActions) {
						// print a new line
						out.print("\r\n");
						// print the child actions
						printAction(childAction, out, details, thislevel);
					}
				} else {
					// print the value
					out.print("\t" + keyValues.get(key) + "\r\n");
				}
			}
		}

    }

    private static String printJSONObject(JSONObject jsonObject, int level) throws JSONException {

    	int thisLevel = level + 2;
    	String output = "";
    	Iterator<String> keys = jsonObject.keys();

    	while (keys.hasNext()) {

    		String key = keys.next();
    		output += "\r\n";
    		for (int i = 0; i < thisLevel; i++) output += "\t";

    		// we want line breaks and tabs in values printed as is (not escaped)
    		String value = jsonObject.get(key).toString();

    		// if it has line breaks - add one in front so it all appears on the margin
    		if (value != null && value.contains("\n")) value = "\r\n" + value;

    		if (looksLikeJSONObject(value)) {
    			value = printJSONObject(jsonObject.getJSONObject(key), thisLevel);
    		} else if (looksLikeJSONArray(value)) {
    			value = printJSONArray(jsonObject.getJSONArray(key), thisLevel);
    		}

    		output += key + "\t" + value;
    	}

    	return output;
	}

	private static String printJSONArray(JSONArray jsonArray, int level) throws JSONException {

    	int thisLevel = level + 2;
    	String output = "";

    	for (int i = 0; i < jsonArray.length(); i++) {
    		output += "\r\n";

    		String value = JSONObject.valueToString(jsonArray.get(i));

    		if (looksLikeJSONObject(value)) {
    			value = printJSONObject(jsonArray.getJSONObject(i), level);
    		} else if (looksLikeJSONArray(value)) {
    			value = printJSONArray(jsonArray.getJSONArray(i), level);
    		} else {
        		for (int j = 0; j < thisLevel; j++) output += "\t";
    		}

    		output += value;
    	}

    	return output;
	}

	private static boolean looksLikeJSONObject(String str) {
		return str.startsWith("{") && str.endsWith("}");
	}

	private static boolean looksLikeJSONArray(String str) {
		return str.startsWith("[") && str.endsWith("]");
	}

	// print details of events (used by page and controls)
    private void printEvents(List<Event> events, PrintWriter out, boolean details) {

    	// check events
		if (events!= null) {
			if (events.size() > 0) {
				// loop them
				for (Event event : events) {
					// check actions
					if (event.getActions() != null) {
						if (event.getActions().size() > 0) {
							// print the event
							out.print("\tEvent:\t" + event.getType() + "\r\n");
							// loop the actions
							for (Action action : event.getActions()) {
								// print the action details
								printAction(action,  out, details, 0);
							}
						}
					}
				}
			}
		}

    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// fake a delay for testing slow servers
		// try { Thread.sleep(3000); } catch (InterruptedException e) {}

		// get request as Rapid request
		RapidRequest rapidRequest = new RapidRequest(this, request);

		// retain the servlet context
		ServletContext context = rapidRequest.getServletContext();

		// get a reference to our logger
		Logger logger = getLogger();

		// if monitor is alive then log the event
		if(_monitor != null && _monitor.isAlive(context) && _monitor.isLoggingAll())
			_monitor.openEntry();

		// we will store the length of the item we are adding
		long responseLength = 0;

		try {

			logger.debug("Designer GET request : " + request.getQueryString());

			String actionName = rapidRequest.getActionName();

			String output = "";

			// get the rapid application
			Application rapidApplication = getApplications().get("rapid");

			// check we got one
			if (rapidApplication != null) {

				// get rapid security
				SecurityAdapter rapidSecurity = rapidApplication.getSecurityAdapter();

				// check we got some
				if (rapidSecurity != null) {

					// get the user name
					String userName = rapidRequest.getUserName();

					// get the rapid user
					User rapidUser = rapidSecurity.getUser(rapidRequest);

					// check permission
					if (rapidSecurity.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {

						// whether we're trying to avoid caching
				    	boolean noCaching = Boolean.parseBoolean(context.getInitParameter("noCaching"));

				    	if (noCaching) {

							// try and avoid caching
							response.setHeader("Expires", "Sat, 15 March 1980 12:00:00 GMT");

							// Set standard HTTP/1.1 no-cache headers.
							response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

							// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
							response.addHeader("Cache-Control", "post-check=0, pre-check=0");

							// Set standard HTTP/1.0 no-cache header.
							response.setHeader("Pragma", "no-cache");

				    	}

						if ("getSystemData".equals(actionName)) {

							// create a system data object
							JSONObject jsonSystemData = new JSONObject();

							// add the controls
							jsonSystemData.put("controls", getJsonControls());

							// add the actions
							jsonSystemData.put("actions", getJsonActions());

							// add the devices
							jsonSystemData.put("devices", getDevices());

							// add the local date format
							jsonSystemData.put("localDateFormat", getLocalDateFormat());

							// look for a controlAndActionSuffix
							String controlAndActionSuffix = context.getInitParameter("controlAndActionSuffix");
							// update to empty string if not present - this is the default and expected for older versions of the web.xml
							if (controlAndActionSuffix == null) controlAndActionSuffix = "";
							// add the controlAndActionPrefix
							jsonSystemData.put("controlAndActionSuffix", controlAndActionSuffix);

							// put into output string
							output = jsonSystemData.toString();

							// send output as json
							sendJsonOutput(response, output);

						} else if ("getApps".equals(actionName)) {

							// create a json array for holding our apps
							JSONArray jsonApps = new JSONArray();

							// get a sorted list of the applications
							for (String id : getApplications().getIds()) {

								// loop the versions
								for (String version : getApplications().getVersions(id).keySet()) {

									// get the this application version
									Application application = getApplications().get(id, version);

									// get the security
									SecurityAdapter security = application.getSecurityAdapter();

									// recreate the request in the name of this app
									RapidRequest appRequest = new RapidRequest(this, request, application);

									// check the users password
									if (security.checkUserPassword(appRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

										// check the users permission to design this application
										boolean designPermission = security.checkUserRole(appRequest, Rapid.DESIGN_ROLE);

										// if app is rapid do a further check
										if (designPermission && "rapid".equals(application.getId())) designPermission = security.checkUserRole(appRequest, Rapid.SUPER_ROLE);

										// if we got permssion - add this application to the list
										if (designPermission) {
											// create a json object
											JSONObject jsonApplication = new JSONObject();
											// add the details we want
											jsonApplication.put("id", application.getId());
											jsonApplication.put("name", application.getName());
											jsonApplication.put("title", application.getTitle());
											// add the object to the collection
											jsonApps.put(jsonApplication);
											// no need to check any further versions
											break;
										}

									}

								}

							}

							output = jsonApps.toString();

							sendJsonOutput(response, output);

						} else if ("getVersions".equals(actionName)) {

							// create a json array for holding our apps
							JSONArray jsonVersions = new JSONArray();

							// get the app id
							String appId = rapidRequest.getAppId();

							// get the versions
							Versions versions = getApplications().getVersions(appId);

							// if there are any
							if (versions != null) {

								// loop the list of applications sorted by id (with rapid last)
								for (Application application : versions.sort()) {

									// get the security
									SecurityAdapter security = application.getSecurityAdapter();

									// recreate the request in the name of this app
									RapidRequest appRequest = new RapidRequest(this, request, application);

									// check the users password
									if (security.checkUserPassword(appRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {

										// check the users permission to design this application
										boolean designPermission = application.getSecurityAdapter().checkUserRole(appRequest, Rapid.DESIGN_ROLE);

										// if app is rapid do a further check
										if (designPermission && "rapid".equals(application.getId())) designPermission = security.checkUserRole(appRequest, Rapid.SUPER_ROLE);

										// check the RapidDesign role is present in the users roles for this application
										if (designPermission) {

											// make a json object for this version
											JSONObject jsonVersion = new JSONObject();
											// add the app id
											jsonVersion.put("id", application.getId());
											// add the version
											jsonVersion.put("version", application.getVersion());
											// add the status
											jsonVersion.put("status", application.getStatus());
											// add the title
											jsonVersion.put("title", application.getTitle());
											// add a formAdapter if present
											if (application.getIsForm()) jsonVersion.put("isForm", true);
											// add whether to show control Ids
											jsonVersion.put("showControlIds", application.getShowControlIds());
											// add whether to show action Ids
											jsonVersion.put("showActionIds", application.getShowActionIds());
											// add the web folder so we can update the iframe style sheets
											jsonVersion.put("webFolder", Application.getWebFolder(application));

											// get the database connections
											List<DatabaseConnection> databaseConnections = application.getDatabaseConnections();
											// check we have any
											if (databaseConnections != null) {
												// make an object we're going to return
												JSONArray jsonDatabaseConnections = new JSONArray();
												// loop the connections
												for (DatabaseConnection databaseConnection : databaseConnections) {
													// add the connection name
													jsonDatabaseConnections.put(databaseConnection.getName());
												}
												// add the connections to the app
												jsonVersion.put("databaseConnections", jsonDatabaseConnections);
											}

											// make an object we're going to return
											JSONArray jsonRoles = new JSONArray();
											// retrieve the roles
											List<Role> roles = security.getRoles(appRequest);
											// check we got some
											if (roles != null) {
												// create a collection of names
												ArrayList<String> roleNames = new ArrayList<>();
												// copy the names in if non-null
												for (Role role : roles) if (role.getName() != null) roleNames.add(role.getName());
												// sort them
												Collections.sort(roleNames);
												// loop the sorted connections
												for (String roleName : roleNames) {
													// only add role if this is the rapid app, or it's not a special rapid permission
													if ("rapid".equals(application.getId()) || (!Rapid.ADMIN_ROLE.equals(roleName)&& !Rapid.SUPER_ROLE.equals(roleName) && !Rapid.USERS_ROLE.equals(roleName) && !Rapid.DESIGN_ROLE.equals(roleName))) jsonRoles.put(roleName);
												}
											}
											// add the security roles to the app
											jsonVersion.put("roles", jsonRoles);

											// get any value lists
											List<ValueList> valueLists = application.getValueLists();

											// add all of the value lists
											jsonVersion.put("valueLists", valueLists);

											// get all the possible json actions
											JSONArray jsonActions = getJsonActions();
											// make an array for the actions in this app
											JSONArray jsonAppActions = new JSONArray();
											// get the types used in this app
											List<String> actionTypes = application.getActionTypes();
											// if we have some
											if (actionTypes != null) {
												// loop the types used in this app
												for (String actionType : actionTypes) {
													// loop all the possible actions
													for (int i = 0; i < jsonActions.length(); i++) {
														// get an instance to the json action
														JSONObject jsonAction = jsonActions.getJSONObject(i);
														// if this is the type we've been looking for
														if (actionType.equals(jsonAction.getString("type"))) {
															// create a simple json object for thi action
															JSONObject jsonAppAction = new JSONObject();
															// add just what we need
															jsonAppAction.put("type", jsonAction.getString("type"));
															jsonAppAction.put("name", jsonAction.getString("name"));
															jsonAppAction.put("visible", jsonAction.optBoolean("visible", true));
															// add it to the app actions collection
															jsonAppActions.put(jsonAppAction);
															// start on the next app action
															break;
														}
													}
												}
											}
											// put the app actions we've just built into the app
											jsonVersion.put("actions", jsonAppActions);

											// get all the possible json controls
											JSONArray jsonControls = getJsonControls();
											// make an array for the controls in this app
											JSONArray jsonAppControls = new JSONArray();
											// get the control types used by this app
											List<String> controlTypes = application.getControlTypes();
											// if we have some
											if (controlTypes != null) {
												// loop the types used in this app
												for (String controlType : controlTypes) {
													// loop all the possible controls
													for (int i = 0; i < jsonControls.length(); i++) {
														// get an instance to the json control
														JSONObject jsonControl = jsonControls.getJSONObject(i);
														// if this is the type we've been looking for
														if (controlType.equals(jsonControl.getString("type"))) {
															// create a simple json object for this control
															JSONObject jsonAppControl = new JSONObject();
															// add just what we need
															jsonAppControl.put("type", jsonControl.getString("type"));
															jsonAppControl.put("name", jsonControl.getString("name"));
															jsonAppControl.put("image", jsonControl.optString("image"));
															jsonAppControl.put("category", jsonControl.optString("category"));
															jsonAppControl.put("canUserAdd", jsonControl.optString("canUserAdd"));
															// add it to the app controls collection
															jsonAppControls.put(jsonAppControl);
															// start on the next app control
															break;
														}
													}
												}
											}
											// put the app controls we've just built into the app
											jsonVersion.put("controls", jsonAppControls);

											// create a json object for the images
											JSONArray jsonImages = new JSONArray();
											// get the directory in which the control xml files are stored
											File dir = new File (application.getWebFolder(context));
											// if it exists (might not if deleted from the file system and apps not refreshed)
											if (dir.exists()) {

												// create a filter for finding image files
												FilenameFilter xmlFilenameFilter = new FilenameFilter() {
											    	@Override
													public boolean accept(File dir, String name) {
											    		return name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".svg");
											    	}
											    };

											    // an array to hold the images as they come out of the filter
											    List<String> images = new ArrayList<>();
												// loop the image files in the folder
												for (File imageFile : dir.listFiles(xmlFilenameFilter)) {
													images.add(imageFile.getName());
												}

												// sort the images
												Collections.sort(images);
												// loop the sorted images and add to json
												for (String image : images) jsonImages.put(image);
											}

											// put the images collection we've just built into the app
											jsonVersion.put("images", jsonImages);

											// create a json array for our style classes
											JSONArray jsonStyleClasses = new JSONArray();
											// get all of the possible style classes
											List<String> styleClasses = application.getStyleClasses();
											// if we had some
											if (styleClasses != null) {
												// loop and add to json array
												for (String styleClass : styleClasses) jsonStyleClasses.put(styleClass);
											}
											// put them into our application object
											jsonVersion.put("styleClasses", jsonStyleClasses);

											// look for any form adpter
											FormAdapter formAdapter = application.getFormAdapter();
											// if we got one
											if (formAdapter != null) {
												// get the type
												String formAdapterType = formAdapter.getType();
												// get the json form adpater details
												JSONArray jsonFormAdapters = getJsonFormAdapters();
												// if we got some
												if (jsonFormAdapters != null) {
													// loop them
													for (int i = 0; i < jsonFormAdapters.length(); i++) {
														// get this form adapter
														JSONObject jsonFormAdapter = jsonFormAdapters.getJSONObject(i);
														// if this is the one we want
														if (formAdapterType.equals(jsonFormAdapter.optString("type"))) {
															// add the properties to the version
															jsonVersion.put("canSaveForms", jsonFormAdapter.optBoolean("canSaveForms"));
															jsonVersion.put("canGeneratePDF", jsonFormAdapter.optBoolean("canGeneratePDF"));
															jsonVersion.put("canSupportIntegrationProperties", jsonFormAdapter.optBoolean("canSupportIntegrationProperties"));
															// we're done
															break;
														}
													}
												}
											}

											// put the app into the collection
											jsonVersions.put(jsonVersion);

										} // design permission

									} // check user password

								} // versions loop

							} // got versions check

							output = jsonVersions.toString();

							sendJsonOutput(response, output);

						} else if ("getPages".equals(actionName)) {

							Application application = rapidRequest.getApplication();

							if (application == null) {

								// send an empty object
								output = "{}";

							} else {

								JSONArray jsonPages = new JSONArray();

								String startPageId = "";
								Page startPage = application.getStartPage(context);
								if (startPage != null) startPageId = startPage.getId();

								// loop the page headers
								for (PageHeader pageHeader : application.getPages().getSortedPages()) {
									// get the page - yip this means that apps loaded in the designer load all of their pages
									Page page = application.getPages().getPage(context, pageHeader.getId());
									// create a simple json object for the page
									JSONObject jsonPage = new JSONObject();
									// add simple properties
									jsonPage.put("id", page.getId());
									jsonPage.put("name", page.getName());
									jsonPage.put("title", page.getTitle());
									jsonPage.put("label", page.getLabel());
									jsonPage.put("simple", page.getSimple());
									jsonPage.put("hideHeaderFooter", page.getHideHeaderFooter());

									/*
									// get a list of page session variables - now deprecated by page variables with the optional session storage
									List<String> pageSessionVariables = page.getSessionVariables();
									// add them if there are some
									if (pageSessionVariables != null) if (pageSessionVariables.size() > 0) 	jsonPage.put("sessionVariables", pageSessionVariables);
									*/

									// get page variables
									Variables pageVariables = page.getVariables();
									// add them if there are some
									if (pageVariables != null && pageVariables.size() > 0) jsonPage.put("variables", pageVariables);

									// assume we don't need to know page visibilty
									boolean includePageVisibiltyControls = false;
									// if there is a form adapter
									if (application.getFormAdapter() != null) {
										// set to true
										includePageVisibiltyControls = true;
										// add visibility conditions
										List<Condition> pageVisibilityConditions = page.getVisibilityConditions();
										// add them if there are some
										if (pageVisibilityConditions != null) if (pageVisibilityConditions.size() > 0) jsonPage.put("visibilityConditions", pageVisibilityConditions);
									}

									// assume no page is selected in the designer that we want dialogue controls for
									Boolean includeFromDialogue = false;
									// if this loadPages was from a save
									if (Boolean.parseBoolean(rapidRequest.getRequest().getParameter("fromSave"))) {
										// get the page from the rapidRequest
										Page designPage = rapidRequest.getPage();
										// if there was one
										if (designPage != null) {
											// get the pageId
											String pageId = page.getId();
											// if we are saving a page in the designer, we will want to includeFromDialogue on all of the others
											if (!pageId.equals(designPage.getId())) {
												// get the list of pages we can open a dialogue to on this page
												List<String> dialoguePageIds = page.getDialoguePageIds();
												// if designerPageId is provided and this page is different from the one we're loading in the designer
												if (dialoguePageIds != null) {
													// if the page in the designer is one this page can navigate to as a dialogue
													if (dialoguePageIds.contains(designPage.getId())) includeFromDialogue = true;
												}
											}
										}
									}
									// get map of other page controls we can access from this page - keep designer page id null to avoid dialogue controls and events
									JSONArray jsonControls = page.getOtherPageComponents(this, includePageVisibiltyControls, includeFromDialogue);
									// if we got some add to the page
									if (jsonControls != null) jsonPage.put("controls", jsonControls);
									// check if the start page and add property if so
									if (startPageId.equals(page.getId())) jsonPage.put("startPage", true);
									// add the page to the collection
									jsonPages.put(jsonPage);
								}

								// set the output to the collection turned into a string
								output = jsonPages.toString();

							} // application check

							sendJsonOutput(response, output);

						} else if ("getPage".equals(actionName)) {

							Application application = rapidRequest.getApplication();

							Page page = rapidRequest.getPage();

							if (page != null) {

								// assume we can't find the user
								String userDescription = "unknown";
								// get the user
								User user = application.getSecurityAdapter().getUser(rapidRequest);
								// if we had one and they have a description use it
								if (user != null) if (user.getDescription() != null) userDescription = user.getDescription();

								// remove any existing page locks for this user
								application.removeUserPageLocks(context, userName);

								// check the page lock (which removes it if it has expired)
								page.checkLock();

								// if there is no current lock add a fresh one for the current user
								if (page.getLock() == null)	page.setLock(new Lock(userName, userDescription, new Date()));

								// turn it into json
								JSONObject jsonPage = new JSONObject(page);

								// remove the bodyHtml property as it is rebuilt in the designer
								jsonPage.remove("htmlBody");
								// remove the rolesHtml property as it is rebuilt in the designer
								jsonPage.remove("rolesHtml");
								// remove allControls (the single all-control list) it is not required
								jsonPage.remove("allControls");
								// remove the otherPageControls property as it is sent with getPages
								jsonPage.remove("otherPageControls");
								// remove bodyStyleClasses property as it should be called classes
								jsonPage.remove("bodyStyleClasses");

								// add the bodyStyleClasses as classes array
								if (page.getBodyStyleClasses() != null) jsonPage.put("classes", page.getBodyStyleClasses().split(" "));

								// add a nicely formatted lock time
								if (page.getLock() != null && jsonPage.optJSONObject("lock") != null) {
									// get the date time formatter and format the lock date time
									String formattedDateTime = getLocalDateTimeFormatter().format(page.getLock().getDateTime());
									// add a special property to the json
									jsonPage.getJSONObject("lock").put("formattedDateTime", formattedDateTime);
								}

								// add the css
								jsonPage.put("css", page.getAllCSS(context, application));

								// add the device as page properties (even though we store this in the app)
								jsonPage.put("device", 1);
								jsonPage.put("zoom", 1);
								jsonPage.put("orientation", "P");

								// add the form page type
								jsonPage.put("formPageType", page.getFormPageType());

								// get any theme
								Theme theme = application.getTheme(context);
								// if there was one
								if (theme != null) {
									// check for headerHtmlDesigner
									if (theme.getHeaderHtmlDesigner() != null) {
										// add headerHtmlDesigner html
										jsonPage.put("headerHtml", theme.getHeaderHtmlDesigner());
									} else {
										// add header html
										jsonPage.put("headerHtml", theme.getHeaderHtml());
									}
									// check for footerHtmlDesigner
									if (theme.getFooterHtmlDesigner() != null) {
										// add footerHtmlDesigner html
										jsonPage.put("footerHtml", theme.getFooterHtmlDesigner());
									} else {
										// add footer html
										jsonPage.put("footerHtml", theme.getFooterHtml());
									}
								}

								// create an other pages object
								JSONObject jsonOtherPages = new JSONObject();
								// loop the page headers
								for (PageHeader pageHeader : application.getPages().getSortedPages()) {
									// get this page id
									String pageId = page.getId();
									// if we are loading a specific page and need to know any other components for it and this is not the destination page itself
									if (!pageId.equals(pageHeader.getId())) {
										// get the other page
										Page otherPage = application.getPages().getPage(context, pageHeader.getId());
										// get the list of pages we can open a dialogue to on this page
										List<String> dialoguePageIds = otherPage.getDialoguePageIds();
										// if designerPageId is provided and this page is different from the one we're loading in the designer
										if (dialoguePageIds != null) {
											// if the pagein the designer is one this page navigates to on a dialogue
											if (dialoguePageIds.contains(pageId)) {
												// get other page components for this page
												JSONArray jsonControls = otherPage.getOtherPageComponents(this, false, true);
												// if we got some
												if (jsonControls != null) {
													// if we got some
													if (jsonControls.length() > 0) {
														// create an other page object
														JSONObject jsonOtherPage = new JSONObject();
														// add the controls to the page
														jsonOtherPage.put("controls", jsonControls);
														// add the other page to the page collection
														jsonOtherPages.put(otherPage.getId(), jsonOtherPage);
													}
												}
											}
										}
									}
								}
								// if other pages objects add to page
								if (jsonOtherPages.length() > 0) jsonPage.put("otherPages", jsonOtherPages);

								// print it to the output
								output = jsonPage.toString();

								// send as json response
								sendJsonOutput(response, output);

							}

						} else if ("getFlows".equals(actionName)) {

							// the JSON array of workflows we are going to return
							JSONArray jsonFlows = new JSONArray();

							// the JSON workflow we are going to return
							JSONObject jsonFlow = new JSONObject();

							// give it an id and a name
							jsonFlow.put("id", "1");
							jsonFlow.put("name", "Test");

							// add it to the array
							jsonFlows.put(jsonFlow);

							// print it to the output
							output = jsonFlows.toString();

							// send as json response
							sendJsonOutput(response, output);

						} else if ("getFlowVersions".equals(actionName)) {

							// the JSON array of workflows we are going to return
							JSONArray jsonFlows = new JSONArray();

							// the JSON workflow we are going to return
							JSONObject jsonFlow = new JSONObject();

							// give it an id and a name
							jsonFlow.put("version", "1");
							jsonFlow.put("status", "0");

							// the JSON array of workflows we are going to return
							JSONArray jsonActions = new JSONArray();

							JSONArray jsonAllActions = getJsonActions();
							// loop all actions for now
							for (int i = 0; i < jsonAllActions.length(); i++) {
								// get all action
								JSONObject jsonAllAction =  jsonAllActions.getJSONObject(i);
								// if it is allowed in workflow
								if (jsonAllAction.optBoolean("canUseWorkflow")) {
									JSONObject jsonAction = new JSONObject();
									jsonAction.put("type", jsonAllActions.getJSONObject(i).getString("type"));
									jsonActions.put(jsonAction);
								}
							}

							jsonFlow.put("actions",jsonActions);

							// add it to the array
							jsonFlows.put(jsonFlow);

							// print it to the output
							output = jsonFlows.toString();

							// send as json response
							sendJsonOutput(response, output);

						} else if ("getFlow".equals(actionName)) {

							// the JSON workflow we are going to return
							JSONObject jsonFlow = new JSONObject();

							// give it an id and a name
							jsonFlow.put("id", "1");
							jsonFlow.put("name", "Test");

							// print it to the output
							output = jsonFlow.toString();

							// send as json response
							sendJsonOutput(response, output);

						} else if ("checkApp".equals(actionName)) {

							String appName = request.getParameter("name");

							if (appName != null) {

								// retain whether we have an app with this name
								boolean exists =  getApplications().exists(Files.safeName(appName));

								// set the response
								output = Boolean.toString(exists);
								// send response as json
								sendJsonOutput(response, output);

							}

						} else if ("checkVersion".equals(actionName)) {

							String appName = request.getParameter("name");
							String appVersion = request.getParameter("version");

							if (appName != null && appVersion != null ) {

								// retain whether we have an app with this name
								boolean exists =  getApplications().exists(Files.safeName(appName), Files.safeName(appVersion));

								// set the response
								output = Boolean.toString(exists);
								// send response as json
								sendJsonOutput(response, output);

							}

						} else if ("checkPage".equals(actionName)) {

							String pageName = request.getParameter("name");

							if (pageName != null) {

								// retain whether we have an app with this name
								boolean pageExists = false;

								// get the application
								Application application = rapidRequest.getApplication();

								if (application != null) {

									for (PageHeader page : application.getPages().getSortedPages()) {
										if (pageName.toLowerCase().equals(page.getName().toLowerCase())) {
											pageExists = true;
											break;
										}
									}

								}

								// set the output
								output = Boolean.toString(pageExists);
								// send response as json
								sendJsonOutput(response, output);

							}

						} else if ("checkWorkflow".equals(actionName)) {

							String name = request.getParameter("name");

							if (name != null) {

								// retain whether we have an app with this name
								boolean exists = false;

								// get the workflows
								Workflows workflows = getWorkflows();
								// look for this on
								Workflow workflow = workflows.get(Files.safeName(name));
								// if we got one
								if (workflow != null) exists = true;

								// set the output
								output = Boolean.toString(exists);
								// send response as json
								sendJsonOutput(response, output);

							}

						} else if ("pages".equals(actionName) || "questions".equals(actionName) || "text".equals(actionName) || "summary".equals(actionName) || "detail".equals(actionName)) {

							// set response as text
							response.setContentType("text/plain;charset=utf-8");

							// get a writer from the response
							PrintWriter out = response.getWriter();

							// get the application
							Application application = rapidRequest.getApplication();

							// get the page headers
							PageHeaders pageHeaders = application.getPages().getSortedPages();

							// get the root path
							String rootPath = context.getRealPath("/");

							// get the root file
							File root = new File(rootPath);

							// get a date/time formatter
							SimpleDateFormat df = getLocalDateTimeFormatter();

							// write some useful things at the top
							out.print("Server name:\t" + InetAddress.getLocalHost().getHostName() + "\r\n");
							out.print("Instance name:\t" + root.getName() + "\r\n");
							out.print("Rapid version:\t" + Rapid.VERSION + "\r\n");
							out.print("Date and time:\t" + df.format(new Date()) + "\r\n\r\n");

							out.print("Rapid " + actionName + " report:\r\n\r\n\r\n");

							// id
							out.print("Application id:\t" + application.getId() + "\r\n");
							// version
							out.print("Version:\t" + application.getVersion() + "\r\n");
							// name
							out.print("Name:\t" + application.getName() + "\r\n");
							// title
							out.print("Title:\t" + application.getTitle() + "\r\n");

							// app details
							if ("summary".equals(actionName) || "detail".equals(actionName)) {

								// safe created date
								if (application.getCreatedDate() != null) out.print("Created date:\t" + df.format(application.getCreatedDate()) + "\r\n");
								// safe created by
								if (application.getCreatedBy() != null) out.print("Created by:\t" + application.getCreatedBy() + "\r\n");
								// safe modified date
								if (application.getModifiedDate() != null) out.print("Modified date:\t" + df.format(application.getModifiedDate()) + "\r\n");
								// safe modified by
								if (application.getModifiedBy() != null) out.print("Modified by:\t" + application.getModifiedBy() + "\r\n");

								// description
								if (application.getDescription() != null && application.getDescription().trim().length() > 0) out.print("Description:\t" + application.getDescription() + "\r\n");
								// form
								if (application.getIsForm()) out.print("Form adapter:\t" + application.getFormAdapterType() + "\r\n");
								// theme
								out.print("Theme:\t" + application.getThemeType() + "\r\n");
							}

							// pages
							out.print("Pages:\t" + pageHeaders.size() + "\r\n");

							// double line break
							out.print("\r\n");

							// get any page id
							String pageId = request.getParameter("p");

							// loop the page headers
							for (PageHeader pageHeader : pageHeaders) {

								// get the page
								Page page = application.getPages().getPage(context, pageHeader.getId());

								// if a specific page has been asked for continue until it comes up
								if (pageId != null && !page.getId().equals(pageId)) continue;

								// get the label
								String label = page.getLabel();
								// if we got one
								if (label == null) {
									label = "";
								} else {
									if (label.length() > 0) label = " - " + label;
								}

								if ("questions".equals(actionName))  {
									// print the name and label
									out.print(page.getTitle());
									// get any visibility conditions
									List<Logic.Condition> visibilityConditions = page.getVisibilityConditions();
									// if we got some
									if (visibilityConditions != null && visibilityConditions.size() > 0) {
										out.print(" (");
										// loop them
										for (int i = 0; i < visibilityConditions.size(); i++) {
											// get the condition
											Logic.Condition condition = visibilityConditions.get(i);
											// get value 1
											Control control1 = application.getControl(context, condition.getValue1().getId());
											// if we got one print it's name
											if (control1 == null) {
												out.print(condition.getValue1().toString().replace("System.field/", ""));
											} else {
												out.print(control1.getName());
											}
											// print operation
											out.print(" " + condition.getOperation() + " ");
											// get control 2
											Control control2 = application.getControl(context, condition.getValue2().getId());
											// if we got one print it's name
											if (control2 == null) {
												out.print(condition.getValue2().toString().replace("System.field/", ""));
											} else {
												if (control2.getLabel() == null || control2.getLabel().isEmpty()) {
													out.print(control2.getName());
												} else {
													out.print(control2.getLabel());
												}
											}
											// if there are more
											if (i < visibilityConditions.size() - 1) out.print(" " + page.getConditionsType() + " ");
										}
										out.print(")");
									}
									// closing line break
									out.print("\r\n");
								} else if ("text".equals(actionName))  {
									// print the page name with some space around it
									out.print(page.getTitle() + "\r\n");
								}

								// page headers
								if ("pages".equals(actionName) || "detail".equals(actionName) || "summary".equals(actionName)) {

									// page id
									out.print("Page:\t" + page.getId() + "\r\n");
									// page title
									out.print("Title:\t" + page.getTitle() + "\r\n");
									// safe created date
									if (page.getCreatedDate() != null) out.print("Created date:\t" + df.format(page.getCreatedDate()) + "\r\n");
									// safe created by
									if (page.getCreatedBy() != null) out.print("Created by:\t" + page.getCreatedBy() + "\r\n");
									// safe modified date
									if (page.getModifiedDate() != null) out.print("Modified date:\t" + df.format(page.getModifiedDate()) + "\r\n");
									// safe modified by
									if (page.getModifiedBy() != null) out.print("Modified by:\t" + page.getModifiedBy() + "\r\n");

									// action summary
									if ("pages".equals(actionName) || "summary".equals(actionName)) {
										out.print("Actions:\t" + page.getAllActions().size() + "\r\n");
									}

									// print the number of controls
									out.print("Controls:\t" + page.getAllControls().size() + "\r\n");

									// events, action, and details
									if ("summary".equals(actionName)) {
										// print page events
										printEvents(page.getEvents(), out, false);
									}

									// events, action, and details
									if ("detail".equals(actionName)) {

										out.print("Description:\t" + page.getDescription() + "\r\n");
										out.print("Simple:\t" + page.getSimple() + "\r\n");
										out.print("HideHeaderFooter:\t" + page.getHideHeaderFooter() + "\r\n");

										// print page events
										printEvents(page.getEvents(), out, true);

									}

								}

								// check questions, summary, detail
								if ("questions".equals(actionName) || "text".equals(actionName) || "summary".equals(actionName) || "detail".equals(actionName)) {

									// get the controls
									List<Control> controls = page.getAllControls();

									// loop them
									for (Control control : controls) {

										// get the name
										String name = control.getName();

										// name null check
										if ((name != null && name.trim().length() > 0) || "summary".equals(actionName) || "detail".equals(actionName) || "text".equals(actionName)) {

											// get the label
											label = control.getLabel();
											// get the type
											String type = control.getType();

											// exclude panels, hidden values (except for questions), and datastores for summary
											if ("summary".equals(actionName) || "detail".equals(actionName) || (!type.contains("panel") && (!("hiddenvalue").equals(type) || "questions".equals(actionName)) && !("dataStore").equals(type))) {

												// if questions it's likely to be a form
												if ("questions".equals(actionName))  {

													// look for a form object
													String formObject = control.getProperty("formObject");

													// if there is a label but not a button, but radios are allowed
													if ((label != null && (!control.getType().contains("button") || control.getType().contains("radio"))) || formObject != null) {

														// use name if label null
														if (label == null) label = name;

														// print the label
														out.print("\t" + label);

														// if we got one
														if (formObject != null) {

															// Get form integration values
															String formObjectAttribute = control.getProperty("formObjectAttribute");
															String formObjectRole = control.getProperty("formObjectRole");
															String formObjectType = control.getProperty("formObjectType");
															String formObjectPartyNumber = control.getProperty("formObjectPartyNumber");
															String formObjectQuestionNumber = control.getProperty("formObjectQuestionNumber");
															String formObjectAddressNumber = control.getProperty("formObjectAddressNumber");
															String formObjectText = control.getProperty("formObjectText");

															if (formObject != null && !formObject.equals("")) {
																out.print(" (");
																if (formObject != null) out.print(formObject);
																if (!"other".equalsIgnoreCase(formObject))
																	if (formObjectRole != null) out.print(" - " + formObjectRole);
																if (formObjectPartyNumber != null) out.print(" - party: " + formObjectPartyNumber);
																if ("party".equals(formObject)) out.print(" " + formObjectAttribute);
																if ("contact".equals(formObject)) out.print(" " + formObjectType);
																if ("address".equals(formObject)) {
																	if (formObjectAddressNumber != null) out.print(" - address: " + formObjectAddressNumber);
																	if (formObjectType != null) out.print(" - " + formObjectType);
																	if (formObjectAttribute != null) out.print(" - " + formObjectAttribute);
																}
																if ("question".equals(formObject) || "other".equals(formObject))
																	if (formObjectQuestionNumber != null) out.print(" - question: " + formObjectQuestionNumber);
																if (formObjectText != null && formObjectText.length() > 0) out.print(" - '" + formObjectText + "'");
																out.print(")");
															}
														}
														out.print("\r\n");
													}

												} else if ("text".equals(actionName)) {

													// no buttons
													if (!control.getType().endsWith("button")) {

														// get the text
														String text = control.getProperty("text");

														// print the text if there was some
														if (text != null) {
															// trim it and remove any line breaks
															text = text.trim().replace("\r", "").replace("\n", "");
															// if we have some
															if (text.length() > 0) out.print(text + "\r\n");
														}

														// try responsivelabel if we don't have one yet
														if (label == null) label = control.getProperty("responsiveLabel");

														// print the label if there was some
														if (label != null && label.trim().length() > 0) out.print(label);

														// if any options
														String options = control.getProperty("options");

														// if we got some
														if (options != null) {

															out.print(" (");

															// read into JSON
															JSONArray jsonOptions = new JSONArray(options);

															// loop
															for (int i = 0; i < jsonOptions.length(); i++) {

																// get the option
																JSONObject JSONOption = jsonOptions.getJSONObject(i);

																// get the option's text
																out.print(JSONOption.getString("text"));

																// add a comma if one is required
																if (i < jsonOptions.length() - 1) out.print(", ");

															}

															out.print(")");

														}

														// print a line break if there was pritning above
														if (label != null && label.trim().length() > 0) out.print("\r\n");

														// if this is a grid
														if ("grid".equals(control.getType())) {

															out.print(control.getName() + " (");

															// get the columns
															JSONArray jsonColumns = new JSONArray(control.getProperty("columns"));

															// loop them
															for (int i = 0; i < jsonColumns.length(); i++) {

																// get the option
																JSONObject JSONOption = jsonColumns.getJSONObject(i);

																// get the option's text
																out.print(JSONOption.getString("title"));

																// add a comma if one is required
																if (i < jsonColumns.length() - 1) out.print(", ");

															}

															out.print(")\r\n");

														}

													}

												} else {

													// print the control details
													out.print("Control:\t" + control.getId() +"\t" + type + "\t");
													// name
													if (name != null && name.length() > 0) out.print(name);
													// label
													if (label != null && label.length() > 0) out.print("\t" + label);
													// line break
													out.print("\r\n");
												}

												// if summary
												if ("summary".equals(actionName)) printEvents(control.getEvents(), out, false);

												// if details
												if ("detail".equals(actionName)) {

													// get the properties
													Map<String, String> properties = control.getProperties();
													// get a list we'll sort for them
													List<String> sortedKeys = new ArrayList<>();
													// loop them
													for (String key : properties.keySet()) {
														// add to sorted list
														sortedKeys.add(key);
													}
													// sort them
													Collections.sort(sortedKeys);
													// loop them
													for (String key : sortedKeys) {
														// print the properties (but not the properties itself)
														if (!"properties".equals(key)) out.print(key + "\t" + properties.get(key) + "\r\n");
													}

													// print the event details
													printEvents(control.getEvents(), out, true);

												} // detail check

											} // exclusion check

										} // name check

									} // control loop

								} // report action check

								// add space after the page
								out.print("\r\n");

							} // page loop

							// close the writer
							out.close();

							// send it immediately
							out.flush();

						} else if ("export".equals(actionName)) {

							// get the application
							Application application = rapidRequest.getApplication();

							// check we've got one
							if (application != null) {

								// the file name is the app id, an under score and then an underscore-safe version
								String fileName = application.getId() + "_" + application.getVersion().replace("_", "-") + ".zip";

								// get the file for the zip we're about to create
								File zipFile = application.zip(this, rapidRequest, rapidUser, fileName);

								// set the type as a .zip
								response.setContentType("application/x-zip-compressed");

								// Shows the download dialog
								response.setHeader("Content-disposition","attachment; filename=" + fileName);

								// send the file to browser
								OutputStream out = response.getOutputStream();
								FileInputStream in = new FileInputStream(zipFile);
								byte[] buffer = new byte[1024];
								int length;
								while ((length = in.read(buffer)) > 0){
								  out.write(buffer, 0, length);
								}
								in.close();
								out.flush();

								// delete the .zip file
								zipFile.delete();

								output = "Zip file sent";

							} // got application

						} else if ("updateids".equals(actionName)) {

							// get a writer from the response
							PrintWriter out = response.getWriter();

							// check we have admin too
							if (rapidSecurity.checkUserRole(rapidRequest, Rapid.ADMIN_ROLE)) {

								// get the suffix
								String suffix = rapidRequest.getRapidServlet().getControlAndActionSuffix();

								// set response as text
								response.setContentType("text/text");

								// get the application
								Application application = rapidRequest.getApplication();

								// print the app name and version
								out.print("Application : " + application.getName() + "/" + application.getVersion() + "\n");

								// get the page headers
								PageHeaders pageHeaders = application.getPages().getSortedPages();

								// get the pages config folder
								File appPagesFolder = new File(application.getConfigFolder(context) + "/pages");

								// check it exists
								if (appPagesFolder.exists()) {

									// loop the page headers
									for (PageHeader pageHeader : pageHeaders) {

										// get the page
										Page page = application.getPages().getPage(context, pageHeader.getId());

										// print the page name and id
										out.print("\nPage : " + page.getName() + "/" + page.getId() + "\n\n");

										// loop the files
										for (File pageFile : appPagesFolder.listFiles()) {

											// if this is a page.xml file
											if (pageFile.getName().endsWith(".page.xml")) {

												// assume no id's found

												// read the copy to a string
												String pageXML = Strings.getString(pageFile);

												// get all page controls
												List<Control> controls = page.getAllControls();

												// loop controls
												for (Control control : controls) {

													// get old/current id
													String id = control.getId();

													// assume new id will be the same
													String newId = id;

													// drop suffix if starts with it
													if (newId.startsWith(suffix)) newId = newId.substring(suffix.length());

													// add suffix to end
													newId += suffix;

													// check if id in file
													if (pageXML.contains(id)) {

														// show old and new id
														out.print(id + " ---> " + newId + " found in page file " + pageFile + "\n");

														// replace
														pageXML = pageXML.replace(id, newId);

													}

												}

												// get all page actions
												List<Action> actions = page.getAllActions();

												// loop actions
												for (Action action : actions) {

													// get old/current id
													String id = action.getId();

													// assume new id will be the same
													String newId = id;

													// drop suffix if starts with it
													if (newId.startsWith(suffix)) newId = newId.substring(suffix.length());

													// add suffix to end if not there already
													if (!newId.endsWith("_" + suffix)) newId += suffix;

													// check if id in file
													if (pageXML.contains(id)) {

														// show old and new id
														out.print(id + " ---> " + newId + " found in page file " + pageFile + "\n");

														// replace
														pageXML = pageXML.replace(id, newId);

													}

												}

												// save it back
												Strings.saveString(pageXML, pageFile);

											} //page ending check

										} // page file loop

									} //page loop

									// get the application file
									File applicationFile = new File(application.getConfigFolder(context) + "/application.xml");

									// if it exists
									if (applicationFile.exists()) {

										// reload the application from file
										Application reloadedApplication = Application.load(context, applicationFile, true);

										// replace it into the applications collection
										getApplications().put(reloadedApplication);

									}


								} // app pages folder exists

							} else {

								// not authenticated
								response.setStatus(403);

								// say so
								out.print("Not authorised");

							}

							// close the writer
							out.close();

							// send it immediately
							out.flush();

						} else if ("getStyleClasses".equals(actionName)) {

							String a = request.getParameter("a");
							String v = request.getParameter("v");

							Application application = getApplications().get(a, v);
							List<String> classNames = application.getStyleClasses();

							JSONArray json = new JSONArray(classNames);
							output = json.toString();
							sendJsonOutput(response, output);

						} // action name check

					} else {

						// not authenticated
						response.setStatus(403);

					} // got design role

				} // rapidSecurity != null

			} // rapidApplication != null

			// log the response
			if (logger.isTraceEnabled()) {
				logger.trace("Designer GET response : " + output);
			} else {
				logger.debug("Designer GET response : " + output.length() + " bytes");
			}

			// add up the accumulated response data with the output
			responseLength += output.length();

			// if monitor is alive then log the event
			if(_monitor != null && _monitor.isAlive(context) && _monitor.isLoggingAll())
				_monitor.commitEntry(rapidRequest, response, responseLength);

		} catch (Exception ex) {

			// if monitor is alive then log the event
			if(_monitor != null && _monitor.isAlive(context) && _monitor.isLoggingExceptions())
				_monitor.commitEntry(rapidRequest, response, responseLength, ex.getMessage());

			logger.debug("Designer GET error : " + ex.getMessage(), ex);

			sendException(rapidRequest, response, ex);

		}

	}

	private Control createControl(JSONObject jsonControl) throws JSONException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {

		// instantiate the control with the JSON
		Control control = new Control(jsonControl);

		// look in the JSON for a validation object
		JSONObject jsonValidation = jsonControl.optJSONObject("validation");
		// add the validation object if we got one
		if (jsonValidation != null) control.setValidation(Control.getValidation(this, jsonValidation));

		// look in the JSON for an event array
		JSONArray jsonEvents = jsonControl.optJSONArray("events");
		// add the events if we found one
		if (jsonEvents != null) control.setEvents(Control.getEvents(this, jsonEvents));

		// look in the JSON for a styles array
		JSONArray jsonStyles = jsonControl.optJSONArray("styles");
		// if there were styles
		if (jsonStyles != null) control.setStyles(Control.getStyles(this, jsonStyles));

		// look in the JSON for any child controls
		JSONArray jsonControls = jsonControl.optJSONArray("childControls");
		// if there were child controls loop and create controls interatively
		if (jsonControls != null) {
			for (int i = 0; i < jsonControls.length(); i++) {
				control.addChildControl(createControl(jsonControls.getJSONObject(i)));
			}
		}

		// return the control we just made
		return control;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// get the rapid request
		RapidRequest rapidRequest = new RapidRequest(this, request);

		// retain the servlet context
		ServletContext context = rapidRequest.getServletContext();

		// get a reference to our logger
		Logger logger = getLogger();

		// if monitor is alive then log the event
		if (_monitor != null && _monitor.isAlive(context) && _monitor.isLoggingAll())
			_monitor.openEntry();

		// we will store the length of the item we are adding
		long responseLength = 0;

		// extra detail for the monitor log
		String monitorEntryDetails = null;

		try {

			// assume no output
			String output = "";

			// get the rapid application
			Application rapidApplication = getApplications().get("rapid");

			// check we got one
			if (rapidApplication != null) {

				// get rapid security
				SecurityAdapter rapidSecurity = rapidApplication.getSecurityAdapter();

				// check we got some
				if (rapidSecurity != null) {

					// get user name
					String userName = rapidRequest.getUserName();
					if (userName == null) userName = "";

					// check permission
					if (rapidSecurity.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {

						Application application = rapidRequest.getApplication();

						if (application != null) {

							// get the body bytes from the request
							byte[] bodyBytes = rapidRequest.getBodyBytes();

							if ("savePage".equals(rapidRequest.getActionName())) {

								String bodyString = new String(bodyBytes, "UTF-8");

								if (logger.isTraceEnabled()) {
									logger.trace("Designer POST request : " + request.getQueryString() + " body : " + bodyString);
								} else {
									logger.debug("Designer POST request : " + request.getQueryString() + " body : " + bodyString.length() + " bytes");
								}

								JSONObject jsonPage = new JSONObject(bodyString);

								// instantiate a new blank page
								Page newPage = new Page();

								// set page properties
								newPage.setId(jsonPage.optString("id"));
								newPage.setName(jsonPage.optString("name"));
								newPage.setTitle(jsonPage.optString("title"));
								newPage.setFormPageType(jsonPage.optInt("formPageType"));
								newPage.setLabel(jsonPage.optString("label"));
								newPage.setDescription(jsonPage.optString("description"));
								newPage.setSimple(jsonPage.optBoolean("simple"));
								newPage.setHideHeaderFooter(jsonPage.optBoolean("hideHeaderFooter"));

								// look in the JSON for an event array
								JSONArray jsonEvents = jsonPage.optJSONArray("events");
								// add the events if we found one
								if (jsonEvents != null) newPage.setEvents(Control.getEvents(this, jsonEvents));

								// look in the JSON for a styles array
								JSONArray jsonStyles = jsonPage.optJSONArray("styles");
								// if there were styles get and save
								if (jsonStyles != null) newPage.setStyles(Control.getStyles(this, jsonStyles));

								// look in the JSON for a style classes array
								JSONArray jsonStyleClasses = jsonPage.optJSONArray("classes");
								// if there were style classes
								if (jsonStyleClasses != null) {
									// start with empty string
									String styleClasses = "";
									// loop array and build classes list
									for (int i = 0; i < jsonStyleClasses.length(); i++) styleClasses += jsonStyleClasses.getString(i) + " ";
									// trim for good measure
									styleClasses = styleClasses.trim();
									// store if something there
									if (styleClasses.length() > 0) newPage.setBodyStyleClasses(styleClasses);
								}

								// if there are child controls from the page loop them and add to the pages control collection
								JSONArray jsonControls = jsonPage.optJSONArray("childControls");
								if (jsonControls != null) {
									for (int i = 0; i < jsonControls.length(); i++) {
										// get the JSON control
										JSONObject jsonControl = jsonControls.getJSONObject(i);
										// call our function so it can go iterative
										newPage.addControl(createControl(jsonControl));
									}
								}

								// if there are roles specified for this page
								JSONArray jsonUserRoles = jsonPage.optJSONArray("roles");
								if (jsonUserRoles != null) {
									List<String> userRoles = new ArrayList<>();
									for (int i = 0; i < jsonUserRoles.length(); i++) {
										// get the JSON role
										String jsonUserRole = jsonUserRoles.getString(i);
										// add to collection
										userRoles.add(jsonUserRole);
									}
									// assign to page
									newPage.setRoles(userRoles);
								}

								/*

								// look in the JSON for a sessionVariables array
								JSONArray jsonSessionVariables = jsonPage.optJSONArray("sessionVariables");
								// if we found one
								if (jsonSessionVariables != null) {
									List<String> sessionVariables = new ArrayList<>();
									for (int i = 0; i < jsonSessionVariables.length(); i++) {
										sessionVariables.add(jsonSessionVariables.getString(i));
									}
									newPage.setSessionVariables(sessionVariables);
								}

								*/

								// look in the JSON for a sessionVariables array
								JSONArray jsonPageVariables = jsonPage.optJSONArray("variables");
								// if we found one
								if (jsonPageVariables != null) {
									Variables pageVariables = new Variables();
									for (int i = 0; i < jsonPageVariables.length(); i++) {
										JSONObject jsonPageVariable = jsonPageVariables.getJSONObject(i);
										pageVariables.add(new Variable(jsonPageVariable.getString("name"), jsonPageVariable.optBoolean("session")));
									}
									newPage.setVariables(pageVariables);
								}

								// look in the JSON for a pageVisibilityRules array
								JSONArray jsonVisibilityConditions = jsonPage.optJSONArray("visibilityConditions");
								// if we found one
								if (jsonVisibilityConditions != null) {
									List<Condition> visibilityConditions = new ArrayList<>();
									for (int i = 0; i < jsonVisibilityConditions.length(); i++) {
										visibilityConditions.add(new Condition(jsonVisibilityConditions.getJSONObject(i)));
									}
									newPage.setVisibilityConditions(visibilityConditions);
								}

								// look in the JSON for a pageVisibilityRules array is an and or or (default to and)
								String jsonConditionsType = jsonPage.optString("conditionsType","and");
								// set what we got
								newPage.setConditionsType(jsonConditionsType);

								// retrieve the html body
								String htmlBody = jsonPage.optString("htmlBody");
								// if we got one trim it and retain in page
								if (htmlBody != null) newPage.setHtmlBody(htmlBody.trim());

								// look in the JSON for roleControlhtml
								JSONObject jsonRoleControlHtml = jsonPage.optJSONObject("roleControlHtml");
								// if we found some add it to the page
								if (jsonRoleControlHtml != null) newPage.setRoleControlHtml(new RoleControlHtml(jsonRoleControlHtml));

								// fetch a copy of the old page (if there is one)
								Page oldPage = application.getPages().getPage(context, newPage.getId());
								// if the page's name changed we need to remove it
								if (oldPage != null) {
									if (!oldPage.getName().equals(newPage.getName())) {
										oldPage.delete(this, rapidRequest, application);
									}
								}

								// save the new page to file
								long fileSize = newPage.save(this, rapidRequest, application, true);
								monitorEntryDetails = "" + fileSize;

								// get any pages collection (we're only sent it if it's been changed)
								JSONArray jsonPages = jsonPage.optJSONArray("pages");
								// if we got some
								if (jsonPages != null) {
									// make a new map for the page orders
									Map<String, Integer> pageOrders = new HashMap<>();
									// loop the page orders
									for (int i = 0; i < jsonPages.length(); i++) {
										// get the page id
										String pageId = jsonPages.getJSONObject(i).getString("id");
										// add the order to the map
										pageOrders.put(pageId, i);
									}
									// replace the application pageOrders map
									application.setPageOrders(pageOrders);
									// update the application start page (forms only)
									if (application.getIsForm() && jsonPages.length() > 0) application.setStartPageId(jsonPages.getJSONObject(0).getString("id"));
									// save the application and the new orders
									application.save(this, rapidRequest, true);
								}
								boolean jsonPageOrderReset = jsonPage.optBoolean("pageOrderReset");
								// empty the application pageOrders map so everything goes alphabetical
								if (jsonPageOrderReset) application.setPageOrders(null);

								// send a positive message
								output = "{\"message\":\"Saved!\"}";

								// set the response type to json
								response.setContentType("application/json");

							} else if ("testSQL".equals(rapidRequest.getActionName())) {

								// turn the body bytes into a string
								String bodyString = new String(bodyBytes, "UTF-8");

								JSONObject jsonQuery = new JSONObject(bodyString);

								JSONArray jsonInputs = jsonQuery.optJSONArray("inputs");

								JSONArray jsonOutputs = jsonQuery.optJSONArray("outputs");

								boolean childQuery = jsonQuery.optBoolean("childQuery");

								int databaseConnectionIndex = jsonQuery.optInt("databaseConnectionIndex",0);

								if (application.getDatabaseConnections() == null || databaseConnectionIndex > application.getDatabaseConnections().size() - 1) {

									throw new Exception("Database connection cannot be found.");

								} else {

									// get the sql
									String sql = jsonQuery.optString("SQL", null);

									if (sql == null || sql.isEmpty()) {

										throw new Exception("SQL must be provided");

									} else {

										// make a data factory for the connection with that index with auto-commit false
										DataFactory df = new DataFactory(context, application, databaseConnectionIndex, false);

										try {

											// assume no outputs
											int outputs = 0;

											// if got some outputs reduce the check count for any duplicates
											if (jsonOutputs != null) {
												// start with full number of outputs
												outputs = jsonOutputs.length();
												// retain fields
												List<String> fieldList = new ArrayList<>();
												// loop outputs
												for (int i = 0; i < jsonOutputs.length(); i++) {
													// look for a field
													String field = jsonOutputs.getJSONObject(i).optString("field", null);
													// if we got one
													if (field != null) {
														// check if we have it already
														if (fieldList.contains(field)) {
															// we do so reduce the control count by one
															outputs --;
														} else {
															// we don't have this field yet so remember
															fieldList.add(field);
														}
													}
												}
											}

											// trim the sql
											sql = sql.trim();

											// merge in any parameters
											sql = application.insertParameters(context, sql);

											// some jdbc drivers need the line breaks removing before they'll work properly - here's looking at you MS SQL Server!
											sql = sql.replace("\n", " ");

											// placeholder for parameters we may need
											Parameters parameters = null;

											// if the query has inputs
											if (jsonInputs != null) {

												// make a list of inputs
												List<String> inputs = new ArrayList<>();
												// make a parameters object to send
												parameters = new Parameters();
												// populate it with nulls
												for (int i = 0; i < jsonInputs.length(); i++) {
													// get this input
													JSONObject input = jsonInputs.getJSONObject(i);
													String inputId = input.getString("itemId");
													String field = input.getString("field");
													if (!field.isEmpty()) inputId += "." + input.getString("field");
													// add this input
													inputs.add(inputId);
													// add a null parameter for it
													parameters.addNull();
												}

												// get a parameter map which is where we have ?'s followed by number/name
												List<Integer> parameterMap = Database.getParameterMap(sql, inputs, application, context);

												// get the right-sized parameters (if we need them) using the map
												parameters = Database.mapParameters(parameterMap, parameters);

												// remove all ? numbers/names from the sql
												sql = Database.unspecifySqlSlots(sql);
											}

											// clean the sql for checking - it has been trimmed already
											String sqlCheck = sql.replace(" ", "").toLowerCase();

											// if it is more than 7 characters just trim it as "declare" is the longest we check for next - some parent statements are empty!
											if (sqlCheck.length() > 7) sqlCheck.substring(0, 7);

											// check outputs (unless a child query)
											if (outputs == 0 && !childQuery) {

												// check verb
												if (sqlCheck.startsWith("select")) {
													// select should have outputs
													throw new Exception("Select statement should have at least one output");
												} else {
													// not a select so just prepare the statement by way of testing it
													df.getPreparedStatement(rapidRequest, sql, parameters).execute();
												}

											} else {

												// check the verb
												if (sqlCheck.startsWith("select") || sqlCheck.startsWith("with")) {

													// get the prepared statement
													PreparedStatement ps = df.getPreparedStatement(rapidRequest, sql, parameters);

													// get the jdbc connection string
													String connString = df.getConnectionString();

													// execute the statement - required by Oracle, but not MS SQL, causes "JDBC: inconsistent internal state" for SQLite
													if (!connString.toLowerCase().contains("jdbc:sqlite:")) ps.execute();

													// get the meta data
													ResultSetMetaData rsmd = ps.getMetaData();

													// get the result columns
													int cols = rsmd.getColumnCount();

													// check there are enough columns for the outputs
													if (outputs > cols) throw new Exception(outputs + " outputs, but only " + cols + " column" + (cols > 1 ? "s" : "") + " selected");

													// check the outputs
													for (int i = 0; i < outputs; i++) {

														// get this output
														JSONObject jsonOutput = jsonOutputs.getJSONObject(i);

														// get the output field - it can be null or a blank space so this way we standardise on the blank
														String field = jsonOutput.optString("field","");

														// if there was one
														if (!"".equals(field)) {

															// lower case it
															field = field.toLowerCase();

															// assume we can't find a column for this output field
															boolean gotOutput = false;

															// loop the columns
															for (int j = 0; j < cols; j++) {

																// look for a query column with the same name as the field
																String sqlField = rsmd.getColumnLabel(j + 1).toLowerCase();

																// if we got one
																if (field.equals(sqlField)) {
																	// remember
																	gotOutput = true;
																	// we're done
																	break;
																}

															}

															// if we didn't get this output
															if (!gotOutput) {
																ps.close();
																df.close();
																throw new Exception("Field \"" + field + "\" from output " + (i + 1) + " is not present in selected columns");
															}

														}

													}

													// close the recordset
													ps.close();

												} else if (sqlCheck.startsWith("exec") || sqlCheck.startsWith("begin") || sqlCheck.startsWith("declare")) {

													//  get the prepared statement to check the parameters
													df.getPreparedStatement(rapidRequest, sql, parameters).execute();

												} else if (sqlCheck.startsWith("call") || sqlCheck.startsWith("{call")) {

													// execute the callable statement to check for errors
													df.executeCallableStatement(rapidRequest, sql, parameters);

												} else {

													// get the prepared statement to check the parameters
													df.getPreparedStatement(rapidRequest, sql, parameters).execute();

													// check the verb
													if (sqlCheck.startsWith("insert") || sqlCheck.startsWith("update") || sqlCheck.startsWith("delete")) {

														// loop the outputs
														for (int i = 0; i < outputs; i++) {

															// get the output
															JSONObject jsonOutput = jsonOutputs.getJSONObject(i);

															// get it's field, if present
															String field = jsonOutput.optString("field","");

															// if we got a field
															if (!"".equals(field)) {

																field = field.toLowerCase();

																if (!"rows".equals(field)) throw new Exception("Field \"" + field + "\" from output " + (i + 1) + " can only be \"rows\"");

															}

														}

													} else {

														throw new Exception("SQL statement not recognised");

													}
												}

												// rollback anything
												df.rollback();
												// close the data factory
												df.close();

											}

										} catch (Exception ex) {

											// if we had a df
											if (df != null) {
												// rollback anything
												df.rollback();
												// close the data factory
												df.close();
											}

											// rethrow to inform user
											throw ex;

										}

										// send a positive message
										output = "{\"message\":\"OK\"}";

										// set the response type to json
										response.setContentType("application/json");

									} // sql check

								} // connection check

							} else if ("uploadImage".equals(rapidRequest.getActionName()) || "import".equals(rapidRequest.getActionName())) {

								// get the content type from the request
								String contentType = request.getContentType();
								// get the position of the boundary from the content type
								int boundaryPosition = contentType.indexOf("boundary=");
								// derive the start of the meaning data by finding the boundary
								String boundary = contentType.substring(boundaryPosition + 10);
								// this is the double line break after which the data occurs
								byte[] pattern = {0x0D, 0x0A, 0x0D, 0x0A};
								// find the position of the double line break
								int dataPosition = Bytes.findPattern(bodyBytes, pattern );
								// the body header is everything up to the data
								String header = new String(bodyBytes, 0, dataPosition, "UTF-8");
								// find the position of the filename in the data header
								int filenamePosition = header.indexOf("filename=\"");
								// extract the file name
								String filename = header.substring(filenamePosition + 10, header.indexOf("\"", filenamePosition + 10));
								// find the position of the file type in the data header
								int fileTypePosition = header.toLowerCase().indexOf("type:");
								// extract the file type
								String fileType = header.substring(fileTypePosition + 6);

								if ("uploadImage".equals(rapidRequest.getActionName())) {

									// check the file type
									if (!fileType.equals("image/jpeg") && !fileType.equals("image/gif") && !fileType.equals("image/png") && !fileType.equals("image/svg+xml") && !fileType.equals("application/pdf")) throw new Exception("Unsupported file type");

									// get the web folder from the application
									String path = rapidRequest.getApplication().getWebFolder(context);
									// create a file output stream to save the data to
									FileOutputStream fos = new FileOutputStream (path + "/" +  filename);
									// write the file data to the stream
									fos.write(bodyBytes, dataPosition + pattern.length, bodyBytes.length - dataPosition - pattern.length - boundary.length() - 9);
									// close the stream
									fos.close();

									// log the file creation
									logger.debug("Saved image file " + path + filename);

									// create the response with the file name and upload type
									output = "{\"file\":\"" + filename + "\",\"type\":\"" + rapidRequest.getActionName() + "\"}";

								} else if ("import".equals(rapidRequest.getActionName())) {

									// check the file type
									if (!"application/x-zip-compressed".equals(fileType) && !"application/zip".equals(fileType)) throw new Exception("Unsupported file type");

									// get the name
									String appName = request.getParameter("name");

									// check we were given one
									if (appName == null) throw new Exception("Name must be provided");

									// get the version
									String appVersion = request.getParameter("version");

									// check we were given one
									if (appVersion == null) throw new Exception("Version must be provided");

									// look for keep settings
									boolean keepSettings = "true".equals(request.getParameter("settings"));

									// make the id from the safe and lower case name
									String appId = Files.safeName(appName).toLowerCase();

									// make the version from the safe and lower case name
									appVersion = Files.safeName(appVersion);

									// get application destination folder
									File appFolderDest = new File(Application.getConfigFolder(context, appId, appVersion));
									// get web contents destination folder
									File webFolderDest = new File(Application.getWebFolder(context, appId, appVersion));

									// look for an existing application of this name and version
									Application existingApplication = getApplications().get(appId, appVersion);
									// if we have an existing application
									if (existingApplication != null) {
										// back it up first
										existingApplication.backup(this, rapidRequest, false);
									}

									// get a file for the temp directory
									File tempDir = new File(context.getRealPath("/") + "WEB-INF/temp");
									// create it if not there
									if (!tempDir.exists()) tempDir.mkdir();

									// the path we're saving to is the temp folder
									String path = context.getRealPath("/") + "/WEB-INF/temp/" + appId + ".zip";
									// create a file output stream to save the data to
									FileOutputStream fos = new FileOutputStream (path);
									// write the file data to the stream
									fos.write(bodyBytes, dataPosition + pattern.length, bodyBytes.length - dataPosition - pattern.length - boundary.length() - 9);
									// close the stream
									fos.close();

									// log the file creation
									logger.debug("Saved import file " + path);

									// get a file object for the zip file
									File zipFile = new File(path);
									// load it into a zip file object
									ZipFile zip = new ZipFile(zipFile);
									// unzip the file
									zip.unZip();
									// delete the zip file
									zipFile.delete();

									// unzip folder (for deletion)
									File unZipFolder = new File(context.getRealPath("/") + "/WEB-INF/temp/" + appId);
									// get application folders
									File appFolderSource = new File(context.getRealPath("/") + "/WEB-INF/temp/" + appId + "/WEB-INF");
									// get web content folders
									File webFolderSource = new File(context.getRealPath("/") + "/WEB-INF/temp/" + appId + "/WebContent");

									// check we have the right source folders
									if (webFolderSource.exists() && appFolderSource.exists()) {

										// get application.xml file
										File appFileSource = new File (appFolderSource + "/application.xml");

										if (appFileSource.exists()) {

											// delete the appFolder if it exists
											if (appFolderDest.exists()) Files.deleteRecurring(appFolderDest);
											// delete the webFolder if it exists
											if (webFolderDest.exists()) Files.deleteRecurring(webFolderDest);

											// copy application content
											Files.copyFolder(appFolderSource, appFolderDest);

											// copy web content
											Files.copyFolder(webFolderSource, webFolderDest);

											try {

												// load the new application (but don't initialise, nor load pages)
												Application appNew = Application.load(context, new File (appFolderDest + "/application.xml"), false);

												// update application name
												appNew.setName(appName);

												// get the old id
												String appOldId = appNew.getId();

												// make the new id
												appId = Files.safeName(appName).toLowerCase();

												// update the id
												appNew.setId(appId);

												// get the old version
												String appOldVersion = appNew.getVersion();

												// make the new version
												appVersion = Files.safeName(appVersion);

												// update the version
												appNew.setVersion(appVersion);

												// update the created by
												appNew.setCreatedBy(userName);

												// update the created date
												appNew.setCreatedDate(new Date());

												// set the status to In development
												appNew.setStatus(Application.STATUS_DEVELOPMENT);

												// get the previous version
												Application appOld = getApplications().get(appOldId);

												// if we're keeping settings
												if (keepSettings) {

													// if we had one
													if (appOld != null) {

														// update database connections from old version
														appNew.setDatabaseConnections(appOld.getDatabaseConnections());

														// update parameters from old version
														appNew.setParameters(appOld.getParameters());

													} // old version check

												} // keepSettings

												// a map of actions that might be unrecognised in any of the pages
												Map<String,Integer> unknownActions = new HashMap<>();
												// a map of actions that might be unrecognised in any of the pages
												Map<String,Integer> unknownControls = new HashMap<>();

												// look for page files
												File pagesFolder = new File(appFolderDest.getAbsolutePath() + "/pages");
												// if the folder is there
												if (pagesFolder.exists()) {

													// create a filter for finding .page.xml files
													FilenameFilter xmlFilenameFilter = new FilenameFilter() {
												    	@Override
														public boolean accept(File dir, String name) {
												    		return name.toLowerCase().endsWith(".page.xml");
												    	}
												    };

												    // loop the .page.xml files
												    for (File pageFile : pagesFolder.listFiles(xmlFilenameFilter)) {

												    	// read the file into a string
												    	String fileString = Strings.getString(pageFile);

												        // prepare a new file string which will update into
												        String newFileString = null;

												        // if the old app did not have a version (for backwards compatibility)
												        if (appOldVersion == null) {

												        	// replace all properties that appear to have a url, and all created links - note the fix for cleaning up the double encoding
													        newFileString = fileString
													        	.replace("applications/" + appOldId + "/", "applications/" + appId + "/" + appVersion  + "/")
													        	.replace("~?a=" + appOldId + "&amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;")
													        	.replace("~?a=" + appOldId + "&amp;amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;");

												        } else {

												        	// replace all properties that appear to have a url, and all created links - note the fix for double encoding
													        newFileString = fileString
													        	.replace("applications/" + appOldId + "/" + appOldVersion + "/", "applications/" + appId + "/" + appVersion  + "/")
													        	.replace("~?a=" + appOldId + "&amp;v=" + appOldVersion + "&amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;")
														        .replace("~?a=" + appOldId + "&amp;amp;v=" + appOldVersion + "&amp;amp;", "~?a=" + appId + "&amp;v=" + appVersion + "&amp;");
												        }

												        // now open the string into a document
														Document pageDocument = XML.openDocument(newFileString);
														// get an xpath factory
														XPathFactory xPathfactory = XPathFactory.newInstance();
														XPath xpath = xPathfactory.newXPath();
														// an expression for any attributes with a local name of "type" - to find actions
														XPathExpression expr = xpath.compile("//@*[local-name()='type']");
														// get them
														NodeList nl = (NodeList) expr.evaluate(pageDocument, XPathConstants.NODESET);
														// get out system actions
														JSONArray jsonActions = getJsonActions();
														// if we found any elements with a type attribute and we have system actions
														if (nl.getLength() > 0 && jsonActions.length() > 0) {
															// a list of action types
															List<String> types = new ArrayList<>();
															// loop the json actions
															for (int i = 0; i < jsonActions.length(); i++) {
																// get the type
																String type = jsonActions.getJSONObject(i).optString("type").toLowerCase();
																// if don't have it already add it
																if (!types.contains(type)) types.add(type);
															}
															// loop the action attributes we found
															for (int i = 0; i < nl.getLength(); i++) {
																// get this attribute
																Attr a = (Attr) nl.item(i);
																// get the value of the type
																String type = a.getTextContent().toLowerCase();
																// remove any namespace
																if (type.contains(":")) type = type.substring(type.indexOf(":") + 1);
																// get the element the attribute is in
																Node n = a.getOwnerElement();
																// if we don't know about this action type
																if (!types.contains(type)) {

																	// assume this is the first
																	int unknownCount = 1;
																	// increment the count of unknown controls
																	if (unknownActions.containsKey(type)) unknownCount = unknownActions.get(type) + 1;
																	// store it
																	unknownActions.put(type, unknownCount);

																} // got type check
															} // attribute loop

														} // attribute and system action check

														// an expression for any controls to get their type
														expr = xpath.compile("//controls/properties/entry[key='type']/value");
														// get them
														nl = (NodeList) expr.evaluate(pageDocument, XPathConstants.NODESET);
														// get out system controls
														JSONArray jsonControls = getJsonControls();
														// if we found any elements with a type attribute and we have system actions
														if (nl.getLength() > 0 && jsonControls.length() > 0) {

															// a list of action types
															List<String> types = new ArrayList<>();
															// loop the json actions
															for (int i = 0; i < jsonControls.length(); i++) {
																// get the type
																String type = jsonControls.getJSONObject(i).optString("type").toLowerCase();
																// if don't have it already add it
																if (!types.contains(type)) types.add(type);
															}
															// loop the control elements we found
															for (int i = 0; i < nl.getLength(); i++) {
																// get this element
																Element e = (Element) nl.item(i);
																// get the value of the type
																String type = e.getTextContent().toLowerCase();
																// remove any namespace
																if (type.contains(":")) type = type.substring(type.indexOf(":") + 1);
																// if we don't know about this action type
																if (!types.contains(type)) {

																	// assume this is the first
																	int unknownCount = 1;
																	// increment the count of unknown controls
																	if (unknownControls.containsKey(type)) unknownCount = unknownControls.get(type) + 1;
																	// store it
																	unknownControls.put(type, unknownCount);

																} // got type check
															} // control loop

														} // control node loop

														// use the transformer to write to disk
														TransformerFactory transformerFactory = TransformerFactory.newInstance();
														Transformer transformer = transformerFactory.newTransformer();
														DOMSource source = new DOMSource(pageDocument);
														StreamResult result = new StreamResult(pageFile);
														transformer.transform(source, result);

												    } // page xml file loop

												} // pages folder check

												// if any items were removed
												if (unknownActions.keySet().size() > 0 || unknownControls.keySet().size() > 0) {

													// delete unzip folder
													Files.deleteRecurring(unZipFolder);

													// start error message
													String error = "Application can't be imported: ";

													// loop unknown actions
													for (String key : unknownActions.keySet()) {
														// get the number
														int count = unknownActions.get(key);
														// add message with correct plural
														error += count + " unrecognised action" + (count == 1 ? "" : "s") + " of type \"" + key + "\", ";
													}

													// loop unknown controls
													for (String key : unknownControls.keySet()) {
														// get the number
														int count = unknownControls.get(key);
														// add message with correct plural
														error += unknownControls.get(key) + " unrecognised control" + (count == 1 ? "" : "s") + " of type \"" + key + "\", ";
													}

													// remove the last comma
													error = error.substring(0, error.length() - 2);

													// throw the exception
													throw new Exception(error);

												}

												// now initialise with the new id but don't make the resource files (this reloads the pages and sets up the security adapter)
												appNew.initialise(context, false);

												// get the security for this application
												SecurityAdapter security = appNew.getSecurityAdapter();

												// if we're keeping settings, and there is an old app, and the security adapter allows users to be added
												if (keepSettings && appOld != null && SecurityAdapter.hasManageUsers(context, appNew.getSecurityAdapterType())) {

													// a Rapid request we'll use to delete users from the new app
													RapidRequest deleteRequest = new RapidRequest(this, request, appNew);

													// get all current users of the new app
													Users users = security.getUsers(rapidRequest);

													// if there are users
													if (users != null && users.size() > 0) {

														// remove all current users
														for (int i = 0; i < users.size(); i++) {
															// get this user
															User user = users.get(i);
															// set their name in the delete Rapid request
															deleteRequest.setUserName(user.getName());
															// delete them
															security.deleteUser(deleteRequest);
															// one less to do
															i--;
														}

													} // users check

													// get the old security adapter
													SecurityAdapter securityOld = appOld.getSecurityAdapter();

													// get any old users
													users = securityOld.getUsers(rapidRequest);

													// if there are users
													if (users != null && users.size() > 0) {

														// add old users to the new app
														for (User user : users) {

															// add the old user to the new app
															security.addUser(rapidRequest, user);

														}

													} else {

														// if we failed to get users using the specified security make the new "safe" Rapid security adapter for the new app
														security = new RapidSecurityAdapter(context, appNew);

														// add it to the new app
														appNew.setSecurityAdapter(context, "rapid");

													}

												} // new app allows adding users and there is an old app to get them from

												// make a rapid request in the name of the import application
												RapidRequest importRapidRequest = new RapidRequest(this, request, appNew);

												// assume we don't have the user
												boolean gotUser = false;
												// allow for the user to be outside the try/catch below
												User user = null;

												try {

													// get the current user's record from the adapter
													user = security.getUser(importRapidRequest);

												} catch (SecurityAdapaterException ex) {

													// log
													logger.error("Error getting user on app import : " + ex.getMessage(), ex);
													// set a new Rapid security adapter for the app (it will construct it)
													appNew.setSecurityAdapter(context, "rapid");
													// retrieve the security adapter we just asked to be made
													security = appNew.getSecurityAdapter();
												}

												// check the current user is present in the app's security adapter
												if (user != null) {
													// now check the current user password is correct too
													if (security.checkUserPassword(importRapidRequest, userName, rapidRequest.getUserPassword())) {
														// we have the right user with the right password
														gotUser = true;
													} else {
														// remove this user in case there is one with the same name but the password does not match
														security.deleteUser(importRapidRequest);
													}
												}

												// if we don't have the user
												if (!gotUser) {
													// get the current user from the Rapid application
													User rapidUser = rapidSecurity.getUser(importRapidRequest);
													// create a new user based on the Rapid user
													user = new User(rapidUser);
													// add the new user to this application
													security.addUser(importRapidRequest, user);
												}

												// add Admin roles for the new user if not present
												if (!security.checkUserRole(importRapidRequest, com.rapid.server.Rapid.ADMIN_ROLE))
													security.addUserRole(importRapidRequest, com.rapid.server.Rapid.ADMIN_ROLE);

												// add Design role for the new user if not present
												if (!security.checkUserRole(importRapidRequest, com.rapid.server.Rapid.DESIGN_ROLE))
													security.addUserRole(importRapidRequest, com.rapid.server.Rapid.DESIGN_ROLE);

												// reload the pages (actually clears down the pages collection and reloads the headers)
												appNew.getPages().loadpages(context);

												// save application (this will also initialise and rebuild the resources)
												long fileSize = appNew.save(this, rapidRequest, false);
												monitorEntryDetails = "" + fileSize;

												// add application to the collection
												getApplications().put(appNew);

												// delete unzip folder
												Files.deleteRecurring(unZipFolder);

												// send a positive message
												output = "{\"id\":\"" + appNew.getId() + "\",\"version\":\"" + appNew.getVersion() + "\"}";

											} catch (Exception ex) {

												// delete the appFolder if it exists
												if (appFolderDest.exists()) Files.deleteRecurring(appFolderDest);
												// if the parent is empty delete it too
												if (appFolderDest.getParentFile().list().length <= 1) Files.deleteRecurring(appFolderDest.getParentFile());

												// delete the webFolder if it exists
												if (webFolderDest.exists()) Files.deleteRecurring(webFolderDest);
												// if the parent is empty delete it too
												if (webFolderDest.getParentFile().list().length <= 1) Files.deleteRecurring(webFolderDest.getParentFile());

												// rethrow exception
												throw ex;

											}

										} else {

											// delete unzip folder
											Files.deleteRecurring(unZipFolder);

											// throw excpetion
											throw new Exception("Must be a valid Rapid " + Rapid.VERSION + " file");

										}

									} else {

										// delete unzip folder
										Files.deleteRecurring(unZipFolder);

										// throw excpetion
										throw new Exception("Must be a valid Rapid file");

									}

								}

							}

							if (logger.isTraceEnabled()) {
								logger.trace("Designer POST response : " + output);
							} else {
								logger.debug("Designer POST response : " + output.length() + " bytes");
							}

							PrintWriter out = response.getWriter();
							out.print(output);
							out.close();

							// record response size
							responseLength = output.length();

						} // got an application

					} // got rapid design role

				} // got rapid security

			} // got rapid application

			// if monitor is alive then log the event
			if (_monitor != null && _monitor.isAlive(context) && _monitor.isLoggingAll()) {
				_monitor.setDetails(monitorEntryDetails);
				_monitor.commitEntry(rapidRequest, response, responseLength);
			}

		} catch (Exception ex) {

			// if monitor is alive then log the event
			if (_monitor != null && _monitor.isAlive(context) && _monitor.isLoggingExceptions()) {
				_monitor.setDetails(monitorEntryDetails);
				_monitor.commitEntry(rapidRequest, response, responseLength, ex.getMessage());
			}

			getLogger().error("Designer POST error : ",ex);

			sendException(rapidRequest, response, ex);

		}

	}

}
