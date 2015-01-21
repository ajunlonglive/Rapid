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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.core.Application.RapidLoadingException;
import com.rapid.core.Applications;
import com.rapid.core.Device.Devices;
import com.rapid.data.ConnectionAdapter;
import com.rapid.utils.Files;
import com.rapid.utils.JAXB.EncryptedXmlAdapter;
import com.rapid.utils.Encryption;
import com.rapid.utils.Strings;
import com.rapid.utils.ZipFile;

public class RapidServletContextListener implements ServletContextListener {
	
	// the logger which we will initialise
	private static Logger _logger;
	
	// this monitor class runs on it's own thread and removed pages from memory that have not been accessed in more than a specified time
	private static class Monitor extends Thread {

		// private instance variables
		private ServletContext _servletContext;
		private int _interval, _maxPageAge;
		private boolean _stopped;
		
		// constructor
		public Monitor(ServletContext servletContext, int interval, int maxPageAge) {
			_servletContext = servletContext;
			_interval = interval;
			_maxPageAge = maxPageAge;
		}
		
		// worker method
		@Override
		public void run() {
			
			// log that we've started
			_logger.info("Monitor has started, checking every " + _interval + " seconds for pages not accessed in the last " + _maxPageAge + " seconds");
			
			// loop until stopped
			while (!_stopped) {
								
				// sleep for set interval
				try {
					Thread.sleep(_interval * 1000);
				} catch (InterruptedException ex) {
					_stopped = true;
				}
				
				// if we're still running
				if (!_stopped) {
					
					// log start of checking
					_logger.debug("Monitor is checking for stale pages");
					
					// get the current date / time
					Date now = new Date();
					
					// get the applications
					Applications applications = (Applications) _servletContext.getAttribute("applications");
					
					// loop them
					for (Application application : applications.get()) {
						// get old pages
						application.getPages().clearOldPages(now, _maxPageAge);						
					}										
				}
			}
			
			_logger.info("Monitor has stopped");
						
		}

		// interrupt
		@Override
		public void interrupt() {
			_stopped = true;
			super.interrupt();
		}
		
	}
	
	// the schema factory that we will load the actions and controls schemas into
	private static SchemaFactory _schemaFactory;
	
	// all of the classes we are going to put into our jaxb context
	private static ArrayList<Class> _jaxbClasses;
	
	// our monitor class
	private Monitor _monitor;
	
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
	
