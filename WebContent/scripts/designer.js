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

/*

This is the core of the designer

Some terms:

Control - this refers to a control the user can add to a page, and is fairly high order, each control has a *.control.xml file in the controls directory that defines its behaviour

Object - this refers to the JQuery object made from the html

Geometry - the pixel space an object takes up 
 
*/

// the application we are designing
var _app = {};
// details of all the available apps
var _apps = [];
// the page of the app we are designing
var _page = {};
// a list of available pages
var _pages = [];
// a list of available actions
var _actions = [];
// a list of available style classes
var _styleClasses = [];

// the iframe that contains the page we are working on
var _pageIframe;
// the div that covers all of the components in design mode so they don't react to clicks
var _designCover;
// track whether the mouse is up or down
var _mouseDown = false;
// track mouseDown offsets
var _mouseDownXOffset = 0;
var _mouseDownYOffset = 0;
// track whether we are current moving a control
var _movingControl = false;
// we need to retain the control we have moved over
var _movedoverControl = null;
// we need to retain whether we are on the left or right so we insert before or after
var _movedoverDirection = "";
// we need to retain any controls we've moused over so we can fire mouse out
var _mousedOverControl = null;

// retain the currenty selected object
var _selectedControl = null;
// the div which we use a border around the selected object
var _selectionBorder;
// the div which we cover the selected object with whilst we are moving it around
var _selectionCover;
// the div which we place to show where an insert/move to the right would occur
var _selectionMoveLeft;
//the div which we place to show where an insert/move to the left would occur
var _selectionMoveRight;
// the div which we place to show where an insert would occur
var _selectionInsert;

// whether the left control panel is pinned
var _panelPinned = true;
// panel offset if pinned (includes padding and border)
var _panelPinnedOffset = 221;

// scroll bar width
var _scrollBarWidth = 0;

// retain the copied control
var _copyControl;
// undo stack
var _undo = [];
// redo stack
var _redo = [];
// whether there are unsaved changes
var _dirty;
// whether this page is locked for editing by another user
var _locked = true;

// the next control id
var _nextId = 1;
// the next page id
var _nextPageId = 1;

// a map of all former control and action id's and the new ones they get in the paste
var _pasteMap = null;

// a global object for the different devices we are supporting, typically for mobiles
var _devices = [{name:"Full screen", ppi: 96, scale: 1 },{name:"HTC One X", width: 720, height: 1280, ppi: 312, scale: 1},{name:"HTC One M8", width: 1080, height: 1920, ppi: 441, scale: 1},{name:"Nexus 10", width: 1600, height: 2560, ppi: 300, scale: 1}];
// a global for the ppi of the device we've loaded the designer in
var _ppi = 96;
// a global for the selected device index
var _device = 0;
// the zoom factor at which we want to see the device screen
var _zoom = 1;
// the orientation we want to see the device screen
var _orientation = "P";
// the difference in resolution between screen and device * zoom
var _scale = 1;
// the scale * zoom
var _mouseScale;

// takes a snapshot of the current page and adds it to the undo stack
function addUndo(keepRedo) {
	// set dirty
	_dirty = true;
	// grab the page
	var page = JSON.stringify(getDataObject(_page));
	// if the page is different from the last item on the undo stack push it on
	if (_undo.length == 0 || (_undo.length > 0 && page != _undo[_undo.length - 1])) _undo.push(page);
	// remove an item from the bottom of the stack if it's too big
	if (_undo.length > 50) _undo.splice(0, 1);
	// enable undo button
	$("#undo").enable();
	// undo snapshots from the undo button create a redo snap shotshot if the snapshot request comes from elsewhere remove redo
	if (!keepRedo && _redo.length > 0) {
		// empty the redo stack
		_redo = [];
		// disable the redo button
		$("#redo").disable();
	}
}

// takes a snapshot of the current page and adds it to the redo stack
function addRedo() {	
	// grab the page
	var page = JSON.stringify(getDataObject(_page));
	// only called in doUndo so less checking
	_redo.push(page);
	// enable undo button
	$("#redo").enable();
}

// used by both undo and redo to apply their snapshot
function applyUndoRedo(page) {
	
	// hide the selection border
	$("#selectionBorder").hide();
	// remove any dialogues or components
	$("#dialogues").children().remove();
	
	// retain reference to page object
	var pageObject = _page.object;
	// remove all current page html
	pageObject.children().remove();
	// retain the selected control id
	var selectedControlId = null;
	// if we have a selected control
	if (_selectedControl) selectedControlId = _selectedControl.id; 
	
	// lose the current selected object
	_selectedControl = null;
	// lose the property control
	_propertiesControl = null;		
	
	// load the page object from the undo snapshot
	_page = new Control(ControlClass_page, null, page, true, false, true);
		       	
	// put the object back
	_page.object = pageObject;
	
	// reset the next control id and control numbers 
	_nextId = 1;
	_controlNumbers = {};
	
	// load the child controls
	if (page.childControls) {
    	// loop the page childControls and create
    	for (var i = 0; i < page.childControls.length; i++) {
    		// get an instance of the control properties (which is what we really need from the JSON)
    		var childControl = page.childControls[i];
    		// create and add
    		_page.childControls.push(loadControl(childControl, _page, true, false, true));
    	}
	}
	
	// if there was a control selected
	if (selectedControlId) {
		// re-select the initial control
		_selectedControl = getControlById(selectedControlId);
		// rebuild any properties
		selectControl(_selectedControl);
	}
}

// takes the most recent snapshot off the top of the undo stack and applies it
function doUndo() {
	// retrieve the last page from the top of the undo stack
	var page = JSON.parse(_undo.pop());
	// if there was one
	if (page) {				
		// add a redo snapshot, just before we undo
		addRedo();
		// apply the undo
		applyUndoRedo(page);				
	}
	// disable undo button if there's nothing more on the stack
	if (_undo.length == 0) $("#undo").disable();
}

// takes the most recent snapshot off the top of the redo stack and applies it
function doRedo() {
	// retrieve the last page from the top of the redo stack
	var page = JSON.parse(_redo.pop());
	// if there was one
	if (page) {
		// add an undo snapshot, just before we redo with the keep redo set to true
		addUndo(true);
		// apply the redo
		applyUndoRedo(page);		
	}
	// disable redo button if there's nothing more on the stack
	if (_redo.length == 0) $("#redo").disable();
}

// if the page is dirty prompt the user that they will lose unsaved changes
function checkDirty() {
	if (_dirty) {
		return confirm("You will lose your unsaved changes. Are you sure?");
	} else {
		return true;
	}
}

// this function is useful for calling from the JavaScript terminal to find out why certain objects have not been found
function debuggMouseControl(ev, array) {	
	
	// now scale the mouse position for in-page controls
	var mouseX = ev.pageX  - _panelPinnedOffset;
	var mouseY = ev.pageY;
	
	// if the scale has been applied some other adjustments are needed
	if (_scale != 1) {
		// get the top and left page padding
		var padLeft = parseInt(_page.object.css("padding-left"));
		var padTop = parseInt(_page.object.css("padding-top"));
		// get the top and left page margin
		var marginLeft = parseInt(_page.object.css("margin-left"));
		var marginTop = parseInt(_page.object.css("margin-top"));
		mouseX = (mouseX + marginLeft) / _scale;
		mouseY = (mouseY + marginTop) / _scale;
	}
		
	console.log("X: " + mouseX + ", Y: " + mouseY);
	
	for (var i in array) {
		var object = array[i].object;
		console.log("id = " + object.attr("id") + " x1: " + object.offset().left + ", x2: " + (object.offset().left + object.outerWidth()) + ", y1: " + object.offset().top + ", y2: " + (object.offset().top + object.outerHeight()));
	}	
}

// this function finds the lowest control in the tree who's object encloses the ev.pageX and ev.pageY 
function getMouseControl(ev, array) {
			
	// set to true when debugging to avoid entering the block on every mouseMove
	var debug = false;
	// only if the mouse is down
	if (_mouseDown) {
										
		// check if we hit the border first		
		var o = $("#selectionBorder");				
		// if we didn't find a control but the selection border is visible, return the current control
		if (o.is(":visible") && !_movingControl) {						
			// did we click on the border (it's position already has the pinned panel taken into account so no need to offset)
			if (ev.pageX >= o.offset().left && ev.pageY >= o.offset().top && ev.pageX <= o.offset().left + o.outerWidth() && ev.pageY <= o.offset().top + o.outerHeight()) {
				// grab the selected object
				o = _selectedControl.object;
				// if we clicked in the object space we skip this section and process the event thoroughly
				if (!(ev.pageX - _panelPinnedOffset >= o.offset().left && ev.pageY >= o.offset().top && ev.pageX - _panelPinnedOffset <= o.offset().left + o.outerWidth() && ev.pageY <= o.offset().top + o.outerHeight())) {					
					// return the selected control
					return _selectedControl;
				}
			}
		}
		
		// we use this function recursively so start at the page if no array specified
		if (array == null) array = _page.childControls;
		
		// loop all of our objects for non-visual controls
		for (var i in array) {
			// get a reference to this control
			var c = array[i];
			// get a reference to this object
			var o = c.object;
			// only if this object is non-visual
			if (o.is(".nonVisibleControl")) {
				// is the mouse below this object
				if (ev.pageX >= o.offset().left && ev.pageY >= o.offset().top) {
					// get the height of the object
					var height = o.outerHeight();
					// does the width and height of this object mean we are inside it
					if  (ev.pageX <= o.offset().left + o.outerWidth() && ev.pageY <= o.offset().top + height) {
						// return this non-visual control
						return c;
					}
				}
			}
		}
				
		// now scale the mouse position for in-page controls
		var mouseX = ev.pageX  - _panelPinnedOffset;
		var mouseY = ev.pageY;
		
		// if the scale has been applied some other adjustments are needed
		if (_scale != 1) {
			// get the top and left page padding
			var padLeft = parseInt(_page.object.css("padding-left"));
			var padTop = parseInt(_page.object.css("padding-top"));
			// get the top and left page margin
			var padLeft = parseInt(_page.object.css("margin-left"));
			var padTop = parseInt(_page.object.css("margin-top"));
			mouseX = mouseX / _scale + padLeft;
			mouseY = mouseY / _scale + padLeft;
		}
		
		debuggMouseControl(ev, array);
		
		// loop all of our objects 
		for (var i in array) {
			// get a reference to this control
			var c = array[i];
			// get a reference to this object
			var o = c.object;
			// only if this object is visible
			if (o.is(":visible")) {
				// is the mouse below this object
				if (mouseX >= o.offset().left && mouseY >= o.offset().top) {
					// get the height of the object
					var height = o.outerHeight();
					// if the height is zero, but there are children assume the height of the first child (this is the case with ul elements where the child li elements are floated )
					if (!height && c.childControls.length > 0) height = c.childControls[0].object.outerHeight(); 
					// does the width and height of this object mean we are inside it
					if  (mouseX <= o.offset().left + o.outerWidth() && mouseY <= o.offset().top + height) {
						// if there are childObjects check for a hit on one of them
						if (c.childControls) {
							// use this function recursively
							var childControl = getMouseControl(ev, c.childControls);
							// if we got a hit on a child object return that instead
							if (childControl) c = childControl;
						}
						// if the mousedover control is different from the one before
						if (!_mousedOverControl || _mousedOverControl.object[0] !== c.object[0]) {
							// remember this as the mousedover controls
							_mousedOverControl = c;	
							// fire the moused over event
							_mousedOverControl.object.mouseover();		
						}											
						// return the control!
						return c;
					} // mouse within
				} // mouse below
			} // visible
		} // control loop
		
		// got to the end without hitting anything fire mouseout on mouseover control
		if (_mousedOverControl)	_mousedOverControl.object.mouseout();
		// unset mouseover control
		_mousedOverControl = null;
		
		// stop additional hits if debugging
		if (debug) _mouseDown = false;
	}
}

