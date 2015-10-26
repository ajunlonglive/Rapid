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

import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidRequest;

public class RapidFormAdapter extends FormAdapter {
	
	// constructor

	public RapidFormAdapter(ServletContext servletContext, Application application) {
		super(servletContext, application);
	}
	
	// class methods
		
	protected Map<String,FormPageControlValues> getUserFormPageControlValues(RapidRequest rapidRequest) {	
		// get the user session
		HttpSession session = rapidRequest.getRequest().getSession();
		// get all app page control values from session
		Map<String,Map<String,FormPageControlValues>> userAppPageControlValues = (Map<String, Map<String, FormPageControlValues>>) session.getAttribute("userFormPageControlValues");
		// if null
		if (userAppPageControlValues == null) {
			// instantiate
			userAppPageControlValues = new HashMap<String, Map<String, FormPageControlValues>>();
			// add to session
			session.setAttribute("userFormPageControlValues", userAppPageControlValues);
		}
		// get the form id
		String formId = getFormId(rapidRequest);
		// the page controls for specified app
		Map<String,FormPageControlValues> userPageControlValues = userAppPageControlValues.get(formId);
		// if null, instantiate
		if (userPageControlValues == null) {
			// instantiate
			userPageControlValues = new HashMap<String, FormPageControlValues>();
			// add to user app pages
			userAppPageControlValues.put(formId, userPageControlValues);
		}
		// return!
		return userPageControlValues;		
	}
	
	// overridden methods
	
	@Override
	public String getFormId(RapidRequest rapidRequest) {
		// get the user session (making a new one if need be)
		HttpSession session = rapidRequest.getRequest().getSession();
		// retrieve the form ids from the session
		Map<String,String> formIds = (Map<String, String>) session.getAttribute("userFormIds");
		// instantiate if null
		if (formIds == null) formIds = new HashMap<String, String>();
		// get the application
		Application application = rapidRequest.getApplication();
		// get the form id based on the app id and version
		String formId = formIds.get(application.getId() + "-" + application.getVersion());
		// if it's null
		if (formId == null) {
			// get the start page id
			String startPageId = application.getStartPageId();
			// get the request page id
			String requestPageId = rapidRequest.getPage().getId();
			// there are some rules for creating new form ids - there must be no action and the page must be the start page
			if (rapidRequest.getRequest().getParameter("action") == null && (startPageId.equals(requestPageId) || !application.getPages().getPageIds().contains(startPageId))) {
				// get the servlet context
				ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
				// the maste form id as a string
				String nextFormIdString = (String) servletContext.getAttribute("nextFormId");
				// if null set to "0"
				if (nextFormIdString == null) nextFormIdString = "0";
				// add 1 to the master form id
				formId = Integer.toString(Integer.parseInt( nextFormIdString ) + 1);
				// retain master
				servletContext.setAttribute("nextFormId", formId);
				// put into form ids
				formIds.put(application.getId() + "-" + application.getVersion(), formId);
				// retain for user
				session.setAttribute("userFormIds",formIds);									
			}			
		}					
		return formId;
	}
	
	@Override
	public FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String pageId)	{
		// retrieve
		return getUserFormPageControlValues(rapidRequest).get(pageId);
	}

	@Override
	public void setFormPageControlValues(RapidRequest rapidRequest, String pageId, FormPageControlValues pageControlValues) {		
		// if there are controls to store
		if (pageControlValues.size() > 0) {;
			// store them
			getUserFormPageControlValues(rapidRequest).put(pageId, pageControlValues);
		}		
	}
	
	@Override
	public String getFormPageControlValue(RapidRequest rapidRequest, String pageId, String controlId) {
		// get all user form page values
		Map<String,FormPageControlValues> userFormPageControlValues = getUserFormPageControlValues(rapidRequest);
		// if there are control values stored
		if (userFormPageControlValues.size() > 0) {
			// look for values from our page
			FormPageControlValues pageControlValues = userFormPageControlValues.get(pageId);
			// if we have some
			if (pageControlValues != null) {
				// loop them
				for (FormControlValue controlValue : pageControlValues) {
					// look for an id match
					if (controlValue.getId().equals(controlId)) return controlValue.getValue();
				}
			}
		}
		return null;
	}
	
	@Override
	public String getFormControlValue(RapidRequest rapidRequest, String controlId) {
		// split the controlid
		String[] controlIdParts = controlId.split("_");
		// use the parts to access the page and control id
		return getFormPageControlValue(rapidRequest, controlIdParts[0], controlId);
	}
	
	@Override
	public String getSummaryStartHtml(RapidRequest rapidRequest, Application application) {
		return "<h1 class='formSummaryTitle'>Form summary</h1>\n";
	}
	
	@Override
	public String getSummaryEndHtml(RapidRequest rapidRequest, Application application) {
		return "";
	}
		
	@Override
	public String getSummaryPageStartHtml(RapidRequest rapidRequest, Application application, Page page) {
		String label = page.getLabel();
		if (label == null) {
			label = page.getTitle();
		} else {
			if (label.trim().length() == 0) label = page.getTitle();
		}
		return "<div class='formSummaryPage'><h2>" + label + "</h2>\n";
	}
	
	@Override
	public String getSummaryPageEndHtml(RapidRequest rapidRequest, Application application, Page page) {
		return "</div>\n";
	}
	
	@Override
	public String getSummaryControlValueHtml(RapidRequest rapidRequest, Application application, Page page, FormControlValue controlValue) {
		Control control = page.getControl(controlValue.getId());
		if (control == null) {
			return "control " + controlValue.getId() + " cannot be found";
		} else {
			String label = control.getLabel();
			if (label == null) {
				return "";
			} else {
				String value = controlValue.getValue();
				// check for nulls
				if (value == null) value = "(no value)";
				// check for json
				if (value.startsWith("{") && value.endsWith("}")) {
					try {
						JSONObject jsonValue = new JSONObject(value);
						value = jsonValue.optString("text");
					} catch (Exception ex) {}
				}
				return "<span class='formSummaryControl'>" + label + " : " + control.getCodeText(value) + "</span>\n";
			}		
		}
	}
	
	@Override
	public String getSummaryPagesEndHtml(RapidRequest rapidRequest, Application application) {
		return "";
	}

	@Override
	public void submitForm(RapidRequest rapidRequest) throws Exception {
		// get the user session
		HttpSession session = rapidRequest.getRequest().getSession();
		// retrieve the form ids from the session
		Map<String,String> formIds = (Map<String, String>) session.getAttribute("userFormIds");
		// get the application
		Application application = rapidRequest.getApplication();
		// null check
		if (formIds != null) {
			// empty the form id - invalidating the form
			formIds.put(application.getId() + "-" + application.getVersion(), null);
		}
		// for a "real" form you would either write to your database form header record that it has been submitted		
	}
		
}
