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
			// add a mousedown listener for all controls
			addMapListener( list.find("li").on("mousedown touchstart", function(ev) {
				// get the target
				var t = $(ev.target);
				// get the id
				var id = t.attr("data-id");
				// get the control
				var c = getControlById(id);
				// if we got one
				if (c) {
					// select the control
					selectControl(c);
					// set mouse down if this control can be moved
					if (_controlTypes[_selectedControl.type].canUserMove) _mouseDown = true;
				}
				// stop bubbling
				event.stopPropagation();
			}));	
			// add a mouseover listener for all controls
			addMapListener( list.find("li").on("mousemove touchmove", function(ev) {				
				// only if mouse is down
				if (_mouseDown) {
					// get the target
					var t = $(ev.target);					
					// get the id
					var id = t.attr("data-id");
					// get the control
					var c = getControlById(id);
					// if we got one
					if (c) {
						// if different from current movedover control
						if (c != _movedoverControl || !_movingControl) {
							// remove all insert covers
							$("#pageMapList").find("span.selectionInsertCover").removeClass("selectionInsertCover");
							// add the insert cover if not the selected control too
							t.addClass("selectionInsertCover");
							// rememeber we are moving a control
							_movingControl = true;
						}
						// set the movedoverControl
						_movedoverControl = c;							
						// check whether a decendent,
						if (isDecendant(_selectedControl,_movedoverControl) || _movedoverControl == null) {
							// null the movedoverControl
							_movedoverControl = null;	
						} else {
							
							// get the span width
							var width =  t.width();
							// calculate a move threshold which is the number of pixels to the left or right of the object the users needs to be within
							var moveThreshold = Math.min(50, width/3);
							// if it's not possible to insert make the move thresholds half the width to cover the full object
							if (!_controlTypes[_movedoverControl.type].canUserInsert) moveThreshold = width/2;
							
							// are we within the move threshold on the left or the right controls that can be moved, or in the middle with an addChildControl method?
							if (_controlTypes[_movedoverControl.type].canUserMove && ev.pageX  < t.offset().left + moveThreshold) {
								// set the cursor
								t.css("cursor","w-resize");
								// remember it's on the left
								_movedoverDirection = "L";
							} else if (_controlTypes[_movedoverControl.type].canUserMove && ev.pageX > t.offset().left + width - moveThreshold) {
								// set the cursor
								t.css("cursor","e-resize");
								// remember it's on the right
								_movedoverDirection = "R";
							} else if (_controlTypes[_movedoverControl.type].canUserInsert) {
								// set the cursor
								t.css("cursor","s-resize");
								// remember it's in the the centre
								_movedoverDirection = "C";
							} else {
								// reset the cursor
								t.css("cursor","initial");
								// null the direction
								_movedoverDirection = null;
								// remove all insert covers
								$("#pageMapList").find("span.selectionInsertCover").removeClass("selectionInsertCover");
							}		
														
						} // decendant check 								
					} // new moved over controls							

				} // mouse down
				// stop bubbling
				event.stopPropagation();
			}));	
			// add a mouseover listener an up anywhere in the box
			addMapListener( list.on("mouseup touchend", function(ev) {				
				// if there is a moved over control
				if (_movedoverControl) {
					// fire the main page mouse up
					
				}
			}));	
			// add a mouseout listener for the box
			addMapListener( list.on("mouseleave touchcancel", function(ev) {				
				// reset mousedown
				_mouseDown = false;
				// reset moved over control
				_movedoverControl = null;
				// remove all insert covers
				$("#pageMapList").find("span.selectionInsertCover").removeClass("selectionInsertCover");
				// set all cursors to default
				$("#pageMapList").find("span").css("cursor","initial");
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
