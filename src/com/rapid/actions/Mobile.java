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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

public class Mobile extends Action {
	
	// private instance variables
	private ArrayList<Action> _successActions, _errorActions, _childActions;
	
	// properties
	public ArrayList<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(ArrayList<Action> successActions) { _successActions = successActions; }
	
	public ArrayList<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(ArrayList<Action> errorActions) { _errorActions = errorActions; }
	
	// constructors
	
	// used by jaxb
	public Mobile() { 
		super(); 
	}
	// used by designer
	public Mobile(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super();
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for success and error actions
			if (!"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		} 
		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some
		if (jsonSuccessActions != null) {
			_successActions = Control.getActions(rapidServlet, jsonSuccessActions);
		}
		
		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some
		if (jsonErrorActions != null) {
			// instantiate our contols collection
			_errorActions = Control.getActions(rapidServlet, jsonErrorActions);
		}
	}
		
	// overridden methods
	
	@Override
	public List<Action> getChildActions() {			
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<Action>();
			// add child success actions
			if (_successActions != null) {
				for (Action action : _successActions) _childActions.add(action);			
			}
			// add child error actions
			if (_errorActions != null) {
				for (Action action : _errorActions) _childActions.add(action);			
			}
		}
		return _childActions;	
	}
	
	@Override
	public String getPageJavaScript(RapidHttpServlet rapidServlet, Application application, Page page, JSONObject jsonDetails) throws Exception {
		
		if (_successActions == null && _errorActions == null) {
			return null;
		} else {
			String js  = "";
			// get our id
			String id = getId();
			// get the control (the slow way)
			Control control = page.getActionControl(id);
			// check if we have any success actions
			if (_successActions != null) {
				js += "function " + id + "success(ev) {\n";
				for (Action action : _successActions) {
					js += "  " + action.getJavaScript(rapidServlet, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
				js += "}\n";
			}
			// check if we have any success actions
			if (_errorActions != null) {
				js += "function " + id + "_error(ev) {\n";
				for (Action action : _errorActions) {
					js += "  " + action.getJavaScript(rapidServlet, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
				js += "}\n";
			}
			return js;			
		}
		
	}
			
	@Override
	public String getJavaScript(RapidHttpServlet rapidServlet, Application application, Page page, Control control, JSONObject jsonDetails) {
		// prepare the js
		String js = "if (typeof _rapidmobile == 'undefined') {\n  alert('This action is only available in Rapid Mobile'); \n} else {\n";
		// get the type
		String type = getProperty("actionType");
		// check we got something
		if (type != null) {
			// check the type
			if ("addImage".equals(type)) {
				// get he gallery control Id
				String galleryControlId = getProperty("galleryControlId");
				// get the gallery control
				Control galleryControl = page.getControl(galleryControlId);
				// check if we got one
				if (galleryControl == null) {
					js += "  //galleryControl " + galleryControlId + " not found\n";
				} else {
					int maxSize = Integer.parseInt(getProperty("imageMaxSize"));
					int quality = Integer.parseInt(getProperty("imageQuality"));
					js += "  _rapidmobile.addImage('" + galleryControlId + "'," + maxSize + "," + quality + ");\n";
				}
			} else if ("uploadImages".equals(type)) {
				// gett he gallery control Id
				String galleryControlId = getProperty("galleryControlId");
				// get the gallery control
				Control galleryControl = page.getControl(galleryControlId);
				// check if we got one
				if (galleryControl == null) {
					js += "  //galleryControl " + galleryControlId + " not found\n";
				} else {
					js += "  var urls = '';\n";
					js += "  $('#" + galleryControlId + "').find('img').each( function() { urls += $(this).attr('src') + ',' });\n";
					// assume no success call back
					String successCallback = "null";
					// update to name of callback if we have any success actions
					if (_successActions != null) successCallback = "'" + getId() + "success'";
					// assume no error call back
					String errorCallback = "null";
					// update to name of callback  if we have any error actions
					if (_errorActions != null) errorCallback = "'" + getId() + "error'";
					// call it!
					js += "  _rapidmobile.uploadImages('" + galleryControlId + "', urls, " + successCallback + ", " + errorCallback + ");\n";
				}
			}
		}
		// close checkRapidMobile
		js += "}\n";
		// return an empty string
		return js;
	}
	
}
