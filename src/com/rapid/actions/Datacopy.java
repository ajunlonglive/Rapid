/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.actions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;

import com.rapid.server.RapidHttpServlet;

public class Datacopy extends Action {
			
	// constructors
	
	// used by jaxb
	public Datacopy() { super(); }
	// used by designer
	public Datacopy(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);				
	}
				
	// overrides
	
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		
		// the javascript we're making
		String js = "";
		
		// get the data source
		String dataSourceId = getProperty("dataSource");
		
		// check there is a datasource
		if (dataSourceId == null) {
			
			// no data source return a comment
			js = "// no data source for action " + getId();
			
		} else {
			
			String dataSourceField = getProperty("dataSourceField");
			
			js = "var data = "  + Control.getDataJavaScript(application, page, dataSourceId, dataSourceField) + "\n";
									
			// we're going to work with the data destinations in a json array
			JSONArray jsonDataDestinations = null;
							
			// try and get the data destinations from the properties into a json array, silently fail if not
			try { jsonDataDestinations = new JSONArray(getProperty("dataDestinations")); } 
			catch (Exception e) {}
			
			if (jsonDataDestinations == null) {
				
				// data source destinations not found return a comment
				js = "// data source destinations not found for " + getId();
				
			} else {
				
				// prepare a string for the outputs array
				String jsOutputs = "";
				// loop the json data destination collection
				for (int i = 0; i < jsonDataDestinations.length(); i++) {
					// try and make an output for this destination
					try {
						JSONObject jsonDataDesintation = jsonDataDestinations.getJSONObject(i);
						Control destinationControl = page.getControl(jsonDataDesintation.getString("itemId"));
						// check we got a control
						if (destinationControl != null) {								
							// get the field
							String dataDestinationField = jsonDataDesintation.optString("field");
							// clean up the field								
							if (dataDestinationField == null) dataDestinationField = "";
							// get any mappings we may have
							String details = destinationControl.getDetailsJavaScript(application, page);
							// set to empty string or clean up
							if (details == null) {
								details = "";
							} else {
								details = ", details: " + details;
							}
							// add the properties we need as a js object
							jsOutputs += "{id: '" + destinationControl.getId() + "', type: '" + destinationControl.getType() + "', field: '" + dataDestinationField + "'" + details + "},";
						}
					} catch (JSONException e) {}
				}
				// trim the last comma
				if (jsOutputs.length() > 0) jsOutputs = jsOutputs.substring(0, jsOutputs.length() - 1);
				// add to js as an array
				js += "  var outputs = [" + jsOutputs + "];\n";
				// add the call
				js += "  Action_datacopy(data, outputs);\n";
				
			}
																								
		}
		
		return js;
				
	}
		
}
