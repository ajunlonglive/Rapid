/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Datacopy extends Action {

	// public static class
	public static class DataCopy {

		// private instance variables
		private String _source;
		private String _sourceField;
		private String _destination;
		private String _destinationField;
		private String _type;

		// properties
		public String getSource() { return _source; }
		public void setSource(String source) { _source = source; }

		public String getSourceField() { return _sourceField; }
		public void setSourceField(String sourceField) { _sourceField = sourceField; }

		public String getDestination() { return _destination; }
		public void setDestination(String destination) { _destination = destination; }

		public String getDestinationField() { return _destinationField; }
		public void setDestinationField(String destinationField) { _destinationField = destinationField; }

		public String getType() { return _type; }
		public void setType(String type) { _type = type; }

		// constructors
		// used by jaxb
		public DataCopy() { super(); }
		// used by designer
		public DataCopy(JSONObject jsonCopy) throws Exception {
			_source = jsonCopy.optString("source", null);
			_sourceField = jsonCopy.optString("sourceField", null);
			_destination = jsonCopy.optString("destination", null);
			_destinationField = jsonCopy.optString("destinationField", null);
			_type = jsonCopy.optString("type", null);
		}

	}

	// private instance variables
	private List<DataCopy> _dataCopies;
	private boolean _mergeChildren;
	private boolean _upgradeFixInProgress = false;

	// properties
	public List<DataCopy> getDataCopies() { return _dataCopies; }
	public void setDataCopies(List<DataCopy> dataCopies) { _dataCopies = dataCopies; }

	public boolean getMergeChildren() { return _mergeChildren; }
	public void setMergeChildren(boolean mergeChildren) {
		// avoid overwriting _mergeChildren if JAXB has already called setProperties and set it false
		if (!_upgradeFixInProgress) {
			_mergeChildren = mergeChildren;
		}
		_upgradeFixInProgress = false;
	}

	// constructors - upgrade fixes are in the setProperties override near the bottom

	// used by jaxb
	public Datacopy() {
		// set the xml version, etc
		super();
		// default child type merge children to true for older applications - new ones will have it set to false by default by the designer - also getJavaScript for other upgrade fixes
		_mergeChildren = true;
	}
	// used by designer
	public Datacopy(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for dataCopies
			if (!"dataCopies".equals(key)) addProperty(key, jsonAction.get(key).toString());
			// if this is a child copy with mergeChildren we need to update our property variable too, to deal with legacy values
			if ("child".equals(_properties.get("copyType")) && "mergeChildren".equals(key)) _mergeChildren = jsonAction.optBoolean(key);
		}
		// look for a dataCopies array
		JSONArray jsonDataCopies = jsonAction.optJSONArray("dataCopies");
		// if we got one
		if (jsonDataCopies != null) {
			// instantiate our collection
			_dataCopies = new ArrayList<>();
			// loop it
			for (int i = 0; i < jsonDataCopies.length(); i++) {
				// add this one
				_dataCopies.add(new DataCopy(jsonDataCopies.getJSONObject(i)));
			}
		}
	}

	// determine if this datacopy get is combined in a row merge
	private boolean isCombineRowMergeGet(int i, int combineRowMergeStartIndex) {

		// promote combineRowMergeStartIndex is not set yet
		if (combineRowMergeStartIndex < 0) combineRowMergeStartIndex = i;

		// get the data copies we want to check, if they exist
		DataCopy dataCopy = _dataCopies.get(i);
		DataCopy dataCopyStart = (combineRowMergeStartIndex > -1 && combineRowMergeStartIndex < _dataCopies.size() ? _dataCopies.get(combineRowMergeStartIndex) : null);
		DataCopy dataCopyNext = (i < _dataCopies.size() - 1 ? _dataCopies.get(i + 1) : null);
		DataCopy dataCopyPrev = (i > 0 ? _dataCopies.get(i - 1) : null);

		// assume it's not
		boolean result = false;

		// check some basics
		if (dataCopy != null && dataCopyStart != null && dataCopy.getDestination() != null) {

			// if this is a row merge, or it's the first one that's a replace
			result = ("row".equals(dataCopy.getType()) || (i == combineRowMergeStartIndex &&"".equals(dataCopyStart.getType()))) && (
				// if the next one is for the same source and it's a row merge
				(dataCopyNext != null && dataCopyNext.getDestination() != null && dataCopy.getDestination().equals(dataCopyNext.getDestination()) && "row".equals(dataCopyNext.getType()))
				||
				// if the previous one is for the same source and it's a row merge or the start which can be a replace
				(dataCopyPrev != null && dataCopyPrev.getDestination() != null && dataCopy.getDestination().equals(dataCopyPrev.getDestination()) && ("row".equals(dataCopyPrev.getType()) || (i-1 == combineRowMergeStartIndex && "".equals(dataCopyPrev.getType()))))
			);

		}

		// we're done!
		return result;

	}

	// determine if this datacopy set is combined in a row merge
	private boolean isCombineRowMergeSet(int i, int combineRowMergeStartIndex) {

		// get this data copy
		DataCopy dataCopy = _dataCopies.get(i);
		// if there is a next one get it
		DataCopy dataCopyNext = (i < _dataCopies.size() - 1 ? _dataCopies.get(i + 1) : null);

		// if there is a next data copy and it's a row merge, and this is a row merge or replace if its the start,
		boolean result = (dataCopyNext != null && dataCopy.getDestination() != null && dataCopy.getDestination().equals(dataCopyNext.getDestination()) && "row".equals(dataCopyNext.getType()) && ("row".equals(dataCopy.getType()) || (i == combineRowMergeStartIndex && "".equals(dataCopy.getType()))));

		// we're done!
		return result;

	}

	// overrides

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) {

		// the javascript we're making
		String js = "";

		// get the Rapid servlet
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();

		// get any copy type
		String copyType = getProperty("copyType");

		// set to replace if null (for backwards compatibility)
		if (copyType == null) copyType = "replace";

		String changeEvents = "false";
		if (Boolean.parseBoolean(getProperty("changeEvents"))) {
			changeEvents = "true";
			if (Boolean.parseBoolean(getProperty("noValidation"))) {
				changeEvents = "{ noValidation: true }";
			}
		}

		// bulk copy is a special animal
		if ("bulk".equals(copyType)) {

			// check we have some data copies
			if (_dataCopies == null) {

				// data copies not found return a comment
				js = "// data copies not found for " + getId();

			} else {

				// compare against last getData call to avoid recalling
				String lastGetDataFunction = null;
				// assume we have not started to combine row merges
				int combineRowMergeStartIndex = -1;

				// loop them
				for (int i = 0; i < _dataCopies.size(); i++) {

					// get this data copy (we want to look ahead to combine row merges in the get data function)
					DataCopy dataCopy = _dataCopies.get(i);

					if (dataCopy.getSource() == null ) {

						// data copies not found return a comment
						js += "// data source not specified\n";

					} else {

						// get the destination id
						String destinationId = dataCopy.getDestination();
						String[] sourceParts = dataCopy.getSource().split("\\.");

						// check we got one
						if (destinationId.length() > 0) {

							String getDataFunction = null;

							// if this is a datetime value, and it has a type
							if ("Datetime".equals(sourceParts[0]) && sourceParts.length > 1) {
								// make the js!
								getDataFunction = makeDateGetter(sourceParts[1]);

							} else {

								// get the get data function
								getDataFunction = Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, dataCopy.getSource(), dataCopy.getSourceField());

							}

							// get the destination field
							String destinationField = dataCopy.getDestinationField();
							// clean up the field
							if (destinationField == null) destinationField = "";

							// add the getData if different from the last one
							if (!getDataFunction.equals(lastGetDataFunction)) {

								// if this is row merge and so is the next or previous one - merge into same data object
								if (isCombineRowMergeGet(i, combineRowMergeStartIndex)) {

									// start data object if
									if (combineRowMergeStartIndex < 0) {
										// retain the we have started
										combineRowMergeStartIndex = i;
										// start the data object
										js += "var data = {};\n";
									}

									// use the data function to add to the data object
									js += "data['" + destinationField + "'] = " + getDataFunction + ";\n";


								} else {

									// use the data function to make a data object
									js += "var data = " + getDataFunction + ";\n";

								}

							}

							// remember this one
							lastGetDataFunction = getDataFunction;

							if ("System.clipboard".equals(destinationId)) {

								// do the data copy
								js += "Action_datacopy(ev, data, [{id:'" + destinationId + "'}]);\n";

							} else {

								// assume control id is destination id
								String controlId = destinationId;
								// split if by escaped .
								String idParts[] = destinationId.split("\\.");
								// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
								if (idParts.length > 1) controlId = idParts[0];
								// first try and look for the control in the page
								Control destinationControl = page.getControl(controlId);
								// assume we found it
								boolean pageControl = true;

								// check we got a control
								if (destinationControl == null) {
									// now look for the control in the application
									destinationControl = application.getControl(rapidServlet.getServletContext(), controlId);
									// set page control to false
									pageControl = false;
								}

								// check we got one from either location
								if (destinationControl == null) {

									// data copies not found return a comment
									js += "// data destination not found for " + controlId + "\n";

								} else {

									// try and get the type
									String type = dataCopy.getType();
									// check it fot backwards compatibility
									if (type == null || "false".equals(type)) {
										// update to empty string
										type = "";
										// add back to object so it can be used to detect combined row merges
										dataCopy.setType(type);
									} else {
										// update to comma-prefixed, string escaped
										type = ", '" + type + "'";
									}

									// get any details we may have
									String details = destinationControl.getDetailsJavaScript(application, page);

									// if we have some details
									if (details != null) {
										// if this is a page control
										if (pageControl) {
											// the details will already be in the page so we can use the short form
											details = destinationControl.getId() + "details";
										}
									}

									// if this is row merge and so is the next one - skip the data copy
									if (!isCombineRowMergeSet(i, combineRowMergeStartIndex)) {

										// if we started combining row merges
										if (combineRowMergeStartIndex > -1) {

											// empty the destination field
											destinationField = "null";

											// if we started with a replace (the rest would be merge) set type to replace
											if ("".equals(_dataCopies.get(combineRowMergeStartIndex).getType())) type = "";

											// reset in case there are others
											combineRowMergeStartIndex = -1;

										} else {

											// wrap the destination field
											destinationField = "'" + destinationField + "'";

										} // this and next are row merge check

										// get the details into variable
										js += "var details = " + details + ";\n";
										// retain that this is bulk so we can turn on the noFieldMatch
										js += "details.copyType = 'bulk';\n";
										// do the data copy
										js += "Action_datacopy(ev, data, [{id:'" + destinationId + "', type:'" + destinationControl.getType() + "', field:" + destinationField + ", details:details}], " + changeEvents + type + ");\n";

									}

								} // destination control check

							} // System.clipboard check

						} // destination id check

					} // source check

				} // data copies loop

			} // data copies check

		} else {

			// get the data source
			String dataSourceId = getProperty("dataSource");

			// check there is a datasource
			if (dataSourceId == null) {

				// no data source return a comment
				js = "// no data source for action " + getId();

			} else {

				// get the data source field
				String dataSourceField = getProperty("dataSourceField");

				// split by escaped .
				String[] idParts = dataSourceId.split("\\.");

				// if this is a datetime value, and it has a type
				if ("Datetime".equals(idParts[0]) && idParts.length > 1) {
					// make the js!
					js = "var data = " + makeDateGetter(idParts[1]);

				} else {

					js = "var data = " + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, dataSourceId, dataSourceField);

				}

				// append the line ending for both the above
				js += ";\n";

				// we're going to work with the data destinations in a json array
				JSONArray jsonDataDestinations = null;

				// assume we have no need for an outputs array
				boolean outputsArray = false;

				// try and get the data destinations from the properties into a json array, silently fail if not
				try { jsonDataDestinations = new JSONArray(getProperty("dataDestinations")); }
				catch (Exception e) {}

				if (jsonDataDestinations == null) {

					// data source destinations not found return a comment
					js += "// data source destinations not found for " + getId() + "\n";

				} else {

					// prepare a string for the outputs array
					String jsOutputs = "";
					// loop the json data destination collection
					for (int i = 0; i < jsonDataDestinations.length(); i++) {

						// try and make an output for this destination
						try {

							// retrieve this data destination
							JSONObject jsonDataDesintation = jsonDataDestinations.getJSONObject(i);
							// get the control id
							String destinationId = jsonDataDesintation.getString("itemId");

							if ("System.clipboard".equals(destinationId)) {
								jsOutputs += "{id: '" + destinationId + "'},";
								// we will need an outputs array
								outputsArray = true;
								// no destination control to worry about
								continue;
							}

							// split by escaped .
							idParts = destinationId.split("\\.");
							// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
							if (idParts.length > 1) destinationId = idParts[0];

							// first try and look for the control in the page
							Control destinationControl = page.getControl(destinationId);
							// assume we found it
							boolean pageControl = true;
							// check we got a control
							if (destinationControl == null) {
								// now look for the control in the application
								destinationControl = application.getControl(rapidServlet.getServletContext(), destinationId);
								// set page control to false
								pageControl = false;
							}

							// check we got one from either location
							if (destinationControl != null) {

								// get the field
								String destinationField = jsonDataDesintation.optString("field");
								// clean up the field
								if (destinationField == null) destinationField = "";

								// get any details we may have
								String details = destinationControl.getDetailsJavaScript(application, page);

								// we will need an outputs array
								outputsArray = true;

								// set details to empty string or clean up
								if (details == null) {
									details = "";
								} else {
									// if this is a page control
									if (pageControl) {
										// the details will already be in the page so we can use the short form
										details = ", details:" + destinationControl.getId() + "details";
									} else {
										// write the full details
										details = ", details:" + details;
									}
								}

								// put any property back on the id
								if (idParts.length > 1) destinationId += "." + idParts[1];

								// add the properties we need as a js object it will go into the outputs array, simple, property, and system (clipboard) data sets are called from the JavaScript in the dataCopy.action.xml file
								jsOutputs += "{id: '" + destinationId + "', type: '" + destinationControl.getType() + "', field: '" + destinationField + "'" + details + "},";

							}

						} catch (JSONException ex) {

							// data source destinations not found return a comment
							js = "// error creating data output for " + getId() + " : " + ex.getMessage();

						}
					}

					// if there was an outputs array
					if (outputsArray) {

						// trim the last comma
						if (jsOutputs.length() > 0) jsOutputs = jsOutputs.substring(0, jsOutputs.length() - 1);

						// add to js as an array
						js += "var outputs = [" + jsOutputs + "];\n";
						// add the start of the call
						js += "Action_datacopy(ev, data, outputs, " + changeEvents;

						// add the copy type to the js
						js += ", '" + copyType + "'";

						// check the copy type
						if ("row".equals(copyType)) {

							// add the details with _mergeChildren
							js += ", null, null, {mergeChildren: " + _mergeChildren + "}";

						} else if ("child".equals(copyType)) {

							// no merge data object
							js += ", null";
							// look for a merge field
							String childField = getProperty("childField");
							// check if got we got one
							if (childField == null || childField.length() == 0) {
								// call it child if not
								js += ", 'child'";
							} else {
								// add it
								js += ", '" + childField + "'";
							}
							// add the details with _mergeChildren
							js += ", {mergeChildren: " + _mergeChildren + "}";

						} else if ("search".equals(copyType)) {

							// get the search data
							js += ", " + Control.getDataJavaScript(rapidServlet.getServletContext(), application, page, getProperty("searchSource"), getProperty("searchSourceField"));
							// look for a search field
							String searchField = getProperty("searchField");
							// add it if present
							if (searchField != null) js += ", '" + searchField + "'";
							// look for a maxRows field
							String maxRows = getProperty("maxRows");
							// add it if present
							if (maxRows != null) js += "," + maxRows;

						} else if ("trans".equals(copyType)) {

							// assume the key fields are null
							String keyFields = "null";

							// try and fetch the key fields
							try {
								JSONArray jsonKeyFields = new JSONArray(getProperty("keyFields"));
								keyFields = jsonKeyFields.toString();
							} catch (JSONException ex) {
								keyFields = "null /*" + ex.getMessage() + "*/";
							}

							// assume the ignore fields are null
							String ignoreFields = "null";

							// try and fetch the ignore fields, show message if issue
							try {
								JSONArray jsonIgnoreFields = new JSONArray(getProperty("ignoreFields"));
								ignoreFields = jsonIgnoreFields.toString();
							} catch (JSONException ex) {
								ignoreFields = "null /*" + ex.getMessage() + "*/";
							}

							// add the details
							js += ", null, null, {keyFields:" + keyFields + ", ignoreFields:" + ignoreFields + "}";

						} // copy type

						// close the copy data call
						js += ");\n";

					} // outputs array

				} // data destinations check

			} // data source check

		} // bulk copy check

		return js;

	}

	private String makeDateGetter(String type) {
		// make format with dateFormat and timeFormat properties
		String format = "";
		//generate the date/time at run time
		if("current date".equals(type)){
			// get its, format
			format = getProperty("dateFormat");

		} else if ("current time".equals(type)){
			// get its, format
			format = getProperty("timeFormat");

		} else { // if the date and time format is selected
			// get its, format
			format = getProperty("dateFormat") + " " + getProperty("timeFormat");
		}

		String formatter = "current date and time".startsWith(type) ? "formatDatetime" : "formatTime";
		// make the js!
		return formatter + "('" + format + "', new Date())";
	}

	// used for upgrade conversions when JAXB sets the properties
	@Override
	public void setProperties(HashMap<String,String> properties) {
		// do the super first
		super.setProperties(properties);

		// upgrade fix - turn off mergeChildren for row copy types
		if ("row".equals(_properties.get("copyType")) && "true".equals(_properties.get("mergeChildren")) && _properties.get("upgradeFix") == null) {
			// turn off merge children as its not what users expect
			_mergeChildren = false;
			// set the mergeChildren to false
			_properties.put("mergeChildren", "false");
			// set upgrade fix
			_properties.put("upgradeFix", "1");
			// override setMergeChildren because JAXB will call it with the original value after setProperties
			_upgradeFixInProgress = true;
		}

	}

}
