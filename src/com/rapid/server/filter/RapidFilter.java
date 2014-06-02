

package com.rapid.server.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class RapidFilter implements Filter {

	public static final String SESSION_VARIABLE_USER_NAME = "userName";
	public static final String SESSION_VARIABLE_USER_PASSWORD = "userPassword";
	
	private static Logger _logger = Logger.getLogger(RapidFilter.class);
		
	private RapidAuthenticationAdapter _authenticationAdapter;
	private boolean _stopCaching;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
		try {
			
			// set the value from stopCaching from the init parameter in web.xml
			_stopCaching = Boolean.parseBoolean(filterConfig.getServletContext().getInitParameter("stopCaching"));
			
			// look for a specified authentication adapter
			String authenticationAdapterClass = filterConfig.getInitParameter("authenticationAdapterClass");
			// if we didn't find one
			if (authenticationAdapterClass == null) {
				// fall back to the formauthenticationadapter
				_authenticationAdapter = new FormAuthenticationAdapter(filterConfig.getServletContext());				
			} else {
				// try and instantiate the authentication adapter
				Class classClass = Class.forName(authenticationAdapterClass);
				// check the class extends com.rapid.server.filter.RapidAuthenticationAdapter
				Class superClass = Class.forName("com.rapid.server.filter.RapidAuthenticationAdapter");
				// check this class has the right super class
				if (!classClass.getSuperclass().equals(superClass)) throw new Exception(authenticationAdapterClass + " must extend " + superClass.getCanonicalName()); 
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
		
		// set all responses as UTF-8
		response.setCharacterEncoding("utf-8");
				
		if (_stopCaching) {
			
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
									
		ServletRequest filteredRequest = _authenticationAdapter.process(request, response);
			
		if (filteredRequest == null) {
			// send response back to client immediately 
			return;
		} else {
			// continue the rest of the chain
			filterChain.doFilter(filteredRequest, response);
		}
	}

	@Override
	public void destroy() {}

}