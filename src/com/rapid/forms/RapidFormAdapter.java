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
	
	//  static finals
	private static final String USERFORMPAGECONTROLVALUES = "userFormPageControlValues";
	private static final String NEXTFORMID = "nextFormId";
	
	// constructor

	public RapidFormAdapter(ServletContext servletContext, Application application) {
		super(servletContext, application);
	}
	
	// class methods
		
	// the RapidFormAdapter holds all values in the user session so this method just gets them from there
	protected Map<String,FormPageControlValues> getUserFormPageControlValues(RapidRequest rapidRequest) throws Exception {	
		// get the user session
		HttpSession session = rapidRequest.getRequest().getSession();
		// get all app page control values from session
		Map<String,Map<String,FormPageControlValues>> userAppPageControlValues = (Map<String, Map<String, FormPageControlValues>>) session.getAttribute(USERFORMPAGECONTROLVALUES);
		// if null
		if (userAppPageControlValues == null) {
			// instantiate
			userAppPageControlValues = new HashMap<String, Map<String, FormPageControlValues>>();
			// add to session
			session.setAttribute(USERFORMPAGECONTROLVALUES, userAppPageControlValues);
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
		
		// example page control pre-population			
		// userPageControlValues.put("P2", new FormPageControlValues(new FormControlValue("P2_C1_", "Hello world !!!")));
		
		// return!
		return userPageControlValues;		
	}
	
	
	// overridden methods
	
	// this gets a new form id, when required, from an attribute in the servletContext
	@Override
	public String getNewFormId(RapidRequest rapidRequest) {
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		// the master form id as a string
		String nextFormIdString = (String) servletContext.getAttribute(NEXTFORMID);
		// if null set to "0"
		if (nextFormIdString == null) nextFormIdString = "0";
		// add 1 to the master form id
		String formId = Integer.toString(Integer.parseInt( nextFormIdString ) + 1);
		// retain it in the context
		servletContext.setAttribute(NEXTFORMID, formId);
		// return it
		return formId;
	}
	
	// uses our user session method to get the form page control values
	@Override
	public FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String pageId) throws Exception	{
		// retrieve
		return getUserFormPageControlValues(rapidRequest).get(pageId);
	}

	// uses our user session method to set the form page control values
	@Override
	public void setFormPageControlValues(RapidRequest rapidRequest, String pageId, FormPageControlValues pageControlValues) throws Exception {		
		// if there are controls to store
		if (pageControlValues.size() > 0) {;
			// store them
			getUserFormPageControlValues(rapidRequest).put(pageId, pageControlValues);
		}		
	}
	
	// uses our user session method to get a control value
	@Override
	public String getFormControlValue(RapidRequest rapidRequest, String controlId) throws Exception {
		// split the controlid
		String[] controlIdParts = controlId.split("_");
		// check we have enough to include the page
		if (controlIdParts.length > 1) {
			// get the page id from the first part
			String pageId = controlIdParts[0];
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
			} // page has values
		} // parts > 1		
		return null;		
	}
	
	// the start of the form summary	page
	@Override
	public String getSummaryStartHtml(RapidRequest rapidRequest, Application application) {
		return "<h1 class='formSummaryTitle'>Form summary</h1>\n";
	}
	
	// the end of the form summary page
	@Override
	public String getSummaryEndHtml(RapidRequest rapidRequest, Application application) {
		return "";
	}
		
	// the start of a page block in the form summary
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
	
	// the end of a page block in the form summary
	@Override
	public String getSummaryPageEndHtml(RapidRequest rapidRequest, Application application, Page page) {
		return "</div>\n";
	}
	
	// a page control's value in the form summary
	@Override
	public String getSummaryControlValueHtml(RapidRequest rapidRequest, Application application, Page page, FormControlValue controlValue) {
		if (controlValue.getHidden()) {
			return "";
		} else {
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
	}
	
	// the end of the page block
	@Override
	public String getSummaryPagesEndHtml(RapidRequest rapidRequest, Application application) {
		return "";
	}

	// submit the form - for the RapidFormAdapter it doesn't go anywhere but the user session state tracking is invalidated afterwards
	@Override
	public void submitForm(RapidRequest rapidRequest) throws Exception {
	}
	 	
	@Override
	public String getSubmittedHtml(RapidRequest rapidRequest) {
		return "Thank you";
	}

	@Override
	public String getSubmittedExceptionHtml(RapidRequest rapidRequest, Exception ex) {
		return "The following error occured : " + ex.getMessage();
	}
		
}
