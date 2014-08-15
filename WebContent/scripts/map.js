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

function createMapEntry(list, c) {
	// create the list entry
	list.append("<li><span data-id='" + c.id + "'>" + ((c._class.image) ? "<img src='" + c._class.image + "'/>" : "") + c.type + (c.name ? " - " + c.name: "") + "</span></li>");
	// get the list entry
	var li = list.children("li").last();
	// add an onclick listener if the object is visible
	li.click( c, function(ev) {
		// select the control
		selectControl(ev.data);
		// scroll to it
		if (ev.data && ev.data.object) $("body").scrollTop(ev.data.object.offset().top);
		// stop bubbling
		return false;
	});	
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
function showPageMap() {	
	// get the map div
	var map = $("#pageMap");
	// only if visible
	if (map.is(":visible")) {
		// empty the current map
		map.html("");		
		// check we have a page and childControls
		if (_page) {
			// create the first list
			map.append("<ul></ul>");
			// get the list
			var list = map.find("ul").last();
			// build the map
			createMapEntry(list, _page);
			// highlight the selected control
			if (_selectedControl) {
				// highlight selected control
				$("#pageMap").find("span[data-id=" + _selectedControl.id + "]").css("background-color","#ccc");
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
		// resize the window
		windowResize("controlsMap hide");
	} else {
		// hide controls
		$("#controlsList").hide("slide", {direction: "up"}, 500);
		// show the map after 500 secs
		window.setTimeout( function() {
			// show the map
			$("#pageMap").show();
			// rebuild the controls
			showPageMap();
			// invert the up/down images
			$("#controlsHeader").children("img.headerToggle").attr("src","images/triangleDown_8x8.png");
			$("#controlsMap").children("img.headerToggle").attr("src","images/triangleUp_8x8.png");
    	}, 500);				
		// resize the window
		windowResize("controlsMap show");
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
