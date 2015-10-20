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
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Pages.PageHeader;
import com.rapid.core.Pages.PageHeaders;
import com.rapid.server.RapidRequest;

public abstract class FormAdapter {
	
	// a single controls value
	public static class FormControlValue {
		
		// instance variables
		private String _id, _value;
		
		// properties
		public String getId() { return _id; }
		public String getValue() { return _value; }
		
		// constructor
		public FormControlValue(String id, String value) {
			_id = id;
			_value = value;
		}
		
		// override
		
		@Override
		public String toString() {
			return _id + "  = " + _value;
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
		
		public void add(String controlId, String controlValue) {
			this.add(new FormControlValue(controlId, controlValue));
		}
						
	}
	
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
	
	public abstract String getFormId(RapidRequest rapidRequest);
			
	public abstract FormPageControlValues getFormPageControlValues(RapidRequest rapidRequest, String pageId);
	
	public abstract void setFormPageControlValues(RapidRequest rapidRequest, String pageId, FormPageControlValues pageControlValues);
	
	public abstract String getFormPageControlValue(RapidRequest rapidRequest, String pageId, String controlId);
	
	public abstract String getFormControlValue(RapidRequest rapidRequest, String controlId);
	
	public abstract void submitForm(RapidRequest rapidRequest) throws Exception;
	
	// overridable methods
	
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
				

	// public instance methods
	
	// this write the form page set values routine, it is called by Page.getPageHtml just before the form is closed
	public  void writeFormPageSetValues(RapidRequest rapidRequest, String formId, Application application, String pageId, Writer writer) throws IOException {
				
		// get any form page values
		FormPageControlValues formControlValues = getFormPageControlValues(rapidRequest, pageId);
		
		// if there are any
		if (formControlValues != null) {
			if (formControlValues.size() > 0) {
				
				// open the javascript
				writer.write("<script>\n");
				
				// wait until jQuery is ready
				writer.write("$(document).ready( function() {\n");
				
				// create a value set event
				writer.write("var ev = $.Event('valueset');\n");
								
				try {
									
					// get the page
					Page page = application.getPages().getPage(rapidRequest.getRapidServlet().getServletContext(), pageId);
					
					// loop the values
					for (FormControlValue formControlValue : formControlValues) {
						
						// get the control
						Control pageControl = page.getControl(formControlValue.getId());

						// if we got one
						if (pageControl != null) {
						
							// get the value
							String value = formControlValue.getValue();
							// if there is a value use the standard setData for it (this might change to something more sophisticated at some point)
							if (value != null) writer.write("  setData_" + pageControl.getType() + "(ev, '" + pageControl.getId() + "', 'field', null, '" + value + "', true);\n");
							
						}
					}
					
				} catch (RapidLoadingException ex) {
					writer.write("// error getting page : " + ex.getMessage());
				}
					
				// close the jQuery
				writer.write("});\n");
				
				// close the javascript
				writer.write("</script>\n");
				
			}
		}					
	}
	
	// this writes the form summary page
	public void writeFormSummary(RapidRequest rapidRequest, HttpServletResponse response) throws IOException, RapidLoadingException {
		
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
				
				// if this page was visible
				if (page.isVisible(rapidRequest, formId, _application)) {
										
					// a string builder for the page values
					StringBuilder valuesStringBuilder = new StringBuilder();
					
					// get any page control values
					FormPageControlValues pageControlValues = _application.getFormAdapter().getFormPageControlValues(rapidRequest, page.getId());
					
					// if we got some
					if (pageControlValues != null) {
						
						// loop the control values
						for (FormControlValue controlValue : pageControlValues) {
							// write the control value!
							valuesStringBuilder.append(getSummaryControlValueHtml(rapidRequest, _application, page, controlValue));
						}
						
					} // control value check
					
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
					
				} // page visibility check
																		
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
	public  void writeFormSubmitOK(RapidRequest rapidRequest, HttpServletResponse response, String formId) throws IOException, RapidLoadingException {
					
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
			writer.write("  <body>Thank you.</body>\n</html>");
																																		
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
			writer.write("  <body>There was a problem submutting your form: " + ex.getMessage() + "</body>\n</html>");
																																		
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
			// loop the pairs
			for (String param : params) {
				// split on =
				String[] parts = param.split("=");						
				// get the name
				String name = null;
				// try and decode the name with a silent fail
				try { 
					// get the name
					name = URLDecoder.decode(parts[0],"UTF-8");
					// if there was a name
					if (name.length() > 0) {
						// assume no value
						String value = null;					
						// if more than 1 part
						if (parts.length > 1) {
							// url decode value with a silent fail
							try { value = URLDecoder.decode(parts[1],"UTF-8"); } catch (UnsupportedEncodingException e) {}				
						}	
						// add name value pair
						pageControlValues.add(name, value);
					}
				} catch (UnsupportedEncodingException e) {}												
			} // param loop			
			return pageControlValues;						
		} // postBody check				
	}

}
