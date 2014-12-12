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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Parameter;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DataFactory.Parameters;
import com.rapid.server.ActionCache;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.server.SOA;


public class Database extends Action {
	
	// details of the query (inputs, sql, outputs)
	public static class Query {
		
		private ArrayList<Parameter> _inputs, _outputs;
		private String _sql;
		private int _databaseConnectionIndex;
		
		public ArrayList<Parameter> getInputs() { return _inputs; }
		public void setInputs(ArrayList<Parameter> inputs) { _inputs = inputs; }
		
		public String getSQL() { return _sql; }
		public void setSQL(String sql) { _sql = sql; }
		
		public int getDatabaseConnectionIndex() { return _databaseConnectionIndex; }
		public void setDatabaseConnectionIndex(int databaseConnectionIndex) { _databaseConnectionIndex = databaseConnectionIndex; }
		
		public ArrayList<Parameter> getOutputs() { return _outputs; }
		public void setOutputs(ArrayList<Parameter> outputs) { _outputs = outputs; }
		
		public Query() {};
		public Query(ArrayList<Parameter> inputs, String sql, int databaseConnectionIndex, ArrayList<Parameter> outputs) {
			_inputs = inputs;
			_sql = sql;
			_databaseConnectionIndex = databaseConnectionIndex;
			_outputs = outputs;
		}
				
	}
	
	// static variables
	private static Logger _logger = Logger.getLogger(Database.class);
	
	// instance variables
	
	private Query _query;
	private boolean _showLoading;
	private List<Database> _childDatabaseActions;
	private List<Action> _successActions, _errorActions, _childActions;

	// properties
	
	public Query getQuery() { return _query; }
	public void setQuery(Query query) { _query = query; }
	
	public boolean getShowLoading() { return _showLoading; }
	public void setShowLoading(boolean showLoading) { _showLoading = showLoading; }
	
