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

package com.rapid.server.filter;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Classes;

public class RapidFilter implements Filter {

	// different applications' security adapters will retrieve different user attributes
	public static final String SESSION_VARIABLE_INDEX_PATH = "index";
	public static final String SESSION_VARIABLE_USER_NAME = "user";
	public static final String SESSION_VARIABLE_USER_PASSWORD = "password";
	public static final String SESSION_VARIABLE_USER_DEVICE = "device";
	public static final String SESSION_VARIABLE_USER_RESOURCE = "resource";

	private static Logger _logger = LogManager.getLogger(RapidFilter.class);
	private static boolean _hasLogon = true;

	private RapidAuthenticationAdapter _authenticationAdapter;
	private boolean _noCaching;
	private String _xFrameOptions;
	private List<String> _noAuthResources;

	private Set<String> _resourceDirs = null;
	private int _contextIdx = 1; // keep track of the context position of the URL

	// private methods
	private void forwardRequest(ServletRequest filteredRequest, ServletResponse response, String uri) throws ServletException, IOException {

		// forward the newly reconstructed URL
		RequestDispatcher dispatcher = filteredRequest.getRequestDispatcher(uri);
		dispatcher.forward(filteredRequest, response);

	}

	// overrides

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

		// placeholder for specified authentication adapter
		String authenticationAdapterClass = null;

		try {

			// look for a specified authentication adapter
			authenticationAdapterClass = filterConfig.getInitParameter("authenticationAdapterClass");

			// if we didn't find one
			if (authenticationAdapterClass == null) {

				// fall back to the FormAuthenticationAdapter
				_authenticationAdapter = new FormAuthenticationAdapter(filterConfig);

			} else {

				// try and instantiate the authentication adapter
				Class classClass = Class.forName(authenticationAdapterClass);
				// check this class has the right super class
				if (!Classes.extendsClass(classClass, com.rapid.server.filter.RapidAuthenticationAdapter.class)) throw new Exception(authenticationAdapterClass + " must extend com.rapid.server.filter.RapidAuthenticationAdapter.");
				// instantiate an object and retain
				_authenticationAdapter = (RapidAuthenticationAdapter) classClass.getConstructor(FilterConfig.class).newInstance(filterConfig);

			}

			// set the static variable here from the class method (which can be overridden)
			_hasLogon = _authenticationAdapter.hasLogon();

			// set the value from stopCaching from the init parameter in web.xml
			_noCaching = Boolean.parseBoolean(filterConfig.getServletContext().getInitParameter("noCaching"));

			// look for a specified xFrameOptions header, the default is SAMEORIGIN
			_xFrameOptions = filterConfig.getInitParameter("xFrameOptions");
			// if we didn't get one set to SAMEORIGIN, the default
			if (_xFrameOptions == null) _xFrameOptions = "SAMEORIGIN";

			// initialise _noAuthResources, authentication adapters may go further and allow no-auth for login/logout, images, styles, scripts, etc
			_noAuthResources = new ArrayList<>();
			// add system no-auth resources
			_noAuthResources.add("/favicon.ico");
			_noAuthResources.add("/online.htm");
			_noAuthResources.add("/manifest.json");
			_noAuthResources.add("/sw.js");

			// look for a specified noAuthResources in web.xml
			String noAuthResourcesParam = filterConfig.getInitParameter("noAuthResources");
			// if we got some
			if (noAuthResourcesParam != null) {
				// split on ,
				for (String noAuthResource : noAuthResourcesParam.split("\\,")) {
					// trim to remove any spaces
					String noAuthResourceTrimmed = noAuthResource.trim();
					// trim and add to list if's greater than 3 characters (for protection)
					if (noAuthResourceTrimmed.length() > 3) _noAuthResources.add(noAuthResourceTrimmed);
				}
			}

		} catch (Exception ex) {

			// log
			_logger.error("Error initilsing Rapid filter, authenticationAdapterClass=" + authenticationAdapterClass + " : " + ex.getMessage(), ex);

			// throw as ServletException
			throw new ServletException("Rapid filter initialisation failed. Reason: " + ex, ex);

		}

