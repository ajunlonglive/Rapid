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

package com.rapid.forms;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import com.rapid.core.Application;
import com.rapid.core.Page;
import com.rapid.server.RapidRequest;

public class RapidFormAdapter extends FormAdapter {
	
	// constructor

	public RapidFormAdapter(ServletContext servletContext, Application application) {
		super(servletContext, application);
	}
	
	// class methods
	private Map<String,PageControlValues> getUserAppPageControlValues(RapidRequest rapidRequest, Application application) {
		// get the user session
		HttpSession session = rapidRequest.getRequest().getSession();
		// get all app page control values from session
		Map<String,Map<String,PageControlValues>> userAppPageControlValues = (Map<String, Map<String, PageControlValues>>) session.getAttribute("userAppPageControlValues");
		// if null
		if (userAppPageControlValues == null) {
			// instantiate
			userAppPageControlValues = new HashMap<String, Map<String, PageControlValues>>();
			// add to session
			session.setAttribute("userAppPageControlValues", userAppPageControlValues);
		}
		// the page controls for specified app
		Map<String,PageControlValues> userPageControlValues = userAppPageControlValues.get(application.getId());
		// if null, instantiate
		if (userPageControlValues == null) {
			// instantiate
			userPageControlValues = new HashMap<String, PageControlValues>();
			// add to user app pages
			userAppPageControlValues.put(application.getId(), userPageControlValues);
		}
		// return!
		return userPageControlValues;		
	}
	
	// overridden methods

	@Override
	public void storePageControlValues(RapidRequest rapidRequest, Application application, String pageId, PageControlValues pageControlValues) {
		// if there are controls to store
		if (pageControlValues.size() > 0) {
			// store them
			getUserAppPageControlValues(rapidRequest, application).put(pageId, pageControlValues);
		}
	}

	@Override
	public PageControlValues retrievePageControlValues(RapidRequest rapidRequest, Application application, String pageId) {
		// retrieve
		return getUserAppPageControlValues(rapidRequest, application).get(pageId);
	}

}
