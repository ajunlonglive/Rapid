/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Pages.PageHeaders;
import com.rapid.security.SecurityAdapter;
import com.rapid.security.SecurityAdapter.SecurityAdapaterException;
import com.rapid.server.Rapid;
import com.rapid.server.RapidRequest;

public abstract class FormAdapter {
	
	// a single controls value
	public static class FormControlValue {
		
		// instance variables
		private String _id, _value;
		private boolean _hidden;
		
		// properties
		public String getId() { return _id; }
		public String getValue() { return _value; }
		public Boolean getHidden() { return _hidden; }
		
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
			return _id + "  = " + _value + (_hidden ? " (hidden)"  : "");
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
						
	}
	
	//  static finals
	private static final String USERFORMIDS = "userFormIds";
	
	// instance variables
	
	protected ServletContext _servletContext;
	protected Application _application;
	
	// properties
	
	public ServletContext getServletContext() { return _servletContext; }
	public Application getApplication() { return _application; }
	
	// constructor
	
	public FormAdapter(ServletContext servletContext, Application application) {
		_servletContext = servletContext;
		_application = application;
	}
	
	// abstract methods
					
	// this method returns a new form id, when allowed, by a given adapter, could be in memory, or database, etc
	public abstract String getNewFormId(RapidRequest rapidRequest) throws Exception;
	
	// this method checks a form id against a password for resuming 
	public abstract boolean checkFormResume(RapidRequest rapidRequest, String formId, String password) throws Exception;
	