	// Here we loop all of the folders under "applications" looking for a application.xml file, copying to the latest version if found before loading the versions
	public static int loadApplications(ServletContext servletContext) throws JAXBException, JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException, RapidLoadingException, XPathExpressionException {
		
		// instatiate a new applications collection which allows us to retrieve by id and version
		Applications applications = new Applications();
		
		File applicationFolderRoot = new File(servletContext.getRealPath("/WEB-INF/applications/"));
		
		for (File applicationFolder : applicationFolderRoot.listFiles()) {
			
			if (applicationFolder.isDirectory()) {
				
				// get the list of files in this folder - should be all version folders
				File[] applicationFolders = applicationFolder.listFiles();
				
				// assume we didn't need to version
				boolean versionCreated = false;
				
				// if we got some
				if (applicationFolders != null) {
					
					// look for an application file in the root of the application folder
					File applicationFile = new File(applicationFolder.getAbsoluteFile() + "/application.xml");
					
					// set a version for this app (just in case it doesn't have one)
					String version = "1";
					
					// if it exists here, it's in the wrong (non-versioned) place!
					if (applicationFile.exists()) {
																				
						// create a file for the new version folder
						File versionFolder = new File(applicationFolder + "/" + version);
						// keep appending the version if the folder already exists
						while (versionFolder.exists()) {
							// append .1 to the version 1, 1.1, 1.1.1, etc
							version += ".1";
							versionFolder = new File(applicationFolder + "/" + version);
						}
						
						// make the dir
						versionFolder.mkdir();
						_logger.info(versionFolder + " created");
						// copy in all files and pages folder
						for (File file : applicationFolders) {
							// copy all files and the pages folder
							if (!file.isDirectory() || (file.isDirectory() && "pages".equals(file.getName()))) {
								// make a desintation file
								File destFile = new File(versionFolder + "/" + file.getName());
								// this is not a version folder itself, copy it to the new version folder
								Files.copyFolder(file, destFile);
								// delete the file or folder
								Files.deleteRecurring(file);
								// log
								_logger.info(file + " moved to " + destFile);
							}							
							
						}
						// record that we created a version
						versionCreated = true;
						
					}	// application.xml non-versioned check
					
					// get the version folders
					File[] versionFolders = applicationFolder.listFiles();
					// get a marsheller
					Marshaller marsheller = RapidHttpServlet.getMarshaller();
					// loop them
					for (File versionFolder : versionFolders) {
						// check is folder
						if (versionFolder.isDirectory()) {
							// look for an application file in the version folder
							applicationFile = new File(versionFolder + "/application.xml");
							// if it exists
							if (applicationFile.exists()) {								
								
								// placeholder for the application we're going to version up or just load
								Application application = null;								
								
								// if we had to create a version for it
								if (versionCreated) {
									
									// load without resources
									application = Application.load(servletContext, applicationFile, false);
									
									// set the new version
									application.setVersion(version);
									
									// re-initialise it without resources (for the security adapter)
									application.initialise(servletContext, false);
									
									// marshal the updated application object to it's file
									FileOutputStream fos = new FileOutputStream(applicationFile);		
									marsheller.marshal(application, fos);	    
								    fos.close();
								    
									// get a dir for the pages
									File pageDir = new File(versionFolder + "/pages");
									// check it exists
									if (pageDir.exists()) {
										// loop the pages files
										for (File pageFile : pageDir.listFiles()) {
											// read the contents of the file
											String pageContent = Strings.getString(pageFile);
											// replace all old file references
											pageContent = pageContent
												.replace("/" + application.getId() + "/", "/" + application.getId() + "/" + application.getVersion() + "/")
												.replace("~?a=" + application.getId() + "&amp;", "~?a=" + application.getId() + "&amp;" + application.getVersion() + "&amp;");
											// create a file writer
											FileWriter fs = new FileWriter(pageFile);
											// save the changes
											fs.write(pageContent);
											// close the writer
											fs.close();
											_logger.info(pageFile + " updated with new references");
										}
									}																		
									// make a dir for it's web resources
									File webDir = new File(application.getWebFolder(servletContext));
									webDir.mkdir();
									_logger.info(webDir + " created");
									// loop all the files in the parent
									for (File file : webDir.getParentFile().listFiles()) {
										// check not dir
										if (!file.isDirectory()) {
											// create a destination file for the new location
											File destFile = new File(webDir + "/" + file.getName());
											// copy it to the new destination
											Files.copyFile(file, destFile);
											// delete the file or folder
											file.delete();
											_logger.info(file + " moved to " + destFile);
										}
										
									}
																		
								} 
									
								// (re)load the application
								application = Application.load(servletContext, applicationFile);

								// put it in our collection
								applications.put(application);
								
							}
							
						} // folder check
																		
					} // version folder loop		
					
				} // application folders check
				
			} // application folder check
			
		} // application folder loop
		
		// store them in the context
		servletContext.setAttribute("applications", applications);
						
		_logger.info(applications.size() + " applications loaded");
		
		return applications.size();
		
	}
					
