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

package com.rapid.actions;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.forms.FormAdapter;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

import org.json.JSONObject;

/*

This action runs JQuery against a specified control. Can be entered with or without the leading "." Such as hide(), or .css("disabled","disabled");

*/

public class Form extends Action {

	// parameterless constructor (required for jaxb)
	Form() { super(); }
	// designer constructor
	public Form(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);				
	}
	
	// methods
		
	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, com.rapid.core.Control control, JSONObject jsonDetails) throws Exception {

		// get the action type
		String actionType = getProperty("actionType");
		// prepare the js
		String js = "";
		
		// check the action type
		if ("next".equals(actionType)) {
			// next submits the form
			js = "$('form').submit();\n";
		} else if ("prev".equals(actionType)) {
			// go back
			js = "window.history.back();\n";
		} else if ("id".equals(actionType) || "val".equals(actionType)) {
			// get the dataDestination
			String destinationId = getProperty("dataDestination");			
			// first try and look for the control in the page
			Control destinationControl = page.getControl(destinationId);
			// check we got a control
			if (destinationControl == null) {
				js = "// destination control " + destinationId + " could not be found\n" ;
			} else if ("id".equals(actionType)) {
				// use the set data to copy in the form id
				js = "setData_" + destinationControl.getType() + "(ev, '" + destinationId + "', null, " + destinationControl.getDetails() + ", _formId);\n";
			} else if ("val".equals(actionType)) {
				// get the form adpater
				FormAdapter formAdapter = application.getFormAdapter();
				// get the value
				String value = formAdapter.getFormControlValue(rapidRequest, getProperty("dataSource"));					
				// enclose it if we got something
				if (value != null) value = "'" + value +"'";				
				// use the set data if we got something
				js = "setData_" + destinationControl.getType() + "(ev, '" + destinationId + "', null, " + destinationControl.getDetails() + ", " + value + ");\n";
			}										
		}		
		// return the js
		return js;
	}
				
}
