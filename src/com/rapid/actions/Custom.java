/*

Copyright (C) 2020 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.bind.annotation.XmlType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/*

If Eclipse underlines the import above as an error, right click "RapidCustom" -> Properties -> "Java Build Path" -> "Libraries".
Expand the JRE library entry, select "Access rules", "Edit..." and "Add..." a "Resolution: Accessible" with a corresponding rule pattern "jdk/nashorn/**".

*/

public class Custom extends Action {

	// this action has generic inputs
	@XmlType(namespace="http://rapid-is.co.uk/custom")
	public static class Input {

		private String _itemId, _field, _inputField;

		public String getItemId() { return _itemId; }
		public void setItemId(String itemId) { _itemId = itemId; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		public String getInputField() { return _inputField; }
		public void setInputField(String inputField) { _inputField = inputField; }

		public Input() {};
		public Input(String itemId, String field, String inputField) {
			_itemId = itemId;
			_field = field;
			_inputField = inputField;
		}

	}

	// this action has generic outputs
	@XmlType(namespace="http://rapid-is.co.uk/custom")
	public static class Output {

		private String _outputField, _itemId, _field;

		public String getOutputField() { return _outputField; }
		public void setOutputField(String outputField) { _outputField = outputField; }

		public String getItemId() { return _itemId; }
		public void setItemId(String itemId) { _itemId = itemId; }

		public String getField() { return _field; }
		public void setField(String field) { _field = field; }

		public Output() {};
		public Output(String outputField, String itemId, String field) {
			_outputField = outputField;
			_itemId = itemId;
			_field = field;
		}

	}

	// static variables
	private static Logger _logger = LogManager.getLogger();
	private static ScriptEngine _engine;

	// private instance variables

	private List<Input> _inputs;
	private List<Output> _outputs;
	private List<Action> _successActions, _errorActions, _childActions;

	// properties

	public List<Input> getInputs() { return _inputs; }
	public void setInputs(List<Input> inputs) { _inputs = inputs; }