// before controls are positioned their parent's are removed as they could be placed anywhere in tree
function removeControlFromParent(control) {
	// loop all of our objects
	for (var i in control.parentControl.childControls) {
		// get a reference to this control
		var c = control.parentControl.childControls[i];
		// do we have a hit on this object?
		if (c === control) {
			// remove that object
			control.parentControl.childControls.splice(i,1);
			// set the parent to null
			control.parentControl = null;
			// bail
			break;
		}
	}
}

// this sizes a border around the geometry of a control (it must be visible at the time, however briefly)
function sizeBorder(control) {
	// get the height of the control's object
	var height = control.object.outerHeight();
	// if the height is zero but there are child controls, assume the height of the first child control
	if (!height && control.childControls.length > 0) height = control.childControls[0].object.outerHeight();
	// get the width
	var width = control.object.outerWidth()
	// get the device
	var device = _devices[_device];
	// check if nonVisualControl
	if (control.object.is(".nonVisibleControl")) {
		width += 1;
		height += 1;
	} else {
		width = width * _scale * device.scale + 2;
		height = height * _scale * device.scale + 2;
	}
	// size the selection border
	_selectionBorder.css({
		"width":width, // an extra pixel either side
		"height":height // an extra pixel either side
	});
}

// this positions the selection border inclduing the mouseDown Offsets which should be zero when the mouse is not moving
function positionBorder(x, y) {
	// check if if nonVisibleControl
	if (_selectedControl.object.is(".nonVisibleControl")) {
		// position the selection border
		_selectionBorder.css({
			position: "fixed",
			left: x + _mouseDownXOffset - 8, // 8 = padding + border + 1 pixel
			top: "auto",
			bottom: 3
		});	
	} else {
		// position the selection border
		_selectionBorder.css({
			position: "absolute",
			left: x + _panelPinnedOffset + _mouseDownXOffset - 8 , // 8 = padding + border + 1 pixel	
			top: y + _mouseDownYOffset - 8, // 8 = padding + border + 1 pixel
			bottom: "auto"
		});	
	}
	
}

// this function returns a flat array of all of the page controls
function getControls(childControls, controls) {
	if (!childControls) childControls = _page.childControls;
	if (!controls) controls = [];
	for (var i in childControls) {
		var c = childControls[i];
		controls.push(c);
		getControls(c.childControls,controls);
	}	
	return controls;
}

// this function returns a set of options for a dropdown using the current set of pages
function getPageOptions(selectId, ignoreId) {
	var options = "";
	for (var i in _pages) {
		var page = _pages[i];
		if (page.id != ignoreId) options += "<option value='" + page.id + "' " + (page.id == selectId ? "selected='selected'" : "") + ">" + page.title + "</option>"; 
	}
	return options;
}

// this function returns a set of options for a dropdown of controls
function getControlOptions(selectId, ignoreId) {
	var controls = getControls();
	var options = "";
	for (var i in controls) {
		var control = controls[i];
		// note how only control with names are included
		if (control.id != ignoreId && control.name) options += "<option value='" + control.id + "' " + (control.id == selectId ? "selected='selected'" : "") + ">" + control.name + "</option>"; 
	}
	return options;
}

//this function returns a set of options for a dropdown of security roles
function getRolesOptions(selectRole, ignoreRoles) {
	var options = "";
	var roles = _app.roles;
	if (roles) {		
		for (var i in roles) {
			// retrieve this role			
			var role = roles[i];
			// assume we're not going to ignore it
			var ignore = false;
			// loop ignore roles
			if (ignoreRoles) {
				for (var j in ignoreRoles) {
					if (role == ignoreRoles[j]) {
						ignore = true;
						break;
					}
				}
			}			
			// if we're not going to ignore it
			if (!ignore) options += "<option " + (role == selectRole ? "selected='selected'" : "") + ">" + role + "</option>"; 
		}
	}
	return options;
}

// this function returns a set of options for a dropdown of sessionVariables and controls with a getData method
function getDataOptions(selectId, ignoreId) {
	var options = "";	
	var controls = getControls();
	var gotSelected = false;
	if (controls) {
		options += "<optgroup label='Page controls'>";
		for (var i in controls) {			
			var control = controls[i];
			if (control._class.getDataFunction && control.id != ignoreId && control.name) {
				if (control.id == selectId && !gotSelected) {
					options += "<option value='" + control.id + "' selected='selected'>" + control.name + "</option>";
					gotSelected = true;
				} else {
					options += "<option value='" + control.id + "' >" + control.name + "</option>";
				}				
			}
		}
		options += "</optgroup>";
	}	
	if (_page && _page.sessionVariables) {
		options += "<optgroup label='Session variables'>";
		for (var i in _page.sessionVariables) {
			if (selectId == _page.sessionVariables[i] && !gotSelected) {
				options += "<option value='" + _page.sessionVariables[i] + "' selected='selected' >" + _page.sessionVariables[i] + "</option>";
				gotSelected = true;
			} else {
				options += "<option value='" + _page.sessionVariables[i] + "' >" + _page.sessionVariables[i] + "</option>";
			}			
		}
		options += "</optgroup>";
	}
	if (_page && _pages) {
		var groupOpen = false;
		var groupClose = false;
		for (var i in _pages) {
			if (_pages[i].id != _page.id && _pages[i].controls) {
				if (!groupOpen) {
					options += "<optgroup label='Other page controls'>";
					groupOpen = true;
				}
				for (var j in _pages[i].controls) {
					if (selectId == _pages[i].controls[j].id && !gotSelected) {
						options += "<option value='" + _pages[i].controls[j].id + "' selected='selected' >" +  _pages[i].title + "." + _pages[i].controls[j].name + "</option>";
						gotSelected = true;
					} else {
						options += "<option value='" + _pages[i].controls[j].id + "' >" +  _pages[i].title + "." + _pages[i].controls[j].name + "</option>";
					}					
				}
			}			
		}
		if (groupOpen && !groupClose) options += "</optgroup>";
	}
	return options;
}

// this function returns a set of options for a dropdown of sessionVariables and controls with a getData method
function getInputOptions(selectId, ignoreId) {
	return getDataOptions(selectId, ignoreId);
}

//this function returns a set of options for a dropdown of sessionVariables and controls with a setData method
function getOutputOptions(selectId, ignoreId) {
	return getDataOptions(selectId, ignoreId);
}


// this function returns a set of options for a dropdown of existing actions from current controls 
function getExistingActionOptions(selectId, ignoreId) {
	var options = "";
	for (var i in _page.events) {
		var event = _page.events[i];
		for (var j in event.actions) {
			var action = event.actions[j];
			if (action.id != ignoreId) options += "<option value='" + action.id + "' " + (action.id == selectId ? "selected='selected'" : "") + ">" + _page.name + "." + event.type + "." + action.type + " " + (j*1+1) + "</option>";
		}			
	}
	var controls = getControls();	
	for (var i in controls) {
		for (var j in controls[i].events) {
			var event = controls[i].events[j];
			for (var k in event.actions) {
				var action = event.actions[k];
				if (action.id != ignoreId) options += "<option value='" + action.id + "' " + (action.id == selectId ? "selected='selected'" : "") + ">" + controls[i].name + "." + event.type + "." + action.type + " " + (k*1+1) + "</option>";
			}			
		}
	}
	return options;
}

function getDatabaseConnectionOptions(selectIndex) {
	var options = "";
	if (_app.databaseConnections) {
		for (var i in _app.databaseConnections) {
			options += "<option value='" + i + "' " + (i == selectIndex ? "selected='selected'" : "") + ">" + _app.databaseConnections[i] + "</option>";
		}
	}
	return options;
}

