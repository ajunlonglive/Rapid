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

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

import org.json.JSONObject;

/*

This action runs JQuery against a specified control. Can be entered with or without the leading "." Such as hide(), or .css("disabled","disabled");

*/

public class Control extends Action {

	// parameterless constructor (required for jaxb)
	Control() { super(); }
	// designer constructor
	public Control(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);				
	}
	
	// methods
		
	@Override
	public String getJavaScript(Application application, Page page, com.rapid.core.Control control) {
		// get the control Id and command
		String controlId = getProperty("control");
		// get the action type
		String actionType = getProperty("actionType");
		// prepare the js
		String js = "";
		// if we have a control id
		if (controlId != null && !"".equals(controlId)) {
			// update the js to use the control
			js = "$(\"#" + getProperty("control") + "\").";
			// check the type
			if ("custom".equals(actionType) || actionType == null) {
				String command = getProperty("command").trim();			
				// command can be cleaned up - remove starting dot (we've already got it above)
				if (command.charAt(0) == '.') command = command.substring(1);
				// add brackets if there aren't any at the end
				if (!command.endsWith(")") && !command.endsWith(");")) command += "();";
				// add a semi colon if there isn't one on the end
				if (!command.endsWith(";")) command += ";";
				// add the command
				js += command;		
			} else if ("slideDown".equals(actionType) || "slideUp".equals(actionType) || "slideToggle".equals(actionType)) {
				js += actionType + "(" + getProperty("duration") + ");";
			} else {
				// just call the action type (hide/show/toggle)
				js += actionType + "();";
			}
		}
		// return the js
		return js;
	}
				
}
