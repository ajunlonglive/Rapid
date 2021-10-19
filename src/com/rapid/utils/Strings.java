/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.codec.binary.Hex;

public class Strings {

	// reads a string from a buffered reader
	public static String getString(BufferedReader reader) throws IOException {

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

	// reads a UTF-8 string from an input stream
	public static String getString(InputStream is) throws IOException {

		BufferedReader reader = new BufferedReader( new InputStreamReader( is, "UTF-8"));
		return getString(reader);

	}

	// uses method above to read a UTF-8 string from a file
	public static String getString(File file) throws IOException {

		return getString( new FileInputStream(file));

	}

	// save UTF-8 string to a file
	public static void saveString(String text, File file) throws IOException {

		Writer out = new BufferedWriter(new OutputStreamWriter(	new FileOutputStream(file), "UTF-8"));
		try {
			out.write(text);
		} finally {
			out.close();
		}

	}

	// a fast way to count the number of occurrences of a pattern within a string
	public static int occurrences(String string, String pattern) {
		// assume no occurrences
		int count = 0;
		// if both the string and pattern are non null
		if (string != null && pattern != null) {
			// get the length
			int length = string.length();
			// get the replaced string length
			int replacedLength = string.replace(pattern, "").length();
			// if any replacement happened
			if (length > replacedLength) {
				// get the pattern length
				int patternLength = pattern.length();
				// replace pattern with nothing and calc difference in length
				count = (length - replacedLength) / patternLength;
			}
		}
		// return
		return count;
	}
	
	public static String toRGB(int number) {
		int hash = hash32(number);
		// int to string
		String c = Integer.toString(hash & 0x00FFFFFF, 16).toUpperCase();
		return c.length() < 6 ?
			toRGB(hash) :
			"#" + c.substring(c.length() - 6, c.length());
	}
	
	public static String toSaturatedRGB(int number) {
		
		int hashR = hash32(number);
		int hashG = hash32(hashR);
		int hashB = hash32(hashG);
		
		// clamp values to range 0 to MAX_VALUE
		hashR = Math.abs(hashR);
		hashG = Math.abs(hashG);
		hashB = Math.abs(hashB);
		
		int min = Math.min(Math.min(hashR, hashG), hashB);
		int max = Math.max(Math.max(hashR, hashG), hashB);
		int range = max - min;
		double scale = new Double(Integer.MAX_VALUE / 2) / new Double(range);
		
		// translate so min is 0
		hashR = hashR - min;
		hashG = hashG - min;
		hashB = hashB - min;
		
		// scale so max is MAX_VALUE
		double DhashR = new Double(hashR) * scale;
		double DhashG = new Double(hashG) * scale;
		double DhashB = new Double(hashB) * scale;
		
		byte BhashR = (byte) DhashR;
		byte BhashG = (byte) DhashG;
		byte BhashB = (byte) DhashB;
		
		String hexString = Hex.encodeHexString(new byte[] {BhashR, BhashG, BhashB});
		return "#" + hexString;
	}
	
	public static String toRGB(String string) {
		return toRGB(string.hashCode());
	}
	
	
	// murmurhash-java
	// Credit to Viliam Holub
	// https://github.com/tnm/murmurhash-java
	
	public static int hash32(final byte[] data, int length, int seed) {
		// 'm' and 'r' are mixing constants generated offline.
		// They're not really 'magic', they just happen to work well.
		final int m = 0x5bd1e995;
		final int r = 24;

		// Initialize the hash to a random value
		int h = seed^length;
		int length4 = length/4;

		for (int i=0; i<length4; i++) {
			final int i4 = i*4;
			int k = (data[i4+0]&0xff) +((data[i4+1]&0xff)<<8)
					+((data[i4+2]&0xff)<<16) +((data[i4+3]&0xff)<<24);
			k *= m;
			k ^= k >>> r;
			k *= m;
			h *= m;
			h ^= k;
		}
		
		// Handle the last few bytes of the input array
		switch (length%4) {
		case 3: h ^= (data[(length&~3) +2]&0xff) << 16;
		case 2: h ^= (data[(length&~3) +1]&0xff) << 8;
		case 1: h ^= (data[length&~3]&0xff);
				h *= m;
		}

		h ^= h >>> 13;
		h *= m;
		h ^= h >>> 15;

		return h;
	}
	
	/** 
	 * Generates 32 bit hash from byte array with default seed value.
	 * 
	 * @param data byte array to hash
	 * @param length length of the array to hash
	 * @return 32 bit hash of the given array
	 */
	public static int hash32(final byte[] data, int length) {
		return hash32(data, length, 0x9747b28c); 
	}

	/** 
	 * Generates 32 bit hash from a string.
	 * 
	 * @param text string to hash
	 * @return 32 bit hash of the given string
	 */
	public static int hash32(final String text) {
		final byte[] bytes = text.getBytes(); 
		return hash32(bytes, bytes.length);
	}
	
	// end of murmurhash-java
	
	public static int hash32(final Number number) {
		String text = number.toString();
		final byte[] bytes = text.getBytes(); 
		return hash32(bytes, bytes.length);
	}

}