// move the border and show properties and actions
function selectControl(control) {
	
	// show all details or cleanup if null
	if (control) {
		
		// store the selection globally
		_selectedControl = control;
		
		// set background of all controls in map to white
		$("#pageMap").find("span").css("background-color","white");
		// highlight selected control
		$("#pageMap").find("span[data-id=" + control.id + "]").css("background-color","#ccc");
		
		// get the body into an object
		var body = $("body");
		
		// retain the current scroll positions
		var scollTop= body.scrollTop();
		var scrolLeft = body.scrollLeft();
		
		// show the properties
		showProperties(_selectedControl);
		// show the validation
		showValidation(_selectedControl);
		// show the events (and the actions)
		showEvents(_selectedControl);
		// show the styles
		showStyles(_selectedControl);
										
		// selectChild
		if (_selectedControl.childControls.length > 0) {
			$("#selectChild").removeAttr("disabled");
		} else {
			$("#selectChild").attr("disabled","disabled");
		}
		
		// if we have a parent control so aren't the page
		if (_selectedControl.parentControl) {
			
			// position the border
			positionBorder(_selectedControl.object.offset().left, _selectedControl.object.offset().top);
			// size the border
			sizeBorder(_selectedControl);
			// show the border if it has any size to it	and the control is visible		
			if (_selectionBorder.width() > 5 && _selectedControl.object.is(":visible")) {
				_selectionBorder.show();
			} else {
				_selectionBorder.hide();
			}
			
			// count the number of child controls
			var contCount = 0;

			// count the controls of this type			
			for (var i in _selectedControl.parentControl.childControls) {
				if (_selectedControl._class.type == _selectedControl.parentControl.childControls[i]._class.type) contCount ++;
			}

			// can delete if no parent class (page control), can insert into parent, or canUserAddPeers and more than 1 peer of this type
			if (!_selectedControl.parentControl._class || _selectedControl.parentControl._class.canUserInsert || (_selectedControl._class.canUserAddPeers && contCount > 1)) {
				$("#deleteControl").removeAttr("disabled");
			} else {
				$("#deleteControl").attr("disabled","disabled");
			}
			
			// addPeerLeft and addPeerRight
			if (_selectedControl._class.canUserAddPeers) {
				$("#addPeerLeft").removeAttr("disabled");
				$("#addPeerRight").removeAttr("disabled");
			} else {
				$("#addPeerLeft").attr("disabled","disabled");
				$("#addPeerRight").attr("disabled","disabled");
			}
			
			
			// get position in parents
			for (var i in _selectedControl.parentControl.childControls) {
				if (_selectedControl == _selectedControl.parentControl.childControls[i]) break;
			}
			// turn into a number
			i = i*1;		
			// selectPeerLeft
			if (i == 0) {
				$("#selectPeerLeft").attr("disabled","disabled");
				$("#swapPeerLeft").attr("disabled","disabled");				
			} else {
				$("#selectPeerLeft").removeAttr("disabled");
				$("#swapPeerLeft").removeAttr("disabled");
			}
			
			// selectPeerRight
			if (i == _selectedControl.parentControl.childControls.length - 1) {
				$("#selectPeerRight").attr("disabled","disabled");
				$("#swapPeerRight").attr("disabled","disabled");
			} else {
				$("#selectPeerRight").removeAttr("disabled");
				$("#swapPeerRight").removeAttr("disabled");
			}
			
			// selectParent
			if (_selectedControl.parentControl) {
				$("#selectParent").removeAttr("disabled");
			} else {
				$("#selectParent").attr("disabled","disabled");
			}
											
			// paste - these rules are fairly tricky: 
			// # for pasting as a child (which is the preference)
			// controls with canUserAdd can be pasted into controls with canUserIsert
			// controls that don't have canUserAdd can be pasted into childControls if any childControls of the same type have canUserAddPeers
			// # for pasting as a peer (which is a fall-back)
			// controls with canUserAdd can be pasted into page or where parent control has canUserInsert
			// control that don't have canUserAdd can be pasted as a peer if any parent.childControls of the same type have canUserAddPeers
			var childCanAddPeers = false;
			var peerCanAddPeers = false;
			if (_copyControl) {
				// find out if there are childControls with the same type with canUserAddPeers			
				for (i in _selectedControl.childControls) {
					if (_copyControl.parentControl && _copyControl._class.type == _selectedControl.childControls[i]._class.type && _selectedControl.childControls[i]._class.canUserAddPeers) {
						childCanAddPeers = true;
						break;
					}
				}
				// find out if there are peers with the same type with canUserAddPeers			
				for (i in _selectedControl.parentControl.childControls) {
					if (_copyControl.parentControl && _copyControl._class.type == _selectedControl.parentControl.childControls[i]._class.type && _selectedControl.parentControl.childControls[i]._class.canUserAddPeers) {
						peerCanAddPeers = true;
						break;
					}
				}
			}		
			// once we know if something allowed enabling/disabling is a lot easier
			if (_copyControl && (_selectedControl._class.canUserInsert || childCanAddPeers || peerCanAddPeers)) {
				$("#paste").removeAttr("disabled");
			} else {
				$("#paste").attr("disabled","disabled");
			}
						
		} else {

			// hide the selection border (this is the page)
			_selectionBorder.hide();	
			// disable swapping of peers
			$("#swapPeerLeft").attr("disabled","disabled");
			$("#swapPeerRight").attr("disabled","disabled");
			// disable selection of parent
			$("#selectParent").attr("disabled","disabled");			
			// disable selection of peers
			$("#selectPeerLeft").attr("disabled","disabled");
			$("#selectPeerRight").attr("disabled","disabled");
			// disable adding of peers
			$("#addPeerLeft").attr("disabled","disabled");
			$("#addPeerRight").attr("disabled","disabled");
			
			// if the copy control is a canUserAdd or the page we can paste
			if (_copyControl && (!_copyControl.parentControl || _copyControl._class.canUserAdd)) {
				$("#paste").removeAttr("disabled");
			} else {
				$("#paste").attr("disabled","disabled");
			}			
		}
						
		// show the properties panel	
		showPropertiesPanel();	
		
		// revert the scroll positions
		body.scrollTop(scollTop);
		body.scrollLeft(scrolLeft);
		
	} else {
		
		_selectedControl = null;
		// hide the selection border
		_selectionBorder.hide();
						
		// hide the properties panel
		$("#propertiesPanel").hide("slide", {direction: "right"}, 200);
		
		// show null properties
		showProperties(null);
		// show null validation
		showValidation(null);
		// show null events (and the actions)
		showEvents(null);
		// show null styles
		showStyles(null);
		
		// resize the window
		windowResize("mousedone-nocontrol");
		
	}	
	
}

// this function shows the whole designer to the user, usually after the first page is loaded but possible earlier if there are no applications or pages
function showDesigner() {	
	// hide the loading message
	$("#loading").hide();
	// show the control panel and properties panel
	$("#designerTools").show();
	// show the page
	$("#page").show();
	// resize the elements on the page
	windowResize("showDesigner");
	// arrange any non-visible controls
	arrangeNonVisibleControls();	
	// show the first tip, if function is present
	if (window["showTip"]) showTip(0);
}

//this function load the apps into appsSelect
function loadApps(selectedAppId, forceLoad) {
	
	// hide the properties panel
	$("#propertiesPanel").hide();
	// remove all current page html
	$("#page").children().remove();
	// remove any dialogues or components
	$("#dialogues").children().remove();
	// remove any current apps
	$("#appSelect").children().remove();
	
	// do the ajax
	$.ajax({
    	url: "designer?action=getApps",
    	type: "GET",
    	contentType: "application/json",
        dataType: "json",            
        data: null,            
        error: function(server, status, error) {
        	// check if there was permission to use rapid
        	if (server && server.status == 403) {
        		// reload the whole page (sends user to login)
        		loaction.reload();
        	} else {
        		// show the error
        		alert("Error loading applications : " + error);
        	}
        },
        success: function(apps) {        	
        	// if an app is not selected try the url
        	if (!selectedAppId) var urlAppId = $.getUrlVar("a");
        	// build the select options for each app
        	var options = "";
        	// loop the apps we received
        	for (var i in apps) {        		
        		// get a reference to the app
        		var app = apps[i];
        		// add an option for this page (if not the rapid app itself)
        		options += "<option value='" + app.id + "' " + (selectedAppId || urlAppId == app.id ? "selected='true'" : "") + ">" + app.id + " - " + app.title + "</option>";        	
        	}
        	// get a reference to apps dropdown
        	var appsDropDown = $("#appSelect");
        	// put the options into the dropdown
        	appsDropDown.html(options);
        	// retain all the apps data
        	_apps = apps;
        	// set the selected app
        	_app = _apps[appsDropDown[0].selectedIndex];        	
        	// load the app and its pages in the drop down if we weren't handed one
        	if (!selectedAppId || forceLoad) {
        		loadApp();
        	} else {
        		// show the designer
        		showDesigner();
        	}
        }
	});
}


// this function loads the apps pages into pagesSelect
function loadApp(forceLoad) {
	
	// grab a reference to the ul where the canUserAdd controls will be added
	var designControls = $("ul.design-controls");
	// hide the controls panel
	designControls.hide();
	// hide the map
	$("#pageMap").hide();
	// empty the designControls panel
	designControls.children().remove();	
	// empty the action options array
	_actionOptions = [];
	// empty the style classes array
	_styleClasses = [];
	// remove any dialogues or components
	$("#dialogues").children().remove();
	// empty the pages list
	_pages = [];
	
	// check we have some apps
	if (_apps) {
		// get the selected app
		var appId = $("#appSelect").val();
		// loop all the apps
    	for (var i in _apps) {    		
    		// if this app matches what's in the dropdown
    		if (_apps[i].id == appId) {
    			// set the global to this one
    			_app = _apps[i];
    			// we're done
        		break;
    		}    		
    	}
	}
			
	// check there is an app
	if (_app) {		
					
		// loop the app actions
		for (var j in _app.actions) {
			// get a reference to the action
			var action = _app.actions[j];
			// add to our _actionOptions excluding the rapid action unless this is the rapid app
			if (action.type != "rapid" || _app.id == "rapid") _actionOptions += "<option value='" + action.type + "'>" + action.name + "</option>";
		}
		
		// retain the app styleclasses
		_styleClasses = _app.styleClasses;
		
		// loop the app controls
    	for (var j in _app.controls) {	    	    		
    		
    		// get a reference to a single control
    		var c = _app.controls[j];      		
    		
    		// if the control can be added by the user
    		if (c.canUserAdd) {
    			
    			// add button (list item + image if exists)
    			designControls.append("<li class='design-control' data-control='" + c.type + "'>" + ((c.image) ? "<img src='" + c.image + "'/>" : "") + "<span>" + c.name + "</span></li>");
    			
    			// when the mouse moves down on this component
    			designControls.children().last().mousedown( function(ev) {		
    				
    				// add an undo position
    				addUndo();
    				
    				// stop text selection as we are moving the new object
    				$("body").css({
    					"-webkit-touch-callout":"none",
    					"-webkit-user-select":"none",
    					"-khtml-user-select":"none",
    					"-moz-user-select":"-moz-none",
    					"-ms-user-select":"none",
    					"user-select":"none"
    				});	
    							
    				// hide the panel
    				if (!_panelPinned) $("#controlPanel").hide("slide", {direction: "left"}, 200);
    				
    				// hide the properties
    				$("#propertiesPanel").hide("slide", {direction: "right"}, 200);
    				
    				// get the control constructor name (way easier to use an attribute than closures)
    				var className = $(ev.target).attr("data-control");
    				if (!className) className = $(ev.target).parent().attr("data-control");
    				
					// get the control constructor
					var controlClass = window["ControlClass_" + className];
					
					// there is a function to create this control
					if (controlClass) {    						
						// instantiate the control with the _page as the parent
						var control = new Control(controlClass, _page, null, true);										
						// size the border for the control while it is still visible		
						sizeBorder(control);
						// set the mouseDown offsets so when we drag the mouse is in the center
						_mouseDownXOffset = -control.object.outerWidth()/2 - _panelPinnedOffset;
						_mouseDownYOffset = -control.object.outerHeight()/2;				
						// hide the control's object as we have the geometery we need
						control.object.hide();
						// show the selection border
						_selectionBorder.show();	
						// set its parent to the _page
						control.parentControl = _page;
						// add it to the _page childControls collection
						_page.childControls.push(control);					
						// retain a reference to the selected control					
						_selectedControl = control;	
						// show the properties
						showProperties(_selectedControl);
						// show the validation
						showValidation(_selectedControl);
						// show the events (and any actions)
						showEvents(_selectedControl);
						// show the styles
						showStyles(_selectedControl);
						// set the _mouseDown so the moving kicks in
						_mouseDown = true;    						
					} else {
						alert("The control cannot be created. " + className + "() can't be found.");
					}    
					
					// we only need the hit on the li
					ev.stopPropagation();
											
    			}); // mouse down
    			
    		} // userCanAdd
    		
    	} // app control loop  
    			

    	// load the pages with a forced page load
    	loadPages(null, true);
		
		// disable the delete button if no rapid app or this is the rapid app
		if (_app.id == "rapid") {
			$("#appDelete").attr("disabled","disabled");
		} else {
			$("#appDelete").removeAttr("disabled");
		}
		// allow editing the app
		$("#appEdit").removeAttr("appEdit");
		
	} else {
		// disable a bunch of stuff as there there are no pages and no apps
		$("#appDelete").attr("disabled","disabled");
		$("#appEdit").attr("disabled","disabled");
		$("#pageNew").attr("disabled","disabled");
		$("#pageEdit").attr("disabled","disabled");
		$("#pageSave").attr("disabled","disabled");
		$("#pageView").attr("disabled","disabled");
		// show the designer
		showDesigner();
	} // no app id
	
	// show the controls panel
	designControls.show();
	
}

