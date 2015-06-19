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

package com.rapid.actions;

import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

public class Event extends Action {
		
	// constructors
	
	// used by jaxb
	public Event() { 
		super(); 
	}
	// used by designer
	public Event(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);;
	}
		
	// overridden methods
			
	@Override
	public String getJavaScript(RapidHttpServlet rapidServlet, Application application, Page page, Control control, JSONObject jsonDetails) {
		// get the event id
		String eventId = getProperty("event");
		// check we got something
		if (eventId != null) {
			// get the parts (not the need to escape the . in regex)
			String eventParts[] = eventId.split("\\.");
			// check we have the right amount
			if (eventParts.length == 2) {
				// the page is a bit different
				if ("page".equals(eventParts[0])) {
					return "Event_" + eventParts[1] + "_" + page.getId() + "(ev);";
				} else {
					return "Event_" + eventParts[1] + "_" + eventParts[0] + "(ev);";
				}
			}			
		}
		// return an empty string
		return "";
	}
	
}
