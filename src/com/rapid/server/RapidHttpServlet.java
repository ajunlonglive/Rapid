package com.rapid.server;

/*

This class wraps HttpServlet and provides a number of useful functions
Mostly getters that retrieve from the servlet context

 */

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.security.SecurityAdapater.User;
import com.rapid.server.filter.RapidFilter;
import com.rapid.utils.Comparators;

@SuppressWarnings({"serial", "unchecked", "rawtypes"})
public class RapidHttpServlet extends HttpServlet {
	
	private static Logger _logger = Logger.getLogger(RapidHttpServlet.class);
	
	// this class provides some utility functions for easily accessing common Rapid objects from a request
	public static class RapidRequest {
		
		private RapidHttpServlet _rapidServlet;
		private HttpServletRequest _request;
		private String _actionName;
		private Application _application;
		private Page _page;
		private Control _control;
		private Action _action;
		
		// useful get methods
		
		public RapidHttpServlet getRapidServlet() { return _rapidServlet; }
		public HttpServletRequest getRequest() { return _request; }
		public String getUserName() { return (String) _request.getSession().getAttribute(RapidFilter.SESSION_VARIABLE_USER_NAME); }
		public String getActionName() { return _actionName; }
		public Application getApplication() { return _application; }		
		public Page getPage() { return _page; }
		public Control getControl() { return _control; }
		public Action getAction() { return _action; }
		public Object getSessionAttribute(String name) { return _request.getSession().getAttribute(name); }
		
		// most likely to construct a rapidRequest from a servlet and an http request
		public RapidRequest(RapidHttpServlet rapidServlet, HttpServletRequest request) {
			_rapidServlet = rapidServlet;
			_request = request;
			_actionName = request.getParameter("action");
			// look for the application
			_application = _rapidServlet.getApplication(request.getParameter("a"));
			// if we've found the application look for the page
			if (_application != null) {
				// try and get the specified page
				_page = _application.getPage(request.getParameter("p"));
				// if no page was found resort to the start page
				if (_page == null) _page = _application.getStartPage();
				// if there is no control paramter could still have a page action
				if (request.getParameter("c") == null) {
					if (request.getParameter("act") != null) {
						// get action from the page
						_action = _page.getAction(request.getParameter("act"));						
					}
				} else {
					_control = _page.getControl(request.getParameter("c"));
					// if we've found the control and have an action parameter
					if (_control != null && request.getParameter("act") != null) {
						// get action from the control
						_action = _control.getAction(request.getParameter("act"));						
					}	
				}
				
			}
			// retain all the query string parameter values in the session
			Enumeration<String> names = _request.getParameterNames();
			while (names.hasMoreElements()) {
				// get the name
				String name = names.nextElement();
				// get any values
				String[] values = _request.getParameterValues(name);
				// store the first value
				if (values != null) _request.getSession().setAttribute(name, values[0]);
			}
		}	
								
		// can also instantiate a rapid request with just an application object (this is used by the rapid action)
		public RapidRequest(RapidHttpServlet rapidServlet, HttpServletRequest request, Application application) {
			_rapidServlet = rapidServlet;
			_request = request;
			_application = application;
		}
		
		// can also instantiate a rapid request with just an HttpServletRequest
		public RapidRequest(HttpServletRequest request) {
			_request = request;
		}
		
		// good for printing details of the Rapid request into logs and error messages
		public String getDetails() {
			
			// assume there was no application
			String details = " no application";
			
			// if there is one
			if (_application != null) {
				
				// set the details response to the application title and id
				details = " app = " + _application.getTitle() + " (" + _application.getId() + ")";
				
				// if there is a page involved
				if (_page != null) {
					
					// add the page title and id
					details += "\n page = " + _page.getTitle() + " (" + _page.getId() + ")";
					
					// check whether there was a control
					if (_control == null) {
						// no control, probably the page
						details += "\n control = page";
					} else {
						// add details of the control
						details += "\n control = " + _control.getName() + "(" + _control.getId() + ")";
					}
										
					// check whethe there was an action
					if (_action == null) {
						// no action, say so
						details += "\n action = no action";
					} else {
						// add details of the action
						details += "\n action = (" + _action.getId() + ")";
					}
											
				}
				
			}
			
			return details;
			
		}
		
	}
	
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
		
	public Map<String,Application> getApplications() {
		return (Map<String,Application>) getServletContext().getAttribute("applications");
	}
	
	public ArrayList<Application> getSortedApplications() {
		// get our applications
		Map<String,Application> applicationsMap = getApplications();
		// prepare the list we are about to sort
		ArrayList<Application> applications = new ArrayList<Application>();
		// add each application in the map to the list
		for (String applicationId : applicationsMap.keySet()) applications.add(applicationsMap.get(applicationId));
		// sort the list by application id
		Collections.sort(applications, new Comparator<Application>() {
			@Override
			public int compare(Application app1, Application app2) {
				return Comparators.AsciiCompare(app1.getId(), app2.getId());
			}			
		});				
		// return the sorted list
		return applications;
	}
	
	public Application getApplication(String id) {
		return getApplications().get(id);
	}
	
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
		
	public void sendException(HttpServletResponse response, Exception ex) throws IOException {
		sendException(null, response, ex);
	}
		
	public void sendException(RapidRequest rapidRequest, HttpServletResponse response, Exception ex) throws IOException {
		
		response.setStatus(500);
		
		PrintWriter out = response.getWriter();		
		
		out.println(ex.getLocalizedMessage());
						
		boolean showStackTrace = Boolean.parseBoolean(getServletContext().getInitParameter("showStackTrace"));
				
		if (showStackTrace) {
			
			String stackTrace = "";
			
			if (rapidRequest != null) stackTrace = rapidRequest.getDetails() + "\n\n";
			
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
	
}
