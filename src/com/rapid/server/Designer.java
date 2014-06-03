package com.rapid.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.security.RapidSecurityAdapter;
import com.rapid.security.SecurityAdapater;
import com.rapid.utils.Bytes;
import com.rapid.utils.Files;
import com.rapid.utils.Html;
import com.rapid.utils.ZipFile;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DataFactory.Parameters;

public class Designer extends RapidHttpServlet {
			
	private static final long serialVersionUID = 2L;
			
	// this byte buffer is used for reading the post data
	byte[] _byteBuffer = new byte[1024];
       
    public Designer() { super(); }
    
    private void sendJsonOutput(HttpServletResponse response, String output) throws IOException {
    	
    	// set response as json
		response.setContentType("application/json");
		
		// send the response
		PrintWriter out = response.getWriter();
		out.print(output);
		out.close();
    	
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		try {
			
			Thread.sleep(200);
			
			getLogger().debug("Designer GET request : " + request.getQueryString());
											
			String actionName = rapidRequest.getActionName();
			String output = "";
			
			// get the rapid application
			Application rapidApplication = getApplication("rapid");
			
			// check we got one
			if (rapidApplication != null) {
				
				// get rapid security
				SecurityAdapater rapidSecurity = rapidApplication.getSecurity();
				
				// check we got some
				if (rapidSecurity != null) {
					
					// get user name
					String userName = rapidRequest.getUserName();		
					if (userName == null) userName = "";
					
					// check permission
					if (rapidSecurity.checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE)) {
						
						// whether we're trying to avoid caching
				    	boolean noCaching = Boolean.parseBoolean(getServletContext().getInitParameter("noCaching"));
				    	
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
															
						if ("getControls".equals(actionName)) {
							
							output = getJsonControls().toString();
							
							sendJsonOutput(response, output);
							
						} else if ("getActions".equals(actionName)) {
							
							output = getJsonActions().toString();
							
							sendJsonOutput(response, output);
							
						} else if ("getApps".equals(actionName)) {
							
							JSONArray jsonApps = new JSONArray();
							
							String password = rapidRequest.getUserPassword();
							if (password == null) password = "";
							
							// loop the list of applications sorted by id (with rapid last)
							for (Application application : getSortedApplications()) {
								
								// check the users permission to design this application
								boolean designPermission = application.getSecurity().checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE);
													
								// check the RapidDesign role is present in the users roles for this application
								if (designPermission) {												
									
									// make a json object for this app
									JSONObject jsonApp = new JSONObject();
									// add the if
									jsonApp.put("id", application.getId());
									// add the title
									jsonApp.put("title", application.getTitle());
									// add whether to show control Ids
									jsonApp.put("showControlIds", application.getShowControlIds());
									// add whether to show action Ids
									jsonApp.put("showActionIds", application.getShowActionIds());
									
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
										jsonApp.put("databaseConnections", jsonDatabaseConnections);
									}
									
									// get the security roles
									SecurityAdapater security = application.getSecurity();
									// check we have any
									if (security != null) {
										// make an object we're going to return
										JSONArray jsonRoles = new JSONArray();
										// retrieve the role
										List<String> roles = security.getRoles(rapidRequest);														
										// check we got some
										if (roles != null) {								
											// sort them
											Collections.sort(roles);
											// loop the sorted connections
											for (String role : roles) {
												// if it's not a special Rapid role add it to the list we're sending
												if (!Rapid.ADMIN_ROLE.equals(role) && !Rapid.DESIGN_ROLE.equals(role)) jsonRoles.put(role);
											}
										}							
										// add the security roles to the app 
										jsonApp.put("roles", jsonRoles);
									}
															
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
													// add it to the app actions collection
													jsonAppActions.put(jsonAppAction);
													// start on the next app action
													break;
												}
											}
										}
									}						
									// put the app actions we've just built into the app
									jsonApp.put("actions", jsonAppActions);						
									
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
									jsonApp.put("controls", jsonAppControls);
									
									
									// create a json object for the images
									JSONArray jsonImages = new JSONArray();									
									// get the directory in which the control xml files are stored
									File dir = new File(getServletContext().getRealPath("/applications/" + application.getId()));									
									// create a filter for finding image files
									FilenameFilter xmlFilenameFilter = new FilenameFilter() {
								    	public boolean accept(File dir, String name) {
								    		return name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg");
								    	}
								    };								    
									// loop the image files in the folder
									for (File imageFile : dir.listFiles(xmlFilenameFilter)) {
										jsonImages.put(imageFile.getName());
									}
									// put the images collection we've just built into the app
									jsonApp.put("images", jsonImages);
									
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
									jsonApp.put("styleClasses", jsonStyleClasses);
									
									// put the app into the collection
									jsonApps.put(jsonApp);	
								}
																		
							}
							
							output = jsonApps.toString();
							
							sendJsonOutput(response, output);
							
						} else if ("getPages".equals(actionName)) {
							
							Application application = rapidRequest.getApplication();
							
							if (application != null) {
								
								JSONArray jsonPages = new JSONArray();
								
								String startPageId = "";
								Page startPage = application.getStartPage();
								if (startPage != null) startPageId = startPage.getId();
								
								for (Page page : application.getSortedPages()) {
									// create a simple json object for the page
									JSONObject jsonPage = new JSONObject();
									// add simple properties
									jsonPage.put("id", page.getId());
									jsonPage.put("title", page.getTitle());
									jsonPage.put("sessionVariables", page.getSessionVariables());
									// get a collection of other page controls in this page
									JSONArray jsonOtherPageControls = page.getOtherPageControls();
									// only add the property if there are some
									if (jsonOtherPageControls.length() > 0) jsonPage.put("controls", jsonOtherPageControls);
									// check if the start page and add property if so
									if (startPageId.equals(page.getId())) jsonPage.put("startPage", true);
									// add the page to the collection
									jsonPages.put(jsonPage);						
								}
								
								// set the output to the collection turned into a string			
								output = jsonPages.toString();
								
								sendJsonOutput(response, output);
								
							}
							
						} else if ("getPage".equals(actionName)) {
							
							Page page = rapidRequest.getPage();
							
							if (page != null) {
													
								// turn it into json
								JSONObject jsonPage = new JSONObject(page);
								
								// remove the bodyHtml property as there is no need to send it to the designer
								jsonPage.remove("htmlBody");
								// remove the otherPageControls property as there is no need to send it to the designer
								jsonPage.remove("otherPageControls");
								
								// print it to the output
								output = jsonPage.toString();
								// send as json response
								sendJsonOutput(response, output);
								
							}
															
						} else if ("checkApp".equals(actionName)) {
							
							String appName = request.getParameter("name");
							
							if (appName != null) {
								
								// retain whether we have an app with this name
								boolean appExists = false;
								
								// loop the list of applications sorted by id (with rapid last)
								for (Application application : getSortedApplications()) {
									if (appName.toLowerCase().equals(application.getName().toLowerCase())) {
										appExists = true;
										break;
									}
								}
													
								// set the response
								output = Boolean.toString(appExists);
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
									
									for (Page page : application.getSortedPages()) {
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
															
						} else if ("export".equals(actionName)) {
							
							// get the application
							Application application = rapidRequest.getApplication();
							
							// check we've got one
							if (application != null) {
																		
								// create the zip file
								application.zip(this, rapidRequest);
								
								// set the type as a .zip
								response.setContentType("application/x-zip-compressed");
								
								// Shows the download dialog
								response.setHeader("Content-disposition","attachment; filename=" + application.getId() + ".zip");
																		
								// get the file for the zip we're about to create
								File zipFile = new File(getServletContext().getRealPath("/WEB-INF/temp/" + application.getId() + ".zip"));
								
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
												
						} // export
						
					} // got design role
					
				} // rapidSecurity != null
				
			} // rapidApplication != null
																					
			// log the response
			getLogger().debug("Designer GET response : " + output);
																
		} catch (Exception ex) {
			
			getLogger().debug("Designer GET error : " + ex.getMessage(), ex); 
			
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


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		try {
			
			String output = "";		
			
			// read bytes from request body into our own byte array (this means we can deal with images) 
			InputStream input = request.getInputStream();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
			for (int length = 0; (length = input.read(_byteBuffer)) > -1;) outputStream.write(_byteBuffer, 0, length);			
			byte[] bodyBytes = outputStream.toByteArray();
												
			// get the rapid application
			Application rapidApplication = getApplication("rapid");
			
			// check we got one
			if (rapidApplication != null) {
				
				// get rapid security
				SecurityAdapater rapidSecurity = rapidApplication.getSecurity();
				
				// check we got some
				if (rapidSecurity != null) {
					
					// get user name
					String userName = rapidRequest.getUserName();		
					if (userName == null) userName = "";
					
					// check permission
					if (rapidSecurity.checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE)) {
						
						Application application = rapidRequest.getApplication();
						
						if (application != null) {
													
							if ("savePage".equals(rapidRequest.getActionName())) {
								
								String bodyString = new String(bodyBytes, "UTF-8");
								
								getLogger().debug("Designer POST request : " + request.getQueryString() + " body : " + bodyString);
								
								JSONObject jsonPage = new JSONObject(bodyString);	
								
								// instantiate a new blank page
								Page newPage = new Page();
								
								// set page properties
								newPage.setId(jsonPage.optString("id"));
								newPage.setName(jsonPage.optString("name"));
								newPage.setTitle(jsonPage.optString("title"));
								newPage.setDescription(jsonPage.optString("description"));
								
								// look in the JSON for the javascriptFiles array
								JSONArray jsonJavaScriptFiles = jsonPage.optJSONArray("javascriptFiles");
								// if we found one
								if (jsonJavaScriptFiles != null) {
									// make the array we're about to populate
									ArrayList<String> javaScriptFiles = new ArrayList<String>();
									// loop members
									for (int i = 0; i < jsonJavaScriptFiles.length(); i++) {
										// add string
										javaScriptFiles.add(jsonJavaScriptFiles.getString(i));
									}
									// assign to page property
									newPage.setJavascriptFiles(javaScriptFiles);
								}
								
								// look in the JSON for the cssFiles array
								JSONArray jsonCSSFiles = jsonPage.optJSONArray("cssFiles");
								// if we found one
								if (jsonCSSFiles != null) {
									// make the array we're about to populate
									ArrayList<String> cssFiles = new ArrayList<String>();
									// loop members
									for (int i = 0; i < jsonCSSFiles.length(); i++) {
										// add string
										cssFiles.add(jsonCSSFiles.getString(i));
									}
									// assign to page property
									newPage.setCssFiles(cssFiles);
								}
													
								// look in the JSON for an event array
								JSONArray jsonEvents = jsonPage.optJSONArray("events");
								// add the events if we found one
								if (jsonEvents != null) newPage.setEvents(Control.getEvents(this, jsonEvents));
										
								// look in the JSON for a styles array
								JSONArray jsonStyles = jsonPage.optJSONArray("styles");
								// if there were styles
								if (jsonStyles != null) newPage.setStyles(Control.getStyles(this, jsonStyles));
																
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
									ArrayList<String> userRoles = new ArrayList<String>(); 
									for (int i = 0; i < jsonUserRoles.length(); i++) {
										// get the JSON role
										String jsonUserRole = jsonUserRoles.getString(i);
										// add to collection
										userRoles.add(jsonUserRole);
									}
									// assign to page
									newPage.setRoles(userRoles);
								}
								
								// look in the JSON for a sessionVariables array
								JSONArray jsonSessionVariables = jsonPage.optJSONArray("sessionVariables");
								// if we found one
								if (jsonSessionVariables != null) {
									ArrayList<String> sessionVariables = new ArrayList<String>();
									for (int i = 0; i < jsonSessionVariables.length(); i++) {
										sessionVariables.add(jsonSessionVariables.getString(i));
									}
									newPage.setSessionVariables(sessionVariables);
								}
								
								// retrieve the html body
								String htmlBody = jsonPage.optString("htmlBody");
								// if we got one trim it and retain in page
								if (htmlBody != null) newPage.setHtmlBody(htmlBody.trim());
								
								// look in the JSON for rolehtml
								JSONArray jsonRolesHtml = jsonPage.optJSONArray("rolesHtml");
								// if we found some
								if (jsonRolesHtml != null) {
									// instantiate the roles html collection
									ArrayList<Page.RoleHtml> rolesHtml = new ArrayList<Page.RoleHtml>();
									// loop the entries
									for (int i =0; i < jsonRolesHtml.length(); i++) {
										// get the entry
										JSONObject jsonRoleHtml = jsonRolesHtml.getJSONObject(i);
										// retain the html
										String html = jsonRoleHtml.optString("html");
										// pretty print and trim it if there is one
										if (html != null) html = Html.getPrettyHtml(html).trim();
										// create an array to hold the roles
										ArrayList<String> roles = new ArrayList<String>(); 
										// get the roles
										JSONArray jsonRoles = jsonRoleHtml.optJSONArray("roles");
										// if we got some
										if (jsonRoles != null) {
											// loop them
											for (int j = 0; j < jsonRoles.length(); j++) {
												// get the role
												String role = jsonRoles.getString(j);
												// add it to the roles collections
												roles.add(role);
											}
										}
										// create and add a new roleHtml  object
										rolesHtml.add(new Page.RoleHtml(roles, html));
									}
									// add it to the page
									newPage.setRolesHtml(rolesHtml);
								}
								
								// fetch a copy of the old page (if there is one)
								Page oldPage = application.getPage(newPage.getId());
								// if the page's name changed we need to remove it
								if (oldPage != null) {
									if (!oldPage.getName().equals(newPage.getName())) {
										oldPage.delete(this, rapidRequest);
									}
								}
																
								// save the new page to file
								newPage.save(this, rapidRequest);					
								
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
													
								Parameters parameters = new Parameters();
								
								if (jsonInputs != null) {
								
									for (int i = 0; i < jsonInputs.length(); i++) parameters.addNull();
									
								}
								
								DatabaseConnection databaseConnection = application.getDatabaseConnections().get(jsonQuery.optInt("databaseConnectionIndex",0));
								
								ConnectionAdapter ca = databaseConnection.getConnectionAdapter(getServletContext());			
								
								DataFactory df = new DataFactory(ca);
								
								int outputs = 0;
								
								if (jsonOutputs != null) outputs = jsonOutputs.length();
																												
								if (outputs == 0) {
									
									df.getPreparedStatement(rapidRequest, jsonQuery.getString("SQL"), parameters);
															
								} else {
									
									ResultSet rs = df.getPreparedResultSet(rapidRequest, jsonQuery.getString("SQL"), parameters);
									
									ResultSetMetaData rsmd = rs.getMetaData();
									
									int cols = rsmd.getColumnCount();
									
									if (outputs > cols) throw new Exception(outputs + " outputs, but only " + cols + " column" + (cols > 1 ? "s" : "") + " selected");
									
									for (int i = 0; i < outputs; i++) {
																								
										JSONObject jsonOutput = jsonOutputs.getJSONObject(i);
										
										String field = jsonOutput.optString("field","");
										
										if (!"".equals(field)) {
											
											field = field.toLowerCase();
										
											boolean gotOutput = false;
											
											for (int j = 0; j < cols; j++) {
												
												String sqlField = rsmd.getColumnLabel(j + 1).toLowerCase();
												
												if (field.equals(sqlField)) {
													gotOutput = true;
													break;
												}
												
											}
											
											if (!gotOutput) throw new Exception("Field \"" + field + "\" from output " + (i + 1) + " is not present in selected columns");
											
										}
										
									}
															
								}
																		
								// send a positive message
								output = "{\"message\":\"Ok\"}";
								
								// set the response type to json
								response.setContentType("application/json");
								
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
									if (!fileType.equals("image/jpeg") && !fileType.equals("image/gif") && !fileType.equals("image/png")) throw new Exception("Unsupported file type");
									
									// the path we're saving to is the applications WebContent folder
									String path = getServletContext().getRealPath("/applications/" + application.getId()) + "/";
									// create a file output stream to save the data to
									FileOutputStream fos = new FileOutputStream (path + filename);
									// write the file data to the stream
									fos.write(bodyBytes, dataPosition + pattern.length, bodyBytes.length - dataPosition - pattern.length - boundary.length() - 9);
									// close the stream
									fos.close();
									
									// log the file creation
									getLogger().debug("Saved image file " + path + filename);
									
									// create the response with the file name and upload type
									output = "{\"file\":\"" + filename + "\",\"type\":\"" + rapidRequest.getActionName() + "\"}";
									
								} else if ("import".equals(rapidRequest.getActionName())) {
									
									// check the file type
									if (!"application/x-zip-compressed".equals(fileType) && !"application/zip".equals(fileType)) throw new Exception("Unsupported file type");
									
									// get the name
									String appName = request.getParameter("name");
									
									// check we were given one
									if (appName == null) throw new Exception("Name must be provided");
									
									// make the name file safe and lower case
									appName = Files.safeName(appName).toLowerCase();
									
									// get a file for the temp directory
									File tempDir = new File(getServletContext().getRealPath("/WEB-INF/temp"));
									// create it if not there
									if (!tempDir.exists()) tempDir.mkdir();
									
									// the path we're saving to is the temp folder
									String path = getServletContext().getRealPath("/WEB-INF/temp/" + appName + ".zip");
									// create a file output stream to save the data to
									FileOutputStream fos = new FileOutputStream (path);
									// write the file data to the stream
									fos.write(bodyBytes, dataPosition + pattern.length, bodyBytes.length - dataPosition - pattern.length - boundary.length() - 9);
									// close the stream
									fos.close();
									
									// log the file creation
									getLogger().debug("Saved import file " + path);
									
									// get a file object for the zip file
									File zipFile = new File(path);
									// unzip the file
									ZipFile zip = new ZipFile(zipFile);
									zip.unZip();
									// delete the zip file
									zipFile.delete();
									
									// unzip folder (for deletion)
									File unZipFolder = new File(getServletContext().getRealPath("/WEB-INF/temp/" + appName));
									// get application folders
									File appFolderSource = new File(getServletContext().getRealPath("/WEB-INF/temp/" + appName + "/WEB-INF"));						
									// get web content folders
									File webFolderSource = new File(getServletContext().getRealPath("/WEB-INF/temp/" + appName + "/WebContent" ));																		
																		
									// check we have the right source folders
									if (webFolderSource.exists() && appFolderSource.exists()) {
									
										// get application destination folder
										File appFolderDest = new File(getServletContext().getRealPath("/WEB-INF/applications/" + appName));
										// get web contents destination folder
										File webFolderDest = new File(getServletContext().getRealPath("/applications/" + appName));							
										// get application.xml file
										File appFileSource = new File (appFolderSource + "/application.xml");
										
										if (appFileSource.exists()) {
											
											// copy web content
											Files.copyFolder(webFolderSource, webFolderDest);
											
											// copy application content
											Files.copyFolder(appFolderSource, appFolderDest);
																											
											// load the new application (but do not generate the resources files)
											Application appNew = Application.load(getServletContext(), new File (appFolderDest + "/application.xml"), false);
															
											// get the old id
											String appOldId = appNew.getId();
											
											// make the new id
											String appId = Files.safeName(appName).toLowerCase();
											
											// update the id
											appNew.setId(appId);
											
											// re-intialise with the new id (this loads in the security adapater, amongst other things)
											appNew.initialise(getServletContext(), false);
											
											// look for page files
											File pagesFolder = new File(appFolderDest.getAbsolutePath() + "/pages");
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
											        		.replace("applications/" + appOldId + "/", "applications/" + appId + "/")
											        		.replace("~?a=" + appOldId + "&amp;", "~?a=" + appId + "&amp;");
											        
											        PrintWriter newFileWriter = new PrintWriter(pageFile);
											        newFileWriter.print(newFileString);
											        newFileWriter.close();
											        								    	
											    }
												
											}
											
											// update application name
											appNew.setName(appName);
											
											// update the application id
											appNew.setId(appId);
											
											// get the security
											SecurityAdapater security = appNew.getSecurity();
											
											// if we have one
											if (security != null) {									
												// get the current user's password
												String password = rapidRequest.getUserPassword();
												// update password to empty string if null
												if (password == null) password = "";												
												// add new user
												security.addUser(rapidRequest, userName, password);
												// add Admin and Design roles for the new user
												security.addUserRole(rapidRequest, userName, com.rapid.server.Rapid.ADMIN_ROLE);
												security.addUserRole(rapidRequest, userName, com.rapid.server.Rapid.DESIGN_ROLE);									
											}
											
											// save application
											appNew.save(this, rapidRequest);
											
											// reload it with the file changes
											appNew = Application.load(getServletContext(), new File (appFolderDest + "/application.xml"));
											
											// add application to the collection
											getApplications().put(appNew.getId(), appNew);
											
											// delete unzip folder
											Files.deleteRecurring(unZipFolder);
																							
											// send a positive message
											output = "Import successful";
											
										} else {
											
											// delete unzip folder
											Files.deleteRecurring(unZipFolder);
											
											// throw excpetion
											throw new Exception("Must be a valid Rapid file");
											
										}
										
									} else {
										
										// delete unzip folder
										Files.deleteRecurring(unZipFolder);
										
										// throw excpetion
										throw new Exception("Must be a valid Rapid file");
										
									}
									
								}
																		
							}
							
							getLogger().debug("Designer POST response : " + output);
																
							PrintWriter out = response.getWriter();
							out.print(output);
							out.close();
							
						} // got an application
												
					} // got rapid design role
					
				} // got rapid security
											
			} // got rapid application
											
		} catch (Exception ex) {
			
			getLogger().error("Designer POST error : ",ex);
			
			sendException(rapidRequest, response, ex);
			
		}
		
	}

}
