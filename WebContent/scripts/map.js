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

// a global for all map listeners so we can de-register
var _mapListeners = [];

// add a map listener
function addMapListener(l) {
	_mapListeners.push(l);
}

// remove all map listeners
function removeMapListeners() {
	// loop all listeners
	for (var l in _mapListeners) {
		_mapListeners[l].unbind();
	}
	// reset the collection
	_mapListeners = [];
}

function createMapEntry(list, c) {
	// get the control class
	var controlClass = _controlTypes[c.type];
	// create the list entry
	list.append("<li><span data-id='" + c.id + "'>" + ((controlClass.image) ? "<img src='" + controlClass.image + "'/>" : "") + c.type + (c.name ? " - " + c.name: "") + "</span></li>");
	// get the list entry
	var li = list.children("li").last();	
	// check for child controls
	if (c.childControls && c.childControls.length > 0) {		
		// add a list for the child controls
		li.append("<ul></ul>");
		// get the child list
		var childList = li.children("ul").last();
		// loop the child controls
		for (var i in c.childControls) {
			// create an entry for the child controls
			createMapEntry(childList, c.childControls[i]);
		}		
	}
}

// rebuild the page map
function buildPageMap() {	
	// get the map div
	var map = $("#pageMap");
	// unregister all listener
	removeMapListeners();
	// only if visible
	if (map.is(":visible")) {
		// get the list
		var list = $("#pageMapList");
		// empty the current list
		list.html("");		
		// check we have a page and childControls
		if (_page) {
			// build the map
			createMapEntry(list, _page);
			// add an onclick listener for all controls
			addMapListener( list.find("li").click( function(ev) {
				// get the target
				var t = $(ev.target);
				// get the id
				var id = t.attr("data-id");
				// get the control
				var c = getControlById(id);
				// select the control
				selectControl(c);
				// get a reference to our body
				// var body = $("body");
				// scroll to it if it's not on the page
				// if (ev.data && ev.data.object) body.scrollTop(ev.data.object.offset().top);
				// stop bubbling
				event.stopPropagation();
			}));	
			// highlight the selected control
			if (_selectedControl) {
				// highlight selected control
				list.find("span[data-id=" + _selectedControl.id + "]").addClass("selected");
			}
		}	
	}
}


// reusable function
function toggleMap() {
	
	// show the controls if the pageMap is visible
	var showControls = $("#pageMap").is(":visible");
	
	// check show / hide
	if (showControls) {
		// hide map instantly
		$("#pageMap").hide();	
		// show controls
		$("#controlsList").show("slide", {direction: "up"}, 500);
		// after 500 secs
		window.setTimeout( function() {
			// invert the up/down images
			$("#controlsHeader").children("img.headerToggle").attr("src","images/triangleUp_8x8.png");
			$("#controlsMap").children("img.headerToggle").attr("src","images/triangleDown_8x8.png");					
    	}, 500);	
	} else {
		// hide controls
		$("#controlsList").hide("slide", {direction: "up"}, 500);
		// show the map after 500 secs
		window.setTimeout( function() {
			// show the map
			$("#pageMap").show();
			// rebuild the controls
			buildPageMap();
			// invert the up/down images
			$("#controlsHeader").children("img.headerToggle").attr("src","images/triangleDown_8x8.png");
			$("#controlsMap").children("img.headerToggle").attr("src","images/triangleUp_8x8.png");
			// resize the window
			windowResize("controlsMap hide");
    	}, 500);						
	}
}

//JQuery is ready! 
$(document).ready( function() {
	
	// controls are clicked on
	$("#controlsHeader").click( function(ev) {
		toggleMap();		
		return false;
	});
	
	// map is clicked on
	$("#controlsMap").click( function(ev) {		
		toggleMap();
		return false;
	});
	
	// hide the map
	$("#pageMap").hide();
	
});	
