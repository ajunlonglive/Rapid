package com.rapid.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
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
import java.util.Scanner;

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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.rapid.core.Page.Lock;
import com.rapid.data.ConnectionAdapter;
import com.rapid.security.RapidSecurityAdapter;
import com.rapid.security.SecurityAdapater;
import com.rapid.server.Rapid;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidHttpServlet.RapidRequest;
import com.rapid.soa.Webservice;
import com.rapid.utils.Comparators;
import com.rapid.utils.Files;
import com.rapid.utils.JAXB;
import com.rapid.utils.XML;
import com.rapid.utils.ZipFile;
import com.rapid.utils.ZipFile.ZipSource;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Application {
	
	// the version of this class's xml structure when marshalled (if we have any significant changes down the line we can upgrade the xml files before unmarshalling)	
	public static final int XML_VERSION = 1;
	
	// public static classes
	
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
				
	// instance variables
	
	private int _xmlVersion, _applicationBackupsMaxSize, _pageBackupsMaxSize;
	private String _id, _name, _title, _description, _startPageId, _styles, _securityAdapterType, _createdBy, _modifiedBy;
	private boolean _showConrolIds, _showActionIds;
	private Date _createdDate, _modifiedDate;
	private Map<String,String> _settings;
	private SecurityAdapater _securityAdapter;
	private List<String> _controlTypes, _actionTypes, _resourceIncludes;
	private List<DatabaseConnection> _databaseConnections;
	private List<Webservice> _webservices;
	private HashMap<String,Page> _pages;
	private List<String> _styleClasses;
	
	// properties
	
	// the version is used to upgrade xml files before unmarshalling (we use a property so it's written ito xml)
	public int getXMLVersion() { return _xmlVersion; }
	public void setXMLVersion(int xmlVersion) { _xmlVersion = xmlVersion; }
	
	// the id uniquely identifies the page (it is produced by taking all unsafe characters out of the name)
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
		
	// the settings map - can be edited in the application.xml file
	public Map<String,String> getSettings() { return _settings; }
	public void setSettings(Map<String,String> settings) { _settings = settings; }
	
	// the styles used to generate the application rapid.css file
	public String getStyles() { return _styles; }
	public void setStyles(String styles) { _styles = styles; }
	
	// a collection of database connections used via the connection adapter class to produce database connections 
	public List<DatabaseConnection> getDatabaseConnections() { return _databaseConnections; }
	public void setDatabaseConnections(List<DatabaseConnection> databaseConnections) { _databaseConnections = databaseConnections; }
	
	// a collection of webservices for this application
	public List<Webservice> getWebservices() { return _webservices; }
	public void setWebservices(List<Webservice> webservices) { _webservices = webservices; }
	
	// the class name of the security adapter this application uses
	public String getSecurityAdapterType() { return _securityAdapterType; }
	public void setSecurityAdapterType(String securityAdapterType) { _securityAdapterType = securityAdapterType; }
	
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
		_applicationBackupsMaxSize = 3;
		_pageBackupsMaxSize = 3;
	};
		
	// instance methods
	
	public Page getStartPage() {
		// retain an instance to the page we are about to return
		Page startPage = null;
		// check we have some pages
		if (_pages != null) {
			// if there is a retained _startPageId try and use that
			if (_startPageId != null) startPage = getPage(_startPageId);
			// if we don't have a page yet
			if (startPage == null) {
				// this seems the only way to retrieve the top map from the map
				for (String pageId : _pages.keySet()) {
					startPage = _pages.get(pageId);
					break;
				}				
			}
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
				return Comparators.AsciiCompare(page1.getName(), page2.getName());
			}
			
		});
		// return the pages
		return pages; 
	}
	
	// get a single page by it's id
	public Page getPage(String id) { return _pages.get(id);	}
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
	
	// this is a list of elements to go in the head section of the page for any resources the applications controls or actions may require
	public List<String> getResourceIncludes() { return _resourceIncludes; }
			
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
						String styleClass = css.substring(startPos + 1, endPos);
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
	
	// this adds resources from either a control or action, they are added to the resources collection for printing in the top of each page if they are files, or ammended to the application .js or .css files
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
					
					// build the resource includes if they're files, or append the string builders
					String include = null;
					if ("javascriptFile".equals(resourceType)) {
						include = "<script type='text/javascript' src='" + resourceContents + "'></script>";
					} else if ("cssFile".equals(resourceType)) {
						include = "<link rel='stylesheet' type='text/css' href='" + resourceContents + "'>";
					} else if ("css".equals(resourceType)) {
						css.append("\n/* " + name + " resource styles */\n\n" + resourceContents + "\n");
					} else if ("javascript".equals(resourceType)) {
						js.append("\n/* " + name + " resource JavaScript */\n\n" + resourceContents + "\n");
					}
					
					// check we got something
					if (include != null) {
						// add if not there already
						if (!_resourceIncludes.contains(include)) _resourceIncludes.add(include);
					}
					
				} // resource loop
				
			} // resource check
			
		}
		
	}
		
	// this function initialises the application when its first loaded, initialises the security adapter and builds the rapid.js and rapid.css files
	public void initialise(ServletContext servletContext, boolean createResources) throws JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException, IOException {
										
		// initialise the resource includes collection
		_resourceIncludes = new ArrayList<String>();
		
		// initialise the security adapter 
		if (_securityAdapterType == null) {
			_securityAdapter = new RapidSecurityAdapter(servletContext, this);
			_securityAdapterType = "rapid";
		} else {			
			HashMap<String,Constructor> constructors = (HashMap<String, Constructor>) servletContext.getAttribute("securityConstructors");
			Constructor<SecurityAdapater> constructor = constructors.get(_securityAdapterType);
			if (constructor == null) throw new InstantiationException("Security adapter \"" +  _securityAdapterType + "\" can't be found");
			_securityAdapter = constructor.newInstance(servletContext, this);
		}
		
		// when importing an application we need to initialise but don't want the resource folders made in the old applications name
		if (createResources) {
			
			// get the jsonControls
			JSONArray jsonControls = (JSONArray) servletContext.getAttribute("jsonControls");
					
			// get the jsonActions
			JSONArray jsonActions = (JSONArray) servletContext.getAttribute("jsonActions");
							
			// string builders for the different sections in our rapid.js file
			StringBuilder initJS = new StringBuilder();
			StringBuilder dataJS = new StringBuilder();
			StringBuilder actionJS = new StringBuilder();
			StringBuilder resourceJS = new StringBuilder();
			
			// string builder for our rapid.css file
			StringBuilder resourceCSS = new StringBuilder();
			
			// check controls
			if (jsonControls != null) {
				
				// check control types
				if (_controlTypes != null) {
					
					// make sure the page is included as we hide it from the users in the admin
					if (!_controlTypes.contains("page")) _controlTypes.add(0, "page");
					
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
				        			if (setDataFunction.trim().length() > 0) dataJS.append("\nfunction setData_" + controlType + "(id, data, field, details) {\n  " + setDataFunction.trim().replace("\n", "\n  ") + "\n}\n");
				        			
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
			String applicationPath = servletContext.getRealPath("/applications/" + _id);		
			File applicationFolder = new File(applicationPath);		
			if (!applicationFolder.exists()) applicationFolder.mkdirs();
			
			// write the rapid.js file
			FileOutputStream fos = new FileOutputStream (applicationPath + "/rapid.js");
			PrintStream ps = new PrintStream(fos);
			
			ps.print("\n/* This file is auto-generated on application load and save */\n");
			ps.print("\n\n/* Action methods */\n\n");
			ps.print(actionJS.toString());
			ps.print("\n\n/* Control initialise methods */\n\n");
			ps.print(initJS.toString());
			ps.print("\n\n/* Control getData and setData methods */\n\n");
			ps.print(dataJS.toString());
			ps.print("\n\n/* Control and Action resource JavaScript */\n\n");
			ps.print(resourceJS.toString());

			ps.close();
			fos.close();
			
			// write the rapid.css file
			fos = new FileOutputStream (applicationPath + "/rapid.css");
			ps = new PrintStream(fos);
			ps.print("\n/* This file is auto-generated on application load and save */\n");		
			ps.print(resourceCSS.toString());
			ps.print("\n\n/* Application styles */\n\n");
			ps.print(_styles);
			ps.close();
			fos.close();
			
		}
										
		// populate the list of style classes by scanning the rapid.css
		_styleClasses = scanStyleClasses(_styles);
								
	}
	
	public int rebuildPages(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws JSONException {
		
		// how many pages were rebuilt
		int pages = 0;
		
		// log that the script was loaded ok
        rapidServlet.getLogger().debug("Initilising script engine to rebuild pages for " + rapidRequest.getApplication().getTitle());
		
        // get script engine context
        Context cx = Context.enter();
        
        // stop it complaining about the large size of env.js
        cx.setOptimizationLevel(-1);
        
        try {
        	        	        	
            // Initialize the standard objects (Object, Function, etc.)
            // This must be done before scripts can be executed. Returns
            // a scope object that we use in later calls.
            Scriptable scope = cx.initStandardObjects();
            
            // get the path of the scripts folder
            String scriptsFolder = rapidServlet.getServletContext().getRealPath("/scripts/");
            
            // these are the scripts we want to load (in order)
            String[] scripts = {"env.js", "jquery-1.10.2.js", "jquery-ui-1.10.3.js", "controls.js", "page.js"};
            
            // loops our script files, reads and loads
            for (String script : scripts) {
            	
            	File scriptFile = new File(scriptsFolder + "/" + script);
            	
            	// get a scanner to read the file
    			Scanner fileScanner = new Scanner(scriptFile).useDelimiter("\\A");
    			
    			// read the xml into a string
    			String js = fileScanner.next();
    			
    			// close the scanner (and file)
    			fileScanner.close();
    			
    			// add the the js to the script engine
                cx.evaluateString(scope, js, script, 1, null);
                
                // log that the script was loaded ok
                rapidServlet.getLogger().debug("Loaded script " + script);
            	
            }
            
            // add a function for getting the alerts that jquery may throw back as errors (there is no alert function in envJs)
            cx.evaluateString(scope, "function alert(message) {	throw new Error('Alert - ' + message); }", "alert", 1, null);
            
            // get the controls to load the class object/functions            
            JSONArray jsonControls = (JSONArray) rapidServlet.getJsonControls();
                     
            // create an args array containing the controls
            Object argsControls[] = { jsonControls };
            
            // get a reference to the loadControls function (in the page.js file)
            Function functionLoadControls = (Function) scope.get("loadControls", scope);
            
            // get a result when calling the function
            functionLoadControls.call(cx, scope, scope, argsControls);            
            
            // check we have pages
            if (_pages != null) {
            	
            	// loop the sorted pages
            	for (Page page : getSortedPages()) {
            		            		
            		// only if it has controls
                    if (page.getControls() != null) {

                    	// get a json object for the page controls
                        JSONArray jsonPageControls = new JSONArray(page.getControls());
                        
                        // create an args array containing the page
                        Object argsPage[] = { jsonPageControls };
                        
                        // get a reference to the loadControls function (in the page.js file)
                        Function functionHtmlBody = (Function) scope.get("getHtmlBody", scope);
                        
                        // get a result when calling the function
                        Object resultLoadPage = functionHtmlBody.call(cx, scope, scope, argsPage);
                        
                        // this the page html
                        page.setHtmlBody(resultLoadPage.toString());
                        
                        // save the page
                        page.save(rapidServlet, rapidRequest);
                        
                        // log that the page was rebuilt ok
                        rapidServlet.getLogger().debug("Rebuilt page " + page.getName());
                        
                        // increment pages counter
                        pages ++;
                        
                    }             		
            		
            	}
            	
            }      
            
        } catch (Exception ex) {
        	
        	rapidServlet.getLogger().error("Rebuilding page ERROR : " + ex.getMessage(), ex);
        	
        	throw new JSONException(ex);
                        
        } finally {
            // Exit from the context.
            Context.exit();
        }
        
        return pages;
	                       
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
				
		File backupFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + Rapid.BACKUP_FOLDER + "/"));
		
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
				backupFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + Rapid.BACKUP_FOLDER + "/" + backups.get(0).getId()));
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
				
		File backupFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + _id + "/" + Rapid.BACKUP_FOLDER + "/"));
		
		if (backupFolder.exists()) {
			
			for (File backup : backupFolder.listFiles()) {
				
				String id = backup.getName();
				
				String[] nameParts = id.split("_");
				
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
					
					backups.add(new Backup(id, name, date, userParts[0], size));
															
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
	
	public void backup(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws IOException {
		
		// get the username
		String userName = rapidRequest.getUserName();
		if (userName == null) userName = "unknown";
		
		// get the current date and time in a string
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateString = formatter.format(new Date());
		
		// create a fileName for the archive
		String fileName = _id + "_" + dateString + "_" + Files.safeName(userName);
								
		// create folders to backup the app
		String backupPath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + Rapid.BACKUP_FOLDER + "/" + fileName);		
		File backupFolder = new File(backupPath);		
		if (!backupFolder.exists()) backupFolder.mkdirs();

		// create a file object for the application data folder
		File appFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + _id));
		
		// create a list of files to ignore
		List<String> ignoreFiles = new ArrayList<String>();
		ignoreFiles.add(com.rapid.server.Rapid.BACKUP_FOLDER);
	 	
	 	// copy the existing files and folders to the backup folder    
	    Files.copyFolder(appFolder, backupFolder, ignoreFiles);
	    
	    // create a file object and folders for the web folder archive
	    backupFolder = new File(backupPath + "/WebContent");
	    if (!backupFolder.exists()) backupFolder.mkdirs();
	    
	    // create a file object for the application web folder
	    appFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + _id));
	 		    	    
	 	// copy the existing web content files and folders to the webcontent archive folder    
	    Files.copyFolder(appFolder, backupFolder, ignoreFiles);
	    	
	}
	
	public void copy(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, String newId) throws IOException {
		
		// get the application source folder
		File sourceFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + _id));	
				
		// create folders to copy the app to
		File destFolder = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + newId));		
		if (!destFolder.exists()) destFolder.mkdirs();
		
		// create a list of files to ignore
		List<String> ignoreFiles = new ArrayList<String>();
		ignoreFiles.add(com.rapid.server.Rapid.BACKUP_FOLDER);
				
		// copy the application folders
		Files.copyFolder(sourceFolder, destFolder, ignoreFiles);
		
		// get the resources source folder
		sourceFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + _id));
		
		// create folders to copy the resources to
		destFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + newId));		
		if (!destFolder.exists()) destFolder.mkdirs();
		
		// copy the resource folders
		Files.copyFolder(sourceFolder, destFolder, ignoreFiles);
	    		
	}
		
	public void save(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws JAXBException, IOException, IllegalArgumentException, SecurityException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
						
		// create folders to save the app
		String folderPath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + _id);		
		File folder = new File(folderPath);		
		if (!folder.exists()) folder.mkdirs();
		
		// create a file object for the application
		File appFile = new File(folderPath + "/application.xml");
		// backup the app if it already exists
		if (appFile.exists()) backup(rapidServlet, rapidRequest);
		
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
	    
	    // add this application to the application map
	    rapidServlet.getApplications().put(_id, this);
	    
	    // initialise the application
	    initialise(rapidServlet.getServletContext(), true);
	    		
	}
	
	public void delete(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws JAXBException, IOException {
		
		// get folders to save locate app file
		String folderPath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + _id);		
		
		// create a file object for the application folder
		File appFolder = new File(folderPath);
		
		// create a file object for the webcontent folder
		File webFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + _id));
				
		// create a file object for the application
		File appFile = new File(folderPath + "/application.xml");
						
		// if the app file exists
		if (appFile.exists()) {
			// backup the application
			backup(rapidServlet, rapidRequest);
			// delete the app folder
			Files.deleteRecurring(appFolder);
			// delete the web folder
			Files.deleteRecurring(webFolder);
			// remove this application from the collection
			rapidServlet.getApplications().remove(_id);
		}
			    	    		
	}
	
	public void zip(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws JAXBException, IOException {
		
		// create folders to save locate app file
		String folderPath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + _id);		
		
		// create a file object for the application
		File appFile = new File(folderPath + "/application.xml");
								
		// if the app file exists
		if (appFile.exists()) {
			
			// create a file object for the application folder
			File appFolder = new File(folderPath);
			
			// create a file object for the webcontent folder
			File webFolder = new File(rapidServlet.getServletContext().getRealPath("/applications/" + _id));
			
			// create a list of sources for our zip
			ArrayList<ZipSource> zipSources = new ArrayList<ZipSource>();
			
			// loop the contents of the appFolder
			for (File file : appFolder.listFiles()) {
				// add this file to the WebContent path
				zipSources.add(new ZipSource(file,"WEB-INF"));
			}
			
			// loop the contents of the webFolder
			for (File file : webFolder.listFiles()) {
				// add this file to the WEB-INF path
				zipSources.add(new ZipSource(file,"WebContent"));
			}
			
			// get a file for the temp directory
			File tempDir = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/temp"));
			// create it if not there
			if (!tempDir.exists()) tempDir.mkdir();
									
			// create the zip file object with our destination
			ZipFile zipFile = new ZipFile(new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/temp/" + _id + ".zip")));

			// create a list of files to ignore
			ArrayList<String> ignoreFiles = new ArrayList<String>();
			// don't include any files or folders from the back in the .zip
			ignoreFiles.add(Rapid.BACKUP_FOLDER);
			
			// zip the sources into the file
			zipFile.zipFiles(zipSources, ignoreFiles);

		}
			    	    		
	}

	
	// static methods
	
	// this is a simple overload for default loading of applications where the resources are all regenerated
	public static Application load(ServletContext servletContext, File file) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException {
				
		return load(servletContext, file, true); 		
		
	}
	
	// this method loads the application by ummarshelling the xml, and then doing the same for all page .xmls, before calling the initialise method
	public static Application load(ServletContext servletContext, File file, boolean createResources) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException {
		
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
			
			// get the logger
			Logger logger = (Logger) servletContext.getAttribute("logger");
			
			// log the difference
			logger.debug("Application " + name + " with version " + xmlVersion + ", current version is " + XML_VERSION);
			
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
			
			logger.debug("Updated " + name + " application version to " + XML_VERSION);
			
		}
		
		// get the unmarshaller from the context
		Unmarshaller unmarshallerObj = (Unmarshaller) servletContext.getAttribute("unmarshaller");	
		// unmarshall the application
		Application application = (Application) unmarshallerObj.unmarshal(file);
		
		// look for pages
		File pagesFolder = new File(file.getParent() + "/pages");
		if (pagesFolder.exists()) {
			
			// create a filter for finding .page.xml files
			FilenameFilter xmlFilenameFilter = new FilenameFilter() {
		    	public boolean accept(File dir, String name) {
		    		return name.toLowerCase().endsWith(".page.xml");
		    	}
		    };
		    
		    // loop the .page.xml files and add to the application
		    for (File pageFile : pagesFolder.listFiles(xmlFilenameFilter)) {
		    	application.addPage(Page.load(servletContext, pageFile));
		    }
			
		}
										
		// initialise the application
		application.initialise(servletContext, createResources);
		
		return application; 		
		
	}
	
	// delete an application backup
	public static void deleteBackup(RapidHttpServlet rapidServlet, String backupId) {
		// get the backup folder into a file object
		File backup = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + Rapid.BACKUP_FOLDER + "/" + backupId));
		// delete
		Files.deleteRecurring(backup);
	}
	
}
