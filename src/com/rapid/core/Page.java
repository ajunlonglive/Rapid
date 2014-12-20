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

package com.rapid.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.rapid.core.Application.Resource;
import com.rapid.security.SecurityAdapater;
import com.rapid.security.SecurityAdapater.SecurityAdapaterException;
import com.rapid.security.SecurityAdapater.User;
import com.rapid.server.Rapid;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Files;
import com.rapid.utils.Html;
import com.rapid.utils.Minify;
import com.rapid.utils.XML;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Page {
	
	// the version of this class's xml structure when marshelled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)	
	public static final int XML_VERSION = 1;
			
	// a class for retaining page html for a set of user roles	
	public static class RoleHtml {
		
		// instance variables
		
		private List<String> _roles;
		private String _html;
		
		// properties
		
		public List<String> getRoles() { return _roles; }
		public void setRoles(List<String> roles) { _roles = roles; }
		
		public String getHtml() { return _html; }
		public void setHtml(String html) { _html = html; }
		
		// constructors
		public RoleHtml() {}
		
		public RoleHtml(List<String> roles, String html) {
			_roles = roles;
			_html = html;
		}
				
	}
	
	// details of a lock that might be on this page
	public static class Lock {
		
		private String _userName, _userDescription;
		private Date _dateTime;
		
		public String getUserName() { return _userName; }
		public void setUserName(String userName) { _userName = userName; }
		
		public String getUserDescription() { return _userDescription; }
		public void setUserDescription(String userDescription) { _userDescription = userDescription; }
		
		public Date getDateTime() { return _dateTime; }
		public void setDateTime(Date dateTime) { _dateTime = dateTime; }
		
		// constructors
		
		public Lock() {}
		
		public Lock(String userName, String userDescription, Date dateTime) {
			_userName = userName;
			_userDescription = userDescription;
			_dateTime = dateTime;
		}
		
	}
			
	// instance variables
	
	private int _xmlVersion;
	private String _id, _name, _title, _description, _createdBy, _modifiedBy, _htmlBody, _cachedStartHtml;
	private Date _createdDate, _modifiedDate;	
	private List<Control> _controls;
	private List<Event> _events;
	private List<Style> _styles;
	private List<String> _sessionVariables;
	private List<String> _roles;
	private List<RoleHtml> _rolesHtml;
	private Lock _lock;
		
	// this array is used to collect all of the lines needed in the pageload before sorting them
	private List<String> _pageloadLines;
	
	// properties
	
	// the xml version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }
		
	// the id uniquely identifies the page (it is quiet short and is concatinated to control id's so more than one page's control's can be working in a document at one time)
	public String getId() { return _id; }
	public void setId(String id) { _id = id; }
	
	// this is expected to be short name, probably even a code that is used by users to simply identify pages (also becomes the file name)
	public String getName() { return _name; }
	public void setName(String name) { _name = name; }

	// this is a user-friendly, long title
	public String getTitle() { return _title; }
	public void setTitle(String title) { _title = title; }
	
	// an even longer description of what this page does
	public String getDescription() { return _description; }
	public void setDescription(String description) { _description = description; }
	
	// the user that created this page (or archived page)
	public String getCreatedBy() { return _createdBy; }
	public void setCreatedBy(String createdBy) { _createdBy = createdBy; }
	
	// the date this page (or archive) was created
	public Date getCreatedDate() { return _createdDate; }
	public void setCreatedDate(Date createdDate) { _createdDate = createdDate; }
	
	// the last user to save this application 
	public String getModifiedBy() { return _modifiedBy; }
	public void setModifiedBy(String modifiedBy) { _modifiedBy = modifiedBy; }
	
	// the date this application was last saved
	public Date getModifiedDate() { return _modifiedDate; }
	public void setModifiedDate(Date modifiedDate) { _modifiedDate = modifiedDate; }
		
	// the html for this page
	public String getHtmlBody() { return _htmlBody; }
	public void setHtmlBody(String htmlBody) { _htmlBody = htmlBody; }
	
	// the child controls of the page
	public List<Control> getControls() { return _controls; }
	public void setControls(List<Control> controls) { _controls = controls; }
	
	// the page events and actions
	public List<Event> getEvents() { return _events; }	
	public void setEvents(List<Event> events) { _events = events; }
	
	// the page styles
	public List<Style> getStyles() { return _styles; }	
	public void setStyles(List<Style> styles) { _styles = styles; }
	
	// session variables used by this page (navigation actions are expected to pass them in)
	public List<String> getSessionVariables() { return _sessionVariables; }	
	public void setSessionVariables(List<String> sessionVariables) { _sessionVariables = sessionVariables; }
	
	// the roles required to view this page
	public List<String> getRoles() { return _roles; }
	public void setRoles(List<String> roles) { _roles = roles; }
		
	// session variables used by this page (navigation actions are expected to pass them in)
	public List<RoleHtml> getRolesHtml() { return _rolesHtml; }	
	public void setRolesHtml(List<RoleHtml> rolesHtml) { _rolesHtml = rolesHtml; }
	
	// any lock that might be on this page
	public Lock getLock() { return _lock; }
	public void setLock(Lock lock) { _lock = lock; }
		
	// constructor
	
	public Page() {
		// set the xml version
		_xmlVersion = XML_VERSION;				
	};
		
	// instance methods
	
	public String getFile(ServletContext servletContext, Application application) {
		return application.getConfigFolder(servletContext) + "/" + "/pages/" + Files.safeName(_name) + ".page.xml";
	}
	
	public String getCSSFile(ServletContext servletContext, Application application, boolean min) {
		if (min) {
			return application.getWebFolder(servletContext) + "/" + Files.safeName(_name) + ".min.css";
		} else {
			return application.getWebFolder(servletContext) + "/" + Files.safeName(_name) + ".css";
		}
	}
	
	// these two methods have different names to avoid being marshelled to the .xml file by JAXB
	public String getHtmlHeadCached(RapidHttpServlet rapidServlet, Application application) throws JSONException {		
		// check whether the page has been cached yet
		if (_cachedStartHtml == null) {
			// generate the page start html
			_cachedStartHtml = getHtmlHead(rapidServlet, application, false, null);																		
			// have the page cache the generated html for next time
			cacheHtmlHead(_cachedStartHtml);
		}				
		return _cachedStartHtml;
	}
	public void cacheHtmlHead(String html) {
		_cachedStartHtml = html;
	}
	
	public void addControl(Control control) {
		if (_controls == null) _controls = new ArrayList<Control>();
		_controls.add(control);
	}
	
	public Control getControl(int index) {
		if (_controls == null) return null;
		return _controls.get(index);
	}
	
	// an iterative function for tree-walking child controls when searching for one
	public Control getChildControl(List<Control> controls, String controlId) {
		Control foundControl = null;
		if (controls != null) {
			for (Control control : controls) {
				if (controlId.equals(control.getId())) {
					foundControl = control;
					break;
				} else {
					foundControl = getChildControl(control.getChildControls(), controlId);
					if (foundControl != null) break;
				}
			} 
		}
		return foundControl;
	}
	
	// uses the tree walking function above to find a particular control
	public Control getControl(String id) {		
		return getChildControl(_controls, id);
	}
	
	public void getChildControls(List<Control> controls, List<Control> childControls) {
		if (controls != null) {
			for (Control control : controls) {
				childControls.add(control);
				getChildControls(control.getChildControls(), childControls);
			}
		}
	}
	
	public List<Control> getAllControls() {
		ArrayList<Control> controls = new ArrayList<Control>();
		getChildControls(_controls, controls);
		return controls;
	}
	
	public Action getChildAction(List<Action> actions, String actionId) {
		Action foundAction = null;
		if (actions != null) {
			for (Action action : actions) {
				if (actionId.equals(action.getId())) return action;
				foundAction = getChildAction(action.getChildActions(), actionId);
				if (foundAction != null) return foundAction;
			}
		}
		return foundAction;
	}
	
	// an iterative function for tree-walking child controls when searching for a specific action
	public Action getChildControlAction(List<Control> controls, String actionId) {
		Action foundAction = null;
		if (controls != null) {
			for (Control control : controls) {
				if (control.getEvents() != null) {
					for (Event event : control.getEvents()) {
						if (event.getActions() != null) {
							foundAction = getChildAction(event.getActions(), actionId);
							if (foundAction != null) return foundAction;
						}
					}
				}
				foundAction = getChildControlAction(control.getChildControls(), actionId);
				if (foundAction != null) break;
			} 
		}
		return foundAction;
	}
	
	// find an action in the page by its id
	public Action getAction(String id) {
		// check the page actions first
		if (_events != null) {
			for (Event event : _events) {
				if (event.getActions() != null) {
					Action action = getChildAction(event.getActions(), id);
					if (action != null) return action;
				}
			}
		}
		// uses the tree walking function above to the find a particular action
		return getChildControlAction(_controls, id);
	}
	
	// recursively append to a list of actions from an action and it's children
	public void getChildActions(List<Action> actions, Action action) {
		// add this one action
		actions.add(action);
		// check there are child actions
		if (action.getChildActions() != null) {
			// loop them
			for (Action childAction : action.getChildActions()) {
				// add their actions too
				getChildActions(actions, childAction);
			}
		}
	}
	
	// recursively append to a list of actions from a control and it's children
	public void getChildActions(List<Action> actions, Control control) {
		
		if ("P9_C28_".equals(control.getId())) {
			boolean test = true;
		}
		
		// check this control has events
		if (control.getEvents() != null) {
			for (Event event : control.getEvents()) {
				// add any actions to the list
				if (event.getActions() != null) {
					// loop the actions
					for (Action action : event.getActions()) {						
						// add any child actions too
						getChildActions(actions, action);
					}
				}
			}
		}	
		// check if we have any child controls
		if (control.getChildControls() != null) {
			// loop the child controls
			for (Control childControl : control.getChildControls()) {
				// add their actions too
				getChildActions(actions, childControl);
			}
		}
	}
	
	// get all actions in the page
	public List<Action> getActions() {
		// instantiate the list we're going to return
		List<Action> actions = new ArrayList<Action>();
		// check the page events first
		if (_events != null) {
			for (Event event : _events) {
				// add all event actions if not null
				if (event.getActions() != null) actions.addAll(event.getActions());
			}
		}		
		// uses the tree walking function above to add all actions
		if (_controls != null) {
			for (Control control : _controls) {
				getChildActions(actions, control);
			}
		}		
		// sort them by action id
		Collections.sort(actions, new Comparator<Action>() {
			@Override
			public int compare(Action obj1, Action obj2) {
				if (obj1 == null) return -1;
				if (obj2 == null) return 1;
				if (obj1.equals(obj2)) return 0;
				String id1 = obj1.getId();
				String id2 = obj2.getId();
				if (id1 == null) return -1;
				if (id2 == null) return -1;
				id1 = id1.replace("_", "");
				id2 = id2.replace("_", "");
				int pos = id1.indexOf("A");
				if (pos < 0) return -1;
				id1 = id1.substring(pos + 1);
				pos = id2.indexOf("A");
				if (pos < 0) return 1;
				id2 = id2.substring(pos + 1);
				return (Integer.parseInt(id1) - Integer.parseInt(id2));
			}			
		});
		return actions;		
	}
	
	// an iterative function for tree-walking child controls when searching for a specific action's control
	public Control getChildControlActionControl(List<Control> controls, String actionId) {
		Control foundControl = null;
		if (controls != null) {
			for (Control control : controls) {
				if (control.getEvents() != null) {
					for (Event event : control.getEvents()) {
						if (event.getActions() != null) {
							for (Action action : event.getActions()) {
								if (actionId.equals(action.getId())) return control;
							}
						}
					}
				}
				foundControl = getChildControlActionControl(control.getChildControls(), actionId);
				if (foundControl != null) break;
			} 
		}
		return foundControl;
	}
	
	// find an action in the page by its id
	public Control getActionControl(String actionId) {
		// uses the tree walking function above to the find a particular action
		return getChildControlActionControl(_controls, actionId);
	}
	
	// an iterative function for tree-walking child controls when searching for a specific action's control
	public Event getChildControlActionEvent(List<Control> controls, String actionId) {
		Event foundEvent = null;
		if (controls != null) {
			for (Control control : controls) {
				if (control.getEvents() != null) {
					for (Event event : control.getEvents()) {
						if (event.getActions() != null) {
							for (Action action : event.getActions()) {
								if (actionId.equals(action.getId())) return event;
							}
						}
					}
				}
				foundEvent = getChildControlActionEvent(control.getChildControls(), actionId);
				if (foundEvent != null) break;
			} 
		}
		return foundEvent;
	}
	
	// find an action in the page by its id
	public Event getActionEvent(String actionId) {
		// check the page actions first
		if (_events != null) {
			for (Event event : _events) {
				if (event.getActions() != null) {
					for (Action action : event.getActions()) {
						if (actionId.equals(action.getId())) return event;
					}
				}
			}
		}		
		// uses the tree walking function above to the find a particular action
		return getChildControlActionEvent(_controls, actionId);
	}
			
	// iterative function for building a flat JSONArray of controls that can be used on other pages
	private void getOtherPageChildControls(RapidHttpServlet rapidServlet, JSONArray jsonControls, List<Control> controls) throws JSONException {
		// check we were given some controls
		if (controls != null) {
			// loop the controls
			for (Control control : controls) {
				// if this control can be used from other pages
				if (control.getCanBeUsedFromOtherPages()) {
					
					// get the control details
					JSONObject jsonControlClass = rapidServlet.getJsonControl(control.getType());
					
					// check we got one
					if (jsonControlClass != null) {
					
						// make a JSON object with what we need about this control
						JSONObject jsonControl = new JSONObject();
						jsonControl.put("id", control.getId());
						jsonControl.put("type", control.getType());
						jsonControl.put("name", control.getName());
						if (jsonControlClass.optString("getDataFunction", null) != null) jsonControl.put("input", true);
						if (jsonControlClass.optString("setDataJavaScript", null) != null) jsonControl.put("output", true);
						
						// look for any runtimeProperties
						JSONObject jsonProperty = jsonControlClass.optJSONObject("runtimeProperties");
						// if we got some
						if (jsonProperty != null) {
							// create an array to hold the properties
							JSONArray jsonRunTimeProperties = new JSONArray();
							// look for an array too
							JSONArray jsonProperties = jsonProperty.optJSONArray("runtimeProperty");
							// assume
							int index = 0;
							int count = 0;
							// if an array 
							if (jsonProperties != null) {
								// get the first item
								jsonProperty = jsonProperties.getJSONObject(index);
								// set the count
								count = jsonProperties.length();
							}
							
							// do once and loop until no more left 
							do {
								
								// create a json object for this runtime property
								JSONObject jsonRuntimeProperty = new JSONObject();
								jsonRuntimeProperty.put("type", jsonProperty.get("type"));
								jsonRuntimeProperty.put("name", jsonProperty.get("name"));
								if (jsonProperty.optString("getPropertyFunction", null) != null) jsonRuntimeProperty.put("input", true);
								if (jsonProperty.optString("setPropertyJavaScript", null) != null) jsonRuntimeProperty.put("output", true);
								
								// add to the collection
								jsonRunTimeProperties.put(jsonRuntimeProperty);
								
								// increment the index
								index ++;
								
								// get the next item if there's one there
								if (index < count) jsonProperty = jsonProperties.getJSONObject(index);
								
							} while (index < count);
							// add the properties to what we're returning
							jsonControl.put("runtimeProperties", jsonRunTimeProperties);
						}
						
						// add it to the collection we are returning
						jsonControls.put(jsonControl);
						
					}
				}
				// run for any child controls
				getOtherPageChildControls(rapidServlet, jsonControls, control.getChildControls());				
			}			
		}
	}
		
	// uses the above iterative method to return a flat array of controls in this page that can be used from other pages, for use in the designer
	public JSONArray getOtherPageControls(RapidHttpServlet rapidServlet) throws JSONException {
		// the array we're about to return
		JSONArray jsonControls = new JSONArray();
		// start building the array using the page controls
		getOtherPageChildControls(rapidServlet, jsonControls, _controls);
		// return the controls
		return jsonControls;		
	}
	
	// used to turn either a page or control style into text for the css file
	public String getStyleCSS(Style style) {
		// start the text we are going to return
		String css = "";
		// get the style rules
		ArrayList<String> rules = style.getRules();
		// check we have some
		if (rules != null) {
			if (rules.size() > 0) {
				// add the style
				css = style.getAppliesTo().trim() + " {\n";
				// check we have 
				// loop and add the rules
				for (String rule : rules) {
					css += "\t" + rule.trim() + "\n";
				}
				css += "}\n\n";
			}
		}
		// return the css
		return css;
	}
	
	// an iterative function for tree-walking child controls when building the page styles
	public void getChildControlStyles(List<Control> controls, StringBuilder stringBuilder) {
		if (controls != null) {
			for (Control control : controls) {
				// look for styles
				ArrayList<Style> controlStyles = control.getStyles();
				if (controlStyles != null) {
					// loop the styles
					for (Style style  : controlStyles) {						
						// get some nice text for the css
						stringBuilder.append(getStyleCSS(style));
					}
				}
				// try and call on any child controls
				getChildControlStyles(control.getChildControls(), stringBuilder);
			} 
		}
	}
				
	public String getAllStyles() {
		// the stringbuilder we're going to use
		StringBuilder stringBuilder = new StringBuilder();
		// check if the page has styles
		if (_styles != null) {
			// loop
			for (Style style: _styles) {
				stringBuilder.append(getStyleCSS(style));
			}
		}
		// use the iterative tree-walking function to add all of the control styles
		getChildControlStyles(_controls, stringBuilder);		
		
		// return it
		return stringBuilder.toString();		
	}
	
	// removes the page lock if it is more than 1 hour old
	public void checkLock() {
		// only check if there is one present
		if (_lock != null) {
			// get the time now
			Date now = new Date();
			// get the time an hour after the lock time
			Date lockExpiry = new Date(_lock.getDateTime().getTime() + 1000 * 60 * 60);
			// if the lock expiry has passed set the lock to null;
			if (now.after(lockExpiry)) _lock = null;
		}
	}
								
	public void backup(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, Application application, File pageFile) throws IOException {
					
		// get the user name
		String userName = Files.safeName(rapidRequest.getUserName());		
		
		// create folders to archive the pages
		String archivePath = application.getBackupFolder(rapidServlet.getServletContext());		
		File archiveFolder = new File(archivePath);		
		if (!archiveFolder.exists()) archiveFolder.mkdirs();
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateString = formatter.format(new Date());
		
		 // create a file object for the archive file
	 	File archiveFile = new File(archivePath + "/" + Files.safeName(_name) + "_" + dateString + "_" + userName + ".page.xml");
	 	
	 	// copy the existing new file to the archive file
	    Files.copyFile(pageFile, archiveFile);
		
	}
	
	public void deleteBackup(RapidHttpServlet rapidServlet, Application application, String backupId) {
		
		// create the path
		String backupPath = application.getBackupFolder(rapidServlet.getServletContext()) + "/" + backupId;
		// create the file
		File backupFile = new File(backupPath);
		// delete 
		Files.deleteRecurring(backupFile);
		
	}
			
	public void saveCSSFiles(ServletContext servletContext, Application application) throws IOException {

		// get the css
		String css = getAllStyles();
		
		// get the file
		String cssFile = getCSSFile(servletContext, application, false);
		
		// set the fos to the css file
		FileOutputStream fos = new FileOutputStream(cssFile);
		
		// get a print stream
		PrintStream ps = new PrintStream(fos);		
		ps.print("\n/* This file is auto-generated on page save */\n\n");
		ps.print(css);
		// close
		ps.close();
		fos.close();	
		
		// get the min file
		String cssFileMin = getCSSFile(servletContext, application, true);
		
		// minify
		Minify.toFile(css, cssFileMin, Minify.CSS);
	}
	
	public void save(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, Application application, boolean backup) throws JAXBException, IOException {
		
		// create folders to save the pages
		String pagePath = application.getConfigFolder(rapidServlet.getServletContext()) + "/pages";		
		File pageFolder = new File(pagePath);		
		if (!pageFolder.exists()) pageFolder.mkdirs();
										
		// create a file object for the new file
	 	File newFile = new File(pagePath + "/" + Files.safeName(getName()) + ".page.xml");
	 	
	 	// if we want a backup and the new file already exists it needs archiving
	 	if (backup && newFile.exists()) backup(rapidServlet, rapidRequest, application, newFile);	 			 	
				
	 	// create a file for the temp file
	    File tempFile = new File(pagePath + "/" + Files.safeName(getName()) + "-saving.page.xml");	
	    
	    // update the modified by and date
	    _modifiedBy = rapidRequest.getUserName();
	    _modifiedDate = new Date();
				
		// get a buffered writer for our page with UTF-8 file format
		BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(tempFile), "UTF-8"));
		
		try {
			// marshall the page object into the temp file
			rapidServlet.getMarshaller().marshal(this, bw);
		} catch (JAXBException ex) {
			// close the file writer
		    bw.close();
		    // re throw the exception
		    throw ex;
		}

		// close the file writer
	    bw.close();
	    	        	    	    	    	 	 		   	 	
	 	// copy the tempFile to the newFile
	    Files.copyFile(tempFile, newFile);
	    
	    // delete the temp file
	    tempFile.delete();
	    	    	  	    
		// replace the old page with the new page
		application.addPage(this);
		
		// empty the cached header html
		_cachedStartHtml = null;
		
		// re-create the css files too
		saveCSSFiles(rapidServlet.getServletContext(), application);
				
	}
	
	public void delete(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, Application application) throws JAXBException, IOException {
						
		// create folders to delete the page
		String pagePath = application.getConfigFolder(rapidServlet.getServletContext()) + "/pages";		
														
		// create a file object for the delete file
	 	File delFile = new File(pagePath + "/" + Files.safeName(getName()) + ".page.xml");
	 	
	 	// if the new file already exists it needs archiving
	 	if (delFile.exists()) {
	 		// archive the page file
	 		backup(rapidServlet, rapidRequest, application, delFile);
	 		// delete the page file
	 		delFile.delete();
	 		// remove it from the current list of pages
		 	application.removePage(_id);
	 	}
	 	
	 	// get the resources path
	 	String resourcesPath = application.getWebFolder(rapidServlet.getServletContext());		
	 	
	 	// create a file object for deleting the page css file
	 	File delCssFile = new File(resourcesPath + "/" + Files.safeName(getName()) + ".css");
	 	
	 	// delete if it exists
	 	if (delCssFile.exists()) delCssFile.delete();
	 	
	 	// create a file object for deleting the page css file
	 	File delCssFileMin = new File(resourcesPath + "/" + Files.safeName(getName()) + ".min.css");
	 	 	
	 	// delete if it exists
	 	if (delCssFileMin.exists()) delCssFileMin.delete();
	 		 				
	}
	
	// this includes functions to iteratively call any control initJavaScript and set up any event listeners
    private void getPageLoadLines(List<String> pageloadLines, List<Control> controls) throws JSONException {
    	if (controls != null) {    		
    		// loop controls
    		for (Control control : controls) {
    			// check for any initJavaScript to call
    			if (control.hasInitJavaScript()) {
    				// get any details we may have
					String details = control.getDetails();
					// set to empty string or clean up
					if (details == null) {
						details = "";
					} else {
						details = ", " + control.getId() + "details";
					}    				
    				// write an init call method
    				pageloadLines.add("Init_" + control.getType() + "('" + control.getId() + "'" + details + ");\n");
    			}
    			// check event actions
    			if (control.getEvents() != null) {
    				// loop events
    				for (Event event : control.getEvents()) {
    					// only if event is non-custome and there are actually some actions to invoke
    					if (!event.isCustomType() && event.getActions() != null) {
    						if (event.getActions().size() > 0) {
    							pageloadLines.add(event.getPageLoadJavaScript(control));
    						}
    					}    					
    				}
    			}
    			// now call iteratively for child controls (of this [child] control, etc.)
    			if (control.getChildControls() != null) getPageLoadLines(pageloadLines, control.getChildControls());     				
    		}    		
    	}    	
    }
    
    // the resources for the page, and whether we want the css (we might like to override it in the designer or for no permission, etc)
    public String getResourcesHtml(Application application, boolean includeRapidCss) {
    	
    	StringBuilder stringBuilder = new StringBuilder();
    	
    	// manage the resources links added already so we don't add twice
    	ArrayList<String> addedLinks = new ArrayList<String>(); 
				
		// loop and add the resources required by this application's controls and actions (created when application loads)
		if (application.getResources() != null) {
			for (Resource resource : application.getResources()) {
				// the link we're hoping to get
				String link = null;
				// set the link according to the type
				switch (resource.getType()) {
					case Resource.JAVASCRIPTFILE : case Resource.JAVASCRIPTLINK :
						link = "<script type='text/javascript' src='" + resource.getContent() + "'></script>";
					break;
					case Resource.CSSFILE : case Resource.CSSLINK :
						link = "<link rel='stylesheet' type='text/css' href='" + resource.getContent() + "'></link>";
					break;
				}				
				// if we got a link and don't have it already 
				if (link != null && !addedLinks.contains(link)) {
					// append it
					stringBuilder.append(link + "\n");
					// rememeber it
					addedLinks.add(link);
				}
			}
		}
		
		// only if css is to be included
		if (includeRapidCss) {
			// check the application status and include this page's css file
			if (application.getStatus() == Application.STATUS_LIVE) {
				stringBuilder.append("<link rel='stylesheet' type='text/css' href='" + Application.getWebFolder(application) + "/" + _name + ".min.css'></link>\n");
			} else {
				stringBuilder.append("<link rel='stylesheet' type='text/css' href='" + Application.getWebFolder(application) + "/" + _name + ".css'></link>\n");
			}
		}
				
		return stringBuilder.toString();
    	
    }
          
    private void getEventJavaScriptFunction(RapidHttpServlet rapidServlet, StringBuilder stringBuilder, Application application, Control control, Event event) {
    	// check actions are initialised
		if (event.getActions() != null) {
			// check there are some to loop
			if (event.getActions().size() > 0) {				
				// create actions separately to avoid redundancy
				StringBuilder actionStringBuilder = new StringBuilder();
				StringBuilder eventStringBuilder = new StringBuilder();
								
				// start the function name
				String functionName = "Event_" + event.getType() + "_";
				// if this is the page (no control) use the page id, otherwise use the controlId
				if (control == null) {
					// append the page id
					functionName += _id;
				} else {					
					// append the control id
					functionName += control.getId();
				}
				
				// create a function for running the actions for this controls events
				eventStringBuilder.append("function " + functionName + "(ev) {\n");
				// open a try/catch
				eventStringBuilder.append("  try {\n");
				
				// get any filter javascript
				String filter = event.getFilter();
				// if we have any add it now
				if (filter != null) {
					// only bother if not an empty string
					if (!"".equals(filter)) {
						eventStringBuilder.append("    " + filter.trim().replace("\n", "    \n") + "\n");
					}
				}
				
				// loop the actions and produce the handling JavaScript
				for (Action action : event.getActions()) {
					
					try {						
						// get the action client-side java script from the action object (it's generated there as it can contain values stored in the object on the server side)
						String actionJavaScript = action.getJavaScript(rapidServlet, application, this, control, null);
						// if non null
						if (actionJavaScript != null) {
							// trim it to avoid tabs and line breaks that might sneak in
							actionJavaScript = actionJavaScript.trim();
							// only if what we got is not an empty string
							if (!("").equals(actionJavaScript)) {
								// if this action has been marked for redundancy avoidance 
								if (action.getAvoidRedundancy()) {
									// add the action function to the action stringbuilder so it's before the event
									actionStringBuilder.append("function Action_" + action.getId() + "(ev) {\n" 
									+ "  " + actionJavaScript.trim().replace("\n", "\n  ") + "\n"
									+ "}\n\n");	
									// add an action function call to the event string builder
									eventStringBuilder.append("    Action_" + action.getId() + "(ev);\n");																
								} else {
									// go straight into the event
									eventStringBuilder.append("    " + actionJavaScript.trim().replace("\n", "\n    ") + "\n");
								}													
							}
							
						}  
						
					} catch (Exception ex) {
						
						// print a commented message
						eventStringBuilder.append("//    Error creating JavaScript for action " + action.getId() + " : " + ex.getMessage() + "\n");
						
					}
					  							
				}
				// close the try/catch
				if (control == null) {
					// page
					eventStringBuilder.append("  } catch(ex) { Event_error('" + event.getType() + "',null,ex); }\n");
				} else {
					// control
					eventStringBuilder.append("  } catch(ex) { Event_error('" + event.getType() + "','" + control.getId() +  "',ex); }\n");
				}				
				// close event function
				eventStringBuilder.append("}\n\n");
				
				// add the action functions
				stringBuilder.append(actionStringBuilder);
				
				// add the event function
				stringBuilder.append(eventStringBuilder);
			}    						
		}
    }
            
    // build the event handling page JavaScript iteratively
    private void getEventHandlersJavaScript(RapidHttpServlet rapidServlet, StringBuilder stringBuilder, Application application, List<Control> controls) throws JSONException {
    	// check there are some controls    			
    	if (controls != null) {    		
			// if we're at the root of the page
    		if (controls.equals(_controls)) {    			
    			// check for page events
    			if (_events != null) {
    				// loop page events and get js functions
        			for (Event event : _events) getEventJavaScriptFunction(rapidServlet, stringBuilder, application, null, event);        			
    			}    			
    		}
    		for (Control control : controls) {
    			// check event actions
    			if (control.getEvents() != null) {
    				// loop page events and get js functions
    				for (Event event : control.getEvents()) getEventJavaScriptFunction(rapidServlet, stringBuilder, application, control, event);    					    				
    			}
    			// now call iteratively for child controls (of this [child] control, etc.)
    			if (control.getChildControls() != null) getEventHandlersJavaScript(rapidServlet, stringBuilder, application, control.getChildControls());     				
    		}    		 		
    	}    	
    }
	
    // this private method produces the head of the page which is often cached, if resourcesOnly is true only page resources are included which is used when sending no permission
	private String getHtmlHead(RapidHttpServlet rapidServlet, Application application, boolean includeJSandCSS, String userName) throws JSONException {
    	
    	StringBuilder stringBuilder = new StringBuilder();
    	    												
		stringBuilder.append("  <head>\n");
		
		stringBuilder.append("    <title>" + _title + " - by Rapid</title>\n");
		
		stringBuilder.append("    <meta description=\"Created using Rapid - www.rapid-is.co.uk\"/>\n");
		
		stringBuilder.append("    <meta charset=\"utf-8\"/>\n");
		
		stringBuilder.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />\n");
						
		stringBuilder.append("    <link rel=\"icon\" href=\"favicon.ico\"></link>\n");
		
		// if you're looking for where the jquery link is added it's the first resource in the page.control.xml file	
		stringBuilder.append("    " + getResourcesHtml(application, includeJSandCSS).trim().replace("\n", "\n    ") + "\n");
												
		// if we want the full contents
		if (includeJSandCSS) {
									
			// if we have requested more than just the resources so start building the inline js for the page				
			stringBuilder.append("    <script type='text/javascript'>\n\n");
			
			stringBuilder.append("var _appId = '" + application.getId() + "';\n");
			
			stringBuilder.append("var _appVersion = '" + application.getVersion() + "';\n");
			
			stringBuilder.append("var _pageId = '" + _id + "';\n");
			
			stringBuilder.append("var _userName = '" + userName + "';\n");
			
			stringBuilder.append("var _mobileResume = false;\n");
			
			// make a new string builder just for the js (so we can minify it independently)
			StringBuilder jsStringBuilder = new StringBuilder("/*\n\n  The following code is minified for live applications\n\n*/\n\n"); 
			
			// get all controls
			List<Control> pageControls = getAllControls();
			
			// if we got some
			if (pageControls != null) {
				// loop them
				for (Control control : pageControls) {
					// get the details
					String details = control.getDetails();
					// check if null
					if (details != null) {
						// create a gloabl variable for it's details
						jsStringBuilder.append("var " + control.getId() + "details = " + details + ";\n");
					}
				}
				jsStringBuilder.append("\n");
			}
					
			// initialise our pageload lines collections
			_pageloadLines = new ArrayList<String>();
			
			// get any control initJavaScript event listeners into he pageloadLine (goes into $(document).ready function)
			getPageLoadLines(_pageloadLines, _controls);
									      					
			// sort the page load lines
			Collections.sort(_pageloadLines, new Comparator<String>() {
				@Override
				public int compare(String l1, String l2) {				
					if (l1.isEmpty()) return -1;
					if (l2.isEmpty()) return 1;
					char i1 = l1.charAt(0);
					char i2 = l2.charAt(0);
					return i2 - i1;						
				}}
			);
			
			// check for page events (this is here so all listeners are registered by now)
			if (_events != null) {
				// loop page events
    			for (Event event : _events) {        				
    				// only if there are actually some actions to invoke
					if (event.getActions() != null) {
						if (event.getActions().size() > 0) {
							// page is a special animal so we need to do each of it's event types differently
							if ("pageload".equals(event.getType())) {
								_pageloadLines.add("if (!_mobileResume) Event_pageload_" + _id + "($.Event('pageload'));\n");
	        				}    			
							// reusable action is only invoked via reusable actions on other events - there is no listener
						}
					}         				
    			}
			}  
			
			// open the page loaded function
			jsStringBuilder.append("$(document).ready( function() {\n");
			
			// add a try
			jsStringBuilder.append("  try {\n");
			
			// print any page load lines such as initialising controls
			for (String line : _pageloadLines) jsStringBuilder.append("    " + line);
															
			// close the try
			jsStringBuilder.append("  } catch(ex) { $('body').html(ex); }\n");
			
			// after 200 milliseconds show and trigger a window resize for any controls that might be listening (this also cuts out any flicker)
			jsStringBuilder.append("  window.setTimeout( function() {\n    $(window).resize();\n    $('body').css('visibility','visible');\n  }, 200);\n");
									
			// end of page loaded function
			jsStringBuilder.append("});\n\n");
							
			// find any redundant actions anywhere in the page, prior to generating JavaScript
			List<Action> pageActions = getActions();
			
			// only proceed if there are actions in this page
			if (pageActions != null) {
				
				// loop the list of actions to indentify potential redundancies before we create all the event handling JavaScript
				for (Action action : pageActions) {
					try {
						// look for any page javascript that this action may have
						String actionPageJavaScript = action.getPageJavaScript(rapidServlet, application, this, null);
						// print it here if so
						if (actionPageJavaScript != null) jsStringBuilder.append(actionPageJavaScript.trim() + "\n\n");
						// if this action adds redundancy to any others 
						if (action.getRedundantActions() != null) {
							// loop them
							for (String actionId : action.getRedundantActions()) {
								// try and find the action
								Action redundantAction = getAction(actionId);
								// if we got one
								if (redundantAction != null) {
									// update the redundancy avoidance flag
									redundantAction.avoidRedundancy(true);
								} 
							}										
						} // redundantActions != null
					} catch (Exception ex) {
						// print the exception as a comment
						jsStringBuilder.append("// Error producing page JavaScript : " + ex.getMessage() + "\n\n");
					}					
					
				} // action loop		
				
				// add event handlers, staring at the root controls
				getEventHandlersJavaScript(rapidServlet, jsStringBuilder, application, _controls);
			}
			
			// check the application status
			if (application.getStatus() == Application.STATUS_LIVE) {			
				try {
					// minify the js before adding
					stringBuilder.append(Minify.toString(jsStringBuilder.toString(),Minify.JAVASCRIPT));
				} catch (IOException ex) {
					// add the error
					stringBuilder.append("\n\n/* Failed to minify JavaScript : " + ex.getMessage() + " */\n\n");
					// add the js as is
					stringBuilder.append(jsStringBuilder);
				}
			} else {
				// add the js as is
				stringBuilder.append("\n" + jsStringBuilder.toString().trim() + "\n\n");
			}
																		
			// close the page inline script block
			stringBuilder.append("</script>\n");
			
		} else {
			
			// just add the index style sheet
			stringBuilder.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"styles/index.css\"></link>\n");
											
		} // include JavaScript and CSS
						
		// close the head
		stringBuilder.append("  </head>\n");
														
		return stringBuilder.toString();
    	
    }
		
	// this routine produces the entire page
	public void writeHtml(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, Application application, User user, Writer writer, boolean includeDesignLink) throws JSONException, IOException {
		
		// get the security
		SecurityAdapater security = application.getSecurity();
		
		// assume the user has permission to access the page
		boolean gotPagePermission = true;
		
		try {
			
			// if this page has roles
			if (_roles != null) {
				if (_roles.size() > 0) {
					// check if the user has any of them
					gotPagePermission = security.checkUserRole(rapidRequest, _roles);
				}
			}
			
		} catch (SecurityAdapaterException ex) {

			rapidServlet.getLogger().error("Error checking for page roles", ex);
			
		}
		
		
		// this doctype is necessary (amongst other things) to stop the "user agent stylesheet" overriding styles
		writer.write("<!DOCTYPE html encoding=\"utf-8\">\n");
								
		writer.write("<html>\n");
		
		if (gotPagePermission) {
		
			// whether we're rebulding the page for each request
	    	boolean rebuildPages = Boolean.parseBoolean(rapidServlet.getServletContext().getInitParameter("rebuildPages"));
			
	    	// check whether or not we rebuild
	    	if (rebuildPages) {
	    		// get the cached head html
	    		writer.write(getHtmlHead(rapidServlet, application, true, user.getName()));
	    	} else {
	    		// get the cached head html
	    		writer.write(getHtmlHeadCached(rapidServlet, application));
	    	}
					
	    	writer.write("  <body id='" + _id + "' style='visibility:hidden;'>\n");
			
			// a reference for the body html
			String bodyHtml = null;
			
			// get the users roles
			List<String> userRoles = user.getRoles();
										
			// check we have userRoles and htmlRoles
			if (userRoles != null && _rolesHtml != null) {
																	
				// loop each roles html entry
				for (RoleHtml roleHtml : _rolesHtml) {
												
					// get the roles from this combination
					List<String> roles = roleHtml.getRoles();
												
					// keep a running count for the roles we have
					int gotRoleCount = 0;
					
					// if there are roles to check
					if (roles != null) {
					
						// retain how many roles we need our user to have
						int rolesRequired = roles.size();
						
						// check whether we need any roles and that our user has any at all
						if (rolesRequired > 0) {
							// check the user has as many roles as this combination requires
							if (userRoles.size() >= rolesRequired) {
								// loop the roles we need for this combination
								for (String role : roleHtml.getRoles()) {
									// check this role
									if (userRoles.contains(role)) {
										// increment the got role count
										gotRoleCount ++;
									}
								}
							}									
						}
														
						// if we have all the roles we need
						if (gotRoleCount == rolesRequired) {
							// use this html
							bodyHtml = roleHtml.getHtml();
							// no need to check any further
							break;
						}
						
					} 
					
				}
																										
			} 
			
			// if haven't got any body html 
			if (bodyHtml == null) {
				// use the full version or empty string
				if (_htmlBody == null) {
					bodyHtml = "";
				} else {					
					bodyHtml = _htmlBody;
				}
			}
						
			// check the status of the application
			if (application.getStatus() == Application.STATUS_DEVELOPMENT) {
				// pretty print
				writer.write(Html.getPrettyHtml(bodyHtml.trim()));
			} else {
				// no pretty print
				writer.write(bodyHtml.trim());
			}
									
		} else {
			
			// write the head html without the JavaScript and CSS (index.css is substituted for us)
			writer.write(getHtmlHead(rapidServlet, application, false, null));
						
			// open the body
			writer.write("  <body>\n");
			
			// write no permission
			writer.write("<div class=\"image\"><img src=\"images/RapidLogo_200x134.png\" /></div><div class=\"title\"><span>Rapid - No permssion</span></div><div class=\"info\"><p>You do not have permssion to view this page</p></div>\n");
			
		} // page permission check		
		
		try {
			
			// only if we want to include the design link
			if (includeDesignLink) {
			
				// assume not admin link
				boolean adminLinkPermission = false;
					
				// check for the design role, super is required as well if the rapid app
				if ("rapid".equals(application.getId())) {
					if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE) && security.checkUserRole(rapidRequest, Rapid.SUPER_ROLE)) adminLinkPermission = true;
				} else {
					if (security.checkUserRole(rapidRequest, Rapid.DESIGN_ROLE)) adminLinkPermission = true;
				}
				
				// if we had the admin link
				if (adminLinkPermission) {
													
					// using attr href was the weirdest thing. Some part of jQuery seemed to be setting the url back to v=1&p=P1 when v=2&p=P2 was printed in the html
					writer.write("<div id='designShow' style='position:fixed;left:0px;bottom:0px;width:30px;height:30px;z-index:1000;'></div>\n"
			    	+ "<a id='designLink' style='position:fixed;left:6px;bottom:6px;z-index:1001;display:none;' href='#'><img src='images/gear_24x24.png' style='border:0;'/></a>\n"
			    	+ "<script type='text/javascript'>\n"
			    	+ "/* designLink */\n"
			    	+ "$(document).ready( function() {\n"
			    	+ "  $('#designShow').mouseover ( function(ev) {\n     $('#designLink').attr('href','design.jsp?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + _id + "').show();\n  });\n"
			    	+ "  $('#designLink').mouseout ( function(ev) {\n     $('#designLink').hide();\n  });\n"
			    	+ "});\n"
			    	+ "</script>\n");
											    			    	
				}
				
			}
			
		} catch (SecurityAdapaterException ex) {

			rapidServlet.getLogger().error("Error checking for the designer link", ex);
			
		}
				
		// add the remaining elements
		writer.write("  </body>\n</html>");
				
	}
		
	// static function to load a new page
	public static Page load(ServletContext servletContext, File file) throws JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
		
		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");
		
		// trace log that we're about to load a page
		logger.trace("Loading page from " + file);
		
		// open the xml file into a document
		Document pageDocument = XML.openDocument(file);
		
		// specify the xmlVersion as -1
		int xmlVersion = -1;
		
		// look for a version node
		Node xmlVersionNode = XML.getChildElement(pageDocument.getFirstChild(), "XMLVersion");
		
		// if we got one update the version
		if (xmlVersionNode != null) xmlVersion = Integer.parseInt(xmlVersionNode.getTextContent());
				
		// if the version of this xml isn't the same as this class we have some work to do!
		if (xmlVersion != XML_VERSION) {
			
			// get the page name
			String name = XML.getChildElementValue(pageDocument.getFirstChild(), "name");						
			
			// log the difference
			logger.debug("Page " + name + " with version " + xmlVersion + ", current version is " + XML_VERSION);
			
			//
			// Here we would have code to update from known versions of the file to the current version
			//
			
			// check whether there was a version node in the file to start with
			if (xmlVersionNode == null) {
				// create the version node
				xmlVersionNode = pageDocument.createElement("XMLVersion");
				// add it to the root of the document
				pageDocument.getFirstChild().appendChild(xmlVersionNode);
			}
			
			// set the xml to the latest version
			xmlVersionNode.setTextContent(Integer.toString(XML_VERSION));
			
			//
			// Here we would use xpath to find all controls and run the Control.upgrade method
			//
			
			//
			// Here we would use xpath to find all actions, each class has it's own upgrade method so
			// we need to identify the class, instantiate it and call it's upgrade method
			// it's probably worthwhile maintaining a map of instantiated classes to avoid unnecessary re-instantiation   
			//
			
			// save it
			XML.saveDocument(pageDocument, file);
			
			logger.debug("Updated " + name + " page version to " + XML_VERSION);
			
		}
		
		// get the unmarshaller from the context
		Unmarshaller unmarshaller = (Unmarshaller) servletContext.getAttribute("unmarshaller");	
		
		// get a buffered reader for our page with UTF-8 file format
		BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream(file), "UTF-8"));
		
		// try the unmarshalling
		try {
			
			// unmarshall the page
			Page page = (Page) unmarshaller.unmarshal(br);
			
			// log that the application was loaded
			logger.trace("Loaded page " + page.getId() + " - " + page.getName());
			
			// close the buffered reader
			br.close();
			
			// return the page		
			return page;
			
		} catch (JAXBException ex) {
			
			// close the buffered reader
			br.close();
			
			// re-throw
			throw ex;
			
		}
				
	}
		
}
