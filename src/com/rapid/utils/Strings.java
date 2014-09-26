package com.rapid.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
		// prepare a scanner
        Scanner s = new java.util.Scanner(file);
        // read the response body to a string
        String response = s.useDelimiter("\\A").next();
        // close the scanner
        s.close();	            
        // return
        return response;
	}
	
}
