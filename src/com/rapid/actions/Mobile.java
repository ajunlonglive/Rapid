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
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

public class Mobile extends Action {
		
	// constructors
	
	// used by jaxb
	public Mobile() { 
		super(); 
	}
	// used by designer
	public Mobile(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);
	}
		
	// overridden methods
			
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		// prepare the js
		String js = "if (typeof _rapidmobile == 'undefined') {\n  alert('This action is only available in Rapid Mobile'); \n} else {\n";
		// get the type
		String type = getProperty("actionType");
		// check we got something
		if (type != null) {
			// check the type
			if ("addImage".equals(type)) {
				// gett he gallery control Id
				String galleryControlId = getProperty("galleryControlId");
				// get the gallery control
				Control galleryControl = page.getControl(galleryControlId);
				// check if we got one
				if (galleryControl == null) {
					js += "  //galleryControl " + galleryControlId + " not found\n";
				} else {
					js += "  _rapidmobile.addImage('" + galleryControlId + "');\n";
				}
			}
		}
		// close checkRapidMobile
		js += "}\n";
		// return an empty string
		return js;
	}
	
}
