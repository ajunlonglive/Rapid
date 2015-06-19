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
