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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFile {
	
	public static class ZipSource {
		
		private File _file;
		private String _path;
		
		public ZipSource(File file) {
			_file = file;
			_path = "";
		}
		
		public ZipSource(File file, String path) {
			_file = file;
			_path = path;
		}
		
		public File getFile() { return _file; }
		public String getPath() { return _path; }
		
	}
	
	public static class ZipSources extends ArrayList<ZipSource> {

		// overrides
		
		@Override
		public boolean contains(Object o) {
			if (o.getClass() == ZipSource.class) {
				ZipSource zs = (ZipSource) o;
				return contains(zs);
			}
			return false;
		}

		@Override
		public boolean add(ZipSource zipSource) {
			if (contains(zipSource)) {
				return false;
			} else {
				return super.add(zipSource);
			}
		}

		@Override
		public void add(int index, ZipSource zipSource) {
			if (!contains(zipSource)) super.add(index, zipSource);
		}
		
		// instance methods

		public boolean contains(ZipSource zipSource) {
			for (ZipSource zs : this) {
				if (zs.getFile().isDirectory()) {
					if (zipSource.getPath().startsWith(zs.getPath())) {
						if (zipSource.getFile().isDirectory()) {
							if (zipSource.getFile().getName().startsWith(zs.getFile().getName())) {
								return true;
							}
						} else {
							if (zipSource.getFile().getParentFile().getName().startsWith(zs.getFile().getName())) {
								return true;
							}
						}
					}
				} else 	if (zipSource.getFile().getName().equals(zs.getFile().getName()) && zipSource.getPath().equals(zs.getPath())) return true;
			}
			return false;
		}
		
		public boolean add(File file) {
			ZipSource zipSource = new ZipSource(file);
			if (contains(zipSource)) {
				return false;
			} else {
				return add(zipSource);
			}
		}
		
		public boolean add(File file, String path) {
			ZipSource zipSource = new ZipSource(file, path);
			if (contains(zipSource)) {
				return false;
			} else {
				return add(zipSource);
			}
		}					
	}
	
	final int BUFFER = 1024;
	
	File _file;
	
	public ZipFile(File file) throws FileNotFoundException {
		_file = file;			
	}
	
	private String buildPath(String path, String file) {
        if (path == null || path.isEmpty()) {
            return file;
        } else {
            return path + "/" + file;
        }
    }

    private void zipDir(ZipOutputStream zos, String path, File dir) throws IOException {

        File[] files = dir.listFiles();
        path = buildPath(path, dir.getName());

        for (File source : files) {
            if (source.isDirectory()) {
                zipDir(zos, path, source);
            } else {
                zipFile(zos, path, source);
            }
        }
    }

    private void zipFile(ZipOutputStream zos, String path, File file) throws IOException {
    	
        zos.putNextEntry(new ZipEntry(buildPath(path, file.getName())));

        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[BUFFER];
        int byteCount = 0;
        while ((byteCount = fis.read(buffer)) != -1) {
            zos.write(buffer, 0, byteCount);
        }
        
        fis.close();
        zos.closeEntry();
    }
	
    public void zipFiles(ZipSources sources, List<String> ignoreFiles) throws IOException {
        
    	ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(_file));
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);

        for (ZipSource source : sources) {
        	
        	File sourceFile = source.getFile();
        	
        	boolean ignore = false;
        	
        	if (ignoreFiles != null) {
        		for (String ignoreFile : ignoreFiles) {
        			if (sourceFile.getName().equals(ignoreFile)) {
        				ignore = true;
        				break;
        			}
        		}
        	}
        	
        	if (!ignore) {	        	
	            if (sourceFile.isDirectory()) {
	                zipDir(zipOut, source.getPath(), sourceFile);
	            } else {
	                zipFile(zipOut, source.getPath(), sourceFile);
	            }
        	}
        }
        
        zipOut.flush();
        zipOut.close();
        
    }	
    
    public void zipFiles(ZipSources sources) throws IOException {
        
    	zipFiles(sources, null);
    	
    }	
    
    public void unZip(File dir) throws IOException  {
    	
    	// create directory if not there
    	if (!dir.exists()) dir.mkdirs();
    	
    	FileInputStream fileInputStream = new FileInputStream(_file.getPath());
		ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(fileInputStream));
		
		int count;
    	byte data[] = new byte[BUFFER];
    	
		// a zip entry for looping
		ZipEntry zipEntry;
		// loop app entries in the file
		while ((zipEntry = zipInputStream.getNextEntry()) != null) {
									
			// get the name of this fle
			String fileName = dir.getAbsolutePath() + "/" + zipEntry.getName();
			// get a file object for it
			File file = new File(fileName);
			
			// check if directory
			
			if (zipEntry.isDirectory()) {
				
				// create directory if not there
				if (!file.exists()) file.mkdirs();
				
			} else {
				
				// get intended directory into file
				File destinationFolder = new File (file.getPath().substring(0,Math.max(file.getPath().lastIndexOf("/"),file.getPath().lastIndexOf("\\"))));
				// create directory if not there
				if (!destinationFolder.exists()) destinationFolder.mkdirs();
				
				// write file
				FileOutputStream fos = new FileOutputStream(fileName);
		    	BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER);
		    			    			    	
		        while ((count = zipInputStream.read(data, 0, BUFFER)) != -1) {
		        	bos.write(data, 0, count);	
		        }
		        
		        bos.flush();
		        bos.close();	
		        fos.close();
				
			}
			
			
		}
			        
        zipInputStream.close();
		fileInputStream.close();
    	
    	
    }
    	    
	public void unZip() throws IOException {
					
		// get intended directory from file
		String rootFolderName = _file.getAbsolutePath();
		// remove .zip from end
		if (rootFolderName.toLowerCase().lastIndexOf(".zip") == rootFolderName.length() - 4) rootFolderName = rootFolderName.substring(0,rootFolderName.length() - 4 );  
		// create a file for the folder
		File rootFolder = new File(rootFolderName);
		// delete the folder if it exists
		if (rootFolder.exists()) Files.deleteRecurring(rootFolder);
		// unzip to this root folder
		unZip(rootFolder);
		                	        
	}
	
}
