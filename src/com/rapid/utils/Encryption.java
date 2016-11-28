/*

Copyright (C) 2016 - Gareth Edwards / Rapid Information Systems

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

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.xml.bind.DatatypeConverter;

public class Encryption {
	
	public interface EncryptionProvider {
		
		public String encrypt(String value) throws GeneralSecurityException, IOException;
		public String decrypt(String value) throws GeneralSecurityException, IOException;
		
	}
	
	public static String base64Encode(byte[] bytes) {
        return DatatypeConverter.printBase64Binary(bytes);
    }
    
    public static byte[] base64Decode(String value) throws IOException {
        return DatatypeConverter.parseBase64Binary(value);
    }

}