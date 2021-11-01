/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

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

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Page.Variables;
import com.rapid.core.Parameter;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DataFactory.Parameters;
import com.rapid.data.DatabaseConnection;
import com.rapid.data.SQLiteDataFactory;
import com.rapid.server.ActionCache;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Database extends Action {

	// details of the query (inputs, sql, outputs)
	public static class Query {

		private List<Parameter> _inputs, _outputs;
		private String _sql;
		private boolean _multiRow;
		private int _databaseConnectionIndex;

		public List<Parameter> getInputs() { return _inputs; }
		public void setInputs(List<Parameter> inputs) { _inputs = inputs; }

		public List<Parameter> getOutputs() { return _outputs; }
		public void setOutputs(List<Parameter> outputs) { _outputs = outputs; }

		public String getSQL() { return _sql; }
		public void setSQL(String sql) { _sql = sql; }

		public boolean getMultiRow() { return _multiRow; }
		public void setMultiRow(boolean multiRow) { _multiRow = multiRow; }

		public int getDatabaseConnectionIndex() { return _databaseConnectionIndex; }
		public void setDatabaseConnectionIndex(int databaseConnectionIndex) { _databaseConnectionIndex = databaseConnectionIndex; }

		public Query() {};
		public Query(List<Parameter> inputs, List<Parameter> outputs, String sql, boolean multiRow, int databaseConnectionIndex) {
			_inputs = inputs;
			_outputs = outputs;
			_sql = sql;
			_multiRow = multiRow;
			_databaseConnectionIndex = databaseConnectionIndex;
		}

	}

	// static variables
	private static Logger _logger = LogManager.getLogger(Database.class);

	// instance variables

	private Query _query;
	private boolean _showLoading, _mergeChildren;
	private List<Database> _childDatabaseActions;
	private List<Action> _successActions, _errorActions, _childActions;
	private List<Integer> _parameterMap;

	// properties

	public Query getQuery() { return _query; }
	public void setQuery(Query query) { _query = query; }

	public boolean getShowLoading() { return _showLoading; }
	public void setShowLoading(boolean showLoading) { _showLoading = showLoading; }

	public List<Database> getChildDatabaseActions() { return _childDatabaseActions; }
	public void setChildDatabaseActions(List<Database> childDatabaseActions) { _childDatabaseActions = childDatabaseActions; };

	public boolean getMergeChildren() { return _mergeChildren; }
	public void setMergeChildren(boolean mergeChildren) { _mergeChildren = mergeChildren; }

	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }

	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }

	// constructors

	// used by jaxb
	public Database() {
		// set the xml version, etc
		super();
		// default merge children to true for older applications - new ones will have it set to false by default by the designer
		_mergeChildren = true;
	}
	// used by designer
	public Database(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for query
			if (!"query".equals(key) && !"showLoading".equals(key) && !"childDatabaseActions".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)  && !"childActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
			// if this is mergeChildren we need to update our property variable too, to deal with legacy values
			if ("mergeChildren".equals(key)) _mergeChildren = jsonAction.optBoolean(key);
		}

		// try and build the query object
		JSONObject jsonQuery = jsonAction.optJSONObject("query");

		// check we got one
		if (jsonQuery != null) {
			// get the parameters
			ArrayList<Parameter> inputs = getParameters(jsonQuery.optJSONArray("inputs"));
			ArrayList<Parameter> outputs = getParameters(jsonQuery.optJSONArray("outputs"));
			String sql = jsonQuery.optString("SQL");
			boolean multiRow = jsonQuery.optBoolean("multiRow");
			int databaseConnectionIndex = jsonQuery.optInt("databaseConnectionIndex");
			// make the object
			_query = new Query(inputs, outputs, sql, multiRow, databaseConnectionIndex);
		}

		// look for showLoading
		_showLoading = jsonAction.optBoolean("showLoading");

		// grab any successActions
		JSONArray jsonChildDatabaseActions = jsonAction.optJSONArray("childDatabaseActions");
		// if we had some
		if (jsonChildDatabaseActions != null) {
			// instantiate collection
			_childDatabaseActions = new ArrayList<>();
			// loop them
			for (int i = 0; i < jsonChildDatabaseActions.length(); i++) {
				// get one
				JSONObject jsonChildDatabaseAction = jsonChildDatabaseActions.getJSONObject(i);
				// instantiate and add to collection
				_childDatabaseActions.add(new Database(rapidServlet, jsonChildDatabaseAction));
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

	// this is used to get both input and output parameters
	private ArrayList<Parameter> getParameters(JSONArray jsonParameters) throws JSONException {
		// prepare return
		ArrayList<Parameter> parameters = null;
		// check
		if (jsonParameters != null) {
			// instantiate collection
			parameters = new ArrayList<>();
			// loop
			for (int i = 0; i < jsonParameters.length(); i++) {
				// instantiate member
				Parameter parameter = new Parameter(
					jsonParameters.getJSONObject(i).optString("itemId"),
					jsonParameters.getJSONObject(i).optString("field")
				);
				// add member
				parameters.add(parameter);
			}
		}
		// return
		return parameters;
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
			// add child error actions
			if (_childDatabaseActions != null) {
				for (Action action : _childDatabaseActions) _childActions.add(action);
			}
		}
		return _childActions;
	}

	public String getLoadingJS(Page page, List<Parameter> parameters, boolean show) {
		String js = "";
		// check there are parameters
		if (parameters != null) {
			// loop the output parameters
			for (int i = 0; i < parameters.size(); i++) {
				// get the parameter
				Parameter output = parameters.get(i);
				// get the control the data is going into
				Control control = page.getControl(output.getItemId());
				// check the control still exists
				if (control != null) {
					if ("grid".equals(control.getType())) {
						if (show) {
							js += "$('#" + control.getId() + "').showLoading();\n";
						} else {
							js += "$('#" + control.getId() + "').hideLoading();\n";
						}
					}
				}
			}
		}
		return js;
	}

	// private function to get inputs into the query object, reused by child database actions
	private String getInputsJavaScript(ServletContext servletContext, Application application, Page page, Query query) {

		// assume it'll be an empty string
		String js = "";

		// if there is a query
		if (query != null) {

			// get the inputs from the query
			List<Parameter> inputs = query.getInputs();

			// if we were given some
			if (inputs != null) {

				// check there is at least one
				if (inputs.size() > 0) {

					// get the first itemId (this is the only one visible to the users)
					String sourceItemId = inputs.get(0).getItemId();

					js += "[";

					// loop them
					for (int i = 0; i < inputs.size(); i++) {
						// get the parameter
						Parameter parameter = inputs.get(i);
						// get this item id
						String itemId = parameter.getItemId();
						// get this item field
						String itemField = parameter.getField();
						// if there was an id
						if (itemId != null) {

							// add the input item
							js += "{";
							if (query.getMultiRow() && itemId.equals(sourceItemId)) {
								js += "field: '" + itemField + "'";
							} else {
								js += "id: '" + itemId + (itemField == null || "".equals(itemField) ? "" : "." + itemField) + "', ";
								js += "value:" + Control.getDataJavaScript(servletContext, application, page, itemId, itemField);
							}
							js += "}";
							// add comma if not last item
							if (i < inputs.size() - 1) js += ", ";

						} // got item

					} // loop inputs

					// close the array
					js += "]";

					// if this is a multirow query
					if (query.getMultiRow()) {
						// add the field-less get data for the first item the first parameter
						js += ", '" + sourceItemId + "', " + Control.getDataJavaScript(servletContext, application, page, sourceItemId, null);
					}

				} // inputs > 0

			} // got inputs

		} // got query

		// if we got no inputs set to null
		if (!js.startsWith("[")) js = "null";

		// return
		return js;
	}

	// private function to get outputs into a string, reused by child database actions
	private String getOutputsJavaScript(ServletContext servletContext, Application application, Page page, List<Parameter> outputs, String childName) {
		// the outputs array we're going to make
		String jsOutputs = "";
		// any property outputs that must be done separately;
		String jsPropertyOutputs = "";
		// loop the output parameters
		for (int i = 0; i < outputs.size(); i++) {
			// get the parameter
			Parameter output = outputs.get(i);
			// get the id
			String outputId = output.getItemId();
			// get the id parts
			String[] idParts = outputId.split("\\.");
			// if there is more than 1 part we are dealing with set properties, for now just update the output id
			if (idParts.length > 1) outputId = idParts[0];

			// get the control the data is going into
			Control outputControl = page.getControl(outputId);
			// assume we found it
			boolean pageControl = true;
			// if not found in the page
			if (outputControl == null) {
				// try the application
				outputControl = application.getControl(servletContext, outputId);
				// set page control to false
				pageControl = false;
			}
			// check we got one
			if (outputControl == null) {
				jsOutputs += " /* output not found for " + outputId + "*/ ";
			} else {
				// get any details we may have
				String details = outputControl.getDetailsJavaScript(application, page);
				// set to empty string or clean up
				if (details == null) {
					details = "";
				} else {
					// if this is a page control
					if (pageControl) {
						// the details will already be in the page so we can use the short form
						details = outputControl.getId() + "details";
					}
					// add details property with json details
					details = ", details: " + details;
				}
				// start the jsOutputs
				jsOutputs += "{id: '" + outputControl.getId() + "', type: '" + outputControl.getType() + "', field: '" + output.getField() + "'" + details;
				// if there are two parts this is a property
				if (idParts.length > 1) {
					// get the property from the second id part
					String property = idParts[1];
					// append the property
					jsOutputs += ", property: '" + property + "'";
				} // property / control check
				// close the jsOutputs
				jsOutputs += "},";
			} // control found check
		} // outputs loop
		// remove the last comma from any conventional outputs
		if (jsOutputs.length() > 0) jsOutputs = jsOutputs.substring(0, jsOutputs.length() - 1);
		// wrap the outputs with their variable
		jsOutputs = "var outputs" + childName + " = [" + jsOutputs + "]";
		// if jsPropertyOutputs, add them before
		if (jsPropertyOutputs.length() > 0) jsOutputs = jsPropertyOutputs.trim().replace("\n", "\n      ") + "\n      " + jsOutputs;
		// return
		return jsOutputs;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		String js = "";

		if (_query != null) {

			// get the rapid servlet
			RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

			// get the sequence for this action requests so long-running early ones don't overwrite fast later ones (defined in databaseaction.xml)
			js += "var sequence = getDatabaseActionSequence('" + getId() + "');\n";

			// open the js function to get the input data
			js += "var data = getDatabaseActionInputData(" + _query.getMultiRow() + ", ";

			// get the inputs
			js += getInputsJavaScript(rapidServlet.getServletContext(), application, page, _query);

			// close the js function to get the input data
			js += ");\n";

			// drop in the query variable used to collect the inputs, and hold the sequence
			js += "var query = { data: data, sequence: sequence };\n";

			// assume no child queries
			boolean childQueries = false;

			// look for any _childDatabaseActions
			if (_childDatabaseActions != null && _childDatabaseActions.size() > 0) {
				// remember we have child queries
				childQueries = true;
				// add a collection into the parent
				js += "query.childQueries = [];\n";
				// count them
				int i = 1;
				// loop them
				for (Database childDatabaseAction : _childDatabaseActions) {
					// get the childQuery
					Query childQuery = childDatabaseAction.getQuery();
					// open function to get input data
					js += "var childData" + i + " = getDatabaseActionInputData(" + childQuery.getMultiRow() + ", ";
					// add inputs
					js += getInputsJavaScript(rapidServlet.getServletContext(), application, page, childQuery);
					// close the function
					js += ");\n";
					// create object
					js += "var childQuery" + i + " = { data: childData" + i + ", index: " + (i - 1) + " };\n";
					// add to query
					js += "query.childQueries.push(childQuery" + i + ");\n";
					// increment the counter
					i ++;
				}
			}

			// control can be null when the action is called from the page load
			String controlParam = "";
			if (control != null) controlParam = "&c=" + control.getId();

			// get the outputs
			List<Parameter> outputs = _query.getOutputs();

			// get the js to hide the loading (if applicable)
			if (_showLoading) js += getLoadingJS(page, outputs, true);

			// stringify the query
			js += "query = JSON.stringify(query);\n";

			// open the ajax call
			js += "$.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
			js += "  data: query,\n";
			js += "  error: function(server, status, message) {\n";

			// hide the loading javascript (if applicable)
			if (_showLoading) js += "    " + getLoadingJS(page, outputs, false);

			// add standard error actions with offline and working handling
			js += getErrorActionsJavaScript(rapidRequest, application, page, control, jsonDetails, _errorActions);

			// open success function
			js += "  success: function(data) {\n";

			// hide the loading javascript (if applicable)
			if (_showLoading) js += "  " + getLoadingJS(page, outputs, false);

			// open if data check
			js += "    if (data) {\n";

			// check there are outputs
			if (outputs != null) {
				// if there are parent outputs
				if (outputs.size() > 0) {
					// add the parent outputs property
					js += "      " + getOutputsJavaScript(rapidServlet.getServletContext(), application, page, outputs, "") + ";\n";
					// add indent
					js += "      ";
					// if there are child queries, add child queries check
					if (childQueries) js += "if (data.fields && data.fields.length > 0) {\n        if (data.fields[0].indexOf('childAction') == 0) {\n          Action_database(ev,'" + getId() + "', {sequence:data.sequence, fields:[], rows:[]}, outputs);\n        } else {\n          ";
					// send them and the data to the database action (unless there are children and this parent has been skipped)
					js += "Action_database(ev,'" + getId() + "', data, outputs);\n";
					// close the extra child queries check
					if (childQueries) js += "        }\n      }\n";
				}
			} // outputs null check

			// if we are expecting child action results
			// check for any child database actions
			if (_childDatabaseActions != null) {
				// loop them
				for (int i = 0; i < _childDatabaseActions.size(); i++) {
					// get the outputs
					List<Parameter> childOutputs = _childDatabaseActions.get(i).getQuery().getOutputs();
					// if it has out puts
					if (childOutputs != null && childOutputs.size() > 0) {
						// get the name of this child
						String childName = "childAction" + (i + 1);
						// get the outputs
						js += "      " + getOutputsJavaScript(rapidServlet.getServletContext(), application, page, childOutputs, childName) + ";\n";
						// get the data
						js += "      Action_database(ev,'" + getId() + "', data, " + "outputs" + childName + ",'" + childName + "');\n";
					} // outputs check
				} // child action loop
			} // child action check

			// close if data check
			js += "    }\n";

			// get the standardised JavaScript to hide any working page only if this action has no children
			js += getWorkingPageHideJavaScript(jsonDetails, _successActions, "    ");

			// add any success actions
			if (_successActions != null) {
				for (Action action : _successActions) {
					js += "    " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n    ") + "\n";
				}
			}

			// close success function
			js += "  }\n";

			// close ajax call
			js += "});";
		}

		// return what we built
		return js;
	}

	private String getJsonInputValue(JSONArray jsonFields, JSONArray jsonRow, String id) {
		for (int j = 0; j < jsonFields.length(); j++) {
			// get the id from the fields
			String jsonId = jsonFields.optString(j);
			// if the id we want matches this one
			if (id.equalsIgnoreCase(jsonId)) {
				// return the value
				return jsonRow.optString(j,null);
			}
		}
		return null;
	}

	// finds ? followed by a name optionally in quotes, or ? followed by a name of lower and/or upper case letters - we'll find any of these first and convert them to numbers, only limitation is names outside of quotes can't start with numbers
	private static String _namedParameterSlotRegex = "(\\?\"[^\"]+\")|(\\?(?=[a-zA-Z])\\w*)";
	// finds ? followed by a name in quotes, or ? followed by 0 or more numbers, or ? on their own
	private static String _parameterSlotRegex = "(\\?\"[^\"]+\")|(\\?\\w\\S*)|\\?";
	// finds ? followed by a number
	private static String _unspecifySlotsRegex = "(\\?\\d*)";


	public JSONObject doQuery(RapidRequest rapidRequest, JSONObject jsonAction, Application application, DataFactory df) throws Exception {

		// place holder for the object we're going to return
		JSONObject jsonData = null;

		// get the rapidServlet
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

		ServletContext context = rapidServlet.getServletContext();

		// retrieve the sql
		String sql = _query.getSQL();

		// only if there is some sql is it worth going further
		if (sql != null) {

			// merge in any application parameters
			sql = application.insertParameters(context, sql);

			// get any json inputs
			JSONObject jsonInputData = jsonAction.optJSONObject("data");

			// initialise the parameters list - there is a parameters object in the list for each row, allowing for multi-row queries
			ArrayList<Parameters> parametersList = new ArrayList<>();

			// populate the parameters from the inputs collection (we do this first as we use them as the cache key due to getting values from the session)
			if (_query.getInputs() == null) {

				// just add an empty parameters member if no inputs
				parametersList.add(new Parameters());

			} else {

				// if there is input data
				if (jsonInputData != null) {

					// get any input fields
					JSONArray jsonFields = jsonInputData.optJSONArray("fields");
					// get any input rows
					JSONArray jsonRows = jsonInputData.optJSONArray("rows");

					// if we have fields and rows
					if (jsonFields != null && jsonRows != null) {

						// if we haven't mapped the parameters yet to deal with ? followed by a number or name, this is expensive, so we'll do it once and cache it
						if (_parameterMap == null) {

							// retain the original inputs
							List<Parameter> originalInputs = _query.getInputs();
							// if null make an empty list
							if (originalInputs == null) originalInputs = new ArrayList<>();
							// a list of input names which will include the field
							List<String> inputNames = new ArrayList<>();
							// loop the original inputs
							for (int i = 0; i < originalInputs.size(); i++) {
								// get this input
								Parameter input = originalInputs.get(i);
								// make its name its id
								String inputName = input.getItemId();
								// if it has a field add that on
								if (!input.getField().isEmpty()) inputName += "." + input.getField();
								// add this to our input names list
								inputNames.add(inputName);
							}

							// the parameter map we are making - it'll be size of the mapped parameters but hold the index back to the original ones - we'll use it below to populate each data row for the parameters list
							_parameterMap = getParameterMap(sql, inputNames, application, context);

						}

						// loop the input rows (only the top row if not multirow)
						for (int i = 0; i < jsonRows.length() && (_query.getMultiRow() || i == 0); i ++) {

							// get this jsonRow
							JSONArray jsonRow = jsonRows.getJSONArray(i);
							// make the parameters for this row
							Parameters parameters = new Parameters();

							// loop the query inputs
							for (Parameter input : _query.getInputs()) {
								// get the input id
								String id = input.getItemId();
								// get the input field
								String field = input.getField();
								// add field to id if present
								if (field != null && !"".equals(field)) id += "." + field;
								// retain the value
								String value = null;
								// if it looks like a control, or a system value (bit of extra safety checking)
								if (id.indexOf("_C") > 0 || id.indexOf("System.") == 0) {
									// check special cases
									switch (id) {
										case "System.device" :
											// get the device from the request
											value = rapidRequest.getDevice();
											break;
										case "System.user name" :
											// get the user name from the session (don't trust the front end)
											value = rapidRequest.getUserName();
											break;
										default :
											// get the value from the json inputs
											value = getJsonInputValue(jsonFields, jsonRow, id);
									}
								} else {
									// didn't look like a control so check page parameters
									if (rapidRequest.getPage() != null) {
										// get page variables
										Variables pageVariables = rapidRequest.getPage().getVariables();
										// check for page parameters
										if (pageVariables != null) {
											// if this is one
											if (pageVariables.contains(id)) {
												// get the value
												value = getJsonInputValue(jsonFields, jsonRow, id);
											}
										}
									}
								}
								// if still null try the session
								if (value == null) value = (String) rapidRequest.getSessionAttribute(input.getItemId());
								// add the parameter
								parameters.add(value);
							}

							// map any parameters (also checks if we need to)
							parameters = mapParameters(_parameterMap, parameters);

							// add the parameters to the list
							parametersList.add(parameters);

						} // row loop

					} // input fields and rows check

				} // input data check

			} // query inputs check

			// placeholder for the action cache
			ActionCache actionCache = rapidRequest.getRapidServlet().getActionCache();

			// if an action cache was found
			if (actionCache != null) {

				// log that we found action cache
				_logger.debug("Database action cache found");

				// attempt to fetch data from the cache
				jsonData = actionCache.get(application.getId(), getId(), parametersList.toString());

			}

			// if there isn't a cache or no data was retrieved
			if (jsonData == null) {

				try {

					// instantiate jsonData
					jsonData = new JSONObject();
					// fields collection
					JSONArray jsonFields = new JSONArray();
					// rows collection can start initialised
					JSONArray jsonRows = new JSONArray();

					// remove any numbers/names after ?'s in the sql so it makes sense to jdbc
					sql = unspecifySqlSlots(sql);

					// trim the sql
					sql = sql.trim();

					// clean the sql for checking - it has been trimmed already (declare is the longest we're looking for so far)
					String sqlCheck = sql.replace(" ", "").toLowerCase();

					// if it is more than 7 characters just trim it as "declare" is the longest we check for next - some parent statements are empty!
					if (sqlCheck.length() > 7) sqlCheck = sqlCheck.substring(0, 7);

					// check the verb
					if (sqlCheck.startsWith("select") || sqlCheck.startsWith("with") || sqlCheck.startsWith("exec")) {

						// if select set readonly to true (makes for faster querying) - but not for SQLite as it throws an exception if done after the connection is established
						if (sqlCheck.startsWith("select") && !df.getConnectionAdapter().getConnectionString().toLowerCase().contains("sqlite")) df.setReadOnly(true);

						// got fields indicator
						boolean gotFields = false;

						// loop the parameterList getting a result set for each parameters (input row)
						for (Parameters parameters : parametersList) {

							// get the result set!
							ResultSet rs = df.getPreparedResultSet(rapidRequest, sql, parameters);

							// check we got one
							if (rs != null) {

								// date formatters we might need but only want to fetch / initialise once
								SimpleDateFormat localDateFormatter = null;
								SimpleDateFormat localDateTimeFormatter = null;

								// assume results
								boolean gotResults = true;
								// get the statement
								Statement st = rs.getStatement();

								while (gotResults) {

									// get this resultset's meta data for the field names
									ResultSetMetaData rsmd = rs.getMetaData();

									// loop the result set
									while (rs.next()) {

										// initialise the row
										JSONArray jsonRow = new JSONArray();

										// loop the columns
										for (int i = 0; i < rsmd.getColumnCount(); i++) {
											// add the field name to the fields collection if not done yet
											if (!gotFields) jsonFields.put(rsmd.getColumnLabel(i + 1));
											// get the value as a string
											String value = rs.getString(i + 1);
											// check for null
											if (value == null) {
												// put null
												jsonRow.put(value);
											} else {
												// get the column type
												int columnType = rsmd.getColumnType(i + 1);
												// add the data to the row according to it's type
												switch (columnType) {
												case (Types.NUMERIC) :
													jsonRow.put(rs.getDouble(i + 1));
												break;
												case (Types.INTEGER) :
													jsonRow.put(rs.getInt(i + 1));
												break;
												case (Types.BIGINT) :
													jsonRow.put(rs.getLong(i + 1));
												break;
												case (Types.FLOAT) :
													jsonRow.put(rs.getFloat(i + 1));
												break;
												case (Types.DOUBLE) : case (Types.DECIMAL) :
													jsonRow.put(rs.getDouble(i + 1));
												break;
												case (Types.DATE) :
													Date date = rs.getDate(i + 1);
													if (date == null) {
														jsonRow.put(date);
													} else {
														if (localDateFormatter == null) localDateFormatter = rapidRequest.getRapidServlet().getLocalDateFormatter();
														jsonRow.put(localDateFormatter.format(date));
													}
												break;
												case (Types.TIMESTAMP) :
													Timestamp timeStamp = rs.getTimestamp(i + 1);
													if (timeStamp == null) {
														jsonRow.put(timeStamp);
													} else {
														// check for 0 millseconds past midnight - a truncated date time (time zone offset is in minutes, multiplied by the number of millis in a minute modulus with number of millis in a day)
														if ((timeStamp.getTime() - timeStamp.getTimezoneOffset() * 60000) % 86400000L == 0) {
															// if so show just date
															if (localDateFormatter == null) localDateFormatter = rapidRequest.getRapidServlet().getLocalDateFormatter();
															jsonRow.put(localDateFormatter.format(timeStamp));
														} else {
															// show date and time
															if (localDateTimeFormatter == null) localDateTimeFormatter = rapidRequest.getRapidServlet().getLocalDateTimeFormatter();
															jsonRow.put(localDateTimeFormatter.format(timeStamp));
														}
													}
												break;
												default :
													jsonRow.put(value);
												}
											}
										}
										// add the row to the rows collection
										jsonRows.put(jsonRow);
										// remember we now have our fields
										gotFields = true;

									}
									// close the record set
									rs.close();

									// look for any more results
									gotResults = st.getMoreResults();

									// if we got some
									if (gotResults) {
										// move result set on
										rs = st.getResultSet();
										// clear fields collection
										jsonFields = new JSONArray();
										// clear rows collection can start initialised
										jsonRows = new JSONArray();
									}

								} // got results loop

							} // check rs

						} // parameters list loop - not sure whether this ever called

					} else if (sqlCheck.startsWith("call") || sqlCheck.startsWith("{call")) {

						// loop the parameterList getting a result set for each parameters (input row)
						for (Parameters parameters : parametersList) {

							// initialise the row
							JSONArray jsonRow = new JSONArray();

							// get the output parameters
							Parameters outputParameters = df.executeCallableStatement(rapidRequest, sql, parameters);

							// loop the output parameters
							for (int i = 0; i < outputParameters.size(); i ++) {
								// if the top row add a field for this, out1, out2, etc
								if (jsonRows.length() == 0) jsonFields.put("out" + (i + 1));
								// get the string value!
								String value = outputParameters.get(i).getString();
								// add it to the jsonRow
								jsonRow.put(value);
							}

							// add the row to the result
							jsonRows.put(jsonRow);

						}

					} else {

						// assume rows affected is 0
						int rows = 0;

						// sql check
						if (sql.length() > 0) {

							// perform update for all incoming parameters (one parameters collection for each row)
							for (Parameters parameters : parametersList) {
								rows += df.getPreparedUpdate(rapidRequest, sql, parameters);
							}

							// add a psuedo field
							jsonFields.put("rows");

							// create a row array
							JSONArray jsonRow = new JSONArray();
							// add the rows updated
							jsonRow.put(rows);
							// add the row we just made
							jsonRows.put(jsonRow);

						}

					}

					// add the fields to the data object
					jsonData.put("fields", jsonFields);
					// add the rows to the data object
					jsonData.put("rows", jsonRows);

					// check for any child database actions
					if (_childDatabaseActions != null) {
						// if there really are some
						if (_childDatabaseActions.size() > 0) {


							// a list of mergeChildrenFields
							List<String> childFields = new ArrayList<>();
							// look for any mergeChildrenFields
							String childDataFields = getProperty("childDataFields");
							// if we got some
							if (childDataFields != null) {
								// split them
								String[] childDataFieldsParts = childDataFields.split(",");
								// loop them
								for (String childField : childDataFieldsParts) {
									// trim
									String childFieldTrim = childField.trim();
									// add if we got something (avoid the blank)
									childFields.add(childFieldTrim);
								}
							}

							// get any child data
							JSONArray jsonChildQueries = jsonAction.optJSONArray("childQueries");
							// if there was some
							if (jsonChildQueries != null) {
								// loop
								for (int i = 0; i < jsonChildQueries.length(); i++) {
									// fetch the data
									JSONObject jsonChildAction = jsonChildQueries.getJSONObject(i);
									// read the index (the position of the child this related to
									int index = jsonChildAction.getInt("index");
									// get the relevant child action
									Database childDatabaseAction = _childDatabaseActions.get(index);
									// get the resultant child data
									JSONObject jsonChildData = childDatabaseAction.doQuery(rapidRequest, jsonChildAction, application, df);

									// prepare the merge child field name
									String childFieldName = "childAction" + (i + 1);
									// if we were given a child field name at this position, use that instead
									if (childFields.size() > i) childFieldName = childFields.get(i);
									// add a field for the results of this child action
									jsonFields.put(childFieldName);

									// if we are merging child data, which was the default before 2.4.4.1
									if (_mergeChildren) {

										// a map for indexes of matching fields between our parent and child
										Map<Integer,Integer> fieldsMap = new HashMap<>();
										// the child fields
										JSONArray jsonChildFields = jsonChildData.getJSONArray("fields");
										if (jsonChildFields != null) {
											// loop the parent fields
											for (int j = 0; j < jsonFields.length(); j++) {
												// loop the child fields
												for (int k = 0; k < jsonChildFields.length(); k++) {
													// get parent field
													String field = jsonFields.getString(j);
													// get child field
													String childField = jsonChildFields.getString(k);
													// if both not null
													if (field != null && childField != null) {
														// check for match
														if (field.toLowerCase().equals(childField.toLowerCase())) fieldsMap.put(j, k);
													}
												}
											}
										}

										// if matching fields exists and not all columns are matched (stops simple queries like for drop down lookups merging)
										if (fieldsMap.size() > 0 && fieldsMap.size() != jsonFields.length() - i - 1) {
											// an object with a null value for when there is no match
											Object nullObject = null;
											// get the child rows
											JSONArray jsonChildRows = jsonChildData.getJSONArray("rows");
											// if we had some
											if (jsonChildRows != null) {
												// loop the parent rows
												for (int j = 0; j < jsonRows.length(); j++) {
													// get the parent row
													JSONArray jsonRow = jsonRows.getJSONArray(j);
													// make a new rows collection for the child subset
													JSONArray jsonChildRowsSubset = new JSONArray();
													// loop the child rows
													for (int k =0; k < jsonChildRows.length(); k++) {
														// get the child row
														JSONArray jsonChildRow = jsonChildRows.getJSONArray(k);
														// assume no matches
														int matches = 0;
														// loop the fields map
														for (Integer l: fieldsMap.keySet()) {
															// parent value
															Object parentValue = null;
															// get the value if there are enough
															if (jsonRow.length() > l) parentValue = jsonRow.get(l);
															// child value
															Object childValue = null;
															// get child value if present
															if (jsonChildRow.length() > l) childValue= jsonChildRow.opt(fieldsMap.get(l));
															// non null check
															if (parentValue != null && childValue != null) {
																// a string we will concert the child value to
																String parentString = null;
																// check the parent value type
																if (parentValue.getClass() == String.class) {
																	parentString = (String) parentValue;
																} else if (parentValue.getClass() == Integer.class) {
																	parentString = Integer.toString((Integer) parentValue);
																} else if (parentValue.getClass() == Long.class) {
																	parentString = Long.toString((Long) parentValue);
																} else if (parentValue.getClass() == Double.class) {
																	parentString = Double.toString((Double) parentValue);
																} else if (parentValue.getClass() == Boolean.class) {
																	parentString = Boolean.toString((Boolean) parentValue);
																}
																// a string we will convert the child value to
																String childString = null;
																// check the parent value type
																if (childValue.getClass() == String.class) {
																	childString = (String) childValue;
																} else if (childValue.getClass() == Integer.class) {
																	childString = Integer.toString((Integer) childValue);
																} else if (childValue.getClass() == Long.class) {
																	childString = Long.toString((Long) childValue);
																} else if (childValue.getClass() == Double.class) {
																	childString = Double.toString((Double) childValue);
																} else if (childValue.getClass() == Boolean.class) {
																	childString = Boolean.toString((Boolean) childValue);
																}
																// non null check
																if (parentString != null && childString != null) {
																	// do the match!
																	if (parentString.equals(childString)) matches++;
																}
															} // values non null
														} // field map loop
														// if we got some matches for all the fields add this row to the subset
														if (matches == fieldsMap.size()) jsonChildRowsSubset.put(jsonChildRow);
													} // child row loop
													// if our child subset has rows in it
													if (jsonChildRowsSubset.length() > 0) {
														// create a new childSubset object
														JSONObject jsonChildDataSubset = new JSONObject();
														// add the fields
														jsonChildDataSubset.put("fields", jsonChildFields);
														// add the subset of rows
														jsonChildDataSubset.put("rows", jsonChildRowsSubset);
														// add the child database action data subset
														jsonRow.put(jsonChildDataSubset);
													} else {
														// add an empty cell
														jsonRow.put(nullObject);
													}
												} // parent row loop
											} // jsonChildRows null check

										} else {

											// add a top row if we need one
											if (jsonRows.length() == 0) jsonRows.put(new JSONArray());
											// get the top row - only this one is used when the child data is retrieved
											JSONArray jsonRow = jsonRows.getJSONArray(0);
											// add the child database action data
											jsonRow.put(jsonChildData);

										} // matching fields check

									} else {

										// add a top row if we need one
										if (jsonRows.length() == 0) jsonRows.put(new JSONArray());
										// get the top row - only this one is used when the child data is retrieved
										JSONArray jsonRow = jsonRows.getJSONArray(0);
										// add the child database action data
										jsonRow.put(jsonChildData);

									} // child merge check

								} // jsonChildQueries loop
							} // jsonChildQueries null check
						} // _childDatabaseActions size > 0
					} // _childDatabaseActions not null

					// cache if in use
					if (actionCache != null) actionCache.put(application.getId(), getId(), parametersList.toString(), jsonData);

				} catch (Exception ex) {

					// log the error
					_logger.error(ex);

					// close the data factory and silently fail
					try { df.close(); } catch (Exception ex2) {}

					// only throw if no action cache
					if (actionCache == null) {
						throw ex;
					} else {
						_logger.debug("Error not shown to user due to cache : " + ex.getMessage());
					}

				} // jsonData not null

			} // jsonData == null

		} // got sql

		return jsonData;

	}

	@Override
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonAction) throws Exception {

		// This code could be optimised to only return required data, according to the outputs collection
		_logger.trace("Database action : " + jsonAction);

		// fetch the application
		Application application = rapidRequest.getApplication();

		// fetch the page
		Page page = rapidRequest.getPage();

		// fetch in the sequence
		int sequence = jsonAction.optInt("sequence", 1);

		// place holder for the object we're going to return
		JSONObject jsonData = null;

		// only if there is a query object, application, and page, and connection
		if (_query != null && application != null && page != null && application.getDatabaseConnections() != null && application.getDatabaseConnections().size() > _query.getDatabaseConnectionIndex()) {

			// get the relevant connection
			DatabaseConnection databaseConnection = application.getDatabaseConnections().get(_query.getDatabaseConnectionIndex());

			// get the connection adapter
			ConnectionAdapter ca = databaseConnection.getConnectionAdapter(rapidRequest.getRapidServlet().getServletContext(), application);

			// placeholder for data factory
			DataFactory df = null;

			// if this is sqlite
			if (databaseConnection.getConnectionString().toLowerCase().contains("sqlite")) {
				// instantiate a SQLite data factory with autocommit = false;
				df = new SQLiteDataFactory(ca, false);
			} else {
				// instantiate a data factory with autocommit = false;
				df = new DataFactory(ca, false);
			}

			// use the reusable do query function (so child database actions can use it as well)
			jsonData = doQuery(rapidRequest, jsonAction, application, df);

			// commit the data factory transaction
			df.commit();
			// close the data factory
			df.close();

		} // got query, app, and page

		// if it's null instantiate one
		if (jsonData == null) jsonData = new JSONObject();

		// add it back to the data object we're returning
		jsonData.put("sequence", sequence);

		return jsonData;

	}

	@Override
	public boolean isWebService() {
		return true;
	}

	// returns a list of all parameters for the sql by finding any ?'s followed by numbers/names and creating a longer parameter list populated with those mapped by index to the inputs - also used by designer sql check where the input names come from json
	public static List<Integer> getParameterMap(String sql, List<String> inputNames, Application application, ServletContext context) throws SQLException {

		// the parameter map we are making - it'll be size of the mapped parameters but hold the index back to the original ones
		List<Integer> parameterMap = new ArrayList<>();

		// use a copy of the sql to find the parameter mappings as we remove quotes and other things that break it for jdbc
		String sqlToMap = "";
		// split on comment open or closers
		String[] stringParts = sql.split("\\/\\*|\\*\\/");
		// loop 2 at a time so skipping the comments (between the closers)
		for (int partIndex = 0; partIndex < stringParts.length; partIndex += 2) {
			// append the non-comment parts
			sqlToMap += stringParts[partIndex];
		}

		// split the sql to map from above on quotes
		stringParts = sqlToMap.split("'");
		// reset the sql to map
		sqlToMap = "";
		// loop 2 at a time so skipping the quoted parts (between the quotes)
		for (int partIndex = 0; partIndex < stringParts.length; partIndex += 2) {
			// append the non-quoted part
			sqlToMap += stringParts[partIndex];
		}

		// we first use this to find parameters with names and convert them into numbers
		Map<String, Integer> parameterIndexesByControlName = new HashMap<>();

		// if there were any ?'s followed by names in the sql
		if (Pattern.compile(_namedParameterSlotRegex).matcher(sqlToMap).find()) {
			// loop the input to see which are in the sql and populate the map
			for (int inputIndex = 0; inputIndex < inputNames.size(); inputIndex++) {

				// the id of the control for this input
				String id = inputNames.get(inputIndex);

				// find any parts by splitting
				String[] parts = id.split("\\.");

				// if this is a system value
				if ("System".equals(parts[0])) {
					// put the name in quotes
					String name = "\"" + id + "\"";
					// add as name to the map
					parameterIndexesByControlName.put(name, inputIndex);
				} else {
					// get the control id
					String controlId = parts[0];
					// find the control
					Control control = application.getControl(context, controlId);
					// if we found a control
					if (control != null) {
						// get is name
						String name = control.getName();
						// update the id by cleaning its name
						id = id.replaceAll(".+\\.", name + ".");
						if (parts.length > 1) name += "." + parts[1].toLowerCase().replace(" ", "");
						for (int idIndex = 2; idIndex < parts.length; idIndex++) name += "." + parts[idIndex];
						// quote the name and put in the map
						parameterIndexesByControlName.put("\"" + name + "\"", inputIndex);
						// if there are no spaces put in map without quotes too
						if (!name.contains(" ")) parameterIndexesByControlName.put(name, inputIndex);

					}
				}
			}
		}

		// named variables should have been replaced so now use the number finding pattern
		Matcher matcher = Pattern.compile(_parameterSlotRegex).matcher(sqlToMap);

		// the parameters we want to map out to the new ones, we use a set to remove the ones we've done until they all are
		Set<Integer> parametersNumbers = new HashSet<>();
		// loop the original inputs to populate the set
		for (int number = 1; number <= inputNames.size(); number++) {
			parametersNumbers.add(number);
		}

		// a running count of ?'s without a number/name to tell if got them all
		int unspecifiedSlots = 0;

		// loop the ?'s and their number/name
		while (matcher.find()) {

			// get what the regex found: the ? followed by either numbers or letters
			String slot = matcher.group();

			// replace any following commas or closing brackets which would have been included by the regex when we were looking for names
			slot = slot.replace(",", "").replace(")", "");

			int parameterIndex = unspecifiedSlots;
			// if there is only a ? this slot is unspecified
			if (slot.length() == 1) {
				unspecifiedSlots++;
			} else {
				// get the number/name after the ?
				String specifier = slot.substring(1);

				// check if all numbers
				if (specifier.matches("\\d+")) {
					// use the number after the ? as the index to find the parameter
					parameterIndex = Integer.parseInt(specifier) - 1;
				} else {
					//
					String[] parts = specifier.split("\\.");
					String name = parts[0];
					if (parts.length > 1) name += "." + parts[1].toLowerCase().replace(" ", "");
					for (int idIndex = 2; idIndex < parts.length; idIndex++) name += "." + parts[idIndex];
					// use the letters after the ? as the key to find the parameter
					parameterIndex = parameterIndexesByControlName.get(name);
				}
			}

			// if we have an input at this index in the original inputs
			if (parameterIndex < inputNames.size()) {
				// add this parameter to our list of mapped indexes
				parameterMap.add(parameterIndex);
				// remove it from the set to track that we've done it
				parametersNumbers.remove(parameterIndex + 1);
			} else {
				//throw new SQLException("Parameter " + (parameterIndex + 1) + " not provided in inputs list");
			}

		} // slot loop

		// if there are any parameters left that weren't used by the query
		if (parametersNumbers.size() > 0) {
			// except for call queries where the un-used parameters are assumed to be outputs
			if (sql.trim().toLowerCase().replace(" ", "").startsWith("{call")) {
				// get the position of the un-used parameter
				int firstUnusedInputNumber = parametersNumbers.iterator().next();
				// tell the designer
				throw new SQLException("Input " + firstUnusedInputNumber + " not used");
			}
		}

		// we're done!
		return parameterMap;

	}

	// returns a set of parameters the same size as the map, populated using the indexes in the map on the original parameters - also used by designer sql check
	public static Parameters mapParameters(List<Integer> parameterMap, Parameters parameters) {

		// only if we have what we need and map is the same size or bigger
		if (parameterMap != null && parameters != null && parameterMap.size() >= parameters.size()) {

			// the longer list of parameters we'll be making using the numbers or names after the ?'s to map to the original inputs/parameters
			Parameters mappedParameters = new Parameters();

			// loop the map
			for (int parameterIndex : parameterMap) {
				// add the parameter at that position
				mappedParameters.add(parameters.get(parameterIndex));
			}

			// use the mapped parameters
			return mappedParameters;

		} else {

			// just return the parameters
			return parameters;

		}

	}

	// returns sql with any numbers after the ? removed (used after the name number replacement above)
	public static String unspecifySqlSlots(String sql) {
		// add an extra space to the end - something to do with ensuring there are an even number of parts when we split on the ' below
		sql = sql + " ";
		// split on the single quote - we only want to replace parameters outside of them
		String[] stringParts = sql.split("'");
		// replace the first part of the sql which goes up to the first single quote
		sql = stringParts[0].replaceAll(_unspecifySlotsRegex, "\\?");
		// loop the remaining parts from index 1 onwards
		for (int partIndex = 1; partIndex < stringParts.length; partIndex++) {
			// if this is an even numbered part
			if (partIndex % 2 == 0) {
				// its outside of a quoted string so replace the ? numbers
				sql += "'" + stringParts[partIndex].replaceAll(_unspecifySlotsRegex, "\\?");
			} else {
				// its within quotes so add back as it was
				sql += "'" + stringParts[partIndex];
			}
		}
		return sql;
	}

}
