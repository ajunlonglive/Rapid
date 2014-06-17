package com.rapid.utils;

public class Comparators {
	
	public static int Ascii (String s1, String s2) {
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

}
