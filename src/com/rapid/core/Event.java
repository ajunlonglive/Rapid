package com.rapid.core;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(namespace="http://rapid-is.co.uk/core")
public class Event {

	// instance variables
			
	private String _type, _filter;
	private ArrayList<Action> _actions;
	
	// properties
	
	public String getType() { return _type; }
	public void setType(String type) { _type = type; }
	
	public String getFilter() { return _filter; }
	public void setFilter(String filter) { _filter = filter; }
	
	public ArrayList<Action> getActions() { return _actions; }
	public void setActions(ArrayList<Action> actions) { _actions = actions; }
	
	// constructors
	
	public Event() {};
	
	public Event(String type, String filter) {
		_type = type;
		_filter = filter;
		_actions = new ArrayList<Action>();
	}
	
}
