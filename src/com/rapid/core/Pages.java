package com.rapid.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.rapid.core.Application.RapidLoadingException;
import com.rapid.server.RapidHttpServlet;
import com.rapid.utils.Comparators;

public class Pages {
	
	// public static class
	
	public static class PageHeader {
		
		// instance variables
		private String _id, _name, _title;
		private File _file;
		private Date _lastGetDateTime;
		
		// properties
		public String getId() { return _id; }
		public String getName() { return _name; }
		public String getTitle() { return _title; }
		public File getFile() { return _file; }
		
		public Date getLastGetDateTime() { return _lastGetDateTime; }
		public void setLastGetDateTime(Date lastGetDateTime) { _lastGetDateTime = lastGetDateTime; }
		
		// constructors
		public PageHeader(String id, String name, String title, File file) {
			_id = id;
			_name = name;
			_title = title;
			_file = file;
		}
		
		public PageHeader(Page page, File pageFile) {
			_id = page.getId();
			_name = page.getName();
			_title = page.getTitle();
			_file = pageFile;
		}
						
	}
	
	// instance variables
	
	private Logger _logger;
	private Application _application;
	private FilenameFilter _filenameFilter;
	private HashMap<String,PageHeader> _pageHeaders;
	private HashMap<String,Page> _pages;
			
	// constructor
	
