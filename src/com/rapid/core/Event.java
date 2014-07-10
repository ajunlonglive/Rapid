/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

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
	
	// the type is defined in the control.xml file
	public String getType() { return _type; }
	public void setType(String type) { _type = type; }
	
	// the filter is a javascript function defined in the control.xml file that executes before the actions and can modify the event or even stop it from firing
	public String getFilter() { return _filter; }
	public void setFilter(String filter) { _filter = filter; }
	
	// these are the actions to perform when this event occurs to the control in which its sitting
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
