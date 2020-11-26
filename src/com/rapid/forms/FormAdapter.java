/*

Copyright (C) 2020 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.forms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Email;
import com.rapid.core.Email.Attachment;
import com.rapid.core.Email.StringDataSource;
import com.rapid.core.Page;
import com.rapid.core.Pages;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Pages.PageHeaders;
import com.rapid.core.Theme;
import com.rapid.core.Validation;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.server.Rapid;
import com.rapid.server.RapidRequest;
import com.rapid.utils.CIFS;
import com.rapid.utils.Html;
import com.rapid.utils.Http;
import com.rapid.utils.Numbers;
import com.rapid.utils.Strings;
import com.rapid.utils.XML;

public abstract class FormAdapter {

	// details about a user form
	public static class UserFormDetails implements Serializable {

		// from serializable
		private static final long serialVersionUID = 101L;

		// instance variables
		private final String _appId, _version, _id, _password;
		private String _maxPageId, _submittedDateTime, _submitMessage, _errorMessage;
		boolean _saved, _complete, _showSubmitPage, _paymentStarted;


		// properties

		// app id
		public String getAppId() { return _appId; }
		// id
		public String getVersion() { return _version; }
		// form id
		public String getId() { return _id; }
		// password
		public String getPassword() { return _password; }

		// whether this form has been saved
		public boolean getSaved() { return _saved; }
		public void setSaved(boolean saved) { _saved = saved; }

		// max page to which users have already been / are allowed
		public String getMaxPageId() { return _maxPageId; }
		public void setMaxPageId(String maxPageId) {_maxPageId = maxPageId; }

		// whether this form has been completed
		public boolean getComplete() { return _complete; }
		public void setComplete(boolean complete) { _complete = complete; }

		// whether this form has started payment
		public boolean getPaymentStarted() { return _paymentStarted; }
		public void setPaymentStarted(boolean paymentStarted) { _paymentStarted = paymentStarted; }

		// the date/time the form was submitted to show on the summary screen
		public String getSubmittedDateTime() { return _submittedDateTime; }
		public void setSubmittedDateTime(String submittedDateTime) { _submittedDateTime = submittedDateTime; }
		// a helper method for the above
		public boolean getSubmitted() { return _submittedDateTime == null ? false : true; }

		// the recently submitted message
		public String getSubmitMessage() { return _submitMessage; }
		public void setSubmitMessage(String submitMessage) { _submitMessage = submitMessage; }

		// whether to show the submission page (not allowed for resuming submitted forms)
		public boolean getShowSubmitPage() { return _showSubmitPage; }
		public void setShowSubmitPage(boolean showSubmitPage) { _showSubmitPage = showSubmitPage; }

		// the recent submission error
		public String getErrorMessage() { return _errorMessage; }
		public void setErrorMessage(String errorMessage) { _errorMessage = errorMessage; }
		// a helper method for the above
		public boolean getError() { return _errorMessage == null ? false : true; }

		// constructors

		// brand new forms
		public UserFormDetails(String appId, String version, String id, String password) {
			_appId = appId;
			_version = version;
			_id = id;
			_password = password;
		}

		// resumed forms
		public UserFormDetails(String appId, String version, String id, String password, String maxPageId, boolean complete, String submittedDateTime) {
			_appId = appId;
			_version = version;
			_id = id;
			_password = password;
			_maxPageId = maxPageId;
			_complete = complete;
			_submittedDateTime = submittedDateTime;
		}

	}

	// details about a submitted form
	public static class SubmissionDetails {

		// instance variables
		String _message, _dateTime;

		// properties
		public String getMessage() { return _message; }
		public String getDateTime() { return _dateTime; }

		// constructor
		public SubmissionDetails(String message, String dateTime) {
			_message = message;
			_dateTime = dateTime;
		}

	}

	// a single controls value
	public static class FormControlValue {

		// instance variables
		private String _id, _value;
		private boolean _hidden;

		// properties
		public String getId() { return _id; }
		public String getValue() { return _value; }
		public void setValue(String value) { _value = value;	}
		public Boolean getHidden() { return _hidden; }
		public void setHidden( boolean hidden) { _hidden = hidden; 	}

		// constructors
		public FormControlValue(String id, String value, boolean hidden) {
			_id = id;
			_value = value;
			_hidden = hidden;
		}

		public FormControlValue(String id, String value) {
			_id = id;
			_value = value;
		}

		// override

		@Override
		public String toString() {
			return _id + "=" + _value + (_hidden ? " (hidden)"  : "");
		}

	}

	// a pages control values
	public static class FormPageControlValues extends ArrayList<FormControlValue> {

		// constructor

		public FormPageControlValues(FormControlValue... controlValues) {
			if (controlValues != null) {
				for (FormControlValue controlValue : controlValues) {
					this.add(controlValue);
				}
			}
		}

		// methods

		public void add(String controlId, String controlValue, boolean hidden) {
			this.add(new FormControlValue(controlId, controlValue, hidden));
		}

		public void add(String controlId, String controlValue) {
			this.add(new FormControlValue(controlId, controlValue));
		}

		public FormControlValue get(String controlId) {
			for (FormControlValue controlValue : this) {
				if (controlId.equals(controlValue.getId())) return controlValue;
			}
			return null;
		}

		public String getValue(String controlId) {
			for (FormControlValue controlValue : this) {
				if (controlId.equals(controlValue.getId())) return controlValue.getValue();
			}
			return null;
		}

	}

	// this exception class can be extended for more meaningful exceptions that may occur within the adapters
	public static class ServerSideValidationException extends Exception {

		private String _message;
		private Throwable _cause;

		public ServerSideValidationException(String message) {
			_message = message;
		}

		public ServerSideValidationException(String message, Throwable cause) {
			_message = message;
			_cause = cause;
		}

		@Override
		public String getMessage() {
			return _message;
		}

		@Override
		public Throwable getCause() {
			if (_cause == null) return super.getCause();
			return _cause;
		}

		@Override
		public StackTraceElement[] getStackTrace() {
			if (_cause == null) return super.getStackTrace();
			return _cause.getStackTrace();
		}

	}

	//  static finals
	private static final String USER_FORM_DETAILS = "userFormDetails";
	private static final String USER_FORMS_SUBMITTED = "userFormsSubmitted";
	private static final String USER_FORM_ID = "userFormId";

	// static variables

	protected static Logger _logger;

	// instance variables

	protected String _type, _css, _submittedPage, _errorPage;
	protected ServletContext _servletContext;
	protected Application _application;
	protected PaymentGateway _paymentGateway;
	protected Map<String,String> _ControlParametersMap;

	// properties

	public ServletContext getServletContext() { return _servletContext; }
	public Application getApplication() { return _application; }
	public PaymentGateway getPaymentGateway() { return _paymentGateway; }
	public String getType() { return _type; }
	public String getSubmittedPage() { return _submittedPage; }
	public String getErrorPage() { return _errorPage; }

	// constructor

	public FormAdapter(ServletContext servletContext, Application application, String type) {
		_servletContext = servletContext;
		_application = application;
		_type = type;
		_logger = LogManager.getLogger(FormAdapter.class);
		// get the array of jsonFormAdapters from the servletContext - this is so we don't need to update the constructor on all FormAdpters out there in the wild
		JSONArray jsonFormAdapters = (JSONArray) servletContext.getAttribute("jsonFormAdapters");
		// loop them
		for (int i = 0; i < jsonFormAdapters.length(); i ++) {
			try {
				// get an adapter
				JSONObject jsonFormAdapter = jsonFormAdapters.getJSONObject(i);
				// if this is us
				if (_type.equals(jsonFormAdapter.getString("type"))) {
					// look for a paymentGateway class
					String paymentClass = jsonFormAdapter.optString("paymentClass", null);
					// if we got one
					if (paymentClass != null) {
						// get the payment constructors
						Map<String, Constructor> paymentConstructors = (Map<String, Constructor>) servletContext.getAttribute("paymentConstructors");
						try {
							// get its contructor
							Constructor constructor = paymentConstructors.get(paymentClass);
							// instantiate it
							_paymentGateway = (PaymentGateway) constructor.newInstance(servletContext, application);
						} catch (Exception ex) {
							// log
							_logger.error("Error instantiating payment gateway " + paymentClass + " for form " + _type);
						}
					}
					// store submitted and erorr pages that we get in the json (from the adapter.xml file)
					_submittedPage = jsonFormAdapter.optString("submittedPage", null);
					_errorPage = jsonFormAdapter.optString("errorPage", null);
					// we're done
					break;
				}
			} catch (JSONException ex) {
				_logger.error("Error loading form adapter", ex);
			}
		}

	}

	// private instance methods

	private String getFormMapKey(RapidRequest rapidRequest) {
		// get the application
		Application application = rapidRequest.getApplication();
		// return the key
		return application.getId() + "-" + application.getVersion();
	}

	// abstract methods

	// this method returns a new form id, when allowed, by a given adapter, could be in memory, or database, etc
	public abstract UserFormDetails getNewFormDetails(RapidRequest rapidRequest) throws Exception;

	// this method checks a form id against a password for resuming
	public abstract UserFormDetails getResumeFormDetails(RapidRequest rapidRequest, String formId, String password) throws Exception;

	// sets the maximum page id the user is allowed to see
	public abstract void setMaxPage(RapidRequest rapidRequest, UserFormDetails formDetails, String pageId) throws Exception;

	// sets that a form has been completed (and we can show the submit button on the summary)
	public abstract void setFormComplete(RapidRequest rapidRequest, UserFormDetails formDetails) throws Exception;

	// gets any page/session variables for this form
	public abstract Map<String,String> getFormPageVariableValues(RapidRequest rapidRequest, String formId) throws Exception;

	// sets any page/session variables for this form
	public abstract void setFormPageVariableValue(RapidRequest rapidRequest, String formId, String name, String value) throws Exception;

	// returns all the form control values for a given page
	public abstract FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId) throws Exception;

	// sets all the form control values for a given page - pages that fail the isVisible method will be sent  a null pageControlValues
	public abstract void setFormPageControlValues(RapidRequest rapidRequest, String formId, String pageId, FormPageControlValues pageControlValues) throws Exception;

	// gets the value of a form control value
	public abstract String getFormControlValue(RapidRequest rapidRequest, String formId, String controlId, boolean notHidden) throws Exception;

	// submits the form and receives a message for the submitted page
	protected abstract SubmissionDetails submitForm(RapidRequest rapidRequest) throws Exception;

	// gets the submission date for later use
	protected abstract String getFormSubmittedDate(RapidRequest rapidRequest, String formId) throws Exception;

	// closes any resources used by the form adapter when the server shuts down
	public abstract void close() throws Exception;

	// protected instance methods

	// overload for the above abstract method
	public String getFormControlValue(RapidRequest rapidRequest, String formId, String controlId) throws Exception {
		// default notHidden to false so the standard behaviour is to return the value whether hidden or not
		return getFormControlValue(rapidRequest, formId, controlId, false);
	}

	// some controls need some further processing to get a summary value, also used in the pdf
	protected String getSummaryControlValue(Application application, Control control, FormControlValue controlValue, String nullValue) {

		// get the value
		String value = controlValue.getValue();

		// check for nulls
		if (value == null) {
			//show the label and no value
			value = control.getLabel() + " : " + nullValue;
		} else {

			// check for json
			if (value.startsWith("{") && value.endsWith("}")) {
				try {
					JSONObject jsonValue = new JSONObject(value);
					value = jsonValue.optString("text");
				} catch (Exception ex) {}
			}

			// get the type
			String type = control.getType();

			// check for special controls
			if (type.contains("checkbox")) {
				// just show the label
				value = control.getLabel();
			} else {
				// show the label and value
				value = control.getLabel() + " : " + control.getCodeText(_application, value);
			}

		}
		return control.getCodeText(application, value);

	}

	// the start of the form summary	page
	protected String getSummaryStartHtml(RapidRequest rapidRequest, Application application, boolean email) {
		// if this is the email use the title
		if (email) {
			return "<h1 class='formSummaryTitle'>" + application.getTitle() + " summary</h1>\n";
		} else {
			// assume no theme header
			String themeHeader = "";
			// get the theme
			Theme theme = application.getTheme(getServletContext());
			// check we got one
			if (theme != null && theme.getHeaderHtml() != null) themeHeader = theme.getHeaderHtml();
			// return theme header and summary
			return themeHeader + "<h1 class='formSummaryTitle'>Form summary</h1>\n";
		}
	}

	// the end of the form summary page
	protected String getSummaryEndHtml(RapidRequest rapidRequest, Application application, boolean email) {
		// check if email
		if (email) {
			// no theme footer on email
			return "";
		} else {
			// assume no theme footer
			String themeFooter = "";
			// get the theme
			Theme theme = application.getTheme(getServletContext());
			// check we got a theme and it has a footer
			if (theme != null && theme.getFooterHtml() != null) themeFooter = theme.getFooterHtml();
			// return theme footer
			return themeFooter;

		}
	}


	// the start of a page block in the form summary
	protected String getSummaryPageStartHtml(RapidRequest rapidRequest, Application application, Page page, boolean email) {
		String label = page.getLabel();
		if (label == null) {
			label = page.getTitle();
		} else {
			if (label.trim().length() == 0) label = page.getTitle();
		}
		return "<div class='formSummaryPage'><h2>" + label + "</h2>\n";
	}

	// the edit or view link for the summary page
	protected String getSummaryPageLinkHtml(Page page, String pageReturn) {
		return "<a href='~?a=" + _application.getId() + "&v=" + _application.getVersion() + "&p=" + page.getId() + "'>" + pageReturn + "</a>\n";
	}

	// get the default form payment button
	protected String getFormPaymentButtonHtml(RapidRequest rapidRequest) throws Exception {
		return "<form action='~?a=" + _application.getId() + "&v=" + _application.getVersion() + "&action=pay' method='POST'>\n<input type='hidden' name='csrfToken' value='" + rapidRequest.getCSRFToken() + "' />\n<button type='submit' class='formSummarySubmit'>Pay</button>\n</form>\n";
	}

	// get the default form submit button
	protected String getFormSubmitButtonHtml(RapidRequest rapidRequest) throws Exception {

		// default Submit button
		String html = "<form action='~?a=" + _application.getId() + "&v=" + _application.getVersion()  + "&action=submit' method='POST'>\n<input type='hidden' name='csrfToken' value='" + rapidRequest.getCSRFToken() + "' />\n<button type='submit' class='formSummarySubmit'>Submit</button>\n</form>\n";

		// if no payment
		if (_paymentGateway == null) {

			// return simple submit straight away
			return html;

		} else {

			// get the form details
			UserFormDetails formDetails = getUserFormDetails(rapidRequest);

			// if payment has started
			if (formDetails.getPaymentStarted()) {

				// assume there was an error getting the status
				int status = PaymentGateway.PAYMENT_ERROR;

				try {

					// get the status
					status = _paymentGateway.getPaymentStatus(rapidRequest);

				} catch (Exception ex) {

					// log error
					_logger.error("Error getting payment status : " + ex.getMessage(), ex);

				}

				// if complete
				if (status != PaymentGateway.PAYMENT_SUCCESS) {

					// get the pay button html
					html = getFormPaymentButtonHtml(rapidRequest);
					// add the feedback
					html += "<p class='paymentMessage'>" + _paymentGateway.getPaymentStatusMessage(rapidRequest, status) + "</p>";

				}

			} else {

				// a payment button without feedback
				html = getFormPaymentButtonHtml(rapidRequest);

			}

			return html;

		}

	}

	// the end of a page block in the form summary
	protected String getSummaryPageEndHtml(RapidRequest rapidRequest, Application application, Page page, boolean email) {
		return "</div>\n";
	}

	// a page control's value in the form summary
	protected String getSummaryControlValueHtml(RapidRequest rapidRequest, Application application, Page page, FormControlValue controlValue, boolean email) {

		if (controlValue.getHidden()) {
			return "";
		} else {
			Control control = page.getControl(controlValue.getId());
			if (control == null) {
				return "control " + controlValue.getId() + " cannot be found";
			} else {
				String label = control.getLabel();
				if (label == null) {
					return "";
				} else {
					// if this control is a grid
					if ("grid".equals(control.getType())) {
						// grid summary html is created specially
						return getSummaryGridHtml(application, control, controlValue);
					} else {
						// otherwise use the conventional way of getting the html and value
						String value = getSummaryControlValue(application, control, controlValue, "(no value)");
						return "<span class='formSummaryControl'>" + Html.escape(value) + "</span><br/>\n";
					}
				}
			}
		}
	}

	protected String getSummaryGridHtml(Application application, Control control, FormControlValue controlValue) {

		// Start of the summary grid table
		String gridTable = "<span class='formSummaryControl'>" + Html.escape(control.getLabel()) + " : </span>\n";

		try {

			// check we have a controlValue and its value is non-null
			if (controlValue != null && controlValue.getValue() != null) {

				// get the json data from the control
				JSONObject jsonData = new JSONObject(controlValue.getValue());

				// get the fields jsonArray
				JSONArray fields = jsonData.getJSONArray("fields");
				// get all the properties for all the fields
				JSONArray columnsProperties = new JSONArray(control.getProperty("columns"));

				// a list to store the visible fields
				List<String> visibleFields = new ArrayList<>();

				// start the grid
				gridTable += "<table><tr>\n";

				// loop through all the fields, to populate the table headers
				for (int i = 0; i < fields.length(); i++){
					// a field
					String field = fields.getString(i);
					// check if we have 'columns' key
					if (columnsProperties != null) {
						// loop through the columnsProperties
						for (int c = 0; c < columnsProperties.length(); c++){
							// for each column, get its properties
							JSONObject column_properties = columnsProperties.getJSONObject(c);

							// find the properties for 'this' field and make sure its visible
							if (field.equals(column_properties.getString("field")) && column_properties.getBoolean("visible")){
								// create the header tag for this field
								gridTable += "<th>" + column_properties.getString("title") + "</th>\n";
								// store the visible field in a list
								visibleFields.add(field);
								// we're done
								break;
							}

						} // end of inner loop

					} // check for 'columns' key

				} // end of outer loop

				// close the tr tag
				gridTable += "</tr>\n";

				// now loop through the rows, to populate the table data
				JSONArray rows = jsonData.getJSONArray("rows");
				for (int i = 0; i < rows.length(); i++){
					// open a new row tag
					gridTable += "<tr>\n";
					JSONArray row = rows.getJSONArray(i);

					// for each row, loop through the row cells
					for (int j = 0; j < row.length(); j++){
						String field = fields.getString(j);

						// if this field is a visible field
						if(visibleFields.contains(field)){
							// get and create its column data table
							gridTable += "<td>" + row.getString(j) + "</td>\n";
						}

					}// end of inner loop

					gridTable += "</tr>\n";

				}// end of outer loop

				// close the table tag
				gridTable += "</table><br/>\n";

			}

		} catch (Exception ex) {
			// log error
			_logger.error("Error creating the grid summary data", ex);
			// return the error message
			return "Error creating the grid summary data : " + ex.getMessage() + "<br/>\n";
		}

		// return the html table
		return gridTable;
	}

	// the end of the page block
	protected String getSummaryPagesEndHtml(RapidRequest rapidRequest, Application application, boolean email) {
		return "";
	}

	// return a forms CSV as a string (for attaching or saving to file)
	protected String getFormCSV(RapidRequest rapidRequest, String formId) throws Exception {

		// the string builder we'll make the attachment with
		StringBuilder sb = new StringBuilder();

		// create the header line
		sb.append("\"page id\",\"control id\",\"name\",\"label\",\"value\",\"hidden\"\n");

		// loop the page ids
		for (String pageId : _application.getPages().getPageIds()) {

			// get the page values
			FormPageControlValues pageControlValues = getFormPageControlValues(rapidRequest, formId, pageId);

			// if we got some
			if (pageControlValues != null) {

				// loop them
				for (FormControlValue pageControlValue : pageControlValues) {

					// get the control
					Control control = _application.getControl(getServletContext(), pageControlValue.getId());
					// if we got one
					if (control != null) {

						// get the user -defined values
						String name = control.getName();
						String label = control.getLabel();
						String value = pageControlValue.getValue();

						// quote enclose and escape if not null
						if (name != null) name = "\"" + name.replace("\"", "\"\"") + "\"";
						if (label != null) label = "\"" + label.replace("\"", "\"\"") + "\"";
						if (value != null) value = "\"" + value.replace("\"", "\"\"") + "\"";

						// create the line for the value
						sb.append(pageId + "," + control.getId() + "," + name + "," + label + "," + value + "," + pageControlValue.getHidden() + "\n");

					} // control null check

				} // page control values loop

			} //page control values null check

		} //  page id loop

		// return
		return sb.toString();

	}

	// return a forms XML as a string (for attaching or saving to file)
	protected String getFormXML(RapidRequest rapidRequest, String formId) throws Exception {

		// the string builder we'll make the attachment with
		StringBuilder sb = new StringBuilder();

		// create the root element
		sb.append("<form>\n\t<id>" + formId + "</id>\n");

		// loop the page ids
		for (String pageId : _application.getPages().getPageIds()) {

			// get the page values
			FormPageControlValues pageControlValues = getFormPageControlValues(rapidRequest, formId, pageId);

			// if we got some
			if (pageControlValues != null) {

				// open a controls array
				sb.append("\t<controls>\n");

				// loop them
				for (FormControlValue pageControlValue : pageControlValues) {

					// get the control
					Control control = _application.getControl(getServletContext(), pageControlValue.getId());
					// if we got one
					if (control != null) {

						// get the user -defined values
						String name = control.getName();
						String label = control.getLabel();
						String value = pageControlValue.getValue();

						// quote enclose and escape if not null
						if (name != null) name = XML.escape(name);
						if (label != null) label = XML.escape(label);
						if (value != null) value = XML.escape(value);

						// create the line for the value
						sb.append("\t\t<control id=\"" + control.getId() + "\" name=\"" + name + "\" label=\"" + label + "\">" + value + "</control>\n");

					} // control null check

				} // page control values loop

				// close controls array
				sb.append("\t</controls>\n");

			} //page control values null check

		} //  page id loop

		// close the root element
		sb.append("</form>");

		// return
		return sb.toString();

	}

	// return a forms CSV as a string (for attaching or saving to file)
	protected String getFormJSON(RapidRequest rapidRequest, String formId) throws Exception {

		// create the object
		JSONObject jsonForm = new JSONObject();

		// add the id
		jsonForm.put("id", formId);

		// create the controls array
		JSONArray jsonControls = new JSONArray();

		// loop the page ids
		for (String pageId : _application.getPages().getPageIds()) {

			// get the page values
			FormPageControlValues pageControlValues = getFormPageControlValues(rapidRequest, formId, pageId);

			// if we got some
			if (pageControlValues != null) {

				// loop them
				for (FormControlValue pageControlValue : pageControlValues) {

					// get the control
					Control control = _application.getControl(getServletContext(), pageControlValue.getId());
					// if we got one
					if (control != null) {

						// create a json object for the controls
						JSONObject jsonControl = new JSONObject();

						// add the control details
						jsonControl.put("pageId", pageId);
						jsonControl.put("id", control.getId());
						jsonControl.put("name", control.getName());
						jsonControl.put("label", control.getLabel());
						jsonControl.put("value", pageControlValue.getValue());
						jsonControl.put("hidden", pageControlValue.getHidden());

						// add to controls
						jsonControls.put(jsonControl);

					} // control null check

				} // page control values loop

			} //page control values null check

		} //  page id loop

		// add the controls
		jsonForm.put("controls", jsonControls);

		// return
		return jsonForm.toString();

	}

	// this returns the .pdf file name
	protected String getFormFileName(RapidRequest rapidRequest, String formId, String extenstion, boolean email) {
		return _application.getName() + formId + "." + extenstion;
	}

	// this returns the input stream for the attachment file
	protected Attachment getEmailAttachment(RapidRequest rapidRequest, String attachmentType, String formId) throws Exception {

		// get the file name
		String fileName = getFormFileName(rapidRequest, formId, attachmentType, true);

		if("csv".equals(attachmentType)) {

			// return the csv attachment
			return new Attachment(fileName, new StringDataSource("text/csv", getFormCSV(rapidRequest, formId)));

		} else 	if("xml".equals(attachmentType)) {

			// return the xml attachment
			return new Attachment(fileName, new StringDataSource("text/xml", getFormXML(rapidRequest, formId)));

		} else 	if("pdf".equals(attachmentType)) {

			// get an in-memory output stream
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			// write the pdf
			writeFormPDF(rapidRequest, outputStream, formId, true);

			// return the pdf attachment with a ByteArrayDataSource
			return new Attachment(fileName, new ByteArrayDataSource(outputStream.toByteArray(), "application/pdf"));

		} else {

			return null;

		}

	}

	// this returns the form email subject and can be overridden if need be
	protected String getEmailSubject(RapidRequest rapidRequest, String formId) { return _application.getTitle() + " " + formId + " submitted";	}

	// saves the form file to the file system
	public void saveFormFile(RapidRequest rapidRequest, String formId) throws Exception {

		// get the form path
		String path = _application.getFormFilePath();

		// add a closing / if need be
		if (!path.endsWith("/") && !path.endsWith("\\")) path += "/";

		// get the fileType
		String fileType = _application.getFormFileType();

		// if empty or null go for csv
		if (fileType == null) fileType = "csv";
		if (fileType.length() == 0) fileType = "csv";

		// get the file name
		String fileName = getFormFileName(rapidRequest, formId, fileType, false);

		// check for a network user
		String user = _application.getFormFileUserName();
		// if null update to empty string
		if (user == null) user = "";

		// if there's a network user
		if (user.length() > 0) {

			// if there is a domain - replace with ;
			user = user.replace("\\", ";");

			// add the file name to the path
			path += fileName;

			// check the type
			if ("csv".equals(fileType)) {

				// get the file and save the network way
				CIFS.saveFile(user, _application.getFormFilePassword(), path, getFormCSV(rapidRequest, formId));

			} else if ("xml".equals(fileType)) {

				CIFS.saveFile(user, _application.getFormFilePassword(), path, getFormXML(rapidRequest, formId));

			} else if ("pdf".equals(fileType)) {

			}  // file type for network save

		} else {

			// get the form file
			File formFile = new File(path + fileName);

			// make any directories
			formFile.getParentFile().mkdirs();

			// check the type
			if ("csv".equals(fileType)) {

				// get the file and save the simple way
				Strings.saveString(getFormCSV(rapidRequest, formId), formFile);

			} else if ("xml".equals(fileType)) {

				// get the file and save the simple way
				Strings.saveString(getFormXML(rapidRequest, formId), formFile);

			} else if ("pdf".equals(fileType)) {

				// open a stream for the file we're writing to
				OutputStream outputStream = new FileOutputStream(formFile);

				// write the file
				writeFormPDF(rapidRequest, outputStream, formId, false);

				// close the stream
				outputStream.close();


			}  // file type check local save

		} // network check

	}

	public void sendFormWebservice(RapidRequest rapidRequest, String formId) throws Exception {

		// get the data type
		String dataType = _application.getFormWebserviceType();

		if ("json".equals(dataType)) {

			// POST the JSON
			Http.post(_application.getFormWebserviceURL(), getFormJSON(rapidRequest, formId));

		} else if ("restful".equals(dataType)) {

			// POST the XML
			Http.post(_application.getFormWebserviceURL(), getFormXML(rapidRequest, formId));

		} else {

			// Wrap the XML in SOAP
			String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n\t<soapenv:Body>\n" + getFormXML(rapidRequest, formId).replace("<form", "<form xmlns=\"http://www.rapid-is.co.uk\"") + "\n\t</soapenv:Body>\n</soapenv:Envelope>";

			// POST it
			Http.postSOAP(_application.getFormWebserviceURL(), _application.getFormWebserviceSOAPAction(), xml);

		}

	}


	// this gets the list of submitted forms from the users session
	protected List<String> getSubmittedForms(RapidRequest rapidRequest) {
		// first try and get from the session
		List<String> submittedForms = (List<String>) rapidRequest.getSessionAttribute(USER_FORMS_SUBMITTED);
		// if this is null
		if (submittedForms == null) {
			// make a new one
			submittedForms = new ArrayList<>();
			// store it
			rapidRequest.getRequest().getSession().setAttribute(USER_FORMS_SUBMITTED, submittedForms);
		}
		// return
		return submittedForms;
	}

	// this adds a form id to the submitted list
	protected void addSubmittedForm(RapidRequest rapidRequest, String formId) {
		// get  the list
		List<String> submittedForms = getSubmittedForms(rapidRequest);
		// if not there already
		if (!submittedForms.contains(formId)) {
			// add it
			submittedForms.add(formId);
			// store it
			rapidRequest.getRequest().getSession().setAttribute(USER_FORMS_SUBMITTED, submittedForms);
		}

	}

	// public instance methods

	// sets whether the form has been saved
	public synchronized void setUserFormSaved(RapidRequest rapidRequest, boolean saved) throws Exception {
		// get the details
		UserFormDetails details = getUserFormDetails(rapidRequest);
		// update if we got some
		if (details != null) details.setSaved(saved);
	}

	// returns the form id in the user session for a given application id and version
	public synchronized UserFormDetails getUserFormDetails(RapidRequest rapidRequest) throws Exception {
		// get the user session (without making a new one)
		HttpSession session = rapidRequest.getRequest().getSession(false);
		// check we got one
		if (session == null) {
			// no form details to return
			return null;
		} else {
			// get the form ids map from the session
			Map<String,UserFormDetails> allFormDetails = (Map<String, UserFormDetails>) session.getAttribute(USER_FORM_DETAILS);
			// if null
			if (allFormDetails == null) {
				// log
				_logger.trace("Creating user session form details store for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr());
				// make some
				allFormDetails = new HashMap<>();
				// add to session
				session.setAttribute(USER_FORM_DETAILS, allFormDetails);
			}
			// get the form key
			String formKey = getFormMapKey(rapidRequest);
			// get the details for this form
			UserFormDetails formDetails = allFormDetails.get(formKey);
			// check we got some
			if (formDetails == null) {
				// log
				_logger.trace("No form details of " + formKey + " for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr());
				// get the application
				Application application = rapidRequest.getApplication();
				// assume no start page
				String startPageId = "";
				// if there are pages
				if (application.getPages() != null) {
					// get the id of the first one
					if (application.getPages().size() > 0) startPageId = application.getStartPage(getServletContext()).getId();
				}
				// get the requested Page
				Page requestPage = rapidRequest.getPage();
				// get the request page id
				String requestPageId = null;
				// if there was a page get the id
				if (requestPage != null) requestPageId = requestPage.getId();
				// get the action
				String action = rapidRequest.getRequest().getParameter("action");
				// assume no new id allowed
				boolean newFormAllowed = false;
				// if this is the start page with no action other than dialogue
				if  (startPageId.equals(requestPageId) && (action == null || "dialogue".equals(action))) {
					// we're ok to request new form details
					newFormAllowed = true;
					// log
					_logger.trace("New form allowed");
				} else {
					// get the security adapter
					SecurityAdapter security = application.getSecurityAdapter();
					// if the user has design
					try {
						if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) {
							// we're ok to request new form details
							newFormAllowed = true;
							// log
							_logger.trace("New form allowed for designer");
						}
					} catch (SecurityAdapaterException e) {}
				}

				// there are some rules for creating new form ids - there must be no action and the page must be the start page
				if (newFormAllowed) {
					// get a new form details from the adapter
					formDetails = getNewFormDetails(rapidRequest);
					// set the new user form details
					setUserFormDetails(rapidRequest, formDetails);
					// store the form id in the session - THIS IS A TEMPORARY MEASURE TO ENSURE USER FORM DETAILS ARE NOT CROSSING OVER TO OTHER USERS !!!!!!!!!!!
					rapidRequest.getRequest().getSession(false).setAttribute(USER_FORM_ID, formDetails.getId());
					// log
					_logger.trace("New form details requested, form id is " + formDetails.getId());
				}
			} else {
				// log
				_logger.trace("Form details retrived for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr() + ", form id is " + formDetails.getId());
				try {
					// get the session form id
					String formId = (String) rapidRequest.getRequest().getSession(false).getAttribute(USER_FORM_ID);
					// check it
					if (formId == null) {
						throw new Exception("Form session id has not been set for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr() + ", but form details object id is " + formDetails.getId());
					} else {
						// compare them
						if (!formId.equals(formDetails.getId())) {
							// they're different so check for designer security before throwing an exception, assume not designer
							boolean isDesigner = false;
							// get the application
							Application app = rapidRequest.getApplication();
							// check it
							if (app != null) {
								// get it's security
								SecurityAdapter security = app.getSecurityAdapter();
								// check we got one
								if (security != null) {
									// check the role (fail silently)
									try {
										if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) isDesigner = true;
									} catch (Exception ex) {}
								}
							}
							// throw the exception if not the designer
							if (!isDesigner) throw new Exception("Form session id mismatch for user " + rapidRequest.getUserName() + " from " + rapidRequest.getRequest().getRemoteAddr() + ", form session id is " + formId + " but form details object id is " + formDetails.getId());
						}
					}
				} catch (Exception ex) {
					// log
					_logger.error("Error checking session form id and form details ", ex);
					// empty the session - THIS IS A TEMPORARY MEASURE TO ENSURE USER FORM DETAILS ARE NOT CROSSING OVER TO OTHER USERS !!!!!!!!!!!
					rapidRequest.getRequest().getSession().invalidate();
					// set the form details to null - THIS IS A TEMPORARY MEASURE TO ENSURE USER FORM DETAILS ARE NOT CROSSING OVER TO OTHER USERS !!!!!!!!!!!
					formDetails = null;
				}

			}
			// return the user form details
			return formDetails;
		}
	}

	// sets the form details in the user session for a given application id and version
	public synchronized void setUserFormDetails(RapidRequest rapidRequest, UserFormDetails details) {
		// get the user session (making a new one if need be)
		HttpSession session = rapidRequest.getRequest().getSession();
		// get all user form details
		Map<String,UserFormDetails> allDetails = (Map<String, UserFormDetails>) session.getAttribute(USER_FORM_DETAILS);
		// make some if we didn't get
		if (allDetails == null) allDetails = new HashMap<>();
		// store the form if for a given app id / version
		allDetails.put(getFormMapKey(rapidRequest), details);
		// put the updated forms details back in the session
		session.setAttribute(USER_FORM_DETAILS, allDetails);
		// if we were given details
		if (details == null) {
			// empty the new form id in the session
			session.setAttribute(USER_FORM_ID, null);
		} else {
			// put the new form id in the session
			session.setAttribute(USER_FORM_ID, details.getId());
		}
	}

	// sets the form details in the user session for a given application id and version
	public synchronized void emptyUserFormDetails(RapidRequest rapidRequest) {
		// get the user session (making a new one if need be)
		HttpSession session = rapidRequest.getRequest().getSession();
		// empty the new form id in the session
		session.setAttribute(USER_FORM_ID, null);
		// empty all user form details
		session.setAttribute(USER_FORM_DETAILS, null);
	}

	// a helper method to get the form id via the details
	public synchronized String getFormId(RapidRequest rapidRequest) throws Exception {
		// get the user form details
		UserFormDetails formDetails = getUserFormDetails(rapidRequest);
		// check we got some
		if (formDetails == null) {
			return null;
		} else {
			return formDetails.getId();
		}
	}

	// checks a given page id against the maximum
	public synchronized boolean checkMaxPage(RapidRequest rapidRequest, UserFormDetails formDetails, String pageId) throws Exception {
		// assume not completed
		boolean check = false;
		// get the application
		Application application = rapidRequest.getApplication();
		// check we got one
		if (formDetails != null && application != null) {
			// get the sorted pages
			PageHeaders pages = application.getPages().getSortedPages();
			// get a scaler
			String maxPageId = formDetails.getMaxPageId();
			// check we got something
			if (maxPageId == null) {
				// fine if the first or start page
				if (pageId.equals(pages.get(0).getId()) || pageId.equals(application.getStartPageId())) {
					// we're allowed
					check = true;
					// update max page
					setMaxPage(rapidRequest, formDetails, pageId);
				}
			} else {
				// check we got some pages
				if (pages != null) {
					if (pages.size() > 0) {
						// get the position of the maxPage
						int maxPageIndex = pages.indexOf(maxPageId);
						// get the position of this page
						int pageIndex = pages.indexOf(pageId);
						// if we're allowed at this point
						if (pageIndex <= maxPageIndex) check = true;
					}
				}
			}
		}
		// return
		return check;
	}

	// this writes the form summary HTML to a writer (used by both the summary method above and the email submit)
	public void writeFormSummaryHTML(RapidRequest rapidRequest, UserFormDetails formDetails, Writer writer, Boolean email) throws Exception {

		// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
		writer.write("<!DOCTYPE html>\n");

		// open the html
		writer.write("<html>\n");

		// open the head
		writer.write("  <head>\n");

		// write a title
		writer.write("    <title>Form summary - by Rapid</title>\n");

		// write responsive header
		writer.write("    <meta description=\"Created using Rapid - www.rapid-is.co.uk\"/>\n" +
				"    <meta charset=\"utf-8\"/>\n" +
				"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />\n" +
				(_application != null ? "    <meta name=\"theme-color\" content=\"" + _application.getStatusBarColour() + "\" />\n" : "" )+
				"    <link rel=\"icon\" href=\"favicon.ico\"></link>\n");

		// get the servletContext
		ServletContext servletContext = this.getServletContext();

		// get app start page
		Page startPage = _application.getStartPage(servletContext);

		// if this is for an email
		if (email) {

			// if the css has not been set yet
			if (_css == null) {
				// get the minified file
				File minifiedCssFile = new File(_application.getWebFolder(servletContext) + "/rapid.min.css");
				// if it exists read it
				if (minifiedCssFile.exists()) _css = Strings.getString(minifiedCssFile);
			}

			// add the application stylesheet
			writer.write("    <style>\n" + _css + "\n    </style>\n");

		} else {

			// write the start page head (and it's resources)
			writer.write(startPage.getResourcesHtml(_application, true));

			// add code to only submit once, updates all forms on the page to return false after first submit
			writer.write("    <script type='text/javascript'>\n" +
				"$(document).ready(function() {\n" +
				"  $('form').submit(function() {\n" +
				"    $('form').submit(function() {\n" +
				"      return false;\n" +
				"    });\n" +
				"    return true;\n" +
				"  });\n" +
				"});\n" +
				"    </script>");

		}

		// close the head
		writer.write("  </head>\n");

		// open the body
		writer.write("  <body>\n");

		// write the summary start
		writer.write(getSummaryStartHtml(rapidRequest, _application, email));

		// get the sorted pages
		PageHeaders pageHeaders = _application.getPages().getSortedPages();

		// assume the page return link is edit
		String pageReturn = "edit";
		// update to view if submitted
		if (formDetails.getSubmitted()) pageReturn = "view";

		// loop the page headers
		for (PageHeader pageHeader : pageHeaders) {

			// a string builder for the page values
			StringBuilder valuesStringBuilder = new StringBuilder();

			// get any page control values
			FormPageControlValues pageControlValues = _application.getFormAdapter().getFormPageControlValues(rapidRequest, formDetails.getId(), pageHeader.getId());

			// if non null
			if (pageControlValues != null) {

				// if we got some
				if (pageControlValues.size() > 0) {

					// get the page with this id
					Page page = _application.getPages().getPage(servletContext, pageHeader.getId());

					// get all page controls (in display order)
					List<Control> pageControls = page.getAllControls();

					// loop the page controls
					for (Control control : pageControls) {

						// loop the page control values
						for (FormControlValue controlValue : pageControlValues) {

							// look for an id match
							if (control.getId().equals(controlValue.getId())) {

								// write the control value!
								valuesStringBuilder.append(getSummaryControlValueHtml(rapidRequest, _application, page, controlValue, email));

								// exit this loop
								break;

							}

						}

						// if there are no controlValues left we can stop entirely
						if (pageControlValues.size() == 0) break;

					} // page control loop

					// if there are some values in the string builder
					if (valuesStringBuilder.length() > 0) {

						// write the page start html
						writer.write(getSummaryPageStartHtml(rapidRequest, _application, page, email));

						// write the values
						writer.write(valuesStringBuilder.toString());

						// if not email write the edit link
						if (!email) writer.write(getSummaryPageLinkHtml(page, pageReturn));

						// write the page end html
						writer.write(getSummaryPageEndHtml(rapidRequest, _application, page, email));

					} // values written check

				} // control values length > 0

			} // control values non null

			// stop here if this is the max page that they got to
			if (pageHeader.getId().equals(formDetails.getMaxPageId())) break;

		} // page loop

		// write the pages end
		writer.write(getSummaryPagesEndHtml(rapidRequest, _application, email));

		// if the form has been completed and it not for email
		if (formDetails.getComplete() && !email) {
			// if it has been submitted
			if (formDetails.getSubmitted()) {
				// look for a submitted date/time
				String submittedDateTime = formDetails.getSubmittedDateTime();
				// add if we got one
				if (submittedDateTime != null) writer.write("<span class='formSubmittedDateTime'>" + submittedDateTime + "</span>");
			} else {
				// add the submit button
				writer.write(getFormSubmitButtonHtml(rapidRequest));
			}
		}

		// write the summary end
		writer.write(getSummaryEndHtml(rapidRequest, _application, email));

		// close the remaining elements
		writer.write("  </body>\n</html>");

	}

	// this writes the form summary page to the request
	public void writeFormSummary(RapidRequest rapidRequest, HttpServletResponse response) throws Exception {

		// get the user form details
		UserFormDetails formDetails = getUserFormDetails(rapidRequest);

		// check for form details - shouldn't ever be empty
		if (formDetails == null) {

			// send users back to the start if no form details
			Rapid.gotoStartPage(rapidRequest.getRequest(), response, _application, false);

		} else {

			// create a writer
			PrintWriter writer = response.getWriter();

			// set the response type
			response.setContentType("text/html");

			// write the html to the print writer
			writeFormSummaryHTML(rapidRequest, formDetails, writer, false);

			// close the writer
			writer.close();

			// flush the writer
			writer.flush();

		} // form id check

	}

	// processes the submitting of the form via the abstract submitForm method as well as generating emails and attachments
	public synchronized void doSubmitForm(RapidRequest rapidRequest) throws Exception {

		// get the form details
		UserFormDetails formDetails = getUserFormDetails(rapidRequest);

		// get the application
		Application application = rapidRequest.getApplication();

		try {

			// get the form Id
			String formId = formDetails.getId();

			// if submitted already throw exception
			if (formDetails.getSubmitted()) throw new Exception("This form has already been submitted");

			// perform any 3rd party submission first so if they fail the whole thing fails

			// webservice
			if (application.getFormWebservice()) sendFormWebservice(rapidRequest, formId);

			// get the submission details
			SubmissionDetails submissionDetails = submitForm(rapidRequest);

			// only email if 3rd party and internal submission did not fail
			if (application.getFormEmail()) {

				// get the email from
				String emailFrom = application.getFormEmailFrom();

				// get the back office email to
				String backOfficeEmailTo = application.getFormEmailTo();

				// keep the attachment here to avoid making it twice
				Attachment attachment = null;

				// if there was a back office email
				if (backOfficeEmailTo != null && backOfficeEmailTo.trim().length() > 0) {

					// get a string writer which the summary html will be written to
					StringWriter writer = new StringWriter();

					// write to the writer
					writeFormSummaryHTML(rapidRequest, formDetails, writer, true);

					// get the attachment (might not be one)
					if (application.getFormEmailAttachmentType().length() > 0) attachment = getEmailAttachment(rapidRequest, application.getFormEmailAttachmentType(), formId);

					// get upload control file names
					List<String> namesOfFilesToAttach = getAllControlValues(rapidRequest, formId, "upload");

					// get file data sources from list of file names
					List<Attachment> attachmentList = getFileAttachments(rapidRequest, namesOfFilesToAttach);

					// add the form summary attachment as the first in the list
					attachmentList.add(0,  attachment);

					// create an array from the list
					Attachment[] attachmentArray = attachmentList.toArray(new Attachment[attachmentList.size()]);

					// send the email
					Email.send(emailFrom, backOfficeEmailTo, getEmailSubject(rapidRequest, formId), "HTML preview not available", writer.toString(), attachmentArray);

				} // back office email check

				// get the customer email address control id
				String custorEmailToControlId = application.getFormEmailCustomerControlId();

				// if there is a customer email
				if (application.getFormEmailCustomer() && custorEmailToControlId != null) {

					// get the customer email address
					String custorEmailTo = this.getFormControlValue(rapidRequest, formId, custorEmailToControlId);

					// if we got one
					if (custorEmailTo != null && custorEmailTo.trim().length() > 0) {

						// get the subject
						String subject = application.getFormEmailCustomerSubject();

						// do the simple parameters here so we don't spend ages looking for them as control values
						subject = insertFormParameters(rapidRequest, formDetails, subject);

						// get the body
						String body = application.getFormEmailCustomerBody();

						// do the simple parameters here so we don't spend ages looking for them as control values
						body = insertFormParameters(rapidRequest, formDetails, body);

						// if parameters have not been scanned yet
						if (_ControlParametersMap == null) {

							// add subject parameters
							populateControlParametersMap(rapidRequest, subject);

							// add body parameters
							populateControlParametersMap(rapidRequest, body);

						}

						// insert parameter values
						subject = insertControlParameters(rapidRequest, formDetails, subject);

						// insert parameter values
						body = insertControlParameters(rapidRequest, formDetails, body);

						// if the customer email attachment is different from the back-office attachment
						if (!application.getFormEmailAttachmentType().equals(application.getFormEmailCustomerAttachmentType())) {
							// get the attachment for the customer
							attachment = getEmailAttachment(rapidRequest, application.getFormEmailCustomerAttachmentType(), formId);
						}

						// email type check
						if ("H".equals(application.getFormEmailCustomerType())) {

							// send the email
							Email.send(emailFrom, custorEmailTo, subject, "HTML preview not available", body, attachment);

						} else {

							// send the email
							Email.send(emailFrom, custorEmailTo, subject, body, null, attachment);

						}

					} // customer email address check

				} // customer email check

			} // email check

			// file
			if (application.getFormFile()) saveFormFile(rapidRequest, formId);

			// retain the submitted date/time in the details
			formDetails.setSubmittedDateTime(submissionDetails.getDateTime());
			// retain the submit message in the details
			formDetails.setSubmitMessage(submissionDetails.getMessage());
			// allow the submission page to be seen
			formDetails.setShowSubmitPage(true);

			// retain that this form was submitted
			addSubmittedForm(rapidRequest, formDetails.getId());

		} catch (Exception ex) {
			// get the error message
			String message = ex.getMessage();
			// set if null
			if (message == null) message = "Check the log for details";
			// if we had form details
			if (formDetails == null) {
				// log the error
				_logger.error("Error submitting form for "  + application.getId(), " - No formDetails! ", ex);
			} else {
				// log the error
				_logger.error("Error submitting form " + formDetails.getId() + " for "  + application.getId(), ex);
				// retain the error message in the details
				formDetails.setErrorMessage(message);
			}
			// rethrow
			throw ex;
		}
	}

	private List<Attachment> getFileAttachments(RapidRequest rapidRequest, List<String> fileNames) {
		List<Attachment> attachmentList = new ArrayList<>();

		String path = (rapidRequest.getRapidServlet().isPublic() ? "WEB-INF/" : "") + "uploads/"+_application.getId();
		path = getServletContext().getRealPath(path);

		for(String fileName : fileNames) {
			File file = new File(path+"/"+fileName);
			if(file.exists())
				attachmentList.add(new Attachment(fileName, new FileDataSource(file)));
		}

		return attachmentList;
	}

	// get a list of control values by their names
	public Map<String,String> getNamedControlValues(RapidRequest rapidRequest, String formId, String... controlNames) throws Exception {

		// the value
		Map<String, String> values = new HashMap<>();

		// get the application
		Application application = rapidRequest.getApplication();

		// get the pages
		Pages pages = application.getPages();

		// loop the pages
		for (String pageId : pages.getPageIds()) {

			// get the page values
			FormPageControlValues pageControlValues = getFormPageControlValues(rapidRequest, formId, pageId);

			// if there were controls with values on this page
			if (pageControlValues != null) {

				// if controls were named
				if (controlNames == null || controlNames.length == 0) {

					// loop the values
					for (FormControlValue pageControlValue : pageControlValues) {
						// get the control
						Control control = application.getControl(rapidRequest.getRapidServlet().getServletContext(), pageControlValue.getId());
						// if we got one
						if (control != null) {
							// add this to our values
							values.put(control.getName(), pageControlValue.getValue());
						}
					}

				} else {

					// loop the control names
					for (String name : controlNames) {
						// loop the values
						for (FormControlValue pageControlValue : pageControlValues) {
							// get the control
							Control control = application.getControl(rapidRequest.getRapidServlet().getServletContext(), pageControlValue.getId());
							// if we got one
							if (control != null) {
								// check the name
								if (name.equals(control.getName())) {
									// add this to our values
									values.put(name, pageControlValue.getValue());
									// remove from collection to check
									pageControlValues.remove(pageControlValue);
									// we're done with this loop
									break;
								}
							}
						}
					}
				}
			}

		}

		// return values
		return values;

	}

	// an overload to the above
	public Map<String,String> getNamedControlValues(RapidRequest rapidRequest, String... controlNames) throws Exception {
		// get the form id
		String formId = this.getFormId(rapidRequest);
		// check we got one
		if (formId == null) {
			return null;
		} else {
			return getNamedControlValues(rapidRequest, formId, controlNames);
		}
	}

	// returns all values of all the controls of the specified type
	private List<String> getAllControlValues(RapidRequest rapidRequest, String formId, String controlType) throws Exception {

		// this is where we will put the result
		List<String> valueList = new ArrayList<>();

		// get the contaxt
		ServletContext servletContext = this.getServletContext();

		// go through the page headers
		PageHeaders pageHeaders = _application.getPages().getSortedPages();
		for (PageHeader pageHeader : pageHeaders) {

			// get the page control values
			FormPageControlValues pageControlValues = _application.getFormAdapter().getFormPageControlValues(rapidRequest, formId, pageHeader.getId());
			if (pageControlValues != null && pageControlValues.size() > 0) {

				// get the pages
				Page page = _application.getPages().getPage(servletContext, pageHeader.getId());

				// get the controls from each page
				List<Control> pageControls = page.getAllControls();

				// go through all of the controls on the page
				for (Control control : pageControls) {
					for (FormControlValue controlValue : pageControlValues) {

						// if the control is of the correct type then add its values to the list
						if(controlType.equals(control.getType()) && control.getId().equals(controlValue.getId())) {
							String controlValueString = controlValue.getValue();
							if(controlValueString!=null) {
								String[] allValues = controlValueString.split(",");
								for(String value : allValues)
									valueList.add(value);
							}
						}
					}
				}
			}
		}

		// a string list of all the control values
		return valueList;
	}

	// this writes the form pdf to an Output stream
	protected float FONT_SIZE_HEADER1 = 14;
	protected float FONT_SIZE_HEADER2 = 12;
	protected float FONT_SIZE = 12;
	protected float MARGIN_LEFT = 20;
	protected float MARGIN_TOP = 20;
	protected float MARGIN_RIGHT = 20;
	protected float MARGIN_BOTTOM = 10;
	protected float MARGIN_HEADER_BOTTOM = 10;
	protected float MARGIN_SECTION_BOTTOM = 5;
	protected float MARGIN_TEXT_BOTTOM = 3;
	protected float MARGIN_GRID_COLUMN = 5;
	protected float MARGIN_NEXT_TOP = 20;
	protected float MARGIN_NEXT_BOTTOM = 20;

	protected float getFontHeight(PDFont font, float fontSize) {
		return (float) (font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize * 0.865);
	}

	protected String getAppFilePath(RapidRequest rapidRequest, String file) {
		// check for /
		if (file.contains("/")) {
			// update with context
			return rapidRequest.getRequest().getServletContext().getRealPath("/") + file;
		} else {
			// prefix with path for app
			return rapidRequest.getApplication().getWebFolder(rapidRequest.getRequest().getServletContext()) + "/" + file;
		}
	}

	protected File getPDFLogoFile(RapidRequest rapidRequest) {
		// assume we can't find the file
		File file = null;
		// get the application
		Application application = rapidRequest.getApplication();
		// if we got one
		if (application != null) {
			// assume no pdf file
			File pdfFile = null;
			// look for a form.pdf.logo parameter
			String logoParam = application.getParameterValue("form.pdf.logo");
			// if we got one
			if (logoParam != null) {
				// get a file for it
				pdfFile = new File(getAppFilePath(rapidRequest, logoParam));
			}
			// if we didn't get one look for the a pdfLogo image in the root images folder
			if (pdfFile == null) pdfFile = new File(application.getWebFolder(rapidRequest.getRapidServlet().getServletContext()) + "/images/pdfLogo.png");
			// if we got one, set it
			if (pdfFile != null && pdfFile.exists()) file = pdfFile;
		}
		return file;
	}

	// remove any non-printable characters
	public String cleanString(String string) {

		// clean the value to only contain printable characters
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			if (WinAnsiEncoding.INSTANCE.contains(string.charAt(i))) sb.append(string.charAt(i));
		}
		// update value to only printable characters
		return sb.toString();

	}

	// get's a parameter value from either the application or theme
	public String getParameterValue(ServletContext servletContext, Application application, String name) {

		String value = application.getParameterValue(name);
		if (value != null) return value;

		Theme theme = application.getTheme(servletContext);
		if (theme != null) value = theme.getParameterValue(name);

		return value;

	}

	public void writeFormPDF(RapidRequest rapidRequest, OutputStream outputStream, String formId, boolean email) throws Exception {

		// get the application
		Application application = rapidRequest.getApplication();

		// check we got an application
		if (application != null) {

			// get the servlet context
			ServletContext context = rapidRequest.getServletContext();

			// get and set any parameter values
			String paramFontHeader1Size = getParameterValue(context, application, "form.pdf.font.header1.size");
			if (paramFontHeader1Size != null) FONT_SIZE_HEADER1 = Float.parseFloat(paramFontHeader1Size);
			String paramFontHeader2Size = getParameterValue(context, application, "form.pdf.font.header2.size");
			if (paramFontHeader2Size != null) FONT_SIZE_HEADER2 = Float.parseFloat(paramFontHeader2Size);
			String paramFontSize = getParameterValue(context, application, "form.pdf.font.size");
			if (paramFontSize != null) FONT_SIZE = Float.parseFloat(paramFontSize);

			String paramMarginTop = getParameterValue(context, application, "form.pdf.margin.top");
			if (paramMarginTop != null) MARGIN_TOP = Float.parseFloat(paramMarginTop);
			String paramMarginBottom = getParameterValue(context, application, "form.pdf.margin.bottom");
			if (paramMarginBottom != null) MARGIN_BOTTOM = Float.parseFloat(paramMarginBottom);
			String paramMarginLeft = getParameterValue(context, application, "form.pdf.margin.left");
			if (paramMarginLeft != null) MARGIN_LEFT = Float.parseFloat(paramMarginLeft);
			String paramMarginRight = getParameterValue(context, application, "form.pdf.margin.right");
			if (paramMarginRight != null) MARGIN_RIGHT = Float.parseFloat(paramMarginRight);

			String paramMarginNextTop = getParameterValue(context, application, "form.pdf.margin.next.top");
			if (paramMarginNextTop != null) MARGIN_NEXT_TOP = Float.parseFloat(paramMarginNextTop);
			String paramMarginNextBottom = getParameterValue(context, application, "form.pdf.margin.next.bottom");
			if (paramMarginNextBottom != null) MARGIN_NEXT_BOTTOM = Float.parseFloat(paramMarginNextBottom);


			String paramMarginHeader = getParameterValue(context, application, "form.pdf.margin.header");
			if (paramMarginHeader != null) MARGIN_HEADER_BOTTOM = Float.parseFloat(paramMarginHeader);
			String paramMarginSection = getParameterValue(context, application, "form.pdf.margin.section");
			if (paramMarginSection != null) MARGIN_SECTION_BOTTOM = Float.parseFloat(paramMarginSection);
			String paramMarginText = getParameterValue(context, application, "form.pdf.margin.text");
			if (paramMarginText != null) MARGIN_TEXT_BOTTOM = Float.parseFloat(paramMarginText);
			String paramMarginGrid = getParameterValue(context, application, "form.pdf.margin.grid");
			if (paramMarginGrid != null) MARGIN_GRID_COLUMN = Float.parseFloat(paramMarginGrid);

			// our document
			PDDocument document = null;

			// our page
			PDPage p = null;

			// our page content stream
			PDPageContentStream cs = null;

			String template = getParameterValue(context, application, "form.pdf.template");
			// if we got one
			if (template != null) {
				// get a file for it
				File templateFile = new File(getAppFilePath(rapidRequest, template));
				// if it exists
				if (templateFile.exists()) {
					// load the template into our document
					document = PDDocument.load(templateFile);
					// get the first page for the template
					p = document.getPage(0);
					// make an ammendable content stream
					cs = new PDPageContentStream(document, p, PDPageContentStream.AppendMode.APPEND, true, true);
				}
			} else {
				// standard empty document
				document = new PDDocument();
				// make a new blank page
				p = new PDPage(PDRectangle.A4);
				// make new content stream
				cs = new PDPageContentStream(document, p);
			}

			// Create a new font object selecting one of the PDF base fonts
			PDFont fontBold = PDType1Font.HELVETICA_BOLD;
			PDFont font = PDType1Font.HELVETICA;

			float h = p.getMediaBox().getHeight() - MARGIN_TOP - MARGIN_BOTTOM;
			float w = p.getMediaBox().getWidth() - MARGIN_LEFT - MARGIN_RIGHT;

			PDDocumentInformation info = document.getDocumentInformation();
			info.setTitle(application.getTitle());
			info.setCreator("Rapid Information Systems - https://www.rapid-is.co.uk");
			info.setSubject("Form number " + formId);

			// look for the a pdfLogo image
			File imageFile = getPDFLogoFile(rapidRequest);
			// if we got one
			if (imageFile != null) {
				if (imageFile.exists()) {
					// read the image
					BufferedImage awtImage = ImageIO.read(imageFile);
					// pass to pdf image
					PDImageXObject ximage = LosslessFactory.createFromImage(document, awtImage);
					// draw at half resolution in top right corner
					cs.drawImage( ximage, w - ximage.getWidth()/2, h - ximage.getHeight()/2, ximage.getWidth()/2, ximage.getHeight()/2);
				}
			}

			float y = MARGIN_TOP;

			cs.beginText();
			cs.setFont(fontBold, FONT_SIZE_HEADER1);
			cs.newLineAtOffset(MARGIN_LEFT, h - y);
			cs.showText(application.getTitle());
			cs.endText();

			y += getFontHeight(fontBold, FONT_SIZE_HEADER1) + MARGIN_SECTION_BOTTOM * 5;

			// get the submitted dateTime
			String submittedDateTime = getFormSubmittedDate(rapidRequest, formId);

			// assume form not submitted
			String headerText = "This form has not been submitted";

			// if there was an app refno and submitted date
			if (submittedDateTime != null) {
				// update the headerText
				headerText = "Form number " + formId + " - " + submittedDateTime;
			}

			// write the header text
			cs.beginText();
			cs.setFont(fontBold, FONT_SIZE_HEADER2);
			cs.newLineAtOffset(MARGIN_LEFT, h - y);
			cs.showText(headerText);
			cs.endText();

			y += getFontHeight(fontBold, FONT_SIZE_HEADER2) + MARGIN_SECTION_BOTTOM * 5;

			for (PageHeader pageHeader : _application.getPages().getSortedPages()) {

				// get any page control values
				FormPageControlValues pageControlValues = getFormPageControlValues(rapidRequest, formId, pageHeader.getId());

				// if non null
				if (pageControlValues != null) {

					// if we got some
					if (pageControlValues.size() > 0) {

						// get the page with this id
						Page page = _application.getPages().getPage(rapidRequest.getRapidServlet().getServletContext(), pageHeader.getId());

						// get all page controls (in display order)
						List<Control> pageControls = page.getAllControls();

						// a list of control values to print
						List<String> controlValues = new ArrayList<>();

						// a running height of this section
						float sh = 0;

						// get the standard font height
						float fh = getFontHeight(font, FONT_SIZE);

						// loop the page controls
						for (Control control : pageControls) {

							if (control.getLabel() != null) {

								// loop the page control values
								for (FormControlValue controlValue : pageControlValues) {

									// only if control was visible
									if (!controlValue.getHidden()) {

										// look for an id match
										if (control.getId().equals(controlValue.getId())) {

											// get the type
											String type = control.getType();
											// get the value
											String value = controlValue.getValue();

											// update value to no vaue
											if (value == null) value = "(no value)";

											// check for grid control
											if ("grid".equals(type)) {

												// add the label
												controlValues.add(control.getLabel());

												// hold the detail for later
												controlValues.add("[[grid," + control.getId() + "]]:" + value);

											} else {

												// check for json
												if (value.startsWith("{") && value.endsWith("}")) {
													try {
														JSONObject jsonValue = new JSONObject(value);
														value = jsonValue.optString("text");
													} catch (Exception ex) {}
												}

												// add the table
												controlValues.add(cleanString(control.getLabel()));

												// check for checkboxes
												if (!"checkbox".equals(type)) {
													// get the value
													value = cleanString(control.getCodeText(_application, value));
												}

												// get the width required to print the value
												float vw = font.getStringWidth(value) / 1000 * FONT_SIZE;

												// if this fits into our allowed width
												if (vw <= w) {

													// add this value
													controlValues.add(value);
													// increment the height by one line
													sh += fh + MARGIN_SECTION_BOTTOM;

												} else {

													// split the value into parts
													String[] parts = value.split(" ");
													// start at the beginning
													int pos = 0;

													// while we have more parts
													while (pos < parts.length) {

														String line = parts[pos];
														pos ++;
														vw = 0;

														// while we haven't crossed the end of the available width yet
														while (vw < w) {
															if (pos < parts.length) {
																String part = parts[pos];
																int breakPos = part.indexOf("\n");
																if (breakPos > 0) {
																	controlValues.add(line + " " + part.substring(0, breakPos));
																	controlValues.add("");
																	line = part.substring(breakPos).trim();
																} else {
																	line = line + " " + part;
																}
															}
															pos ++;
															if (pos >= parts.length) break;
															vw = font.getStringWidth(line + parts[pos]) / 1000 * FONT_SIZE;
														}

														// add this line
														controlValues.add(line);
														// increment the height by one line and add
														sh += fh + MARGIN_TEXT_BOTTOM;

													} // parts left

												} // width fit

											} // control type / grid check

											// add an empty line after the value
											controlValues.add("");


											// exit this loop
											break;

										}

									}

								}

							}

							// if there are no controlValues left we can stop entirely
							if (pageControlValues.size() == 0) break;

						} // page control loop

						// if there are some values in the string builder
						if (controlValues.size() > 0) {

							// add the header height
							sh += getFontHeight(fontBold, FONT_SIZE_HEADER2) + MARGIN_HEADER_BOTTOM;

							// if this is going to push us past the page height make a new page and reset heights
							if (y + sh > h) {
								p = new PDPage(PDRectangle.A4);
								document.addPage(p);
								cs.close();
								cs = new PDPageContentStream(document, p);
								y = MARGIN_TOP;
							}

							String pageTitle = page.getLabel();
							if (pageTitle == null) {
								pageTitle = page.getTitle();
							} else {
								if (pageTitle.trim().length() == 0) pageTitle = page.getTitle();
							}

							if (pageTitle != null) {
								
								// if this is going to push us past the page height make a new page and reset heights
								if (y + sh > h - MARGIN_BOTTOM) {
									p = new PDPage(PDRectangle.A4);
									document.addPage(p);
									cs.close();
									cs = new PDPageContentStream(document, p);
									h = p.getMediaBox().getHeight() - MARGIN_NEXT_TOP - MARGIN_NEXT_BOTTOM;
									y = MARGIN_NEXT_TOP - FONT_SIZE_HEADER2;
								}

								cs.beginText();
								cs.setFont(fontBold, FONT_SIZE_HEADER2);
								cs.newLineAtOffset(MARGIN_LEFT, h - y);
								cs.showText(pageTitle);
								cs.endText();

								y += getFontHeight(fontBold, FONT_SIZE_HEADER2) + MARGIN_HEADER_BOTTOM;

								for (String value : controlValues) {

									// look for { and }, must be a grid
									if (value.startsWith("[[grid,") && value.endsWith("}")) {

										//[[grid,P3_C9_]]:{...
										String controlId = value.substring(7, value.indexOf("]]:"));

										value = value.substring(value.indexOf("]]:") + 3);

										JSONObject jsonData = new JSONObject(value);

										Control control = application.getControl(rapidRequest.getRapidServlet().getServletContext(), controlId);

										//get the fields
										JSONArray fields = jsonData.getJSONArray("fields");
										//get the rows
										JSONArray rows = jsonData.getJSONArray("rows");
										//get all the properties for all the fields
										JSONArray columnsProperties = new JSONArray(control.getProperty("columns"));

										// if this is going to push us past the page height make a new page and reset heights
										if (y + fh * rows.length() > h - MARGIN_BOTTOM) {
											p = new PDPage(PDRectangle.A4);
											document.addPage(p);
											cs.close();
											cs = new PDPageContentStream(document, p);
											h = p.getMediaBox().getHeight() - MARGIN_NEXT_TOP - MARGIN_NEXT_BOTTOM;
											y = MARGIN_NEXT_TOP - FONT_SIZE;
										}

										//get a list of the visible fields
										List<String> visibleFields = new ArrayList<>();
										for (int i = 0; i < fields.length(); i++){
											//a field
											String field = fields.getString(i);

											if (columnsProperties != null){
												//loop through the columnsProperties
												for (int c = 0; c < columnsProperties.length(); c++){
													//for each column, get its properties
													JSONObject column_properties = columnsProperties.getJSONObject(c);
													//find the properties for 'this' field and make sure its visible
													if(field.equals(column_properties.getString("field")) && column_properties.getBoolean("visible")){
														//store the visible field in a list
														visibleFields.add(field);
														break;
													}
												}
											}
										}

										float newXOffset = MARGIN_LEFT;
										float initialYOffset = y;
										for (int i = 0; i < fields.length(); i++){
											//a field
											String field = fields.getString(i);
											if (visibleFields.contains(field)){

												//keep a track of the maxWidth - assume maxWidth is the field
												float maxWidth = font.getStringWidth(field) / 1000 * FONT_SIZE;

												cs.beginText();
												cs.setFont(fontBold, FONT_SIZE);
												cs.newLineAtOffset(newXOffset, h - y);
												cs.showText(field);
												cs.endText();

												y += getFontHeight(font, FONT_SIZE) + MARGIN_TEXT_BOTTOM;

												for (int j = 0; j < rows.length(); j++){

														JSONArray row = rows.getJSONArray(j);
														String columnCell = row.getString(i);

														float cellWidth = font.getStringWidth(columnCell) / 1000 * FONT_SIZE;

														if (cellWidth > maxWidth){
															maxWidth = cellWidth;
														}

														cs.beginText();
														cs.setFont(font, FONT_SIZE);
														//newXOffset += maxWidth;
														cs.newLineAtOffset(newXOffset, h - y);
														cs.showText(columnCell);
														cs.endText();

														y += getFontHeight(font, FONT_SIZE) + MARGIN_TEXT_BOTTOM;
														
												}// end of inner loop

												//set the x position for the new column
												newXOffset += maxWidth + MARGIN_GRID_COLUMN;
												//reset the y position for the new column
												y = initialYOffset;

											}

										}// end of outer loop

									} else {
										
										// if this is going to push us past the page height make a new page and reset heights
										if (y + fh + MARGIN_TEXT_BOTTOM > h - MARGIN_BOTTOM) {
											p = new PDPage(PDRectangle.A4);
											document.addPage(p);
											cs.close();
											cs = new PDPageContentStream(document, p);
											h = p.getMediaBox().getHeight() - MARGIN_NEXT_TOP - MARGIN_NEXT_BOTTOM;
											y = MARGIN_NEXT_TOP - FONT_SIZE;
										}

										// write the value text
										cs.beginText();
										cs.setFont(font, FONT_SIZE);
										cs.newLineAtOffset(MARGIN_LEFT, h - y);
										cs.showText(value);
										cs.endText();

										// move to the next line
										y += fh + MARGIN_TEXT_BOTTOM;

									} // grid check

								} // control values loop

							} // page title check

						} // values written check

					} // control values length > 0

				} // control values non null

			} // page loop

			// Make sure that the content stream is closed:
			cs.close();

			// write to response output stream
			document.save(outputStream);
			// close
			document.close();

		} // application check

	}

	// writes the form PDF to the http response
	public void doWriteFormPDF(RapidRequest rapidRequest, HttpServletResponse response, String formId, boolean email) throws Exception {

		// assume not passed
		boolean passed = false;

		// check we got a form id
		if (formId != null) {
			// check this has been submitted
			if (getSubmittedForms(rapidRequest).contains(formId)) {
				// we're good to go
				passed = true;
			} else {
				// get password
				String password = rapidRequest.getRequest().getParameter("pwd");
				// if we got one
				if (password != null) {
					// if we're allowed to resume with the password we can get the .pdf
					if (getResumeFormDetails(rapidRequest, formId, password) != null) passed = true;
				}
			}
		}

		// check for a form id - should be null if form not commence properly
		if (passed) {

			// set the appropriate content type
			response.setContentType("application/pdf");

			// set a suggested filename without forcing save as
			response.setHeader("Content-disposition","attachment; filename=" + getFormFileName(rapidRequest, formId, "pdf", email));

			// write the pdf
			writeFormPDF(rapidRequest, response.getOutputStream(), formId, email);

		} else {

			// send users back to the start
			response.sendRedirect("~?a=" + _application.getId() + "&v=" + _application.getVersion());

		}

	}

	// called by the form action when saving the form
	public synchronized boolean saveForm(RapidRequest rapidRequest, String email, String password) throws Exception {

		// assume we are unable to save the form
		return false;

	}

	// called by the form action when resuming forms
	public synchronized boolean resumeForm(RapidRequest rapidRequest, String formId, String password) throws Exception {

		// assume we have not saved the form
		return false;

	}

	// used when resuming forms
	public synchronized UserFormDetails doResumeForm(RapidRequest rapidRequest, String formId, String password) throws Exception {
		// get the application
		Application application = rapidRequest.getApplication();
		// if there was one
		if (application != null) {
			// get the pages
			Pages pages = application.getPages();
			// if we got some
			if (pages != null) {
				// loop them
				for (String pageId : pages.getPageIds()) {
					// get the page
					Page page = pages.getPage(getServletContext(), pageId);
					// if it has variables
					List<String> variables = page.getSessionVariables();
					// if it has some
					if (variables != null) {
						// loop them
						for (String variable : variables) {
							// set them to null
							rapidRequest.getRequest().getSession().setAttribute(variable, null);
						}
					}
				}
			}
		}
		// check the password against the formId using the user-implemented method
		UserFormDetails details = getResumeFormDetails(rapidRequest, formId, password);
		// check success
		if (details == null) {
			// invalidate any current form
			setUserFormDetails(rapidRequest, null);
		} else {
			// set the form details
			setUserFormDetails(rapidRequest, details);
			// get the user page variable values
			Map<String, String> pageVariableValues = getFormPageVariableValues(rapidRequest, formId);
			// if we got some
			if (pageVariableValues != null) {
				// loop them
				for (String variable  : pageVariableValues.keySet()) {
					// get the value
					String value = pageVariableValues.get(variable);
					// set the value
					rapidRequest.getRequest().getSession().setAttribute(variable, value);
				}
			}
		}
		// return the result
		return details;
	}

	// called when form application is deleted in Rapid Admin - might update any casework system reference tables, matching code for first insert / updates in Form Adapter constructor
	public synchronized void delete(RapidRequest rapidRequest) {}

	// whether this form has any summary labels, if not we can skip the summary
	public boolean getHasSummaryLabels(RapidRequest rapidRequest, String formId) throws Exception {

		// assume not
		boolean hasSummaryLabels = false;

		// if there is an application
		if (_application != null) {

			// get it's pages
			PageHeaders pages = _application.getPages().getSortedPages();

			// loop them
			for (PageHeader pageHeader : pages) {

				// get the page
				Page page = _application.getPages().getPage(pageHeader.getId());

				// get the page label
				String pageLabel = page.getLabel();

				// if the page has a label we can go!
				if (pageLabel != null && pageLabel.trim().length() > 0) {
					// we have labels
					hasSummaryLabels = true;
					// we're done
					break;
				}

				// get user form page values
				FormPageControlValues controlValues = getFormPageControlValues(rapidRequest, formId, pageHeader.getId());

				// if there are some
				if (controlValues != null && controlValues.size() > 0) {

					// loop them
					for (FormControlValue value : controlValues) {

						// if not hidden
						if (!value.getHidden()) {

							// get the control
							Control control = page.getControl(value.getId());

							// if we got one
							if (control != null) {

								// get its label
								String label = control.getLabel();

								// if there is something we're good!
								if (label != null && label.trim().length() > 0) {

									// we have labels
									hasSummaryLabels = true;
									// we're done
									break;

								} // label check

							} // control null check

						} // control hidden check

					} // control value loop

				} // control value check

			} // page header loop

		} // application check

		return hasSummaryLabels;

	}

	// simple parameters are insert into the subject or body
	protected String insertFormParameters(RapidRequest rapidRequest, UserFormDetails formDetails, String string) {

		// application parameters
		string = _application.insertParameters(rapidRequest.getRapidServlet().getServletContext(), string);
		// form id
		if (string.contains("[[id]]")) string = string.replace("[[id]]", formDetails.getId());
		// title
		if (string.contains("[[title]]")) string = string.replace("[[title]]", _application.getTitle());

		// return string
		return string;

	}

	// scans a string and adds mapping entries of keys and control ids to _ControlParametersMap which is used to search and replace form values for customer email subject and body
	protected void populateControlParametersMap(RapidRequest rapidRequest, String string) {

		// make a new parameters map - this will be emptied each time the app is saved in Rapid Admin as the form adapter is re-initalised
		if (_ControlParametersMap == null) _ControlParametersMap = new HashMap<>();

		// get any startPos
		int startPos = string.indexOf("[[");
		// if there was one#
		if (startPos > -1) {
			// get the end pos
			int endPos = string.indexOf("]]", startPos);

			// get the servlet context
			ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();

			// named controls, we only want to fetch them once
			List<Control> namedControls = null;

			// if it has [[ and ]] thereafter
			while (startPos > -1 && endPos > startPos) {

				// get the key
				String key = string.substring(startPos + 2, endPos);

				// whether we have added the parameter yet
				boolean parameterAdded = false;

				// if it looks like a control it
				if (key.contains("_C")) {

					// get control from key
					Control control = _application.getControl(servletContext, key);

					// check control
					if (control != null) {

						// the key is a control id so the key and value are the same
						_ControlParametersMap.put(key, key);

						// remember we added it
						parameterAdded = true;

					}

				}

				// if we haven't added the parameter yet - use the more expensive get by names
				if (!parameterAdded) {

					// get named controls if we have to
					if (namedControls == null) namedControls = _application.getAllNamedControls(servletContext);

					// loop the named controls
					for (Control namedControl : namedControls) {

						// if this is the one we're looking for
						if (key.equals(namedControl.getName())) {

							// retain
							_ControlParametersMap.put(key, namedControl.getId());

							// we're done!
							break;

						}

					}

				}

				// get the next startPos
				startPos = string.indexOf("[[", endPos);
				// if we got one get the next endPos
				if (startPos > -1) endPos = string.indexOf("]]", startPos);

			} // loop for ]]

		} // ]] check again

	}

	// insert parameters, used in customer email subject and body
	public String insertControlParameters(RapidRequest rapidRequest, UserFormDetails formDetails, String string) throws Exception {

		// get pos of [[
		int startPos = string.indexOf("[[");
		// check string contains [[
		if (startPos > -1) {

			// get the form id
			String formId = formDetails.getId();

			// loop the email parameters map
			for (String key : _ControlParametersMap.keySet()) {

				// if we have this key in our string
				if (string.contains(key)) {

					// get the value if not hidden
					String value = this.getFormControlValue(rapidRequest, formId, _ControlParametersMap.get(key), true);

					// update to blank (unrecognised [[names]] will still appear)
					if (value == null) value = "";

					// put it in!
					string = string.replace("[[" + key + "]]", value);

				} // check for key

			} // loop parameter keys

		} // ]] check

		// return
		return string;

	}

	// static methods

	public static FormPageControlValues getPostPageControlValues(RapidRequest rapidRequest, String postBody, String formId) throws ServerSideValidationException, UnsupportedEncodingException  {
		// check for a post body
		if (postBody == null) {
			// send null if nothing
			return null;
		} else {
			// create our pageControlValues
			FormPageControlValues pageControlValues = new FormPageControlValues();
			// split into name value pairs
			String[] params = postBody.split("&");
			// hidden control values
			String[] hiddenControls = null;
			// assume no reCaptcha value
			String recaptcha = null;
			// get the app
			Application app = rapidRequest.getApplication();
			// get the page
			Page page = rapidRequest.getPage();
			// assume not passed CSRF
			boolean csrfPass = false;
			// check there was one
			if (page != null) {
				// loop the pairs
				for (int i = 0; i < params.length; i++) {
					// get the param
					String param = params[i];
					// split on =
					String[] parts = param.split("=");
					// the key/name is the control id
					String id = null;
					// assume it's not hidden
					boolean hidden = false;
					// try and decode the if with a silent fail
					try { id = URLDecoder.decode(parts[0],"UTF-8");	} catch (UnsupportedEncodingException e) {}
					// check we got something
					if (id != null) {
						// if there was a name but not the _hiddenControls
						if (id.length() > 0) {
							// assume there are no more of this parameter
							boolean lastValue = true;
							// now check there are no more (check boxes get sent with a null in front, in case they are not ticked so we know it update their value)
							for (int j = i + 1; j < params.length; j++) {
								// get the check param
								String checkParam = params[j];
								// if this starts with the id
								if (checkParam.startsWith(id)) {
									// update last value
									lastValue = false;
									// we're done
									break;
								}
							}
							// if this was the last value for the control
							if (lastValue) {
								// assume no value
								String value = null;
								// if more than 1 part
								if (parts.length > 1) {
									// url decode value
									try { value = URLDecoder.decode(parts[1],"UTF-8"); } catch (UnsupportedEncodingException ex) {}
								} // parts > 0
								// find the control in the page
								Control control = page.getControl(id);
								// null values can't do any harm so don't check
								if (value != null) {
									// check we found a control
									if (control == null) {
										// if this is the hidden values
										if (id.endsWith("_hiddenControls")) hiddenControls = value.split(",");
										// if this is the recapcha store it
										if ("g-recaptcha-response".equals(id)) recaptcha = value;
										// if this is the csrfToken check it
										if ("csrfToken".equals(id)) {
											// check the value
											if (value.equals(rapidRequest.getCSRFToken())) {
												// remember we passed
												csrfPass = true;
											} else {
												// we're done
												break;
											}
										}
									} else {
										// get any control validation
										Validation validation = control.getValidation();
										// if there was some
										if (validation != null) {
											// proceed to check regex if there is a value or we are not allowing nulls
											if (value.length() > 0 || !validation.getAllowNulls()) {
												// get the RegEx
												String regEx = validation.getRegEx();
												// set to empty string if null (most seem to be empty)
												if (regEx == null) regEx = "";
												// not if none, and not if javascript
												if (regEx.length() > 0 && !"".equals(validation.getType()) && !"none".equals(validation.getType()) && !"javascript".equals(validation.getType())) {
													// check for null
													if (value != null) {
														// place holder for the patter
														Pattern pattern = null;
														// this exception is uncaught but we want to know about it
														try {
															// we recognise a small subset of switches
															if (regEx.endsWith("/i")) {
																// trim out the switch
																regEx = regEx.substring(0, regEx.length() - 2);
																// build the pattern with case insensitivity
																pattern = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
															} else {
																// build the patter as-is
																pattern = Pattern.compile(regEx);
															}
														} catch (PatternSyntaxException ex) {
															// rethrow
															throw new ServerSideValidationException("Server side validation error - regex for control " + id + " in form " + formId + " failed regex syntax for " + regEx + " - regex PatternSyntaxException", ex);
														} catch (IllegalArgumentException  ex) {
															// rethrow
															throw new ServerSideValidationException("Server side validation error - value '" + value + "' for control " + id + " in form " + formId + " failed regex " + regEx + " - regex ServerSideValidationException", ex);
														}
														// compile and check it
														if (!pattern.matcher(value).find()) throw new ServerSideValidationException("Server side validation error - value " + id + " for form " + formId+ " failed regex");
													} // javascript type check
												} // regex check
											} // length and allow nulls check
										} // validation check

										// look for a maxLength property
										String maxLength = control.getProperty("maxLength");
										// if we got one
										if (maxLength != null) {
											if (Numbers.isInteger(maxLength)) {
												// convert to int
												int max = Integer.parseInt(maxLength);
												// only check the decimal length of the integer part of a number (e.g. "123.45" -> 3)
												if ("Num".equals(control.getProperty("controlType")) && value.contains(".")) value = value.substring(0, value.indexOf("."));
												// make line breaks \n instead of \n\r so the Java length matches the front end
												value = value.replace("\r\n", "\n");
												// check length
												if (value.length() > max) throw new ServerSideValidationException("Server side validation error - value " + id + " for form " + formId+ " failed regex");
											}
										} // maxlength check

									} // found control in page

								} // value null check

								// if there was a control
								if (control != null) {

									// if we have hidden controls to check
									if (hiddenControls != null) {
										// loop the hidden controls
										for (String hiddenControl : hiddenControls) {
											// if there's a match
											if (id.equals(hiddenControl)) {
												// retain as hidden
												hidden = true;
												// we're done
												break;
											} // this is a hidden control
										} // loop the hidden controls
									} // got hidden controls to check

									// add name value pair - controls with null values still need storing for display in the summary page
									pageControlValues.add(id, value, hidden);
								}

							} // last value
						}	// id .length > 0
					} // id != null
				} // param loop
				// get any recapture controls
				List<Control> recaptureControls = page.getRecaptchaControls();
				// if the page had a reCaptcha
				if (page.getRecaptchaControls().size() > 0) {
					// assume we failed
					boolean passRecapture = false;
					// loop the controls
					for (Control control : recaptureControls) {
						// get the secret
						String secret = control.getProperty("secret");
						// try
						try {
							// get the check response
							String checkResponse = Http.post("https://www.google.com/recaptcha/api/siteverify", "secret=" + secret + "&response=" + recaptcha);
							// make sure the response looks like JSON
							if (checkResponse != null && checkResponse.trim().startsWith("{")) {
								// read it into json
								JSONObject jsonCheck = new JSONObject(checkResponse);
								// check the success
								if (jsonCheck.optBoolean("success")) {
									// update the id to the reCAPTCHA control
									passRecapture = true;
									// record the control value as true
									pageControlValues.add(control.getId(), "true");
									// we're done
									break;
								}
							}
							if (!passRecapture) {
								// log the issue
								_logger.info("reCAPTCHA check failed for form " + formId + " app " + app.getId() + " page " + page.getId() + " : " + checkResponse);
							}
						} catch (Exception ex) {
							_logger.error("Error checking reCAPTCHA for form " + formId + " app " + app.getId() + " page " + page.getId() + " : " + ex.getMessage(), ex);
						}
					}
					// error is we didn't pass recapture
					if (!passRecapture) throw new ServerSideValidationException("Server side validation error - recapture failed on page " + page.getId() + " for form " + formId);
				}
			}
			// check csrfpass
			if (!csrfPass) throw new ServerSideValidationException("Failed CSRF");

			// return values
			return pageControlValues;
		} // postBody check
	}

}
