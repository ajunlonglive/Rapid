package com.rapid.core;

import org.json.JSONException;
import org.json.JSONObject;

public class Template {
	
	// private instance variables
	private String _type, _name, _css;

	// properties
	public String getType() { return _type; }
	public String getName() { return _name; }
	public String getCSS() { return _css; }

	
	// constructor
	public Template(String xml) throws JSONException {		
		// convert the xml string into JSON
		JSONObject jsonTemplate = org.json.XML.toJSONObject(xml).getJSONObject("template");
		// retain properties
		_type = jsonTemplate.getString("type");
		_name = jsonTemplate.getString("name");
		_css = jsonTemplate.getString("css");
	}
	
}