// this function loads the selected apps pages into the drop down, in case the order has changed
function loadPages(selectedPageId, forceLoad) {
	
	$.ajax({
    	url: "designer?action=getPages&a=" + _app.id,
    	type: "GET",
    	contentType: "application/json",
        dataType: "json",            
        data: null,            
        error: function(server, status, error) { 
        	// show the designer as there's a small chance it might not be visible yet
        	showDesigner();
        	// if it's an authentication thing
        	if (server && server.status == 403) {
        		// reload the page from the top
        		location.reload(true);
        	} else {
        		// show an error
        		alert("Error loading pages : " + error);
        	}
        },
        success: function(pages) {        	       	
        	
        	// reset the next page id to 1
        	_nextPageId = 1;
        	// if a page is not selected try the url
        	if (!selectedPageId) selectedPageId = $.getUrlVar("p");
        	// build the select options for each page
        	var options = "";
        	// retain the pages
        	_pages = pages;
        	
        	// loop them
        	for (var i in pages) {
        		// get a reference to the page
        		var page = pages[i];
        		// assume not selected
        		var selected = "";
        		// check if selected already or if not whether its the start page
        		if (selectedPageId == page.id || (!selectedPageId && page.startPage)) selected = "selected='true'";
        		// add an option for this page
        		options += "<option value='" + page.id + "' " + selected + ">" + page.name + " - " + page.title + "</option>";
        		// check the next pageId
        		if (parseInt(page.id.substring(1)) >= _nextPageId) _nextPageId = parseInt(page.id.substring(1)) + 1; 
        	}
        	
        	// put the options into the dropdown
        	$("#pageSelect").html(options);
        	
        	// check we got some pages
        	if (options) {
        		// unlock controls and page edit
        		$("#controlControls").show();
        		$("#pageEdit").removeAttr("disabled");
        		$("#pageSave").removeAttr("disabled");
        		$("#pageView").removeAttr("disabled");
        		// only if we have to do we load the selected page
        		if (forceLoad) loadPage();
        	} else {
        		// remove any current html
        		if (_page.object) _page.object.children().remove();
        		// lock controls and page edit
        		$("#controlControls").hide();
        		$("#pageEdit").attr("disabled","disabled");
        		$("#pageSave").attr("disabled","disabled");
        		$("#pageView").attr("disabled","disabled");
        		// show the designer
        		showDesigner();
        		// show the new page dialogue
        		showDialogue('~?action=page&a=rapid&p=P3'); 
        	}
        	
        } // success
	}); // ajax
	
}

// this function loads the controls into the page
function loadPage() {
	
	// hide the properties panel
	$("#propertiesPanel").hide();	
	// remove any dialogues or components
	$("#dialogues").children().remove();	
	// hide any selection border
	if (_selectionBorder) _selectionBorder.hide();	
	// remove any current page html
	if (_page.object) _page.object.children().remove();
	// remove any nonVisibleControls
	$(".nonVisibleControl").remove();
	// lose the selected control
	_selectedControl = null;	
	// lose the property control
	_propertiesControl = null;	
	// set the next id back
	_nextId = 1;
	// reset the control numbering
	_controlNumbers = {};
	// empty undo stack
	_undo = [];	
	// disable undo
	$("#undo").disable();
	// empty redo stack
	_redo = [];
	// disble redo
	$("#redo").disable();
	
	// get the id of the selected page
	var pageId = $("#pageSelect").val();	
	// check there is a page selected in the dropdown
	if (pageId) {										
		// set the page id
		_page.id = pageId;		
		// reload the page iFrame with resources for the app and this page
    	_pageIframe[0].contentDocument.location.href = "designpage.jsp?a=" + _app.id + "&p=" + _page.id;	
    	// set dirty to false
    	_dirty = false;    	
	} // drop down val check
	
}

//this function removes properties that create circular references from the control tree when saving
function getDataObject(object) {
	// make a new empty object
	var o = {};
	// loop the properties
	for (var i in object) {
		// ignore "static" properties, or those that create circular references
		if (i.indexOf("_") != 0 && i != "object" && i != "parentControl") {
			// grab a property
			var p = object[i];
			// if a blank space, or not a null
			if (p === "" || p != null) {
				// child controls and actions need cleaning up recursively 
				if (p.type && (window["ControlClass_" + p.type] || window["ActionClass_" + p.type])) {
					// get an object
					o[i] = getDataObject(p);
				} else if ($.isArray(p) && p.length > 0 && p[0].type && (window["ControlClass_" + p[0].type] || window["ActionClass_" + p[0].type] || i == "events" )) {
					// make an array
					o[i] = [];
					// loop to clean up childControls
					for (var j in p) o[i].push(getDataObject(p[j]));								
				} else {
					// simple copy
					o[i] = p;
				}
				// copy the type in from _class  (page "control" doesn't have one)
				if (object._class) o.type = object._class.type;
			}
		}
	}
	// return our safe object
	return o;	
}

// the page can't be strigified as is so remove the objects with the above iterative function and send that
function getSavePageData() {
	
	// retain the id of any selected control
	var selectedControlId = null;
	if (_selectedControl) selectedControlId = _selectedControl.id;
	
	// get all of the controls
	var controls = getControls();
	// create a list of roles used on this page
	var pageRoles = [];
	
	// show message
	$("#rapid_P11_C7_").html("Checking controls");
	
	// loop them looking for roles and getDetails functions to run 
	for (var i in controls) {
		// get the control
		var control = controls[i];
		// check for any roles
		if (control.roles) {			
			// loop the control's roles
			for (var j in control.roles) {
				// assume we don'y know about this role yet
				var gotRole = false;
				// loop the known roles
				for (var k in pageRoles) {
					// if we do known about the control 
					if (control.roles[j] == pageRoles[k]) {
						// record that we know
						gotRole = true;
						// stop looping
						break;
					}					
				}
				// if we've not seen this control retain it in the known collection
				if (!gotRole) pageRoles.push(control.roles[j]);
			}
		}
		// check for a get details function
		if (control._getDetails) {
			// run the get details function
			control.details = control._getDetails.apply(control, []);
		}
	}
	
	// get a page object based on the page "control" (this creates a single property array called childControls)
	var pageObject = getDataObject(_page);
			
	// get the roles from the app
	var roles = _app.roles;	
	// get all possible combinations of the roles in this page
    var combinations = []; 
    
    // show message
	$("#rapid_P11_C7_").html("Checking roles");
    
	// if this application has any explicit roles, we need to build the possible combinations
	if (pageRoles.length > 0) {
				
	 	// left shift 1 by length to raise to power of length
	    var quantity = 1 << pageRoles.length; 
	 	// loop the powered length
	    for (var i = 1; i < quantity; i++) {
	    	// combinations of this length
	        var combination = []; 
	    	// check each item
	        for (var j = 0; j < pageRoles.length; j++) {
	        	// binary and
	            if (i & (1 << j)) combination.push(pageRoles[j]);	            
	        }
	        if (combination.length !== 0) combinations.push(combination);	        
	    }
	    		
		// sort the combinations
	    combinations.sort( function(a, b) {
			// a and b are each a list of job roles
			if (a.length == b.length) {
				// loop the items
				for (var i = 0; i < a.length; i ++) {
					// retrieve the individual roles
					var aRole = a[i];
					var bRole = b[i];
					// get the min length
					var l = Math.min(aRole.length, bRole.length);
					// loop the chars
					for (var j = 0; j < l; j++) {
						// get the chars
						var aChar = aRole.charAt(j);
						var bChar = bRole.charAt(j);
						// if different return
						if (aChar != bChar) return aChar - bChar; 
					}
					// if we got all the way here use length of string
					return aRole.length - bRole.length;
				}
			} else {
				// return the difference in length
				return b.length - a.length;
			}
		});
	    	    		
	}
	
	// add a combination to represent a user with no roles
	combinations.push([]);
	
	// add an array to the page object for the each role combination html
    pageObject.rolesHtml = [];
    
    // show message
	$("#rapid_P11_C7_").html("Generating html");
    
    // for each combination
    for (var i in combinations) {
    	
    	// get the combination
    	var combination = combinations[i];
    	
    	// get a fresh set of all the controls (as new html will have been added in the regeneration between combinations)
    	controls = getControls();
    	// get a fresh role controls array
    	var roleControls = [];
    	    	    	
    	// loop them looking for roles, and pre-save functions to run 
    	for (var j in controls) {    		
    		// get the control
    		var control = controls[j];
    		// check for roles
    		if (control.roles) {
    			// remember this control has roles
    			roleControls.push(control);
    		}
    		// check for a pre-save function
    		if (control._save) {
    			control._save();
    		}
    	}
    	
    	// loop only the controls that have roles, removing them if no role in this combination
    	for (var j in roleControls) {
    		// get an instance of the control
    		var roleControl = roleControls[j];
    		// assume we don't have the role in this combination
    		var gotRole = false;
    		// loop the controls roles (should be smaller than combination roles)
    		for (var k in roleControl.roles) {
    			// loop the combination
	    		for (var l in combination) {
	    			// if a role in the control is present in the combination
	    			if (roleControl.roles[k] == combination[l]) {
	    				// remember we've got the role
	    				gotRole = true;
	    				// exit the combination loop
	    				break;
	    			}
	    		}
	    		// exit the control role loop if we found the role earlier
	    		if (gotRole) break;
    		}
    		// remove the control if we don't have a role for it in this combination
    		if (!gotRole) roleControl._remove();	    		
    	}
    	
    	// add the html for this security role combination to the pageObject rolesHtml property
    	pageObject.rolesHtml.push( { roles : combination, html :  _page.object.html() });
    	    	
    	// remove any dialogues or components
    	$("#dialogues").children().remove();
    	// empty the child controls collection
    	_page.childControls = [];
    	// remove the child controls from the page
    	_page.object.children().remove();
    		    	
    	// loop the current page childControls and re-create
    	for (var j = 0; j < pageObject.childControls.length; j++) {
    		// get an instance of the control properties (which is what we really need from the JSON)
    		var control = pageObject.childControls[j];
    		// create and add (using the undo routine)
    		_page.childControls.push( loadControl(control, _page, true, false, true));
    	}
    	// arrange any non-visible controls
    	arrangeNonVisibleControls();
    		    		    	
    }
    
    // add the page html this is used by the designed and is always the html for the combination with the most roles
	pageObject.htmlBody = pageObject.rolesHtml[0].html;
		
	// stringify the page control object and add to the page (this creates an array called childControls)
	var pageData = JSON.stringify(pageObject);
	
	// re-selected any selected control
	if (selectedControlId) {
		// re-select the initial control
		_selectedControl = getControlById(selectedControlId);
		// rebuild any properties
		selectControl(_selectedControl);
	}
	
	// show message
	$("#rapid_P11_C7_").html("Sending html");
	
	// return it
	return pageData;	
}

