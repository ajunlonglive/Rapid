package com.rapid.core;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Style {

	// instance variables
	
	private String _appliesTo;
	private ArrayList<String> _rules;
	
	// properties
	
	public String getAppliesTo() { return _appliesTo; }
	public void setAppliesTo(String appliesTo) { _appliesTo = appliesTo; }
	
	public ArrayList<String> getRules() { return _rules; }
	public void setRules(ArrayList<String> rules) { _rules = rules;	}
	
	// constructors
	
	public Style() {};
	
	public Style(String appliesTo) {
		_appliesTo = appliesTo;
		_rules = new ArrayList<String>();
	}
	
}
