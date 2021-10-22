/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public abstract class RapidAuthenticationAdapter {

	// public static variables

	public static final String INIT_PARAM_IP_CHECK = "ipcheck";
	public static final String INIT_PARAM_PUBLIC_ACCESS = "public";
	public static final String INIT_PARAM_PUBLIC_ACCESS_RESOURCES = "publicResources";
	public static final String INIT_PARAM_PUBLIC_UPLOADS = "publicUploads";
	public static final String PUBLIC_ACCESS_USER = "public";

	// instance variables

	protected ServletContext _servletContext;
	protected boolean _publicAccess = false;
	protected List<String> _publicResources;
	protected boolean _publicUploads = false;

	// properties

	// return the servelet context as given to us in the constructor
	public ServletContext getServletContext() { return _servletContext; }
	// an overrideable default property (most authentication adaptors require logons, if not we take the buttons and links out of the screens)
	public boolean hasLogon() { return true; }

	// constructor

	public RapidAuthenticationAdapter(FilterConfig filterConfig) {
		_servletContext = filterConfig.getServletContext();
		// look for whether public access is allowed
		_publicAccess = Boolean.parseBoolean(filterConfig.getInitParameter(INIT_PARAM_PUBLIC_ACCESS));
		// add this to the context
		filterConfig.getServletContext().setAttribute(INIT_PARAM_PUBLIC_ACCESS, _publicAccess);
		// look for any resources that will be given public authentication
		String publicResources = filterConfig.getInitParameter(INIT_PARAM_PUBLIC_ACCESS_RESOURCES);
		// make our list
		_publicResources = new ArrayList<>();
		// if we got some
		if (publicResources != null) {
			// split and loop
			for (String publicResource : publicResources.split(",")) {
				// trim to remove any spaces
				String publicResourceTrimmed = publicResource.trim();
				// trim and add to list if's greater than 3 characters (for protection)
				if (publicResourceTrimmed.length() > 3) _publicResources.add(publicResourceTrimmed);
				// add to our list
				_publicResources.add(publicResourceTrimmed);
			 }
		 }
		// look for whether public uploads is enabled
		_publicUploads = Boolean.parseBoolean(filterConfig.getInitParameter(INIT_PARAM_PUBLIC_UPLOADS));
		// add this to the context
		filterConfig.getServletContext().setAttribute(INIT_PARAM_PUBLIC_UPLOADS, _publicUploads);
	}

	// the abstract process method called from the main Rapid filter
	public abstract ServletRequest process(ServletRequest request, ServletResponse response) throws IOException, ServletException;

}
