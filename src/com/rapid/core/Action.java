/*

Copyright (C) 2016 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

package com.rapid.core;

import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONObject;
import org.w3c.dom.Node;

import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public abstract class Action {

	// the version of this class's xml structure when marshelled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)
	public static final int XML_VERSION = 1;

	// we can this version to be written into the xml when marshelled so we can upgrade any xml before marshelling
	private int _xmlVersion;

	// all properties are stored here (allowing us to describe just in the .action.xml files)
	protected HashMap<String,String> _properties;
	// whether this actions JavaScript should be placed in its own reusable function
	private boolean _avoidRedundancy;

	// the xml version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }

	// properties
	public HashMap<String,String> getProperties() { return _properties; }
	public void setProperties(HashMap<String,String> properties) { _properties = properties; }

	// a parameterless constructor is required so they can go in the JAXB context and be unmarshalled
	public Action() {
		// set the xml version
		_xmlVersion = XML_VERSION;
		// initialise properties
		_properties = new HashMap<>();
	}

	// json constructor allowing properties to be sent in from the designer
	public Action(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// run the parameterless constructor
		this();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties
			addProperty(key, jsonAction.get(key).toString());
		}
	}

	// these are some helper methods for common properties, ignoring some common ones that will always have their own getters/setters
	public void addProperty(String key, String value) {
		if (!"childActions".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key))
			_properties.put(key, value);
	}
	// retrieves a specified property
	public String getProperty(String key) { return _properties.get(key); }
	// retrieves the action type
	public String getType() { return getProperty("type"); }
	// retrieves the action id
	public String getId() { return getProperty("id"); }

	// if any actions have success, fail, or other follow on actions this function must return them all
	public List<Action> getChildActions() { return null; }

	// returns whether this action has been marked for redundancy avoidance and a separate JavaScript function and reusable calls will be created for it to avoid printing in the whole thing each time
	public boolean getAvoidRedundancy() { return _avoidRedundancy; }
	// this doesn't start with set so it's not marshalled to the xml file
	public void avoidRedundancy(boolean avoidRedundancy) { _avoidRedundancy = avoidRedundancy; }

	// if any actions run other actions return their id's when we generate the page JavaScript we will create a special action function which we will reuse to avoid redundantly recreating the js each time
	public List<String> getRedundantActions() { return null; }

	// this generates the clientside javascript at the top of the page for any reusable functions or global callbacks
	public String getPageJavaScript(RapidRequest rapidRequest, Application application, Page page, JSONObject jsonDetails) throws Exception { return null; }

	// this generates the clientside javascript inside the events for the action to happen (must be implemented as every action is kicked off from the client side [for now anyway])
	public abstract String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception;

	// this generates the clientside javascript header which has comments to help debugging - used in page and child actions
	public String getJavaScriptWithHeader(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		// assume no comments
		String comments = "";
		// if we have comments, add them and make it safe if they included a closer
		if (_properties.containsKey("comments")) comments = " - " + _properties.get("comments").replace("*/", "*");
		// add the header, then the regular JavaScript
		return "/* " + getType() + " action " + getId() + comments + " */\n" + getJavaScript(rapidRequest, application, page, control, jsonDetails);
	};

	// this is where any server-side action happens! (some actions are client side only)
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonData) throws Exception { return null; };

	// whether this action is a webservice
	public boolean isWebService() { return false; }

	// this method can be overridden to check the xml versions, and upgrade any xml nodes representing specific actions before the xml document is unmarshalled
	public Node upgrade(Node actionNode) { return actionNode; }

	// this produces a helpful string with the source of of the action / control, including ids if selected on the application
	protected String errorSourceMessage(Application application, Control control) {

		// start with a blank message
		String errorSourceMessage = "";

		// if we're in development and showing id's add that to the message
		if (application.getStatus() == Application.STATUS_DEVELOPMENT && application.getShowActionIds()) errorSourceMessage += " " + getId();

		// if the control is null it's the page
		if (control == null) {
			errorSourceMessage += " on page";
		} else {

			// add the control type
			errorSourceMessage += " on " + control.getType();

			// get the control name
			String controlName = control.getName();

			// if we're in development and showing id's
			if (application.getStatus() == Application.STATUS_DEVELOPMENT && application.getShowControlIds()) {
				// add id to the message
				errorSourceMessage += " " + control.getId();
				// add name if there is one
				if (controlName != null && !"".equals(controlName)) errorSourceMessage += " " + controlName;
			} else {
				// if there is no name
				if (controlName == null || "".equals(controlName)) {
					// add the id
					errorSourceMessage += " " + control.getId();
				} else {
					// add the name
					errorSourceMessage += " " + controlName;
				}

			} // development status check

		} // control null check

		// if this action has comments
		if (getProperties().containsKey("comments")) {
			// add the comments to the message with the apostrophes escaped
			errorSourceMessage += ("\\n\\n" + getProperty("comments").replace("'", "\\'"));
		}

		return errorSourceMessage.replace("'", "\'");
	}

	@Override
	public String toString() { return getClass().getName() + " - " + getId(); }

}
