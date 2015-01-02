package com.rapid.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;

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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.rapid.core.Application.RapidLoadingException;

public class Pages extends HashMap<String,Page> {
	
	// instance variables
	
	private Application _application;
	private FilenameFilter _filenameFilter;
	private HashMap<String,String> _pageIdFiles;
	private File _pagesFolder;
	private DocumentBuilder _docBuilder;
	private XPathExpression _xpathExpression;
	private Page _startPage;
	
	// constructor
	
	public Pages(Application application) throws ParserConfigurationException, XPathExpressionException {
		// store the application
		_application = application;
		// create a filter for finding .page.xml files
		_filenameFilter = new FilenameFilter() {
	    	public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".page.xml");
	    	}
	    };
	    // creat the map for caching page files by id's
	    _pageIdFiles = new HashMap<String,String>();
	    // create objects for xml parsing
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    _docBuilder = factory.newDocumentBuilder();	    
	    XPathFactory xPathfactory = XPathFactory.newInstance();
	    XPath xpath = xPathfactory.newXPath();
	    // compile the xpath expression
	    _xpathExpression = xpath.compile("/page/id");
	    	    
	}
	
	// private methods
	
	private Page loadPage(ServletContext servletContext, File pageFile) throws JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
		
		// load the page from file
		Page page = Page.load(servletContext, pageFile);
	
		// add it to this collection
		super.put(page.getId(), page);
		
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
	
	private Page getPage(ServletContext servletContext, String pageId) throws RapidLoadingException {
		
		// placeholder for page
		Page page = null;
		
		// look for this file in the cache
		String pageIdFile = _pageIdFiles.get(pageId);
		
		// if we didn't get one from the cache
		if (pageIdFile == null) {
			
			// initiate pages folder if null
			if (_pagesFolder == null) _pagesFolder = new File(_application.getConfigFolder(servletContext) + "/pages");
			
			// if the folder is there
			if (_pagesFolder.exists()) {
				
			    // loop the .page.xml files and add to the application
			    for (File pageFile : _pagesFolder.listFiles(_filenameFilter)) {
			    	
			    	try {
			    		
			    		// if we only want the start page
			    		if (pageId == null) {
			    			
			    			// load the page from file
			    			page = loadPage(servletContext, pageFile);		    	
				    		
				    		// we're done
				    		break;
				    		
			    		} else {
			    			
			    			// parse the xml file
			    			Document doc = _docBuilder.parse(pageFile);
			    			
			    		    // get the page id from the file
			    		    String docPageId = _xpathExpression.evaluate(doc);
			    		    			    		    
			    		    // cache the page id against the file so we don't need to use the expensive parsing operation again
			    		    _pageIdFiles.put(docPageId, pageFile.toString());
			    		    
			    		    // if it matches the page id in the document
			    		    if (pageId.equals(docPageId)) {
			    		    	
			    		    	// load the page from file
				    			page = loadPage(servletContext, pageFile);	
					    		
					    		// we're done
					    		break;
			    		    }
			    			
			    		}
			    		
			    	} catch (Exception ex) {	

			    		// throw the exception with the page name
			    		throw new Application.RapidLoadingException("Error loading page " + getPageName(pageFile), ex);
			    		
					}
			    	
			    }
				
			}
			
		} else {
			
			// read the string into a file object
			File pageFile = new File(pageIdFile);
			
			// load the page from file
			try {
				
				// load the page
				page = loadPage(servletContext, pageFile);
				
			} catch (Exception ex) {
				
				// throw the exception with the page name
	    		throw new Application.RapidLoadingException("Error loading page " + getPageName(pageFile), ex);
	    		
			}	
			
		}
						
		// return the page
		return page;
		
	}
	
	// public methods

	public Page get(ServletContext servletContext, String pageId) throws RapidLoadingException {

		// look for the page in the table first
		Page page = super.get(pageId);
		// if we didn't find one
		if (page == null) {
			// if weren't given a page id
			if (pageId == null) {
				// get the start page
				page = getStartPage(servletContext);
			} else {
				// load it from file
				page = getPage(servletContext, pageId);
			}
		}
		// return it
		return page;
		
	}
	
	// loads the first page from file
	public Page getStartPage(ServletContext servletContext) throws RapidLoadingException {
		// load the first page from file if no start page
		if (_startPage == null) _startPage = getPage(servletContext, null);			
		// return it
		return _startPage;		
	}

	// overrides
	@Override
	public void clear() {
		// clear the pages
		super.clear();
		// clear the cached page file ids
		_pageIdFiles.clear();
		// clear the pages folder (just in case)
		_pagesFolder = null;
		// clear the start page
		_startPage = null;
		
	}
	
	
}
