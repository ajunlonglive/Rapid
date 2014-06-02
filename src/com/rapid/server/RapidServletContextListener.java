package com.rapid.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.data.ConnectionAdapter;
import com.rapid.utils.Files;
import com.rapid.utils.ZipFile;

public class RapidServletContextListener implements ServletContextListener {
	
	// the logger which we will initialise
	private static Logger _logger;
	
	// the schema factory that we will load the actions and controls schemas into
	private static SchemaFactory _schemaFactory;
	
	// all of the classes we are going to put into our jaxb context
	private static ArrayList<Class> _jaxbClasses;
	
	public static void logFileNames(File dir, String rootPath) {
		
		for (File file : dir.listFiles()) {
			
			if (file.isDirectory()) {
				
				logFileNames(file, rootPath);				
				
			} else {
				
				String fileName = file.toString();
				
				_logger.info(fileName.substring(rootPath.length()));
				
			}
			
		}
		
	}
	
	// copy files placed in WEB-INF/custom into position in the application
	public static void copyCustomFiles(ServletContext servletContext) throws IOException {
		
		// get the custom directory into a file object
		File customDir = new File(servletContext.getRealPath("/WEB-INF/custom/"));
		
		// if it exists
		if (customDir.exists()) {
			
			// create a filter for finding .jar or .zip files
			FilenameFilter customFilenameFilter = new FilenameFilter() {
		    	public boolean accept(File dir, String name) {
		    		return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip");
		    	}
		    };
		    
		    // filter the files in the custom folder
		    File[] customFiles = customDir.listFiles(customFilenameFilter);
			
			// if it contains any .jar files
			if (customFiles.length > 0) {
				
				// the reason we want all custom stuff in a .jar is so that it can all be compiled in a seperate java project and easily exported
				_logger.info("Custom .jar files found");
				
				// loop the jar files
				for (File customFile : customFiles) {
					
					_logger.info("Uncompressing " + customFile.getName());
					
					// unzip the .jar
					ZipFile zipFile = new ZipFile(customFile);
					
					// create a File dir in which to unzip this file
					File dirCustomRoot = new File(customDir.getAbsolutePath() + "/" + customFile.getName().replace(".jar", "").replace(".zip", ""));
					
					// unzip to this location
					zipFile.unZip(dirCustomRoot);
					
					// create a File dir where custom classes would be - always "com"
					File dirCustomClasses = new File(dirCustomRoot.getAbsolutePath() + "/com");
					
					// check exists
					if (dirCustomClasses.exists()) {
						
						// get a dir for where we're going to copy these to
						File dirClasses = new File(servletContext.getRealPath("/") + "/WEB-INF/classes/com");
						
						// copy them
						Files.copyFolder(dirCustomClasses, dirClasses);
						
						_logger.info("Custom classes copied to server");
						
						logFileNames(dirCustomClasses, dirCustomRoot.getAbsolutePath());
						
					}
					
					// create a File dir for any WebContent - for now if any new filters are added into the web.xml the server will just need to be restarted a second time as it seems too late to re-load the contents
					File dirCustomWebContent = new File(dirCustomRoot.getAbsolutePath() + "/WebContent");
					
					// check exists
					if (dirCustomWebContent.exists()) {
						
						// get a dir for where we're going to copy these to
						File dirWebContent = new File(servletContext.getRealPath("/"));
						
						// copy them
						Files.copyFolder(dirCustomWebContent, dirWebContent);
						
						_logger.info("Custom WebContent copied to server");
						
						logFileNames(dirCustomWebContent, dirCustomWebContent.getAbsolutePath());
						
					}
					
					// rename file to stop it being processed the next time the server loads
					customFile.renameTo(new File(customFile.getAbsoluteFile() + ".done"));
					
				}
												
			}
			
		}
		
	}
	
