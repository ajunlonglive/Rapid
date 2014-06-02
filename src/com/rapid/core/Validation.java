package com.rapid.core;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Validation {

	// instance variables
	
	private String _type, _regEx, _message, _javaScript;
	private boolean _allowNulls;
	
	// properties
	
	public String getType() { return _type; }
	public void setType(String type) { _type = type; }
	
	public boolean getAllowNulls() { return _allowNulls; }
	public void setAllowNulls(boolean allowNulls) { _allowNulls = allowNulls; }
	
	public String getRegEx() { return _regEx; }
	public void setRegEx(String regEx) { _regEx = regEx; }
	
	public String getMessage() { return _message; }
	public void setMessage(String message) { _message = message; }
	
	public String getJavaScript() { return _javaScript; }
	public void setJavaScript(String javaScript) { _javaScript = javaScript; }
	
	
	// constructors
	
	public Validation() {};
	
	public Validation(String type, boolean allowNulls, String regEx, String message, String javaScript) {
		_type = type;
		_allowNulls = allowNulls;
		_regEx = regEx;
		_message = message;
		_javaScript = javaScript;
	}
	
}
