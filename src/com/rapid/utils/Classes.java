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

public class Classes {
	
	// whether a given class extends another (super) class
	public static boolean extendsClass(Class classClass, Class superClass) {
		
		// assume not
		boolean extendsClass = false;
		
		// get the class super class
		Class classSuperClass = classClass.getSuperclass();
		
		// if the given super class is the same as the classes super class we're good
		if (superClass.equals(classSuperClass)) {
			
			return true;
			
		} else {
			
			// if there are super classes to check go interative!
			if (classSuperClass != null) extendsClass = extendsClass(classSuperClass, superClass);
			
		}
		
		// return whatever came back!
		return extendsClass;
		
	}

}
