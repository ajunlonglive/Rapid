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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;

public class Mobile extends Action {

	// private instance variables
	private List<Action> _successActions, _errorActions, _onlineActions, _childActions;

	// properties
	public List<Action> getSuccessActions() { return _successActions; }
	public void setSuccessActions(List<Action> successActions) { _successActions = successActions; }

	public List<Action> getErrorActions() { return _errorActions; }
	public void setErrorActions(List<Action> errorActions) { _errorActions = errorActions; }

	public List<Action> getOnlineActions() { return _onlineActions; }
	public void setOnlineActions(List<Action> onlineActions) { _onlineActions = onlineActions; }

	// constructors

	// used by jaxb
	public Mobile() {
		super();
	}
	// used by designer
	public Mobile(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
		this();
		// save all key/values from the json into the properties
		for (String key : JSONObject.getNames(jsonAction)) {
			// add all json properties to our properties, except for success and error actions
			if (!"successActions".equals(key) && !"errorActions".equals(key) && !"onlineActions".equals(key)) addProperty(key, jsonAction.get(key).toString());
		}

		// upload images was modified to have a number of gallery ids (rather than 1) migrate for old versions
		String type = getProperty("actionType");
		// if this is upload images
		if ("uploadImages".equals(type)) {
			// get any single gallery controlId
			String galleryControlId = getProperty("galleryControlId");
			// if not null
			if (galleryControlId != null) {
				// empty the property
				_properties.remove("galleryControlId");
				// move it into the galleryControlIds
				_properties.put("galleryControlIds", "[\"" + galleryControlId + "\"]");
			}
		}

		// grab any successActions
		JSONArray jsonSuccessActions = jsonAction.optJSONArray("successActions");
		// if we had some
		if (jsonSuccessActions != null) {
			_successActions = Control.getActions(rapidServlet, jsonSuccessActions);
		}

		// grab any errorActions
		JSONArray jsonErrorActions = jsonAction.optJSONArray("errorActions");
		// if we had some
		if (jsonErrorActions != null) {
			// instantiate our contols collection
			_errorActions = Control.getActions(rapidServlet, jsonErrorActions);
		}

		// grab any onlineActions
		JSONArray jsonOnlineActions = jsonAction.optJSONArray("onlineActions");
		// if we had some
		if (jsonOnlineActions != null) {
			// instantiate our contols collection
			_onlineActions = Control.getActions(rapidServlet, jsonOnlineActions);
		}
	}

	// overridden methods

