/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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

package com.rapid.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Scanner;

public class Strings {

	public static String getString(InputStream is) throws IOException {
		// prepare a scanner
        Scanner s = new java.util.Scanner(is);
        // read the response body to a string
        String response = s.useDelimiter("\\A").next();
        // close the scanner
        s.close();	            
        // return
        return response;
	}
	
	public static String getString(File file) throws IOException {
		
		BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream(file), "UTF-8"));
	    String line = null;
	    StringBuilder stringBuilder = new StringBuilder();
	    String ls = System.getProperty("line.separator");

	    try {
	        while( ( line = reader.readLine() ) != null ) {
	            stringBuilder.append( line );
	            stringBuilder.append( ls );
	        }
	        return stringBuilder.toString();	        
	    } finally {
	        reader.close();
	    }
		
	}
	
	public static void saveString(String text, File file) throws IOException {
		
		Writer out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(file), "UTF-8"));
		try {
		    out.write(text);
		} finally {
		    out.close();
		}
		
	}
	
	
	public static int occurrences(String string, String pattern) {
		// assume no occurences
		int count = 0;
		// if both the string and pattern are non null
		if (string != null && pattern != null) {
			// replace pattern with nothing and calc difference in length
			count = string.length() - string.replace(pattern, "").length() / pattern.length();
		}
		// return
		return count;		
	}
	
}
