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

package com.rapid.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.core.Page;
import com.rapid.core.Pages.PageHeaders;
import com.rapid.forms.FormAdapter;
import com.rapid.forms.FormAdapter.FormControlValue;
import com.rapid.forms.FormAdapter.FormPageControlValues;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.User;
import com.rapid.utils.Files;

public class Rapid extends RapidHttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	// these are held here and referred to globally
	public static final String VERSION = "2.3.1"; // the master version of this Rapid server
	public static final String MOBILE_VERSION = "1"; // the mobile version. update it if you want all mobile devices to updates on their next version check
	public static final String DESIGN_ROLE = "RapidDesign";
	public static final String ADMIN_ROLE = "RapidAdmin";
	public static final String SUPER_ROLE = "RapidSuper";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				
		// get a logger
		Logger logger = getLogger();
		
		// log!
		logger.debug("Rapid GET request : " + request.getQueryString());
														
		// get a new rapid request passing in this servlet and the http request
		RapidRequest rapidRequest = new RapidRequest(this, request);		
					
		try {
			
			// get the application object
			Application app = rapidRequest.getApplication();
			
			// check app exists
			if (app == null) {
				
				// send message
				sendMessage(response, 404, "Application not found", "The application you requested can't be found");
								
				//log
				logger.debug("Rapid GET response (404) : Application not found on this server");
				
			} else {
				
				// get the application security
				SecurityAdapter security = app.getSecurityAdapter();
																							
				// check the password
				if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
																			
					// get the user
					User user = security.getUser(rapidRequest);
			
					// check if there is a Rapid action
					if ("summary".equals(rapidRequest.getActionName())) {
						
						// get the form adapter
						FormAdapter formAdapter = app.getFormAdapter();
						
						// check there is one
						if (formAdapter == null) {
							
							// send message
							sendMessage(response, 500, "Not a form", "This Rapid app is not a form");
							
							// log
							logger.debug("Rapid GET response (500) : Page not found");
							
						} else {
															
							// write the form summary page
							formAdapter.writeFormSummary(rapidRequest, response);								
														
						}

					} else if ("download".equals(rapidRequest.getActionName())) {
						
						// set the file name
						String fileName = app.getId() + "_" + rapidRequest.getUserName() + ".zip";
										
						// create the zip file
						app.zip(this, rapidRequest, user, fileName, true);
						
						// set the type as a .zip
						response.setContentType("application/x-zip-compressed");
						
						// Shows the download dialog
						response.setHeader("Content-disposition","attachment; filename=" + app.getId() + ".zip");
																
						// get the file for the zip we're about to create
						File zipFile = new File(getServletContext().getRealPath("/WEB-INF/temp/" + fileName));
						
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
						
						// delete the file
						zipFile.delete();
						
					} else {
						
						// assume it's ok to print the page
						boolean pageCheck = true;
						// assume we won't be showing the summary
						boolean showSummary = false;
						
						// get the page object
						Page page = rapidRequest.getPage();
						
						// get the form adapter (if there is one)
						FormAdapter formAdapter = app.getFormAdapter();
						
						// if there is a formAdapter, make sure there's a form id, unless it's for a dialogue
						if (formAdapter != null) {
							// get form id
							String formId = formAdapter.getFormId(rapidRequest);
							// if there isn't one go back to the start
							if (formId == null) {
								pageCheck = false;							
							} else {
								// only if this is not a dialogue
								if (!"dialogue".equals(rapidRequest.getActionName())) {
									// get all of the pages
									PageHeaders pageHeaders = app.getPages().getSortedPages();
									// get this page position
									int pageIndex = pageHeaders.indexOf(page.getId());
									// check the page visibility
									while (!page.isVisible(rapidRequest, formId, app)) {
										// if we're here the visibility check on the current page failed so increment the index
										pageIndex ++;
										// if there are no more pages go to the summary
										if (pageIndex > pageHeaders.size() - 1) {
											pageCheck = false;
											showSummary = true;
											break;
										} else {
											// select the next page to check the visibility of
											page = app.getPages().getPage(getServletContext(), pageHeaders.get(pageIndex).getId());
										}									
									}
								}
							}
						}
						
						// if the pageCheck was ok (or not invalidated by lack of a form id or summary page)
						if (pageCheck) {
																																	
							// check we got one
							if (page == null) { 
								
								// send message
								sendMessage(response, 404, "Page not found", "The page you requested can't be found");
								
								// log
								logger.debug("Rapid GET response (404) : Page not found");
								
							} else {
															
								// if the page we're about to write is the page we asked for (visibility rules might move us on a bit)								
								if (page.getId().equals(rapidRequest.getPage().getId())) {
									
									// create a writer
									PrintWriter out = response.getWriter();
									
									// assume we require the designer link
									boolean designerLink = true;
									
									// set designer link to false if action is dialogue
									if ("dialogue".equals(rapidRequest.getActionName())) designerLink = false;
									
									// set the response type
									response.setContentType("text/html");
									
									// write the page html
									page.writeHtml(this, rapidRequest,  app, user, out, designerLink);
									
									// close the writer
									out.close();
									
									// flush the writer
									out.flush();
									
								} else {
									
									// redirect user to correct page
									response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + page.getId());
									
								}
																																
							} // page check
							
						} else {
							
							if (showSummary) {
								
								// go to the summary
								response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&action=summary");
								
							} else {
							
								// go to what should be the start page
								response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion());
								
							}
							
						} // form id check
																																					
					} // action name check
					
				} else {
					
					// send message
					sendMessage(response, 403, "No permission", "You do not have permission to use this application");
					
					//log
					logger.debug("Rapid GET response (403) : User " + rapidRequest.getUserName() +  " not authorised for application");
																
				} // password check
				
			} // app exists check
								
		} catch (Exception ex) {
		
			logger.error("Rapid GET error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
																								
	}
	
	private JSONObject getJSONObject(byte[] bodyBytes) throws UnsupportedEncodingException, JSONException {
		
		// assume we weren't passed any json				
		JSONObject jsonData = null;
		
		// read the body into a string
		String bodyString = new String(bodyBytes, "UTF-8");
						
		// if there is something in the body string it must be json so parse it
		if (!"".equals(bodyString)) {
			// get a logger
			Logger logger = getLogger();
			// log the body string
			logger.debug(bodyString);
			// get the data
			jsonData = new JSONObject(bodyString);
		}
		
		return jsonData;
		
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
		// this byte buffer is used for reading the post data 
		byte[] byteBuffer = new byte[1024];
		
		// read bytes from request body into our own byte array (this means we can deal with images) 
		InputStream input = request.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
		for (int length = 0; (length = input.read(byteBuffer)) > -1;) outputStream.write(byteBuffer, 0, length);			
		byte[] bodyBytes = outputStream.toByteArray();
		
		// get a logger
		Logger logger = getLogger();
				
		// log
		logger.debug("Rapid POST request : " + request.getQueryString() + " bytes=" + bodyBytes.length);
				
		// create a Rapid request				
		RapidRequest rapidRequest = new RapidRequest(this, request);
				
		try {
			
			// this is the only variant where an application isn't specified and secured first
			if ("getApps".equals(rapidRequest.getActionName())) {
				
				// create an empty array which we will populate
				JSONArray jsonApps = new JSONArray();
				
				// get all available applications
				List<Application> apps = getApplications().sort();
				
				// if there were some
				if (apps != null) {
					
					// assume the request wasn't for testing on Rapid Mobile
					boolean forTesting = false;
					
					// assume we weren't passed any json				
					JSONObject jsonData = getJSONObject(bodyBytes);
																	
					// if we got some data, look for a test = true entry - this is sent from Rapid Mobile
					if (jsonData != null) forTesting = jsonData.optBoolean("test");
											
					// loop the apps
					for (Application app : apps) {
						
						// if Rapid app must not be for testing / from Rapid Mobile
						if (!"rapid".equals(app.getId()) || !forTesting) {
							
							// get the relevant security adapter
							SecurityAdapter security = app.getSecurityAdapter();
							
							// fail silently if there was an issue
							try {
								
								// make a rapidRequest for this application
								RapidRequest getAppsRequest = new RapidRequest(this, request, app);
														
								// check the user password
								if (security.checkUserPassword(getAppsRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
									
									// create a json object for the details of this application
									JSONObject jsonApp = new JSONObject();
									// add details
									jsonApp.put("id", app.getId());
									jsonApp.put("version", app.getVersion());
									jsonApp.put("title", app.getTitle());								
									// add app to our main array
									jsonApps.put(jsonApp);
									
									// check if we are testing
									if (forTesting) {
										
										// if the user has Rapid Design for this application, (or Rpaid Super if this is the rapid app)
										if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE) && (!app.getId().equals("rapid") || security.checkUserRole(rapidRequest, Rapid.SUPER_ROLE))) {
											
											// loop the versions
											for (Application version :	getApplications().getVersions(app.getId()).sort()) {
												
												// create a json object for the details of this version
												jsonApp = new JSONObject();
												// add details
												jsonApp.put("id", version.getId());
												jsonApp.put("version", version.getVersion());
												jsonApp.put("status", version.getStatus());										
												jsonApp.put("title", version.getTitle());
												jsonApp.put("test", true);
												// add app to our main array
												jsonApps.put(jsonApp);
											}
											
										} // got design role
																			
									} // forTesting check
									
								} // user check
								
							} catch (Exception ex) {
								// only log
								logger.error("Error geting apps : ", ex);
							}
							
						} // rapid app and not for testing check
																																								
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
				
				// log response
				logger.debug("Rapid POST response : " + jsonApps.toString());
				
			} else {
			
				// get the application
				Application app = rapidRequest.getApplication();
				
				// check we got one
				if (app == null) {
					
					// send forbidden response			
					sendMessage(response, 400, "Application not found", "The application you requested can't be found");
					
					// log
					logger.debug("Rapid POST response (403) : Application not found");
					
				} else {
					
					// get the security
					SecurityAdapter security = app.getSecurityAdapter();
										
					// check the user password
					if (security.checkUserPassword(rapidRequest, rapidRequest.getUserName(), rapidRequest.getUserPassword())) {
				
						// if an application action was found in the request						
						if (rapidRequest.getAction() != null) {			
				
							// assume we weren't passed any json				
							JSONObject jsonData = getJSONObject(bodyBytes);
																			
							// if we got some data
							if (jsonData != null) {
								
								// fetch the action result
								JSONObject jsonResult = rapidRequest.getAction().doAction(rapidRequest, jsonData);
								
								// set response to json
								response.setContentType("application/json");
								
								// create a writer
								PrintWriter out = response.getWriter();
								
								// print the results
								out.print(jsonResult.toString());
								
								// close the writer
								out.close();
								
								// log response
								logger.debug("Rapid POST response : " + jsonResult);
								
							} // jsonData
																																																													
						}  else if("application/x-www-form-urlencoded".equals(request.getContentType())) {
							
							// log
							logger.debug("Form data received");
							
							// get the form adapter
							FormAdapter formAdapter = app.getFormAdapter();
							
							// form adapter check
							if (formAdapter == null) {
								
								// send message
								sendMessage(response, 500, "Not a form", "This Rapid app is not a form");
								
								// log
								logger.debug("Rapid GET response (500) : Not a form");
								
							} else {
							
								// this is a form page's data being submitted
								String formData = new String(bodyBytes, "UTF-8");
								
								// log it!
								logger.trace("Form data : " + formData);
								
								// if there's a submit action
								if ("submit".equals(request.getParameter("action"))) {
									
									// get the form id
									String formId = formAdapter.getFormId(rapidRequest);
									
									try {
										
										// do the submit
										formAdapter.submitForm(rapidRequest);
										
										// write the form submit OK page
										formAdapter.writeFormSubmitOK(rapidRequest, response, formId);
										
									} catch (Exception ex) {
										
										// write the form submit Error page
										formAdapter.writeFormSubmitError(rapidRequest, response, formId, ex);
										
									} // submit check
									
								} else {
								
									// get the requestPage id
									String requestPageId = rapidRequest.getPage().getId();
									
									// get the page control values
									FormPageControlValues pageControlValues = FormAdapter.getPostPageControlValues(requestPageId, formData);
									
									// loop and print them if trace on
									if (logger.isTraceEnabled()) {
										for (FormControlValue controlValue : pageControlValues) {
											logger.debug(controlValue.getId() + " = " + controlValue.getValue());
										}
									}									
									
									// store the form page control values
									formAdapter.setFormPageControlValues(rapidRequest, requestPageId, pageControlValues);
																							
									// get all of the app pages
									PageHeaders pageHeaders = app.getPages().getSortedPages();
									
									// get it's position
									int pageIndex = pageHeaders.indexOf(requestPageId);
									
									// if this is the last page
									if (pageIndex >= pageHeaders.size() - 1) {
										
										// send a redirect for the summary (this also avoids ERR_CACH_MISS issues on the back button )
										response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&action=summary");
										
									} else {
																				
										// increment the page index
										pageIndex++;
										
										// send a redirect for the page (this avoids ERR_CACH_MISS issues on the back button )
										response.sendRedirect("~?a=" + app.getId() + "&v=" + app.getVersion() + "&p=" + pageHeaders.get(pageIndex).getId());
										
									}																											

								} // submit action check
								
							} // form adapter check
																							
						} else if ("checkVersion".equals(rapidRequest.getActionName())) {
							
							// create a json version object
							JSONObject jsonVersion = new JSONObject();
																
							// add the mobile version, followed by the app version
							jsonVersion.put("version", MOBILE_VERSION + " - " + app.getVersion());
																											
							// create a writer
							PrintWriter out = response.getWriter();
							
							// print the results
							out.print(jsonVersion.toString());
							
							// close the writer
							out.close();
							
							// log response
							logger.debug("Rapid POST response : " + jsonVersion.toString());
														
						} else if ("uploadImage".equals(rapidRequest.getActionName())) {
																			
							// get the name
							String imageName = request.getParameter("name");
							
							// if we got one
							if (imageName == null) {
								
								// send forbidden response			
								sendMessage(response, 400, "Name required", "Image name must be provided");
								
								// log
								logger.debug("Rapid POST response (403) : Name must be provided");
								
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
									logger.debug("Saved image file " + imageFile);
									
									// create a writer
									PrintWriter out = response.getWriter();
									
									// print the results
									out.print(imagePath);
									
									// close the writer
									out.close();
									
								} else {
									
									// send forbidden response			
									sendMessage(response, 400, "Unrecognised", "Unrecognised file type");
									
									// log
									logger.debug("Rapid POST response (403) : Unrecognised file type");
									
								} // signature check
								
							} // upload file name check
							
						} // action check
																																	
					} else {
						
						// send forbidden response			
						sendMessage(response, 403, "No permisssion", "You do not have permssion to use this application");
						
						// log
						logger.debug("Rapid POST response (403) : User not authorised for application");
											
					}  // user check
				
				} // app check
				
			} // pre app action check
		
		} catch (Exception ex) {
		
			logger.error("Rapid POST error : ", ex);
			
			sendException(rapidRequest, response, ex);
		
		} 											
		
	}

}
