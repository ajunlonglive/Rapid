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
import com.rapid.core.Email.Attachment;
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
	
	// protected instance methods
	
	// produced any js required for additional data from the client
	protected String getAdditionalDataJS(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		return "";
	}
	
	// produces any attachments
	protected Attachment[] getAttachments(RapidRequest rapidRequest, JSONObject jsonData) throws Exception {
		return null;
	}
	
	// overrides
	
	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		
		// get the servlet context
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		
		// start with empty JavaScript
        String js = "";
        
		// control can be null when the action is called from the page load
        String controlParam = "";
        if (control != null) controlParam = "&c=" + control.getId();
        
        // get the from control id
        String fromControlId = getProperty("from");
        // get the from field
        String fromField = getProperty("fromField");
        
        // get the to control id
        String toControlId = getProperty("to");
        // get the to field
        String toField = getProperty("toField");
        
        // assume empty data
        js += "var data = {};\n";
        
        // get the get data js call for the to and to field
        String getFromJs = Control.getDataJavaScript(servletContext, application, page, fromControlId, fromField);
        
        // get the get data js call for the to and to field
        String getToJs = Control.getDataJavaScript(servletContext, application, page, toControlId, toField);
        
        // check we got some
        if (getToJs == null) {
        	
        	js = "// email to control can't be found\n";
        	
        } else {
        	
        	// add the from address
	        js += "data.from = " + getFromJs + ";\n";
	        
	        // add the to address
	        js += "data.to = " + getToJs + ";\n";
	        
	        // get the contents as a string
	        String stringContent = getProperty("content");
	        // if we got one
	        if (stringContent != null) {
	        	// get it into json
	        	JSONObject jsonContent = new JSONObject(stringContent);
	        	// get the inputs
	        	JSONArray jsonInputs = jsonContent.optJSONArray("inputs");
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
	        
	        // add any js for additional data
	        js += getAdditionalDataJS(rapidRequest, application, page, control, jsonDetails);
	        	       			
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
		String from = jsonData.getString("from");
		// get the to address
		String to = jsonData.getString("to");
		// get the content as a string
        String stringContent = getProperty("content");
		// get the type
		String type = getProperty("emailType");		
        // if we got one
        if (from == null) {
        	throw new Exception("Email from address must be provided");
        } else if (to == null) {
        	throw new Exception("Email to address must be provided");
        } else if (stringContent == null) {
        	throw new Exception("Email content must be provided");
        } else {
        	// get it into json
        	JSONObject jsonContent = new JSONObject(stringContent);
        	// get the subject template
        	String subject = jsonContent.optString("subject");
        	// get the body template
        	String body = jsonContent.optString("body");
        	// if we got one
        	if (subject == null) {
        		throw new Exception("Email subject must be provided");
        	} else if (body == null) {
        		throw new Exception("Email body must be provided");
        	} else {
        		
        		// update the subject template with any parameters
				if (subject.contains("[[")) subject = rapidRequest.getApplication().insertParameters(rapidRequest.getRapidServlet().getServletContext(), subject);
        		// update the body template with any parameters
				if (body.contains("[[")) body = rapidRequest.getApplication().insertParameters(rapidRequest.getRapidServlet().getServletContext(), body);
        		
				// the index in the input values
				int i = 0;
				
        		// get any inputs
        		JSONArray jsonInputs = jsonData.optJSONArray("inputs");
        		// check we got inputs
        		if (jsonInputs != null) {
        			// check any inputs to look for
        			if (jsonInputs.length() > 0) {
        				        				
        				// split the subject part
                		String[] subjectParts = subject.split("\\?");
                		// if there is more than 1 part
                		if (subjectParts.length > 1) {
                			// set subject to first part
                			subject = subjectParts[0];
                			// loop the remaining parts
                			for (int j = 1; j < subjectParts.length; j++) {
                				// if there is an escape character or not more inputs
                				if (subject.endsWith("\\") || i >= jsonInputs.length()) {
                					// trim the \
                					subject = subject.substring(0, body.length() - 1);
                					// add back the ?
                					subject += "?";
                				} else {
                					// add the input value
                					subject += jsonInputs.getString(i);
                					// increment for next value
                					i ++;                					
                				}
                				// add this part
                				subject += subjectParts[j];
                			} // loop subject parts                			
                		} // got subject parts
                		// if we need an input at the end 
            			if (jsonContent.getString("subject").endsWith("?")) {
            				// if we have some left 
            				if (i < jsonInputs.length()) {
            					// remove last ? if still there
                				if (subjectParts.length == 1) subject = subject.substring(0, subject.length() - 1);
                				// add input value
                				subject += jsonInputs.getString(i);
                				// increment
                				i ++;
            				} else {
            					// add back ? if need be
            					if (subjectParts.length > 1) subject += "?";
            				} // got inputs
            			} // subject ends in ?

                		// split the body parts
                		String[] bodyParts = body.split("\\?");
                		// if there is more than 1 part
                		if (bodyParts.length > 1) {
                			// set body to first part
                			body = bodyParts[0];
                			// loop the remaining parts
                			for (int j = 1; j < bodyParts.length; j++) {
                				// if there is an escape character or not more inputs
                				if (body.endsWith("\\") || i >= jsonInputs.length()) {
                					// trim the \
                					body = body.substring(0, body.length() - 1);
                					// add back the ?
                					body += "?";
                				} else {
                					// add the input value
                					body += jsonInputs.getString(i);
                					// increment for next value
                					i ++;                					
                				}
                				// add this part
                				body += bodyParts[j];
                			} // loop body parts                			
                		} // got body parts
                		// if we need an input at the end 
            			if (jsonContent.getString("body").endsWith("?")) {
            				// if we have inputs some left 
            				if (i < jsonInputs.length()) {
            					// remove last ? if still there
                				if (bodyParts.length == 1) body = body.substring(0, body.length() - 1);
                				// add input value
                				body += jsonInputs.getString(i);
                				// increment
                				i ++;
            				} else {
            					// add back ? if need be
            					if (bodyParts.length > 1) body += "?";
            				} // got inputs
            			} // body ends with ?
            			
        			} // got inputs
        		} // inputs not null
        		
        		// if the type is html
        		if ("html".equals(type)) {
        			// send email as html
        			com.rapid.core.Email.send(from, to, subject, "Please view this email with an application that supports HTML", body, getAttachments(rapidRequest, jsonContent));
        		} else {
        			// send email as text
        			com.rapid.core.Email.send(from, to, subject, body, null, getAttachments(rapidRequest, jsonContent));
        		}
        	}
        }
		
		// return an empty json object
		return new JSONObject();
	}
	
}
