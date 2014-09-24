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

package com.rapid.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Event;
import com.rapid.core.Page;
import com.rapid.core.Page.RoleHtml;
import com.rapid.security.SecurityAdapater;
import com.rapid.security.SecurityAdapater.Role;
import com.rapid.security.SecurityAdapater.User;
import com.rapid.utils.Files;
import com.rapid.utils.Html;

public class Rapid extends RapidHttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	// these are held here and referred to globally
	public static final String VERSION = "2.2.1";
	public static final String DESIGN_ROLE = "RapidDesign";
	public static final String ADMIN_ROLE = "RapidAdmin";
	public static final String SUPER_ROLE = "RapidSuper";
	public static final String BACKUP_FOLDER = "_backups";
					
	// this byte buffer is used for reading the post data
	private byte[] _byteBuffer = new byte[1024];
					     
    private String getAdminLink(String appId, String pageId) {
    	
    	String html = "<div id='designShow' style='position:fixed;left:0px;bottom:0px;width:30px;height:30px;z-index:1000;'></div>\n"
    	+ "<img id='designLink' style='position:fixed;left:6px;bottom:6px;z-index:1001;display: none;' src='images/gear_24x24.png'></img>\n"
    	+ "<script type='text/javascript'>\n"
    	+ "/* designLink */\n"
    	+ "$(document).ready( function() {\n"
    	+ "  $('#designShow').mouseover ( function(ev) { $('#designLink').show(); });\n"
    	+ "  $('#designLink').mouseout ( function(ev) { $('#designLink').hide(); });\n"
    	+ "  $('#designLink').click ( function(ev) { window.location='design.jsp?a=" + appId + "&p=" + pageId + "' });\n"
    	+ "})\n"
    	+ "</script>\n";
    	
    	return html;
    	
    }
                
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
										
		getLogger().debug("Rapid GET request : " + request.getQueryString());
											
		// whether we're rebulding the page for each request
    	boolean rebuildPages = Boolean.parseBoolean(getServletContext().getInitParameter("rebuildPages"));
						
		// get a new rapid request passing in this servelet and the http request
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		// set the page html to an empty string
		String pageHtml = "";
					
		try {
			
			// get the application object
			Application app = rapidRequest.getApplication();
			
			// check app exists
			if (app == null) {
				
				// send message
				sendMessage(rapidRequest, response, 404, "Application not found on this server");
								
				//log
				getLogger().debug("Rapid GET response (404) : Application not found on this server");
				
			} else {
				
				// get the application security
				SecurityAdapater security = app.getSecurity();
				
				// get the userName
				String userName = rapidRequest.getUserName();
				
				// get the user
				User user = security.getUser(rapidRequest, userName);
				
				// check we got a user
				if (user == null) {
										
					// send message
					sendMessage(rapidRequest, response, 403, "User not authorised for application");
					
					//log
					getLogger().debug("Rapid GET response (403) : User " + userName +  " not authorised for application");
					
				} else {
			
					// check if there is a Rapid action
					if ("download".equals(rapidRequest.getActionName())) {
										
						// create the zip file
						app.zip(this, rapidRequest, app.getId() + ".zip", true);
						
						// set the type as a .zip
						response.setContentType("application/x-zip-compressed");
						
						// Shows the download dialog
						response.setHeader("Content-disposition","attachment; filename=" + app.getId() + ".zip");
																
						// get the file for the zip we're about to create
						File zipFile = new File(getServletContext().getRealPath("/WEB-INF/temp/" + app.getId() + ".zip"));
						
						// get it's size
						long fileSize = Files.getSize(zipFile);
						
						// add size to response headers if small enough
						if (fileSize < Integer.MAX_VALUE) response.setContentLength((int) fileSize);
						
						// send the file to browser
						OutputStream os = response.getOutputStream();
						FileInputStream in = new FileInputStream(zipFile);
						byte[] buffer = new byte[1024];
						int length;
						while ((length = in.read(buffer)) > 0){
						  os.write(buffer, 0, length);
						}
						in.close();
						os.flush();
						
					} else {
																	
						// get the page object
						Page page = rapidRequest.getPage();
								
						// check we got one
						if (page == null) { 
							
							// send message
							sendMessage(rapidRequest, response, 404, "Page not found");
							
							// log
							getLogger().debug("Rapid GET response (404) : Page not found");
							
						} else {
							
							// create a writer
							PrintWriter out = response.getWriter();
							
							// check if we are in debug mode
							if (rebuildPages) {
								
								// (re)generate the page start html
								pageHtml = page.getHtmlHead(rapidRequest.getApplication());
								
							} else {
							
								// get any cached header html from the page object (this will regenerate and cache if not present)
								pageHtml = page.getHtmlHeadCached(rapidRequest.getApplication());
														
							}
							
							// output the start of the page
							out.print(pageHtml);
																										
							// get the users roles
							List<String> userRoles = user.getRoles();
												
							// retrieve and rolesHtml for the page
							List<RoleHtml> rolesHtml = page.getRolesHtml();
		
							// check we have userRoles and htmlRoles
							if (userRoles != null && rolesHtml != null) {
																					
								// loop each roles html entry
								for (RoleHtml roleHtml : rolesHtml) {
																
									// get the roles from this combination
									List<String> roles = roleHtml.getRoles();
																
									// keep a running count for the roles we have
									int gotRoleCount = 0;
									
									// if there are roles to check
									if (roles != null) {
									
										// retain how many roles we need our user to have
										int rolesRequired = roles.size();
										
										// check whether we need any roles and that our user has any at all
										if (rolesRequired > 0) {
											// check the user has as many roles as this combination requires
											if (userRoles.size() >= rolesRequired) {
												// loop the roles we need for this combination
												for (String role : roleHtml.getRoles()) {
													// check this role
													if (userRoles.contains(role)) {
														// increment the got role count
														gotRoleCount ++;
													}
												}
											}									
										}
																		
										// if we have all the roles we need
										if (gotRoleCount == rolesRequired) {
											// use this html
											out.print("  " + roleHtml.getHtml());
											// no need to check any further
											break;
										}
										
									} else {
										
										// no roles to check means we can use this html immediately 
										out.print("  " + Html.getPrettyHtml(page.getHtmlBody()).trim());
										
									}
									
								}
									
																										
							} else {
								
								out.print("  " + Html.getPrettyHtml(page.getHtmlBody()).trim());
								
							}
						
							// check for the design role, super is required as well if the rapid app
							if ("rapid".equals(app.getId())) {
								if (security.checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE) && security.checkUserRole(rapidRequest, userName, Rapid.SUPER_ROLE)) out.print(getAdminLink(app.getId(), page.getId()).trim());
							} else {
								if (security.checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE)) out.print(getAdminLink(app.getId(), page.getId()).trim());
							}
							
							// add the remaining elements
							out.print("  </body>\n</html>");
							
							// close the writer
							out.close();
												
						} // page check
																																					
					} // action name check
							
				} // security check
				
			} // app exists check
								
		} catch (Exception ex) {
		
			getLogger().error("Rapid GET error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
																			
		getLogger().trace("Rapid GET response : " + pageHtml);
					
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				
		// read bytes from request body into our own byte array (this means we can deal with images) 
		InputStream input = request.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
		for (int length = 0; (length = input.read(_byteBuffer)) > -1;) outputStream.write(_byteBuffer, 0, length);			
		byte[] bodyBytes = outputStream.toByteArray();
				
		// create a Rapid request				
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		// log
		getLogger().debug("Rapid POST request : " + request.getQueryString() + " mimetype : " + request.getContentType());
					
		try {
			
			// if an application action was found in the request						
			if (rapidRequest.getAction() != null) {			
			
				// get the application
				Application app = rapidRequest.getApplication();
				
				// check we got one
				if (app == null) {
					
					// send forbidden response			
					sendMessage(rapidRequest, response, 403, "Application not found");
					
					// log
					getLogger().debug("Rapid POST response (403) : Application not found");
					
				} else {
					
					// get the security
					SecurityAdapater security = app.getSecurity();
					
					// get the user
					User user = security.getUser(rapidRequest, rapidRequest.getUserName());
					
					// check the user
					if (user == null) {
						
						// send forbidden response			
						sendMessage(rapidRequest, response, 403, "User not authorised for application");
						
						// log
						getLogger().debug("Rapid POST response (403) : User not authorised for application");
						
					} else {
							
						// assume we weren't passed any json				
						JSONObject jsonData = null;
						
						// read the body into a string
						String bodyString = new String(bodyBytes, "UTF-8");
						
						// log the body string
						getLogger().debug(bodyString);
						
						// if there is something in the body string it must be json so parse it
						if (!"".equals(bodyString)) jsonData = new JSONObject(bodyString);
						
						// if we got some data
						if (jsonData != null) {
							
							// fetch the action result
							JSONObject jsonResult = rapidRequest.getAction().doAction(this, rapidRequest, jsonData);
							
							// set response to json
							response.setContentType("application/json");
							
							// create a writer
							PrintWriter out = response.getWriter();
							
							// print the results
							out.print(jsonResult.toString());
							
							// close the writer
							out.close();
							
						}
																																																												
					}
					
				}				
											
			} else if ("getApps".equals(rapidRequest.getActionName())) {
				
				// create an empty array which we will populate
				JSONArray jsonApps = new JSONArray();
				
				// get all available applications
				List<Application> apps = getApplications().sort();
				
				// if there were some
				if (apps != null) {
					
					// retain the user name 
					String userName = rapidRequest.getUserName();
					
					// loop the apps
					for (Application app : apps) {
									
						// get the relevant security adapter
						SecurityAdapater security = app.getSecurity();
						
						// fail silently if there was an issue
						try {
						
							// fetch a user object in the name of the current user for the current app
							User user = security.getUser(rapidRequest, userName);
							
							// if we got one
							if (user != null) {
								
								// create a json object for the details of this application
								JSONObject jsonApp = new JSONObject();
								// add details
								jsonApp.put("id", app.getId());
								jsonApp.put("title", app.getTitle());
								// add app to our main array
								jsonApps.put(jsonApp);
								
							} // user check
							
						} catch (Exception ex) {}
																									
					} // apps loop
					
				} // apps check
				
				// set response to json
				response.setContentType("application/json");
				
				// create a writer
				PrintWriter out = response.getWriter();
				
				// print the results
				out.print(jsonApps.toString());
				
				// close the writer
				out.close();
				
			} else if ("uploadImage".equals(rapidRequest.getActionName())) {
				
				// get the application
				Application app = rapidRequest.getApplication();
				
				// check we got one
				if (app == null) {
					
					// send forbidden response			
					sendMessage(rapidRequest, response, 403, "Application not found");
					
					// log
					getLogger().debug("Rapid POST response (403) : Application not found");
					
				} else {
					
					// get the security
					SecurityAdapater security = app.getSecurity();
					
					// get the user
					User user = security.getUser(rapidRequest, rapidRequest.getUserName());
					
					// check the user
					if (user == null) {
						
						// send forbidden response			
						sendMessage(rapidRequest, response, 403, "User not authorised for application");
						
						// log
						getLogger().debug("Rapid POST response (403) : User not authorised for application");
						
					} else {					
					
						// get the name
						String imageName = request.getParameter("name");
						
						// if we got one
						if (imageName == null) {
							
							// send forbidden response			
							sendMessage(rapidRequest, response, 403, "Name must be provided");
							
							// log
							getLogger().debug("Rapid POST response (403) : Name must be provided");
							
						} else {
														
							// check the jpg file signature (from http://en.wikipedia.org/wiki/List_of_file_signatures)
							if (bodyBytes[0] == (byte)0xFF && bodyBytes[1] == (byte)0xD8 && bodyBytes[2] == (byte)0xFF) {
								
								// create the paht
								String imagePath = "uploads/" +  app.getId() + "/" + imageName;
								// create a file
								File imageFile = new File(getServletContext().getRealPath(imagePath));
								// create app folder if need be
								imageFile.getParentFile().mkdir();
								// create a file output stream to save the data to
								FileOutputStream fos = new FileOutputStream(imageFile);
								// write the body bytes to the stream
								fos.write(bodyBytes);
								// close the stream
								fos.close();
								
								// log the file creation
								getLogger().debug("Saved image file " + imageFile);
								
								// create a writer
								PrintWriter out = response.getWriter();
								
								// print the results
								out.print(imagePath);
								
								// close the writer
								out.close();
								
							} else {
								
								// send forbidden response			
								sendMessage(rapidRequest, response, 403, "Unrecognised file type");
								
								// log
								getLogger().debug("Rapid POST response (403) : Unrecognised file type");
								
							} // signature check
							
						} // name check
						
					} // user check
					
				} // app check
																
			} // action check
			
		} catch (Exception ex) {
		
			getLogger().error("Rapid POST error : ", ex);
			
			sendException(rapidRequest, response, ex);
		
		} 											
		
	}

}
