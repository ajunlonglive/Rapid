package com.rapid.core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidHttpServlet.RapidRequest;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public abstract class Action {
	
	protected HashMap<String,String> _properties;
	
	// all properties are stored here (allowing us to describe just in the .action.xml files)
	public HashMap<String,String> getProperties() { return _properties; }
	public void setProperties(HashMap<String,String> properties) { _properties = properties; }
		
	// these are some helper methods for common properties
	public void addProperty(String key, String value) {
		if (_properties == null) _properties = new HashMap<String,String>();
		_properties.put(key, value); 
	}
	public String getProperty(String key) { return _properties.get(key); }				
	public String getType() { return getProperty("type"); }		
	public String getVersion() { return getProperty("version"); }
	public String getId() { return getProperty("id"); }	
	
	// if any actions have success, fail, or other follow on actions this function musr return them
	public List<Action> getChildActions() { return null; }	
	
	// this is where any clientside action happens (must be implemented as every action is kicked off from the client side [for now anyway])
	public abstract String getJavaScript(Application application, Page page, Control control);

	// this is where any serverside action happens! (some actions are client side only)
	public JSONObject doAction(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, JSONObject jsonData) throws JSONException, JAXBException, IOException { return null; };
	
	// a parameterless constructor is required so they can go in the JAXB context and be unmarshalled 
	public Action() {}
	
	// json constructor for designer
	public Action(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws JSONException {
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties
			addProperty(key, jsonAction.get(key).toString());
		}
	}
	
}
