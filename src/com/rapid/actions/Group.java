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
import com.rapid.server.RapidRequest;

public class Group extends Action {

	// instance variables
	private List<Action> _actions, _successActions, _errorActions, _childActions;
	private List<String> _redundantActions;

	// properties

	public List<Action> getActions() { return _actions; }
	public void setActions(List<Action> actions) { _actions = actions; }

	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }

	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }

	// constructors

	// used by jaxb
	public Group() { super(); }

	// used by designer
	public Group(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for the ones we want directly accessible
			if (!"actions".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// grab any actions
		JSONArray jsonActions = jsonAction.optJSONArray("actions");
		// if we had some
		if (jsonActions != null) _actions = Control.getActions(rapidServlet, jsonActions);

		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some instantiate our collection
		if (jsonSuccessActions != null) _successActions = Control.getActions(rapidServlet, jsonSuccessActions);

		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some instantiate our collection
		if (jsonErrorActions != null) _errorActions = Control.getActions(rapidServlet, jsonErrorActions);

	}

	// overrides

	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<>();
			// add group actions
			if (_actions != null)
				_childActions.addAll(_actions);
			// add child success actions
			if (_successActions != null)
				_childActions.addAll(_successActions);
			// add child error actions
			if (_errorActions != null) {
				_childActions.addAll(_errorActions);
			}
		}
		return _childActions;
	}

	@Override
	public List<String> getRedundantActions() {
		// if the list is still null
		if (_redundantActions == null) {
			// instantiate if so
			_redundantActions = new ArrayList<>();
			// add our actionId which means the group will get it's own method
			_redundantActions.add(getId());
		}
		// return the list we made on initialisation
		return _redundantActions;
	}

	@Override
	public String getPageJavaScript(RapidRequest rapidRequest, Application application, Page page, JSONObject jsonDetails) throws Exception {

		// get this actions id
		String id = getId();

		// if there are success or error actions
		if ((_successActions != null && _successActions.size() > 0) || (_errorActions != null && _errorActions.size() > 0)) {

			// we add the successCheck to the the details in the getJavaScript (and empty is again each time before it runs)
			String js = "_" + id + "successChecks = {};\n\n";

			// get the control (the slow way)
			Control control = page.getActionControl(id);
			// check if we have any success actions
			if (_successActions != null) {
				// start the callback function
				js += "function " + id + "success(ev) {\n";
				// the success actions
				for (Action action : _successActions) {
					js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
				js += "}\n\n";
			}
			// check if we have any error actions, or we are doing uploadImages
			if (_errorActions != null) {
				// start the callback function
				js += "function " + id + "error(ev, server, status, message) {\n";
				// the error actions
				for (Action action : _errorActions) {
					js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
				}
				js += "}\n\n";
			}
			return js;

		} else {
			// no page js to make
			return null;
		}

	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		// start the js that we're making
		String js = "";

		// get this actions id
		String id = getId();

		// assume we're not doing any success check
		boolean successCheck = false;

		// see if we have any online success or error actions
		if ((_successActions != null && _successActions.size() > 0) || (_errorActions != null && _errorActions.size() > 0)) {

			// retain that we're doing a success check
			successCheck = true;

			// ensure we have a details object
			if (jsonDetails == null) jsonDetails = new JSONObject();

			// retain on the details that we have an offline page
			jsonDetails.put("successCheck", id);
			// set it to empty
			js += "_" + id + "successChecks = {};\n";

		}

		// add any actions
		if (_actions != null && _actions.size() > 0) {

			// loop the actions and add them
			for (Action action : _actions) js += action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim() + "\n";

			// check the success, if there is one
			if (successCheck) js += "successCheck('" + id + "', null, true, ev);\n";

		} else {

			// if there are no actions for any reason, but there is a success, call it immediately
			if (_successActions != null && _successActions.size() > 0) js += id + "success(ev);\n";

		}

		// return what we built
		return js;
	}

}
