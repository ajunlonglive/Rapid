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

package com.rapid.actions;

import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Event;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

public class Existing extends Action {
		
	// constructors
	
	// used by jaxb
	public Existing() { super(); }
	// used by designer
	public Existing(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);				
	}
	
	// methods
	
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		// get the action id
		String actionId = getProperty("action");
		// check we got something
		if (actionId == null) {
			return "";
		} else {
			// get the control for this action
			Control actionControl = page.getActionControl(actionId);			
			// if there is no control this is a page event
			if (actionControl == null) {
				// get the event
				Event existingEvent = page.getActionEvent(actionId);
				// if we got an existing event update the type name
				if (existingEvent == null) {
					return "";
				} else {
					// return
					return "Event_" + existingEvent.getType() + "_" + page.getId() + "(ev)";
				}								
			} else {				
				// get the action property
				Action existingAction = page.getAction(actionId);
				// check we got something
				if (existingAction == null) {
					return "";
				} else {
					return existingAction.getJavaScript(application, page, actionControl);
				}
			}
		}		
	}
	
}
