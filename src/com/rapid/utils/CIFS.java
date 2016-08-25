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

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

public class CIFS {
	
	public static void saveFile(String user, String password, String path, byte[] bytes) throws IOException {
		
		String credentials = user + ":" + password;
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(credentials);
		SmbFile sFile = new SmbFile(path, auth);
		SmbFileOutputStream sfos = new SmbFileOutputStream(sFile);
		sfos.write(bytes);
		sfos.close();
				
	}
	
	public static void saveFile(String user, String password, String path, String contents) throws IOException {
		saveFile(user, password, path, contents.getBytes());
	}

}
