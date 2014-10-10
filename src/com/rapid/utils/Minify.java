package com.rapid.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public class Minify {
	
	// public static finals
	
	public static final int JAVASCRIPT = 0;
	public static final int CSS = 1;
	
	public static class MinifyIssue {
		
		// private instance variables
		private String _message, _sourceName, _lineSource;
		int _line, _column;
		
		// constructor
		public MinifyIssue(String message, String sourceName, int line, String lineSource, int column) {
			_message = message;
			_sourceName = sourceName;
			_line = line;
			_lineSource = lineSource;
			_column = column;
		}
		
		// override
		@Override
		public String toString() {
			return _message + " at line " + _line + ", column " + _column + " : " + _lineSource;
		}
		
	}
	
	public static class MinifyErrorReporter implements ErrorReporter {
		
		// private instance variables
		private List<MinifyIssue> _errors;
		private List<MinifyIssue> _warnings;
		
		// constructor
		public MinifyErrorReporter() {
			_errors = new ArrayList<MinifyIssue>();
			_warnings = new ArrayList<MinifyIssue>();
		}
		
		// properties
		public List<MinifyIssue> getErrors() { return _errors; }
		public List<MinifyIssue> getWarnings() { return _warnings; }
		
		// overrides

		@Override
		public void error(String message, String sourceName, int line, String lineSource, int column) {
			_errors.add(new MinifyIssue(message, sourceName, line, lineSource, column));
			
		}

		@Override
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int column) {
			_errors.add(new MinifyIssue(message, sourceName, line, lineSource, column));
			return new EvaluatorException(message, sourceName, line, lineSource, column);
		}

		@Override
		public void warning(String message, String sourceName, int line, String lineSource, int column) {
			_warnings.add(new MinifyIssue(message, sourceName, line, lineSource, column));			
		}
		
		public String getDetails() {
			
			StringBuilder detail = new StringBuilder();
			
			// if we have any errors
			if (_errors.size() > 0) {				
				// append to the String builder
				detail.append("/* This file could not be minified due to  the following errors */\n\n/*\n");
				// loop the errors
				for (MinifyIssue error : _errors) {
					detail.append(error.toString() + "\n\n");
				}
				// close the comments
				detail.append("*/\n\n");				
			}
			
			// if we have any warnings
			if (_warnings.size() > 0) {				
				// append to the String builder
				detail.append("/* This file contained the following warnings */\n\n/*\n");
				// loop the errors
				for (MinifyIssue warning : _warnings) {
					detail.append(warning.toString() + "\n\n");
				}
				// close the comments
				detail.append("*/\n\n");				
			}
						
			// return the detail
			return detail.toString();
			
		}
				
	}
	
	// the main method
	public static Writer toWriter(String string, Writer writer, int type) throws IOException {
		
		// get an error reporter
		MinifyErrorReporter errorReporter = new MinifyErrorReporter();
		
		// get a string reader
		StringReader sr = new StringReader(string);
		
		try {									
			// check the type
			switch (type) {
				case CSS :
					// get the css compressor
					CssCompressor cssCompressor = new CssCompressor(sr);
					// compress
					cssCompressor.compress(writer, 0);
				break;
				default :
					// the error reporter is only used with js
					JavaScriptCompressor jsCompressor = new JavaScriptCompressor(sr, errorReporter);
					// compress
					jsCompressor.compress(writer, 1000, false, false, false, false);
					// check for any errors
					if (errorReporter.getErrors().size() > 0) {
						// write the original string
						writer.write(string);
						// add the details
						writer.write("\n\n" + errorReporter.getDetails());
						// return the original string and the details
						return writer;
					}
					// check for any warnings
					if (errorReporter.getWarnings().size() > 0) {
						// add the details
						writer.write("\n\n" + errorReporter.getDetails());
					}
			}												
			// return the writer
			return writer;
			
		} catch (EvaluatorException ex) {
			
			// write the original string
			writer.write(string);
			
			// return the original string and the error details
			writer.write("\n\n" + errorReporter.getDetails());
			
			// return
			return writer;
			
		}
					
	}
	
	public static String toString(String string, int type) throws IOException {
		// get the string writer
		StringWriter sw = (StringWriter) toWriter(string, new StringWriter(), type);
		// turn to string
		return sw.toString();
	}
	
	public static void toFile(File fromFile, File toFile, int type) throws IOException {		
		// read the string from file
		String string = Strings.getString(fromFile);		
		// create a file writer
		FileWriter fw = (FileWriter) toWriter(string, new FileWriter(toFile), type);		
		// close file writer
		fw.close();		
	}
	
	public static void toFile(String string, File file, int type) throws IOException {		
		// create a file writer
		FileWriter fw = new FileWriter(file);
		// compress
		fw.write(toString(string, type));
		// close file writer
		fw.close();		
	}
	
	public static void toFile(String string, String file, int type) throws IOException {		
		// get a file
		File fileObject = new File(file);
		// run it
		toFile(string, fileObject, type);		
	}
	
}
