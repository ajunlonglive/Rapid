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

package com.rapid.server.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.rapid.utils.Classes;

public class RapidFilter implements Filter {

	// different applications' security adapters will retrieve different user objects
	public static final String SESSION_VARIABLE_USER_NAME = "user";
	public static final String SESSION_VARIABLE_USER_PASSWORD = "password";
	public static final String SESSION_VARIABLE_USER_DEVICE = "device";	
	
	private static Logger _logger = Logger.getLogger(RapidFilter.class);
		
	private RapidAuthenticationAdapter _authenticationAdapter;
	private boolean _noCaching;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
		try {						
			
			// set the value from stopCaching from the init parameter in web.xml
			_noCaching = Boolean.parseBoolean(filterConfig.getServletContext().getInitParameter("noCaching"));
			
			// look for a specified authentication adapter
			String authenticationAdapterClass = filterConfig.getInitParameter("authenticationAdapterClass");
									
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
				_authenticationAdapter = (RapidAuthenticationAdapter) classClass.getConstructor(ServletContext.class).newInstance(filterConfig.getServletContext());				
			
			}						
			
		} catch (Exception ex) {
			
			throw new ServletException("Rapid filter initialisation failed. Reason: " + ex, ex);
			
		}
		
		_logger.info("Rapid filter initialised.");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		_logger.trace("Process filter request...");
		
		// fake slower responses like on mobile
		//try { Thread.sleep(10000); } catch (InterruptedException e) {}
				
		if (_logger.isTraceEnabled()) {
						
			Integer minimumFirefox = 12;
			Integer minimumChrome = 18;
			Integer minimumIE = 9;
			// any version of IE11 is fine
			
			HttpServletRequest r = (HttpServletRequest) request;

			String ua = r.getHeader("User-Agent");
			boolean unsupportedBrowser = true;
			
			if (ua != null) {
								
				boolean isMSIE = (ua.indexOf("MSIE") != -1);				
				boolean isChrome = (ua.indexOf("Chrome/") != -1);
				boolean isFirefox = (ua.indexOf("Firefox/") != -1);
				boolean isMSIE11 = (ua.indexOf("Windows NT") != -1 && ua.indexOf("rv:1") != -1); // this might be good for IE12 etc. too

				// assume version is 1
				String version = "1";
				
				// look for versions for families tested above
				if (isFirefox) {
					// Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:13.0) Gecko/20100101 Firefox/13.0
					version = ua.replaceAll("^.*?Firefox/", "").replaceAll("\\.\\d+", "");
				} else if (isChrome) {
					// Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5
					version = ua.replaceAll("^.*?Chrome/(\\d+)\\..*$", "$1");
				} else if (isMSIE) {
					// Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET4.0C; .NET4.0E)
					version = ua.replaceAll("^.*?MSIE\\s+(\\d+).*$", "$1");
				}
				
				// check for any browser hits - note any version of IE11 is fine
				unsupportedBrowser = false
					|| (isFirefox && Integer.parseInt(version) < minimumFirefox)
					|| (isChrome && Integer.parseInt(version) < minimumChrome)
					|| (isMSIE && Integer.parseInt(version) < minimumIE)
					|| !isMSIE11;
				
				_logger.debug("Browser supported = " + !unsupportedBrowser + ", Agent string : " + ua);
				
			}
						
		}
		
		// set all responses as UTF-8
		response.setCharacterEncoding("utf-8");
				
		if (_noCaching) {
			
			// cast response to http
			HttpServletResponse httpResponse = (HttpServletResponse) response;
		
			// try and avoid caching
			httpResponse.setHeader("Expires", "Sat, 15 March 1980 12:00:00 GMT");

			// Set standard HTTP/1.1 no-cache headers.
			httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

			// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
			httpResponse.addHeader("Cache-Control", "post-check=0, pre-check=0");

			// Set standard HTTP/1.0 no-cache header.
			httpResponse.setHeader("Pragma", "no-cache");
			
		}
		
		// cast the ServletRequest to a HttpServletRequest
		HttpServletRequest req = (HttpServletRequest) request;
		
		// assume this is not an soa request
		boolean isSoaRequest = false;
		
		// all webservice related requests got to the soa servelet
		if ("/soa".equals(req.getServletPath())) {			
			// if this is a get request
			if ("GET".equals(req.getMethod())) {
				// remember this is a webservice
				isSoaRequest = true;
				// continue the rest of the chain
				filterChain.doFilter(request, response);				
			} else {
				// get the content type (only present for POST)
				String contentType = request.getContentType();
				// if we got one
				if (contentType != null) {
					// put into lower case
					contentType = contentType.toLowerCase();			
					// check this is known type of soa request xml
					if (((req.getHeader("Action") != null || req.getHeader("SoapAction") != null) && contentType.contains("xml")) || (req.getHeader("Action") != null && contentType.contains("json"))) {
						// remember this is a webservice
						isSoaRequest = true;
						// continue the rest of the chain
						filterChain.doFilter(request, response);
					}
				}
			}
		}
											
		// if we have not handled the request via the soa checks
		if (!isSoaRequest) {			
			// get a filtered request
			ServletRequest filteredRequest = _authenticationAdapter.process(request, response);			
			// continue the rest of the chain with it if we got one
			if (filteredRequest != null) 	filterChain.doFilter(filteredRequest, response);			
		}				
	}

	@Override
	public void destroy() {}

}