package com.rapid.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Files {

	// deletes the given file object, if it is a directory recursively deletes its contents
	public static boolean deleteRecurring(File file) {
		// check file is directory
		if (file.isDirectory()) {
			// get a list of contents
			File[] files = file.listFiles();
			// loop contents recursively calling itself to delete those contents
			for (int i = 0; i < files.length; i ++) {
				deleteRecurring(files[i]);
			}			
		} 
		// if we're here we've arrived at a physical file, return its delete	
		return file.delete();					
	}
	
	// byte copies one file to another
	public static void copyFile(File src, File dest) throws IOException {
		
		FileInputStream fis = new FileInputStream(src.getPath());
		FileOutputStream fos = new FileOutputStream(dest.getPath());
			 				
		int size = 1024;
	    byte data[] = new byte[size];
	    int count;
	    
	    BufferedOutputStream bos = new BufferedOutputStream(fos, size);
	    while ((count = fis.read(data, 0, size)) != -1) {
	       bos.write(data, 0, count);
	    }
	    
	    bos.flush();
	    bos.close();
	    fos.close();
	    fis.close();
		
	}
	
	// copies the contents of one folder to another recursively, allowing for a list of files/folder to ignore by name
	public static void copyFolder(File src, File dest, List<String> ignoreFiles) throws IOException {
		
		// whether to ignore
		boolean ignore = false;
		
		// check if we have some ignore files
		if (ignoreFiles != null) {
			// loop them and compare
			if (ignoreFiles != null) {
        		for (String ignoreFile : ignoreFiles) {
        			if (src.getName().equals(ignoreFile)) {
        				ignore = true;
        				break;
        			}
        		}
        	}
		}
		
		// not ignoring so proceed
		if (!ignore) {
		
			// if source is directory
	    	if (src.isDirectory()){ 
	    		// if directory not exists, create it
	    		if (!dest.exists()) dest.mkdirs();
	    		// list all the directory contents
	    		String files[] = src.list();
	    		// loop directory contents
	    		for (String file : files) {
	    		   // create a file object for the source
	    		   File srcFile = new File(src, file);
	    		   // create a file object for the destination, note the dest folder is the parent
	    		   File destFile = new File(dest, file);
	    		   // recursive copy
	    		   copyFolder(srcFile, destFile, ignoreFiles);
	    		}
	    	} else {
	    		// not a directory so only copy the file to the destination
	    		copyFile(src, dest);    		
	    	}
	    	
		}
    	
    }
	
	// an override to the above without the list of folder/file ignore names
	public static void copyFolder(File src, File dest) throws IOException {		
		copyFolder(src, dest, null);		
	}
	
	public static String safeName(String name) {
		
		// start with an empty string
		String safeName = "";
		// loop all the characters in the input and add back just those that are "safe"
		for (int i = 0; i < name.length(); i ++) {
			// get the char at the position
			char c = name.charAt(i);
			// append to return if a safe character (0-9, A-Z, a-z, -, _)
			if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c == 45 || c == 95 ) {
				safeName += c;
			}
		}
		// send back the string we just made of the safe characters
		return safeName;
		
	}
	
	// this function is called recurringly in the tree walk
	public static long getSize(File file) {
		// instantiate the return value
		long size = 0;		
		// if the file is a directory
		if (file.isDirectory()) {				
			// loop the contents constantly incrimenting the size
			for (File childFile : file.listFiles()) {
				size += getSize(childFile);			
			}			
		} else {			
			// just return the size
			return file.length();			
		}	
		// return the size of this branch
		return size;
	}
	
}
