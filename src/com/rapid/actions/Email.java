/*

Copyright (C) 2016 - Gareth Edwards / Rapid Information Systems

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

import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Email extends Action {
	
	// parameterless constructor (required for jaxb)
	public Email() { super(); }
	// designer constructor
	public Email(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);				
	}
	
	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		
		// start with empty JavaScript
        String js = "";
        
		// control can be null when the action is called from the page load
        String controlParam = "";
        if (control != null) controlParam = "&c=" + control.getId();
        
        // get the to control id
        String toControlId = getProperty("to");
        // get the to field
        String toField = getProperty("toField");
        
        // assume empty data
        js += "var data = {};\n";
        
        // get the get data js call for the to and to field
        String getToJs = Control.getDataJavaScript(servletContext, application, page, toControlId, toField);
        
        // check we got some
        if (getToJs == null) {
        	
        	js = "// email to control can't be found\n";
        	
        } else {
        	
	        // add the to address
	        js += "data.to = " + getToJs + ";\n";
	        
	        // get the body as a string
	        String stringBody = getProperty("body");
	        // if we got one
	        if (stringBody != null) {
	        	// get it into json
	        	JSONObject jsonBody = new JSONObject(stringBody);
	        	// get the inputs
	        	JSONArray jsonInputs = jsonBody.optJSONArray("inputs");
	        	// if we got some
	        	if (jsonInputs != null) {
	        		// add to the data object
	        		js += "data.inputs = [];\n";
	        		// now loop
	        		for (int i = 0; i < jsonInputs.length(); i++) {
	        			// get the input
	        			JSONObject jsonInput = jsonInputs.getJSONObject(i);
	        			// get value and add to arrray
	        			js += "data.inputs.push(" + Control.getDataJavaScript(servletContext, application, page, jsonInput.getString("itemId"), jsonInput.optString("field")) + ");\n";
	        		}
	        	}
	        }
	        	       			
			// open the ajax call
	        js += "$.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
	        js += "  data: JSON.stringify(data),\n";
	        js += "  error: function(server, status, fields, rows) {\n";
	        // this avoids doing the errors if the page is unloading or the back button was pressed
	        js += "    if (server.readyState > 0) {\n";
	        // show the server exception message
	        js += "      alert('Error with email action : ' + server.responseText||message);\n";	
	        // close unloading check
	        js += "    }\n";				       
	        // close error actions
	        js += "  }\n";
	        // close ajax call
	        js += "});\n";
        }

		
		return js;
	}
	
	@Override
    public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonData) throws Exception {
		
		// get the from address
		String from = getProperty("from");
		// get the subject
		String subject = getProperty("subject");
		// get the type
		String type = getProperty("emailType");
		// get the body as a string
        String stringBody = getProperty("body");
        // if we got one
        if (stringBody == null) {
        	throw new Exception("Email body must be provided");
        } else {
        	// get it into json
        	JSONObject jsonBody = new JSONObject(stringBody);
        	// get the template
        	String template = jsonBody.optString("template");
        	// if we got one
        	if (template == null) {
        		throw new Exception("Email template must be provided");
        	} else {
        		// get the to address
        		String to = jsonData.getString("to");
        		// set the text to  an empty string
        		String text = "";
        		// assume no template parts to merge
        		String[] templateParts = null;
        		// get any inputs
        		JSONArray jsonInputs = jsonData.optJSONArray("inputs");
        		// check we got inputs
        		if (jsonInputs != null) {
        			// check input size size
        			if (jsonInputs.length() > 1) {
        				// update the template with any parameters
        				template = rapidRequest.getApplication().insertParameters(rapidRequest.getRapidServlet().getServletContext(), template);
	        			// split the template on [[?]]
	        			templateParts = template.split("\\[\\[\\?\\]\\]");
        			}
        		}
        		// if we merge
        		if (templateParts == null) {
        			// no template parts to merge so just set text to template
        			text = template;        			
        		} else {
        			// loop the template parts
        			for (int i = 0; i < templateParts.length; i++) {
        				// append the part to text
        				text += templateParts[i];
        				// add any input
        				if (jsonInputs.length() > i && i < templateParts.length - 1) text += jsonInputs.getString(i);
        			}
        		}
        		// if the type is html
        		if ("html".equals(type)) {
        			// send email as html
        			com.rapid.core.Email.send(from, to, subject, "Please view this email with a tool that supports HTML", text);
        		} else {
        			// send email as text
        			com.rapid.core.Email.send(from, to, subject, text);
        		}
        	}
        }
		
		// return an empty json object
		return new JSONObject();
	}
	
}
