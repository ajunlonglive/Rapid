package com.rapid.actions;

import java.util.ArrayList;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Navigate extends Action {
	
	// details of the inputs
	public static class SessionVariable {
				
		private String _name, _itemId, _field;
		
		public String getName() { return _name; }
		public void setName(String name) { _name = name; }
		
		public String getItemId() { return _itemId; }
		public void setItemId(String itemId) { _itemId = itemId; }
		
		public String getField() { return _field; }
		public void setField(String field) { _field = field; }
		
		public SessionVariable() {};
		public SessionVariable(String name, String itemId, String field) {
			_name = name;
			_itemId = itemId;
			_field = field;
		}
		
	}
	
	// instance variables
	
	private ArrayList<SessionVariable> _sessionVariables;
	
	// properties
	
	public ArrayList<SessionVariable> getSessionVariables() { return _sessionVariables; }
	public void setSessionVariables(ArrayList<SessionVariable> sessionVariables) { _sessionVariables = sessionVariables; }

	// parameterless constructor for jaxb
	public Navigate() { super(); }	
	// json constructor for designer
	public Navigate(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call super constructor to set xml version
		super();
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties (except for sessionVariables)
			if (!"sessionVariables".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}
		// get the inputs collections
		JSONArray jsonInputs = jsonAction.optJSONArray("sessionVariables");
		// check it
		if (jsonInputs != null) {
			// initialise the collection
			_sessionVariables = new ArrayList<SessionVariable>();
			// loop it
			for (int i = 0; i < jsonInputs.length(); i++) {
				// get this input
				JSONObject jsonInput = jsonInputs.getJSONObject(i);
				// add it to the collection
				_sessionVariables.add(new SessionVariable(
					jsonInput.getString("name"),
					jsonInput.getString("itemId"),
					jsonInput.optString("field")
				));
			}
		}
	}
	
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		String pageId = getProperty("page");
		if (pageId == null) {
			return "";
		} else {
			// string into which we're about to build the session variables
			String sessionVariables = "";
			// check we have some
			if (_sessionVariables != null) {
				// loop
				for (SessionVariable sessionVariable : _sessionVariables) {
					// look for a control with this item id
					Control svControl = page.getControl(sessionVariable.getItemId());
					// only if we found a control (session variables will move in the session)
					if (svControl != null) {
						sessionVariables += "&" + sessionVariable.getName() + "=' + getData_" + svControl.getType() + "(ev, '" + svControl.getId() + "','" + sessionVariable.getField() + "', " + svControl.getDetails() + ") + '";
					}					
				}
			}
			// build the action string 
			String action = "Action_navigate('~?a=" + application.getId() + "&p=" + pageId + sessionVariables + "'," + Boolean.parseBoolean(getProperty("dialogue")) + ");";
			// replace any unnecessary characters
			action = action.replace(" + ''", "");
			// return it into the page!
			return action;
		}
		
	}	
		
}
