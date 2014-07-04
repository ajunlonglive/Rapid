package com.rapid.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import com.rapid.security.SecurityAdapater.User;
import com.rapid.server.Rapid;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidHttpServlet.RapidRequest;
import com.rapid.utils.Files;
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
	private List<String> _javascriptFiles, _cssFiles;
	private List<Control> _controls;
	private List<Event> _events;
	private List<Style> _styles;
	private List<String> _sessionVariables;
	private List<String> _roles;
	private List<RoleHtml> _rolesHtml;
	private Lock _lock;
	
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
	
	// any bespoke javascript files can be added to the page by manually setting this property in the page xml
	public List<String> getJavascriptFiles() { return _javascriptFiles; }
	public void setJavascriptFiles(List<String> javascriptFiles) { _javascriptFiles = javascriptFiles; }
	
	// any bespoke css files can be added to the page by manually setting this property in the page xml
	public List<String> getCssFiles() { return _cssFiles; }
	public void setCssFiles(List<String> cssFiles) { _cssFiles = cssFiles; }
	
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
		_xmlVersion = XML_VERSION;
	};
		
	// instance methods
	
	// these two methods have different names to avoid being marshelled to the .xml file by JAXB
	public String getCachedStartHtml() {
		return _cachedStartHtml;
	}
	public void cacheStartHtml(String html) {
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
	private void getOtherPageChildControls(JSONArray jsonControls, List<Control> controls) throws JSONException {
		// check we were given some controls
		if (controls != null) {
			// loop the controls
			for (Control control : controls) {
				// if this control can be used from other pages
				if (control.getCanBeUsedFromOtherPages()) {
					// make a simple JSON object with what we need about this control
					JSONObject jsonControl = new JSONObject();
					jsonControl.put("id", control.getId());
					jsonControl.put("type", control.getType());
					jsonControl.put("name", control.getName());
					jsonControls.put(jsonControl);
				}
				// run for any child controls
				getOtherPageChildControls(jsonControls, control.getChildControls());				
			}			
		}
	}
		
	// uses the above iterative method to return a flat array of controls in this page that can be used from other pages, for use in the designer
	public JSONArray getOtherPageControls() throws JSONException {
		// the array we're about to return
		JSONArray jsonControls = new JSONArray();
		// start building the array using the page controls
		getOtherPageChildControls(jsonControls, _controls);
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
		
	public String getStylesFile() {
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
								
	public void backup(RapidHttpServlet rapidServlet, RapidRequest rapidRequest, File pageFile) throws IOException {
		
		// get the application
		Application application = rapidRequest.getApplication();
			
		// get the user name
		String userName = Files.safeName(rapidRequest.getUserName());		
		
		// create folders to archive the pages
		String archivePath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + application.getId() + "/" + Rapid.BACKUP_FOLDER);		
		File archiveFolder = new File(archivePath);		
		if (!archiveFolder.exists()) archiveFolder.mkdirs();
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateString = formatter.format(new Date());
		
		 // create a file object for the archive file
	 	File archiveFile = new File(archivePath + "/" + Files.safeName(_name) + "_" + dateString + "_" + userName + ".page.xml");
	 	
	 	// copy the existing new file to the archive file
	    Files.copyFile(pageFile, archiveFile);
		
	}
	
	public void save(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws JAXBException, IOException {
		
		// get the application
		Application application = rapidRequest.getApplication();
		
		// create folders to save the pages
		String pagePath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + application.getId() + "/pages");		
		File pageFolder = new File(pagePath);		
		if (!pageFolder.exists()) pageFolder.mkdirs();
										
		// create a file object for the new file
	 	File newFile = new File(pagePath + "/" + Files.safeName(getName()) + ".page.xml");
	 	
	 	// if the new file already exists it needs archiving
	 	if (newFile.exists()) backup(rapidServlet, rapidRequest, newFile);	 			 	
				
	 	// create a file for the temp file
	    File tempFile = new File(pagePath + "/" + Files.safeName(getName()) + "-saving.page.xml");	
	    
	    // update the modified by and date
	    _modifiedBy = rapidRequest.getUserName();
	    _modifiedDate = new Date();
		
		// create a file output stream for the temp file
		FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());
		// marshall the page object into the temp file
		rapidServlet.getMarshaller().marshal(this, fos);
		// close the file writer
	    fos.close();
	    	        	    	    	    	 	 		   	 	
	 	// copy the tempFile to the newFile
	    Files.copyFile(tempFile, newFile);
	    
	    // delete the temp file
	    tempFile.delete();
	    
		// replace the old page with the new page
		application.addPage(this);
		
		// empty the cached header html
		_cachedStartHtml = null;
		
		// set the fos to the css file
		fos = new FileOutputStream(rapidServlet.getServletContext().getRealPath("/applications/" + application.getId() + "/" + Files.safeName(getName()) + ".css"));
		// get a print stream
		PrintStream ps = new PrintStream(fos);		
		ps.print("\n/* This file is auto-generated on page save */\n\n");
		ps.print(getStylesFile());
		// close
		ps.close();
		fos.close();
				
	}
	
	public void delete(RapidHttpServlet rapidServlet, RapidRequest rapidRequest) throws JAXBException, IOException {
		
		// get the application
		Application application = rapidRequest.getApplication();
				
		// create folders to delete the page
		String pagePath = rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + application.getId() + "/pages");		
														
		// create a file object for the delete file
	 	File delFile = new File(pagePath + "/" + Files.safeName(getName()) + ".page.xml");
	 	
	 	// if the new file already exists it needs archiving
	 	if (delFile.exists()) {
	 		// archive the page file
	 		backup(rapidServlet, rapidRequest, delFile);
	 		// delete the page file
	 		delFile.delete();
	 		// remove it from the current list of pages
		 	application.removePage(_id);
	 	}
	 		 				
	}
		
	public static Page load(ServletContext servletContext, File file) throws JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
		
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
			
			// get the logger
			Logger logger = (Logger) servletContext.getAttribute("logger");
			
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
		// unmarshall the page
		return (Page) unmarshaller.unmarshal(file);
				
	}
	
	// delete a page backup
	public static void deleteBackup(RapidHttpServlet rapidServlet, String appId, String backupId) {
		// get the backup folder into a file object
		File backup = new File(rapidServlet.getServletContext().getRealPath("/WEB-INF/applications/" + appId + "/" + Rapid.BACKUP_FOLDER + "/" + backupId));
		// delete
		backup.delete();
	}

}
