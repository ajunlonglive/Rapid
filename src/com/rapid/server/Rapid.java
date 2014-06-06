package com.rapid.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import com.rapid.security.RapidSecurityAdapter.Security;
import com.rapid.utils.Html;

public class Rapid extends RapidHttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	// these are held here and referred to globally
	public static final String VERSION = "2.1.2";
	public static final String DESIGN_ROLE = "RapidDesign";
	public static final String ADMIN_ROLE = "RapidAdmin";
					
	// this byte buffer is used for reading the post data
	private byte[] _byteBuffer = new byte[1024];
	
	// this array is used to collect all of the lines needed in the pageload before sorting them
	private List<String> _pageloadLines;
	
	// this includes functions to call any control initJavaScript and set up any event listeners
    private void getPageLoadLines(List<String> pageloadLines, Page page, List<Control> controls) throws JSONException {
    	if (controls != null) {
    		// if we're at the root (look through the page events)
    		if (page.getControls().equals(controls)) {
    			// check for page events
    			if (page.getEvents() != null) {
    				// loop page events
        			for (Event event : page.getEvents()) {        				
        				// only if there are actually some actions to invoke
    					if (event.getActions() != null) {
    						if (event.getActions().size() > 0) {
    							// page is a special animal so we need to do each of it's event types differently
    							if ("pageload".equals(event.getType())) {
    								pageloadLines.add("Event_" + event.getType() + "_" + page.getId() + "();\n");
    	        				}    			
    							// reusable action is only invoked via reusable actions on other events - there is no listener
    						}
    					}         				
        			}
    			}    			
    		}
    		// loop controls
    		for (Control control : controls) {
    			// check for any initJavaScript to call
    			if (control.hasInitJavaScript()) {
    				// get any details we may have
					String details = control.getDetails();
					// set to empty string or clean up
					if (details == null) {
						details = "";
					} else {
						details = ", " + details;
					}    				
    				// write an init call method
    				pageloadLines.add("Init_" + control.getType() + "('" + control.getId() + "'" + details + ");\n");
    			}
    			// check event actions
    			if (control.getEvents() != null) {
    				// loop events
    				for (Event event : control.getEvents()) {
    					// only if there are actually some actions to invoke
    					if (event.getActions() != null) {
    						if (event.getActions().size() > 0) {
    							// add the line
    							pageloadLines.add("$('#" + control.getId() + "')." + event.getType() + "(Event_" + event.getType() + "_" + control.getId() + ");\n");    							
    						}
    					}    					
    				}
    			}
    			// now call iteratively for child controls (of this [child] control, etc.)
    			if (control.getChildControls() != null) getPageLoadLines(pageloadLines, page, control.getChildControls());     				
    		}
    	}    	
    }
            
    private void getEventHandlers(StringBuilder stringBuilder, Application application, Page page, List<Control> controls) throws JSONException {
    	if (controls != null) {
    		// if we're at the root
    		if (page.getControls().equals(controls)) {    			
    			// check for page events
    			if (page.getEvents() != null) {
    				// loop page events
        			for (Event event : page.getEvents()) {
        				// check there are actions
    					if (event.getActions() != null) {
    						if (event.getActions().size() > 0) {
    							// derive the function name
    							String functionName = "Event_" + event.getType() + "_" + page.getId();
    							// create a function for running the actions for this controls events
        						stringBuilder.append("function " + functionName + "(ev) {\n");
        						// open a try/catch
        						stringBuilder.append("  try {\n");
        						// get any filter javascript
        						String filter = event.getFilter();
        						// if we have any add it now
        						if (filter != null) {
        							// only bother if not an empty string
        							if (!"".equals(filter)) {
        								stringBuilder.append("    " + filter.trim().replace("\n", "    \n") + "\n");
        							}
        						}
        						// loop the actions and produce the handling JavaScript
        						for (Action action : event.getActions()) {
        							// get the action client-side java script from the action object (it's generated there as it can contain values stored in the object on the server side)
        							String actionJavaScript = action.getJavaScript(application, page, null).trim();
        							// if non null
        							if (actionJavaScript != null) {
        								// only if what we got is not an empty string (once trimmmed)
        								if (!("").equals(actionJavaScript)) stringBuilder.append("    " + actionJavaScript.trim().replace("\n", "    \n") + "\n");
        							}    							
        						}
        						// close the try/catch
        						stringBuilder.append("  } catch(ex) { alert('Error in " + functionName + " ' + ex); }\n");
        						// close function
        						stringBuilder.append("}\n\n");
    						}    						
    					}
        			}
    			}    			
    		}
    		for (Control control : controls) {
    			// check event actions
    			if (control.getEvents() != null) {
    				// loop events
    				for (Event event : control.getEvents()) {
    					// check there are actions
    					if (event.getActions() != null) {
    						if (event.getActions().size() > 0) {
    							// derive the function name
    							String functionName = "Event_" + event.getType() + "_" + control.getId();
    							// create a function for running the actions for this controls events
        						stringBuilder.append("function " + functionName + "(ev) {\n");
        						// open a try/catch
        						stringBuilder.append("  try {\n");
        						// get any filter javascript
        						String filter = event.getFilter();
        						// if we have any add it now
        						if (filter != null) {
        							// only bother if not an empty string
        							if (!"".equals(filter)) {
        								stringBuilder.append("    " + filter.trim().replace("\n", "    \n") + "\n");
        							}
        						}
        						// loop the actions and produce the handling JavaScript
        						for (Action action : event.getActions()) {
        							// get the action client-side java script from the action object (it's generated there as it can contain values stored in the object on the server side)
        							String actionJavaScript = action.getJavaScript(application, page, control).trim();
        							// if non null
        							if (actionJavaScript != null) {
        								// only if what we got is not an empty string (once trimmmed)
        								if (!("").equals(actionJavaScript)) stringBuilder.append("  " + actionJavaScript + "\n");
        							}    							
        						}
        						// close the try/catch
        						stringBuilder.append("  } catch(ex) { alert('Error in " + functionName + " ' + ex); }\n");
        						// close function
        						stringBuilder.append("}\n\n");
    						}    						
    					}
    				}
    			}
    			// now call iteratively for child controls (of this [child] control, etc.)
    			if (control.getChildControls() != null) getEventHandlers(stringBuilder, application, page, control.getChildControls());     				
    		}
    	}    	
    }
     
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
    
    private String getPageStartHtml(Application application, Page page) throws JSONException {
    	
    	StringBuilder stringBuilder = new StringBuilder();
    	    								
		// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
		stringBuilder.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
								
		stringBuilder.append("<html>\n");
		
		stringBuilder.append("  <head>\n");
		
		stringBuilder.append("    <title>" + page.getTitle() + " - by Rapid</title>\n");
		
		// these files are considered essential
		
		stringBuilder.append("    <script type='text/javascript' src='scripts/jquery-1.10.2.js'></script>\n");
		stringBuilder.append("    <script type='text/javascript' src='scripts/extras.js'></script>\n");
		stringBuilder.append("    <script type='text/javascript' src='scripts/json2.js'></script>\n");								
						
		// loop and add the resources required by this application's controls and actions (created when application loads)
		if (application.getResourceIncludes() != null) {
			for (String resource : application.getResourceIncludes()) {
				stringBuilder.append("    " + resource.trim() + "\n");
			}
		}
		
		// include the application's JavaScript file (also generated when the page loads from the controls and actions)
		stringBuilder.append("    <script type='text/javascript' src='applications/" + application.getId() +"/rapid.js'></script>\n");
		
		// look for any page javascript files
		if (page.getJavascriptFiles() != null) {
			for (String file : page.getJavascriptFiles()) {
				stringBuilder.append("    <script type='text/javascript' src='" + file + "'></script>\n");
			}
		}
														
		// include the application's css file (generated when the application is saved)
		stringBuilder.append("    <link rel='stylesheet' type='text/css' href='applications/" + application.getId() +"/rapid.css'></link>\n");
		
		// include the page's css file (generated when the page is saved)
		stringBuilder.append("    <link rel='stylesheet' type='text/css' href='applications/" + application.getId() +"/" + page.getName() + ".css'></link>\n");
		
		// look for any page css files
		if (page.getCssFiles() != null) {
			for (String file : page.getCssFiles()) {
				stringBuilder.append("    <link rel='stylesheet' type='text/css' href='" + file + "'></link>\n");
			}
		}
		
		// start building the inline js for the page				
		stringBuilder.append("    <script type='text/javascript'>\n\n");
										
		// initialise our pageload lines collections
		_pageloadLines = new ArrayList<String>();
		
		// get any control initJavaScript event listeners into he pageloadLine (goes into $(document).ready function)
		getPageLoadLines(_pageloadLines, page, page.getControls());
		
		// sort the page load lines
		Collections.sort(_pageloadLines, new Comparator<String>() {
			@Override
			public int compare(String l1, String l2) {
				char i1 = l1.charAt(0);
				char i2 = l2.charAt(0);
				return i2 - i1;						
			}}
		);
		
		// open the page loaded function
		stringBuilder.append("$(document).ready( function() {\n");
		
		// print them
		for (String line : _pageloadLines) {
			stringBuilder.append("  " + line);
		}
		
		// show the page
		stringBuilder.append("  $('body').show();\n");
								
		// end of page loaded function
		stringBuilder.append("});\n\n");
		
		// add event handlers
		getEventHandlers(stringBuilder, application, page, page.getControls());
					
		// close the page inline script block
		stringBuilder.append("    </script>\n");
		
		stringBuilder.append("  <head>\n");
		
		stringBuilder.append("  <body id='" + page.getId() + "' style='display:none;'>\n");
						
		return stringBuilder.toString();
    	
    }
        
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
										
		getLogger().debug("Rapid GET request : " + request.getQueryString());
		
		PrintWriter out = response.getWriter();
		
		response.addHeader("Cache-Control", "no-store");
		
		// whether we're trying to avoid caching
    	boolean noCaching = Boolean.parseBoolean(getServletContext().getInitParameter("noCaching"));
						
		// get a new rapid request passing in this servelet and the http request
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		// set the page html to an empty string
		String pageHtml = "";
					
		try {
			
			// get the application object
			Application app = rapidRequest.getApplication();
						
			// get the page object
			Page page = rapidRequest.getPage();
					
			// check we got one
			if (page != null) {
				
				if (noCaching) {
					
					// generate the page start html
					pageHtml = getPageStartHtml(rapidRequest.getApplication(), page);
					
				} else {
				
					// get any cached header html from the page object
					pageHtml = page.getCachedStartHtml();
					
					// if no html was returned from the cache
					if (pageHtml == null) {
						
						// generate the page start html
						pageHtml = getPageStartHtml(rapidRequest.getApplication(), page);
																					
						// have the page cache the generated html for next time
						page.cacheStartHtml(pageHtml);
						
					} 
					
				}
				
				// output the start of the page
				out.print(pageHtml);
				
				// get the application security
				SecurityAdapater security = app.getSecurity();
				
				// if we have some
				if (security != null) {
					
					// get the username
					String userName = rapidRequest.getUserName();
					
					// get the users roles
					List<String> userRoles = security.getUserRoles(rapidRequest, userName);
										
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
				
					// if this user has the design role
					if (security.checkUserRole(rapidRequest, userName, Rapid.DESIGN_ROLE)) out.print(getAdminLink(app.getId(), page.getId()).trim());
					
				} else {
					
					out.print("  " + Html.getPrettyHtml(page.getHtmlBody()).trim());
					
				} // security check
							
				// add the remaining elements
				out.print("  </body>\n</html>");
				
																
			} // page check
						
								
		} catch (Exception ex) {
		
			getLogger().error("Rapid GET error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
															
		out.close();
		
		getLogger().trace("Rapid GET response : " + pageHtml);
					
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter out = response.getWriter();
						
		// read bytes from request body into our own byte array (this means we can deal with images) 
		InputStream input = request.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();												
		for (int length = 0; (length = input.read(_byteBuffer)) > -1;) outputStream.write(_byteBuffer, 0, length);			
		byte[] bodyBytes = outputStream.toByteArray();
		
		String bodyString = new String(bodyBytes, "UTF-8");
				
		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		StringBuilder stringBuilder = new StringBuilder();
		
		getLogger().debug("Rapid POST request : " + request.getQueryString() + " body : " + bodyString);
					
		try {
			
			if (rapidRequest.getAction() != null) {								
				
				JSONObject jsonData = null;
				
				if (!"".equals(bodyString)) jsonData = new JSONObject(bodyString);
					
				JSONObject jsonResult = rapidRequest.getAction().doAction(this, rapidRequest, jsonData);
				
				stringBuilder.append(jsonResult.toString());	
								
			} else if ("getApps".equals(rapidRequest.getActionName())) {
				
				JSONArray jsonApps = new JSONArray();
				
				List<Application> apps = getSortedApplications();
				
				if (apps != null) {
					
					for (Application app : apps) {
						
						SecurityAdapater security = app.getSecurity();
						
						String userName = rapidRequest.getUserName();
						if (userName == null) userName = "";
						
						String password = rapidRequest.getUserPassword();
						if (password == null) password = "";
						
						if (security.checkUserPassword(rapidRequest, userName, password)) {
							
							JSONObject jsonApp = new JSONObject();
							jsonApp.put("id", app.getId());
							jsonApp.put("title", app.getTitle());
							jsonApps.put(jsonApp);
							
						}
																			
					}
					
				}
				
				stringBuilder.append(jsonApps.toString());
				
			}
			
		} catch (Exception ex) {
		
			getLogger().error("Rapid POST error : ",ex);
			
			sendException(rapidRequest, response, ex);
		
		} 
		
		// all responses are in json
		response.setContentType("application/json");
															
		out.print(stringBuilder.toString());
		out.close();
		
		getLogger().debug("Rapid POST response : " + stringBuilder.toString());
		
	}

}