	public static int loadDatabaseDrivers(ServletContext servletContext) throws Exception {
		
		// create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/WEB-INF/schemas/") + "/databaseDrivers.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();
	    
	    // get a scanner to read the file
		Scanner fileScanner = new Scanner(new File(servletContext.getRealPath("/WEB-INF/database/") + "/databaseDrivers.xml")).useDelimiter("\\A");
		
		// read the xml into a string
		String xml = fileScanner.next();
		
		// close the scanner (and file)
		fileScanner.close();
		
		// validate the control xml file against the schema
		validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
		
		// convert the xml string into JSON
		JSONObject jsonDatabaseDriverCollection = org.json.XML.toJSONObject(xml).getJSONObject("databaseDrivers");
		
		// prepare the array we are going to popoulate
		JSONArray jsonDatabaseDrivers = new JSONArray();
				
		JSONObject jsonDatabaseDriver;
		int index = 0;
		int count = 0;
		
		if (jsonDatabaseDriverCollection.optJSONArray("databaseDriver") == null) {
			jsonDatabaseDriver = jsonDatabaseDriverCollection.getJSONObject("databaseDriver");				
		} else {
			jsonDatabaseDriver = jsonDatabaseDriverCollection.getJSONArray("databaseDriver").getJSONObject(index);
			count = jsonDatabaseDriverCollection.getJSONArray("databaseDriver").length();
		}
		
		do {
			
			// check this type does not already exist
			for (int i = 0; i < jsonDatabaseDrivers.length(); i++) {
				if (jsonDatabaseDriver.getString("name").equals(jsonDatabaseDrivers.getJSONObject(i).getString("name"))) throw new Exception(" database driver type is loaded already. Type names must be unique");
			}
			
			// add the jsonControl to our array
			jsonDatabaseDrivers.put(jsonDatabaseDriver);

			// inc the count of controls in this file
			index++;
			
			// get the next one
			if (index < count) jsonDatabaseDriver = jsonDatabaseDriverCollection.getJSONArray("databaseDriver").getJSONObject(index);
			
		} while (index < count);
		
		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonDatabaseDrivers", jsonDatabaseDrivers);
															
		_logger.info(index + " database drivers loaded from databaseDrivers.xml file");
		
		return index;
		
	}
	
