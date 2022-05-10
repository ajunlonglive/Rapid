/*

Copyright (C) 2022 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.core;

import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONObject;
import org.w3c.dom.Node;

import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public abstract class Action {

	// the version of this class's xml structure when marshelled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)
	public static final int XML_VERSION = 1;

	// we can this version to be written into the xml when marshelled so we can upgrade any xml before marshelling
	private int _xmlVersion;

	// all properties are stored here (allowing us to describe just in the .action.xml files)
	protected HashMap<String,String> _properties;
	// whether this actions JavaScript should be placed in its own reusable function
	private boolean _avoidRedundancy;

	// the xml version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }

	// properties
	public HashMap<String,String> getProperties() { return _properties; }
	public void setProperties(HashMap<String,String> properties) { _properties = properties; }

	// a parameterless constructor is required so they can go in the JAXB context and be unmarshalled
	public Action() {
		// set the xml version
		_xmlVersion = XML_VERSION;
		// initialise properties
		_properties = new HashMap<>();
	}

	// json constructor allowing properties to be sent in from the designer
	public Action(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		// run the parameterless constructor
		this();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties
			addProperty(key, jsonAction.get(key).toString());
		}
	}

	// these are some helper methods for common properties, ignoring some common ones that will always have their own getters/setters
	public void addProperty(String key, String value) {
		if (!"childActions".equals(key) && !"successActions".equals(key) && !"errorActions".equals(key))
			_properties.put(key, value);
	}
	// retrieves a specified property
	public String getProperty(String key) { return _properties.get(key); }
	// retrieves the action type
	public String getType() { return getProperty("type"); }
	// retrieves the action id
	public String getId() { return getProperty("id"); }

	// if any actions have success, fail, or other follow on actions this function must return them all
	public List<Action> getChildActions() { return null; }

	// returns whether this action has been marked for redundancy avoidance and a separate JavaScript function and reusable calls will be created for it to avoid printing in the whole thing each time
	public boolean getAvoidRedundancy() { return _avoidRedundancy; }
	// this doesn't start with set so it's not marshalled to the xml file
	public void avoidRedundancy(boolean avoidRedundancy) { _avoidRedundancy = avoidRedundancy; }

	// if any actions run other actions return their id's when we generate the page JavaScript we will create a special action function which we will reuse to avoid redundantly recreating the js each time
	public List<String> getRedundantActions() { return null; }

	// this generates the clientside javascript at the top of the page for any reusable functions or global callbacks
	public String getPageJavaScript(RapidRequest rapidRequest, Application application, Page page, JSONObject jsonDetails) throws Exception { return null; }

	// this generates the clientside javascript inside the events for the action to happen (must be implemented as every action is kicked off from the client side [for now anyway])
	public abstract String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception;

	// this generates the clientside javascript header which has comments to help debugging - used in page and child actions
	public String getJavaScriptWithHeader(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
		// assume no comments
		String comments = "";
		// if we have comments, add them and make it safe if they included a closer
		if (_properties.containsKey("comments")) comments = " - " + _properties.get("comments").replace("*/", "*");
		// add the header, then the regular JavaScript
		return "/* " + getType() + " action " + getId() + comments + " */\n" + getJavaScript(rapidRequest, application, page, control, jsonDetails);
	};

	// this is where any server-side action happens! (some actions are client side only)
	public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonData) throws Exception { return null; };

	// whether this action is a webservice
	public boolean isWebService() { return false; }

	// this method can be overridden to check the xml versions, and upgrade any xml nodes representing specific actions before the xml document is unmarshalled
	public Node upgrade(Node actionNode) { return actionNode; }

	// this produces a helpful string with the source of of the action / control, including ids if selected on the application
	protected String getErrorSource(Application application, Control control) {

		// start with a blank message
		String errorSource = getType() + " action";

		// if we're in development and showing id's add that to the message
		if (application.getStatus() == Application.STATUS_DEVELOPMENT && application.getShowActionIds()) errorSource += " " + getId();

		// if the control is null it's the page
		if (control == null) {
			errorSource += " on page";
		} else {

			// add the control type
			errorSource += " from " + control.getType();

			// get the control name
			String controlName = control.getName();

			// if we're in development and showing id's
			if (application.getStatus() == Application.STATUS_DEVELOPMENT && application.getShowControlIds()) {
				// add id to the message
				errorSource += " " + control.getId();
				// add name if there is one
				if (controlName != null && !"".equals(controlName)) errorSource += " " + controlName;
			} else {
				// if there is no name
				if (controlName == null || "".equals(controlName)) {
					// add the id
					errorSource += " " + control.getId();
				} else {
					// add the name
					errorSource += " " + controlName;
				}

			} // development status check

		} // control null check

		// if this action has comments
		if (getProperties().containsKey("comments")) {
			// get the comments
			String comments = getProperty("comments");
			// if there is a double line break stop at that (sometimes there are paragraphs and sql statements in here)
			if (comments.contains("\n\n")) comments = comments.substring(0, comments.indexOf("\n\n"));
			// trim for good measure
			comments = comments.trim();
			// escape any quotes and new lines (this causes issues for the JavaScript)
			comments = comments.replace("'", "\\'").replace("\n", "\\n");
			// add the comments after the error message
			errorSource += ("\\n\\n" + comments);
		}

		return errorSource;
	}

	// the JavaScript to actually hide the working page - used both in the method below from actions, and from the group success of ones like mobile online success
	protected String getWorkingPageHideJavaScript(String workingPage, String padding) {

		// check if there is a working page
		if (workingPage == null || workingPage.trim().isEmpty()) {

			// safely return empty string if not
			return "";

		} else {

			// return js to hide working page
			return padding + "$('#" + workingPage + "').hideDialogue(false, '" + workingPage + "');\n";

		}
	}

	// this produces JavaScript with the error actions and a standardised way of dealing with and showing or hiding offline and working dialogues
	protected String getWorkingPageHideJavaScript(JSONObject jsonDetails, boolean success, List<Action> successActions, String padding) throws Exception {

		// start the JavaScript as an empty string
		String js = "";

		// instantiate the jsonDetails if required
		if (jsonDetails == null) jsonDetails = new JSONObject();

		// look for a successCheck in the jsonDetail
		String successCheck = jsonDetails.optString("successCheck", null);

		// if there is a success check the working page dialigue hide will be done when they all complete successfully, not here
		if (successCheck == null) {

			// look for a working page in the jsonDetails
			String workingPage = jsonDetails.optString("workingPage", null);

			// if there is a working page (from the details) and we have no further success children so we must have come to the end of a success branch / finished the action chain successfully
			if (workingPage != null && (successActions == null || successActions.size() == 0)) {

				// hide if we are doing an error branch, or we have not hidden the working yet
				boolean hide = !success || !jsonDetails.optBoolean("workingHidden");
				// if we've not yet hidden the working dialogue on success
				if (hide) {
					// hide any working page dialogue
					js += getWorkingPageHideJavaScript(workingPage, padding);
					// mark that we have applied the working page hide at the end of a success branch
					if (success) jsonDetails.put("workingHidden", true);
				}

			} // working page and success actions check

		} // no successCheck

		return js;

	}

	// overload to produces JavaScript with the success actions and a standardised way of dealing with and showing or hiding offline and working dialogues
	protected String getWorkingPageHideJavaScript(JSONObject jsonDetails, List<Action> successActions, String padding) throws Exception {
		return getWorkingPageHideJavaScript(jsonDetails, true, successActions, padding);
	}

	// overload to produces JavaScript with the error actions and a standardised way of dealing with and showing or hiding offline and working dialogues
	protected String getWorkingPageHideJavaScript(JSONObject jsonDetails, String padding) throws Exception {
		return getWorkingPageHideJavaScript(jsonDetails, false, null, padding);
	}

	protected String getDefaultErrorJavaScript(Application application, Page page, Control control, String offlinePage) {
		// prepare a default error hander we'll show if no error actions, or pass to child actions for them to use
		String defaultErrorHandler = "alert('Error with " + getErrorSource(application, control) + "\\n\\n' + (server.getResponseHeader('Content-Type') && server.getResponseHeader('Content-Type').startsWith('text/html') ? server.status + ' - ' + $(server.responseText).filter('title').text() : server.responseText||message));";
		// if we have an offline page wrap the default above with the offline check and dialogue show
		if (offlinePage != null) {
			// update defaultErrorHandler to navigate to offline page, if we have the navigate action, and we know we're offline, or it looks like we must be
			defaultErrorHandler = "if (Action_navigate && !(typeof _rapidmobile == 'undefined' ? navigator.onLine && server.getAllResponseHeaders() : _rapidmobile.isOnline())) {\n" +
			"  Action_navigate('~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + offlinePage + "&action=dialogue',true,'" + getId() + "');\n" +
			"} else {\n  " + defaultErrorHandler + "\n}";
		}
		return defaultErrorHandler;
	}

	// this produces JavaScript with the error actions and a standardised way of dealing with and showing or hiding offline and working dialogues - some actions like the PDF use a callback instead of this making the calls
	protected String getErrorActionsJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails, List<Action> errorActions, String errorCallback) throws Exception {

		// start the JavaScript by hiding any working page
		String js = getWorkingPageHideJavaScript(jsonDetails, "    ");

		// instantiate the jsonDetails if required
		if (jsonDetails == null) jsonDetails = new JSONObject();
		// look for an offline page in the jsonDetails
		String offlinePage = jsonDetails.optString("offlinePage", null);

		// this avoids doing the errors if the page is unloading or the back button was pressed, unless we know we're offline
		js += "    if (server.readyState > 0 || !navigator.onLine) {\n";

		// prepare a default error hander we'll show if no error actions, or pass to child actions for them to use
		String defaultErrorHandler = getDefaultErrorJavaScript(application, page, control, offlinePage);

		// check for any error actions
		if (errorActions == null || errorActions.size() == 0) {
			// check for any error callback
			if (errorCallback == null) {
				// add default error handler if none
				js += "      " + defaultErrorHandler.replaceAll("\n", "\n      ") + "\n";
			} else {
				// add error callback
				js += "      " + errorCallback + ";\n";
			}
		} else {
			// loop the actions
			for (Action action : errorActions) {
				// add the js for this error child action
				js += "       " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n       ") + "\n";
			}
		}

		// close unloading check
		js += "    }\n";

		// close error actions
		js += "  },\n";


		return js;

	}

	// overload to produce JavaScript with the error actions and a standardised way of dealing with and showing or hiding offline and working dialogues with a list of error actions
	protected String getErrorActionsJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails, List<Action> errorActions) throws Exception {
		// call the full method with null callback
		return getErrorActionsJavaScript(rapidRequest, application, page, control, jsonDetails, errorActions, null);
	}

	// overload to produce JavaScript with the error actions and a standardised way of dealing with and showing or hiding offline and working dialogues with a callback
	protected String getErrorActionsJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails, String errorCallback) throws Exception {
		// call the full method with null callback
		return getErrorActionsJavaScript(rapidRequest, application, page, control, jsonDetails, null, errorCallback);
	}

	// safely check jsonDetails for a successCheck object and return it's contents if so, null otherwise
	protected String getSuccesCheck(JSONObject jsonDetails) {

		// assume no successCheck
		String successCheck = null;
		// if we have jsonDetails
		if (jsonDetails != null) {
			// check for it successCheck
			successCheck = jsonDetails.optString("successCheck", null);
		}

		return successCheck;

	}

	// produce the js at the start of the success check
	protected String getSuccessCheckStart(String successCheck) {

		String js = "";

		if (successCheck != null) js += "if (!successCheck('" + successCheck + "', '" + getId() + "')) return false;\n";

		return js;

	}

	// produce the js for the asynchronous succeeding
	protected String getSuccessCheckSuccess(String successCheck, String padding) {

		String js = "";

		// if there is a successCheck, check it, with the event to fire if all succeeded (further actions will have registered themselves above)
		if (successCheck != null) js += padding + "successCheck('" + successCheck + "', '" + getId() + "', true, ev);\n";

		return js;

	}

	// produce the js for the asynchronous failing
	protected String getSuccessCheckError(String successCheck, String padding) {

		String js = "";

		if (successCheck != null) js += padding + "successCheck('" + successCheck + "', '" + getId() + "', false, ev);\n";

		return js;

	}

	@Override
	public String toString() { return getClass().getName() + " - " + getId(); }

}