	@Override
	public void contextInitialized(ServletContextEvent event) {   
		
		// request windows line breaks to make the files easier to edit (in particular the marshalled .xml files)
		System.setProperty("line.separator", "\r\n");
		
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
			
			// we're looking for a password and salt for the encryption
			char[] password = null;			
			byte[] salt = null;
			// look for the rapid.txt file with the saved password and salt
			File secretsFile = new File(servletContext.getRealPath("/") + "/WEB-INF/security/rapid.txt");
			// if it exists
			if (secretsFile.exists()) {
				// get a file reader
				BufferedReader br = new BufferedReader(new FileReader(secretsFile));
				// read the first line
				String p = br.readLine();
				// read the next line
				String s = br.readLine();
				// close the reader
				br.close();
				// set the password
				password = p.toCharArray();
				// set the salt
				salt = Encryption.base64Decode(s);
			} 
			
			// create the encypted xml adapter (if the file above is not found there no encryption will occur)
			RapidHttpServlet.setEncryptedXmlAdapter(new EncryptedXmlAdapter(password, salt));
			
			// initialise the schema factory (we'll reuse it in the various loaders)
			_schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			
			// initialise the list of classes we're going to want in the JAXB context (the loaders will start adding to it)
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
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.NameRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MinOccursRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MaxOccursRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MaxLengthRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.MinLengthRestriction.class);
			_jaxbClasses.add(com.rapid.soa.SOAElementRestriction.EnumerationRestriction.class);
			_jaxbClasses.add(com.rapid.soa.Webservice.class);
			_jaxbClasses.add(com.rapid.soa.SQLWebservice.class);
			_jaxbClasses.add(com.rapid.soa.JavaWebservice.class);			
			_jaxbClasses.add(com.rapid.core.Validation.class);
			_jaxbClasses.add(com.rapid.core.Action.class);
			_jaxbClasses.add(com.rapid.core.Event.class);
			_jaxbClasses.add(com.rapid.core.Style.class);			
			_jaxbClasses.add(com.rapid.core.Control.class);			
			_jaxbClasses.add(com.rapid.core.Page.class);
			_jaxbClasses.add(com.rapid.core.Application.class);
			_jaxbClasses.add(com.rapid.core.Device.class);
			_jaxbClasses.add(com.rapid.core.Device.Devices.class);
									
			// convert arraylist to array
			Class[] classes = _jaxbClasses.toArray(new Class[_jaxbClasses.size()]);			
			// re-init the JAXB context to include our injectable classes					
			JAXBContext jaxbContext = JAXBContext.newInstance(classes);
			
			// store the jaxb context in RapidHttpServlet
			RapidHttpServlet.setJAXBContext(jaxbContext);
												
			// load the applications!
			loadApplications(servletContext);	
			
			// load the devices
			Devices.load(servletContext);
			
			// add some useful global objects 
			servletContext.setAttribute("xmlDateFormatter", new SimpleDateFormat("yyyy-MM-dd"));
			servletContext.setAttribute("xmlDateTimeFormatter", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
			
			String localDateFormat = servletContext.getInitParameter("localDateFormat");			
			if (localDateFormat == null) localDateFormat = "dd/MM/yyyy";
			servletContext.setAttribute("localDateFormatter", new SimpleDateFormat(localDateFormat));
			
			String localDateTimeFormat = servletContext.getInitParameter("localDateTimeFormat");
			if (localDateTimeFormat == null) localDateTimeFormat = "dd/MM/yyyy HH:mm a";
			servletContext.setAttribute("localDateTimeFormatter", new SimpleDateFormat(localDateTimeFormat));
			
			boolean actionCache = Boolean.parseBoolean(servletContext.getInitParameter("actionCache"));
			if (actionCache) servletContext.setAttribute("actionCache", new ActionCache(servletContext));
			
			int pageAgeCheckInterval = 1800; 
			try { 
				String pageAgeCheckIntervalString = servletContext.getInitParameter("pageAgeCheckInterval");
				if (pageAgeCheckIntervalString != null) pageAgeCheckInterval = Integer.parseInt(pageAgeCheckIntervalString);	
			} catch (Exception ex) {
				_logger.error("pageAgeCheckInterval is not an integer");
			}
						
			int pageMaxAge = 1800;
			try { 
				String pageMaxAgeString = servletContext.getInitParameter("pageMaxAge");
				if (pageMaxAgeString != null) pageAgeCheckInterval = Integer.parseInt(pageMaxAgeString);	
			} catch (Exception ex) {
				_logger.error("pageMaxAge is not an integer");
			}

			// start the monitor
			_monitor = new Monitor(servletContext, pageAgeCheckInterval, pageMaxAge);
			_monitor.start();
								    		  									
		} catch (Exception ex) {	
			
			_logger.error("Error loading applications : " + ex.getMessage());
			
			ex.printStackTrace();
		}
							
	}
 	
	@Override
	public void contextDestroyed(ServletContextEvent event){
			
		_logger.info("Shutting down...");
		
		// interrupt the monitor
		_monitor.interrupt();
		
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
