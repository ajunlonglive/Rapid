/*
Copyright (C) 2016 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk

This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version. The terms require you
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.actions;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Comparators;

public class ListFolder extends Action {
   
    // static variables
    private static Logger _logger = Logger.getLogger(ListFolder.class);
           
    // constructors
    
   
    // used by jaxb
    public ListFolder() { super(); }
    // used by designer
    public ListFolder(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception {
        super(rapidServlet, jsonAction);
    }
   
    // overrides
       
    @Override
    public String getJavaScript(RapidRequest rapidRequest, Application application, Page page, Control control, JSONObject jsonDetails) throws Exception {
        
    	// get the folder
    	String rootFolder = getProperty("rootFolder");
    	
    	// start with empty JavaScript
        String js = "";
        
        // check we have a root folder
    	if (rootFolder == null) {
    		// write something helpful into the js
    		js += "// The root folder you have entered is null\n";
    		
    	} else {
    		
    		if (rootFolder.trim().length() == 0) {
    			// message to say whitespace has been entered
    			js += "// The root folder you have entered cannot be blank\n";
    			
    		} else {
    		
	    		// control id
	        	String destinationId = getProperty("dataDestination");
	        	// destination control
	        	Control destinationControl = page.getControl(destinationId);
	        	
	        	if (destinationControl == null) {
	        		// message to say the destination control is null
	        		js += "// The destination control you have specified cannot be found\n";
	        		
	        	} else {
	                       
			        // control can be null when the action is called from the page load
			        String controlParam = "";
			        if (control != null) controlParam = "&c=" + control.getId();
			        
			        // assume we did click on a folder
			        js += "var folder = true;\n";
			        
			        // assume no path
			        String path = "";
			        // if this is being called from the destination control
			        if (control.getId().equals(destinationId)) {
			        	// get the path
			        	path = "path: \"' + getData_" + destinationControl.getType() + "(ev, '" + destinationId + "', 'path', " + destinationControl.getDetails() + ") + '\"";
			        	// get whether it's really a folder
			        	js += "folder = getData_" + destinationControl.getType() + "(ev, '" + destinationId + "', 'folder', " + destinationControl.getDetails() + ");\n";
			        }
			        
			        // open if folder is true
			        js += "if (folder != 'false') {\n";

			        // open the ajax call
			        js += "  $.ajax({ url : '~?a=" + application.getId() + "&v=" + application.getVersion() + "&p=" + page.getId() + controlParam + "&act=" + getId() + "', type: 'POST', contentType: 'application/json', dataType: 'json',\n";
			        js += "    data: '{" + path + "}',\n";
			        js += "    error: function(server, status, fields, rows) {\n";
			        // this avoids doing the errors if the page is unloading or the back button was pressed
			        js += "      if (server.readyState > 0) {\n";
			        // show the server exception message
			        js += "        alert('Error with list folder action : ' + server.responseText||message);";	
			        // close unloading check
			        js += "      }\n";
			       
			        // close error actions
			        js += "    },\n";
			       
			        // open success function
			        js += "    success: function(data) {\n";
			        // open if data check
			        js += "      if (data) {\n";
			        
			        // Code that breaks the functions completely
			        if (rootFolder != null) js += "        setData_" + destinationControl.getType() + "(ev, '" + destinationId + "',null, " + destinationControl.getDetails() + ", data);\n";
			        
			        // close if data check
			        js += "      }\n";
			        
			        // close success function
			        js += "    }\n";
			       
			        // close ajax call
			        js += "  });\n";
			        
			        // close if folder is true
			        js += "}\n";
				        
	        	} // destination control null check
    		} // root folder whitespace check
    	} // root folder null check
                   
        // return what we built
        return js;
    }
   
    @Override
    public JSONObject doAction(RapidRequest rapidRequest, JSONObject jsonAction) throws Exception {
    	
    	// get the root
    	String rootFolder = getProperty("rootFolder");
    	// get showHidden output
    	boolean showHidden = Boolean.parseBoolean(getProperty("showHidden"));
    	
    	// get any child path
    	String path = jsonAction.optString("path", "");
    	
    	// get the full path
    	String fullPath = rootFolder + path;
    	
    	// log
    	_logger.debug("List folder action path = " + fullPath);
		
    	// get a file object for the full path
		File files = new File(fullPath);
		
		// if it is not a folder then use it's parent
		if (!files.isDirectory()) files = new File(files.getParent());
			
		// get the files within
		File[] directoryListing = files.listFiles();
		
		// throw meaningful exception if directoryListing is null
        if (directoryListing == null) {
        	throw new Exception ("The root folder cannot be read as it is null");
        }
        
        // Sorting the array of files
        Arrays.sort(directoryListing, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				// do a case insensitive compare on file names
				return Comparators.AsciiCompare(f1.getName(), f2.getName(), false);
			}
        });
        
        JSONArray jsonFields = new JSONArray();
        jsonFields.put("name");
        jsonFields.put("size");
        jsonFields.put("path");
        jsonFields.put("type");
        jsonFields.put("modified");
        jsonFields.put("folder");
        if (showHidden) jsonFields.put("hidden");
        
        JSONArray jsonItems = new JSONArray();
        
        // if we are in a sub-folder
        if (files.getPath().length() > rootFolder.length()) {
        	
	        JSONArray jsonUpItem = new JSONArray();
			
	        jsonUpItem.put("..");
	        jsonUpItem.put("0");
	        jsonUpItem.put(files.getParent().substring(rootFolder.length()));
	        jsonUpItem.put("");
	        jsonUpItem.put("");
	        jsonUpItem.put(true);
	    	if (showHidden) jsonUpItem.put(false);
	    	
	    	jsonItems.put(jsonUpItem);
	    	
        }
        
        // Loop folder contents
        for (File child : directoryListing) {
        	
        	if (!child.getPath().contains("..") && (showHidden || !child.isHidden())) {
        		
        		Date d = new Date(child.lastModified());
            	
            	JSONArray jsonItem = new JSONArray();
            	
            	// get the file name
            	String fileName = child.getName();
            	// assume no type
                String type = "";
            	// get the extension type
                int dotPos = fileName.lastIndexOf('.');
                // if there was a dot get the extension
                if (dotPos > -1) type = fileName.substring(dotPos +1);
        		
	        	jsonItem.put(fileName);
	        	jsonItem.put(com.rapid.utils.Files.getSizeName(child));
	        	jsonItem.put(child.getPath().substring(rootFolder.length()));
	        	jsonItem.put(type);
	        	jsonItem.put(d);
	        	jsonItem.put(Boolean.toString(child.isDirectory()));
	        	if (showHidden) jsonItem.put(Boolean.toString(child.isHidden()));
	        	
	        	jsonItems.put(jsonItem);
        	}
        	
        }
        
        // the object we're going to return
        JSONObject jsonData = new JSONObject();
        
        jsonData.put("fields", jsonFields);
        jsonData.put("rows", jsonItems);
       
        // return the jsonData
        return jsonData;
       
    }
}