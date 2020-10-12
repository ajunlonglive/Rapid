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

package com.rapid.utils;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSON {

	// turns a JSONArray, either from a single member, or muliple if they exist - solves the issue when creating JSON from XML if there is only one member
	public static JSONArray getJSONArray(JSONObject jsonObject, String memberName) throws JSONException {

		// if we got one
		if (jsonObject != null) {
			// look for an array
			JSONArray jsonArray = jsonObject.optJSONArray(memberName);
			// null check
			if (jsonArray == null) {
				// look for a single object
				Object jsonMemberObject = jsonObject.opt(memberName);
				// if we got one
				if (jsonMemberObject != null) {
					// create an empty array
					jsonArray = new JSONArray();
					// add member
					jsonArray.put(jsonMemberObject);
				}
			}
			// return the array
			return jsonArray;
		}
		// no object
		return null;
	}

	// gets a JSONObject if an attribute if of a specified type
	public static String getStringWithAttributeValue(Object object, String attribute, String value) throws JSONException {

		// if we got one
		if (object != null) {
			// if the object is a json object
			if (object instanceof JSONObject) {
				// cast
				JSONObject jsonObject = (JSONObject) object;
				// look for the attribute
				String objectAttribute = jsonObject.optString(attribute, null);
				// check we got one
				if (objectAttribute != null) {
					// check it matches our value
					if (objectAttribute.equals(value)) {
						// extract and send string
						return jsonObject.optString("content", null);
					} else {
						// does not match specified attribute value, return null
						return null;
					}
				}
			}
			return object.toString();
		}
		// no object, or no attribute
		return null;
	}

	// turns a jsonArray into a String array
	public static String[] getStringArray(JSONArray jsonArray) throws JSONException {

		String[] strings = null;

		if (jsonArray != null) {

			strings = new String[jsonArray.length()];

			for (int i = 0; i < jsonArray.length(); i++) {

				strings[i] = jsonArray.getString(i);

			}

		}

		return strings;

	}
	
	public static class JSONData {

		private JSONArray _fields;
		private Map<String, Integer> _fieldIndexes = new HashMap<String, Integer>();
		private JSONArray _rows;
		
		public class Row {
			private JSONArray _row;
			
			Row(JSONArray row) throws Exception {
				_row = row;
			}
			
			public int length() {
				return _fields.length();
			}
			
			public Boolean getBoolean(String field) throws Exception {
				return _row.getBoolean(_fieldIndexes.get(field));
			}
			
			public int getInt(String field) throws Exception {
				return _row.getInt(_fieldIndexes.get(field));
			}
			
			public double getDouble(String field) throws Exception {
				return _row.getDouble(_fieldIndexes.get(field));
			}
			
			public String getString(String field) throws Exception {
				return _row.getString(_fieldIndexes.get(field));
			}
			
			public JSONObject getJSONObject(String field) throws Exception {
				return _row.getJSONObject(_fieldIndexes.get(field));
			}
			
			public JSONData getJSONData(String field) throws Exception {
				return new JSONData(getJSONObject(field));
			}
			
			public String getType(String field) throws Exception {
				return _row.get(_fieldIndexes.get(field)).getClass().getName();
			}
			
			public String toString() {
				return "{"
						+ "\n    fields: " + _fields
						+ "\n    rows: ["
						+ _row
						+ "\n    ]"
						+ "\n}";
			}
		}
		
		public JSONData(JSONArray jsonFields, JSONArray jsonRows) throws Exception {
			
			_fields = jsonFields;
			_rows = jsonRows;

			for (int fieldIndex = 0; fieldIndex < _fields.length(); fieldIndex++) {
				_fieldIndexes.put(_fields.getString(fieldIndex), fieldIndex);
			}
			
			for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
				int rowCellCount = getRow(rowIndex).length();
				if (rowCellCount != columnCount()) {
					throw new Exception("Row " + rowIndex + " has " + rowCellCount + " cells in a table of " + columnCount() + " columns.");
				}
			}
		}
		
		public JSONData(JSONObject jsonObject) throws Exception {
			this(jsonObject.getJSONArray("fields"), jsonObject.getJSONArray("rows"));
		}
		
		public JSONData(String json) throws Exception {
			this(new JSONObject(json));
		}
		
		public Row getRow(int rowIndex) throws Exception {
			return new Row(_rows.getJSONArray(rowIndex));
		}
		
		public int columnCount() throws JSONException {
			return _fields.length();
		}
		
		public int rowCount() throws JSONException {
			return _rows.length();
		}
		
		public String toString() {
			String rowsString = "";
			for (int rowIndex = 0; rowIndex < _rows.length(); rowIndex++) {
				try {
					rowsString += "\n        " + _rows.getJSONArray(rowIndex).toString();
				} catch (Exception e) {}
			}
			return "{"
					+ "\n    fields: " + _fields
					+ "\n    rows: ["
					+ rowsString
					+ "\n    ]"
					+ "\n}";
		}
	}
}