	public List<Output> getOutputs() { return _outputs; }
	public void setOutputs(List<Output> outputs) { _outputs = outputs; }

	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }

	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }

	// parameterless constructor (required for jaxb)
	Custom() { super(); }

	// designer constructor
	public Custom(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the parameterless constructor which sets the xml version
		this();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for inputs, successActions and errorActions
			if (!"inputs".equals(key) && !"outputs".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// grab any inputs
		JSONArray jsonInputs = jsonAction.optJSONArray("inputs");
		// if we got some
		if (jsonInputs != null) {
			// instantiate our array
			_inputs = new ArrayList<>();
			// loop them
			for (int i = 0; i < jsonInputs.length(); i++) {
				// get the input
				JSONObject jsonInput = jsonInputs.getJSONObject(i);
				// add it
				_inputs.add(new Input(jsonInput.optString("itemId"), jsonInput.optString("field"), jsonInput.optString("inputField")));
			}

		}

		// grab any outputs
		JSONArray jsonOutputs = jsonAction.optJSONArray("outputs");
		// if we got some
		if (jsonOutputs != null) {
			// instantiate our array
			_outputs = new ArrayList<>();
			// loop them
			for (int i = 0; i < jsonOutputs.length(); i++) {
				// get the input
				JSONObject jsonOutput = jsonOutputs.getJSONObject(i);
				// add it
				_outputs.add(new Output(jsonOutput.optString("outputField"), jsonOutput.optString("itemId"), jsonOutput.optString("field")));
			}

		}

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
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		// get the JavaScript
		String javaScript = getProperty("javascript");
		// if we have some javascript
		if (javaScript == null) {
			// send comments
			return "/* no JavaScript provided */";
		} else {

			// check server side
			boolean server = Boolean.parseBoolean(getProperty("server"));

			// if server-side
			if (server) {

				// start the more complex js
				String js = "";

				// start the data object with the type
				js += "var data = {};\n";

				// check if we have inputs
				if (_inputs != null) {

					// loop them
					for (int i = 0; i < _inputs.size(); i++) {
						// get the input
						Input input = _inputs.get(i);
						// get this item id
						String itemId = input.getItemId();
						// get this item field
						String itemField = input.getField();
						// get the inpute field
						String inputField = input.getInputField();
						// update the input field with the index if blank
						if (inputField == null || "".equals(inputField)) inputField = Integer.toString(i);

						// get their data and add to data object
						js += "data['" + input.getInputField().replace("'", "\\'") + "'] = " + Control.getDataJavaScript(rapidRequest.getRapidServlet().getServletContext(), application, page, itemId, itemField) + ";\n";

					}

				}

				// control can be null when the action is called from the page load
				String controlParam = "";
				if (control != null) controlParam = "&c=" + control.getId();

				// open the ajax call
				js += "$.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
				js += "  data: JSON.stringify(data),\n";
				js += "  error: function(server, status, message) {\n";

				// this avoids doing the errors if the page is unloading or the back button was pressed
				js += "    if (server.readyState > 0) {\n";

				// retain if error actions
				boolean errorActions = false;

				// prepare a default error hander we'll show if no error actions, or pass to child actions for them to use
				String defaultErrorHandler = "alert('Error with custom action : ' + server.responseText||message);";

				// add any error actions
				if (_errorActions != null) {
					// instantiate the jsonDetails if required
					if (jsonDetails == null) jsonDetails = new JSONObject();
					// count the actions
					int i = 0;
					// loop the actions
					for (Action action : _errorActions) {
						// retain that we have custom error actions
						errorActions = true;
						// if this is the last error action add in the default error handler
						if (i == _errorActions.size() - 1) jsonDetails.put("defaultErrorHandler", defaultErrorHandler);
						// add the js
						js += "         " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n         ") + "\n";
						// if this is the last error action and the default error handler is still present, remove it so it isn't sent down the success path
						if (i == _errorActions.size() - 1 && jsonDetails.optString("defaultErrorHandler", null) != null) jsonDetails.remove("defaultErrorHandler");
						// increase the count
						i++;
					}
				}
				// add default error handler if none in collection
				if (!errorActions) js += "        " + defaultErrorHandler + "\n";

				// close unloading check
				js += "    }\n";

				// close error actions
				js += "  },\n";

				// open success function
				js += "  success: function(data) {\n";

				// open if data check
				js += "    if (data) {\n";

				// add any outputs
				if (_outputs != null) {
					for (Output output : _outputs) {
						// get the output field
						String outputField = output.getOutputField();
						// get this item id
						String itemId = output.getItemId();
						// get this item field
						String itemField = output.getField();
						// get their data and add to data object
						js += "       " + Control.setDataJavaScript(rapidRequest.getRapidServlet().getServletContext(), application, page, itemId, itemField) + ";\n";
					}
				}

				// add any sucess actions
				if (_successActions != null) {
					for (Action action : _successActions) {
						js += "       " + action.getJavaScript(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n       ") + "\n";
					}
				}

				// close if data check
				js += "    }\n";
				// close success function
				js += "  }\n";

				// close ajax call
				js += "});";

				// we're done!
				return js;

			} else {
				// trim and send for front-end
				return javaScript.trim();
			}
		}
	}


	// get a json array from a ScriptObjectMirror
	private JSONArray getJSONArray(ScriptObjectMirror scriptObject) throws JSONException {

		if (scriptObject == null) {

			return null;

		} else {

			JSONArray jsonArray = new JSONArray();

			for (String key : scriptObject.keySet()) {

				Object object = scriptObject.get(key);

				if (object instanceof ScriptObjectMirror) {

					ScriptObjectMirror childScriptObject = (ScriptObjectMirror) object;

					if (childScriptObject.isArray()) {

						jsonArray.put(getJSONArray(childScriptObject));

					} else {

						jsonArray.put(getJSONObject(childScriptObject));

					}

				} else {

					jsonArray.put(object);

				}

			}

			return jsonArray;

		}

	}

	// get a json object from a ScriptObjectMirror
	private JSONObject getJSONObject(ScriptObjectMirror scriptObject) throws JSONException {

		if (scriptObject == null) {

			return null;

		} else {

			JSONObject jsonObject = new JSONObject();

			for (String key : scriptObject.keySet()) {

				Object object = scriptObject.get(key);

				if (object instanceof ScriptObjectMirror) {

					ScriptObjectMirror childScriptObject = (ScriptObjectMirror) object;

					if (childScriptObject.isArray()) {

						jsonObject.put(key, getJSONArray(childScriptObject));

					} else {

						jsonObject.put(key, getJSONObject(childScriptObject));

					}

				} else {

					jsonObject.put(key, object);

				}

			}

			return jsonObject;

		}

	}

	@Override
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonData) throws Exception {

		// https://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/

		// https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/prog_guide/javascript.html

		// we'll have _engine as a singleton - I think this is threadsafe, means we only need one
		if (_engine == null) {
			// get a new engine manager
			ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
			// get the nashorn engine
			_engine = scriptEngineManager.getEngineByName("nashorn");
		}

		// get the engine script context
		ScriptContext context = _engine.getContext();
		// string writer for standard output
		StringWriter sw = new StringWriter();
		// this gets print statments in the script to come out in the log
        context.setWriter(sw);
        // string writer for errors
        StringWriter swError = new StringWriter();
        // errors in the script should come out in the log
        context.setErrorWriter(swError);

		// get the JavaScript
		String javaScript = getProperty("javascript");

		// this is the bit that makes me worry if it is threadsafe, note the wrapper function that allows us to have "return" in our JavaScript
		_engine.eval("var f = function(data) {data = JSON.parse(data); " + javaScript + "\n}");

		// get an invocable from the engine - I think this is the threadsafe part
		Invocable invocable = (Invocable) _engine;

		// invoke our wrapper function and get our return
		Object response = invocable.invokeFunction("f", jsonData.toString());

		// the json data object we want to return
		JSONObject jsonDataOut = null;

		// check we got something back
		if (response == null) {

			// make an empty JSON Object
			jsonDataOut = new JSONObject();

		} else {

			// if the data out looks like json
			if (response instanceof ScriptObjectMirror) {

				// cast
				ScriptObjectMirror scriptObject = (ScriptObjectMirror) response;

				// make a JSONObject to return
				jsonDataOut = getJSONObject(scriptObject);

			} else {

				// make a Rapid data object
				jsonDataOut = new JSONObject();
				JSONArray jsonFields = new JSONArray();
				JSONArray jsonRows = new JSONArray();
				JSONArray jsonRow = new JSONArray();

				// add a single field
				jsonFields.put("value");

				// add output to row
				jsonRow.put(response);

				// add rows
				jsonRows.put(jsonRow);

				// add fields
				jsonDataOut.put("fields", jsonFields);
				// add rows
				jsonDataOut.put("rows", jsonRows);

			}

		} // dataOut check

		// if we have any standard output put it in the log
		if (sw.getBuffer().length() > 0) _logger.debug("Custom action " + getId() + " output:\n" + sw);

		// if we have any standard output put it in the log
		if (swError.getBuffer().length() > 0) _logger.error("Custom action " + getId() + " error:\n" + swError);

		// we're done!
		return jsonDataOut;

	}


}
