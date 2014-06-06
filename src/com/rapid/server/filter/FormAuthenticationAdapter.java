package com.rapid.server.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.server.RapidHttpServlet.RapidRequest;

public class FormAuthenticationAdapter extends RapidAuthenticationAdapter {
	
	private static Logger _logger = Logger.getLogger(RapidFilter.class);
	
	public FormAuthenticationAdapter(ServletContext servletContext) {
		 super(servletContext);		
		_logger.info("Form authentication filter initialised.");
		
	}
		
	@Override
	public ServletRequest process(ServletRequest req, ServletResponse res) throws IOException, ServletException {
						
		HttpServletRequest request = (HttpServletRequest) req;
		
		String requestPath = request.getServletPath();
		
		if ("/".equals(requestPath) || requestPath.contains("login.jsp") || requestPath.contains("index.jsp") || requestPath.contains("design.jsp") || requestPath.contains("designpage.jsp") || requestPath.contains("/rapid") || requestPath.contains("/~")) {
			
			String userName = null;
			String userPassword = null;
			
			HttpSession session = request.getSession();
			
			// look in the session for username/password
			userName = (String) session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME);
			userPassword = (String) session.getAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD);
			
			// cast response to http
			HttpServletResponse response = (HttpServletResponse) res;
			
			if (userName == null || userPassword == null) {
																				
				// look for an authorisation attribute in the session
				String sessionRequestPath = (String) session.getAttribute("requestPath");
				
				// look in the request for the username/password
				userName = request.getParameter("userName");
				userPassword = request.getParameter("userPassword");
												
				if (userName == null || userPassword == null) {
																									
					// if we are attempting to authorise progress to the next step in the filter
					if (requestPath.contains("login.jsp") && sessionRequestPath != null) return req;
					
					// if this is json just send an empty response
					if (request.getHeader("Accept").contains("application/json")) {
						
						// set the 401 - access denied
						response.sendError(401);
												
					} else {
						
						// retain the request path less the leading /
						String authorisationRequestPath = requestPath.substring(1);
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
					
					// remember whether we are authorised for at least one application
					boolean authorised = false;
					
					// get the map of applications
					Map<String,Application> applications = (Map<String,Application>) getServletContext().getAttribute("applications");
					
					// if there are some applications
					if (applications != null) {
						// loop them
						for (String key : applications.keySet()) {
							try {
								// get the application
								Application application = applications.get(key);
								// get a Rapid request
								RapidRequest rapidRequest = new RapidRequest(request);
								// see if the user is known to this application
								authorised = application.getSecurity().checkUserPassword(rapidRequest, userName, userPassword);
								// we can exit if so as we only need one
								if (authorised) break;								
							} catch (Exception ex) {}
						}
					}
					
					// we passed authorisation so redirect the client to the resource they wanted
					if (authorised) {
						
						// retain username / password in the session
						session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME, userName);
						session.setAttribute(RapidFilter.SESSION_VARIABLE_USER_PASSWORD, userPassword);
						
						// make the sessionRequest path the root just in case it was null (or login.jsp itself)
						if (sessionRequestPath == null || "login.jsp".equals(sessionRequestPath)) sessionRequestPath = ".";
						
						// remove the authorisation session attribute
						session.setAttribute("requestPath", null);
						
						// send a redirect to reload what we wanted before
						response.sendRedirect(sessionRequestPath);
						
						// return to client immediately
						return null;
						
					} else {
						
						// retain the authorisation attempt in the session
						session.setAttribute("Message", "Your user name / login has not been recognised");
						
						// send a redirect to load the login page
						response.sendRedirect("login.jsp");
											
						// return immediately
						return null;
						
					}
																			
				}
												
			}
			
			// if we are requesting the login.jsp but have authenticated go to index instead
			if (requestPath.contains("login.jsp")) {
				
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
			
		} else {
									
			// proceed to the next step
			return req;
			
		}
						
	}

}
