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

package com.rapid.server;

/*

This class wraps HttpServlet and provides a number of useful functions
Mostly getters that retrieve from the servlet context

 */

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Applications;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.core.Applications.Versions;
import com.rapid.security.SecurityAdapater;
import com.rapid.security.SecurityAdapater.Role;
import com.rapid.security.SecurityAdapater.SecurityAdapaterException;
import com.rapid.server.filter.RapidFilter;
import com.rapid.utils.Comparators;

@SuppressWarnings({"serial", "unchecked", "rawtypes"})
public class RapidHttpServlet extends HttpServlet {
	
	private static Logger _logger = Logger.getLogger(RapidHttpServlet.class);

	public Logger getLogger() {
		return (Logger) getServletContext().getAttribute("logger");
	}
	
	public JAXBContext getJAXBContext() {
		return (JAXBContext) getServletContext().getAttribute("jaxbContext");
	}
	
	public Marshaller getMarshaller() {
		return (Marshaller) getServletContext().getAttribute("marshaller");
	}
	
	public Unmarshaller getUnmarshaller() {
		return (Unmarshaller) getServletContext().getAttribute("unmarshaller");
	}
	
	public Constructor getSecurityConstructor(String type) {
		HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) getServletContext().getAttribute("securityConstructors");
		return constructors.get(type);
	}
	
	public Constructor getActionConstructor(String type) {
		HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) getServletContext().getAttribute("actionConstructors");
		return constructors.get(type);
	}
	
	public JSONArray getJsonDatabaseDrivers() {
		return (JSONArray) getServletContext().getAttribute("jsonDatabaseDrivers");
	}
	
	public JSONArray getJsonConnectionAdapters() {
		return (JSONArray) getServletContext().getAttribute("jsonConnectionAdapters");
	}
	
	public JSONArray getJsonSecurityAdapters() {
		return (JSONArray) getServletContext().getAttribute("jsonSecurityAdapters");
	}
	
	public JSONArray getJsonControls() {
		return (JSONArray) getServletContext().getAttribute("jsonControls");
	}
	
	public JSONArray getJsonActions() {
		return (JSONArray) getServletContext().getAttribute("jsonActions");
	}
		
	public Applications getApplications() {
		return (Applications) getServletContext().getAttribute("applications");
	}
			
	/*
	public Application getApplication(String id) {
		return getApplications().get(id);
	}
	*/
	
	public String getSecureInitParameter(String name) {
		return getInitParameter(name);
	}
	
	// this is used to format between Java Date and XML date
	public SimpleDateFormat getXMLDateFormatter() {
		return (SimpleDateFormat) getServletContext().getAttribute("xmlDateFormatter");
	}
	
	// this is used to format between Java Date and XML dateTime
	public SimpleDateFormat getXMLDateTimeFormatter() {
		return (SimpleDateFormat) getServletContext().getAttribute("xmlDateTimeFormatter");
	}
	
	// this is used to format between Java Date and Local date format
	public SimpleDateFormat getLocalDateFormatter() {
		return (SimpleDateFormat) getServletContext().getAttribute("localDateFormatter");
	}
	
	// this is used to format between Java Date and local dateTime format (used by backups and page lock)
	public SimpleDateFormat getLocalDateTimeFormatter() {
		return (SimpleDateFormat) getServletContext().getAttribute("localDateTimeFormatter");
	}
	
	// this is used is actions such as database and webservice to cache results for off-line demos
	public ActionCache getActionCache() {
		return (ActionCache) getServletContext().getAttribute("actionCache");
	}
	
	public void sendException(HttpServletResponse response, Exception ex) throws IOException {
		sendException(null, response, ex);
	}
		
	public void sendException(RapidRequest rapidRequest, HttpServletResponse response, Exception ex) throws IOException {
		
		response.setStatus(500);
		
		PrintWriter out = response.getWriter();		
		
		out.println("Error : " + ex.getLocalizedMessage());
						
		boolean showStackTrace = Boolean.parseBoolean(getServletContext().getInitParameter("showStackTrace"));
				
		if (showStackTrace) {
			
			String stackTrace = "";
			
			if (rapidRequest != null) stackTrace = rapidRequest.getDetails() + "\n\n";
			
			stackTrace += ex.getClass().getName() + "\n\n";
			
			for (StackTraceElement element : ex.getStackTrace()) stackTrace += element + "\n";
						
			out.print(stackTrace);
		
		}
		
		if (rapidRequest == null) {
			
			_logger.error(ex);
						
		} else {
			
			_logger.error(ex.getLocalizedMessage() + "\n" + rapidRequest.getDetails(), ex);
						
		}
		
		out.close();
				
	}
	
	public void sendMessage(RapidRequest rapidRequest, HttpServletResponse response, int status, String message ) throws IOException {
		
		response.setStatus(status);
		
		PrintWriter out = response.getWriter();		
		
		out.println(message);
										
		out.close();
		
	}
	
}
