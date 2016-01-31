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

public class Comparators {
	
	public static int AsciiCompare (String s1, String s2) {
		// easy comparisons first
		if (s1 == null && s2 == null) return 0;
		if (s1 == null) return -1;
		if (s2 == null) return 1;
		if (s1.length() == 0 && s2.length() == 0) return 0;
		if (s1.length() == 0) return -1;
		if (s2.length() == 0) return 1;
		if (s1.equals(s2)) return 0;					
		// get the smallest number of characters they both have
		int minLength = Math.min(s1.length(), s2.length());
		// loop characters and as soon as they're different return that
		for (int i = 0; i < minLength; i++) {
			if ( s1.charAt(i) != s2.charAt(i)) return s1.charAt(i) - s2.charAt(i);					
		}
		// all characters they might have in common have been compared, return the difference in length
		return s1.length() - s2.length();
	}
	
	public static int AsciiCompare (String s1, String s2, boolean caseSensitive) {
		// easy comparisons first
		if (s1 == null && s2 == null) return 0;
		if (s1 == null) return -1;
		if (s2 == null) return 1;
		if (s1.length() == 0 && s2.length() == 0) return 0;
		if (s1.length() == 0) return -1;
		if (s2.length() == 0) return 1;
		// put both strings in the same case if not caseSensitive 
		if (!caseSensitive) {
			s1 = s1.toLowerCase();
			s2 = s2.toLowerCase();
		}
		if (s1.equals(s2)) return 0;					
		// get the smallest number of characters they both have
		int minLength = Math.min(s1.length(), s2.length());
		// loop characters and as soon as they're different return that
		for (int i = 0; i < minLength; i++) {
			if ( s1.charAt(i) != s2.charAt(i)) return s1.charAt(i) - s2.charAt(i);					
		}
		// all characters they might have in common have been compared, return the difference in length
		return s1.length() - s2.length();
	}
	
}
