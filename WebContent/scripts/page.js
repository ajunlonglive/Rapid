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

// a fake root control for starting the object tree
var _page = {};
// this doesn't do anything here but is referred to in the designer code
var _nextId;

// this loads the control constructors into the page from a json string (used with envJS and Rhino when rebuilding the page server side)
function loadControls(controlsString) {
	
	var controls = JSON.parse(controlsString);
	
	var count = 0;
	
	for (var i in controls) {
		
		// get a reference to a single control
		var c = controls[i];
		// create a new control ControlClass object/function (this is a closure)
		var f = new ControlClass(c);        		        		     			
		// assign the control controlClass function function globally
		window["ControlClass_" + c.type] = f; 
		
		// inc counter
		count ++;
		
	}
	
	return count + " controls loaded into script engine";
	
}

// this loads the controls and rebuilds the page (used with envJS and Rhino when rebuilding the page server side)
function getHtmlBody(controlsString) {
			
	// give _page a child array
	_page.childControls = new Array();
	// make the root control's object the html body
	_page.object = $('body');
	// remove any existing markup
	_page.object.children().remove();
	
	// parse our json page string into the object
	var controls = JSON.parse(controlsString);
	
	// if we have controls
	if (controls) {
    	// loop the page childControls and create
    	for (var i = 0; i < controls.length; i++) {
    		// get an instance of the control properties (which is what we really need from the JSON)
    		var control = controls[i];
    		// create and add (without actions)
    		_page.childControls.push(loadControl(control, _page, false));
    	}
	}
	
	return _page.object.html();
	
}