	@Override
	public List<Action> getChildActions() {
		// initialise and populate on first get
		if (_childActions == null) {
			// our list of all child actions
			_childActions = new ArrayList<>();
			// add online actions
			if (_onlineActions != null) {
				for (Action action : _onlineActions) _childActions.add(action);
			}
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
	public String getPageJavaScript(RapidRequest rapidRequest, Application application, Page page, JSONObject jsonDetails) throws Exception {

		// get this actions id
		String id = getId();
		// get the type
		String type = getProperty("actionType");
		// get any working / loading page
		String workingPage = getProperty("onlineWorking");

		// if it's online
		if ("online".equals(type)) {
			// if there was one record that we have a working page in the details
			if (workingPage != null && workingPage.trim().length() > 0) jsonDetails.put("workingPage", id);
			// get the offline dialogue
			String offlinePage = getProperty("onlineFail");
			// record that we have an offline page
			if (offlinePage != null && !offlinePage.equals("")) jsonDetails.put("offlinePage", offlinePage);
		}

		// reference to these success and fail actions are sent as callbacks to the on-mobile device file upload function, uploadImages always has at least an error
		if ( _successActions == null && _errorActions == null && !"uploadImages".equals(type)) {
			return null;
		} else {

			// we add the successCheck to the the details in the getJavaScript (and empty is again each time before it runs)
			String js = "_" + id + "successChecks = {};\n\n";

			// get the control (the slow way)
			Control control = page.getActionControl(id);
			// check if we have any success actions
			if (_successActions != null || "uploadImages".equals(type)) {
				// start the callback function
				js += "function " + id + "success(ev) {\n";
				// hide any working page
				js += getWorkingPageHideJavaScript(workingPage, "  ");
				// if there are success actions
				if (_successActions != null) {
					// the success actions
					for (Action action : _successActions) {
						js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
					}
				}
				js += "}\n\n";
			}
			// check if we have any error actions, or we are doing uploadImages
			if (_errorActions != null || "uploadImages".equals(type)) {
				// start the callback function
				js += "function " + id + "error(ev, server, status, message) {\n";
				// hide any working page
				js += getWorkingPageHideJavaScript(workingPage, "  ");
				if (_errorActions == null) {
					// look for any offline page
					String offlinePage = jsonDetails.optString("offlinePage", null);
					// use the default error handler
					js += "  " + getDefaultErrorJavaScript(application, page, control, offlinePage).replaceAll("\n", "\n  ") + "\n";
				} else {
					// the error actions
					for (Action action : _errorActions) {
						js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
					}
				}
				js += "}\n\n";
			}
			return js;
		}

	}

	// a re-usable function to check whether we are on a mobile device - this is used selectively according to the type and whether the alert should appear or we can silently ignore
	private String getMobileCheck(boolean alert) {
		// check that rapidmobile is available
		String js = "if (typeof _rapidmobile == 'undefined') {\n";
		// check we have errorActions
		if (_errorActions == null) {
			if (alert) js += "  alert('This action is only available in Rapid Mobile');\n";
		} else {
			js += "  " + getId() + "error(ev, {}, 1, 'This action is only available in Rapid Mobile');\n";
		}
		js += "} else {\n";
		return js;
	}

	// this function is used where an alternative exists that would not require an error message
	private String getMobileCheckAlternative() {
		// check that rapidmobile is available
		String js = "if (typeof _rapidmobile != 'undefined') {\n";
		// return
		return js;
	}

	// a re-usable function for printing the details of the outputs
	private String getMobileOutputs(RapidHttpServlet rapidServlet, Application application, Page page, String outputsJSON) throws JSONException {

		// start the outputs string
		String outputsString = "";

		// read into json Array
		JSONArray jsonOutputs = new JSONArray(outputsJSON);

		// loop
		for (int i = 0; i < jsonOutputs.length(); i++) {

			// get the gps desintation
			JSONObject jsonGpsDestination = jsonOutputs.getJSONObject(i);

			// get the itemId
			String itemId = jsonGpsDestination.getString("itemId");
			// split by escaped .
			String idParts[] = itemId.split("\\.");
			// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
			if (idParts.length > 1) itemId = idParts[0];

			// get the field
			String field = jsonGpsDestination.optString("field","");

			// first try and look for the control in the page
			Control destinationControl = page.getControl(itemId);
			// assume we found it
			boolean pageControl = true;
			// check we got a control
			if (destinationControl == null) {
				// now look for the control in the application
				destinationControl = application.getControl(rapidServlet.getServletContext(), itemId);
				// set page control to false
				pageControl = false;
			}

			// check we got one from either location
			if (destinationControl != null) {

				// get any details we may have
				String details = destinationControl.getDetailsJavaScript(application, page);

				// if we have some details
				if (details != null) {
					// if this is a page control
					if (pageControl) {
						// the details will already be in the page so we can use the short form
						details ="\\\"" +  destinationControl.getId() + "details" + "\\\"";
					} else {
						// escape the object
						details = "\\\"" + details.replace("\"","\\\\\\\"") + "\\\"";
					}
				}

				// if the idParts is greater then 1 this is a set property
				if (idParts.length > 1) {

					// get the property from the second id part
					String property = idParts[1];

					// make the set data call to the bridge
					outputsString += "{\\\"f\\\":\\\"setProperty_" + destinationControl.getType() +  "_" + property + "\\\",\\\"id\\\":\\\"" + itemId + "\\\",\\\"field\\\":\\\"" + field + "\\\",\\\"details\\\":" + details + ",\\\"changeEvents\\\":true}";

				} else {

					outputsString += "{\\\"f\\\":\\\"setData_" + destinationControl.getType() + "\\\",\\\"id\\\":\\\"" + itemId + "\\\",\\\"field\\\":\\\"" + field + "\\\",\\\"details\\\":" + details + ",\\\"changeEvents\\\":true}";

				} // copy / set property check

				// add a comma if more are to come
				if (i < jsonOutputs.length() - 1) outputsString += ", ";

			} // destination control check

		} // destination loop

		// return
		return outputsString;

	}

	// a re-usable function for printing the details of the outputs
	private String getOutputs(RapidHttpServlet rapidServlet, Application application, Page page, String outputsJSON, String data) throws JSONException {

		// start the outputs string
		String outputsString = "";

		// read into json Array
		JSONArray jsonOutputs = new JSONArray(outputsJSON);

		// loop
		for (int i = 0; i < jsonOutputs.length(); i++) {

			// get the gps desintation
			JSONObject jsonGpsDestination = jsonOutputs.getJSONObject(i);

			// get the itemId
			String itemId = jsonGpsDestination.getString("itemId");
			// split by escaped .
			String idParts[] = itemId.split("\\.");
			// if there is more than 1 part we are dealing with set properties, for now just update the destintation id
			if (idParts.length > 1) itemId = idParts[0];

			// get the field
			String field = jsonGpsDestination.optString("field","");

			// first try and look for the control in the page
			Control destinationControl = page.getControl(itemId);
			// assume we found it
			boolean pageControl = true;
			// check we got a control
			if (destinationControl == null) {
				// now look for the control in the application
				destinationControl = application.getControl(rapidServlet.getServletContext(), itemId);
				// set page control to false
				pageControl = false;
			}

			// check we got one from either location
			if (destinationControl != null) {

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

				// if the idParts is greater then 1 this is a set property
				if (idParts.length > 1) {

					// get the property from the second id part
					String property = idParts[1];

					// make the getGps call to the bridge
					outputsString += "setProperty_" + destinationControl.getType() +  "_" + property + "(ev, '" + itemId + "', '" + field + "', " + details + ", " + data + ", true)";

				} else {

					outputsString += "setData_" + destinationControl.getType() + "(ev, '" + itemId + "', '" + field + "', " + details + ", " + data + ", true)";

				} // copy / set property check

				// add a comma if more are to come
				if (i < jsonOutputs.length() - 1) outputsString += ", ";

			} // destination control check

		} // destination loop

		// return
		return outputsString;

	}

	// a helper method to check controls exist
	private boolean checkControl(ServletContext servletContext, Application application, Page page, String controlId) {

		// assume control not found
		boolean controlFound = false;
		// check we got a control id
		if (controlId != null) {
			// if i starts with System
			if (controlId.startsWith("System.")) {
				// we're ok
				controlFound = true;
			} else {
				// look for the control
				if (Control.getControl(servletContext, application, page, controlId) != null) controlFound = true;
			}
		}
		return controlFound;
	}

	@Override
	public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {

		// start the js
		String js = "";
		// get the servlet
		RapidHttpServlet rapidServlet = rapidRequest.getRapidServlet();
		// get the context
		ServletContext servletContext = rapidServlet.getServletContext();
		// get the type
		String type = getProperty("actionType");
		// get this actions id
		String id = getId();
		// check we got something
		if (type != null) {

			// check the type
			if ("dial".equals(type) || "sms".equals(type)) {
				// get the number control id
				String numberControlId = getProperty("numberControlId");
				// get the control
				Control numberControl = Control.getControl(servletContext, application, page, numberControlId);
				// check we got one, unless it's System.field
				if (numberControl == null && !"System.field".equals(numberControlId)) {
					js += "// phone number control " + numberControlId + " not found\n";
				} else {
					// get the number field
					String numberField = getProperty("numberField");
					// get number
					js += "  var number = " + Control.getDataJavaScript(servletContext, application, page, numberControlId, numberField) + ";\n";
					// sms has a message too
					if ("sms".equals(type)) {
						// get the message control id
						String messageControlId = getProperty("messageControlId");
						// get the messagecontrol
						Control messageControl = Control.getControl(servletContext, application, page, messageControlId);
						// check we got one
						if (messageControl == null && !"System.field".equals(messageControlId)) {
							js += "// message control " + numberControlId + " not found\n";
						} else {
							// get the field
							String messageField = getProperty("messageField");
							// get the message
							js += "var message = " + Control.getDataJavaScript(servletContext, application, page, messageControlId, messageField) + ";\n";
							// start mobile check
							js += getMobileCheckAlternative();
							// send the message
							js += "  _rapidmobile.openSMS(number, message);\n";
							// else
							js += "} else {\n";
							// no rapid mobile so just open in new tab
							js += "  window.location.href = 'sms:' + number + '?body=' + message;\n";
							// close mobile check
							js += "}\n";
						}
					} else {
						// start mobile check
						js += getMobileCheckAlternative();
						// dial number
						js += "  _rapidmobile.openPhone(number);\n";
						// else
						js += "} else {\n";
						// no rapid mobile so just open in new tab
						js += "  window.location.href = 'tel:' + number;\n";
						// close mobile check
						js += "}\n";
					}
				}

			} else if ("email".equals(type)) {

				// get the email control id
				String emailControlId = getProperty("emailControlId");
				// check we got one
				if (checkControl(servletContext, application, page, emailControlId)) {
					// get the email field
					String emailField = getProperty("emailField");
					// get the email
					js += "var email = " + Control.getDataJavaScript(servletContext, application, page, emailControlId, emailField) + ";\n";
					// get the subject js
					String subjectGetDataJS = Control.getDataJavaScript(servletContext, application, page, getProperty("subjectControlId"), getProperty("subjectField"));
					// add the subject js
					js += "var subject = " + (("".equals(subjectGetDataJS) || subjectGetDataJS == null) ? "''" : subjectGetDataJS) + ";\n";
					// subject safety check
					js += "if (!subject) subject = '';\n";
					// get the message js
					String messageGetDataJS = Control.getDataJavaScript(servletContext, application, page, getProperty("messageControlId"), getProperty("messageField"));
					// get the message
					js += "var message = " + (("".equals(messageGetDataJS) || messageGetDataJS == null) ? "''" : messageGetDataJS) + ";\n";
					// message safety check
					js += "if (!message) message = '';\n";
					// start the alernative mobile check
					js += getMobileCheckAlternative();
					// start the check for rapid mobile function
					js += "  if (_rapidmobile.openEmail) {\n";
					// send the message
					js += "    _rapidmobile.openEmail(email, subject, message);\n";
					// close the open url check
					js += "  } else alert('Opening emails is not supported in this version of Rapid Mobile');\n";
					// else
					js += "} else {\n";
					// comma's seem to affect the line breaking in our body
					js += "  message = message.replace(',','%2c').replace(' ','%20').replace('.','%2e');\n";
					// open mail link
					js += "  window.location.href = 'mailto:' + email + '?subject=' + subject + '&body=' + message;\n";
					// close the mobile check
					js += "}\n";


				} else {
					js += "// email control " + emailControlId + " not found\n";
				}

			} else if ("url".equals(type)) {

				// get the url control id
				String urlControlId = getProperty("urlControlId");
				// check we got one
				if (checkControl(servletContext, application, page, urlControlId)) {
					// get the field
					String urlField = getProperty("urlField");
					// get the url
					js += "var url = " + Control.getDataJavaScript(servletContext, application, page, urlControlId, urlField) + ";\n";
					// start the alernative mobile check
					js += getMobileCheckAlternative();
					// start the check for the openurl function
					js += "  if (_rapidmobile.openURL) {\n";
					// send the message
					js += "    _rapidmobile.openURL(url);\n";
					// close the open url check
					js += "  } else alert('Opening URLs is not supported in this version of Rapid Mobile');\n";
					// else
					js += "} else {\n";
					// no rapid mobile so just open in new tab
					js += "  window.open(url, '_blank');\n";
					// close the mobile check
					js += "}\n";
				} else {
					js += "// url control " + urlControlId + " not found\n";
				}

			} else if ("addImage".equals(type) || "addVideo".equals(type) || "addImageVideo".equals(type)) {

				// get the gallery control Id
				String galleryControlId = getProperty("galleryControlId");
				// get the gallery control
				Control galleryControl = page.getControl(galleryControlId);
				// check if we got one
				if (galleryControl == null) {
					js += "  //gallery control " + galleryControlId + " not found\n";
				} else {
					int maxSize = Integer.parseInt(getProperty("imageMaxSize"));
					int quality = Integer.parseInt(getProperty("imageQuality"));
					int maxDuration = 0;
					if(getProperty("videoMaxDuration") != null && !getProperty("videoMaxDuration").isEmpty())
						maxDuration = Integer.parseInt(getProperty("videoMaxDuration"));
					Boolean cameraSelectImage = false;
					if(getProperty("cameraSelectImage")!=null)
						cameraSelectImage = Boolean.parseBoolean(getProperty("cameraSelectImage"));
					String remoteSource = getProperty("remoteSource");
					String captureMode = getProperty("captureMode");
					String includeAudio = getProperty("includeAudio");

					String mediums =
							"addImage".equals(type) ? "['image']" :
							"addVideo".equals(type) ? "['video']" :
							"addImageVideo".equals(type) ? "['image','video']" :
							"[]";

					// mobile check, use
					js += "if (typeof _rapidmobile == 'undefined') {\n"
						+ "  turnOnCamera('" + galleryControlId + "'," + maxSize + "," + quality + "," + maxDuration + "," + cameraSelectImage + ", " + mediums + ", " + remoteSource + ", " + includeAudio + ", '" + captureMode + "');\n"
						+ "} else {\n";
					js += "  _rapidmobile.addImage('" + galleryControlId + "'," + maxSize + "," + quality + ");\n";
					// close mobile check
					js += "}\n";
				}

			} else if ("selectImage".equals(type)) {

				// get the output control Id
				String galleryControlId = getProperty("galleryControlId");
				// get the control
				Control galleryControl = page.getControl(galleryControlId);
				// check if we got one
				if (galleryControl == null) {
					js += "  //output control " + galleryControlId + " not found\n";
				} else {
					int maxSize = Integer.parseInt(getProperty("imageMaxSize"));
					js += "selectItem('" + galleryControlId + "', " + maxSize + ", ['image','video']);\n";
				}

			} else if ("uploadImages".equals(type)) {

				// make a list of control ids
				List<String> controlIds = new ArrayList<>();

				// get the old style gallery id
				String galleryControlIdProperty = getProperty("galleryControlId");
				// if we got one
				if (galleryControlIdProperty != null) {
					//  add to list if it contains something
					if (galleryControlIdProperty.trim().length() > 0) controlIds.add(galleryControlIdProperty);
				}

				// get the new style gallery ids
				String galleryControlIdsProperty = getProperty("galleryControlIds");
				// if we got one
				if (galleryControlIdsProperty != null) {
					// clean it up
					galleryControlIdsProperty = galleryControlIdsProperty.replace("\"","").replace("[", "").replace("]", "");
					// if anything is left
					if (galleryControlIdsProperty.length() > 0) {
						// split and loop
						for (String galId : galleryControlIdsProperty.split(",")) {
							// add to collection
							controlIds.add(galId);
						}
					}
				}

				// check if we got one
				if (controlIds.size() == 0) {
					js += "  // no controls specified\n";
				} else {

					// ensure we have a details object
					if (jsonDetails == null) jsonDetails = new JSONObject();
					// if it has a workingDialogue
					boolean gotWorkingDialogue = jsonDetails.has("workingDialogue");

					// assume no success call back
					String successCallback = "null";
					// update to name of callback if we have any success actions
					if (_successActions != null || gotWorkingDialogue) successCallback = "'" + id + "success'";
					// we always have an error call back for either showing errors and/or hiding dialogues
					String errorCallback = "'" + id + "error'";

					// start building the js
					js += "var urls = '';\n";

					// a collection of control id's we won't be using, as we can't find them in the page
					List<String> removeControlIds = new ArrayList<>();

					// get any urls from the gallery controls
					for (String controlId : controlIds) {

						// get the control object from its id
						Control imageControl = page.getControl(controlId);
						// if we couldn't find it in the page, try the rest of the application
						if (imageControl == null)
							imageControl = application.getControl(servletContext, controlId);
						// if we got one
						if (imageControl == null) {
							// retain that we will remove this control
							removeControlIds.add(controlId);
						} else {
							// Check whether this control is a signature
							if ("signature".equals(imageControl.getType())) {
								js += "urls += getData_signature(ev, '" + controlId + "');\n";
							} else {
								js += "$('#" + controlId + "').find('.galleryItem').each( function() { urls += $(this).attr('src') + ',' });\n";
							}
						}

					}

					// remove any controls we couldn't find
					controlIds.removeAll(removeControlIds);

					// get any progress output id
					String progressOutputId = getProperty("progressOutputControl");
					// assume there is no progress output JavaScript
					String progressOutputJavaScript = "";
					// if we got one
					if (progressOutputId != null) {
						// get any progress output field
						String progressOutputField = getProperty("progressOutputControlField");
						// get the progress out control
						Control progressOutput = application.getControl(servletContext, progressOutputId);
						// if we got a control
						if (progressOutput != null) progressOutputJavaScript = ", {id:'" + progressOutputId + "', type:'" + progressOutput.getType() + "', field:'" + progressOutputField + "', details:" + progressOutput.getDetails() + "}";
					}

					// if we got any urls check whether request is from a mobile - upload the images
					js += "if (urls) { \n"
						+ "   if (typeof _rapidmobile == 'undefined') {\n"
						+ "      uploadImages(" + new JSONArray(controlIds) + ", ev, " + successCallback + ", " + errorCallback + progressOutputJavaScript + ");\n"
						+ "   } else {\n"
						+ "      _rapidmobile.uploadImages('" + id + "', urls, " + successCallback + ", " + errorCallback + ");\n"
						+ "   }\n"
						+ "}";

					// if there is a successCallback call it now
					if (!"null".equals(successCallback) && successCallback.length() > 0) js += " else {\n  " + successCallback.replace("'", "") + "(ev);\n}";

					// a line-break for either option above
					js += "\n";

				}

			} else if ("downloadImages".equals(type)) {

				// make a list of control ids
				List<String> controlIds = new ArrayList<>();

				// get the old style gallery id
				String galleryControlIdProperty = getProperty("galleryControlId");
				// if we got one
				if (galleryControlIdProperty != null) {
					//  add to list if it contains something
					if (galleryControlIdProperty.trim().length() > 0) controlIds.add(galleryControlIdProperty);
				}

				// get the new style gallery ids
				String galleryControlIdsProperty = getProperty("galleryControlIds");
				// if we got one
				if (galleryControlIdsProperty != null) {
					// clean it up
					galleryControlIdsProperty = galleryControlIdsProperty.replace("\"","").replace("[", "").replace("]", "");
					// if anything is left
					if (galleryControlIdsProperty.length() > 0) {
						// split and loop
						for (String galId : galleryControlIdsProperty.split(",")) {
							// add to collection
							controlIds.add(galId);
						}
					}
				}

				// check if we got one
				if (controlIds.size() == 0) {
					js += "  // no controls specified\n";
				} else {

					// assume no success call back
					String successCallback = "null";
					// update to name of callback if we have any success actions
					if (_successActions != null) successCallback = "'" + id + "success'";
					// assume no error call back
					String errorCallback = "null";
					// update to name of callback  if we have any error actions
					if (_errorActions != null) errorCallback = "'" + id + "error'";

					// start building the js
					js += "var urls = '';\n";

					// a collection of control id's we won't be using, as we can't find them in the page
					List<String> removeControlIds = new ArrayList<>();

					// get any urls from the gallery controls
					for (String controlId : controlIds) {

						// get the control object from its id
						Control imageControl = page.getControl(controlId);
						// if we got one
						if (imageControl == null) {
							// retain that we will remove this control
							removeControlIds.add(controlId);
						} else {
							// Check whether this control is a signature
							if ("signature".equals(imageControl.getType())) {
								js += "urls += getData_signature(ev, '" + controlId + "');\n";
							} else {
								js += "$('#" + controlId + "').find('.galleryItem').each( function() { urls += $(this).attr('src') + ',' });\n";
							}
						}

					}

					// remove any controls we couldn't find
					controlIds.removeAll(removeControlIds);

					// if we got any urls check whether request is from a mobile - upload the images
					js += "if (urls) { \n"
						+ "    downloadImages(" + new JSONArray(controlIds) + ", ev, " + successCallback + ", " + errorCallback + ");\n"
						+ "}";

					// if there is a successCallback call it now
					if (!"null".equals(successCallback) && successCallback.length() > 0) js += " else {\n  " + successCallback.replace("'", "") + "(ev);\n}";

					// a line-break for either option above
					js += "\n";

				}

			} else if ("navigate".equals(type)) {

				// get the naviagte source control id
				String navigateControlId = getProperty("navigateControlId");
				// get the control
				Control navigateControl = Control.getControl(servletContext, application, page, navigateControlId);
				// check we got one (but allow System.field)
				if ((navigateControlId == null || navigateControl == null) && !"System.field".equals(navigateControlId)) {
					js += "// navigate to control " + navigateControlId + " not found\n";
				} else {
					// get the navigate to field
					String navigateField = getProperty("navigateField");
					// get the mode
					String navigateMode = getProperty("navigateMode");
					// get the data
					js += "var data = " + Control.getDataJavaScript(servletContext, application, page, navigateControlId, navigateField) + ";\n";
					// assume no search fields
					String searchFields = getProperty("navigateSearchFields");
					// if we got some
					if (searchFields != null) {
						// if there's something
						if (searchFields.trim().length() > 0) {
							// build the JavaScript object
							searchFields = "{searchFields:'" + searchFields.replace("'", "\'") + "'}";
						} else {
							// set to null
							searchFields = null;
						}
					}
					// get a position object
					js += "var pos = getMapPosition(data, 0, null, null, " + searchFields + ");\n";
					// assume no travelMode
					String travelMode = "";
					// use navigateMode to determine
					if (navigateMode != null) {
						switch (navigateMode) {
							case "d": travelMode = "driving"; break;
							case "w": travelMode = "walking"; break;
							case "b": travelMode = "bicycling"; break;
							case "t": travelMode = "transit"; break;
						}
						// enclose in commas
						navigateMode = "'" + navigateMode + "'";
					}
					// mobile check
					js += "if (typeof _rapidmobile == 'undefined') {\n";
					// add js, replacing any dodgy inverted commas - simple data will not get lat, lng, or s so send it just as the search
					js += "  if (pos && (pos.lat && pos.lng)) {\n     window.open('https://www.google.com/maps/dir/?api=1&destination=' + pos.lat + ',' + pos.lng + '&travelmode=" + travelMode + "','_blank');\n  } else {\n     window.open('https://www.google.com/maps/dir/?api=1&destination=' + data.replaceAll(' ','+') + '&travelmode=" + travelMode + "','_blank');\n  }\n";
					js += "} else {\n";
					// add js, replacing any dodgy inverted commas - simple data will not get lat, lng, or s so send it just as the search
					js += "  if (pos && (pos.lat || pos.lng || pos.s)) {\n     _rapidmobile.navigateTo(pos.lat, pos.lng, pos.s, " + navigateMode + ");\n  } else {\n    _rapidmobile.navigateTo(null, null, data, " + navigateMode + ");\n  }\n";
					// close mobile check
					js += "}\n";
				}

			}  else if ("message".equals(type)) {

				// retrieve the message
				String message = getProperty("message");
				// update to empty string if null
				if (message == null) message = "";
				// mobile check with silent fail
				js += getMobileCheck(false);
				// add js, replacing any dodgy inverted commas
				js += "  _rapidmobile.showMessage('" + message.replace("'", "\\'") + "');\n";
				// close mobile check
				js += "}\n";

			} else if ("disableBackButton".equals(type)) {

				// mobile check with silent fail
				js += getMobileCheck(false);
				// add js
				js += "    _rapidmobile.disableBackButton();\n";
				// close mobile check
				js += "  }\n";

			} else if ("sendGPS".equals(type)) {

				// get the gps destinations
				String gpsDestinationsString = getProperty("gpsDestinations");

				// if we had some
				if (gpsDestinationsString != null) {

					// mobile check manually
					js +="if (typeof _rapidmobile == 'undefined') {\n";

					// not on Rapid Mobile - check for location
					js += "  if (navigator.geolocation) {\n";
				    js += "    navigator.geolocation.getCurrentPosition(function(pos) {\n";
				    js += "      var data = {fields:['lat','lng','accuracy'],rows:[[pos.coords.latitude,pos.coords.longitude,pos.coords.accuracy]]};\n";

				    try {

						// add the gpsDestinationsString
						String getGPSjs = "      " + getOutputs(rapidServlet, application, page, gpsDestinationsString,"data") + ";\n";

						// add it into the js
						js += getGPSjs;

					} catch (JSONException ex) {

						// print an error into the js instead
						js += "  // error reading gpsDestinations : " + ex.getMessage();

					}

				    js	+= "    });\n";
				    js += "  } else {\n    alert('Location is not available');\n  }\n";
				    js += "} else {\n";

					// get whether to check if gps is enabled
					boolean checkGPS = Boolean.parseBoolean(getProperty("gpsCheck"));
					// if we had one call it
					if (checkGPS) js += "  _rapidmobile.checkGPS();\n";

					// get the gps frequency into an int
					int gpsFrequency = Integer.parseInt(getProperty("gpsFrequency"));

					try {

						// start the getGPS string
						String getGPSjs = "  _rapidmobile.getGPS(" + gpsFrequency + ",\"[";

						// add the gpsDestinationsString
						getGPSjs += getMobileOutputs(rapidServlet, application, page, gpsDestinationsString);

						// close the get gps string
						getGPSjs += "]\");\n";

						// add it into the js
						js += getGPSjs;

					} catch (JSONException ex) {

						// print an error into the js instead
						js += "  // error reading gpsDestinations : " + ex.getMessage();

					}

					// close mobile check
					js += "}\n";

				} // gps destinations check

			} else if ("stopGPS".equals(type)) {

				// mobile check with silent fail
				js += getMobileCheck(false);
				// call stop gps
				js += "  _rapidmobile.stopGPS();\n";
				// close mobile check
				js += "}\n";

			} else if ("swipe".equals(type)) {

				// check we have online actions
				if (_onlineActions != null) {
					// check size
					if (_onlineActions.size() > 0) {

						try {

							// get the direction
							String direction = getProperty("swipeDirection");
							// get the finders
							String fingers = getProperty("swipeFingers");
							// update if "any"
							if ("any".equals(fingers)) fingers = "0";
							// get the target
							String target = getProperty("swipeControl");
							// add # if not html
							if (!"html".equals(target)) target = "#" + target;

							// see if there is a swipe handler present for the target AND if this is a mobile browser
							js += "if(/iPhone|iPad|iPod|Android/i.test(navigator.userAgent)){\n"
								+ "		if (!_swipeHandlers['" + target + "']) {\n";
							// register the handler for any fingers on the target
							js += "			$('" + target + "').swipe( { swipe:function(event, direction, distance, duration, fingers, fingerData) { handleSwipe(event, direction, distance, duration, fingers, fingerData, '" + target + "', ev); },fingers:'all'});\n";
							// make the array
							js += "  		_swipeHandlers['" + target + "'] = [];\n";
							// close the check
							js += "		}\n";


							// start the function
							js += "		var f = function(ev) {\n";

							// loop actions
							for (Action action : _onlineActions) {
								// add action js
								js += "			" + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n			") + "\n";
							}

							// close the functions
							js += "		};\n";

							// add this handler
							js += "		_swipeHandlers['" + target + "'].push({'direction':'" + direction + "','fingers':" + fingers + ",'function':f});\n"
								+ "}\n";

						} catch (Exception ex) {
							// print an error instead
							js = "// failed to create swipe mobile action " + id + " JavaScript : " + ex.getMessage() + "\n";
						}

					} // actions count check
				} // actions null check

			} else if ("online".equals(type)) {

				// assume we're not doing any success check
				boolean successCheck = false;

				// see if we have any online success or error actions
				if ((_successActions != null && _successActions.size() > 0) || (_errorActions != null && _errorActions.size() > 0)) {

					// retain that we're doing a success check
					successCheck = true;

					// ensure we have a details object
					if (jsonDetails == null) jsonDetails = new JSONObject();

					// retain on the details that we have an offline page
					jsonDetails.put("successCheck", id);
					// set it to empty
					js += "_" + id + "successChecks = {};\n";

				}

				// check we have online actions
				if (_onlineActions != null && _onlineActions.size() > 0) {

					try {

						// ensure we have a details object
						if (jsonDetails == null) jsonDetails = new JSONObject();

						// add js online check
						js += "if (typeof _rapidmobile == 'undefined' ? navigator.onLine : _rapidmobile.isOnline()) {\n";

						// get any working / loading page
						String workingPage = getProperty("onlineWorking");
						// if there was one
						if (workingPage != null && !workingPage.equals("")) {
							// show working page as a dialogue
							js += "  if (Action_navigate) Action_navigate('~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + workingPage + "&action=dialogue', true, '" + id + "');\n";
							// record that we have a working page in the details
							jsonDetails.put("workingPage", id);
						}

						// get the offline dialogue
						String offlinePage = getProperty("onlineFail");
						// record that we have an offline page
						if (offlinePage != null && !offlinePage.equals("")) jsonDetails.put("offlinePage", offlinePage);

						// loop the online actions (this will apply the working and offline entries in the details)
						for (Action action : _onlineActions) {
							// add the child action JavaScript
							js += "  " + action.getJavaScriptWithHeader(rapidRequest, application, page, control, jsonDetails).trim().replace("\n", "\n  ") + "\n";
						}

						// check the success, if there is one, in case all actions were client-side
						if (successCheck) js += "  successCheck('" + id + "', null, true, ev);\n";

						// js online check fail
						js += "} else {\n";

						// if we have an offline page one show it
						if (offlinePage != null) js += "  if (Action_navigate) Action_navigate('~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + offlinePage + "&action=dialogue', true, '" + id + "');\n";

						// close online check
						js += "}\n";

					} catch (Exception ex) {
						// print an error instead
						js = "// failed to create online mobile action " + id + " JavaScript : " + ex.getMessage() + "\n";
					}

				} else {

					// if there are no online actions but there are success actions call the any success immediately
					if (successCheck) js += id + "success(ev);\n";

				} // online actions check non-null check

			} else if ("addBarcode".equals(type) || "addRFID".equals(type)) {

				try {

					// get the barcodeDestinations
					String barcodeDestinations = getProperty("barcodeDestinations");

					// start the check for the add function
					String jsBarcode = "if (typeof _rapidmobile != 'undefined' && _rapidmobile." + type + ") {\n";

					// start the add barcode or add RFID call
					jsBarcode += "  _rapidmobile." + type + "(\"[";

					jsBarcode += getMobileOutputs(rapidServlet, application, page, barcodeDestinations);

					// call get barcode
					jsBarcode +=  "]\");\n";

					jsBarcode += "} else {\n";

					if ("addBarcode".equals(type)) {

						// since no Rapid mobile do scanning using the client JavaScript
						jsBarcode += "  scanQrCodeAction(function(data) {\n";
						jsBarcode += "    " + getOutputs(rapidServlet, application, page, barcodeDestinations, "data") + "\n";
						jsBarcode +=  " });\n";

					} else {

						// tell the user - but we will add JavaScript RFID scanning at some point!
						jsBarcode += "    alert('RFID scanning is not available in this version of Rapid Mobile');\n";

					}

					// close if (_rapidmobile.addBarcode)
					jsBarcode += "}\n";

					// now safe to add back into main js
					js += jsBarcode;

				} catch (JSONException ex) {

					// print an error into the js instead
					js += "  // error reading barcode : " + ex.getMessage();

				}

			} // mobile action type check

		} // mobile action type non-null check

		// return an empty string
		return js;
	}

}