function cleanControlForPaste(control) {
	// create an empty clean control object
	var cleanControl = {};	
	// loop the properties, ignoring certain ones
	for (var i in control) {
		if (i.indexOf("_") != 0 && i != "object" && i != "parentControl" && i != "childControls") {
			cleanControl[i] = control[i];
		}
	}
	// add a child control collection
	cleanControl.childControls = [];
	// loop the child controls
	for (var i in control.childControls) {
		// add a clean child control
		cleanControl.childControls.push(cleanControlForPaste(control.childControls[i]));
	}
	// return the clean control
	return cleanControl;
}

function applyStyleForPaste(control, styleSheet) {
	// check has style
	if (control.styles) {
		// loop them
		for (var i in control.styles) {
			var style = control.styles[i];
			var appliesTo = style.appliesTo;
			// create a style sheet rule
			var styleSheetRule = appliesTo + " {";
			// loop the style rows and add to the style rules
			for (var j in style.rules) {
				// get the rule 
				var rule = style.rules[j];
				// if we got something 
				if (rule) {
					// add it to the styleSheetRule
					styleSheetRule += rule;
				}
			};
			// close the styleSheet rule
			styleSheetRule += "}";
			// if there are rules
			if (style.rules.length > 0) {
				// add the styleSheet rule
				if (styleSheet) {
					// check length of parameters
					if (styleSheet.insertRule.length > 1) {
						styleSheet.insertRule(styleSheetRule, 0);
					} else {
						try { styleSheet.insertRule(styleSheetRule); } catch (ex) {}						
					}					
				}
			}
		}
	}
	for (var i in control.childControls) {
		applyStyleForPaste(control.childControls[i], styleSheet);
	}
}

// this function will paste an existing control into a specified parent - if no parent is specified we assume we are pasting a whole page
function doPaste(control, parentControl) {
		
	// remove any dialogues or components
	$("#dialogues").children().remove();
	
	// reset the paste map
	_pasteMap = {};
	
	// it's a little different for the page (we can idenitfy it as it doesn't have a parent)
	if (parentControl) {
		
		// create the new control
		var newControl = loadControl(control, parentControl, true, true);
		
		// retain the next id at this point
		var nextId = _nextId;
		
		// retain the control numbers at this point
		var controlNumbers = JSON.stringify(_controlNumbers);
		
		// remove the current object if not the body
		if (!newControl.object.is("body")) newControl._remove();
		
		// remove any items that were placed in dialogues
		$("#dialogues").children().remove();
		
		// clean the control for stringifying
		var cleanControl = cleanControlForPaste(newControl);
		
		// stringify newControl
		var newControlString = JSON.stringify(cleanControl);
		
		// loop all entries in the paste map
		for (var i in _pasteMap) {
			// update all references
			newControlString = newControlString.replaceAll(_pasteMap[i],i);
		}
		
		// turned the replaced string back into an object
		var mappedControl = JSON.parse(newControlString);
		
		// reload the control with all the new references
		newControl = loadControl(mappedControl, parentControl, true, true);
		
		// apply any styling in the new control
		applyStyleForPaste(newControl, getStyleSheet());
		
		// restore the next id
		_nextId = nextId;
		
		// restore the control numbers
		_controlNumbers = JSON.parse(controlNumbers);
		
		// return the updated control
		return newControl;
				
	} else {
			
		// remove all children
		_page.object.children().remove();																				
		// reset the next id at this point
		_nextId = 1;
		// reset the control numbers at this point
		_controlNumbers = {};
		// retain the page id
		var id = _page.id;
		// retain the page name
		var name = _page.name;
		// retain the page name
		var title = _page.title;
		// retain the page name
		var description = _page.description;
				
		// stringify control
		var controlString = JSON.stringify(control);
		
		// update all references of the page id to this page id
		controlString = controlString.replaceAll(control.id + "_",id + "_");
		
		// turned the replaced string back into an object
		var mappedControl = JSON.parse(controlString);
		
		// add back object from the current page
		mappedControl.object = _page.object;
		
		// reload the page control using the undo functionality (this preserves the control ids)
		_page = loadControl(mappedControl, null, true, false, true);

		// restore the id
		_page.id = id;
		// restore the name
		_page.name = name;
		// restore the title
		_page.title = title;
		// restore the description
		_page.description = description;
		// set the page object to the iframe body
		_page.object = $(_pageIframe[0].contentWindow.document.body);
		
		// apply any styling in the new control
		applyStyleForPaste(_page, getStyleSheet());
		
		// return the page
		return _page;		
		
	}
	
}

