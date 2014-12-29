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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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

import com.rapid.core.Page.Lock;
import com.rapid.data.ConnectionAdapter;
import com.rapid.security.RapidSecurityAdapter;
import com.rapid.security.SecurityAdapater;
import com.rapid.security.SecurityAdapater.User;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.soa.Webservice;
import com.rapid.utils.Comparators;
import com.rapid.utils.Files;
import com.rapid.utils.JAXB;
import com.rapid.utils.Minify;
import com.rapid.utils.Strings;
import com.rapid.utils.XML;
import com.rapid.utils.ZipFile;
import com.rapid.utils.ZipFile.ZipSource;
import com.rapid.utils.ZipFile.ZipSources;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Application {
	
	// the version of this class's xml structure when marshalled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)	
	public static final int XML_VERSION = 1;

	// application version status in development
	public static final int STATUS_DEVELOPMENT = 0;
	// application version status live
	public static final int STATUS_LIVE = 1;
	
	// the name of the folder in which to store backups
	public static final String BACKUP_FOLDER = "_backups";
	
	// public static classes
	
	// an exception class when loading
	public static class RapidLoadingException extends Exception {

		private static final long serialVersionUID = 5010L;
		
		private String _message;
		private Exception _exception;
		
		public RapidLoadingException(String message, Exception ex) {
			_message = message + " - " + ex.getMessage();
			_exception = ex;
			// clean up the jaxb suggestion
			if (_message.contains(". Did you mean")) {
				_message = _message.substring(0, _message.indexOf(". Did you mean"));
			}
		}

		@Override
		public String getMessage() {
			return _message;
		}

		@Override
		public StackTraceElement[] getStackTrace() {
			return _exception.getStackTrace();
		}
		
	}
	
	// the details of a database connection (WebService is defined in its own class as its extendable)
	public static class DatabaseConnection {
		
		// instance variables
		
		String _name, _driverClass, _connectionString, _connectionAdapterClass, _userName, _password;		
		ConnectionAdapter _connectionAdapter;
		
		// properties
		
		public String getName() { return _name; }
		public void setName(String name) { _name = name; }
		
		public String getDriverClass() { return _driverClass; }
		public void setDriverClass(String driverClass) { _driverClass = driverClass; }
		
		public String getConnectionString() { return _connectionString; }
		public void setConnectionString(String connectionString) { _connectionString = connectionString; }
		
		public String getConnectionAdapterClass() { return _connectionAdapterClass; }
		public void setConnectionAdapterClass(String connectionAdapterClass) { _connectionAdapterClass = connectionAdapterClass; }
		
		public String getUserName() { return _userName; }
		public void setUserName(String userName) { _userName = userName; }
		
		@XmlJavaTypeAdapter( JAXB.SecureAdapter.class )
		public String getPassword() { return _password; }
		public void setPassword(String password) { _password = password; }
		
		// constructors
		public DatabaseConnection() {};
		public DatabaseConnection(String name, String driverClass, String connectionString, String connectionAdapterClass, String userName, String password) {
			_name = name;
			_driverClass = driverClass;
			_connectionString = connectionString;
			_connectionAdapterClass = connectionAdapterClass;	
			_userName = userName;
			_password = password;
		}
		
		// instance methods
		
		// get the connection adapter, instantiating if null as this is quite expensive
		public synchronized ConnectionAdapter getConnectionAdapter(ServletContext servletContext) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
			
			// only if the connection adapter has not already been initialised
			if (_connectionAdapter == null) {
				// get our class
				Class classClass = Class.forName(_connectionAdapterClass);
				// initialise a constructor
				Constructor constructor = classClass.getConstructor(ServletContext.class, String.class, String.class, String.class, String.class);
				// initialise the class
				_connectionAdapter = (ConnectionAdapter) constructor.newInstance(servletContext, _driverClass, _connectionString, _userName, _password) ;
			}
			
			return _connectionAdapter;
			
		}
		
		// set the connection adapter to null to for it to be re-initialised
		public synchronized void reset() {
			_connectionAdapter = null;
		}
		
	}
	
	// application parameters which will can access in some of our actions
	@XmlType(namespace="http://rapid-is.co.uk/core")
	public static class Parameter {
		
		// private instance variables
		
		private String _name, _value;
		
		// properties
		
		public String getName() { return _name; }
		public void setName(String name) { _name = name; }
		
		public String getValue() { return _value; }
		public void setValue(String value) { _value = value; }
		
		// constructor
		public Parameter() {
			_name = "";
			_value = "";
		}
		
	}
	
	// application and page backups
	public static class Backup {
		
		private String _id, _name, _user, _size;
		private Date _date;

		public String getId() {	return _id;	}
		
		public String getName() { return _name; }

		public Date getDate() { return _date; }

		public String getUser() { return _user; }

		public String getSize() { return _size;	}
		
		public Backup(String id, Date date, String user, String size) {
			_id = id;
			_date = date;
			_user = user;
			_size = size;
		}
		
		public Backup(String id, String name, Date date, String user, String size) {
			_id = id;
			_name = name;
			_date = date;
			_user = user;
			_size = size;
		}
					
	}
		
	// the resource is specified in the control or action xml files
	public static class Resource {
		
		// these are the types defined in the control and action .xsd files
		public static final int JAVASCRIPT = 1;
		public static final int CSS = 2;
		public static final int JAVASCRIPTFILE = 3;
		public static final int CSSFILE = 4;				
		public static final int JAVASCRIPTLINK = 5;
		public static final int CSSLINK = 6;
		public static final int FILE = 7;
		
		// private instance variables
		private int _type;
		private String _content;
		
		// properties
		public int getType() { return _type; }		
		public String getContent() { return _content; }
		public void setContent(String content) { _content = content; }
		
		// constructor
		public Resource(int type, String content) {
			_type = type;
			_content = content;
		}
				
	}
	
	// some overridden methods for the Resource collection
	public static class Resources extends ArrayList<Resource> {

		@Override
		public boolean contains(Object o) {
			if (o.getClass() == Resource.class) {
				Resource r = (Resource) o;
				for (Resource resource : this) {
					if (r.getType() == resource.getType() && r.getContent() == resource.getContent()) return true;
				}
			}
			return false;
		}

		@Override
		public boolean add(Resource resource) {
			if (contains(resource)) {
				return false;
			} else {
				return super.add(resource);
			}
		}

		@Override
		public void add(int index, Resource resource) {
			if (!contains(resource)) {
				super.add(index, resource);
			}
		}
			
	}
	
	// instance variables	
	private int _xmlVersion, _status, _applicationBackupsMaxSize, _pageBackupsMaxSize;
	private String _id, _version, _name, _title, _description, _startPageId, _styles, _functions, _securityAdapterType, _createdBy, _modifiedBy;
	private boolean _showConrolIds, _showActionIds;
	private Date _createdDate, _modifiedDate;
	private SecurityAdapater _securityAdapter;	
	private List<DatabaseConnection> _databaseConnections;	
	private List<Webservice> _webservices;
	private List<Parameter> _parameters;
	private List<String> _controlTypes, _actionTypes;
	private HashMap<String,Page> _pages;
	private Resources _resources;
	private List<String> _styleClasses;
	
	// properties
				
	// the XML version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }
				
	// the id uniquely identifies the page (it is produced by taking all unsafe characters out of the name)
	public String getId() { return _id; }
	public void setId(String id) { _id = id; }
	
	// the version is used for Rapid Mobile's offline files to work with different published versions of the app
	public String getVersion() { return _version; }
	public void setVersion(String version) { _version = version; }
	
	// the status is used for Rapid Mobile's offline files to work with different published versions of the app
	public int getStatus() { return _status; }
	public void setStatus(int status) { _status = status; }
		
	// this is expected to be short name, probably even a code that is used by users to simply identify pages (also becomes the file name)
	public String getName() { return _name; }
	public void setName(String name) { _name = name; }

	// this is a user-friendly, long title
	public String getTitle() { return _title; }
	public void setTitle(String title) { _title = title; }
	
	// an even longer description of what this page does
	public String getDescription() { return _description; }
	public void setDescription(String description) { _description = description; }
	
	// the user that created this application 
	public String getCreatedBy() { return _createdBy; }
	public void setCreatedBy(String createdBy) { _createdBy = createdBy; }
	
	// the date this application was created
	public Date getCreatedDate() { return _createdDate; }
	public void setCreatedDate(Date createdDate) { _createdDate = createdDate; }
	
	// the last user to save this application 
	public String getModifiedBy() { return _modifiedBy; }
	public void setModifiedBy(String modifiedBy) { _modifiedBy = modifiedBy; }
	
	// the date this application was last saved
	public Date getModifiedDate() { return _modifiedDate; }
	public void setModifiedDate(Date modifiedDate) { _modifiedDate = modifiedDate; }
		
	// whether control ids should be shown when designing this app
	public boolean getShowControlIds() { return _showConrolIds; }
	public void setShowControlIds(boolean showConrolIds) { _showConrolIds = showConrolIds; }
		
	// whether action ids should be shown when designing this app
	public boolean getShowActionIds() { return _showActionIds; }
	public void setShowActionIds(boolean showActionIds) { _showActionIds = showActionIds; }
	
	// the application start page which will be supplied if no page is explicitly provided
	public String getStartPageId() { return _startPageId; }
	public void setStartPageId(String startPageId) { _startPageId = startPageId; }
			
	// the CSS styles added to the generated application rapid.css file
	public String getStyles() { return _styles; }
	public void setStyles(String styles) { _styles = styles; }
	
	// the JavaScript functions added to the generated application rapid.js file
	public String getFunctions() { return _functions; }
	public void setFunctions(String functions) { _functions = functions; }
	
	// a collection of database connections used via the connection adapter class to produce database connections 
	public List<DatabaseConnection> getDatabaseConnections() { return _databaseConnections; }
	public void setDatabaseConnections(List<DatabaseConnection> databaseConnections) { _databaseConnections = databaseConnections; }
	
	// a collection of webservices for this application
	public List<Webservice> getWebservices() { return _webservices; }
	public void setWebservices(List<Webservice> webservices) { _webservices = webservices; }
	
	// the class name of the security adapter this application uses
	public String getSecurityAdapterType() { return _securityAdapterType; }
	public void setSecurityAdapterType(String securityAdapterType) { _securityAdapterType = securityAdapterType; }
	
	// a collection of parameters for this application
	public List<Parameter> getParameters() { return _parameters; }
	public void setParameters(List<Parameter> parameters) { _parameters = parameters; }
	
	// control types used in this application
	public List<String> getControlTypes() { return _controlTypes; }
	public void setControlTypes(List<String> controlTypes) { _controlTypes = controlTypes; }
	
	// control types used in this application
	public List<String> getActionTypes() { return _actionTypes; }
	public void setActionTypes(List<String> actionTypes) { _actionTypes = actionTypes; }
	
	// number of application backups to keep
	public int getApplicationBackupsMaxSize() { return _applicationBackupsMaxSize; }
	public void setApplicationBackupMaxSize(int applicationBackupsMaxSize) { _applicationBackupsMaxSize = applicationBackupsMaxSize; }
	
	// number of page backups to keep
	public int getPageBackupsMaxSize() { return _pageBackupsMaxSize; }
	public void setPageBackupsMaxSize(int pageBackupsMaxSize) { _pageBackupsMaxSize = pageBackupsMaxSize; }
		
	// constructors
	
	public Application() {
		_xmlVersion = XML_VERSION;
		_pages = new HashMap<String,Page>();
		_databaseConnections = new ArrayList<DatabaseConnection>();
		_webservices = new ArrayList<Webservice>();
		_parameters = new ArrayList<Parameter>();
		_applicationBackupsMaxSize = 3;
		_pageBackupsMaxSize = 3;
	};
		
	// instance methods
	
	// this is where the application configuration will be stored
	public String getConfigFolder(ServletContext servletContext) {
		return getConfigFolder(servletContext, _id, _version);		
	}
	
	// this is the web folder with the full system path
	public String getWebFolder(ServletContext servletContext) {
		return getWebFolder(servletContext, _id, _version);
	}
			
	// this is the backup folder
	public String getBackupFolder(ServletContext servletContext, boolean allVersions) {
		return getBackupFolder(servletContext, _id, _version, allVersions);
	}
	
	// this replaces [[xxx]] in a string where xxx is a known system or application parameter
	public String insertParameters(String string) {
		// update commmon known system parameters
		string = string
			.replace("[[webfolder]]", getWebFolder(this));
		// if we have parameters
		if (_parameters != null) {
			// loop them
			for (Parameter parameter : _parameters) {
				// define the match string
				String matchString = "[[" + parameter.getName() + "]]";
				// if the match string is present replace it with the value
				if (string.contains(matchString)) string = string.replace(matchString, parameter.getValue());
			}
		}
		// return it
		return string;
	}
		
	// get the first page the users want to see (set in Rapid Admin on first save)
	public Page getStartPage() {
		// retain an instance to the page we are about to return
		Page startPage = null;
		// check we have some pages
		if (_pages != null) {
			// if there is a retained _startPageId try and use that
			if (_startPageId != null) startPage = getPage(_startPageId);
			// if we don't have a start page set yet, get the first entry from the map
			if (startPage == null && _pages.keySet().iterator().hasNext()) startPage = _pages.get(_pages.keySet().iterator().next());
		}
		return startPage;
	}
	
	// loop the collection of database connections looking for a named one
	public DatabaseConnection getDatabaseConnection(String name) {
		if (_databaseConnections != null) {
			for (DatabaseConnection databaseConnection : _databaseConnections) if (name.equals(databaseConnection.getName())) return databaseConnection;
		}
		return null;
	}
	// add a single database connection 
	public void addDatabaseConnection(DatabaseConnection databaseConnection) { _databaseConnections.add(databaseConnection); }	
	// remove a single database connection
	public void removeDatabaseConnection(String name) {
		DatabaseConnection databaseConnection = getDatabaseConnection(name);
		if (databaseConnection != null) _databaseConnections.remove(databaseConnection);
	}
	
	// get a parameter value by name
	public String getParameterValue(String parameterName) {
		// if there are parameters
		if (_parameters != null) {
			// loop them
			for (Parameter parameter : _parameters) {
				// check the name and return if match
				if (parameterName.equals(parameter.getName())) return parameter.getValue();
			}
		}
		// return null if nothing found
		return null;
	}
	
	// get a single page by it's id
	public Page getPage(String id) { return _pages.get(id);	}
	// get all page id's
	public Set<String> getPagsIds() { return _pages.keySet(); }
	// we don't want the pages in the application.xml so no setPages to avoid the marshaler
	public ArrayList<Page> getSortedPages() {
		// prepare the list we are going to send back
		ArrayList<Page> pages = new ArrayList<Page>();
		// add each page to the list
		for (String pageId : _pages.keySet()) {
			pages.add(_pages.get(pageId));
		}
		// sort the list by the page name
		Collections.sort(pages, new Comparator<Page>() {
			@Override
			public int compare(Page page1, Page page2) {
				return Comparators.AsciiCompare(page1.getName(), page2.getName(), false);
			}
			
		});
		// return the pages
		return pages; 
	}	
	// get a single page by it's name
	public Page getPageByName(String name) {
		// loop the page keyset
		for (String pageId : _pages.keySet()) {
			// get this page
			Page page = _pages.get(pageId);
			// return immediately  with the matching page
			if (name.equals(page.getName())) return page;
		}
		// return if we got here
		return null;	
	}
	// add them singly 
	public void addPage(Page page) { _pages.put(page.getId(), page); }	
	// remove them one by one too
	public void removePage(String id) {	_pages.remove(id); }
	
	// get a control by it's id
	public Control getControl(String id) {
		Control control = null;
		// check we have pages
		if (_pages != null) {
			for (String pageId : _pages.keySet()) {
				// fetch this page
				Page page = _pages.get(pageId);
				// look for the control
				control = page.getControl(id);
				// if we found it we can stop looping
				if (control != null) break;
			}
		}
		return control;
	}
	
	// get a webservice by it's id
	public Webservice getWebservice(String id) {
		if (_webservices != null) {
			for (Webservice webservice : _webservices) {
				if (id.equals(webservice.getId())) return webservice;
			}
		}
		return null;
	}
	
	// return the list of style classes
	public List<String> getStyleClasses() {
		return _styleClasses;		
	}
	
	// an instance of the security adapter used by this class
	public SecurityAdapater getSecurity() { return _securityAdapter; }
	// set the security to a given type
	public void setSecurity(ServletContext servletContext, String securityAdapterType) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		_securityAdapterType = securityAdapterType;
		if (_securityAdapterType == null) {
			_securityAdapter = new RapidSecurityAdapter(servletContext, this);
			_securityAdapterType = "rapid";
		} else {			
			HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) servletContext.getAttribute("securityConstructors");
			Constructor<SecurityAdapater> constructor = constructors.get(_securityAdapterType);
			if (constructor == null) throw new InstantiationException("Security adapter \"" +  _securityAdapterType + "\" can't be found");
			_securityAdapter = constructor.newInstance(servletContext, this);
		}
	}
	
	// this is a list of elements to go in the head section of the page for any resources the applications controls or actions may require
	public List<Resource> getResources() { return _resources; }
			
	// scan the css for classes
	private List<String> scanStyleClasses(String css) {
		
		ArrayList<String> classes = new ArrayList<String>();
		
		// only if we got something we can use
		if (css != null) {
			
			// find the first .
			int startPos = css.indexOf(".");
						
			// if we got one
			while (startPos >= 0) {
			
				// find the start of the next style
				int styleStartPos = css.indexOf("{", startPos);
				
				// find the end of the next style
				int styleEndPos = css.indexOf("}", startPos);
																
				// only if we are in front of a completed style and there is a . starting before the style 
				if (styleStartPos < styleEndPos && startPos < styleStartPos) {
				
					// find the end of the class by the first space
					int endPos = css.indexOf(" ", startPos);
					// adjust back to the start of the class if we went past some how
					if (styleStartPos < endPos) endPos = styleStartPos;
					// if it works out					
					if (endPos > startPos) {
						// fetch the class
						String styleClass = css.substring(startPos + 1, endPos).trim();
						// remove any closing brackets
						if (styleClass.indexOf(")") > 0) styleClass = styleClass.substring(0, styleClass.indexOf(")"));						
						// check we don't have it already and add it if ok
						if (!classes.contains(styleClass)) classes.add(styleClass);					
					}
										
				}
				
				// exit here if styleEndPos is going to cause problems
				if (styleEndPos == -1) break;
				
				// find the next .
				startPos = css.indexOf(".", styleEndPos);
								
			}
						
		}
		
		// sort the classes into alphabetical order
		Collections.sort(classes);
		
		return classes;
		
	}
	
	// this adds resources from either a control or action, they are added to the resources collection for printing in the top of each page if they are files, or amended to the application .js or .css files
	private void addResources(JSONObject jsonObject, String jsonObjectType, StringBuilder js, StringBuilder css) throws JSONException {
		
		// look for a resources object
		JSONObject jsonResourcesObject = jsonObject.optJSONObject("resources");
		
		// if we got one
		if (jsonResourcesObject != null) {
			
			// get the resource into an array (which is how the jaxb is passed the json)
			JSONArray jsonResources = jsonResourcesObject.optJSONArray("resource");
			
			// if we didn't get an array this is probably a single item collection stored as an object
			if (jsonResources == null) {
				// get the resource as an object
				JSONObject jsonResource = jsonResourcesObject.optJSONObject("resource");
				// if we got something
				if (jsonResource != null) {
					// make a proper array
					jsonResources = new JSONArray();
					// add the item
					jsonResources.put(jsonResource);
				}			
			}
			
			// check we have something
			if (jsonResources != null) {
				// loop them
				for (int j = 0; j < jsonResources.length(); j++) {

					// get a reference to this resource
					JSONObject jsonResource = jsonResources.getJSONObject(j);
					// get the type
					String resourceType = jsonResource.getString("type");
					// get the contens which is either a path, or the real stuff
					String resourceContents = jsonResource.getString("contents").trim();
					// get a name for the jsonObject
					String name = jsonObject.optString("name");
					// safety check
					if (name == null) name = resourceType;
					// add json object type
					name += " " + jsonObjectType;
					
					// add as resources if they're files, or append the string builders (the app .js and .css are added as resources at the end)
					if ("javascript".equals(resourceType)) {
						js.append("\n/* " + name + " resource JavaScript */\n\n" + resourceContents + "\n");
					} else if ("css".equals(resourceType)) {
						css.append("\n/* " + name + " resource styles */\n\n" + resourceContents + "\n");
					} else if ("javascriptFile".equals(resourceType)) {
						_resources.add(new Resource(Resource.JAVASCRIPTFILE, resourceContents));
					} else if ("cssFile".equals(resourceType)) {
						_resources.add(new Resource(Resource.CSSFILE, resourceContents));
					} else if ("javascriptLink".equals(resourceType)) {
						_resources.add(new Resource(Resource.JAVASCRIPTLINK, resourceContents));
					} else if ("cssLink".equals(resourceType)) {
						_resources.add(new Resource(Resource.CSSLINK, resourceContents));
					} else if ("file".equals(resourceType)) {
						_resources.add(new Resource(Resource.FILE, resourceContents));
					} 
										
				} // resource loop
				
			} // json resource check
									
		} // json resources check
						
	}
	
	public void loadPages(ServletContext servletContext, File pagesFolder) throws RapidLoadingException {
		
		// empty the collection
		_pages.clear();
		
		if (pagesFolder.exists()) {
			
			// create a filter for finding .page.xml files
			FilenameFilter xmlFilenameFilter = new FilenameFilter() {
		    	public boolean accept(File dir, String name) {
		    		return name.toLowerCase().endsWith(".page.xml");
		    	}
		    };
		    
		    // loop the .page.xml files and add to the application
		    for (File pageFile : pagesFolder.listFiles(xmlFilenameFilter)) {
		    	
		    	try {
		    	
		    		addPage(Page.load(servletContext, pageFile));
		    		
		    	} catch (Exception ex) {	

		    		// assume the page name is the whole path
		    		String pageName = pageFile.getPath();
		    		// if it contains the pages folder start from there
		    		if (pageName.contains(File.separator + "pages" + File.separator)) pageName = pageName.substring(pageName.indexOf(File.separator + "pages" + File.separator) + 7);		    					    		
		    		// remove any .page.xml
		    		if (pageName.contains(".page.xml")) pageName = pageName.substring(0, pageName.indexOf(".page.xml"));
		    		// throw the exception
		    		throw new RapidLoadingException("Error loading page " + pageName, ex);
		    		
				}
		    	
		    }
			
		}
		
	}
		
	// this function initialises the application when its first loaded, initialises the security adapter and builds the rapid.js and rapid.css files
	public void initialise(ServletContext servletContext, boolean createResources) throws JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException, IOException {
		
		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");
		
		// trace log that we're initialising
		logger.trace("Initialising application " + _name + "/" + _version);
		
		// initialise the security adapter 
		setSecurity(servletContext, _securityAdapterType);
				
		// initialise the resource includes collection
		_resources = new Resources();
		
		// if the created date is null set to today
		if (_createdDate == null) _createdDate = new Date();
						
		// when importing an application we need to initialise but don't want the resource folders made in the old applications name
		if (createResources) {
			
			// get the jsonControls
			JSONArray jsonControls = (JSONArray) servletContext.getAttribute("jsonControls");
					
			// get the jsonActions
			JSONArray jsonActions = (JSONArray) servletContext.getAttribute("jsonActions");
							
			// string builders for the different sections in our rapid.js file
			StringBuilder resourceJS = new StringBuilder();
			StringBuilder initJS = new StringBuilder();
			StringBuilder dataJS = new StringBuilder();
			StringBuilder actionJS = new StringBuilder();
						
			// string builder for our rapid.css file
			StringBuilder resourceCSS = new StringBuilder();
			
			// check controls
			if (jsonControls != null) {
				
				// check control types
				if (_controlTypes != null) {
					
					// make sure the page is included as we hide it from the users in the admin
					if (!_controlTypes.contains("page")) _controlTypes.add(0, "page");
					
					// collection of dependent controls that need adding
					ArrayList<String> dependentControls = new ArrayList<String>();
					
					// loop control types used by this application
					for (String controlType : _controlTypes) {
						
						// loop all available controls
			    		for (int i = 0; i < jsonControls.length(); i++) {
			    			
			    			// get the control
			    			JSONObject jsonControl = jsonControls.getJSONObject(i);
			    			
			    			// check if we're on the type we need
			    			if (controlType.equals(jsonControl.optString("type"))) {
			    				
			    				// look for any dependant control types
			    				JSONObject dependantTypes = jsonControl.optJSONObject("dependentTypes");
			    				// if we got some
			    				if (dependantTypes != null) {
			    					// look for an array
			    					JSONArray dependantTypesArray = dependantTypes.optJSONArray("dependentType");
			    					// if we got one
			    					if (dependantTypesArray != null) {
			    						// loop the array
			    						for (int j = 0; j < dependantTypesArray.length(); j++) {
			    							String dependantType = dependantTypesArray.getString(j);
				    						if (!_controlTypes.contains(dependantType) && !dependentControls.contains(dependantType)) dependentControls.add(dependantType);
			    						}
			    					} else {
			    						// just use the object
			    						String dependantType = dependantTypes.getString("dependentType");
			    						if (!_controlTypes.contains(dependantType) && !dependentControls.contains(dependantType)) dependentControls.add(dependantType);
			    					}
			    				}
			    				
			    				// we're done
			    				break;
			    			}
			    						    			
			    		}
			    		
					}
					
					// now add all of the dependent controls
					_controlTypes.addAll(dependentControls);
					
					// loop control types used by this application
					for (String controlType : _controlTypes) {
						
						// loop all available controls
			    		for (int i = 0; i < jsonControls.length(); i++) {
			    			
			    			// get the control
			    			JSONObject jsonControl = jsonControls.getJSONObject(i);
			    			
			    			// check if we're on the type we need
			    			if (controlType.equals(jsonControl.optString("type"))) {
			    				
			    				// add any resources (actions can have them too)
			    				addResources(jsonControl, "control", resourceJS, resourceCSS);
			    				
			    				// get any initJavaScript
				    			String js = jsonControl.optString("initJavaScript");
				    			// check
				    			if (js != null) {
				    				// only if something there
				    				if (!"".equals(js.trim())) {
				    					initJS.append("\nfunction Init_" + jsonControl.getString("type") + "(id, details) {\n");
				    					initJS.append("  " + js.trim().replace("\n", "\n  "));
				    					initJS.append("\n}\n");
				    				}    				
				    			}
				    			
				    			// check for a getData method
				    			String getDataFunction = jsonControl.optString("getDataFunction");    			
				    			// if there was something
				    			if (getDataFunction != null) {
				        			// clean and print! (if not an empty string)
				        			if (getDataFunction.trim().length() > 0) dataJS.append("\nfunction getData_" + controlType + "(ev, id, field, details) {\n  " + getDataFunction.trim().replace("\n", "\n  ") + "\n}\n");
				        			
				    			}    	
				    			
				    			// check for a setData method
				    			String setDataFunction = jsonControl.optString("setDataJavaScript");    			
				    			// if there was something
				    			if (setDataFunction != null) {
				        			// clean and print! (if not an empty string)
				        			if (setDataFunction.trim().length() > 0) dataJS.append("\nfunction setData_" + controlType + "(id, data, field, details, changeEvents) {\n  " + setDataFunction.trim().replace("\n", "\n  ") + "\n}\n");
				        			
				    			}	
				    			
				    			// retrieve any runtimeProperties
				    			JSONObject jsonRuntimePropertyCollection = jsonControl.optJSONObject("runtimeProperties");
				    			// check we got some
				    			if (jsonRuntimePropertyCollection != null) {
				    				
				    				// get the first one
				    				JSONObject jsonRuntimeProperty = jsonRuntimePropertyCollection.optJSONObject("runtimeProperty");
				    				// get an array
				    				JSONArray jsonRunTimeProperties = jsonRuntimePropertyCollection.optJSONArray("runtimeProperty");
				    				
				    				// initialise counters
				    				int index = 0;
					    			int count = 0;
					    			
					    			// if we got an array
					    			if (jsonRunTimeProperties != null) {
					    				// retain the first entry in the object
					    				jsonRuntimeProperty = jsonRunTimeProperties.getJSONObject(0);
					    				// retain the size
					    				count = jsonRunTimeProperties.length();
					    			}
					    			
					    			do {
					    				
					    				// get the type
				    					String type = jsonRuntimeProperty.getString("type");
				    					
				    					// get the get function
				    					String getFunction = jsonRuntimeProperty.optString("getPropertyFunction", null);					    				
				    					// print the get function if there was one
					    				if (getFunction != null) dataJS.append("\nfunction getProperty_" + controlType + "_" + type + "(ev, id, field, details) {\n  " + getFunction.trim().replace("\n", "\n  ") + "\n}\n");
					    				
					    				// get the set function
				    					String setFunction = jsonRuntimeProperty.optString("setPropertyJavaScript", null);					    				
				    					// print the get function if there was one
					    				if (setFunction != null) dataJS.append("\nfunction setProperty_" + controlType + "_" + type + "(ev, id, field, details, data, changeEvents) {\n  " + setFunction.trim().replace("\n", "\n  ") + "\n}\n");
					    				
					    				// increment index
					    				index++;
					    				
					    				// get the next one
					    				if (index < count) jsonRuntimeProperty = jsonRunTimeProperties.getJSONObject(index);
					    				
					    			} while (index < count);
					    							    				 
				    			}
				    				 				    							    							    						    				
			    				// we're done with this jsonControl
			    				break;
			    			}
			    					    					    					    					    					    			
			    		} // jsonControls loop
						
					} // control types loop
					
				} // control types check
										
			} // jsonControls check
			
			// check  actions
	    	if (jsonActions != null) {
	    		
	    		// check action types
	    		if (_actionTypes != null) {
	    				    			
	    			// collection of dependent controls that need adding
					ArrayList<String> dependentActions = new ArrayList<String>();
					
					// loop control types used by this application
					for (String actionType : _actionTypes) {
						
						// loop all available controls
			    		for (int i = 0; i < jsonActions.length(); i++) {
			    			
			    			// get the action
			    			JSONObject jsonAction = jsonActions.getJSONObject(i);
			    			
			    			// check if we're on the type we need
			    			if (actionType.equals(jsonAction.optString("type"))) {
			    				
			    				// look for any dependant control types
			    				JSONObject dependantTypes = jsonAction.optJSONObject("dependentTypes");
			    				// if we got some
			    				if (dependantTypes != null) {
			    					// look for an array
			    					JSONArray dependantTypesArray = dependantTypes.optJSONArray("dependentType");
			    					// if we got one
			    					if (dependantTypesArray != null) {
			    						// loop the array
			    						for (int j = 0; j < dependantTypesArray.length(); j++) {
			    							String dependantType = dependantTypesArray.getString(j);
				    						if (!_actionTypes.contains(dependantType) && !dependentActions.contains(dependantType)) dependentActions.add(dependantType);
			    						}
			    					} else {
			    						// just use the object
			    						String dependantType = dependantTypes.getString("dependentType");
			    						if (!_actionTypes.contains(dependantType) && !dependentActions.contains(dependantType)) dependentActions.add(dependantType);
			    					}
			    				}
			    				
			    				// we're done
			    				break;
			    			}
			    						    			
			    		}
			    		
					}
					
					// now add all of the dependent controls
					_controlTypes.addAll(dependentActions);
	    			
	    			// loop action types used by this application
					for (String actionType : _actionTypes) {
						
						// loop jsonActions
			    		for (int i = 0; i < jsonActions.length(); i++) {
			    			
			    			// get action
			    			JSONObject jsonAction = jsonActions.getJSONObject(i);
			    			
			    			// check the action is the one we want
			    			if (actionType.equals(jsonAction.optString("type"))) {
			    				
			    				// add any resources (controls can have them too)
			    				addResources(jsonAction, "action", resourceJS, resourceCSS);
			    				  			
				    			// get action JavaScript
				    			String js = jsonAction.optString("actionJavaScript");
				    			// only produce rapid action is this is rapid app
				    			if (js != null && ("rapid".equals(_id) || !"rapid".equals(actionType))) {    				
				        			// clean and print! (if not an empty string)
				        			if (js.trim().length() > 0) actionJS.append("\n" + js.trim() + "\n");
				        			
				    			}
			    				
			    				// move onto the next action type
			    				break;
			    			}
			    			
			    		} // jsonActions loop
						
					} // action types loop
	    			    						
	    		} // action types check
	    		
	    	} // jsonAction check
	    		    					    	
	    	// create folders to write the rapid.js file
			String applicationPath = getWebFolder(servletContext);		
			File applicationFolder = new File(applicationPath);		
			if (!applicationFolder.exists()) applicationFolder.mkdirs();
			
			// write the rapid.js file
			FileOutputStream fos = new FileOutputStream (applicationPath + "/rapid.js");
			PrintStream ps = new PrintStream(fos);
						
			ps.print("\n/* This file is auto-generated on application load and save - it is minified when the application status is live */\n");
			if (_functions != null) {
				ps.print("\n\n/* Application functions JavaScript */\n\n");
				ps.print(_functions);
			}
			if (resourceJS.length() > 0) {
				ps.print("\n\n/* Control and Action resource JavaScript */\n\n");
				ps.print(resourceJS.toString());
			}
			if (initJS.length() > 0) {
				ps.print("\n\n/* Control initialisation methods */\n\n");
				ps.print(initJS.toString());
			}
			if (dataJS.length() > 0) {
				ps.print("\n\n/* Control getData and setData methods */\n\n");
				ps.print(dataJS.toString());
			}
			if (actionJS.length() > 0) {
				ps.print("\n\n/* Action methods */\n\n");
				ps.print(actionJS.toString());
			}
						
			ps.close();
			fos.close();
			
						
			// minify it to a file
			Minify.toFile(actionJS.toString() + initJS.toString() + dataJS.toString() + resourceJS.toString(), applicationPath + "/rapid.min.js", Minify.JAVASCRIPT);
			
			// get the rapid CSS into a string and insert parameters
			String resourceCSSWithParams = insertParameters(resourceCSS.toString());
			String appCSSWithParams = insertParameters(_styles);
			
			// write the rapid.css file
			fos = new FileOutputStream (applicationPath + "/rapid.css");
			ps = new PrintStream(fos);
			ps.print("\n/* This file is auto-generated on application load and save - it is minified when the application status is live */\n");		
			ps.print(resourceCSSWithParams);
			ps.print("\n\n/* Application styles */\n\n");
			ps.print(appCSSWithParams);
			ps.close();
			fos.close();
			
			// minify it to a rapid.min.css file
			Minify.toFile(resourceCSSWithParams + appCSSWithParams, applicationPath + "/rapid.min.css", Minify.CSS);
			
			// check the status
	    	if (_status == STATUS_LIVE) {
	    		// add the application js min file as a resource
	    		_resources.add(new Resource(Resource.JAVASCRIPTFILE, getWebFolder(this) + "/rapid.min.js"));
	    		// add the application css min file as a resource	    	
	    		_resources.add(new Resource(Resource.CSSFILE, getWebFolder(this) + "/rapid.min.css"));
	    	} else {
	    		// add the application js file as a resource
	    		_resources.add(new Resource(Resource.JAVASCRIPTFILE, getWebFolder(this) + "/rapid.js"));
	    		// add the application css file as a resource	    	
	    		_resources.add(new Resource(Resource.CSSFILE, getWebFolder(this) + "/rapid.css"));
	    	}
			
	    	// loop all resources and minify js and css files
			for (Resource resource : _resources) {
				// get the content (which is the filename)
				String fileName = resource.getContent();
				// only interested in js and css files
				switch (resource.getType()) {
					case Resource.JAVASCRIPTFILE :						
						// get a file for this
						File jsFile = new File(servletContext.getRealPath(fileName));
						// if the file exists, and it's in the scripts folder and ends with .js
						if (jsFile.exists() && fileName.startsWith("scripts/") && fileName.endsWith(".js")) {
							// derive the min file name by modifying the start and end
							String fileNameMin = "scripts_min/" + fileName.substring(8, fileName.length() - 3) + ".min.js";
							// get a file for minifying 
							File jsFileMin = new File(servletContext.getRealPath(fileNameMin));
							// if this file does not exist
							if (!jsFileMin.exists()) {
								// make any dirs it may need
								jsFileMin.getParentFile().mkdirs();
								// minify to it
								Minify.toFile(jsFile, jsFileMin, Minify.JAVASCRIPT);
							}
							// if this application is live, update the resource to the min file
							if (_status == STATUS_LIVE) resource.setContent(fileNameMin);
						}
					break;
					case Resource.CSSFILE :
						// get a file for this
						File cssFile = new File(servletContext.getRealPath(fileName));
						// if the file exists, and it's in the scripts folder and ends with .js
						if (cssFile.exists() && fileName.startsWith("styles/") && fileName.endsWith(".css")) {
							// derive the min file name by modifying the start and end
							String fileNameMin = "styles_min/" + fileName.substring(7, fileName.length() - 4) + ".min.css";
							// get a file for minifying 
							File cssFileMin = new File(servletContext.getRealPath(fileNameMin));
							// if this file does not exist
							if (!cssFileMin.exists()) {
								// make any dirs it may need
								cssFileMin.getParentFile().mkdirs();
								// minify to it
								Minify.toFile(cssFile, cssFileMin, Minify.CSS);
							}
							// if this application is live, update the resource to the min file
							if (_status == STATUS_LIVE) resource.setContent(fileNameMin);
						}
					break;
				}
				
	    	} // loop resources
								
		} // create resources
														
		// populate the list of style classes by scanning the rapid.css
		_styleClasses = scanStyleClasses(_styles);
		
		// debug log that we initialised
		logger.debug("Initialised application " + _name + "/" + _version + (createResources ? "" : " (no resources)"));
								
	}
			
	// remove any page locks for a given user
	public void removeUserPageLocks(String userName) {
		// check there are pages
		if (_pages != null) {
			// loop them
			for (String pageId : _pages.keySet()) {
				// get the page
				Page page = _pages.get(pageId);
				// get the page lock
				Lock pageLock = page.getLock();
				// if there was one
				if (pageLock != null) {
					// if it matches the user name remove the lock
					if (userName.equals(pageLock.getUserName())) page.setLock(null);
				}
			}
		}
	}
	
	public List<Backup> getApplicationBackups(RapidHttpServlet rapidServlet) throws JSONException {
		
		List<Backup> backups = new ArrayList<Backup>();
				
		File backupFolder = new File(getBackupFolder(rapidServlet.getServletContext(), false));
		
		if (backupFolder.exists()) {
			
			for (File backup : backupFolder.listFiles()) {
				
				String id = backup.getName();
				
				String[] nameParts = id.split("_");
				
				if (id.contains(_id) && nameParts.length >= 3) {
					
					long sizeBytes = Files.getSize(backup);
					
					String size = "0b";
									
					if (sizeBytes < 1024) {
						size = sizeBytes + " bytes";
					} else if (sizeBytes < 1024 * 1024) {
						size = Math.floor(sizeBytes / 1024d * 100) / 100d + " KB";
					} else if (sizeBytes  < 1024 * 1024 * 1024) {
						size =  Math.floor(sizeBytes / 1024d / 1024d * 100) / 100d + " MB";
					} else if (sizeBytes < 1024 * 1024 * 1024 * 1024) {
						size =  Math.floor(sizeBytes / 1024d / 1024d / 1024d * 100) / 100d + " GB";
					} else {
						size = "huge!";
					}
					
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");
					
					Date date = new Date();
					
					try { 
						date = df.parse(nameParts[nameParts.length - 3] + " " + nameParts[nameParts.length - 2]); 
					} catch (ParseException ex) {
						throw new JSONException(ex);
					}
					
					backups.add(new Backup(id, date, nameParts[nameParts.length - 1], size));
															
				} // name parts > 3
				
			} // file loop
			
			// sort the list by date
			Collections.sort(backups, new Comparator<Backup>() {
				@Override
				public int compare(Backup obj1, Backup obj2) {
					if (obj1.getDate().before(obj2.getDate())) {
						return -1;
					} else {
						return 1;
					}
				}			
			});
			
			// check if we have too many
			while (backups.size() > _applicationBackupsMaxSize) {
				// get the top backup folder into a file object
				backupFolder = new File(getBackupFolder(rapidServlet.getServletContext(), false) + "/" + backups.get(0).getId());
				// delete it
				Files.deleteRecurring(backupFolder);
				// remove it
				backups.remove(0);
			}
			
		}
		
		return backups;
		
	}
	
	public List<Backup> getPageBackups(RapidHttpServlet rapidServlet) throws JSONException {
		
		List<Backup> backups = new ArrayList<Backup>();
				
		File backupFolder = new File(getBackupFolder(rapidServlet.getServletContext(), false));
		
		if (backupFolder.exists()) {
			
			for (File backup : backupFolder.listFiles()) {
				
				String fileName = backup.getName();
				
				if (fileName.endsWith(".page.xml")) {
					
					String[] nameParts = fileName.split("_");
					
					if (nameParts.length >= 3) {
						
						String name = nameParts[0];
						
						for (int i = 1; i < nameParts.length - 3; i++) {
							name += "_" + nameParts[i];
						}
						
						long sizeBytes = Files.getSize(backup);
						
						String size = "0b";
										
						if (sizeBytes < 1024) {
							size = sizeBytes + " bytes";
						} else if (sizeBytes < 1024 * 1024) {
							size = Math.floor(sizeBytes / 1024d * 100) / 100d + " KB";
						} else if (sizeBytes  < 1024 * 1024 * 1024) {
							size =  Math.floor(sizeBytes / 1024d / 1024d * 100) / 100d + " MB";
						} else if (sizeBytes < 1024 * 1024 * 1024 * 1024) {
							size =  Math.floor(sizeBytes / 1024d / 1024d / 1024d * 100) / 100d + " GB";
						} else {
							size = "huge!";
						}
						
						SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");
						
						Date date = new Date();
						
						try { 
							date = df.parse(nameParts[nameParts.length - 3] + " " + nameParts[nameParts.length - 2]); 
						} catch (ParseException ex) {
							throw new JSONException(ex);
						}
						
						String[] userParts = nameParts[nameParts.length - 1].split("\\.");
						
						backups.add(new Backup(fileName, name, date, userParts[0], size));
																
					} // name parts > 3
					
				} // ends .page.xml
												
			} // file loop
			
			// sort the list by date
			Collections.sort(backups, new Comparator<Backup>() {
				@Override
				public int compare(Backup obj1, Backup obj2) {
					if (obj1.getDate().before(obj2.getDate())) {
						return -1;
					} else {
						return 1;
					}
				}			
			});
			
			// create a map to count backups of each page
			Map<String,Integer> pageBackupCounts = new HashMap<String,Integer>();
			// loop all of the backups in reverse order
			for (int i = backups.size() - 1; i >= 0; i--) {
				// get the back up
				Backup pageBackup = backups.get(i);
				// assume no backups so far for this page
				int pageBackupCount = 0;
				// set the backup count if we have one
				if (pageBackupCounts.get(pageBackup.getName()) != null) pageBackupCount = pageBackupCounts.get(pageBackup.getName());
				// increment the count
				pageBackupCount ++;
				// check the size
				if (pageBackupCount > _pageBackupsMaxSize) {
					// get the backup into a file object
					File backupFile = new File(backupFolder.getAbsolutePath() + "/" + backups.get(i).getId());
					// delete it
					backupFile.delete();
					// remove this backup
					backups.remove(i);							
				}
				// store the page backup count
				pageBackupCounts.put(pageBackup.getName(), pageBackupCount);
			}
			
		}
		
		return backups;
		
	}
	
	public void backup(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, boolean allVersions) throws IOException {
		
		// get the username
		String userName = rapidRequest.getUserName();
		if (userName == null) userName = "unknown";
		
		// get the current date and time in a string
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateString = formatter.format(new Date());
		
		// create a fileName for the archive
		String fileName = _id + _version + "_" + dateString + "_" + Files.safeName(userName);
								
		// create folders to backup the app
		String backupPath = getBackupFolder(rapidServlet.getServletContext(), allVersions) + "/" + fileName;		
		File backupFolder = new File(backupPath);		
		if (!backupFolder.exists()) backupFolder.mkdirs();

		// create a file object for the application data folder
		File appFolder = new File(getConfigFolder(rapidServlet.getServletContext()));
		
		// create a list of files to ignore
		List<String> ignoreFiles = new ArrayList<String>();
		ignoreFiles.add(BACKUP_FOLDER);
	 	
	 	// copy the existing files and folders to the backup folder    
	    Files.copyFolder(appFolder, backupFolder, ignoreFiles);
	    
	    // create a file object and folders for the web folder archive
	    backupFolder = new File(backupPath + "/WebContent");
	    if (!backupFolder.exists()) backupFolder.mkdirs();
	    
	    // create a file object for the application web folder
	    appFolder = new File(getWebFolder(rapidServlet.getServletContext()));
	 		    	    
	 	// copy the existing web content files and folders to the webcontent backup folder    
	    Files.copyFolder(appFolder, backupFolder, ignoreFiles);
	    	
	}
		
	public Application copy(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String newId, String newVersion, boolean backups, boolean delete) throws IOException, IllegalArgumentException, SecurityException, JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException {
		
		// retain the ServletContext
		ServletContext servletContext = rapidServlet.getServletContext();
				
		// load the app into a copy without initialising
		Application appCopy = Application.load(servletContext, new File(getConfigFolder(servletContext) + "/application.xml"), false);
		
		// update the copy id 
		appCopy.setId(newId);
		// update the copy version
		appCopy.setVersion(newVersion);
		// update the copy status to in developement
		appCopy.setStatus(Application.STATUS_DEVELOPMENT);
		// update the created date
		appCopy.setCreatedDate(new Date());
						
		// save the copy to create the folder and, application.xml and page.xml files
		appCopy.save(rapidServlet, rapidRequest, false);
				
		// get the copy of the application.xml file
		File appCopyFile = new File(appCopy.getConfigFolder(servletContext) + "/application.xml");
		// read the copy to a string
		String appCopyXML = Strings.getString(appCopyFile);
		// replace all app/version references
		appCopyXML = appCopyXML.replace("/" + _id + "/" + _version + "/", "/" + newId + "/" + newVersion + "/");
		// save it back
		FileWriter fs = new FileWriter(appCopyFile);
		fs.write(appCopyXML);
		fs.close();
		
		// look for a security.xml file
		File appSecurityFile = new File(getConfigFolder(servletContext) + "/security.xml");
		// if we have one, copy it
		if (appSecurityFile.exists()) Files.copyFile(appSecurityFile, new File(appCopy.getConfigFolder(servletContext) + "/security.xml"));
		
		// get the pages config folder
		File appPagesFolder = new File(getConfigFolder(servletContext) + "/pages");		
		// check it exists
		if (appPagesFolder.exists()) {
			// the folder we are copying to
			File appPagesCopyFolder = new File(appCopy.getConfigFolder(servletContext) + "/pages");
			// make the dirs
			appPagesCopyFolder.mkdirs();
			// loop the files
			for (File appCopyPageFile : appPagesFolder.listFiles()) {
				// if this is a page.xml file
				if (appCopyPageFile.getName().endsWith(".page.xml")) {
					// read the copy to a string
					String pageCopyXML = Strings.getString(appCopyPageFile);
					// replace all app/version references
					pageCopyXML = pageCopyXML
							.replace("/" + _id + "/" + _version + "/", "/" + newId + "/" + newVersion + "/")
							.replace("~?a=" + _id + "&amp;" + _version + "&amp;", "~?a=" + newId + "&amp;" + newVersion + "&amp;");
					// save it back to it's new location
					fs = new FileWriter(new File(appPagesCopyFolder + "/" + appCopyPageFile.getName()));
					fs.write(pageCopyXML);
					fs.close();
				}
			}
		}
		
		// get the web folder
		File appWebFolder = new File(getWebFolder(servletContext));	
		// if it exists
		if (appWebFolder.exists()) {
			// get a folder for the new location
			File appWebCopyFolder = new File(appCopy.getWebFolder(servletContext));
			// copy everything
			Files.copyFolder(appWebFolder, appWebCopyFolder);
		}
		
		// if we want to copy the backups too
		if (backups) {
			// get the backups folder
			File appBackupFolder = new File(getBackupFolder(servletContext, false));
			// check it exists
			if (appBackupFolder.exists()) {
				// create a folder to copy to
				File appBackupCopyFolder = new File(appCopy.getBackupFolder(servletContext, false));
				// make the dirs
				appBackupCopyFolder.mkdirs();
				// copy the folder
				Files.copyFolder(appBackupFolder, appBackupCopyFolder);
			}
		}
						
		// reload the application with the new app and page references
		appCopy = Application.load(servletContext, appCopyFile);
										
		// add this one to the applications collection
		rapidServlet.getApplications().put(appCopy);
				
		// delete this one
		if (delete) delete(rapidServlet, rapidRequest, false);
		
		// return the copy
		return appCopy;
	    		
	}
				
	public void save(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, boolean backup) throws JAXBException, IOException, IllegalArgumentException, SecurityException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
						
		// create folders to save the app
		String folderPath = getConfigFolder(rapidServlet.getServletContext());		
		File folder = new File(folderPath);		
		if (!folder.exists()) folder.mkdirs();
		
		// create a file object for the application
		File appFile = new File(folderPath + "/application.xml");
		// backup the app if it already exists
		if (appFile.exists() && backup) backup(rapidServlet, rapidRequest, false);
		
		// create a temp file for saving the application to
		File tempFile = new File(folderPath + "/application-saving.xml");
		
		// update the modified by and date
		_modifiedBy = rapidRequest.getUserName();
		_modifiedDate = new Date();
		
		// marshal the application object to the temp file
		FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());		
	    rapidServlet.getMarshaller().marshal(this, fos);	    
	    fos.close();
	    
	    // copy / overwrite the app file with the temp file	    	    	    
	    Files.copyFile(tempFile, appFile);
	    
	    // delete the temp file
	    tempFile.delete();
	    
	    // put this application in the collection
	    rapidServlet.getApplications().put(this);
	    
	    // initialise the application, rebuilding the resources
	    initialise(rapidServlet.getServletContext(), true);
	    		
	}
	
	public void delete(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, boolean allVersions) throws JAXBException, IOException {
		
		// create a file object for the config folder
		File appFolder = new File(getConfigFolder(rapidServlet.getServletContext()));
		// if this is all versions promote from version to app
		if (allVersions) appFolder = appFolder.getParentFile();
		
		// create a file object for the webcontent folder
		File webFolder = new File (getWebFolder(rapidServlet.getServletContext()));
		// if this is all versions promote from version to app
		if (allVersions) webFolder = webFolder.getParentFile();
				
		// if the app folder exists
		if (appFolder.exists()) {
			// backup the application
			backup(rapidServlet, rapidRequest, allVersions);
			// delete the app folder
			Files.deleteRecurring(appFolder);
			// delete the web folder
			Files.deleteRecurring(webFolder);
			// remove this application from the collection
			rapidServlet.getApplications().remove(this);
		}
			    	    		
	}
	
	// create a named .zip file for the app in the /temp folder	
	public void zip(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, User user, String fileName, boolean offlineUse) throws JAXBException, IOException, JSONException {
		
		// create folders to save locate app file
		String folderPath = getConfigFolder(rapidServlet.getServletContext());		
		
		// create a file object for the application
		File appFile = new File(folderPath + "/application.xml");
								
		// if the app file exists
		if (appFile.exists()) {
															
			// create a list of sources for our zip
			ZipSources zipSources = new ZipSources();
			
			// deleteable folder
			File deleteFolder = null;
			
			// create a file object for the webcontent folder
			File webFolder = new File(getWebFolder(rapidServlet.getServletContext()));
			
			// if for offlineUse
			if (offlineUse) {
												
				// loop the contents of the webFolder 
				for (File file : webFolder.listFiles()) {
					// add this file to the zip using the web folder path
					zipSources.add(file, getWebFolder(this));
				}
				
				// set the delete folder
				deleteFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/temp/" + _id + "_" + rapidRequest.getUserName()));
				// create it
				deleteFolder.mkdirs();
				
				// make a details.txt file
				File detailsFile = new File(deleteFolder + "/details.txt");
				// get a file writer
				Writer fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(detailsFile), "UTF-8"));
				// write the details
				fw.write(_id + "\r\n" + _version + "\r\n" + _title);
				// close the file writer
				fw.close();
				// add the file to the zip with a root path
				zipSources.add(detailsFile, "");
				
				// check we have pages
				if (_pages != null) {					
					// loop them
					for (String pageId : _pages.keySet()) {
						// get a reference to the page
						Page page = _pages.get(pageId);
						// create a file for it in the delete folder
						File pageFile = new File(deleteFolder + "/" + pageId + ".htm");
						// create a file writer for it for now
						fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pageFile), "UTF-8"));
						// for now get a printWriter to write the page html
						page.writeHtml(rapidServlet, rapidRequest, this, user, fw, false);
						// close it
						fw.close();
						// add the file to the zip with a root path
						zipSources.add(pageFile, "");
					}
					// get the start page
					Page page = getStartPage();
					// if we got one add it as index.htm
					if (page != null) {
						// create a file for it for it in the delete folder
						File pageFile = new File(deleteFolder + "/" + "index.htm");
						// create a file writer for it for now
						fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pageFile), "UTF-8"));
						// for now get a printWriter to write the page html
						page.writeHtml(rapidServlet, rapidRequest, this, user, fw, false);
						// close it
						fw.close();
						// add the file to the zip with a root path
						zipSources.add(pageFile, "");
					}
					
				}
				
				// check we have resources
				if (_resources != null) {
					// loop them
					for (Resource resource : _resources) {
						// check they're any of our file types
						if (resource.getType() == Resource.JAVASCRIPTFILE || resource.getType() == Resource.CSSFILE || resource.getType() == Resource.FILE) {
							// get a file object for them
							File resourceFile = new File(rapidServlet.getServletContext().getRealPath("") + "/" + resource.getContent());
							// get the path from the file name
							String path = Files.getPath(resource.getContent());
							// add as zip source
							zipSources.add(resourceFile, path);
						}
					}
				}
				
			} else {
												
				// loop the contents of the webFolder and place in WebContent subfolder
				for (File file : webFolder.listFiles()) {
					// add this file to the WEB-INF path
					zipSources.add(new ZipSource(file,"WebContent"));
				}
				
				// create a file object for the application folder
				File appFolder = new File(folderPath);
				
				// loop the contents of the appFolder and place in WEB-INF subfolder
				for (File file : appFolder.listFiles()) {
					// add this file to the WebContent path
					zipSources.add(new ZipSource(file,"WEB-INF"));
				}
				
			}
									
			// get a file for the temp directory
			File tempDir = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/temp"));
			// create it if not there
			if (!tempDir.exists()) tempDir.mkdir();
									
			// create the zip file object with our destination, always in the temp folder
			ZipFile zipFile = new ZipFile(new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/temp/" + fileName)));

			// create a list of files to ignore
			ArrayList<String> ignoreFiles = new ArrayList<String>();
			// don't include any files or folders from the back in the .zip
			ignoreFiles.add(BACKUP_FOLDER);
			
			// zip the sources into the file
			zipFile.zipFiles(zipSources, ignoreFiles);
			
			// loop the deleteable files
			if (deleteFolder != null) {
				try {
					// delete the folder and all contents
					Files.deleteRecurring(deleteFolder);
				} catch (Exception ex) {
					// log exception
					Logger.getLogger(this.getClass()).error("Error deleting temp file " + deleteFolder, ex);
				}
								
			}

		}
			    	    		
	}
	
	// an overload for the above which will include the for export rather than offlineUse files
	public void zip(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, User user, String fileName) throws JAXBException, IOException, JSONException {
		zip(rapidServlet, rapidRequest, user, fileName, false);
	}
		
	// static methods
	
	// this is where the application configuration will be stored
	public static String getConfigFolder(ServletContext servletContext, String id, String version) {
		return servletContext.getRealPath("/WEB-INF/applications/" + id + "/" + version);		
	}
	
	// this is the web folder with the full system path
	public static String getWebFolder(ServletContext servletContext, String id, String version) {
		return servletContext.getRealPath("/applications/" + id + "/" + version);
	}
	
	// this is the web folder as seen externally
	public static String getWebFolder(Application application) {
		return "applications/" + application.getId() + "/" + application.getVersion();
	}
	
	// this is the backup folder
	public static String getBackupFolder(ServletContext servletContext, String id, String version, boolean allVersions) {
		if (allVersions) {
			return servletContext.getRealPath("/WEB-INF/applications/" +  BACKUP_FOLDER + "/" + id + "/" + version);
		} else {
			return servletContext.getRealPath("/WEB-INF/applications/" + id + "/" + version + "/" + BACKUP_FOLDER);
		}
	}
	
	// this is a simple overload for default loading of applications where the resources are all regenerated
	public static Application load(ServletContext servletContext, File file) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException {				
		return load(servletContext, file, true); 				
	}
	
	// this method loads the application by ummarshelling the xml, and then doing the same for all page .xmls, before calling the initialise method
	public static Application load(ServletContext servletContext, File file, boolean initialise) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException {
		
		// get the logger
		Logger logger = (Logger) servletContext.getAttribute("logger");
		
		// trace log that we're about to load a page
		logger.trace("Loading application from " + file);
					
		// open the xml file into a document
		Document appDocument = XML.openDocument(file);
		
		// specify the version as -1
		int xmlVersion = -1;
		
		// look for a version node
		Node xmlVersionNode = XML.getChildElement(appDocument.getFirstChild(), "XMLVersion");
		
		// if we got one update the version
		if (xmlVersionNode != null) xmlVersion = Integer.parseInt(xmlVersionNode.getTextContent());
				
		// if the version of this xml isn't the same as this class we have some work to do!
		if (xmlVersion != XML_VERSION) {
			
			// get the page name
			String name = XML.getChildElementValue(appDocument.getFirstChild(), "name");
									
			// log the difference
			logger.debug("Application " + name + " with xml version " + xmlVersion + ", current xml version is " + XML_VERSION);
			
			//
			// Here we would have code to update from known versions of the file to the current version
			//
			
			// check whether there was a version node in the file to start with
			if (xmlVersionNode == null) {
				// create the version node
				xmlVersionNode = appDocument.createElement("XMLVersion");
				// add it to the root of the document
				appDocument.getFirstChild().appendChild(xmlVersionNode);
			}
			
			// set the xml to the latest version
			xmlVersionNode.setTextContent(Integer.toString(XML_VERSION));
			
			// save it
			XML.saveDocument(appDocument, file);
			
			logger.debug("Updated " + name + " application xml version to " + XML_VERSION);
			
		}
		
		// get the unmarshaller from the context
		Unmarshaller unmarshaller = (Unmarshaller) servletContext.getAttribute("unmarshaller");
										
		try {
		
			// unmarshall the application
			Application application = (Application) unmarshaller.unmarshal(file);
									
			// look for pages
			File pagesFolder = new File(file.getParent() + "/pages");
												
			// if we don't want pages loaded or resource generation skip this
			if (initialise) {
				
				// load the pages
				application.loadPages(servletContext, pagesFolder);
																										
				// initialise the application and create the resources
				application.initialise(servletContext, true);
				
			}
			
			// log that the application was loaded
			logger.info("Loaded application " + application.getName() + "/" + application.getVersion() + (initialise ? "" : " (no itialise)"));
			
			return application; 		
			
			
		} catch (JAXBException ex) {
			
			throw new RapidLoadingException("Error loading application file at " + file, ex);
			
		}
						
	}
		
}
