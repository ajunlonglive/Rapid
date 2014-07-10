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
		String command = getProperty("command").trim();
		// if either is null it's not going to work
		if (controlId == null || command == null) {
			return "";
		} else if ("".equals(controlId) || "".equals(command)) {
			return "";
		} else {
			// command can be cleaned up - remove starting dot (we'll add it back later)
			if (command.charAt(0) == '.') command = command.substring(1);
			// add a semi colon if there isn't one on the end
			if (!(command.charAt(command.length() - 1) == ';')) command += ";";
			// return the command
			return "$(\"#" + getProperty("control") + "\")." + command;
		}		
	}
				
}
