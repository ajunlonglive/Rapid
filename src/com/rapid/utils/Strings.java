/*

Copyright (C) 2022 - Gareth Edwards / Rapid Information Systems

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

		final int hashR = hash32(number + 1);
		final int hashG = hash32(hashR);
		final int hashB = hash32(hashG);

		// clamp values to range 0 to MAX_VALUE
		final int r = Math.abs(hashR);
		final int g = Math.abs(hashG);
		final int b = Math.abs(hashB);

		final int min = Math.min(Math.min(r, g), b);
		final int max = Math.max(Math.max(r, g), b);
		final int range = max - min;

		final byte red = (byte) (r == min ? 0 : r == max ? -16 : (new Double(Byte.MAX_VALUE - 16) / (new Double(r) / range)));
		final byte green = (byte) (g == min ? 0 : g == max ? -16 : (new Double(Byte.MAX_VALUE - 16) / (new Double(g) / range)));
		final byte blue = (byte) (b == min ? 0 : b == max ? -16 : (new Double(Byte.MAX_VALUE - 16) / (new Double(b) / range)));

		final String hexString = encodeHexString(new byte[] {red, green, blue});
		return "#" + hexString;
	}

	public static String toRGB(String string) {
		return toRGB(string.hashCode());
	}

	public static String ordinalIndicator(int n) {
		int units = n % 10;
		if (10 <= n && n <= 19)
			return "th";
		else if(units == 1)
			return "st";
		else if(units == 2)
			return "nd";
		else if(units == 3)
			return "rd";
		else
			return "th";
	}

	public static String toOrdinal(int n) {
		return Integer.toString(n) + ordinalIndicator(n);
	}

	public static String deleteSubstring(String text, String textToDelete) {

		// check the location of one string in another
		int location = text.indexOf(textToDelete);

		// if it contains the string then remove it
		if(location>=0) {
			text = text.substring(0, location) + text.substring(location + textToDelete.length());
		}

		return text;
	}

	public static int editPercentage(String word1, String word2) {

		// get the longest of the two words
		float maxWordLength = (word1.length()>word2.length())?word1.length():word2.length();

		// get the edit distance of the two words
		float editDistance = editDistance(word1, word2);

		// now calculate the percentage difference
		float percentageDifference = (100f/maxWordLength) * editDistance;

		// return it
		return (int)percentageDifference;
	}

	public static int editDistance(String word1, String word2) {
		int len1 = word1.length();
		int len2 = word2.length();

		// len1+1, len2+1, because finally return dp[len1][len2]
		int[][] dp = new int[len1 + 1][len2 + 1];

		for (int i = 0; i <= len1; i++) {
			dp[i][0] = i;
		}

		for (int j = 0; j <= len2; j++) {
			dp[0][j] = j;
		}

		//iterate though, and check last char
		for (int i = 0; i < len1; i++) {
			char c1 = word1.charAt(i);
			for (int j = 0; j < len2; j++) {
				char c2 = word2.charAt(j);

				//if last two chars equal
				if (c1 == c2) {
					//update dp value for +1 length
					dp[i + 1][j + 1] = dp[i][j];
				} else {
					int replace = dp[i][j] + 1;
					int insert = dp[i][j + 1] + 1;
					int delete = dp[i + 1][j] + 1;

					int min = replace > insert ? insert : replace;
					min = delete > min ? min : delete;
					dp[i + 1][j + 1] = min;
				}
			}
		}

		return dp[len1][len2];
	}

	// Apache commons
	// https://github.com/apache/commons-codec/blob/master/src/main/java/org/apache/commons/codec/binary/Hex.java

	/**
     * Used to build output as hex.
     */
    private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Used to build output as hex.
     */
    private static final char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	 /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to hex characters
     * @return A char[] containing lower-case hexadecimal characters
     */
    public static char[] encodeHex(final byte[] data) {
        return encodeHex(data, true);
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data        a byte[] to convert to Hex characters
     * @param toLowerCase {@code true} converts to lowercase, {@code false} to uppercase
     * @return A char[] containing hexadecimal characters in the selected case
     * @since 1.4
     */
    public static char[] encodeHex(final byte[] data, final boolean toLowerCase) {
        return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data     a byte[] to convert to hex characters
     * @param toDigits the output alphabet (must contain at least 16 chars)
     * @return A char[] containing the appropriate characters from the alphabet For best results, this should be either
     *         upper- or lower-case hex.
     * @since 1.4
     */
    protected static char[] encodeHex(final byte[] data, final char[] toDigits) {
        final int dataLength = data.length;
        final char[] out = new char[dataLength << 1];
        encodeHex(data, 0, dataLength, toDigits, out, 0);
        return out;
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     *
     * @param data a byte[] to convert to hex characters
     * @param dataOffset the position in {@code data} to start encoding from
     * @param dataLen the number of bytes from {@code dataOffset} to encode
     * @param toDigits the output alphabet (must contain at least 16 chars)
     * @param out a char[] which will hold the resultant appropriate characters from the alphabet.
     * @param outOffset the position within {@code out} at which to start writing the encoded characters.
     */
    private static void encodeHex(final byte[] data, final int dataOffset, final int dataLen, final char[] toDigits, final char[] out, final int outOffset) { //
        // two characters form the hex value.
        for (int i = dataOffset, j = outOffset; i < dataOffset + dataLen; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
    }

    /**
     * Converts an array of bytes into a String representing the hexadecimal values of each byte in order. The returned
     * String will be double the length of the passed array, as it takes two characters to represent any given byte.
     *
     * @param data a byte[] to convert to hex characters
     * @return A String containing lower-case hexadecimal characters
     * @since 1.4
     */
    public static String encodeHexString(final byte[] data) {
        return new String(encodeHex(data));
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
