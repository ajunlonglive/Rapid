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
			
			js = "var data = "  + Control.getDataJavaScript(application, page, dataSourceId, dataSourceField) + ";\n";
									
			// we're going to work with the data destinations in a json array
			JSONArray jsonDataDestinations = null;
			
			// assume we have no need for an outputs array
			boolean outputsArray = false;
							
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
						
						// retrieve this data destination
						JSONObject jsonDataDesintation = jsonDataDestinations.getJSONObject(i);
						// get the control id
						String destinationId = jsonDataDesintation.getString("itemId");
						
						// split by escaped .
						String idParts[] = destinationId.split("\\.");
						// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
						if (idParts.length > 1) destinationId = idParts[0];
													
						// first try and look for the control in the page
						Control destinationControl = page.getControl(destinationId);
						// assume we found it
						boolean pageControl = true;
						// check we got a control
						if (destinationControl == null) {
							// now look for the control in the application
							destinationControl = application.getControl(destinationId);
							// set page control to false
							pageControl = false;
						} 
						
						// check we got one from either location
						if (destinationControl != null) {
							
							// get the field
							String destinationField = jsonDataDesintation.optString("field");
							// clean up the field								
							if (destinationField == null) destinationField = "";
							
							// get any details we may have
							String details = destinationControl.getDetailsJavaScript(application, page);
														
							// if the idParts is greater then 1 this is a set property
							if (idParts.length > 1) {
								// get the property from the second id part
								String property = idParts[1];
								// append the set property call
								js += "setProperty_" + destinationControl.getType() +  "_" + property + "(ev,'" + destinationControl.getId() + "','" + destinationField + "'," + details + ",data);\n";
							} else {
								// we will need an outputs array
								outputsArray = true;
								// set details to empty string or clean up
								if (details == null) {
									details = "";
								} else {
									// if this is a page control
									if (pageControl) {
										// the details will already be in the page so we can use the short form
										details = ",details:" + destinationControl.getId() + "details";
									} else {
										// write the full details
										details = ",details:" + details;
									}
								}
								// add the properties we need as a js object it will go into the array
								jsOutputs += "{id:'" + destinationControl.getId() + "',type: '" + destinationControl.getType() + "',field:'" + destinationField + "'" + details + "},";
							}
							
						}
																									
					} catch (JSONException e) {
						
					}
				}
				
				// if there was an outputs array 
				if (outputsArray) {
					
					// trim the last comma
					if (jsOutputs.length() > 0) jsOutputs = jsOutputs.substring(0, jsOutputs.length() - 1);
					
					// add to js as an array
					js += "var outputs = [" + jsOutputs + "];\n";
					// add the start of the call
					js += "Action_datacopy(ev, data, outputs";
					
					// get any copy type
					String copyType = getProperty("copyType");
					// check if we got one
					if (copyType == null) {
						
						// if not update to empty string
						copyType = "";
						
					} else {
						
						// add the copy type to the js
						js += ", '" + copyType + "'";
						// check the copy type
						if ("child".equals(copyType)) {
							// no merge data object
							js += ", null";						
							// look for a merge field
							String childField = getProperty("childField");
							// check if got we got one
							if (childField == null) {
								// call it child if not
								js += ", 'child'";						 
							} else {
								// add it
								js += ", '" + childField + "'";
							}
							
						} else if ("search".equals(copyType)) {
							
							// get the search data
							js += ", " + Control.getDataJavaScript(application, page, getProperty("searchSource"), getProperty("searchSourceField"));
							// look for a search field
							String searchField = getProperty("searchField");
							// add it if present
							if (searchField != null) {
								js += ", '" + searchField + "'";
							}
							
						} // copy type
						
					} // copy type check
					
					// close the copy data call
					js += ");\n";
					
				} // outputs array
												
			}
																								
		}
		
		return js;
				
	}
		
}
