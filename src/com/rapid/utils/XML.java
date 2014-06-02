package com.rapid.utils;

public class XML {
	
	public static String getElementValue(String xml, String elementName) {
		
		String value = null;
		
		int startPos = xml.indexOf("<" + elementName + ">");
	    
	    if (startPos > 0) {
	    	
	    	int endPos = xml.indexOf("</" + elementName + ">");
	    
	    	value = xml.substring(startPos + elementName.length() + 2, endPos);
	    	
	    }
		
		return value;
		
	}
	
	public static String escape(String value) {
		
		return value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;").replace("'", "&apos;");
		
	}
	
	public static String unescape(String value) {
		
		return value.replace("&lt;","<").replace("&gt;",">").replace("&amp;","&").replace("&apos;","'");
		
	}

}