	// loop all of the .connectionAdapter.xml files and check the injectable classes, so we can re-initialise JAXB context to be able to serialise them, and cache their constructors for speedy initialisation
	public static int loadConnectionAdapters(ServletContext servletContext) throws Exception {
			
		int adapterCount = 0;
		
		// retain our class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> connectionConstructors = new HashMap<String,Constructor>();
						
		// create an array list of json objects which we will sort later according to the order
		ArrayList<JSONObject> connectionAdapters = new ArrayList<JSONObject>();
		
		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/WEB-INF/database/"));
		
		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".connectionadapter.xml");
	    	}
	    };
	    
	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/WEB-INF/schemas/") + "/connectionAdapter.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();
	    	
		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) { 
			
			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");
			
			// read the xml into a string
			String xml = fileScanner.next();
			
			// close the scanner (and file)
			fileScanner.close();
			
			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
			
			// convert the string into JSON
			JSONObject jsonConnectionAdapter = org.json.XML.toJSONObject(xml).getJSONObject("connectionAdapter");
			
			// get the class name from the json
			String className = jsonConnectionAdapter.getString("class");
			// get the class 
			Class classClass = Class.forName(className);
			// check the class extends com.rapid.data.ConnectionAdapter
			if (!com.rapid.data.ConnectionAdapter.class.equals(classClass.getSuperclass())) throw new Exception(className + " must extend " + classClass.getCanonicalName()); 
			// check this class is unique
			if (connectionConstructors.get(className) != null) throw new Exception(className + " connection adapter already loaded.");
			// add to constructors hashmap referenced by type
			connectionConstructors.put(className, classClass.getConstructor(ServletContext.class, String.class, String.class, String.class, String.class));
			
			// add to to our array list
			connectionAdapters.add(jsonConnectionAdapter);
												
			// increment the count
			adapterCount++;
			
		}
		
		// sort the connection adapters according to their order property
		Collections.sort(connectionAdapters, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				try {
					return o1.getInt("order") - o2.getInt("order");
				} catch (JSONException e) {
					return 999;
				}
			}			
		});
		
		// create a JSON Array object which will hold json for all of the available security adapters
		JSONArray jsonConnectionAdapters = new JSONArray();
		
		// loop the sorted connection adapters and add to the json array
		for (JSONObject jsonConnectionAdapter : connectionAdapters) jsonConnectionAdapters.put(jsonConnectionAdapter);
		
		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonConnectionAdapters", jsonConnectionAdapters);
		
		// put the constructors hashmapin a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("securityConstructors", connectionConstructors);
															
		_logger.info(adapterCount + " connection adapters loaded in .connectionAdapter.xml files");
		
		return adapterCount;
		
	}
		
	// loop all of the .securityAdapter.xml files and check the injectable classes, so we can re-initialise JAXB context to be able to serialise them, and cache their constructors for speedy initialisation
	public static int loadSecurityAdapters(ServletContext servletContext) throws Exception {
		
		int adapterCount = 0;
		
		// retain our class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> securityConstructors = new HashMap<String,Constructor>();
		
		// create a JSON Array object which will hold json for all of the available security adapters
		JSONArray jsonSecurityAdapters = new JSONArray();
		
		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/WEB-INF/security/"));
		
		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".securityadapter.xml");
	    	}
	    };
	    
	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/WEB-INF/schemas/") + "/securityAdapter.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();
	    	
		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) { 
			
			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");
			
			// read the xml into a string
			String xml = fileScanner.next();
			
			// close the scanner (and file)
			fileScanner.close();
			
			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
			
			// convert the string into JSON
			JSONObject jsonSecurityAdapter = org.json.XML.toJSONObject(xml).getJSONObject("securityAdapter");
			
			// get the type from the json
			String type = jsonSecurityAdapter.getString("type");
			// get the class name from the json
			String className = jsonSecurityAdapter.getString("class");
			// get the class 
			Class classClass = Class.forName(className);
			// check the class extends com.rapid.Action
			if (!com.rapid.security.SecurityAdapater.class.equals(classClass.getSuperclass())) throw new Exception(type + " security adapter class must extend " + classClass.getCanonicalName()); 
			// check this type is unique
			if (securityConstructors.get(type) != null) throw new Exception(type + " security adapter already loaded. Type names must be unique.");
			// add to constructors hashmap referenced by type
			securityConstructors.put(type, classClass.getConstructor(ServletContext.class, Application.class));
			
			// add to our collection
			jsonSecurityAdapters.put(jsonSecurityAdapter);
									
			// increment the count
			adapterCount++;
			
		}
		
		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonSecurityAdapters", jsonSecurityAdapters);
		
		// put the constructors hashmapin a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("securityConstructors", securityConstructors);
															
		_logger.info(adapterCount + " security adapters loaded in .securityAdapter.xml files");
		
		return adapterCount;
		
	}
		
	// loop all of the .action.xml files and check the injectable classes, so we can re-initialise JAXB context to be able to serialise them, and cache their constructors for speedy initialisation
	public static int loadActions(ServletContext servletContext) throws Exception {
		
		int actionCount = 0;
		
		// retain our class constructors in a hashtable - this speeds up initialisation
		HashMap<String,Constructor> actionConstructors = new HashMap<String,Constructor>();
		
		// build a collection of classes so we can re-initilise the JAXB context to recognise our injectable classes
		ArrayList<Action> actions = new ArrayList<Action>(); 
		
		// create a JSON Array object which will hold json for all of the available controls
		JSONArray jsonActions = new JSONArray();
		
		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/WEB-INF/actions/"));
		
		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".action.xml");
	    	}
	    };
	    
	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/WEB-INF/schemas/") + "/action.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();
	    	
		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) { 
			
			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");
			
			// read the xml into a string
			String xml = fileScanner.next();
			
			// close the scanner (and file)
			fileScanner.close();
			
			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
			
			// convert the string into JSON
			JSONObject jsonActionCollection = org.json.XML.toJSONObject(xml).getJSONObject("actions");
			
			JSONObject jsonAction;
			int index = 0;
			int count = 0;
			
			// the JSON library will add a single key of there is a single class, otherwise an array
			if (jsonActionCollection.optJSONArray("action") == null) {
				jsonAction = jsonActionCollection.getJSONObject("action");				
			} else {
				jsonAction = jsonActionCollection.getJSONArray("action").getJSONObject(index);
				count = jsonActionCollection.getJSONArray("action").length();
			}
			
			do {
				
				// add the jsonControl to our array
				jsonActions.put(jsonAction);
				// get the named type from the json
				String type = jsonAction.getString("type");
				// get the class name from the json
				String className = jsonAction.getString("class");
				// get the class 
				Class classClass = Class.forName(className);
				// check the class extends com.rapid.Action
				if (!com.rapid.core.Action.class.equals(classClass.getSuperclass())) throw new Exception(type + " action class must extend " + classClass.getCanonicalName()); 
				// check this type is unique
				if (actionConstructors.get(type) != null) throw new Exception(type + " action already loaded. Type names must be unique.");
				// add to constructors hashmap referenced by type
				actionConstructors.put(type, classClass.getConstructor(RapidHttpServlet.class, JSONObject.class));
				// add to our jaxb classes collection				
				_jaxbClasses.add(classClass);	
				// inc the control count
				actionCount ++;
				// inc the count of controls in this file
				index++;
				
				// get the next one
				if (index < count) jsonAction = jsonActionCollection.getJSONArray("control").getJSONObject(index);
				
			} while (index < count);
			
		}
		
		// put the jsonControls in a context attribute (this is available via the getJsonActions method in RapidHttpServlet)
		servletContext.setAttribute("jsonActions", jsonActions);
		
		// put the constructors hashmapin a context attribute (this is available via the getContructor method in RapidHttpServlet)
		servletContext.setAttribute("actionConstructors", actionConstructors);
															
		_logger.info(actionCount + " actions loaded in .action.xml files");
		
		return actionCount;
		
	}
 
	// here we loop all of the control.xml files and instantiate the json class object/functions and cache them in the servletContext
	public static int loadControls(ServletContext servletContext) throws Exception {
		
		int controlCount = 0;
				
		// create a JSON Array object which will hold json for all of the available controls
		JSONArray jsonControls = new JSONArray();
		
		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("/WEB-INF/controls/"));
		
		// create a filter for finding .control.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".control.xml");
	    	}
	    };
	    
	    // create a schema object for the xsd
	    Schema schema = _schemaFactory.newSchema(new File(servletContext.getRealPath("/WEB-INF/schemas/") + "/control.xsd"));
	    // create a validator
	    Validator validator = schema.newValidator();
	    	
		// loop the xml files in the folder
		for (File xmlFile : dir.listFiles(xmlFilenameFilter)) { 
			
			// get a scanner to read the file
			Scanner fileScanner = new Scanner(xmlFile).useDelimiter("\\A");
			
			// read the xml into a string
			String xml = fileScanner.next();
			
			// close the scanner (and file)
			fileScanner.close();
			
			// validate the control xml file against the schema
			validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
			
			// convert the string into JSON
			JSONObject jsonControlCollection = org.json.XML.toJSONObject(xml).getJSONObject("controls");
			
			JSONObject jsonControl;
			int index = 0;
			int count = 0;
			
			if (jsonControlCollection.optJSONArray("control") == null) {
				jsonControl = jsonControlCollection.getJSONObject("control");				
			} else {
				jsonControl = jsonControlCollection.getJSONArray("control").getJSONObject(index);
				count = jsonControlCollection.getJSONArray("control").length();
			}
			
			do {
				
				// check this type does not already exist
				for (int i = 0; i < jsonControls.length(); i++) {
					if (jsonControl.getString("type").equals(jsonControls.getJSONObject(i).getString("type"))) throw new Exception(" control type is loaded already. Type names must be unique");
				}
				
				// add the jsonControl to our array
				jsonControls.put(jsonControl);
	
				// inc the control count
				controlCount ++;
				// inc the count of controls in this file
				index++;
				
				// get the next one
				if (index < count) jsonControl = jsonControlCollection.getJSONArray("control").getJSONObject(index);
				
			} while (index < count);
			
		}
		
		// put the jsonControls in a context attribute (this is available via the getJsonControls method in RapidHttpServlet)
		servletContext.setAttribute("jsonControls", jsonControls);
							
		_logger.info(controlCount + " controls loaded in .control.xml files");
		
		return controlCount;
		
	}
	
	// Here we loop all of the folder under "applications" looking for a application.xml file and loading it if so
	public static int loadApplications(ServletContext servletContext) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException {
		
		HashMap<String,Application> applications = new HashMap<String,Application>();
		
		File applicationFolderRoot = new File(servletContext.getRealPath("/WEB-INF/applications/"));
		
		for (File applicationFolder : applicationFolderRoot.listFiles()) {
			if (applicationFolder.isDirectory()) {
				File applicationFile = new File(applicationFolder.getAbsoluteFile() + "/application.xml");
				if (applicationFile.exists()) {
					Application application = Application.load(servletContext, applicationFile);
					applications.put(application.getId(), application);
				}
			}
		}
		
		// store them in the context
		servletContext.setAttribute("applications", applications);
						
		_logger.info(applications.size() + " applications loaded");
		
		return applications.size();
		
	}
	
	@Override
	public void contextInitialized(ServletContextEvent event) {   
		
		// get a reference to the servlet context
		ServletContext servletContext = event.getServletContext();						 		
		
		// set up logging
		try {
			
			// set the log path
			System.setProperty("logPath", servletContext.getRealPath("/") + "/WEB-INF/logs/Rapid.log");
			
			// get a logger
			_logger = Logger.getLogger(RapidHttpServlet.class);
			
			// set the logger and store in servletConext
			servletContext.setAttribute("logger", _logger);
			
			// log!
			_logger.info("Logger created");
		
		} catch (Exception e) {	
			
			System.err.println("Error initilising logging : " + e.getMessage());
			
			e.printStackTrace();
		}		
			
		try {
			
			// copy any files from the WEB-INF/custom folder into the application
			copyCustomFiles(servletContext);
			
			// initialise the schema factory (we'll reuse it in the various loaders)
			_schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			
			// initialise the list of classes
			_jaxbClasses = new ArrayList<Class>();	
			
			// load the database drivers first
			loadDatabaseDrivers(servletContext);
			
			// load the connection adapters 
			loadConnectionAdapters(servletContext);
			
			// load the security adapters 
			loadSecurityAdapters(servletContext);
			
			// load the actions 
			loadActions(servletContext);
						
			// load the controls 
			loadControls(servletContext);						
			
			// add some classes manually
			_jaxbClasses.add(Class.forName("com.rapid.soa.SOAElementRestriction"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.SOAElementRestriction$NameRestriction"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.SOAElementRestriction$MinOccursRestriction"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.SOAElementRestriction$MaxOccursRestriction"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.SOAElementRestriction$MaxLengthRestriction"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.SOAElementRestriction$MinLengthRestriction"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.SOAElementRestriction$EnumerationRestriction"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.Webservice"));
			_jaxbClasses.add(Class.forName("com.rapid.soa.SQLWebservice"));
			_jaxbClasses.add(Class.forName("com.rapid.core.Application"));
			_jaxbClasses.add(Class.forName("com.rapid.core.Validation"));
			_jaxbClasses.add(Class.forName("com.rapid.core.Action"));
			_jaxbClasses.add(Class.forName("com.rapid.core.Event"));
			_jaxbClasses.add(Class.forName("com.rapid.core.Style"));			
			_jaxbClasses.add(Class.forName("com.rapid.core.Control"));			
			_jaxbClasses.add(Class.forName("com.rapid.core.Page"));
			_jaxbClasses.add(Class.forName("com.rapid.core.Application"));
									
			// convert arraylist to array
			Class[] classes = _jaxbClasses.toArray(new Class[_jaxbClasses.size()]);			
			// re-init the JAXB context to include our injectable classes					
			JAXBContext jaxbContext = JAXBContext.newInstance(classes);
			// store as context attribute (this is available via the getJAXBContext method in the RapidHttpServlet)
			servletContext.setAttribute("jaxbContext", jaxbContext);
			
			// initialise the marshallers
			Marshaller marshaller = jaxbContext.createMarshaller();
			// request formatted output
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			// request windows line breaks to make the files easier to edit
			System.setProperty("line.separator", "\r\n");
			// store marshaller (this is available via the getMarshaller method in the RapidHttpServlet)
			servletContext.setAttribute("marshaller", marshaller);
			// store unmarshaller (this is available via the getUnmarshaller method in the RapidHttpServlet)
			servletContext.setAttribute("unmarshaller", jaxbContext.createUnmarshaller());
			
			// load the applications!
			loadApplications(servletContext);	
			
			// add some useful global objects
			servletContext.setAttribute("dateFormatter", new SimpleDateFormat("yyyy-MM-dd"));
			servletContext.setAttribute("dateTimeFormatter", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
					    		  									
		} catch (Exception e) {	
			
			_logger.error("Error loading applications : " + e.getMessage());
			
			e.printStackTrace();
		}
							
	}
 	
	@Override
	public void contextDestroyed(ServletContextEvent event){
			
		_logger.info("Shutting down...");
		
		// get all of the applications
		Map<String, Application> applications = (Map<String, Application>) event.getServletContext().getAttribute("applications");
		// loop them
		for (String key : applications.keySet()) {
			// get the application
			Application application = applications.get(key);
			// check for any connections
			if (application.getDatabaseConnections() != null) {
				// loop them
				for (DatabaseConnection databaseConnection : application.getDatabaseConnections()) {
					// check adapter
					try {
						// get adapter
						ConnectionAdapter connectionAdapter = databaseConnection.getConnectionAdapter(event.getServletContext());
						// if we got one try and close it
						if (connectionAdapter != null) connectionAdapter.close();						
					} catch (Exception ex) {						
						_logger.error("Error closing database adapter for " + application.getName(), ex);						
					}
				}
			}			
		}
		
		// sleep for 2 seconds to allow any database connection cleanup to complete
		try { Thread.sleep(2000); } catch (Exception ex) {}
		
		// This manually deregisters JDBC drivers, which prevents Tomcat 7 from complaining about memory leaks from this class
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                _logger.info(String.format("Deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
            	_logger.error(String.format("Error deregistering driver %s", driver), e);
            }
        }
        
        
        // last log
		_logger.info("Logger shutdown");
		// shutdown logger
		if (_logger != null) LogManager.shutdown();
		
	}

}
