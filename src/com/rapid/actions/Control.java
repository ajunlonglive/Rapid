package com.rapid.actions;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

/*

This action runs JQuery against a specified control. Can be entered with or without the leading "." Such as hide(), or .css("disabled","disabled");

*/

public class Control extends Action {

	// parameterless constructor for jaxb
	public Control() {}
	
	// json constructor for designer - invoked with reflection in Control.getEvents from Desinger.java
	public Control(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws JSONException {	super(rapidServlet, jsonAction); }	
		
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