// JQuery is ready! 
$(document).ready( function() {
	
	//console.log("Jquery is ready!");
	
	// derive the scroll bar width first - http://stackoverflow.com/questions/986937/how-can-i-get-the-browsers-scrollbar-sizes
	var inner = document.createElement('p');
	inner.style.width = "100%";
	inner.style.height = "200px";

	var outer = document.createElement('div');
	outer.style.position = "absolute";
	outer.style.top = "0px";
	outer.style.left = "0px";
	outer.style.visibility = "hidden";
	outer.style.width = "200px";
	outer.style.height = "150px";
	outer.style.overflow = "hidden";
	outer.appendChild (inner);

	document.body.appendChild (outer);
	var w1 = inner.offsetWidth;
	outer.style.overflow = 'scroll';
	var w2 = inner.offsetWidth;
	if (w1 == w2) w2 = outer.clientWidth;

	document.body.removeChild(outer);

	_scrollBarWidth = (w1 - w2);
	
	// check if we have local storage
	if (typeof(localStorage) !== "undefined") {
		// retrieve device index from local storage
		var device = localStorage.getItem("_device");
		// if there was one and there's a slot for it in our devices
		if (device && device * 1 < _devices.length) _device = device*1;
		// retrieve the orientation
		var orientation = localStorage.getItem("_orientation");
		// update global if we got one
		if (orientation) _orientation = orientation;
		// retrieve the zoom
		var zoom = localStorage.getItem("_zoom");
		// update global if we got one
		if (zoom) _zoom = zoom*1;
		// calculate the scale
		_scale = _ppi / _devices[_device].ppi * _zoom;
	}
	
	// check for unsaved page changes if we move away
	$(window).on('beforeunload', function(){
		  if (_dirty) return 'You have unsaved changes.';
	});
	
		
	// the iframe in which we load the page
	_pageIframe = $("#page");
	// the div that covers all of the components in design mode so they don't react to clicks
	_designCover = $("#designCover");
	// attach a call to the window resize function to the window resize event listener
	$(window).resize("windowResize",windowResize);
	
	// load the action classes
	$.ajax({
    	url: "designer?action=getActions",
    	type: "GET",
    	contentType: "application/json",
        dataType: "json",       
        data: null,            
        error: function(server, status, error) {
        	// just show an error        	
        	alert("Error loading actions : " + error); 
        },
        success: function(actions) {         	        	
	    	
        	// loop the actions we got back
	    	for (var i in actions) {
	    		// get a reference to a single action
	    		var a = actions[i];
	    		// create a new action class object/function (this is a closure)
	    		var f = new ActionClass(a);        		        		     			
				// assign the action class object/function globally
				window["ActionClass_" + a.type] = f; 	    		
	    	} // action loop
	    	
	    	// load the controls classes	
	    	$.ajax({
	        	url: "designer?action=getControls",
	        	type: "GET",
	        	contentType: "application/json",
	            dataType: "json",            
	            data: null,            
	            error: function(server, status, error) { 
	            	// just show an error
	            	alert("Error loading controls : " + error); 
	            },
	            success: function(controls) { 
	            	        			    	    			    	    	
	    	    	// loop the controls we got back
	    	    	for (var i in controls) {
	    	    		// get a reference to a single control
	    	    		var c = controls[i];
	    	    		// create a new control ControlClass object/function (this is a closure)
	    	    		var f = new ControlClass(c);        		        		     			
	    				// assign the control controlClass function function globally
	    				window["ControlClass_" + c.type] = f; 		    	    		
	    	    	}
	    	    	
	    	    	// now load the other apps
	    	    	loadApps();		    	
	            			            	
	            } // success function
	            
	        }); // load controls ajax
	    	
        } // load actions success function
        
    }); // load actions ajax
	
	// when we load an app the iframe is refreshed with the resources for that app and page
	_pageIframe.load( function () {										
		
		// only if the app and page id's have been set
		if (_app.id && _page.id) {
			
			// now load the page definition with ajax
			$.ajax({
		    	url: "designer?action=getPage&a=" + _app.id + "&p=" + _page.id,
		    	type: "GET",
		    	contentType: "application/json",
		        dataType: "json",            
		        data: null,            
		        error: function(server, status, error) { 
		        	// ensure the designer is visble
		        	showDesigner();
		        	// if it's an authentication thing
		        	if (server && server.status == 403) {
		        		// reload the page from the top
		        		location.reload(true);
		        	} else {
		        		// show an error
		        		alert("Error loading page : " + error);
		        	}
		        },
		        success: function(page) {       
		        	
		        	try {
		        		
		        		// get the page (control) object
			        	_page = new Control(ControlClass_page, null, page, true);
			        	
			        	// retain the iframe body element as the page object
			    		_page.object = $(_pageIframe[0].contentWindow.document.body);
			    		
			    		// hide it
			    		_page.object.hide();
			    		
			    		// find the header section
			    		var head = $(_pageIframe[0].contentWindow.document).find("head");
			    		
			    		// remove any current app or page style sheets
			    		head.find("link[rel=stylesheet][href^=applications\\/" + _app.id + "]").remove();
			    		// make sure the app styling is correct (this can go wrong if the back button was clicked which loads the current page but the previous styling)
			    		head.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"applications/" + _app.id + "/rapid.css\">");
			    		// make sure the page styling is correct (this can go wrong if the back button was clicked which loads the current page but the previous styling)
			    		head.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"applications/" + _app.id + "/" + _page.name + ".css\">");
	     	
			        	// if we have childControls
			        	if (page.controls) {
				        	// loop the page childControls and create
				        	for (var i = 0; i < page.controls.length; i++) {
				        		// get an instance of the control properties (which is what we really need from the JSON)
				        		var control = page.controls[i];
				        		// create and add
				        		_page.childControls.push(loadControl(control, _page, true));
				        	}				        					        	
			        	}
			        	
			        	// show it after a pause to allow the new style sheets to apply
			        	window.setTimeout( function() {
			        		_page.object.show();
		            	}, 200);
			    					        	
			        	// make everything visible
			        	showDesigner();
			        	
			        	// refresh the page map
			        	showPageMap();
			        	
			        	// get the page lock object
			        	var lock = _page.lock;
			        	
			        	// if there is a lock and the userName is different (see how the userName is set at the top of the design.jsp)
			        	if (lock && lock.userName != _userName) {
			        				        		
			        		// set that this page is locked
			        		_locked = true;
			        		
			        		// hide / disable certain features
			        		$("#pageLock").show().children().first().html("This page is locked for editing by " + lock.userDescription);	
			        		$("#pageSave").attr("disabled","disabled");
			        		$("#controlControls").hide();			        		
	        		
			        		// show alert
			        		alert("This page was locked for editing by " + lock.userDescription + " at " + lock.formattedDateTime + ".\nYou will not be able to make or save changes to this page until they start work on a different page, or an hour has passed.");
			        					        					        		
			        	} else {
			        		
			        		// no lock make sure all functionality is present
			        		_locked = false;
			        		// show / enable features
			        		$("#pageLock").hide();
			        		$("#pageSave").removeAttr("disabled");
			        		$("#controlControls").show();
			        	}
			        	
			        	
		        	} catch (ex) {
		        		
		        		// ensure the designer is visible
			        	showDesigner();
		        		// show an error
		        		alert("Error loading page : " + ex);	
		        		
		        	}
		       		        		        		        			        			        		
		        } // success function
		        
			}); // ajax
		}
		
		// remove all children of the dialogues element as for some reason on a back button press they can appear here
		$("#dialogues").children().remove();
				
	});
					
	// the div which we use a border around the selected object
	_selectionBorder = $("#selectionBorder");
	// the div which we cover the selected object with whilst we are moving it around
	_selectionCover = $("#selectionCover");
	// the div which we place to show where a move/insert would occur on the left
	_selectionMoveLeft = $("#selectionMoveLeft");
	// the div which we place to show where an move/insert would occur on the right
	_selectionMoveRight = $("#selectionMoveRight");
	// the div which we place to show where an insert would occur
	_selectionInsert = $("#selectionInsert");
	
	// the div into which all the styles go
	_stylesPanelDiv = $("#stylesPanelDiv");
	// the editiable div in which we input the style rule name or value
	_styleInput = $("#styleInput");
	// a span that hints at what would be selected
	_styleHint = $("#styleHint");
	// a list of matching style rules
	_styleList = $("#styleList");
	
	// panel pin
	$("#controlPanelPin").click( function(ev) {
		// check pinned
		if (_panelPinned) {
			_panelPinned = false;
			_panelPinnedOffset = 0;
			$("#controlPanelPin").html("<img src='images/unpinned_14x14.png' title='pin'>");
			windowResize("pin");
			selectControl(_selectedControl);
			$("#controlPanel").hide("slide", {direction: "left"}, 200);
		} else {
			_panelPinned = true;
			_panelPinnedOffset = 221;
			$("#controlPanelPin").html("<img src='images/pinned_14x14.png' title='unpin'>");
			windowResize("unpin");
			selectControl(_selectedControl);
		}
	});
	
	// panel slide out
	$("#controlPanelShow").mouseenter( function(ev){
		// show the panel if we're not moving a control
		if (!_movingControl) showControlPanel();
	});
	
	// panel slide in
	$("#controlPanel").mouseleave( function(ev){		
		// slide the control panel back in
		if (!_panelPinned && !$(ev.target).is("select")) $("#controlPanel").hide("slide", {direction: "left"}, 200);		
	});

	// if we click on the cover (have we hit a control)
	$("#designCover").mousedown( function(ev) {				
		// remember that the mouse is down
		_mouseDown = true;
		// hide the control panel (sometimes it's mouseout isn't hit)
		if (!_panelPinned) $("#controlPanel").hide("slide", {direction: "left"}, 200);
		// get the control under the mouse X/Y
		var c = getMouseControl(ev);
		// if we got one
		if (c) {
			// retain reference to the selected object
			_selectedControl = c;									
			// stop mousemove if canMove is not a property of the control class
			if (!_selectedControl._class.canUserMove) _mouseDown = false;
			// fire the mousedown event for the object
			c.object.trigger("mousedown", ev);			
			// select control
			selectControl(c);
			// calculate the mouse offsets, for moving later, note the adding page of the pinned Offset as we removed it when calling the object event
			_mouseDownXOffset = _selectedControl.object.offset().left - ev.pageX;
			_mouseDownYOffset = _selectedControl.object.offset().top - ev.pageY;
		} else { 
			// not got an object if there are no child controls on the page select it, otherwise clear everything
			if (_page.childControls && _page.childControls.length == 0 && !$("#propertiesPanel").is(":visible") ) {
				// select the page
				selectControl(_page);
			} else {
				// select a null control (this does a lot of cleanup)
				selectControl(null);				
			}
			// hide the select border
			_selectionBorder.hide();			
		}				
	}); // cover mouseDown	
	
	// administration
	$("#appAdmin").click( function(ev) {
		if (_app && _app.id) {
			window.location = "~?a=rapid&appId=" + _app.id;
		} else {
			window.location = "~?a=rapid";
		}		 
	});
	
	// load app
	$("#appSelect").change( function() {
    	// load the selected app and its pages in the drop down 
    	if (checkDirty()) {
    		loadApp();
    	} else {
    		// revert the drop down on cancel
    		$("#appSelect").val(_app.id);
    	}
	});
			
	// load page
	$("#pageSelect").change( function() {
		// load the selected page
		if (checkDirty()) {
			loadPage();
		} else {
			// revert the drop down on cancel
			$("#pageSelect").val(_page.id);
		}
	});
	
	// new page
	$("#pageNew").click( function(ev) {
		showDialogue('~?action=page&a=rapid&p=P3'); 
	});
	
	// edit page
	$("#pageEdit").click( function(ev) {
		// hide any selection border
		_selectionBorder.hide();
		// set the selected control to the page
		selectControl(_page);		
	});
	
	// undo
	$("#undo").click( function(ev) {
		doUndo(); 
		showPageMap();
	});
	
	// redo
	$("#redo").click( function(ev) {
		doRedo();
		showPageMap();
	});
		
	// save page
	$("#pageSave").click( function() {
		// show the saving page dialogue
		showDialogue('~?action=page&a=rapid&p=P11');
		// show message
		$("#rapid_P11_C7_").html("Saving page");
		// send the data to the backend
		$.ajax({
	    	url: "designer?action=savePage&a=" + _app.id,
	    	type: "POST",
	    	contentType: "application/json",
	        dataType: "json",            
	        data: getSavePageData(),            
	        error: function(server, status, error) { 
	        	// show error
	        	$("#rapid_P11_C7_").html(error);
	        	// enable close button
	        	$("#rapid_P11_C10_").enable();
	        },
	        success: function(controls) {
	        	// show message
	        	$("#rapid_P11_C7_").html("Page saved!");
	        	// enable close button
	        	$("#rapid_P11_C10_").enable();
	        	// set dirty to false
	        	_dirty = false;
	        	// reload the pages as the order may have changed, but keep the current one selected
	        	loadPages(_page.id);
	        	// arrange any non-visible controls
	        	arrangeNonVisibleControls();	        		        	
	        }
		});
	});
	
	// view page
	$("#pageView").click( function(ev) {
		// prompet the user if the page is dirty
		if (checkDirty()) window.location = "~?a=" + _app.id + "&p=" + _page.id;
	});
							
	// select parent
	$("#selectParent").click( function(ev) {
		// if we have a parentControl
		if (_selectedControl.parentControl) {			 
			// select the parent
			selectControl(_selectedControl.parentControl);
		}
	});
	
	// select child
	$("#selectChild").click( function(ev) {
		// maker sure there is a childControl to go to
		if (_selectedControl.childControls.length > 0) {
			// select the first visible child
			for (var i in _selectedControl.childControls) {
				if (_selectedControl.childControls[i].object.is(":visible") || _selectedControl.childControls[i].type == "custom") {
					selectControl(_selectedControl.childControls[i]);
					break;
				}
			}			
		}
	});
		
	// select left peer
	$("#selectPeerLeft").click( function(ev) {
		// maker sure there we've not got the left most control already
		if (_selectedControl != _selectedControl.parentControl.childControls[0]) {
			// find our position
			for (var i in _selectedControl.parentControl.childControls) {
				if (_selectedControl == _selectedControl.parentControl.childControls[i]) break;
			}
			// run any control selection code - for complex controls that may need to update their parent
			if (_selectedControl._selectLeft) _selectedControl._selectLeft();
			// select the childControl before this one 
			selectControl(_selectedControl.parentControl.childControls[i*1-1]);
		}
	});
	
	// select right peer
	$("#selectPeerRight").click( function(ev) {
		// maker sure there we've not got the right most control already
		if (_selectedControl != _selectedControl.parentControl.childControls[_selectedControl.parentControl.childControls.length - 1]) {
			// find our position
			for (var i in _selectedControl.parentControl.childControls) {
				if (_selectedControl == _selectedControl.parentControl.childControls[i]) break;
			}
			// run any control selection code - for complex controls that may need to update their parent
			if (_selectedControl._selectRight) _selectedControl._selectRight();
			// select the childControl before this one if it's visible
			selectControl(_selectedControl.parentControl.childControls[i*1+1]);
		}
	});
			
	// swap peer left
	$("#swapPeerLeft").click( function(ev) {
		// maker sure there we've not got the left most control already
		if (_selectedControl != _selectedControl.parentControl.childControls[0]) {
			// add an undo snapshot
			addUndo();
			// find our position
			for (var i in _selectedControl.parentControl.childControls) {
				if (_selectedControl == _selectedControl.parentControl.childControls[i]) break;
			}
			// remove control from parent childControls
			_selectedControl.parentControl.childControls.splice(i,1);
			// add back one position earlier
			_selectedControl.parentControl.childControls.splice(i*1-1,0,_selectedControl);
			// check if there is a routine for the swap
			if (_selectedControl._swapLeft) {
				// run the function
				_selectedControl._swapLeft();
			} else {				
				// move object
				_selectedControl.object.insertBefore(_selectedControl.parentControl.childControls[i*1].object);
			}
			// arrange any non visible controls
			arrangeNonVisibleControls();
			// re-select the control
			selectControl(_selectedControl);
			// rebuild the page map
			showPageMap();
		}
	});
	
	// swap peer right
	$("#swapPeerRight").click( function(ev) {
		// maker sure there we've not got the right most control already
		if (_selectedControl != _selectedControl.parentControl.childControls[_selectedControl.parentControl.childControls.length - 1]) {
			// add an undo snapshot
			addUndo();
			// find our position
			for (var i in _selectedControl.parentControl.childControls) {
				if (_selectedControl == _selectedControl.parentControl.childControls[i]) break;
			}
			// remove control from parent childControls
			_selectedControl.parentControl.childControls.splice(i,1);
			// add back one position later
			_selectedControl.parentControl.childControls.splice(i*1+1,0,_selectedControl);
			// check if there is a routine for the swap
			if (_selectedControl._swapRight) {
				// run the function
				_selectedControl._swapRight();
			} else {
				// just move the object
				_selectedControl.object.insertAfter(_selectedControl.parentControl.childControls[i*1].object);
			}			
			// arrange any non visible controls
			arrangeNonVisibleControls();
			// re-select the control
			selectControl(_selectedControl);
			// rebuild the page map
			showPageMap();
		}
	});
	
	// add peer left
	$("#addPeerLeft").click( function(ev) {
		// check whether adding of peers is allowed
		if (_selectedControl._class.canUserAddPeers) {
			// add an undo snapshot
			addUndo();
			// create a new control of the selected class
			var newControl = new Control(_selectedControl._class, _selectedControl.parentControl, null, true);						
			// run any control insertion code - for complex controls that may need to update their parent
			if (newControl._insertLeft) {
				newControl._insertLeft();
			} else {
				// add it to the parent in the correct position
				_selectedControl.parentControl.childControls.splice(_selectedControl.object.index(), 0, newControl);	
				// move the object
				newControl.object.insertBefore(_selectedControl.object);							
			}			
			// select the new one
			selectControl(newControl);
			// rebuild the page map
			showPageMap();
		}
		
	});
	
	// add peer right
	$("#addPeerRight").click( function(ev) {
		// check whether adding of peers is allowed
		if (_selectedControl._class.canUserAddPeers) {
			// add an undo snapshot
			addUndo();
			// create a new control of the selected class
			var newControl = new Control(_selectedControl._class, _selectedControl.parentControl, null, true);					
			// run any control insertion code - for complex controls that may need to update their parent
			if (newControl._insertRight) {
				newControl._insertRight();
			} else {
				// add it to the parent in the correct position
				_selectedControl.parentControl.childControls.splice(_selectedControl.object.index() + 1, 0, newControl);
				// move the object
				newControl.object.insertAfter(_selectedControl.object);				
			}			
			// select the new one
			 selectControl(newControl);
			// rebuild the page map
			showPageMap();
		}
		
	});
	
	// delete control
	$("#deleteControl").click( function(ev) {
		// check this control isn't the page (shows dialogue if so)
		if (_selectedControl.parentControl) {
			var contCount = 0;
			// count the controls of this type
			for (var i in _selectedControl.parentControl.childControls) {
				if (_selectedControl._class.type == _selectedControl.parentControl.childControls[i]._class.type) contCount ++;
			}
			// can delete if no parent class (page control), can insert into parent, or canUserAddPeers and more than 1 peer of this type
			if (!_selectedControl.parentControl._class || _selectedControl.parentControl._class.canUserInsert || (_selectedControl._class.canUserAddPeers && contCount > 1)) {				
				// add an undo snapshot
				addUndo();
				// call the remove routine
				_selectedControl._remove();
				// find our position
				for (var i in _selectedControl.parentControl.childControls) {
					if (_selectedControl == _selectedControl.parentControl.childControls[i]) break;
				}
				// remove from parents child controls
				_selectedControl.parentControl.childControls.splice(i,1);
				// hide the selection and properties panel
				selectControl(null);
				// if no controls remain reset the nextid and control numbers
				if (_page.childControls.length == 0) {
					_nextId = 1;
					_controlNumbers = {};
				}
				// arrange the non visible page controls
				arrangeNonVisibleControls();
				// rebuild the page map
				showPageMap();
			}			
		} else {
			showDialogue('~?a=rapid&p=P4');
		}
		 		
	});
	
	// copy
	$("#copy").click( function(ev) {
		// if there is a selected control
		if (_selectedControl) {
			// treat the page differently
			if (_selectedControl.parentControl) {
				_copyControl = _selectedControl;
			} else {
				_copyControl = cleanControlForPaste(_selectedControl);
			}
		}
		if (_copyControl) $("#paste").removeAttr("disabled");
	});
	
	// paste
	$("#paste").click( function(ev) {
		// see the enable/disable rules for the past button to see all the rules but basically we're working out whether we can insert into the selected control, into the parent, or not at all
		if (_selectedControl && _copyControl) {
			// add an undo snapshot
			addUndo();
			// if no parent it's the page
			if (_selectedControl.parentControl) {
				// find out if there are childControls with the same type with canUserAddPeers
				var childCanAddPeers = false;
				for (i in _selectedControl.childControls) {
					if (_copyControl.type == _selectedControl.childControls[i]._class.type && _selectedControl.childControls[i]._class.canUserAddPeers) {
						childCanAddPeers = true;
						break;
					}
				}
				// find out if there are peers with the same type with canUserAddPeers
				var peerCanAddPeers = false;
				for (i in _selectedControl.parentControl.childControls) {
					if (_copyControl.type == _selectedControl.parentControl.childControls[i]._class.type && _selectedControl.parentControl.childControls[i]._class.canUserAddPeers) {
						peerCanAddPeers = true;
						break;
					}
				}
				// can we do an insert, or add as a peer
				if (_selectedControl._class.canUserInsert && (_copyControl._class.canUserAdd || childCanAddPeers)) {
					// create the new control and place in child collection of current parent
					var newControl = doPaste(_copyControl, _selectedControl);
					// add to childControl collection of current parent
					_selectedControl.childControls.push(newControl);
					// move the html to the right place
					_selectedControl.object.append(newControl.object);
				} else if (_copyControl._class.canUserAdd || peerCanAddPeers) {
					// create the new control as peer of current selection
					var newControl = doPaste(_copyControl, _selectedControl.parentControl);
					// use the insert right routine if we've got one
					if (newControl._insertRight) {
						newControl._insertRight();
					} else {						
						// move the object (if the parent isn't the page)
						if (_selectedControl.parentControl.parentControl) newControl.object.insertAfter(_selectedControl.object);
						// add it to the parent at the correct position
						 _selectedControl.parentControl.childControls.splice(_selectedControl.object.index()+1,0,newControl);
					}
					// select the new one
					selectControl(newControl);				
				}					
				
			} else {
								
				if (_copyControl.parentControl && _copyControl._class.canUserAdd) {
					// create the new control and place in child collection of current parent
					var newControl = doPaste(_copyControl, _selectedControl);
					// add to childControl collection of current parent (which is the page)
					_selectedControl.childControls.push(newControl);
					// select the new control
					selectControl(newControl);
				} else {
					// create the new page control with paste
					var newControl = doPaste(_copyControl);
					// select the new control
					selectControl(newControl);
				} // page copy check
				
			} // page paste check
			
			// rebuild the page map
			showPageMap();
					
		}		
	});		
							
});

