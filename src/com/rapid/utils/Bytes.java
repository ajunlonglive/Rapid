package com.rapid.utils;

public class Bytes {
	
	public static int findPattern(byte[] bytes, byte[] pattern, int position) {
		
		int i,j;
		
		// do we have bytes in our arrays and is the position low enough to find something
		if (bytes.length > 0 && pattern.length > 0 && position >=0 && position <= bytes.length - pattern.length) {
			
			// loop from start position
			for (i = position; i < bytes.length; i++) {
				
				// if we matched on the first byte of the pattern and have enough left to find the whole thing
				if (bytes[i] == pattern[0] && i + pattern.length < bytes.length) {
					
					// check remaining bytes in pattern
					for (j = 1; j < pattern.length; j++) {
						
						// bail early if we fail to match the pattern
						if (bytes[i + j] != pattern[j]) break;
																		
					}
					
					// if we got to the end of the pattern we're there!
					if (j == pattern.length) return i;
					
				}
				
			}
			
		}
						
		return -1;
				
	}
	
	public static int findPattern(byte[] bytes, byte[] pattern) {
		return findPattern(bytes, pattern, 0);
	}
	

}