		_logger.info("Rapid filter initialised.");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		_logger.trace("Process filter request...");

		// fake slower responses like on mobile
		// try { Thread.sleep(10000); } catch (InterruptedException e) {}

		// cast the request to http servlet
		HttpServletRequest req = (HttpServletRequest) request;

		// cast the response to http servlet
		HttpServletResponse res = (HttpServletResponse) response;

		// set all responses as UTF-8
		response.setCharacterEncoding("utf-8");

		// add x frame option for iframe's etc
		if (_xFrameOptions != null && _xFrameOptions.length() > 0) res.addHeader("X-FRAME-OPTIONS", _xFrameOptions);

		// get the user agent
		String ua = req.getHeader("User-Agent");

		// assume not IE
		boolean isIE = false;

		// if IE
		if (ua != null && ua.indexOf("MSIE") != -1) {
			// remember IE
			isIE = true;
			// send X-UA-Compatible to prevent compatibility view
			res.addHeader("X-UA-Compatible", "IE=edge,chrome=1");
		}

		// get the requested URI without the hostname, port, or application context - we'll use this both for caching and authentication
		String path = req.getServletPath();

		// assume no query string
		String queryString = "";

		// if there is one
		if (req.getQueryString() != null) queryString = "?" + req.getQueryString();

		// if no caching is on, try and prevent cache, unless IE and request is for Font Awesome (fix for it not displaying sometimes)
		if (_noCaching && !(isIE && path.contains("fontawesome-webfont."))) noCache(res);

		// get the user session without making a new one
		HttpSession session = req.getSession(false);