// stop controls being inserted into their pass children for example pass a table cell and a table
function isDecendant(control1, control2) {	
	var result = false;
	if (control1 === control2) {
		result = true;
	} else {
		if (control1.childControls) {
			for (var i in control1.childControls) {
				var c = control1.childControls[i];
				result = isDecendant(c, control2);
				if (result) break;
			}
		}	
	}		
	return result;
}

//if the mouse moves anywhere
$(document).mousemove( function(ev) {
	// get a reference to the control
	var c = getMouseControl(ev);
	// if a control is selected and the mouse is down look for the controls new destination
	if (_selectedControl) {
		
		// check the mouse is down (and the selected control has an object)
		if (_mouseDown && _selectedControl.object[0]) {		
		
			// if we have just started moving position the cover
			if (!_movingControl) {
				
				// position the cover
				_selectionCover.css({
					"width":_selectedControl.object.outerWidth() * _scale, 
					"height":_selectedControl.object.outerHeight() * _scale, 
					"left":_selectedControl.object.offset().left * _scale + _panelPinnedOffset, 	
					"top":_selectedControl.object.offset().top * _scale
				});
				
				if (_selectedControl.object.is(":visible")) {
					// show it if selected object visible
					_selectionCover.show();				
					// show the insert
					_selectionInsert.show();
				}			
				
				// hide the properties - this can cause the properties panel to bounce
				$("#propertiesPanel").hide("slide", {direction: "right"}, 200);
				
				// remember we are now moving an object
				_movingControl = true;
			}
			
			// position the selection border
			positionBorder(ev.pageX, ev.pageY);
				
			// if we got a control and it's allowed to be moved by the user (non-visual controls can be added but not moved so this way they remain with their parent control as the page)
			if (c && _selectedControl._class.canUserMove) {
				// retain a reference to the movedoverObject
				_movedoverControl = c;			
				// if over the selected object or a descendant don't show anything
				if (_movedoverControl === _selectedControl || isDecendant(_selectedControl,_movedoverControl)) {
					_selectionInsert.hide();
					_selectionMoveLeft.hide();
					_selectionMoveRight.hide();
				} else {			
					// calculate a move threshold which is the number of pixels to the left or right of the object the users needs to be within
					var moveThreshold = Math.min(50,_movedoverControl.object.outerWidth()/3);
					// if it's not possible to insert make the move thresholds half the width to cover the full object
					if (!_movedoverControl._class.canUserInsert) moveThreshold = _movedoverControl.object.outerWidth()/2;
					// are we within the move threshold on the left or the right controls that can be moved, or in the middle with an addChildControl method?
					if (_movedoverControl._class.canUserMove && ev.pageX - _panelPinnedOffset < _movedoverControl.object.offset().left + moveThreshold) {
						// position the insert left
						_selectionMoveLeft.css({
							"display": "block",
							"left": _panelPinnedOffset + _movedoverControl.object.offset().left,	
							"top": ev.pageY - _selectionInsert.outerHeight()/2
						});
						// remember it's on the left
						_movedoverDirection = "L";
						// make sure the other selections are hidden					
						_selectionMoveRight.hide();
						_selectionInsert.hide();
					} else if (_movedoverControl._class.canUserMove && ev.pageX - _panelPinnedOffset > _movedoverControl.object.offset().left + _movedoverControl.object.outerWidth() - moveThreshold) {
						// position the insert right
						_selectionMoveRight.css({
							"display": "block",
							"left": _panelPinnedOffset + _movedoverControl.object.offset().left + _movedoverControl.object.outerWidth() - _selectionMoveRight.outerWidth(),	
							"top":ev.pageY - _selectionInsert.outerHeight()/2
						});
						// remember it's on the right
						_movedoverDirection = "R";
						// make sure the other selections are hidden						
						_selectionMoveLeft.hide();
						_selectionInsert.hide();
					} else if (_movedoverControl._class.canUserInsert) {
						// position the insert in the middle
						_selectionInsert.css({
							"display": "block",
							"left": _panelPinnedOffset + _movedoverControl.object.offset().left + (_movedoverControl.object.outerWidth() - _selectionInsert.outerWidth())/2,	
							"top":ev.pageY - _selectionInsert.outerHeight()
						});
						// remember it's in the the centre
						_movedoverDirection = "C";
						// make sure the other selections are hidden					
						_selectionMoveLeft.hide();
						_selectionMoveRight.hide();
					}									
				}
			} // if over object		
		} // if mouse down
	}; // if selectedObject
	
	// if the current mousedOver object is different from the last one
	if (c != _mousedOverControl) {
		// if there actuall is a last one fire it's mouseout event
		if (_mousedOverControl) _mousedOverControl.object.mouseout();
	}
	// fire the mouseover event for any hit control object
	if (c) {
		_mousedOverControl = c;
		c.object.mouseover();
	} else {
		_mousedOverControl = null;
	}
	
}); // mousemove

