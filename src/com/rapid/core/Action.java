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

package com.rapid.core;

import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONObject;
import org.w3c.dom.Node;

import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidHttpServlet.RapidRequest;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public abstract class Action {
	
	// the version of this class's xml structure when marshelled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)	
	public static final int XML_VERSION = 1;
	
	// we can this version to be written into the xml when marshelled so we can upgrade any xml before marshelling
	private int _xmlVersion;
		
	// all properties are stored here (allowing us to describe just in the .action.xml files)
	protected HashMap<String,String> _properties;
			
	// the xml version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }
	
	// properties
	public HashMap<String,String> getProperties() { return _properties; }
	public void setProperties(HashMap<String,String> properties) { _properties = properties; }
		
	// these are some helper methods for common properties
	public void addProperty(String key, String value) {
		if (_properties == null) _properties = new HashMap<String,String>();
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
	
	// this generates the clientside javascript for the action to happen (must be implemented as every action is kicked off from the client side [for now anyway])
	public abstract String getJavaScript(Application application, Page page, Control control);

	// this is where any serverside action happens! (some actions are client side only)
	public JSONObject doAction(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, JSONObject jsonData) throws Exception { return null; };
	
	// this method can be overridden to check the xml versions, and upgrade any xml nodes representing specific actions before the xml document is unmarshalled
	public Node upgrade(Node actionNode) { return actionNode; }
	
	// a parameterless constructor is required so they can go in the JAXB context and be unmarshalled 
	public Action() {
		// set the xml version
		_xmlVersion = XML_VERSION;
	}
	
	// json constructor allowing properties to be sent in from the designer
	public Action(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// set the xml version
		_xmlVersion = XML_VERSION;
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties
			addProperty(key, jsonAction.get(key).toString());
		}
	}
	
}
