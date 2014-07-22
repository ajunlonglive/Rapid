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
