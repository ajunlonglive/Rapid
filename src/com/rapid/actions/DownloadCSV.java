/*

Copyright (C) 2022 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class DownloadCSV extends Action{

	// constructors

	// used by jaxb
	public DownloadCSV() {
		super();
	}

	// used by designer when saving
	public DownloadCSV(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// call the super parameterless constructor which sets the xml version
		super();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for dataCopies
			addProperty(key, jsonAction.get(key).toString());
		}
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control,	JSONObject jsonDetails) throws Exception {

		// get the source table ID (previously this was just grids so we've kept the property named the same for backwards compatibility)
		String controlId = getProperty("gridId");
		// get the output file name
		String outputFile = getProperty("outputFilename");

		String includeHiddenColumns = getProperty("includeHiddenColumns");
		String includeFields = getProperty("includeFields");
		if (includeFields == null) includeFields = "";

		Control dataControl = application.getControl(rapidRequest.getRapidServlet().getServletContext(), controlId);

		String js = "var details = ";

		if (dataControl == null) {

			js += " null;\n";

		} else {

			// get any details we may have
			String details = dataControl.getDetailsJavaScript(application, page);

			// check we got some
			if (details == null) {

				js += " null;\n";

			} else {

				js += details + ";\n";

			}

		}

		if(outputFile == null || "".equals(outputFile)) outputFile = "filename.csv";
		if(!outputFile.endsWith(".csv")) outputFile += ".csv";

		js += "Action_downloadCSV(ev, '" + this.getId() + "', '" + controlId + "', '" + outputFile + "', " + includeHiddenColumns + ", '" + includeFields + "', details)";

		return js;
	}

}
