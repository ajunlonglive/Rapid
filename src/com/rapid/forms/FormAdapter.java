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
	public static class ControlValue {
		
		// instance variables
		private String _id, _value;
		
		// properties
		public String getId() { return _id; }
		public String getValue() { return _value; }
		
		// constructor
		public ControlValue(String id, String value) {
			_id = id;
			_value = value;
		}
		
	}
	
	// a pages control values
	public static class PageControlValues extends ArrayList<ControlValue> {
		
		// instance variables
		
		private String _pageId;
		
		// properties
		
		public String getPageId() { return _pageId; }
		
		// constructor
		
		public PageControlValues(String pageId, ControlValue... controlValues) {
			_pageId  = pageId;
			if (controlValues != null) {
				for (ControlValue controlValue : controlValues) {
					this.add(controlValue);
				}
			}
		}
		
		// methods
		
		public void add(String controlId, String controlValue) {
			this.add(new ControlValue(controlId, controlValue));
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
	
	public abstract void storePageControlValues(RapidRequest rapidRequest, Application application, String pageId, PageControlValues pageControlValues);
	
	public abstract PageControlValues retrievePageControlValues(RapidRequest rapidRequest, Application application, String pageId);
	
	// overridable methods
	
	public String getSummaryStartHtml(RapidRequest rapidRequest, Application application) {
		return "<h1>Form summary</h1>\n";
	}
	
	public String getSummaryEndHtml(RapidRequest rapidRequest, Application application) {
		return "";
	}
	
	public String getSummaryPageStartHtml(RapidRequest rapidRequest, Application application, Page page) {
		return "<h2>" + page.getTitle() + "</h2>";
	}
	
	public String getSummaryPageEndHtml(RapidRequest rapidRequest, Application application, Page page) {
		return "";
	}
	
	public String getSummaryControlValueHtml(RapidRequest rapidRequest, Application application, Page page, ControlValue controlValue) {
		Control control = page.getControl(controlValue.getId());
		return "<p>" + control.getName() + " = " + controlValue.getValue() + "</p>";
	}
		
	// static methods
	
	public static PageControlValues getPostPageControlValues(String pageId, String postBody)  {
		// check for a post body
		if (postBody == null) {
			// send null if nothing 
			return null;
		} else {
			// create our pageControlValues
			PageControlValues pageControlValues = new PageControlValues(pageId);
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
	
	public static void writeFormSummary(ServletContext servletContext, RapidRequest rapidRequest, HttpServletResponse response, Application application) throws IOException, RapidLoadingException {
		
		// get the form adapter
		FormAdapter formAdapter = application.getFormAdapter();
		
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
		
		// add the Rapid application style sheet
		writer.write("    <link rel='stylesheet' type='text/css' href='applications/forms/1/rapid.css'></link>\n");
		
		// close the head
		writer.write("  </head>\n");
		
		// open the body
		writer.write("  <body>\n");
		
		// write the summary start
		writer.write(formAdapter.getSummaryStartHtml(rapidRequest, application));
		
		// get the sorted pages
		PageHeaders pageHeaders = application.getPages().getSortedPages();
		
		// loop the page headers
		for (PageHeader pageHeader : pageHeaders) {
			
			// get the page
			Page page = application.getPages().getPage(servletContext, pageHeader.getId());
									
			// get any page control values
			PageControlValues pageControlValues = application.getFormAdapter().retrievePageControlValues(rapidRequest, application, page.getId());
			
			// if we got some
			if (pageControlValues != null) {
				
				// write the page start html
				writer.write(formAdapter.getSummaryPageStartHtml(rapidRequest, application, page));
				
				// loop the control values
				for (ControlValue controlValue : pageControlValues) {
					// write the control value!
					writer.write(formAdapter.getSummaryControlValueHtml(rapidRequest, application, page, controlValue));
				}
				
				// write the edit link
				writer.write("<a href='~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + "'>edit</a>");
				
				// write the page end html
				writer.write(formAdapter.getSummaryPageEndHtml(rapidRequest, application, page));
				
			} // control value check
			
		} // page loop
								
		// close the remaining elements
		writer.write("  </body>\n</html>");
																																	
		// close the writer
		writer.close();
		
		// flush the writer
		writer.flush();
		
	}

}
