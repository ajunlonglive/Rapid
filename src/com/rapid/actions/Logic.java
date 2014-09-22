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

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidHttpServlet.RapidRequest;

public class Logic extends Action {
	
	// static class
	public static class Value {
		
		// private instance variables
		private String _type, _controlId, _controlField, _constant, _system;
		
		// public properties
		public String getType() { return _type; }
		public void setType(String type) { _type = type; }
		
		public String getControlId() { return _controlId; }
		public void setControlId(String controlId) { _controlId = controlId; }
		
		public String getControlField() { return _controlField; }
		public void setControlField(String controlField) { _controlField = controlField; }
		
		public String getConstant() { return _constant; }
		public void setConstant(String constant) { _constant = constant; }
		
		public String getSystem() { return _system; }
		public void setSystem(String system) { _system = system; }
		
		public Value() {}
		public Value(JSONObject jsonValue) {
			_type = jsonValue.optString("type");
			_controlId = jsonValue.optString("controlId");
			_controlField = jsonValue.optString("controlField");
			_constant = jsonValue.optString("constant");
			_system = jsonValue.optString("system");
		}
		
		public String getArgument(Application application, Page page) {
			
			String arg = "null";
			
			// check the different types (these are in the properties.js file for Property_logicValue
			if ("CTL".equals(_type)) {
				if (_controlId != null) {
					arg = Control.getDataJavaScript(application, page, _controlId, _controlField);
				}
			} else if ("CNT".equals(_type)) {
				if (_constant != null) {
					// wrap in same quotes
					arg = "'" + _constant.replace("'", "\'")  + "'";
				}
			} else if ("SYS".equals(_type)) {
				if (_system != null) {
					// the available system values are specified above Property_logicValue in properties.js
					if ("mobile".equals(_system)) {
						arg = "(typeof _rapidmobile == 'undefined' ? false : true)";
					} else if ("online".equals(_system)) {
						arg = "(typeof _rapidmobile == 'undefined' ? true : _rapidmobile.isOnline())";
					} else {
						// pass through as literal
						arg = _system;
					}
				}				
			}
			
			return arg;
			
		}
		
	}
		
	// static variables
	private static Logger _logger = Logger.getLogger(Logic.class);
	
	// instance variables
	
	private Value _value1, _value2;
	private String _operation;
	private ArrayList<Action> _trueActions, _falseActions, _childActions;

	// properties
		
	public ArrayList<Action> getTrueActions() { return _trueActions; }
	public void setTrueActions(ArrayList<Action> trueActions) { _trueActions = trueActions; }
	
	public String getOperation() { return _operation; }
	public void setOperation(String operation) { _operation = operation; }
	
	public ArrayList<Action> getFalseActions() { return _falseActions; }
	public void setFalseActions(ArrayList<Action> falseActions) { _falseActions = falseActions; }
		
	// constructors
	
	// used by jaxb
	public Logic() { super(); }
	
	// used by designer
	public Logic(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for the ones we want directly accessible
			if (!"value1".equals(key) && !"operation".equals(key) && !"value2".equals(key) && !"trueActions".equals(key) && !"falseActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		} 
		
		// grab value1 from json
		JSONObject jsonValue1 = jsonAction.optJSONObject("value1");
		// instantiate if present
		if (jsonValue1 != null) _value1 = new Value(jsonValue1);
		
		// grab the operation
		_operation = jsonAction.getString("operation");
		
		// grab value1 from json
		JSONObject jsonValue2 = jsonAction.optJSONObject("value2");
		// instantiate if present
		if (jsonValue2 != null) _value2 = new Value(jsonValue2);
				
		// grab any successActions
		JSONArray jsonTrueActions = jsonAction.optJSONArray("trueActions");
		// if we had some
		if (jsonTrueActions != null) {
			_trueActions = Control.getActions(rapidServlet, jsonTrueActions);
		}
		
		// grab any errorActions
		JSONArray jsonFalseActions = jsonAction.optJSONArray("falseActions");
		// if we had some
		if (jsonFalseActions != null) {
			// instantiate our contols collection
			_falseActions = Control.getActions(rapidServlet, jsonFalseActions);
		}
		
	}
	
	// properties
	
	public Value getValue1() { return _value1; }
	public void setValue1(Value value) { _value1 = value; }
	
	public Value getValue2() { return _value2; }
	public void setValue2(Value value) { _value2 = value; }
				
	// overrides
	
	@Override
	public List<Action> getChildActions() {			
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<Action>();
			// add child success actions
			if (_trueActions != null) {
				for (Action action : _trueActions) _childActions.add(action);			
			}
			// add child error actions
			if (_falseActions != null) {
				for (Action action : _falseActions) _childActions.add(action);			
			}
		}
		return _childActions;	
	}
		
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		
		String js = "";
		
		// asume we couldn't make a condition
		String condition = null;
		
		// check we have everything we need to make a condition
		if (_value1 != null && _operation != null && _value2 != null) {
			// construct the condition
			condition = _value1.getArgument(application, page) + " " + _operation + " " + _value2.getArgument(application, page);
		}
		
		// if we were able to make a condition
		if (condition != null) {
			
			js += "if (" + condition + ") {\n";
			
			if (_trueActions != null) {
				for (Action action : _trueActions) js += "  " + action.getJavaScript(application, page, control).trim().replace("\n", "\n  ") + "\n";
			}
			
			js += "}";
			
			if (_falseActions != null) {
				js += " else {\n";
				for (Action action : _falseActions) js += "  " + action.getJavaScript(application, page, control).trim().replace("\n", "\n  ") + "\n";
				js += "}";
			}
			
			js+= "\n";
			
		}
						
		// return what we built			
		return js;
	}
	
	@Override
	public JSONObject doAction(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, JSONObject jsonAction) throws Exception {
		
		// This code could be optimised to only return required data, according to the outputs collection
		_logger.trace("Logic action : " + jsonAction);
				
		return null;
		
	}
	
}