		// check session
		if (session == null) {

			// if this is the resources request from the service worker return an empty response
			if (path.startsWith("/~") && queryString != null && queryString.endsWith("action=resources")) return;

		} else {

			// retain device if not set already - the form authentication adapter will modify it on login so we musn't overwrite it each time
			if (session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_DEVICE) == null) {

				// get the address from the request host
				InetAddress inetAddress = InetAddress.getByName(request.getRemoteHost());

				// get the request device details
				String deviceDetails = "ip=" + inetAddress.getHostAddress() + ",name=" + inetAddress.getHostName() + ",agent=" + ua;

				// store the device details
				session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_DEVICE, deviceDetails);
			}
			// retain uri as resource request in the session (only if it requires authentication and is not common)
			if (!_noAuthResources.contains(path)
				&& !path.startsWith("/images")
				&& !path.startsWith("/scripts")
				&& !path.startsWith("/styles")
			) {
				session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_RESOURCE, path + queryString);
			}

		}

		// assume this request requires authentication
		boolean requiresAuthentication = true;

		// all webservice related requests got to the soa servelet. Also allow application resources like theme fonts to be defined with a path from the root.
		if (path.startsWith("/soa") || path.startsWith("/applications/")) {

			// if this is a get request
			if ("GET".equals(req.getMethod())) {

				// remember we don't need authentication
				requiresAuthentication = false;

				// get the resource
				String resource = req.getRequestURI();

				// position of any important context folders in the path
				int pos = Math.max(resource.indexOf("/scripts/"), resource.indexOf("/styles/"));

				// if we one of these folders in the path
				if (pos > 0) {

					// forward to just the important bit
					RequestDispatcher dispatcher = request.getRequestDispatcher(resource.substring(pos));
					dispatcher.forward(request, response);

				}

			} else {

				// get the content type (only present for POST)
				String contentType = request.getContentType();
				// if we got one
				if (contentType != null) {
					// put into lower case
					contentType = contentType.toLowerCase();
					// check this is known type of soa request xml
					if (
							(contentType.contains("xml") && (req.getHeader("Action") != null || req.getHeader("SoapAction") != null))
							|| contentType.contains("json")
						) {
						// remember we don't need standard authentication
						requiresAuthentication = false;
					}
				}
			}
		}

		// loop the no auth resources - /online.htm, /safety, /manifest.json, /sw.js have been moved to system no auth resources in this filters constructor
		for (String noAuthResource : _noAuthResources) {
			// check any parameterises no authentication resources;
			if (path.startsWith(noAuthResource)) {
				// record we don't need authentication
				requiresAuthentication = false;
				// we're done
				break;
			}
		}

		// if this request requires authentication
		if (requiresAuthentication) {

			// log full url
			_logger.trace("RapidFilter request " + path + queryString);

			// get a filtered request
			ServletRequest filteredRequest = _authenticationAdapter.process(request, response);

			// continue the rest of the chain with it if we got one
			if (filteredRequest != null) {

				// the url we will forward to
				String rapidForwardURL;

				// split the path into parts by /
				String[] pathPart = (path.replaceFirst("/", "")).split("/");

				// get the last path part
				String lastPathPart = pathPart[pathPart.length - 1];

				// get a lower case version of it for case insensitive checking
				String lastPathPartLower = lastPathPart.toLowerCase();

				// assume no second last path part
				String secondLastPathPart = null;

				// set second last path part if there was one
				if (pathPart.length > 1) secondLastPathPart = pathPart[pathPart.length - 2];

				// get the list of applications to try to find any in the parts
				Applications applications = (Applications) request.getServletContext().getAttribute("applications");

				// assets that we know are in the root
				if (pathPart.length > 1 && ("favicon.ico".equals(lastPathPart) || "sw.js".equals(lastPathPart))) {

					// forward to off the root
					forwardRequest(filteredRequest, response, "/" + lastPathPart);

				// assets that we know are one folder off the root - also avoiding the common scenario of an images folder in the styles
				} else if (pathPart.length > 2 && pathPart.length < 5 && (("images".equals(secondLastPathPart) && !"styles".equals(pathPart[0])) || "scripts".equals(secondLastPathPart) || "scripts_min".equals(secondLastPathPart) || "styles".equals(secondLastPathPart) || "styles_min".equals(secondLastPathPart))) {

					// forward to it in its folder
					forwardRequest(filteredRequest, response, "/" + secondLastPathPart + "/" + lastPathPart);

				// if user has provided at least 1 path part (i.e. part1/) and is requesting known resources like login.jsp or downloadCSV.jsp for known application, if not known application we assume a child Rapid instance
				} else if (pathPart.length > 1 && lastPathPartLower.endsWith(".jsp") && applications.get(pathPart[0]) != null) {

					// check POST (or get)
					if ("POST".equals(req.getMethod())) {

						// forward it so we don't lose the data
						forwardRequest(filteredRequest, response, "/" + lastPathPart + queryString);

					} else {

						// redirect to the root of the context - which is the root when seen from outside
						res.sendRedirect(req.getContextPath() + "/" + lastPathPart + queryString);

						// send redirect immediately
						return;

					}

				// if user has provided at least 1 path part (i.e. part1/) and the first part is a known application
				} else if (pathPart.length > 0 && applications.get(pathPart[0]) != null) {

					// get the resource filenames only once
					if (_resourceDirs == null) setResourceDirs(req);

					String appID = pathPart[0];
					rapidForwardURL = "/~?a=" + appID;

					if ("POST".equals(req.getMethod()) || ("GET".equals(req.getMethod()) && "~".equals(lastPathPart))) {

						rapidForwardURL = "/~" + queryString;

					} else { //any other get requests

						String version, page;

						switch (pathPart.length) {
						case 1:	//if URL contains only the appID
							rapidForwardURL = "/~?a=" + appID;
							break;

						case 2:	//if URL contains appID/p or appID/v
							// if what followed by appID is a Resource folder deal with it
							if (isResource(pathPart[1])) {
								rapidForwardURL = path.replaceFirst("/" + appID, "");

							} else { // otherwise it must be an application

								// Check whether 2nd part is a page or version only
								if ("p".equalsIgnoreCase(String.valueOf(pathPart[1].charAt(0)))) {
									page = pathPart[1];
									rapidForwardURL += "&p=" + page;

								} else {
									version = pathPart[1];
									rapidForwardURL += "&v=" + version;
								}

								_contextIdx = 1;

							}
							break;

						default:	// if more than 2 parts
							// check if this is a resource - remember the contextIdx position
							if (isResource(pathPart[_contextIdx])) {
								// restructure the url- without the applicationName/versionNumber
								if (_contextIdx < 2) {
									rapidForwardURL = path.replaceFirst("/" + appID, "");
								} else {
									rapidForwardURL = path.replaceFirst("/" + appID + "/" + pathPart[1], "");
								}

							} else { // otherwise it must be an application
								version = pathPart[1];
								page = pathPart[2];
								rapidForwardURL += "&v=" + version + "&p=" + page;
								_contextIdx = 2;
							}
							break;
						} // pathPart length check

					} // method check

					// forward the newly reconstructed URL
					forwardRequest(filteredRequest, response, rapidForwardURL);

				} else {

					// for uploads there must be a known app and a file requested
					if (pathPart.length > 1 && "uploads".equals(pathPart[0])) {

						// assume we won't pass
						boolean pass = false;

						// get the app
						Application app = applications.get(pathPart[1]);

						// if there is an app and an existing session
						if (app != null && session != null) {

							// get the security adapter
							SecurityAdapter security = app.getSecurityAdapter();

							// get the user name
							String userName = (String) session.getAttribute(SESSION_VARIABLE_USER_NAME);

							// create a rapid request
							RapidRequest rapidRequest = new RapidRequest(req, app);

							// check the security for this user and password
							try {

								// get the password
								String userPaswd = rapidRequest.getUserPassword();

								// check the password
								pass = security.checkUserPassword(rapidRequest, userName, userPaswd);

							} catch (Exception ex) {
								_logger.error("Error checking user security for uploads folder, app = " + app.getId() + ", user = " + userName + ", path = " + path, ex);
							}

							// log
							_logger.debug("Checked user security for uploads folder, app = " + app.getId() + ", user = " + userName + ", path = " + path + ", pass = " + pass);

						}

						// return an empty response if we didn't pass
						if (!pass) return;

					}

					// for regular rapid url formats
					filterChain.doFilter(filteredRequest, response);

				}

			}

		} else {// for requests not requiring authentication

			// continue the rest of the chain
			filterChain.doFilter(request, response);

		}

	}

	// setter method
	private void setResourceDirs(HttpServletRequest request) {

		// check whether this is a RESOURCE or application request
		String webContentString = request.getServletContext().getRealPath("/");
		File webContentDir = new File(webContentString);
		File[] directoryListing = webContentDir.listFiles();

		_resourceDirs = new HashSet<>();

		// Loop over the root of the WebContent directory, to see whether
		// appID/** is followed by one of the folders in this directory
		// If so, then this request must be a RESOURCE--
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// if
				if (child.isDirectory()) {
					_resourceDirs.add(child.getName());
				}
			}
		}
	}

	@Override
	public void destroy() {	}

	// private static methods

	private boolean isResource(String fileName) {

		return _resourceDirs.contains(fileName);

	}

	// public static methods

	public static void noCache(HttpServletResponse response) {

		// if we were provided with a reponse object
		if (response != null) {

			// try and avoid caching
			response.setHeader("Expires", "Tue, 1 January 1980 12:00:00 GMT");

			// Set standard HTTP/1.1 no-cache headers.
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

			// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
			response.addHeader("Cache-Control", "post-check=0, pre-check=0");

			// Set standard HTTP/1.0 no-cache header.
			response.setHeader("Pragma", "no-cache");

		}

	}

	public static boolean isAuthorised(ServletRequest servletRequest, String userName, String userPassword, String path) {

		// assume we are not authorised for any applications
		boolean authorised = false;

		// get the applications collection
		Applications applications = (Applications) servletRequest.getServletContext().getAttribute("applications");

		// cast the ServletRequest to a HttpServletRequest
		HttpServletRequest request = (HttpServletRequest) servletRequest;

		// if there are some applications
		if (applications != null) {
			// if the index path is for a specific app
			if (path.contains("a=")) {
				// get the app id
				String appId = path.substring(path.indexOf("a=") + 2);
				// assume no version
				String version = null;
				// see if the user is known to this application
				try {
					// if other parameters clean to there
					if (appId.indexOf("&") > 0) appId = appId.substring(0, appId.indexOf("&"));
					// if version parameter
					if (path.contains("v=")) {
						// get the version
						version = path.substring(path.indexOf("v=") + 2);
						// if other parameters clean to there
						if (version.indexOf("&") > 0) version = version.substring(0, version.indexOf("&"));
					}
					// get this application
					Application application = applications.get(appId, version);
					// if we got it
					if (application == null) {
						_logger.error("Error checking permission for app " + appId + " - can't be found");
					} else {
						// get a Rapid request
						RapidRequest rapidRequest = new RapidRequest(request, application);
						// check if authorised
						authorised = application.getSecurityAdapter().checkUserPassword(rapidRequest, userName, userPassword);
					}
				} catch (SecurityAdapaterException ex) {
					_logger.error("Error checking permission for app " + appId + " in login index : ", ex);
				}
			} else {
				// loop all applications
				for (Application application : applications.get()) {
					try {
						// get a Rapid request for the application
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
		}

		return authorised;

	}

	public static boolean hasLogon() {
		return _hasLogon;
	}

}

/*

package com.rapid.server.filter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Classes;

public class RapidFilter implements Filter {

	// different applications' security adapters will retrieve different user
	// objects
	public static final String SESSION_VARIABLE_INDEX_PATH = "index";
	public static final String SESSION_VARIABLE_USER_NAME = "user";
	public static final String SESSION_VARIABLE_USER_PASSWORD = "password";
	public static final String SESSION_VARIABLE_USER_DEVICE = "device";

	private static Logger _logger = LogManager.getLogger(RapidFilter.class);

	private RapidAuthenticationAdapter _authenticationAdapter;
	private boolean _noCaching;
	private String _xFrameOptions;
	private Set<String> _resourceDirs = null;

	// overrides

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

		// placeholder for specified authentication adapter
		String authenticationAdapterClass = null;

		try {

			// set the value from stopCaching from the init parameter in web.xml
			_noCaching = Boolean.parseBoolean(filterConfig.getServletContext().getInitParameter("noCaching"));

			// look for a specified authentication adapter
			authenticationAdapterClass = filterConfig.getInitParameter("authenticationAdapterClass");

			// if we didn't find one
			if (authenticationAdapterClass == null) {

				// fall back to the FormAuthenticationAdapter
				_authenticationAdapter = new FormAuthenticationAdapter(filterConfig);

			} else {

				// try and instantiate the authentication adapter
				Class classClass = Class.forName(authenticationAdapterClass);
				// check this class has the right super class
				if (!Classes.extendsClass(classClass, com.rapid.server.filter.RapidAuthenticationAdapter.class)) throw new Exception(authenticationAdapterClass + " must extend com.rapid.server.filter.RapidAuthenticationAdapter.");
				// instantiate an object and retain
				_authenticationAdapter = (RapidAuthenticationAdapter) classClass.getConstructor(FilterConfig.class).newInstance(filterConfig);

			}

			// look for a specified xFrameOptions header, the default is SAMEORIGIN
			_xFrameOptions = filterConfig.getInitParameter("xFrameOptions");
			// if we didn't get one set to SAMEORIGIN, the default
			if (_xFrameOptions == null) _xFrameOptions = "SAMEORIGIN";

		} catch (Exception ex) {

			// log
			_logger.error("Error initilsing Rapid filter, authenticationAdapterClass=" + authenticationAdapterClass + " : " + ex.getMessage(), ex);

			// throw as ServletException
			throw new ServletException("Rapid filter initialisation failed. Reason: " + ex, ex);

		}

		_logger.info("Rapid filter initialised.");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		_logger.trace("Process filter request...");

		// fake slower responses like on mobile
		// try { Thread.sleep(10000); } catch (InterruptedException e) {}

		// cast the request to http servlet
		HttpServletRequest req = (HttpServletRequest) request;

		// cast the response to http servlet
		HttpServletResponse res = (HttpServletResponse) response;

		// get the user agent
		String ua = req.getHeader("User-Agent");

		// if IE send X-UA-Compatible to prevent compatibility view
		if (ua != null && ua.indexOf("MSIE") != -1) res.addHeader("X-UA-Compatible", "IE=edge,chrome=1");

		// add x frame option for iframe's etc
		if (_xFrameOptions != null && _xFrameOptions.length() > 0) res.addHeader("X-FRAME-OPTIONS", _xFrameOptions);

		// set all responses as UTF-8
		response.setCharacterEncoding("utf-8");

		// if no caching is on, try and prevent cache
		if (_noCaching) noCache(res);

		// assume this request requires authentication
		boolean requiresAuthentication = true;

		// get the requested URI without the hostname -- gets servlet path
		String path = req.getServletPath();

		// all webservice related requests got to the soa servelet
		if (path.startsWith("/soa")) {
			// if this is a get request
			if ("GET".equals(req.getMethod())) {
				// remember we don't need authentication
				requiresAuthentication = false;
			} else {
				// get the content type (only present for POST)
				String contentType = request.getContentType();
				// if we got one
				if (contentType != null) {
					// put into lower case
					contentType = contentType.toLowerCase();
					// check this is known type of soa request xml
					if (
							(contentType.contains("xml") && (req.getHeader("Action") != null || req.getHeader("SoapAction") != null))
							|| contentType.contains("json")
						) {
						// remember we don't need standard authentication
						requiresAuthentication = false;
					}
				}
			}
		}

		// online.htm doesn't need authentication
		if ("/online.htm".equals(path)) requiresAuthentication = false;

		// safety doesn't need authentication
		if ("/safety".equals(path)) requiresAuthentication = false;

		// if this request requires authentication
		if (requiresAuthentication) {

			// log full url
			_logger.trace("RapidFilter request " + ((HttpServletRequest)request).getRequestURL().toString());

			// get a filtered request
			ServletRequest filteredRequest = _authenticationAdapter.process(request, response);

			// continue the rest of the chain with it if we got one
			if (filteredRequest != null) {

				// the url we will forward to
				String rapidForwardURL = null;

				// split the path into parts by /
				String[] pathPart = (path.replaceFirst("/", "")).split("/");

				// get the last pathPart
				String lastPathPart = pathPart[pathPart.length - 1];

				// get the list of applications to try to find any in the parts
				Applications applications = (Applications) request.getServletContext().getAttribute("applications");

				// if favicon
				if (lastPathPart.equals("favicon.ico")) {

					// set redirect to root
					rapidForwardURL = "/" + lastPathPart;

				} else if (lastPathPart.equals("index.css")) {

					// set redirect to root
					rapidForwardURL = "/styles/" + lastPathPart;

				} else if (lastPathPart.equals("RapidLogo.svg")) {

					// set redirect to root
					rapidForwardURL = "/images/" + lastPathPart;

				// if user has provided at least 1 path part (i.e. part1/) and the first part is a known application
				} else if (pathPart.length >= 1 && applications.get(pathPart[0]) != null) {

					// assume context id position is 0
					int contextIdx = 0;

					// get the resource filenames only once
					if (_resourceDirs == null) 	setResourceDirs(req);

					String appID = pathPart[0];
					rapidForwardURL = "/~?a=" + appID;

					if ("~".equals(lastPathPart)) {

						rapidForwardURL = "/~?" + req.getQueryString();

					} else { //any other get requests

						String version, page;

						switch (pathPart.length) {
						case 1:	//if URL contains only the appID
							rapidForwardURL = "/~?a=" + appID;
							break;

						case 2:	//if URL contains appID/p or appID/v
							// if what followed by appID is a Resource folder deal with it
							if (isResource(pathPart[1])) {
								rapidForwardURL = path.replaceFirst("/" + appID, "");

							} else { // otherwise it must be an application

								// Check whether 2nd part is a page or version only
								if ("p".equalsIgnoreCase(String.valueOf(pathPart[1].charAt(0)))) {
									page = pathPart[1];
									rapidForwardURL += "&p=" + page;

								} else {
									version = pathPart[1];
									rapidForwardURL += "&v=" + version;
								}

								contextIdx = 1;

							}
							break;

						default:	// if more than 2 parts
							// check if this is a resource - remember the contextIdx position
							if (isResource(pathPart[contextIdx])) {
								// restructure the url- without the applicationName/versionNumber
								if (contextIdx < 2) {
									rapidForwardURL = path.replaceFirst("/" + appID, "");
								} else {
									rapidForwardURL = path.replaceFirst("/" + appID + "/" + pathPart[1], "");
								}

							} else { // otherwise it must be an application
								version = pathPart[1];
								page = pathPart[2];
								rapidForwardURL += "&v=" + version + "&p=" + page;
							}
							break;

						} // pathPart.length switch

					} // ~ check

				} // forward pattern checks

				if (rapidForwardURL == null) {

					// for regular rapid url formats
					filterChain.doFilter(filteredRequest, response);

				} else {

					// forward the newly reconstructed URL
					RequestDispatcher dispatcher = filteredRequest.getRequestDispatcher(rapidForwardURL);
					dispatcher.forward(filteredRequest, response);

				}

			}

		} else {// for requests not requiring authentication
			// continue the rest of the chain
			filterChain.doFilter(request, response);
		}
	}

	// setter method
	private void setResourceDirs(HttpServletRequest request) {

		// check whether this is a RESOURCE or application request
		String webContentString = request.getServletContext().getRealPath("/");
		File webContentDir = new File(webContentString);
		File[] directoryListing = webContentDir.listFiles();

		_resourceDirs = new HashSet<>();

		// Loop over the root of the WebContent directory, to see whether
		// appID/** is followed by one of the folders in this directory
		// If so, then this request must be a RESOURCE--
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// if
				if (child.isDirectory()) {
					_resourceDirs.add(child.getName());
				}
			}
		}
	}

	@Override
	public void destroy() {	}

	// private static methods

	private boolean isResource(String fileName) {

		return _resourceDirs.contains(fileName);

	}

	// public static methods

	public static void noCache(HttpServletResponse response) {

		// if we were provided with a reponse object
		if (response != null) {

			// try and avoid caching
			response.setHeader("Expires", "Tue, 1 January 1980 12:00:00 GMT");

			// Set standard HTTP/1.1 no-cache headers.
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

			// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
			response.addHeader("Cache-Control", "post-check=0, pre-check=0");

			// Set standard HTTP/1.0 no-cache header.
			response.setHeader("Pragma", "no-cache");

		}

	}

	public static boolean isAuthorised(ServletRequest servletRequest, String userName, String userPassword, String indexPath) {

		// remember whether we are authorised for at least one application
		boolean authorised = false;

		// get the applications collection
		Applications applications = (Applications) servletRequest.getServletContext().getAttribute("applications");

		// cast the ServletRequest to a HttpServletRequest
		HttpServletRequest request = (HttpServletRequest) servletRequest;

		// if there are some applications
		if (applications != null) {
			// if the index path is for a specific app
			if (indexPath.contains("a=")) {
				// get the app id
				String appId = indexPath.substring(indexPath.indexOf("a=") + 2);
				// assume no version
				String version = null;
				// see if the user is known to this application
				try {
					// if other parameters clean to there
					if (appId.indexOf("&") > 0) appId = appId.substring(0, appId.indexOf("&"));
					// if version parameter
					if (indexPath.contains("v=")) {
						// get the version
						version = indexPath.substring(indexPath.indexOf("v=") + 2);
						// if other parameters clean to there
						if (version.indexOf("&") > 0) version = version.substring(0, version.indexOf("&"));
					}
					// get this application
					Application application = applications.get(appId, version);
					// if we got it
					if (application == null) {
						_logger.error("Error checking permission for app " + appId + " - can't be found");
					} else {
						// get a Rapid request
						RapidRequest rapidRequest = new RapidRequest(request, application);
						// check if authorised
						authorised = application.getSecurityAdapter().checkUserPassword(rapidRequest, userName, userPassword);
					}
				} catch (SecurityAdapaterException ex) {
					_logger.error("Error checking permission for app " + appId + " in login index : ", ex);
				}
			} else {
				// loop all applications
				for (Application application : applications.get()) {
					try {
						// get a Rapid request for the application
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
		}

		return authorised;

	}

}

*/