	public List<Database> getChildDatabaseActions() { return _childDatabaseActions; }
	public void setChildDatabaseActions(List<Database> childDatabaseActions) { _childDatabaseActions = childDatabaseActions; };
	
	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }
	
	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }
		
	// constructors
	
	// used by jaxb
	public Database() { super(); }
	// used by designer
	public Database(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties 
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for query
			if (!"query".equals(key) && !"showLoading".equals(key) && !"childDatabaseActions".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key)  && !"childActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		} 
		
		// try and build the query object
		JSONObject jsonQuery = jsonAction.optJSONObject("query");
		
		// check we got one
		if (jsonQuery != null) {
			// get the parameters						
			ArrayList<Parameter> inputs = getParameters(jsonQuery.optJSONArray("inputs"));
			String sql = jsonQuery.optString("SQL");
			int databaseConnectionIndex = jsonQuery.optInt("databaseConnectionIndex");
			ArrayList<Parameter> outputs = getParameters(jsonQuery.optJSONArray("outputs"));
			// make the object
			_query = new Query(inputs, sql, databaseConnectionIndex, outputs);
		}
		
		// look for showLoading
		_showLoading = jsonAction.optBoolean("showLoading");
		
		// grab any successActions
		JSONArray jsonChildDatabaseActions = jsonAction.optJSONArray("childDatabaseActions");
		// if we had some 
		if (jsonChildDatabaseActions != null) {
			// instantiate collection
			_childDatabaseActions = new ArrayList<Database>();
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
			parameters = new ArrayList<Parameter>();
			// loop
			for (int i = 0; i < jsonParameters.length(); i++) {
				// instaniate member
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
							js += "  $('#" + control.getId() + "').showLoading();\n";						
						} else {
							js += "  $('#" + control.getId() + "').hideLoading();\n";
						}
					}
				}
			}
			
		}
		return js;
	}
	
	// private function to get inputs into the query object, reused by child database actions
	private String getInputsJavaScript(Application application, Page page, ArrayList<Parameter> inputs, String jsInputCollection) {
		
		// assume it'll be an empty string
		String js = "";
		
		// if we were given some
		if (inputs != null) {
			// loop them
			for (Parameter parameter : inputs) {
				// get this item id
				String itemId = parameter.getItemId();
				// if there was an id
				if (itemId != null) {
					// get any parameter field
					String field = parameter.getField();
					// check if there was one
					if (field == null) {
						// no field
						js += jsInputCollection + ".push({id:'" + itemId + "',value:" + Control.getDataJavaScript(application, page, itemId, null) + "});\n";
					} else {
						// got field so let in appear in the inputs for matching later
						js += jsInputCollection + ".push({id:'" + itemId + "',value:" + Control.getDataJavaScript(application, page, itemId, field) + ",field:'" + field + "'});\n";
					}
				}
			}
		} // got inputs
		
		// return
		return js;
	}
	
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		
		String js = "";
		
		if (_query != null) {
									
			// get the sequence for this action requests so long-running early ones don't overwrite fast later ones (defined in databaseaction.xml)
			js += "  var sequence = getDatabaseActionSequence('" + getId() + "');\n";
						
			// drop in the query variable used to collect the inputs, and hold the sequence
			js += "  var query = { inputs:[], sequence:sequence };\n";
			
			// get the inputs
			js += "  " + getInputsJavaScript(application, page, _query.getInputs(), "query.inputs").trim().replace("\n", "\n  ") + "\n";
			
			// look for any _childDatabaseActions
			if (_childDatabaseActions != null) {
				// add a collection into the parent
				js += "  query.childQueries = [];\n";
				// count them
				int i = 1;
				// loop them
				for (Database childDatabaseAction : _childDatabaseActions) {
					// create object
					js += "  var childQuery" + i + " = {index:" + (i - 1) + ",inputs:[]};\n";					
					// add inputs
					js += "  " + getInputsJavaScript(application, page, childDatabaseAction.getQuery().getInputs(), "childQuery" + i + ".inputs").trim().replace("\n", "\n  ") + "\n";
					// add to query
					js += "  query.childQueries.push(childQuery" + i + ");\n";			
					// increment the counter
					i ++;
				}
			}
						
			// control can be null when the action is called from the page load
			String controlParam = "";
			if (control != null) controlParam = "&c=" + control.getId();
						
			// get the outputs
			ArrayList<Parameter> outputs = _query.getOutputs();
			
			// get the js to hide the loading (if applicable)
			if (_showLoading) js += getLoadingJS(page, outputs, true);
						
			// stringify the query
			js += "  query = JSON.stringify(query);\n";
			
			// open the ajax call
			js += "  $.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
			js += "    data: query,\n";
			js += "    error: function(server, status, message) {\n";
			
			// hide the loading javascript (if applicable)
			if (_showLoading) js += "      " + getLoadingJS(page, outputs, false);
							
			// this avoids doing the errors if the page is unloading or the back button was pressed
			js += "      if (server.readyState > 0) {\n";
			
			// retain if error actions
			boolean errorActions = false;
			
			// add any error actions
			if (_errorActions != null) {
				for (Action action : _errorActions) {
					errorActions = true;
					js += "         " + action.getJavaScript(application, page, control).trim().replace("\n", "\n         ") + "\n";
				}
			}
			// add manual if not in collection
			if (!errorActions) {
				js += "        alert('Error with database action : ' + server.responseText||message);\n";
			}
			
			// close unloading check
			js += "      }\n";
			
			// close error actions
			js += "    },\n";
			
			// open success function
			js += "    success: function(data) {\n";	
			
			// hide the loading javascript (if applicable)
			if (_showLoading) js += "    " + getLoadingJS(page, outputs, false);
			
			// open if data check
			js += "      if (data) {\n";
						
			// check there are outputs
			if (outputs != null) {
				// the outputs array we're going to make
				String jsOutputs = "";				
				// loop the output parameters
				for (int i = 0; i < outputs.size(); i++) {
					// get the parameter
					Parameter output = outputs.get(i);
					// get the control the data is going into
					Control outputControl = page.getControl(output.getItemId());
					// try the application if still null
					if (outputControl == null) outputControl = application.getControl(output.getItemId());
					// check we got one
					if (outputControl != null) {
						// get any mappings we may have
						String details = outputControl.getDetailsJavaScript(application, page);
						// set to empty string or clean up
						if (details == null) {
							details = "";
						} else {
							details = ", details: " + outputControl.getId() + "details";
						}
						// append the javascript outputs
						jsOutputs += "{id: '" + outputControl.getId() + "', type: '" + outputControl.getType() + "', field: '" + output.getField() + "'" + details + "}";
						// add a comma if not the last
						if (i < outputs.size() - 1) jsOutputs += ","; 
					}					
				}			
				js += "       var outputs = [" + jsOutputs + "];\n";
				// send them them and the data to the database action				
				js += "       Action_database('" + getId() + "', data, outputs);\n";				
			}
			
			// add any sucess actions
			if (_successActions != null) {
				for (Action action : _successActions) {
					js += "       " + action.getJavaScript(application, page, control).trim().replace("\n", "\n       ") + "\n";
				}
			}
			
			// close if data check
			js += "      }\n";						
			// close success function
			js += "    }\n";
			
			// close ajax call
			js += "  });";
		}
				
		// return what we built			
		return js;
	}
	
	
	public JSONObject doQuery(RapidRequest rapidRequest, JSONObject jsonAction, Application application, DataFactory df) throws Exception {
		
		// place holder for the object we're going to return
		JSONObject jsonData = null;
		
		// retrieve the sql
		String sql = _query.getSQL();
		
		// only if there is some sql is it worth going further
		if (sql != null) {
			
			// get any json inputs
			JSONArray jsonInputs = jsonAction.optJSONArray("inputs");
			
			// initialise the parameters list
			Parameters parameters = new Parameters();
			
			// populate the parameters from the inputs collection (we do this first as we use them as the cache key due to getting values from the session)
			if (_query.getInputs() != null) {
				
				// loop the query inputs
				for (Parameter input : _query.getInputs()) {
					// get the input id
					String id = input.getItemId();
					// get the input field
					String field = input.getField();
					// retain the value
					String value = null;
					// if it looks like a control, or a system value (bit of extra safety checking)
					if ("P".equals(id.substring(0,1)) && id.indexOf("_C") > 0 || id.indexOf("System.") == 0) {
						// loop the json inputs looking for the value
						if (jsonInputs != null) {
							for (int i = 0; i < jsonInputs.length(); i++) {
								// get this jsonInput
								JSONObject jsonInput = jsonInputs.getJSONObject(i);
								// check we got one 
								if (jsonInput != null) {
									// if the id we want matches this one 
									if (id.equals(jsonInput.optString("id"))) {
										// get the input field
										String jsonField = jsonInput.optString("field");
										// field check
										if ((jsonField == null && "".equals(field)) || jsonField.equals(field)) {
											// set the value
											value = jsonInput.getString("value");
											// no need to keep looking
											break;
										}
									}
								}																	
							}
						}
					}
					// if still null try the session
					if (value == null) value = (String) rapidRequest.getSessionAttribute(input.getItemId());
					// add the parameter
					parameters.add(value);
				}
			}
			
			// placeholder for the action cache
			ActionCache actionCache = rapidRequest.getRapidServlet().getActionCache();
				
			// if an action cache was found
			if (actionCache != null) {
				
				// log that we found action cache
				_logger.debug("Database action cache found");
				
				// attempt to fetch data from the cache
				jsonData = actionCache.get(application.getId(), getId(), parameters.toString());
				
			}
			
			// if there isn't a cache or no data was retrieved
			if (jsonData == null) {
				
				try {
					
					// instantiate jsonData
					jsonData = new JSONObject();
																		
					// trim the sql
					sql = sql.trim();
					
					// check the verb
					if (sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with")) {
						
						// set readonly to true
						df.setReadOnly(true);
						
						// get the resultset!
						ResultSet rs = df.getPreparedResultSet(rapidRequest, sql, parameters);
						
						ResultSetMetaData rsmd = rs.getMetaData();
						
						// fields collection
						JSONArray jsonFields = new JSONArray();
						// got fields indicator
						boolean gotFields = false;
						// rows collection can start initialised
						JSONArray jsonRows = new JSONArray();
						
						// loop the result set
						while (rs.next()) {
							
							// initialise the row
							JSONArray jsonRow = new JSONArray();
							
							// loop the columns
							for (int i = 0; i < rsmd.getColumnCount(); i++) {
								// add the field name to the fields collection if not done yet
								if (!gotFields) jsonFields.put(rsmd.getColumnName(i + 1));
								// add the data to the row according to it's type	
								switch (rsmd.getColumnType(i + 1)) {
								case (Types.INTEGER) : 
									jsonRow.put(rs.getInt(i + 1));
								break;
								case (Types.BIGINT) :
									jsonRow.put(rs.getLong(i + 1));
								break;
								case (Types.FLOAT) : 
									jsonRow.put(rs.getFloat(i + 1));
								break;
								case (Types.DOUBLE) : 
									jsonRow.put(rs.getDouble(i + 1));
								break;
								default :
									jsonRow.put(rs.getString(i + 1));
								}						
							}
							// add the row to the rows collection
							jsonRows.put(jsonRow);
							// remember we now have our fields
							gotFields = true;
							
						}
						
						// add the fields to the data object
						jsonData.put("fields", jsonFields);
						// add the rows to the data object
						jsonData.put("rows", jsonRows);
						
						// close the record set
						rs.close();
						
						// cache if in use
						if (actionCache != null) actionCache.put(application.getId(), getId(), parameters.toString(), jsonData);
						
					} else {
						
						// perform an update
						int rows = df.getPreparedUpdate(rapidRequest, sql, parameters);
						
						// create a fields array
						JSONArray jsonFields = new JSONArray();
						// add a psuedo field 
						jsonFields.put("rows");
						
						// create a row array
						JSONArray jsonRow = new JSONArray();
						// add the rows updated
						jsonRow.put(rows);
						
						// create a rows array
						JSONArray jsonRows = new JSONArray();
						// add the row we just made
						jsonRows.put(jsonRow);
						
						// add the fields to the data object
						jsonData.put("fields", jsonFields);
						// add the rows to the data object
						jsonData.put("rows", jsonRows);
											
					}
					
					// check for any child database actions
					if (_childDatabaseActions != null) {						
						// create an array object for all the child data
						JSONArray jsonChildDataCollection = new JSONArray();
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
								// add it to the collection
								jsonChildDataCollection.put(jsonChildData);								
							}
							
						}
						// add a field to the result for the child actions
						jsonData.getJSONArray("fields").put("childData");
						// add the collection to the result
						jsonData.getJSONArray("rows").put(jsonChildDataCollection);
												
					}
																		
				} catch (Exception ex) {
					
					// log the error
					_logger.error(ex);

					// only throw if no action cache
					if (actionCache == null) {
						throw ex;
					} else {
						_logger.debug("Error not shown to user due to cache : " + ex.getMessage());
					}
					
				}
														
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
		int sequence = jsonAction.optInt("sequence",1);
				
		// place holder for the object we're going to return
		JSONObject jsonData = null;
										
		// only if there is a query object, application, and page				
		if (_query != null && application != null && page != null) {
			
			// get the relevant connection
			DatabaseConnection databaseConnection = application.getDatabaseConnections().get(_query.getDatabaseConnectionIndex());
			
			// get the connection adapter
			ConnectionAdapter ca = databaseConnection.getConnectionAdapter(rapidRequest.getRapidServlet().getServletContext());			
							
			// instantiate a data factory with autocommit = false;
			DataFactory df = new DataFactory(ca, false);
			
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
	
}