	public Pages(Application application) throws RapidLoadingException, ParserConfigurationException, XPathExpressionException, SAXException, IOException {
		// get a logger
		_logger = Logger.getLogger(Pages.class);
		// store the application
		_application = application;
		// initialise the page headers collection
		_pageHeaders = new HashMap<String,PageHeader>();
		// initialise the pages collection
		_pages = new HashMap<String,Page>();
		// create a filter for finding .page.xml files
		_filenameFilter = new FilenameFilter() {
	    	public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".page.xml");
	    	}
	    };	   
	}
	
	// private methods
	
	private Page loadPage(ServletContext servletContext, File pageFile) throws JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
				
		// load the page from file
		Page page = Page.load(servletContext, pageFile);
	
		// add it to the collection
		_pages.put(page.getId(), page);
		
		// we're done
		return page;
		
	}
	
	private String getPageName(File pageFile) {
		
		// assume the page name is the whole path
		String pageName = pageFile.getPath();
		// if it contains the pages folder start from there
		if (pageName.contains(File.separator + "pages" + File.separator)) pageName = pageName.substring(pageName.indexOf(File.separator + "pages" + File.separator) + 7);		    					    		
		// remove any .page.xml
		if (pageName.contains(".page.xml")) pageName = pageName.substring(0, pageName.indexOf(".page.xml"));
		// return
		return pageName;
		
	}
	
	// public methods
	
	// add them singly 
	public void addPage(Page page, File pageFile) {
		// add to page headers
		_pageHeaders.put(page.getId(), new PageHeader(page, pageFile));
		// add to pages collection
		_pages.put(page.getId(), page); 
	}
	
	// remove them one by one too
	public void removePage(String id) {
		// remove from pages
		_pageHeaders.remove(id);
		// remove from page headers
		_pages.remove(id); 
	}
	
	// the number of pages
	public int size() {
		return _pageHeaders.size();
	}
		
	// a list of page id's sorted by rank, the idea is that pages that are used more often will move higher up the rank and lower ranked pages will not be required in memory
	public Set<String> getPageIds() {
		return _pageHeaders.keySet();				
	}
			
	// return a specific page (or the start page if pageId is null)
	public Page getPage(ServletContext servletContext, String pageId) throws RapidLoadingException {
		
		// placeholder for the page
		Page page = null;
		
		// look for the page header
		PageHeader pageHeader = _pageHeaders.get(pageId);
		
		// if there is a page header
		if (pageHeader != null) {
			
			// try and get the page from the collection
			page = _pages.get(pageId);
			
			// if not in collection, load it
			if (page == null) {
				
				// read the string into a file object
				File pageFile = pageHeader.getFile();
				
				// load the page from file
				try {
					
					// load the page
					page = loadPage(servletContext, pageFile);
					
				} catch (Exception ex) {
					
					// throw the exception with the page name
		    		throw new Application.RapidLoadingException("Error loading page " + getPageName(pageFile), ex);
		    		
				}
				
			}
			
			// set the last get time
			pageHeader.setLastGetDateTime(new Date());
			
		}
															
		// return the page
		return page;
		
	}
		
	// get a single page by it's name (used by backups as the name is in the file)
	public Page getPageByName(ServletContext servletContext, String name) throws RapidLoadingException {
		// loop the page headers keyset
		for (String pageId : _pageHeaders.keySet()) {
			// return immediately  with the matching page
			if (_pageHeaders.get(pageId).getName().equals(name)) {
				return getPage(servletContext, pageId);
			}
		}		
		// return if we got here
		return null;	
	}
	
	// we don't want the pages in the application.xml so no setPages to avoid the marshaler
	public ArrayList<PageHeader> getSortedPages() {
		// prepare the list we are going to send back
		ArrayList<PageHeader> pages = new ArrayList<PageHeader>();
		// add each page to the list
		for (String pageId : _pageHeaders.keySet()) {
			pages.add(_pageHeaders.get(pageId));
		}
		// sort the list by the page name
		Collections.sort(pages, new Comparator<PageHeader>() {
			@Override
			public int compare(PageHeader page1, PageHeader page2) {
				return Comparators.AsciiCompare(page1.getName(), page2.getName(), false);
			}
			
		});		
		// return the pages
		return pages; 
	}	

	// clears the pages and reloads the page headers
	public void loadpages(ServletContext servletContext) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		
		// clear the pages
		_pages.clear();
		
		// create a new map for caching page files by id's
	    _pageHeaders = new HashMap<String,PageHeader>();		   
	    
	    // initiate pages folder 
		File pagesFolder = new File(_application.getConfigFolder(servletContext) + "/pages");
		
		// if the folder is there
		if (pagesFolder.exists()) {
			
			// these are not thread safe so can't be reused
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
									
			// create objects for xml parsing	       
		    XPathFactory xPathfactory = XPathFactory.newInstance();
		    XPath xpath = xPathfactory.newXPath();
		    // compile the xpath expressions
		    XPathExpression xpathId = xpath.compile("/page/id");
		    XPathExpression xpathName = xpath.compile("/page/name");
		    XPathExpression xpathTitle = xpath.compile("/page/title");
			
		    // loop the .page.xml files and add to the application
		    for (File pageFile : pagesFolder.listFiles(_filenameFilter)) {
		    	
	    		// parse the xml file
				Document doc = docBuilder.parse(pageFile);
    			
    		    // get the page id from the file
    		    String pageId = xpathId.evaluate(doc);
    		    // get the name
    		    String pageName = xpathName.evaluate(doc);
    		    // get the title
    		    String pageTitle = xpathTitle.evaluate(doc);
    		    			    		    			    		    			    		    
    		    // cache the page id against the file so we don't need to use the expensive parsing operation again
    		    _pageHeaders.put(pageId, new PageHeader(pageId, pageName, pageTitle, pageFile));
		    				    	
		    }
			
		}
		
	}
	
	// removes old pages from the collection
	public void clearOldPages(Date now, int maxPageAge) {
		// list of pages to clear
		List<PageHeader> clearPages = null;
		// loop the pages that have been intialised
		for (String pageId : _pages.keySet()) {
			// get the header
			PageHeader pageHeader = _pageHeaders.get(pageId);
			// if the number of seconds between now and the last get time is greater than our age
			if (now.getTime() - pageHeader.getLastGetDateTime().getTime() > maxPageAge * 1000) {
				// initialise if need be
				if (clearPages == null) clearPages = new ArrayList<PageHeader>();
				// add this id for clearing
				clearPages.add(pageHeader);
			}
		}
		// if the list got intialised (i.e. there are some to clear)
		if (clearPages != null) {
			// loop the ids
			for (PageHeader pageHeader : clearPages) {
				// remove from the collection
				_pages.remove(pageHeader.getId());
				// log that we did so
				_logger.debug("Page " + pageHeader.getName() + " removed as not accessed since " + pageHeader.getLastGetDateTime());
			}
		}
	}
			
}