	// returns all the form control values for a given page
	public abstract FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String pageId) throws Exception;
	
	// sets all the form control values for a given page - pages that fail the isVisible method will be sent  a null pageControlValues
	public abstract void setFormPageControlValues(RapidRequest rapidRequest, String pageId, FormPageControlValues pageControlValues) throws Exception;
	
	// gets the value of a form control value	
	public abstract String getFormControlValue(RapidRequest rapidRequest, String controlId, boolean notHidden) throws Exception;
			
	
	// this html is written after the body tag
	public abstract String getSummaryStartHtml(RapidRequest rapidRequest, Application application);
	
	// this html is written after the submit button, before the body close tag 
	public abstract String getSummaryEndHtml(RapidRequest rapidRequest, Application application);
		
	// this html is written for the start of each page
	public abstract String getSummaryPageStartHtml(RapidRequest rapidRequest, Application application, Page page);
	
	// this html is written for the end of each page
	public abstract String getSummaryPageEndHtml(RapidRequest rapidRequest, Application application, Page page);
	
	// this html contains the control value
	public abstract String getSummaryControlValueHtml(RapidRequest rapidRequest, Application application, Page page, FormControlValue controlValue);
	
	// this html is written after the end of the pages, before the submit button
	public abstract String getSummaryPagesEndHtml(RapidRequest rapidRequest, Application application);
	
	
	// submits the form and receives a message for the submitted page
	public abstract String submitForm(RapidRequest rapidRequest) throws Exception;
				
	// this html is written for successfully submitted forms
	public abstract String getSubmittedHtml(RapidRequest rapidRequest, String message);
	
	// this html is written for any forms where there was an error on submission
	public abstract String getSubmittedExceptionHtml(RapidRequest rapidRequest, Exception ex);

	// protected instance methods
		
	protected String getUserFormId(RapidRequest rapidRequest) {
		// get the user session (without making a new one)
		HttpSession session = rapidRequest.getRequest().getSession(false);
		// get the form ids map from the session
		Map<String,String> formIds = (Map<String, String>) session.getAttribute(USERFORMIDS);
		// check we got some
		if (formIds == null) {
			return null;
		} else {
			// get the application
			Application application = rapidRequest.getApplication();
			// return the user form id for a given app id / version
			return formIds.get(application.getId() + "-" + application.getVersion());
		}
	}
	
	protected void setUserFormId(RapidRequest rapidRequest, String formId) {
		// get the user session (making a new one if need be)
		HttpSession session = rapidRequest.getRequest().getSession();
		// get the form ids
		Map<String,String> formIds = (Map<String, String>) session.getAttribute(USERFORMIDS);
		// make some if we didn't get
		if (formIds == null)  formIds = new HashMap<String, String>(); 					
		// get the application
		Application application = rapidRequest.getApplication();
		// store the form if for a given app id / version
		formIds.put(application.getId() + "-" + application.getVersion(), formId);		
		// update the session with the new form ids
		session.setAttribute(USERFORMIDS, formIds);		
	}
	
	// public instance methods
	
	// this looks for a form id in the user session and uses the adapter specific getNewId routine if one is allowed. Returning null sends the user to the start page
	public String getFormId(RapidRequest rapidRequest) throws Exception {		
		// get the form id using what's in the request
		String formId = getUserFormId(rapidRequest);
		// if it's null
		if (formId == null) {			
			// get the application
			Application application = rapidRequest.getApplication();
			// get the start page header
			String startPageId = application.getPages().getSortedPages().get(0).getId();
			// get the requested Page
			Page requestPage = rapidRequest.getPage();
			// get the request page id
			String requestPageId = null;
			// if there was a page get the id
			if (requestPage  != null) requestPageId = requestPage.getId();
			// assume no new id allowed
			boolean newIdAllowed = false;
			// if this is the start page
			if  (startPageId.equals(requestPageId)) {
				// we're ok to hand out a new id
				newIdAllowed = true;
			} else {
				// get the security adapter
				SecurityAdapter security = application.getSecurityAdapter();
				// if the user has design 
				try {
					if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) newIdAllowed = true;
				} catch (SecurityAdapaterException e) {}
			}
			// there are some rules for creating new form ids - there must be no action and the page must be the start page
			if (rapidRequest.getRequest().getParameter("action") == null && newIdAllowed) {				
				// get a new form id from the adapter
				formId = getNewFormId(rapidRequest);
				// put into form ids
				setUserFormId(rapidRequest, formId);						
			}			
		}					
		return formId;
	}
	
	// used when resuming forms
	public boolean doResumeForm(RapidRequest rapidRequest, String formId, String password) throws Exception {
		// first check the password against the formId using the user-implemented method
		boolean canResume = checkFormResume(rapidRequest, formId, password);
		// check success
		if (canResume) {			
			// set the form id based on the app id and version
			setUserFormId(rapidRequest, formId);
		} else {
			// invalidate any current form
			setUserFormId(rapidRequest, null);
		}		
		// return the result
		return canResume;
	}
	
	// this is called from the Rapid servelet and is the form submission and management of the form state in the user session 
	public String doFormSubmit(RapidRequest rapidRequest) throws Exception {
		// first run the subitForm in the non-abstract class
		String message = submitForm(rapidRequest);
		// empty the form id - invalidating the form
		setUserFormId(rapidRequest, null);
		// return the message
		return message;
	}
	
	// this writes the form summary page
	public void writeFormSummary(RapidRequest rapidRequest, HttpServletResponse response) throws Exception {
		
		// get the form id
		String formId = getFormId(rapidRequest);
		
		// check for a form id - should be null if form not commence properly
		if (formId == null) {
			
			// send users back to the start
			response.sendRedirect("~?a=" + _application.getId() + "&v=" + _application.getVersion());
			
		} else {
		
			// create a writer
			PrintWriter writer = response.getWriter();
					
			// set the response type
			response.setContentType("text/html");
			
			// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
			writer.write("<!DOCTYPE html>\n");
																							
			// open the html
			writer.write("<html>\n");
			
			// open the head
			writer.write("  <head>\n");
			
			// write a title
			writer.write("    <title>Form summary - by Rapid</title>\n");
			
			// get the servletContext
			ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
			
			// get app start page
			Page startPage = _application.getStartPage(servletContext);
						
			// write the start page head (and it's resources)
			writer.write(startPage.getResourcesHtml(_application, true));
			
			// close the head
			writer.write("  </head>\n");
			
			// open the body
			writer.write("  <body>\n");
			
			// write the summary start
			writer.write(getSummaryStartHtml(rapidRequest, _application));
			
			// get the sorted pages
			PageHeaders pageHeaders = _application.getPages().getSortedPages();
			
			// loop the page headers
			for (PageHeader pageHeader : pageHeaders) {
				
				// get the page
				Page page = _application.getPages().getPage(servletContext, pageHeader.getId());
														
				// a string builder for the page values
				StringBuilder valuesStringBuilder = new StringBuilder();
				
				// get any page control values
				FormPageControlValues pageControlValues = _application.getFormAdapter().getFormPageControlValues(rapidRequest, page.getId());
				
				// if non null
				if (pageControlValues != null) {
					
					// if we got some
					if (pageControlValues.size() > 0) {
					
						// get all page controls (in display order)
						List<Control> pageControls = page.getAllControls();
					
						// loop the page controls
						for (Control control : pageControls) {
							
							// loop the page control values
							for (FormControlValue controlValue : pageControlValues) {
																						
								// look for an id match
								if (control.getId().equals(controlValue.getId())) {
							
									// write the control value!
									valuesStringBuilder.append(getSummaryControlValueHtml(rapidRequest, _application, page, controlValue));
									// we've found a match so are done with this controlValue
									pageControlValues.remove(controlValue);
									// exit this loop
									break;
																		
								}
								
							}
							
							// if there are no controlValues left we can stop entirely
							if (pageControlValues.size() == 0) break;
							
						}
						
					} // control values length > 0
					
				} // control values non null
				
				// if there are some values in thre string builder
				if (valuesStringBuilder.length() > 0) {
					
					// write the page start html
					writer.write(getSummaryPageStartHtml(rapidRequest, _application, page));
					
					// write the values
					writer.write(valuesStringBuilder.toString());
					
					// write the edit link
					writer.write("<a href='~?a=" + _application.getId() + "&v=" + _application.getVersion() + "&p=" + page.getId() + "'>edit</a>\n");
					
					// write the page end html
					writer.write(getSummaryPageEndHtml(rapidRequest, _application, page));
					
				} // values written check
																							
			} // page loop
			
			// write the pages end
			writer.write(getSummaryPagesEndHtml(rapidRequest, _application));
			
			// write the submit button form and button!
			writer.write("<form action='~?a=" + _application.getId() + "&v=" + _application.getVersion()  + "&action=submit' method='POST'><button type='submit' class='formSummarySubmit'>Submit</button></form>");
			
			// write the summary end
			writer.write(getSummaryEndHtml(rapidRequest, _application));
									
			// close the remaining elements
			writer.write("  </body>\n</html>");
																																		
			// close the writer
			writer.close();
			
			// flush the writer
			writer.flush();
			
		} // form id check
		
	}
	
	// this write the form submit page if all was ok
	public  void writeFormSubmitOK(RapidRequest rapidRequest, HttpServletResponse response, String formId, String message) throws IOException, RapidLoadingException {
					
		// create a writer
		PrintWriter writer = response.getWriter();
				
		// set the response type
		response.setContentType("text/html");
		
		// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
		writer.write("<!DOCTYPE html>\n");
																						
		// open the html
		writer.write("<html>\n");
		
		// open the head
		writer.write("  <head>\n");
		
		// write a title
		writer.write("    <title>Form submitted - by Rapid</title>\n");
		
		// get the servletContext
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		
		// get app start page
		Page startPage = _application.getStartPage(servletContext);
					
		// write the start page head (and it's resources)
		writer.write(startPage.getResourcesHtml(_application, true));
		
		// close the head
		writer.write("  </head>\n");
		
		// open the body
		writer.write("  <body>\n");
		
		// writer the adapter specific submission message
		writer.write(getSubmittedHtml(rapidRequest, message));
		
		// close the body and html
		writer.write("  </body>\n</html>");
																																	
		// close the writer
		writer.close();
		
		// flush the writer
		writer.flush();
						
	}
	
	// this write the form submit page if all was ok
	public  void writeFormSubmitError(RapidRequest rapidRequest, HttpServletResponse response, String formId, Exception ex) throws IOException, RapidLoadingException {
					
		// create a writer
		PrintWriter writer = response.getWriter();
				
		// set the response type
		response.setContentType("text/html");
		
		// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
		writer.write("<!DOCTYPE html>\n");
																						
		// open the html
		writer.write("<html>\n");
		
		// open the head
		writer.write("  <head>\n");
		
		// write a title
		writer.write("    <title>Form submit error - by Rapid</title>\n");
		
		// get the servletContext
		ServletContext servletContext = rapidRequest.getRapidServlet().getServletContext();
		
		// get app start page
		Page startPage = _application.getStartPage(servletContext);
					
		// write the start page head (and it's resources)
		writer.write(startPage.getResourcesHtml(_application, true));
		
		// close the head
		writer.write("  </head>\n");
		
		// open the body
		writer.write("  <body>\n");
		
		// write the adapter specific error html
		writer.write(getSubmittedExceptionHtml(rapidRequest, ex));
		
		// close the body and html
		writer.write("  </body>\n</html>");
																																	
		// close the writer
		writer.close();
		
		// flush the writer
		writer.flush();
						
	}
	
	// static methods
	
	public static FormPageControlValues getPostPageControlValues(String pageId, String postBody)  {
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
			String[] hiddenControls =null;
			// loop the pairs
			for (String param : params) {
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
						// assume no value
						String value = null;					
						// if more than 1 part
						if (parts.length > 1) {
							// url decode value 
							try {  value = URLDecoder.decode(parts[1],"UTF-8"); } catch (UnsupportedEncodingException ex) {}				
						} // parts > 0													
						// if this is the hidden values
						if (id.endsWith("_hiddenControls") && value != null) {
							// retain the hidden values
							hiddenControls = value.split(",");
						} else	{
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
							// add name value pair
							pageControlValues.add(id, value, hidden);
						} // ends with hidden controls						
					}	// id .length > 0
				} // id != null																
			} // param loop			
			return pageControlValues;						
		} // postBody check				
	}

}