// if the mouse is upped anywhere
$(document).mouseup( function(ev) {
	
	_mouseDown = false;
	_mouseDownXOffset = 0;
	_mouseDownYOffset = 0;
	_reorderDetails = null;
	
	if (_selectedControl && _selectedControl.object[0]) {		
		// show it in case it was an add
		if (_selectedControl._class.canUserMove) _selectedControl.object.show();
		// if we were moving a control different from the _selectedControl
		if (_movingControl && _movedoverControl && _movedoverDirection && _movedoverControl.object[0] !== _selectedControl.object[0]) {
			// add an undo snapshot
			addUndo();
			// remove the object from it's current parent
			removeControlFromParent(_selectedControl);
			// move the selectedObject to the left or right of the movedoverObject, or insert if in the centre
			switch (_movedoverDirection) {
			case "L" : 
				// retain the same parent control as the moved over control
				_selectedControl.parentControl = _movedoverControl.parentControl;
				// move the markup object before the moved over object
				_selectedControl.object.insertBefore(_movedoverControl.object);
				// add to childControls at correct position
				_movedoverControl.parentControl.childControls.splice(_movedoverControl.object.index(),0,_selectedControl);				
				break;		
			case "R" :
				// retain the same parent control as the moved over control
				_selectedControl.parentControl = _movedoverControl.parentControl;
				// move the markup object after the moved over object
				_selectedControl.object.insertAfter(_movedoverControl.object);
				// add to childControls at correct position
				_movedoverControl.parentControl.childControls.splice(_movedoverControl.object.index()+1,0,_selectedControl);								
				break;	
			case "C" :
				// assign the correct parent
				_selectedControl.parentControl = _movedoverControl;
				// add the selected control into the moved over control  
				_movedoverControl.childControls.push(_selectedControl);				
				// move the object into the right place
				_movedoverControl.object.append(_selectedControl.object);
				break;
			}				
			// rebuild the page map
			showPageMap();
		}
		
		// remember we have only selected (no longer moving)
		selectedState = 1;
		// null the moveedoverObject
		_movedoverControl = null;
		// arrange the non-visible controls if our selected control looks like one
		if (!_selectedControl._class.canUserMove) arrangeNonVisibleControls();
		// hide the cover
		_selectionCover.hide();
		// hide the insert/moves
		_selectionInsert.hide();
		_selectionMoveLeft.hide();
		_selectionMoveRight.hide();
		// position the selection border			
		_selectionBorder.css({
			"left": _selectedControl.object.offset().left - 8 , // padding + border + 1 pixel	
			"top": _selectedControl.object.offset().top - 8 // padding + border + 1 pixel
		});		
		positionBorder(_selectedControl.object.offset().left, _selectedControl.object.offset().top);
		// size the border in case moving it has changed it's geometery
		sizeBorder(_selectedControl);		
		// show the properties panel			
		showPropertiesPanel();
		// show a normal cursor
		$("body").css("cursor","");
		// allow text selection in the document again
		$("body").css({
			"-webkit-touch-callout":"",
			"-webkit-user-select":"",
			"-khtml-user-select":"",
			"-moz-user-select":"",
			"-ms-user-select":"",
			"user-select":""
		});
	}; // if selectedObject
	_movingControl = false;	
}); // mouseup

// called whenever a control is added or deleted in case one was a non-visible control and needs rearranging
function arrangeNonVisibleControls() {	
	// check there is a page and a page object
	if (_page && _page.object) {
		// get existing page controls	         
		var pageControls = $("body").children(".nonVisibleControl");
		// remember the last x
		var x = _panelPinnedOffset + 10;
		// loop each one
		pageControls.each( function(i) {
			// get a reference to this object
			var o = $(this);		
			// ensure this control is visible
			o.show();
			// ensure this page control is in the right place
			o.css("left",x);
			// get the width
			var w = Math.max(o.outerWidth(),25);
			// add to the growing x value
			x += (5 + w);
		});
	}	
}

//this makes sure the control panel is visible and tall enough for all properties
function showControlPanel() {
	
	$("#controlPanel").show("slide", {direction: "left"}, 200);
		
}

// this makes sure the properties panel is visible and tall enough for all properties
function showPropertiesPanel() {
	
	// resize the window to accomodate the panel
	windowResize("showPropertiesPanel");
	
	// set all controls to readonly if the page is locked
	if (_locked) {		
		// get the properties panel to include actions and styles
		var propertiesPanel = $("#propertiesPanel");
		// get the dialogues
		var propertiesDialogues = $("#propertiesDialogues");
		// disable inputs
		propertiesPanel.find("input").attr("disabled","disabled");
		propertiesDialogues.find("input").attr("disabled","disabled");
		// disable drop downs
		propertiesPanel.find("select").attr("disabled","disabled");
		propertiesDialogues.find("select").attr("disabled","disabled");
		// readonly textareas
		propertiesPanel.find("textarea").attr("readonly","readonly");
		propertiesDialogues.find("textarea").attr("readonly","readonly");
		// disable deletes
		propertiesPanel.find("img").off("click");
		propertiesDialogues.find("img").off("click");	
		// disable order moves
		propertiesPanel.find("img").off("mousedown");
		propertiesDialogues.find("img").off("mousedown");
	}
	
	// show the panel
	$("#propertiesPanel").show("slide", {direction: "right"}, 200);
				
}

// gets the height of an object including any children that may have absoloute positioning
function getAbsoluteHeight(o) {
	// assume height is 0
	var height = 0;
	// get the children
	var childElements = o.children();
	// loop them
	for (var i = 0; i < childElements.length; i++) {
		// get the child
		var c = $(childElements[i]);
		// if position absoloute
		if (c.css("position") == "absolute") {
			// get the height from the child's children
			height += getAbsoluteHeight(c);
		} else {
			// add the height normally
			height += c.outerHeight(true);
		}
	}	
	// return the greatest of calculated and reported height	
	return Math.max(height, o.outerHeight(true));
}

// called whenever the page is resized
function windowResize(ev) {
	
	// get the caller of this function
	var caller = ev.data || ev;
	
	// get the window object
	var win = $(window);
	// get the window width
	var width = win.width();
	// get the window height
	var height = win.height();
				
	// get the control panel
	var controlPanel = $("#controlPanel");		
	// set it's height to auto
	controlPanel.css("height","auto");
	
	// get the properties panel
	var propertiesPanel = $("#propertiesPanel");
	// set it's height to auto
	propertiesPanel.css("height","auto");
			
	// reset the page iFrame height so non-visual controls aren't too far down the page
	_pageIframe.css("height","auto");
	// set the iFrame height by it's contents
	var iframeHeight = getAbsoluteHeight($(_pageIframe[0].contentWindow.document.body));
		
	// get its current height (less the combined top and bottom padding)
	var controlPanelHeight = controlPanel.outerHeight(true);
		
	// get its current height
	var propertiesPanelHeight = propertiesPanel.outerHeight(true);
			
	// log
	//console.log("caller = " + caller + ", window = " + height + ", control panel = " + controlPanelHeight + ", properties panel = " + propertiesPanelHeight + ", iframe = " + iframeHeight);
		
	// increase height to the tallest of the window, the panels, or the iFrame
	height = Math.max(height, controlPanelHeight, propertiesPanelHeight, iframeHeight);
	
	// adjust controlPanel height, less it's padding
	controlPanel.css({height: height - 20});
	
	// adjust propertiesPanel height, less it's padding
	propertiesPanel.css({height: height - 20});
	
	// get the device
	var device = _devices[_device];
			
	// if the device has a height scale and apply
	if (device.height) {
		// assume the width and heigth from the device
		var width = device.width;
		var height = device.height;
		// if landscape swap width and height
		if (_orientation == "L") {
			width =  device.height;
			height = device.width;
		} 
		// apply it
		_pageIframe.css({
			left: _panelPinnedOffset,
			width: _scale * width,
			height: _scale * height,
			"border-right": "1px solid black",
			"border-bottom": "1px solid black"
		});		
		
	} else {
		// adjust iframe position, width and height
		_pageIframe.css({
			left: _panelPinnedOffset,
			width: width - _panelPinnedOffset,
			height: height,
			"border-right": "0px",
			"border-bottom": "0px"
		});
	}
	// adjust the scale
	_page.object.css("transform","scale(" + _scale * device.scale + ")");
	// remove it if we can
	if (_scale * device.scale == 1) _page.object.css("transform", "none");
				
}

function fileuploaded(fileuploadframe) {

    var f = window.frames["uploadIFrame"];
    if (!f) f = document.getElementById("uploadIFrame").contentDocument;

    var r = $(f.document.body).text();

    if ((r) && (r != "")) {
    	
    	if (r.indexOf("Error") == 0) {
    		
    		alert(r);
    		
    	} else {
    		
    		var response = JSON.parse(r);
        	
        	switch (response.type) {
        	case "uploadImage" :
        		// update file property in control
        		_selectedControl.file = response.file;
        		// init the ap images array if need be
        		if (!_app.images) _app.images = [];
        		// add to array
        		_app.images.push(response.file);
        		// rebuild html
        		rebuildHtml(_selectedControl);        		
        	}
    		
    	}
    	
    }

}
