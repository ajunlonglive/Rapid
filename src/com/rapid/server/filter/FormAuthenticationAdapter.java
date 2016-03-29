/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.server.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.Enumeration;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class FormAuthenticationAdapter extends RapidAuthenticationAdapter {
	
	public static final String INIT_PARAM_IP_CHECK = "ipcheck";
	public static final String INIT_PARAM_PUBLIC_ACCESS = "public";
	public static final String PUBLIC_ACCESS_USER = "public";
		
	private static Logger _logger = Logger.getLogger(RapidAuthenticationAdapter.class);
	
	private boolean _publicAccess = false;
	private String[] _ipChecks = null;
	
	public FormAuthenticationAdapter(FilterConfig filterConfig) {
		 super(filterConfig);
		// look for ip check for sensitive pages
		 String ipCheck = filterConfig.getInitParameter(INIT_PARAM_IP_CHECK);
		 // if we got some, build the array now
		 if (ipCheck != null) {
			 // split them
			 _ipChecks = ipCheck.split(",");
			 // loop them
			 for (int i = 0; i < _ipChecks.length; i++) {
				 // trim them for good measure, and replace *
				 _ipChecks[i] = _ipChecks[i].trim().replace("*","");
			 }
			 // log
			 _logger.info("IP addresses will be checked against " + ipCheck + " for access to sensitive resources.");	
		 }
		 // look for whether public access is allowed
		 _publicAccess  = Boolean.parseBoolean(filterConfig.getInitParameter(INIT_PARAM_PUBLIC_ACCESS));
		 // log
		 _logger.info("Form authentication filter initialised.");		
	}
		
	@Override
	public ServletRequest process(ServletRequest req, ServletResponse res) throws IOException, ServletException {
		
		// cast the ServletRequest to a HttpServletRequest
		HttpServletRequest request = (HttpServletRequest) req;
		
		// log the full request
		if (_logger.isTraceEnabled()) {
			_logger.debug("FormAuthenticationAdapter request : " + request.getMethod() + " " + request.getRequestURL() + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
			Enumeration<String> headerNames = request.getHeaderNames();
			String headers = "";
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				headers += headerName + " = " + request.getHeader(headerName) + "; ";
			}
			_logger.debug("Headers : " + headers);
		}
		
		// now get just the resource path
		String requestPath = request.getServletPath();
		// if null set to root
		if (requestPath == null) requestPath = "/";
		if (requestPath.length() == 0) requestPath = "/"; 
		// make lower case - just to be sure
		requestPath = requestPath.toLowerCase();
		
		// if ip checking is in place
		if (_ipChecks != null) {
			// get the query string			
			String queryString = request.getQueryString();
			// check we got one
			if (queryString == null) {
				// set to empty string
				queryString = "";
			} else {
				// set to lower case
				queryString = queryString.toLowerCase();
			}
			// if this is a sensitive resource
			if (requestPath.startsWith("/login.jsp") || requestPath.startsWith("/logout.jsp") || requestPath.startsWith("/design.jsp") || requestPath.startsWith("/designpage.jsp") || requestPath.startsWith("/designer") || (requestPath.startsWith("/~") && queryString.contains("a=rapid"))) {
				// get the client IP
				String ip = request.getRemoteAddr();
				// assume no pass
				boolean pass = false;
				// log
				_logger.debug("Checking IP " + ip + " for " + requestPath);				
				// loop the ip checks
				for (String ipCheck : _ipChecks) {
					// check the ip starts with the filter, this allows full, or partial IPs (we remove the * for good measure)
					if (ip.startsWith(ipCheck)) {
						// we passed
						pass = true;
						// we're done
						break;
					}
				}
				// if we failed
				if (!pass) {
					// log
					_logger.info("Access from " + ip + " for " + requestPath + " failed IP check");
					// cast the ServletRequest to a HttpServletRequest
					HttpServletResponse response = (HttpServletResponse) res;
					// send a not found
					response.sendError(404);
					// no further processing
					return null;
				}			
			}
		}
				
		// if we can return this resource without authentication
		if (requestPath.endsWith("favicon.ico") || requestPath.startsWith("/images/") || requestPath.startsWith("/scripts") || requestPath.startsWith("/styles")) {
			
			// proceed to the next step
			return req;			
			
		} else {
						
			// if it's a resource that requires authentication
			_logger.trace("FormAuthenticationAdapter checking authorisation");
			
			// cast response to http
			HttpServletResponse response = (HttpServletResponse) res;
			
			// only allow in frame if sameorigin
			response.addHeader("X-FRAME-OPTIONS", "SAMEORIGIN" );
			
			// assume no userName
			String userName = null;
						
			// create a new session
			HttpSession session = request.getSession();
			
			// look in the session for username
			userName = (String) session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);

			// check if we got one
			if (userName == null) {
				
				_logger.trace("No userName found in session");
																				
				// look for a sessionRequestPath attribute in the session
				String sessionRequestPath = (String) session.getAttribute("requestPath");
				
				// look in the request for the username
				userName = request.getParameter("userName");				
			
				if (userName == null) {
					
					_logger.trace("No userName found in request");
																									
					// if we are attempting to authorise 
					if (requestPath.endsWith("login.jsp") && sessionRequestPath != null) {
												
						// check the url for a requestPath
						String urlRequestPath = request.getParameter("requestPath");
						// overide the session one if so
						if (urlRequestPath != null) session.setAttribute("requestPath", urlRequestPath);
						// progress to the next step in the filter
						return req;
						
					} else {
						
						// if we're allowing public access, but not if this is the login page
						if (_publicAccess && !requestPath.endsWith("login.jsp")) {
							
							// set the user name to public
							session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME, PUBLIC_ACCESS_USER);
							// set the password to none
							session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD, "");
							// progress to the next step in the filter
							return req;
							
						}
						
					}
					
					String acceptHeader = request.getHeader("Accept");
					if (acceptHeader == null) acceptHeader = ""; 
					
					// if this is json just send a 401
					if (acceptHeader.contains("application/json")) {
						
						// set the 401 - access denied
						response.sendError(401);
												
					} else {
						
						// retain the request path less the leading /
						String authorisationRequestPath = requestPath.substring(1);
						// replace designpage.jsp with design.jsp to get us out of the parent
						authorisationRequestPath = authorisationRequestPath.replace("designpage.jsp", "design.jsp");
						// append the query string if there was one
						if (request.getQueryString() != null) authorisationRequestPath += "?" + request.getQueryString(); 						
						// retain the request path in the session
						session.setAttribute("requestPath", authorisationRequestPath);
						
						// send a redirect to load the login page
						response.sendRedirect("login.jsp");
						
					}
									
					// return immediately
					return null;
					
				} else {
					
					_logger.trace("userName found in request");
																				
					// remember whether we are authorised for at least one application
					boolean authorised = false;
					
					// get the applications collection
					Applications applications = (Applications) getServletContext().getAttribute("applications");
					
					// look in the request for the password
					String userPassword = request.getParameter("userPassword");
					
					// look in the request for device details
					String deviceId = request.getParameter("deviceId");
					
					// get the address from the request host
					InetAddress inetAddress = InetAddress.getByName(request.getRemoteHost());
					
					// get the request device details 
					String deviceDetails = "ip=" + inetAddress.getHostAddress() + ",name=" + inetAddress.getHostName() + ",agent=" + request.getHeader("User-Agent");
					
					// if we were sent a device id add it to the device details
					if (deviceId != null)  deviceDetails += "," + deviceId;
																	
					// retain device id in the session
					session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_DEVICE, deviceDetails);
					
					// if there are some applications
					if (applications != null) {
						// loop them
						for (Application application : applications.get()) {
							try {
								// get a Rapid request
								RapidRequest rapidRequest = new RapidRequest(request, application);
								// see if the user is known to this application
								authorised = application.getSecurityAdapter().checkUserPassword(rapidRequest, userName, userPassword);
								// we can exit if so as we only need one
								if (authorised) break;								
							} catch (Exception ex) {
								// log the error
								_logger.error("FormAuthenticationAdapter error checking user", ex);
							}
						}
					}
					
					// we passed authorisation so redirect the client to the resource they wanted
					if (authorised) {
						
						// retain user name in the session
						session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME, userName);
						
						// retain encrypted user password in the session
						try {
							session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD, RapidHttpServlet.getEncryptedXmlAdapter().marshal(userPassword));
						} catch (GeneralSecurityException ex) {
							// log the error
							_logger.error("FormAuthenticationAdapter error storing encrypted password", ex);
						}

						// log that authentication was granted
						_logger.debug("FormAuthenticationAdapter authenticated " + userName + " from " + deviceDetails);
						
						// make the sessionRequest path the root just in case it was null (or login.jsp itself)
						if (sessionRequestPath == null || "login.jsp".equals(sessionRequestPath)) sessionRequestPath = ".";
												
						// if we had a requestApp in the sessionRequestPath, go straight to the app
						if (sessionRequestPath.indexOf("requestApp") > 0) {
							// split the parts
							String[] requestAppParts = sessionRequestPath.split("=");
							// if we have a second part with the appId in it
							if (requestAppParts.length > 1) {
								// set the sessionRequestPath to the appId
								sessionRequestPath = "~?a=" + requestAppParts[1];
							}
 						}
						
						// remove the authorisation session attribute
						session.setAttribute("requestPath", null);
						
						// send a redirect to reload what we wanted before
						response.sendRedirect(sessionRequestPath);
						
						// return to client immediately
						return null;
						
					} else {
						
						// retain the authorisation attempt in the session
						session.setAttribute("message", "Your user name / password has not been recognised");
						
						// delay by 1sec to make brute force attacks a little harder
						try { Thread.sleep(1000); } catch (InterruptedException e) {}
						
						// send a redirect to load the login page
						response.sendRedirect("login.jsp");
											
						// return immediately
						return null;
						
					}
																			
				}
												
			}
			
			// if we are requesting the login.jsp but have authenticated go to index instead
			if (requestPath.contains("login.jsp") && "GET".equals(request.getMethod())) {
				
				// send a redirect to load the index page
				response.sendRedirect("index.jsp");
				
				// return immediately
				return null;
				
			}
				
			// return the request which will process the chain
			// hold a reference to the original request
			HttpServletRequest filteredReq = request;
			
			// wrap the request if it is not a rapid request (with our username and principle)
			if(!(request instanceof RapidRequestWrapper)) filteredReq = new RapidRequestWrapper(request, userName);
			
			return filteredReq;
			
		} 
						
	}

}
