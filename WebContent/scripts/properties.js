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

Functions related to control properties

*/

// an id to unqiuely identify each property dialogue as we create them
var _dialogueId = 0;
// this holds all the property listeners by dialogue id so they can be properly detached
var _dialogueListeners = {};

// this finds the dialogue each listener is in and stores it so the relevant ones can be detached when the dialogue is closed
function addListener(listener) {
	// get the listener dialogue
	var dialogue = listener.closest("div[data-dialogueId]");
	// get the id
	dialogueId = dialogue.attr("data-dialogueId");
	// instantiate array if need be
	if (!_dialogueListeners[dialogueId]) _dialogueListeners[dialogueId] = [];
	// add the listener
	_dialogueListeners[dialogueId].push(listener);
}

function removeListeners(dialogueId) {
	// if we were given a dialogueId
	if (dialogueId) {
		// loop all the listeners
		for (var i in _dialogueListeners[dialogueId]) {
			// remove all dialogues
			_dialogueListeners[dialogueId][i].unbind();
		}
		// remove the dialogue
		delete _dialogueListeners[dialogueId];
	} else {
		// loop all the dialogues
		for (var i in _dialogueListeners) {
			// loop all the listeners
			for (var j in _dialogueListeners[i]) {
				// remove all dialogues
				_dialogueListeners[i][j].unbind();
			}
			// remove the dialogue
			delete _dialogueListeners[i];
		}
		// reset the dialogueId
		_dialogueId = 0;
	}
}

// this renders all the control properties in the properties panel
function showProperties(control) {
			
	// remove all listeners for this panel
	removeListeners("propertiesPanel");
		
	// grab a reference to the properties div
	var propertiesPanel = $(".propertiesPanelDiv");
	// set the parent height to auto
	propertiesPanel.parent().css("height","auto");
	
	// if there was a control
	if (control) {
		
		// get the contro class
		var controlClass = _controlTypes[control.type];
	
		// write the properties heading
		propertiesPanel.html("<h2>Properties<img id='helpProperties' class='headerHelp' src='images/help_16x16.png' /></h2>");
		// add the help hint
		addHelp("helpProperties",true);
		// append a table
		propertiesPanel.append("<table class='propertiesPanelTable'><tbody></tbody></table>");		
		// get a reference to the table
		var propertiesTable = propertiesPanel.children().last().children().last();
		// add the properties header
		propertiesTable.append("<tr><td colspan='2'><h3>" + controlClass.name + "</h3></td></tr>");
		// add a small break
		propertiesTable.append("<tr><td colspan='2'></td></tr>");
		// show the control id if requested
		if (_version.showControlIds) propertiesTable.append("<tr><td>ID</td><td class='canSelect'>" + control.id + "</td></tr>");
		// check there are class properties
		var properties = controlClass.properties;
		if (properties) {
			// (if a single it's a class not an array due to JSON class conversionf from xml)
			if ($.isArray(properties.property)) properties = properties.property; 			
			// loop the class properties
			for (var i in properties) {
				// add a row
				propertiesTable.append("<tr></tr>");
				// get a reference to the row
				var propertiesRow = propertiesTable.children().last();
				// retrieve a property object from the control class
				var property = properties[i];
				// check that visibility is not false
				if (property.visible === undefined || !property.visible === false) {
					// get the property itself from the control
					propertiesRow.append("<td>" + property.name + "</td><td></td>");
					// get the cell the property update control is going in
					var cell = propertiesRow.children().last();
					// apply the property function if it starts like a function or look for a known Property_[type] function and call that
					if (property.changeValueJavaScript.trim().indexOf("function(") == 0) {
						try {
							var changeValueFunction = new Function(property.changeValueJavaScript);
							changeValueFunction.apply(this,[cell, control, property]);
						} catch (ex) {
							alert("Error - Couldn't apply changeValueJavaScript for " + control.name + "." + property.name + " " + ex);
						}
					} else {
						if (window["Property_" + property.changeValueJavaScript]) {
							window["Property_" + property.changeValueJavaScript](cell, control, property, true);
						} else {
							alert("Error - There is no known Property_" + property.changeValueJavaScript + " function");
						}
					}
				}			
			} // visible property
			
		} // got properties
		
	} // got control
		
}

function updateProperty(propertyObject, property, value, refreshHtml) {
	// if the page isn't locked
	if (!_locked) {
		// add an undo snapshot
		addUndo();
		// update the object property value
		propertyObject[property.key] = value;
		// if an html refresh is requested
		if (refreshHtml) {
			// in controls.js
			rebuildHtml(propertyObject);			
		}	
		// if this is the name
		if (property.key == "name") {
			// if the property map is visible
			if ($("#pageMap").is(":visible")) {
				// get the control class
				var controlClass = _controlTypes[propertyObject.type];
				// update the name
				$("#pageMap").find("span[data-id=" + propertyObject.id + "]").html(((controlClass.image) ? "<img src='" + controlClass.image + "'/>" : "") + propertyObject.type + (propertyObject.name ? " - " + propertyObject.name: ""));
			}
		}
	}
}

function setPropertyVisibilty(propertyObject, propertyKey, visibile) {
	// get the class from controls
	var objectClass = _controlTypes[propertyObject.type];
	// try actions if not found
	if (!objectClass) objectClass = _actionTypes[propertyObject.type];
	
	if (propertyObject && objectClass && objectClass.properties) {
		var properties = objectClass.properties;
		// (if a single it's a class not an array due to JSON class conversionf from xml)
		if ($.isArray(properties.property)) properties = properties.property; 
		// loop them
		for (var i in properties) {
			var property = properties[i];
			if (property.key == propertyKey) {
				property.visible = visibile;
				break;
			}
		}
	}
}

// this is a reusable function for creating dialogue boxes
function createDialogue(cell, width, title) {	
	// increment the dialogue id counter
	_dialogueId ++;
	// create a dialogue if we need to
	var dialogue = $("#propertiesDialogues").append("<div data-dialogueId='" + _dialogueId + "' class='actionsPanelDiv dialogue' style='position:absolute;display:none;width:" + width + "px;z-index:10012;border:1px solid black;background-color:white;font-size:11px;padding:10px;'></div>").children().last();
	// add a close link
	var close = dialogue.append("<b style='float:left;margin-top:-5px;'>" + title + "</b><a href='#' style='float:right;margin-top:-5px;'>close</a></div>").children().last();
	// note that this is not in the listeners collection so it's retained between property updates
	addListener(close.click({dialogueId: _dialogueId}, function(ev) {
		// remove listeners for this dialogue
		removeListeners(ev.data.dialogueId);
		// hide this dialogue
		$(ev.target).closest("div.dialogue").remove();
		// update the properties 
		showProperties(_selectedControl);
		// update the events
		showEvents(_selectedControl);
		// update the screen layout
		windowResize("PropertyDialogue");
	}));	
	// listener to show the dialogue also retained between property updates
	addListener(cell.click( function(ev) { 
		dialogue.css({
			"left": cell.offset().left + cell.outerWidth() - dialogue.outerWidth() + 1, 
			"top": cell.offset().top			
		});
		// show this drop down
		dialogue.slideDown(500);			
	}));
	// add an options table
	dialogue.append("<br/><table style='width:100%' class='propertiesPanelTable dialogueTable'><tbody></tbody></table>");
	// return
	return dialogue;	
}

// this function clears down all of the property dialogues
function hideDialogues() {		
	// remove all listeners
	removeListeners();	
	// grab a reference to any dialogues
	var propertiesDialogues = $("#propertiesDialogues");
	// empty any propertyDialogues that we may have used before
	propertiesDialogues.children().remove();				
}

function Property_text(cell, propertyObject, property, refreshHtml) {
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key]) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) { updateProperty(propertyObject, property, ev.target.value, refreshHtml); }));
}

// this function will re-render the propertyObject's table, or dialogue (at some point in the future)
function refreshPropertyObject(ev, propertyObject) {
	// only if we got an event and it has refresh
	if (ev && ev.data.refreshProperties) {
		// get the target
		var target = $(ev.target);
		// refresh properties if appropriate
		if (target.closest(".propertiesPanelDiv")[0]) showProperties(_selectedControl);
		// refresh events and actions if appropriate
		if (target.closest(".actionsPanelDiv")[0]) showEvents(_selectedControl);		
	}		
}

function Property_integer(cell, propertyObject, property, refreshHtml) {
	var value = "";
	// set the value if it exists (or is 0)
	if (propertyObject[property.key] || parseInt(propertyObject[property.key]) == 0) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check integer match
		if (val.match(new RegExp("^\\d+$"))) {
			// update value
			updateProperty(propertyObject, property, ev.target.value, refreshHtml);						
		} else {
			// restore value
			input.val(propertyObject[property.key]);
		}
	}));
}

function Property_bigtext(cell, propertyObject, property, refreshHtml) {
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key]) value = propertyObject[property.key];
	// add the visible bit
	cell.text(value);
	// append and get a reference to the adjustable form control
	var textarea = $("#propertiesDialogues").append("<textarea style='position:absolute;display:none;height:300px;width:600px;z-index:10012' wrap='off'></textarea>").children().last();
	// add the text
	textarea.text(value);
	// add a listener to update the property
	addListener( cell.click( {textarea: textarea}, function(ev) { 
		textarea.css({
			"left": cell.offset().left + cell.outerWidth() - 605, 
			"top": cell.offset().top			
		});
		textarea.slideDown(500);
		// focus it so a click anywhere else fires the unfocus and hides the textbox
		textarea.focus();
	}));	
	// hide the textarea and update the cell on unfocus
	addListener( textarea.blur( function(ev) {
		cell.text(textarea.val());
		textarea.hide(); 
	}));
	// listen for key's we don't want to affect behaviour
	addListener( textarea.keydown( textareaOverride ));
	// modify if the text is updated
	addListener( textarea.keyup( function(ev) { 
		updateProperty(propertyObject, property, textarea.val(), refreshHtml);  
	}));
	
}

function Property_select(cell, propertyObject, property, refreshHtml, refreshProperties, details) {
	// holds the options html
	var options = "";
	var js = property.getValuesFunction;
	try {
		// get and create the getValuesFunctions
		var f = new Function(js);	
		// apply and get what we're after
		var values = f.apply(propertyObject,[]);
		// check if we got an array back, or just use the response
		if ($.isArray(values)) {
			// loop the array and build the options html
			for (var i in values) {
				// if an array of simple types no value attribute
				if ($.type(values[i]) == "string" || $.type(values[i]) == "number" || $.type(values[i]) == "boolean") {
					// if the value is matched add selected
					if (propertyObject[property.key] == values[i]) {
						options += "<option selected='selected'>" + values[i] + "</option>";
					} else {
						options += "<option>" + values[i] + "</option>";
					}	
				} else {
					// this allows value/text pairs either in the form [{"v1":"t1"},{"v2":"t2"}] or [{"value":"v1","text":"t1"},{"value":"v2","text":"t2"}] 
					var value = values[i].value;
					if (!value) value = values[i][0];
					var text = values[i].text;
					if (!text) text = values[i][1];
					if (!text) text = value;
					// if the value is matched add selected
					if (propertyObject[property.key] == value) {
						options += "<option value='" + value + "' selected='selected'>" + text + "</option>";
					} else {
						options += "<option value='" + value + "'>" + text + "</option>";
					}
				}
			}
		} else {
			options = values;
		}
	} catch (ex) {
		alert("getValuesFunction failed for " + propertyObject.type + ". " + ex + "\r\r" + js);
	}
	
	// add the select object
	cell.append("<select class='propertiesPanelTable'>" + options + "</select>");
	// get a reference to the object
	var select = cell.children().last();
	// add a listener to update the property
	addListener( select.change( {refreshProperties: refreshProperties}, function(ev) {
		// apply the property update
		updateProperty(propertyObject, property, ev.target.value, refreshHtml);
		// refresh the properties if requested
		if (ev.data.refreshProperties) refreshPropertyObject(ev, propertyObject);	
	}));
	// read the property
	var val = propertyObject[property.key];
	// update from any primitive values to string values
	switch (val) {
		case (true) : val = "true"; break;
		case (false) : val = "false"; break;
	}
	// set the value
	select.val(val);	
}

function Property_checkbox(cell, propertyObject, property, refreshHtml, refreshProperties, details) {
	var checked = "";
	// set the value if it exists
	if (propertyObject[property.key] && propertyObject[property.key] != "false") checked = "checked='checked'";
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' type='checkbox' " + checked + " />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.change(  {refreshProperties: refreshProperties}, function(ev) {
		// update the property
		updateProperty(propertyObject, property, ev.target.checked, refreshHtml);
		// refresh the property object if requested
		refreshPropertyObject(ev, propertyObject);
	}));
}

function Property_inputAutoHeight(cell, input, property, refreshHtml, refreshDialogue, details) {
	// check if the input control type is large
	if (input.controlType == "L") {
		// add a checkbox
		Property_checkbox(cell, input, property, refreshHtml);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_galleryImages(cell, gallery, property, refreshHtml, refreshDialogue, details) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Images");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
		
	// instantiate if need be
	if (!gallery.images) gallery.images = [];
	// retain variable for them
	var images = gallery.images;
	// assume no text
	var text = "";
	// loop images
	for (var i in images) {
		// add url;
		text += images[i].url;
		// add comma if there are more
		if (i < images.length - 1) text += ",";
	}
	// set the cell
	if (text) {
		cell.html(text);
	} else {
		cell.html("Click to add...");
	}
	
	// append the drop down for existing images
	table.append("<tr><td  colspan='3' style='text-align:center;'>Url</td></tr>");
	
	// loop the images
	for (var i in images) {
		// get this image
		var image = images[i];
		// append
		table.append("<tr><td><input value='" + image.url + "' /></td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
	}
	
	// add the change listeners
	addListener( table.find("input").keyup( function (ev) {
		// get the input
		var input = $(ev.target);
		// get the image according to the row index, less the header
		var image = images[input.closest("tr").index() - 1];
		// update the url
		image.url = input.val();
		// update the reference and rebuild the html (this adds an undo)
		updateProperty(gallery, property, images, refreshHtml); 			
	}));
	
	// add delete listeners
	addListener( table.find("img.delete").click( function (ev) {
		// add undo
		addUndo();
		// get the image
		var img = $(ev.target);
		// remove the image at this location
		images.splice(img.index() - 1);
		// update html 
		rebuildHtml(gallery);
		// update the dialogue;
		Property_galleryImages(cell, gallery, property, refreshHtml, dialogue);
	}));
	
	// add reorder listeners
	addReorder(images, table.find("img.reorder"), function() { rebuildHtml(gallery); Property_galleryImages(cell, gallery, property, refreshHtml, dialogue); });
		
	// append add
	table.append("<tr><td colspan='3'><a href='#'>add...</a></td></tr>");
	
	// add listener
	addListener( table.find("a").click( function (ev) {
		// add an image
		images.push({url:""});
		// refresh dialogue
		Property_galleryImages(cell, gallery, property, refreshHtml, dialogue);
	}));
	
}

function Property_imageFile(cell, propertyObject, property, refreshHtml, refreshDialogue, details) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Image file");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key]) value = propertyObject[property.key];
	// set the cell
	if (value) {
		cell.html(value);
	} else {
		cell.html("Click to add...");
	}
	
	// check we have some images
	if (_version.images) {
		
		// append the drop down for existing images
		table.append("<tr><td><select><option value=''>Please select...</option></select></td></tr>");
		
		// get a reference to the drop down
		var dropdown = table.find("select");
		
		// loop the images and add to select
		for (var i in _version.images) {
			var selected = "";
			if (_version.images[i] == propertyObject.file) selected = " selected='selected'";
			dropdown.append("<option" + selected + ">" + _version.images[i] + "</option>");
		}
		
		// add change listener
		addListener( dropdown.change( function (ev) {
			// get the file
			var file = $(this).val();
			// update the reference and rebuild the html
			updateProperty(propertyObject, property, file, true);
			// all some time for the page to load in the image before re-establishing the selection border
        	window.setTimeout( function() {
        		// show the dialogue
        		positionAndSizeBorder(_selectedControl);        
        		// resize the window and check for any required scroll bars
        		windowResize("Image file dropdown change");
        	}, 200);
				
		}));
		
	}
	
	// append the  form control and the submit button
	table.append("<tr><td><form id='form_" + propertyObject.id + "' method='post' enctype='multipart/form-data' target='uploadIFrame' action='designer?action=uploadImage&a=" + _version.id + "&v=" + _version.version + "&p=" + _page.id + "&c=" + propertyObject.id + "'><input id='file_" + propertyObject.id + "' name='file' type='file'></input></form></td></tr><tr><td><input type='submit' value='Upload' /></td></tr>");
	
	// get a reference to the submit button
	addListener( table.find("input[type=submit]").click( {id : propertyObject.id}, function (ev) {
		// get the file value
		var file = $("#file_" + ev.data.id).val();
		// submit form if something provided
		if (file) $("#form_" + ev.data.id).submit();
	}));
}

function Property_linkType(cell, propertyObject, property, refreshHtml) {
	// generate a select with refreshProperties = true
	Property_select(cell, propertyObject, { key : "linkType", getValuesFunction : 'return [["P","Page"],["U","URL"]];' }, refreshHtml, true);
}

function Property_linkPage(cell, propertyObject, property, refreshHtml) {
	// if the type is a page
	if (propertyObject.linkType == "P") {
		// generate a select with refreshProperties = true
		Property_select(cell, propertyObject, { key : "page", getValuesFunction : 'return "<option value=\'\'>Please select...</option>" + getPageOptions(this.page, _page.id);' }, refreshHtml, true);
	} else {
		// remove the row
		cell.parent().remove();
	}
}

function Property_linkURL(cell, propertyObject, property, refreshHtml) {
	// if the type is a url
	if (propertyObject.linkType == "U")	{
		// add a big text
		Property_bigtext(cell, propertyObject, property, refreshHtml);
	} else {
		// remove the row
		cell.parent().remove();
	}
}

function Property_linkPopup(cell, propertyObject, property, refreshHtml) {
	// if the type is a url
	if (propertyObject.linkType == "U")	{
		// add a big text
		Property_checkbox(cell, propertyObject, property, refreshHtml);
	} else {
		// remove the row
		cell.parent().remove();
	}
}

function Property_pageName(cell, page, property, refreshHtml, refreshDialogue, details) {
	// get the value from the page name
	var value = page.name;
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) {
		// get the input into a jquery object
		var input = $(ev.target);
		// get the current value
		var val = input.val();
		// prepare a string which will hold "safe" values
		var safeVal = "";
		// get the cursor position
		var pos = input.caret();
		// loop the characters in the current value
		for (var i = 0; i < val.length; i++) {
			// retrieve the ascii code for this character
			var c = val.charCodeAt(i);
			// only if the character is in the safe range (0-9, A-Z, a-z, -, _)
			if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c == 45 || c == 95 ) {
				 // add to safe value
				safeVal += val.charAt(i);
			} else {
				// otherwise drop a cursor position
				pos --;
			}
		}
		// if the value isn't safe
		if (val != safeVal) {		
			// update edit box to the safe value
			input.val(safeVal);
			// set the cursor position
			input.caret(pos);
			// retain the safe value
			val = safeVal;
		} 
		// update the property
		updateProperty(page, {key:"name"}, val, refreshHtml);
	}));

}

function Property_validationControls(cell, propertyObject, property, refreshHtml, refreshDialogue, details) {
		
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Controls");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	// retain the controls
	var controls = [];
	// get the value if it exists
	if (propertyObject[property.key]) controls = propertyObject[property.key];	
	// make some text
	var text = "";
	for (var i = 0; i < controls.length; i++) {
		var control = getControlById(controls[i]);
		if (control) {
			text += control.name;
		} 		
		if (text && i < controls.length - 1) text += ",";
	}
	
	// add a message if nont
	if (!text) text = "Click to add";
	// append the text into the cell
	cell.append(text);
	
	// add the current options
	for (var i in controls) {
		// see if we can get the control
		var control = getControlById(controls[i]);
		// check we can find the control - we can loose them when pasting
		if (control) {
			// add the row for this value
			table.append("<tr><td>" + control.name + "</td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");			
		} else {
			// remove this control
			controls.splice(i,1);
		}		
	}	
	
	// add listeners to the delete image
	addListener( table.find("img.delete").click( function(ev) {
		// get the row
		var row = $(this).parent().parent();
		// remove the control
		propertyObject.controls.splice(row.index(),1);
		// remove the row
		row.remove();
	}));
	
	// add reorder listeners
	addReorder(controls, table.find("img.reorder"), function() { Property_validationControls(cell, propertyObject, property, refreshHtml, dialogue); });
		
	// add an add dropdown
	var addControl = table.append("<tr><td colspan='2'><select><option value=''>Add control...</option>" + getValidationControlOptions() + "</select></td></tr>").children().last().children().last().children().last();
	addListener( addControl.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, refreshDialogue: dialogue}, function(ev) {
		// get a reference to the dropdown
		var dropdown = $(ev.target);
		// get the controlId
		var controlId = dropdown.val();
		// if we got one
		if (controlId) {
			// initialise the array if need be
			if (!propertyObject.controls) propertyObject.controls = [];
			// add the selected control id to the collection
			propertyObject.controls.push(controlId);
			// set the drop down back to "Please select..."
			dropdown.val("");		
			// re-render the dialogue
			Property_validationControls(ev.data.cell, ev.data.propertyObject, {key: "controls"}, ev.data.refreshHtml, ev.data.refreshDialogue);
		}
	}));
	
}

function Property_childActions(cell, propertyObject, property, refreshHtml, refreshDialogue, details) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Actions");		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// remove the dialogue class so it looks like the properties
	table.parent().removeClass("dialogueTable");
	// make sure its empty
	table.children().remove();
	
	// build what we show in the parent cell
	var actions = [];
	// get the value if it exists
	if (propertyObject[property.key]) actions = propertyObject[property.key];	
	// make some text
	var text = "";
	for (var i = 0; i < actions.length; i++) {
		text += actions[i].type;
		if (i < actions.length - 1) text += ",";
	}
	// if nothing add friendly message
	if (!text) text = "Click to add...";
	// put the text into the cell
	cell.text(text);
	
	// add the current options
	for (var i in actions) {
		// retrieve this action
		var action = actions[i];
		// show the action (in actions.js)
		showAction(table, action, actions, function() { Property_childActions(cell, propertyObject, property, refreshHtml, dialogue); });
	}	
	
	// add reorder listeners
	addReorder(actions, table.find("img.reorder"), function() { Property_childActions(cell, propertyObject, property, refreshHtml, dialogue); });
	
	// add a small space
	if (actions.length > 0) table.append("<tr><td colspan='2'></td></tr>");
	
	// add an add dropdown
	var addAction = table.append("<tr><td colspan='2'><select><option value=''>Add action...</option>" + _actionOptions + "</select></td></tr>").children().last().children().last().children().last();
	
	addListener( addAction.change( { cell: cell, propertyObject : propertyObject, property : property, refreshHtml : refreshHtml, dialogue: dialogue }, function(ev) {
		// get a reference to the dropdown
		var dropdown = $(ev.target);
		// get the controlId
		var actionType = dropdown.val();
		// if we got one
		if (actionType) {			
			// retrieve the propertyObject
			var propertyObject = ev.data.propertyObject;
			// retrieve the property
			var property = ev.data.property;
			// initilise the array if need be
			if (!propertyObject[property.key]) propertyObject[property.key] = [];
			// initialise this action
			var action = new Action(actionType);
			// add it to the array
			propertyObject[property.key].push(action);
			// set the drop down back to "Please select..."
			dropdown.val("");
			// rebuild the dialgue
			Property_childActions(ev.data.cell, propertyObject, property, ev.data.refreshHtml, ev.data.dialogue);			
		}		
	}));
			
}

function Property_childActionsForType(cell, propertyObject, property, refreshHtml, refreshDialogue, details) {
	
	// check we have what we need
	if (details && details.type) {
		
		// find the action class
		var actionClass = _actionTypes[details.type];
		
		// check we have one
		if (actionClass) {
			
			// retain a reference to the dialogue (if we were passed one)
			var dialogue = refreshDialogue;
			// if we weren't passed one - make what we need
			if (!dialogue) dialogue = createDialogue(cell, 200, "Child " + actionClass.name + " actions");		
			// grab a reference to the table
			var table = dialogue.children().last().children().last();
			// remove the dialogue class so it looks like the properties
			table.parent().removeClass("dialogueTable");
			// make sure its empty
			table.children().remove();
			
			// build what we show in the parent cell
			var actions = [];
			// get the value if it exists
			if (propertyObject[property.key]) actions = propertyObject[property.key];	
			// make some text
			var text = "";
			for (var i = 0; i < actions.length; i++) {
				text += actions[i].type;
				if (i < actions.length - 1) text += ",";
			}
			// if nothing add friendly message
			if (!text) text = "Click to add...";
			// put the text into the cell
			cell.text(text);
			
			// add the current options
			for (var i in actions) {
				// retrieve this action
				var action = actions[i];
				// show the action (in actions.js)
				showAction(table, action, actions, function() { Property_childActionsForType(cell, propertyObject, property, refreshHtml, dialogue, details); });
			}	
			
			// add reorder listeners
			addReorder(actions, table.find("img.reorder"), function() { Property_childActionsForType(cell, propertyObject, property, refreshHtml, dialogue, details); });
			
			// add a small space
			if (actions.length > 0) table.append("<tr><td colspan='2'></td></tr>");
			
			// add an add dropdown
			var addAction = table.append("<tr><td colspan='2'><a href='#'>add...</a></td></tr>").children().last().children().last().children().last();
			
			addListener( addAction.click( { details: details, cell: cell, propertyObject : propertyObject, property : property, refreshHtml : refreshHtml, dialogue: dialogue }, function(ev) {
				// initialise this action
				var action = new Action(ev.data.details.type);
				// if we got one
				if (action) {			
					// retrieve the propertyObject
					var propertyObject = ev.data.propertyObject;
					// retrieve the property
					var property = ev.data.property;
					// initilise the array if need be
					if (!propertyObject[property.key]) propertyObject[property.key] = [];
					// add it to the array
					propertyObject[property.key].push(action);
					// rebuild the dialgue
					Property_childActionsForType(ev.data.cell, propertyObject, property, ev.data.refreshHtml, ev.data.dialogue, ev.data.details);			
				}		
			}));
			
		} else {
			
			// put a message into the cell
			cell.text("Action " + details.type + " not found");
			
		}
		
	} else {
		
		// put a message into the cell
		cell.text("Action type not provided");
		
	}
						
}

// this is a dialogue to specify the inputs, sql, and outputs for the database action
function Property_databaseQuery(cell, propertyObject, property, refreshHtml, refreshDialogue, details) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 1000, "Query");		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
		
	// initialise the query object if need be
	if (!propertyObject.query) propertyObject.query = {inputs:[], sql:"select field1, field2\nfrom table\nwhere field3 = ?", databaseConnectionIndex: 0, outputs:[]};
	// get the query
	var query = propertyObject.query;
	// get the sql into a variable
	var text = query.SQL;
	// change to message if not provided
	if (!text) text = "Click to define...";
	// put the elipses in the cell
	cell.text(text);
	
	// add inputs table, sql, and outputs table
	table.append("<tr><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%'  class='propertiesPanelTable'><tr><td><b>Input</b></td><td colspan='2'><b>Field</b></td></tr></table></td><td colspan='2' style='width:500px;padding:0px 6px 0px 0px;'><b>SQL</b><br/><textarea style='width:100%;min-height:300px;'></textarea></td><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%' class='propertiesPanelTable'><tr><td><b>Field</b></td><td colspan='2'><b>Output</b></td></tr></table></td></tr>");
	
	// find the inputs table
	var inputsTable = table.children().last().children().first().children().last();
	// loop input parameters
	for (var i in query.inputs) {
		// get the input name
		var itemName = query.inputs[i].itemId;
		// look for a control with an item of this item
		var control = getControlById(itemName);
		// if we found a control use this as the name
		if (control) itemName = control.name;
		// get the field
		var field = query.inputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		inputsTable.append("<tr><td>" + itemName + "</td><td><input value='" + field + "' /></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldInput = inputsTable.find("tr").last().children(":nth(1)").last().children().last();
		// add a listener
		addListener( fieldInput.keyup( {parameters: query.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = inputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: query.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));
	}
	// add reorder listeners
	addReorder(query.inputs, inputsTable.find("img.reorder"), function() { 
		Property_databaseQuery(cell, propertyObject, {key: "query"}, refreshHtml, dialogue); 
	});
	// add the add input
	inputsTable.append("<tr><td style='padding:0px;'><select style='margin:0px'><option value=''>Add input...</option>" + getInputOptions() + "</select></td><td>&nbsp;</td><td>&nbsp;</td></tr>");
	// find the input add
	var inputAdd = inputsTable.find("tr").last().children().first().children().first();
	// listener to add input
	addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.query.inputs) ev.data.propertyObject.query.inputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.query.inputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_databaseQuery(ev.data.cell, ev.data.propertyObject, {key: "query"}, ev.data.refreshHtml, ev.data.dialogue);
	}));
	
	// find the sql textarea
	var sqlControl = table.find("textarea").first();
	sqlControl.text(query.SQL);
	// listener for the sql
	addListener( sqlControl.keyup( {query: query}, function(ev) {
		query.SQL = $(ev.target).val();
	}));
	
	// find the outputs table
	var outputsTable = table.children().last().children().last().children().last();
	// loop output parameters
	for (var i in query.outputs) {
		// get the output name
		var itemName = query.outputs[i].itemId;
		// look for a control with an item of this item
		var control = getControlById(itemName);
		// if we found a control use this as the name
		if (control) itemName = control.name;
		// get the field
		var field = query.outputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		outputsTable.append("<tr><td><input value='" + field + "' /></td><td>" + itemName + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldOutput = outputsTable.find("tr").last().children().first().children().last();
		// add a listener
		addListener( fieldOutput.keyup( {parameters: query.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = outputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: query.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));			
	}
	// add reorder listeners
	addReorder(query.outputs, outputsTable.find("img.reorder"), function() { 
		Property_databaseQuery(cell, propertyObject, {key: "query"}, refreshHtml, dialogue); 
	});
	// add the add
	outputsTable.append("<tr><td>&nbsp;</td><td style='padding:0px;'><select style='margin:0px'><option value=''>Add output...</option>" + getOutputOptions() + "</select></td><td>&nbsp;</td></tr>");
	// find the output add
	var outputAdd = outputsTable.find("tr").last().children(":nth(1)").last().children().last();
	// listener to add output
	addListener( outputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.query.outputs) ev.data.propertyObject.query.outputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.query.outputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_databaseQuery(ev.data.cell, ev.data.propertyObject, {key: "query"}, ev.data.refreshHtml, ev.data.dialogue);	
	}));
	
	table.append("<tr><td>Database connection <select style='width:auto;'>" + getDatabaseConnectionOptions(query.databaseConnectionIndex) + "</select></td><td style='text-align:right;'><button>Test SQL</button></td></tr>");
	
	// get a reference to the db connection
	var dbConnection = table.find("tr").last().find("select");
	// add a listener for the database connection
	addListener( dbConnection.change( {query: query}, function(ev) {
		// set the index value
		ev.data.query.databaseConnectionIndex = ev.target.selectedIndex;
	}));
	
	// get a reference to the test button
	var testSQL = table.find("tr").last().find("button");
	// add a listener for the database connection
	addListener( testSQL.click( {query: query}, function(ev) {
		
		var query = JSON.stringify(ev.data.query);
		
		$.ajax({
	    	url: "designer?a=" + _version.id + "&v=" + _version.version + "&action=testSQL",
	    	type: "POST",
	    	contentType: "application/json",
		    dataType: "json",  
	    	data: query,
	        error: function(server, status, error) { 
	        	alert(error + " : " + server.responseText); 
	        },
	        success: function(response) {
	        	alert(response.message); 		       		        	
	        }
		});
		
	}));
}

// reuse the generic childActionsForType but set the details with type = database
function Property_databaseChildActions(cell, propertyObject, property, refreshHtml, refreshDialogue, details) {
	Property_childActionsForType(cell, propertyObject, property, refreshHtml, refreshDialogue, {type:"database"});
}

//this is a dialogue to specify the inputs, post body, and outputs for the webservice action
function Property_webserviceRequest(cell, propertyObject, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 1000, "Webservice request");		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
	// make sure its empty
	table.children().remove();
		
	// initialise the request object if need be
	if (!propertyObject.request) propertyObject.request = {inputs:[], type:"SOAP", url: 'soa', action: 'aaa.Samplewebservice', body: '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:soa="http://soa.rapid-is.co.uk">\n  <soapenv:Body>\n    <soa:personSearchRequest>\n      <soa:surname>A</soa:surname>\n    </soa:personSearchRequest>\n  </soapenv:Body>\n</soapenv:Envelope>', outputs:[]};
	// get the request
	var request = propertyObject.request;
	// get the sql into a variable
	var text = request.type;
	// change to message if not provided
	if (!text) text = "Click to define...";
	// put the elipses in the cell
	cell.text(text);
	
	// add inputs table, body, and outputs table
	table.append("<tr><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%' class='propertiesPanelTable'><tr><td><b>Input</b></td><td colspan='2'><b>Field</b></td></tr></table></td><td colspan='2' style='width:500px;padding:0px 6px 0px 0px;'><b>Type</b><br/><input type='radio' name='WSType' value='SOAP'/>SOAP<input type='radio' name='WSType' value='JSON'/>JSON<input type='radio' name='WSType' value='XML'/>XML/Restfull</br><b>URL</b><br/><input class='WSUrl'/></br><b>Action</b><br/><input class='WSAction'/></br><b>Body</b><br/><textarea style='width:100%;min-height:300px;' class='WSBody'></textarea></td><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%' class='propertiesPanelTable'><tr><td><b>Field</b></td><td colspan='2'><b>Output</b></td></tr></table></td></tr>");
	
	// find the inputs table
	var inputsTable = table.children().last().children().first().children().last();
	// loop input parameters
	for (var i in request.inputs) {
		// get the input name
		var itemName = request.inputs[i].itemId;
		// look for a control with an item of this item
		var control = getControlById(itemName);
		// if we found a control use this as the name
		if (control) itemName = control.name;
		// get the field
		var field = request.inputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		inputsTable.append("<tr><td>" + itemName + "</td><td><input value='" + field + "' /></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldInput = inputsTable.find("tr").last().children(":nth(1)").last().children().last();
		// add a listener
		addListener( fieldInput.keyup( {parameters: request.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = inputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: request.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));
	}
	// add reorder listeners
	addReorder(request.inputs, inputsTable.find("img.reorder"), function() { 
		Property_webserviceRequest(cell, propertyObject, {key: "request"}, refreshHtml, dialogue); 
	});
	// add the add input
	inputsTable.append("<tr><td style='padding:0px;'><select style='margin:0px'><option value=''>Add input...</option>" + getInputOptions() + "</select></td><td>&nbsp;</td><td>&nbsp;</td></tr>");
	// find the input add
	var inputAdd = inputsTable.find("tr").last().children().first().children().first();
	// listener to add input
	addListener( inputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.request.inputs) ev.data.propertyObject.request.inputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.request.inputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_webserviceRequest(ev.data.cell, ev.data.propertyObject, {key: "request"}, ev.data.refreshHtml, ev.data.dialogue);
	}));
	
	// find the type radios
	var typeControls = table.find("input[type=radio]");
	typeControls.filter("[value=" + request.type + "]").prop("checked","true");
	// listener for the action
	addListener( typeControls.click( {request: request}, function(ev) {
		request.type = $(ev.target).val();
	}));
	
	// find the url input box
	var actionControl = table.find("input.WSUrl");
	actionControl.val(request.url);
	// listener for the action
	addListener( actionControl.keyup( {request: request}, function(ev) {
		request.url = $(ev.target).val();
	}));
	
	// find the action input box
	var actionControl = table.find("input.WSAction");
	actionControl.val(request.action);
	// listener for the action
	addListener( actionControl.keyup( {request: request}, function(ev) {
		request.action = $(ev.target).val();
	}));
	
	// find the request body textarea
	var bodyControl = table.find("textarea");
	bodyControl.text(request.body);
	// listener for the body
	addListener( bodyControl.keyup( {request: request}, function(ev) {
		request.body = $(ev.target).val();
	}));
	
	// find the outputs table
	var outputsTable = table.children().last().children().last().children().last();
	// loop output parameters
	for (var i in request.outputs) {
		// get the output name
		var itemName = request.outputs[i].itemId;
		// look for a control with an item of this item
		var control = getControlById(itemName);
		// if we found a control use this as the name
		if (control) itemName = control.name;
		// get the field
		var field = request.outputs[i].field;
		// make it an empty space if null
		if (!field) field = "";
		// add the row
		outputsTable.append("<tr><td><input value='" + field + "' /></td><td>" + itemName + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field input
		var fieldOutput = outputsTable.find("tr").last().children().first().children().last();
		// add a listener
		addListener( fieldOutput.keyup( {parameters: request.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = outputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		addListener( fieldDelete.click( {parameters: request.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.parameters.splice(input.parent().parent().index()-1,1);
			// remove row
			input.parent().parent().remove();
		}));			
	}
	// add reorder listeners
	addReorder(request.outputs, outputsTable.find("img.reorder"), function() { 
		Property_webserviceRequest(cell, propertyObject, {key: "request"}, refreshHtml, dialogue); 
	});
	// add the add
	outputsTable.append("<tr><td>&nbsp;</td><td style='padding:0px;'><select style='margin:0px'><option value=''>Add output...</option>" + getOutputOptions() + "</select></td><td>&nbsp;</td></tr>");
	// find the output add
	var outputAdd = outputsTable.find("tr").last().children(":nth(1)").last().children().last();
	// listener to add output
	addListener( outputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.request.outputs) ev.data.propertyObject.request.outputs = [];
		// get the parameters (inputs or outputs)
		var parameters = ev.data.propertyObject.request.outputs;
		// add a new one
		parameters.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_webserviceRequest(ev.data.cell, ev.data.propertyObject, {key: "request"}, ev.data.refreshHtml, ev.data.dialogue);	
	}));
		
}

// this is a special drop down that can make the property below visible
function Property_navigationPage(cell, navigationAction, property, refreshHtml, refreshDialogue) {
	
	// add the drop down with it's values
	cell.append("<select><option value=''>Please select...</option>" + getPageOptions(navigationAction.page) + "</select>");
	// get a reference to the drop down
	var pageDropDown = cell.find("select").last();
	// add a listener
	addListener( pageDropDown.change( {navigationAction: navigationAction}, function(ev) {
		// update the page value
		ev.data.navigationAction.page = $(ev.target).val();
		// rebuild all events/actions
		showEvents(_selectedControl);
	}));
	
}

// this is a dialogue to specify the session variables of the current page
function Property_pageSessionVariables(cell, page, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Page variables");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var variables = [];
	// set the value if it exists
	if (page.sessionVariables) variables = page.sessionVariables;
	// make some text
	var text = "";
	for (var i = 0; i < variables.length; i++) {
		text += variables[i];
		if (i < variables.length - 1) text += ",";
	}
	// add a descrption if nothing yet
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// show variables
	for (var i in variables) {
		// add the line
		table.append("<tr><td style='padding-left:0px'><input class='variable' value='" + variables[i] + "' /></td><td><img src='images/bin_16x16.png' style='float:right;' /></td></tr>");
		
		// find the text
		var valueEdit = table.find("input.variable").last();
		// add a listener
		addListener( valueEdit.keyup( {variables: variables}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.variables[input.parent().parent().index()] = input.val();
		}));
				
		// find the delete
		var optionDelete = table.find("tr").last().children().last().children().last();
		// add a listener
		addListener( optionDelete.click( {variables: variables}, function(ev) {
			// add an undo snapshot
			addUndo();
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.variables.splice(input.parent().parent().index(),1);
			// remove row
			input.parent().parent().remove();
		}));
	}
		
	// have an add row
	table.append("<tr><td colspan='2'><a href='#'>add...</a></td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.click( {cell: cell, page: page, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// add an undo snapshot
		addUndo();
		// initialise if required
		if (!ev.data.page.sessionVariables) ev.data.page.sessionVariables = [];
		// add a blank option
		ev.data.page.sessionVariables.push("");
		// refresh
		Property_pageSessionVariables(ev.data.cell, ev.data.page, {key: "sessionVariables"}, ev.data.refreshHtml, ev.data.dialogue);		
	}));
	
}

// this is a dalogue which allows for the adding of user roles
function Property_roles(cell, control, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "User roles");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var roles = [];
	// set the value if it exists
	if (control.roles) roles = control.roles;
	// make some text
	var text = "";
	for (var i = 0; i < roles.length; i++) {
		text += roles[i];
		if (i < roles.length - 1) text += ",";
	}
	// add a descrption if nothing yet
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// show roles
	for (var i in roles) {
		// add the line
		table.append("<tr><td style='padding-left:0px'>" + roles[i] + "</td><td><img src='images/bin_16x16.png' style='float:right;' /></td></tr>");
						
		// find the delete
		var optionDelete = table.find("tr").last().children().last().children().last();
		// add a listener
		addListener( optionDelete.click( {roles: roles, cell: cell, control: control, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
			// get the input
			var input = $(ev.target);
			// remove from parameters
			ev.data.roles.splice(input.parent().parent().index(),1);
			// remove row
			input.parent().parent().remove();
			// refresh
			Property_roles(ev.data.cell, ev.data.control, {key: "roles"}, ev.data.refreshHtml, ev.data.dialogue);
		}));
	}
		
	// have an add dropdown
	table.append("<tr><td colspan='2'><select><option value=''>add...</option>" + getRolesOptions(null, roles) + "</td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.change( {cell: cell, control: control, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// initialise if required
		if (!ev.data.control.roles) ev.data.control.roles = [];
		// get the role
		var role = ev.target.value;
		// add the selected role if one was chosen
		if (role) ev.data.control.roles.push(role);
		// refresh
		Property_roles(ev.data.cell, ev.data.control, {key: "roles"}, ev.data.refreshHtml, ev.data.dialogue);		
	}));

}

// this is a dialogue to specify the session variables to set when navigating
function Property_navigationSessionVariables(cell, navigation, property, refreshHtml, refreshDialogue) {
	
	// this is some reuse in the link control - if it's type isn't P for page
	if (navigation.linkType && navigation.linkType != "P") {
		// remove this row
		cell.parent().remove();
		// stop going any further
		return false;
	}
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 300, "Set page variables");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	// find the page we're set up to go to
	var page = null;
	for (var i in _pages) {
		if (_pages[i].id == navigation.page) page = _pages[i];
	}
	
	// check a page to go to has been specified
	if (page) {
	
		// initialise the collection if need be
		if (!navigation.sessionVariables || navigation.sessionVariables == "[]") navigation.sessionVariables = [];
		// reset if there are no page session variables
		if (page.sessionVariables.length == 0) navigation.sessionVariables = [];
		// retrieve the collection
		var sessionVariables = navigation.sessionVariables;
		
		// make some text
		var text = "";
		for (var i = 0; i < sessionVariables.length; i++) {
			text += sessionVariables[i].name;
			if (i < sessionVariables.length - 1) text += ",";
		}
		// add a descrption if nothing yet
		if (!text) text = "Click to add...";
		// append the adjustable form control
		cell.text(text);
		
		// add a header
		table.append("<tr><td><b>Variable</b></td><td><b>Input</b></td><td><b>Field</b></td></tr>");
						
		// show all session parameters in the target page
		for (var i in page.sessionVariables) {
			
			// get the corresponding action variable
			var sessionVariable = sessionVariables[i];
			// if not there or not the right one
			if (!sessionVariable || sessionVariable.name != page.sessionVariables[i]) {
				// create a new one with the right name
				sessionVariable = {name:page.sessionVariables[i], itemId:"", field:""};
				// add to the aray at this position				
				sessionVariables.splice(i,0,sessionVariable);
			}

			// set the input name
			var name = sessionVariable.name;
			// look for a control with this id
			var control = getControlById(name);
			// update name if we found one
			if (control) name = control.name;
			
			// add the line
			table.append("<tr><td>" + name + "</td><td><select><option value=''>Please select...</option>" + getInputOptions(sessionVariable.itemId) + "</select></td><td style='padding-left:0px'><input value='" + sessionVariable.field + "' /></td></tr>");
			
			// find the dropdown
			var itemEdit = table.find("select").last();
			// add a listener
			addListener( itemEdit.change( {sessionVariables: sessionVariables}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update value
				ev.data.sessionVariables[input.parent().parent().index()-1].itemId = input.val();
			}));
			
			// find the input
			var fieldEdit = table.find("input").last();
			// add a listener
			addListener( fieldEdit.keyup( {sessionVariables: sessionVariables}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update value
				ev.data.sessionVariables[input.parent().parent().index()-1].field = input.val();
			}));
					
		}
		// remove any extra entries (in case our insertion made the collection too big)
		if (sessionVariables.length > i) sessionVariables.splice(i*1 + 1, sessionVariables.length);
		
	} else {
		
		// hide this row until a page is selected
		cell.parent().hide();
		
	}
					
}

//this is a dialogue to define radio buttons
function Property_radiobuttons(cell, radiobuttons, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Radio buttons");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var buttons = [];
	// set the value if it exists
	if (radiobuttons.buttons) buttons = radiobuttons.buttons;
	// make some text
	var text = "";
	for (var i = 0; i < buttons.length; i++) {
		text += buttons[i].label;
		if (radiobuttons.codes) text += " (" + buttons[i].value + ")";	
		if (i < buttons.length - 1) text += ",";
	}
	// add a descrption if nothing yet
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// add a heading
	table.append("<tr>" + (radiobuttons.codes ? "<td><b>Code</b></td>" : "") + "<td colspan='2'><b>Text</b></td></tr>");
	
	// show options
	for (var i in buttons) {
		// add the line
		table.append("<tr>" + (radiobuttons.codes ? "<td style='padding-left:0px'><input class='value' value='" + buttons[i].value + "' /></td>" : "") + "<td style='padding-left:0px'><input class='label' value='" + buttons[i].label + "' /></td><td><img src='images/bin_16x16.png' style='float:right;' /></td></tr>");
		
		// find the code
		var valueEdit = table.find("input.value").last();
		// add a listener
		addListener( valueEdit.keyup( {radiobuttons : radiobuttons, buttons: buttons}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.buttons[input.parent().parent().index()-1].value = input.val();
			// update html 
			rebuildHtml(ev.data.radiobuttons);
		}));
		
		// find the label
		var textEdit = table.find("input.label").last();
		// add a listener
		addListener( textEdit.keyup( {radiobuttons : radiobuttons, buttons: buttons}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update text
			ev.data.buttons[input.parent().parent().index()-1].label = input.val();
			// update html 
			rebuildHtml(ev.data.radiobuttons);
		}));
		
		
		// find the delete
		var buttonDelete = table.find("tr").last().children().last().children().last();
		// add a listener
		addListener( buttonDelete.click( {cell: cell, radiobuttons: radiobuttons, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
			// get the del image
			var delImage = $(ev.target);
			// remove from parameters
			ev.data.radiobuttons.buttons.splice(delImage.parent().parent().index()-1,1);
			// remove row
			delImage.parent().parent().remove();
			// update html if top row
			if (delImage.parent().index() == 1) rebuildHtml(ev.data.radiobuttons);
			// refresh
			Property_radiobuttons(ev.data.cell, ev.data.radiobuttons, {key: "buttons"}, ev.data.refreshHtml, ev.data.dialogue);
		}));
	}
		
	// have an add row
	table.append("<tr><td colspan='" + (radiobuttons.codes ? "3" : "2") + "'><a href='#'>add...</a></td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.click( {cell: cell, radiobuttons: radiobuttons, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// add a blank option
		ev.data.radiobuttons.buttons.push({value: "", label: ""});
		// refresh
		Property_radiobuttons(ev.data.cell, ev.data.radiobuttons, {key: "buttons"}, ev.data.refreshHtml, ev.data.dialogue);		
	}));
	
	// check we don't have a checkbox already
	if (!dialogue.find("input[type=checkbox]")[0]) {
		// add checkbox
		dialogue.append("Use codes <input type='checkbox' " + (radiobuttons.codes ? "checked='checked'" : "") + " />");
		// get a reference
		var optionsCodes = dialogue.children().last();
		// add a listener
		addListener( optionsCodes.change( {cell: cell, radiobuttons: radiobuttons, buttons: buttons, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
			// get the value
			ev.data.radiobuttons.codes = ev.target.checked;
			// refresh
			Property_radiobuttons(ev.data.cell, ev.data.radiobuttons, {key: "buttons"}, ev.data.refreshHtml, ev.data.dialogue);
		
		}));
		
	}
	
}

//possible system values used by the Logic property
var _logicOperations = [["==","= equals"],["!=","!= doesn't equal"],[">","> greater than"],[">=",">= greater than or equal to"],["<","< less than"],["<=","<= less than or equal to"]];

function logicConditionText(condition) {
	// make some text
	var text = "Not set";	
	// check the type
	switch (condition.type) {
		case "CTL" :
			// assume there is no control
			var control = null;
			// get the id parts
			var idParts = condition.id.split(".");
			// get the control id
			var controlId = idParts;
			// if  there was more than one part, take the first
			if (idParts.length > 1) controlId = idParts[0];
			// look for the control
			if (controlId) control = getControlById(controlId);
			// if we don't find one just show id (could be page variable)
			text = (control ? control.name : condition.id);
			// add the property if present
			if (idParts.length > 1) text += "." + idParts[1];
			// add the field if present
			if (condition.field) text += "." + condition.field;
		break;
		case "SYS" :
			// the second part of the id
			text = condition.id.split(".")[1];
		break;		
	}
	// return
	return text;
}

function logicConditionValue(cell, action, conditionIndex, valueId) {
	
	// get a reference to the condition
	var condition = action.conditions[conditionIndex];
	// instantiate the value if need be
	if (!condition[valueId]) condition[valueId] = {type:"CTL"};
	// get a reference for the value
	var value = condition[valueId];
		
	// clear and add a table into the cell for this value
	cell.html("<table class='propertiesPanelTable'></table>")
	// get a reference to it
	var table = cell.find("table").last();
	
	table.append("<tr><td>" + (valueId == "value1" ? "Item 1" : "Item 2") + "</td><td><select>" + getInputOptions(value.id) + "</select></td></tr>");
	// get a reference to the select
	var select = table.find("select");
	// retain the value if we don't have one yet
	if (!value.id) value.id = select.val();			
	// add listers
	addListener( table.find("select").change( function(ev) {		
		// get the id
		var id = $(ev.target).val();
		// derive the new type
		var type = "CTL";				
		// check for system value
		if (id.substr(0,7) == "System.") type = "SYS";
		// set the new type 
		value.type = type;
		// set the id
		value.id = id;
		// refresh the property
		logicConditionValue(cell, action, conditionIndex, valueId); 
	}));
		
	switch (value.type) {
		case "CTL" :
			// set the html			
			table.append("<tr><td>Field</td><td><input /></td></tr>");			
			// get the field
			var input = table.find("input").last();
			// set any current value
			if (value.field) input.val(value.field);
			// add the listener
			addListener( input.keyup( function(ev) {		
				// set the new value 
				value.field = $(ev.target).val();
			}));
		break;
		case "SYS" :
			if (value.id == "System.field") {
				// set the html
				table.append("<tr><td>Value</td><td><input /></td></tr>");
				// get the input
				var input = table.find("input").last();
				// set any current value
				if (value.field) input.val(value.field);
				// add the listeners
				addListener( input.keyup( function(ev) {		
					// set the new value into the field
					value.field = $(ev.target).val();
				}));
			}
		break;		
	}
	
}

function Property_logicConditions(cell, action, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 500, "Conditions");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// add the style that removed the restriction on column widths
	table.addClass("dialogueTable");
	// make sure table is empty
	table.children().remove();
	// get the conditions
	var conditions = action[property.key];
	// instantiate if required
	if (!conditions) conditions = [];
	// assume there is no text
	var text = "";
		
	// loop the conditions
	for (var i in conditions) {
		
		// get the condition
		var condition = conditions[i];
		
		// add cells
		table.append("<tr><td style='vertical-align:top;'></td><td style='vertical-align:top;'></td><td style='vertical-align:top;'></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		
		// get last row
		var lastrow = table.find("tr").last();
		
		// get cell references
		var value1Cell = lastrow.children("td:nth(0)");		
		var value2Cell = lastrow.children("td:nth(2)");
		var operationCell = lastrow.children("td:nth(1)");
		
		// add (sub)properties
		logicConditionValue(value1Cell, action, i, "value1");
		logicConditionValue(value2Cell, action, i, "value2");
		
		var operationHtml = "<select style='margin-top:1px;'>"
		for (var j in _logicOperations) {
			operationHtml += "<option value='" + _logicOperations[j][0] + "'" + (condition.operation == _logicOperations[j][0] ? " selected='selected'" : "") + ">" + _logicOperations[j][1] + "</option>";
		}
		operationHtml += "</select>";
		operationCell.append(operationHtml);
		
		// add a listener for the operation
		operationCell.find("select").last().change({condition: condition}, function(ev){
			ev.data.condition.operation = $(ev.target).val();
		});
		
		// build the text from the conditions, operation (== is mapped to =)
		text += logicConditionText(condition.value1) + " " + (condition.operation == "==" ? "=" : condition.operation)  + " " + logicConditionText(condition.value2);
		// add the type to seperate conditions
		if (i < conditions.length - 1) text += " " + action.conditionsType + " ";
		
	}
	
	// update text if not set
	if (!text) text = "Click to add...";
	// add in the text
	cell.text(text);
	
	// find the deletes
	var deleteImages = table.find("img.delete");
	// add a listener
	addListener( deleteImages.click( {conditions: conditions}, function(ev) {
		// get the del image
		var delImage = $(ev.target);
		// remove from conditions
		ev.data.conditions.splice(delImage.parent().parent().index(),1);
		// if there are now less than 2 conditions remove and/or row too
		if (ev.data.conditions.length < 2) $(ev.target).closest("table").find("tr:last").prev().remove();
		// remove row
		delImage.parent().parent().remove();		
	}));
		
	// add reorder listeners
	addReorder(conditions, table.find("img.reorder"), function() { 		
		// refresh the property
		Property_logicConditions(cell, action, {key: "conditions"}, refreshHtml, dialogue); 
	});
	
	// only if there are 2 or more conditions
	if (conditions.length > 1) {		
		// add type
		table.append("<tr><td colspan='4' style='padding-left:12px;'><input type='radio' name='" + action.id + "type' value='and'" + (action.conditionsType == "and" ? " checked='checked'" : "") + "/>all conditions must be true (And) <input type='radio' name='" + action.id + "type' value='or'" + (action.conditionsType == "or" ? " checked='checked'" : "") + "/>any condition can be true (Or) </td></tr>");
		// add change listeners
		table.find("input[name=" + action.id + "type]").change( function(ev){
			// set the condition type to the new val
			action.conditionsType = $(ev.target).val();
		});
	}
				
	// add add
	table.append("<tr><td colspan='4'><a href='#'>add...</a></td></tr>");
	// add listener
	table.find("a").last().click( function(ev) {
		// instatiate if need be
		if (!action.conditions) action.conditions = [];
		// add new condition
		action.conditions.push({value1:{type:"CTL"}, operation: "==", value2: {type:"CTL"}});
		// update this table
		Property_logicConditions(cell, action, property, refreshHtml, dialogue);
	})
	
}

// this is a dialogue to refine the options available in a dropdown control
function Property_options(cell, dropdown, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Options");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var options = [];
	// set the value if it exists
	if (dropdown.options) options = dropdown.options;
	// make some text
	var text = "";
	for (var i = 0; i < options.length; i++) {
		text += options[i].text;
		if (dropdown.codes) text += " (" + options[i].value + ")";	
		if (i < options.length - 1) text += ",";
	}
	// add a descrption if nothing yet
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// add a heading
	table.append("<tr><td><b>Text</b></td>" + (dropdown.codes ? "<td colspan='2'><b>Code</b></td>" : "") + "</tr>");
	
	// show options
	for (var i in options) {
		// add the line
		table.append("<tr><td style='padding-left:0px'><input class='text' value='" + options[i].text + "' /></td>" + (dropdown.codes ? "<td style='padding-left:0px'><input class='value' value='" + options[i].value + "' /></td>" : "") + "<td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
						
		// find the text
		var textEdit = table.find("input.text").last();
		// add a listener
		addListener( textEdit.keyup( {dropdown : dropdown, options: options}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update text
			ev.data.options[input.parent().parent().index()-1].text = input.val();
			// update html if top row
			if (input.parent().parent().index() == 1) rebuildHtml(dropdown);
		}));
		
		// find the code
		var valueEdit = table.find("input.value").last();
		// add a listener
		addListener( valueEdit.keyup( {options: options}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.options[input.parent().parent().index()-1].value = input.val();
		}));
				
	}
	
	// find the deletes
	var deleteImages = table.find("img.delete");
	// add a listener
	addListener( deleteImages.click( {options: options}, function(ev) {
		// get the del image
		var delImage = $(ev.target);
		// remove from parameters
		ev.data.options.splice(delImage.parent().parent().index()-1,1);
		// remove row
		delImage.parent().parent().remove();
		// update html if top row
		if (delImage.parent().index() == 1) rebuildHtml(dropdown);
	}));
		
	// add reorder listeners
	addReorder(options, table.find("img.reorder"), function() { 
		// refresh the html and regenerate the mappings
		rebuildHtml(dropdown);
		// refresh the property
		Property_options(cell, dropdown, {key: "options"}, refreshHtml, dialogue); 
	});
		
	// have an add row
	table.append("<tr><td colspan='" + (dropdown.codes ? "3" : "2") + "'><a href='#'>add...</a></td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.click( {cell: cell, dropdown: dropdown, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// add a blank option
		ev.data.dropdown.options.push({value: "", text: ""});
		// refresh
		Property_options(ev.data.cell, ev.data.dropdown, {key: "options"}, ev.data.refreshHtml, ev.data.dialogue);		
	}));
	
	// check we don't have a checkbox already
	if (!dialogue.find("input[type=checkbox]")[0]) {
		// add checkbox
		dialogue.append("Use codes <input type='checkbox' " + (dropdown.codes ? "checked='checked'" : "") + " />");
		// get a reference
		var optionsCodes = dialogue.children().last();
		// add a listener
		addListener( optionsCodes.change( {cell: cell, dropdown: dropdown, options: options, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
			// get the value
			dropdown.codes = ev.target.checked;
			// refresh
			Property_options(ev.data.cell, ev.data.dropdown, {key: "options"}, ev.data.refreshHtml, ev.data.dialogue);
		
		}));
		
	}
	
}

//this is a dialogue to refine the options available in a dropdown control
function Property_gridColumns(cell, grid, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 650, "Columns");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	// get the grid columns
	var columns = grid.columns;
	// if they don't exist or an empty array string make them an empty array
	if (!columns || columns == "[]") columns = [];
	// make some text
	var text = "";
	for (var i = 0; i < columns.length; i++) {
		text += columns[i].title;
		if (i < columns.length - 1) text += ",";
	}
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// add a header
	table.append("<tr><td style='width:20px;'><b>Visible</b></td><td><b>Title</b></td><td><b>Title style</b></td><td><b>Field</b></td><td><b>Field style</b></td><td colspan='2'><b>Cell function</b></td></tr>");
		
	// show columns
	for (var i in columns) {

		// set the cellFunction text to ellipses
		var cellFunctionText = "...";
		// update to function if present
		if (columns[i].cellFunction) cellFunctionText = columns[i].cellFunction;
		
		// add the line
		table.append("<tr><td style='text-align:center;'><input type='checkbox' " + (columns[i].visible ? "checked='checked'" : "")  + " /></td><td style='padding-left:0px'><input style='max-width:none;' value='" + columns[i].title + "' /></td><td style='padding-left:0px'><input style='max-width:none;' value='" + columns[i].titleStyle + "' /></td><td style='padding-left:0px'><input style='max-width:none;' value='" + columns[i].field + "' /></td><td style='padding-left:0px'><input style='max-width:none;' value='" + columns[i].fieldStyle + "' /></td><td style='width:20px;padding-left:5px'>" + cellFunctionText.replaceAll("<","&lt;") + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		
		// find the checkbox
		var visibleEdit = table.find("tr").last().children(":nth(0)").first().children().first();
		// add a listener
		addListener( visibleEdit.change( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].visible = ev.target.checked;
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the title
		var titleEdit = table.find("tr").last().children(":nth(1)").first().children().first();
		// add a listener
		addListener( titleEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].title = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the titleStyle
		var titleStyleEdit = table.find("tr").last().children(":nth(2)").first().children().first();
		// add a listener
		addListener( titleStyleEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].titleStyle = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the field
		var fieldEdit = table.find("tr").last().children(":nth(3)").first().children().first();
		// add a listener
		addListener( fieldEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].field = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the fieldStyle
		var fieldStyleEdit = table.find("tr").last().children(":nth(4)").first().children().first();
		// add a listener
		addListener( fieldStyleEdit.keyup( {grid: grid}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.grid.columns[input.parent().parent().index()-1].fieldStyle = input.val();
			// refresh the html and regenerate the mappings
			rebuildHtml(grid);
		}));
		
		// find the cellFunction
		var fieldStyleEdit = table.find("tr").last().children(":nth(5)").first();
		// add a listener
		addListener( fieldStyleEdit.click( {grid: grid}, function(ev) {
			// get the td
			var td = $(ev.target);
			// get the index
			var index = td.parent().index()-1;
			// set the index
			cellFunctionTextArea.attr("data-index",index);
			// get the function text
			var cellFunctionText = ev.data.grid.columns[index].cellFunction;
			// set the text
			if (cellFunctionText) cellFunctionTextArea.val(cellFunctionText);
			// show and focus the textarea
			cellFunctionTextArea.show().focus();
		}));
						
	}
	
	// add the cell function text area
	var cellFunctionTextArea = dialogue.append("<textarea data-index='-1' style='position:absolute;display:none;width:500px;height:300px;top:26px;right:10px;'  wrap='off'></textarea>").find("textarea:first");
	// hide it on unfocus
	addListener( cellFunctionTextArea.blur( function(ev) {		
		// get the value
		var value = cellFunctionTextArea.val();
		// update to elipses if nothing
		if (!value) value = "...";
		// get the index
		var index = cellFunctionTextArea.attr("data-index")*1;		
		// update the td text
		if (index >= 0) table.find("tr:nth(" + (index + 1) + ")").last().children("td:nth(5)").html(value);
		// empty the value
		cellFunctionTextArea.val("");
		// hide it
		cellFunctionTextArea.hide();		
	}));
	
	// update the applicable cellFunction
	addListener( cellFunctionTextArea.keyup( {grid: grid}, function(ev) {
		// get the index
		var index = cellFunctionTextArea.attr("data-index")*1;
		// update the object value
		if (index >= 0) ev.data.grid.columns[index].cellFunction = cellFunctionTextArea.val();		
	}));
	
	// add delete listeners
	var deleteImages = table.find("img.delete");
	// add a listener
	addListener( deleteImages.click( {columns: columns}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.columns.splice(input.parent().parent().index()-1,1);
		// remove row
		input.parent().parent().remove();
		// refresh the html and regenerate the mappings
		rebuildHtml(grid);
	}));
	
	// add reorder listeners
	addReorder(columns, table.find("img.reorder"), function() { 
		// refresh the html and regenerate the mappings
		rebuildHtml(grid);
		// refresh the property
		Property_gridColumns(cell, grid, {key: "columns"}, refreshHtml, dialogue); 
	});
	
	// have an add row
	table.append("<tr><td colspan='5'><a href='#'>add...</a></td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.click( {cell: cell, grid: grid, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// add a blank option
		ev.data.grid.columns.push({visible: true, title: "", titleStyle: "", field: "", fieldStyle: "", cellFunction: ""});
		// refresh
		Property_gridColumns(ev.data.cell, ev.data.grid, {key: "columns"}, ev.data.refreshHtml, ev.data.dialogue);		
	}));

}

// this is a dialogue to choose controls and specify their hints
function Property_controlHints(cell, hints, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 500, "Control hints");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	var text = "";
	// get the hint controls
	var controlHints = hints.controlHints;
	// if they don't exist or an empty array string make them an empty array
	if (!controlHints || controlHints == "[]") controlHints = [];
	
	// add a header
	table.append("<tr><td><b>Control</b></td><td><b>Action</b></td><td style='min-width:150px;max-width:150px;'><b>Hint text</b></td><td colspan='2'><b>Style</b></td></td></tr>");
		
	// loop the controls
	for (var i in controlHints) {
		
		// find the control hint
		var controlHint = controlHints[i];
		
		// find the control
		var control = getControlById(controlHint.controlId);

		// append the control name to the hints text
		if (control) text += control.name;
		
		// create the type options
		var typeOptions = "<option value='hover'" + ((controlHint.type == 'hover') ? " selected": "") + ">hover</option><option value='click'" + ((controlHint.type == 'click') ? " selected": "") + ">click</option>";
		
		// add the row
		table.append("<tr class='nopadding'><td><select class='control'><option value=''>Please select...</option>" + getControlOptions(controlHint.controlId) + "</select></td><td><select class='type'>" + typeOptions + "</select></td><td style='max-width:150px;'><span>" + controlHint.text + "</span></td><td><input value='" + controlHint.style + "'/></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
	
		// add a seperating comma to the text if not the last hint
		if (i < controlHints.length - 1) text += ",";
					
	}
	
	// if the hints text is empty
	if (!text) text = "Click to add...";
	// append the text into the hints property
	cell.text(text);
	
	// add control listeners
	var controlSelects = table.find("select.control");
	// add a listener
	addListener( controlSelects.change( {controlHints: controlHints}, function(ev) {
		// get the select
		var select = $(ev.target);
		// update the control id
		ev.data.controlHints[select.parent().parent().index()-1].controlId = select.val();
	}));
	
	// add type listeners
	var typeSelects = table.find("select.type");
	// add a listener
	addListener( typeSelects.change( {controlHints: controlHints}, function(ev) {
		// get the select
		var select = $(ev.target);
		// update the control id
		ev.data.controlHints[select.parent().parent().index()-1].type = select.val();
	}));
	
	// add text listeners
	var texts = table.find("span");
	// loop them
	texts.each(function() {		
		// get a reference to the span
		var span = $(this);
		//function Property_bigtext(cell, propertyObject, property, refreshHtml) {
		Property_bigtext(span.parent(), controlHints[span.parent().parent().index()-1], {key: "text"}, false);		
	});
	
	// add style listeners
	var styles = table.find("input");
	// add a listener
	addListener( styles.change( {controlHints: controlHints}, function(ev) {
		// get the input
		var input = $(ev.target);
		// update the control id
		ev.data.controlHints[input.parent().parent().index()-1].style = input.val();
	}));
				
	// add delete listeners
	var deleteImages = table.find("img.delete");
	// add a listener
	addListener( deleteImages.click( {controlHints: controlHints}, function(ev) {
		// get the input
		var input = $(ev.target);
		// remove from parameters
		ev.data.controlHints.splice(input.parent().parent().index()-1,1);
		// remove row
		input.parent().parent().remove();
		// refresh the html and regenerate the mappings
		rebuildHtml(controlHints);
	}));
	
	// add reorder listeners
	addReorder(controlHints, table.find("img.reorder"), function() { 
		// refresh the html and regenerate the mappings
		rebuildHtml(hints);
		// refresh the property
		Property_controlHints(cell, hints, {key: "controlHints"}, refreshHtml, dialogue); 
	});
		
	// have an add row
	table.append("<tr><td colspan='4'><a href='#'>add...</a></td></tr>");
	// get a reference to the add
	var add = table.find("tr").last().children().last().children().last();
	// add a listener
	addListener( add.click( {cell: cell, hints: hints, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// instantiate array if need be
		if (!ev.data.hints.controlHints) ev.data.hints.controlHints = [];
		// add a blank hint
		ev.data.hints.controlHints.push({controlId: "", type: "hover", text: "", style: ""});
		// refresh
		Property_controlHints(ev.data.cell, ev.data.hints, {key: "controlHints"}, ev.data.refreshHtml, ev.data.dialogue);		
	}));

}

function Property_slidePanelVisibility(cell, propertyObject, property, refreshHtml, refreshProperties) {
	// if we're holding a P (this defaulted in designerer.js)
	cell.text(propertyObject.visible);
	// add the listener to the cell
	addListener( cell.click( function(ev) {
		// add an undo snapshot
		addUndo();
		// get a reference to the slidePanel
		var slidePanel = propertyObject;
		// toggle the value
		slidePanel.visible = !slidePanel.visible;		
		// add/remove classes
		if (slidePanel.visible) {
			slidePanel.object.addClass("slidePanelOpen");
			slidePanel.object.removeClass("slidePanelClosed");
		} else {
			slidePanel.object.addClass("slidePanelClosed");
			slidePanel.object.removeClass("slidePanelOpen");
		}
		// refresh the html
		rebuildHtml(propertyObject);
		// update text
		$(ev.target).text(slidePanel.visible);
	}));
}


function Property_flowLayoutCellWidth(cell, flowLayout, property, refreshHtml, refreshProperties) {
	var value = "";
	// set the value if it exists
	if (flowLayout[property.key]) value = flowLayout[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) {
		// update the property
		updateProperty(flowLayout, property, ev.target.value, refreshHtml);
		// update the iFrame control details
		var pageWindow =  _pageIframe[0].contentWindow || _pageIframe[0];
		// add the design-time details object into the page
		pageWindow[flowLayout.id + "details"] = {};
		// set the cell width
		pageWindow[flowLayout.id + "details"].cellWidth = ev.target.value;
		// iframe resize
		_pageIframe.resize();		
	}));
}

function Property_datacopyType(cell, datacopyAction, property, refreshHtml, refreshProperties) {
	// show the type and allow the refreshing of properties
	Property_select(cell, datacopyAction, property, refreshHtml, true);
}

function Property_datacopySource(cell, datacopyAction, property, refreshHtml, refreshProperties) {
	// only if datacopyAction type is child
	if (datacopyAction.copyType == "bulk") {
		// remove this row
		cell.closest("tr").remove();		
	} else {
		// show the source drop down		
		Property_select(cell, datacopyAction, property, refreshHtml);
	}
}

function Property_datacopySourceField(cell, datacopyAction, property, refreshHtml, refreshProperties) {
	// only if datacopyAction type is child
	if (datacopyAction.copyType == "bulk") {
		// remove this row
		cell.closest("tr").remove();
	} else {
		// show the source field text		
		Property_text(cell, datacopyAction, property, refreshHtml);
	}
}

function Property_datacopyChildField(cell, datacopyAction, property, refreshHtml, refreshProperties) {
	// only if datacopyAction type is child
	if (datacopyAction.copyType == "child") {
		// show the duration
		Property_text(cell, datacopyAction, property, refreshHtml);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_datacopySearchField(cell, datacopyAction, property, refreshHtml, refreshProperties) {
	// only if datacopyAction is search
	if (datacopyAction.copyType == "search") {
		// show the duration
		Property_text(cell, datacopyAction, property, refreshHtml);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_datacopySearchSource(cell, datacopyAction, property, refreshHtml, refreshProperties) {
	// only if datacopyAction is search
	if (datacopyAction.copyType == "search") {
		// show the duration
		Property_select(cell, datacopyAction, property, refreshHtml, refreshProperties);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function getDataItemDetails(id) {
	
	// if we got an id
	if (id) {
		// get the id parts
		var idParts = id.split(".");
		// derive the id
		var itemId = idParts[0];
		// assume the name is the entire id
		var itemName = id
		// get the control
		var itemControl = getControlById(itemId);
		// if we got one
		if (itemControl) {
			// look for a "other" page name
			if (itemControl._pageName) {
				// use the page name and control name
				itemName = itemControl._pageName + "." + itemControl.name;
			} else {
				// take just the control name
				itemName = itemControl.name;
			}
			// if there's a complex key
			if (idParts.length > 1) {
				// get the class
				var controlClass = _controlTypes[itemControl.type];
				// get any run time properties
				var properties = controlClass.runtimeProperties;
				// if there are runtimeProperties in the class
				if (properties) {
					// promote if array
					if ($.isArray(properties.runtimeProperty)) properties = properties.runtimeProperty;
					// loop them
					for (var i in properties) {
						// get the property
						var property = properties[i];
						// if there's set javascript (so it can be a destination)
						if (property.setPropertyJavaScript) {
							// append the property name if the key matches
							if (idParts[1] == property.type) itemName += "." + property.name;
						}						
					}					
				}
			} 
		}
		// return an object with the name and id
		return {id:itemId, name:itemName};
	} else {
		// return an empty object
		return {id:"",name:""};
	}
}

function Property_datacopyDestinations(cell, propertyObject, property, refreshHtml, refreshDialogue) {
	
	// only if datacopyAction type is not bulk
	if (propertyObject.copyType == "bulk") {
		// remove this row
		cell.closest("tr").remove();
	} else {
		// retain a reference to the dialogue (if we were passed one)
		var dialogue = refreshDialogue;
		// if we weren't passed one - make what we need
		if (!dialogue) dialogue = createDialogue(cell, 300, "Destinations");		
		// grab a reference to the table
		var table = dialogue.find("table").first();
		// make sure table is empty
		table.children().remove();
		
		// build what we show in the parent cell
		var dataDestinations = [];
		// get the value if it exists
		if (propertyObject[property.key]) dataDestinations = propertyObject[property.key];	
		// make some text for our cell (we're going to build in in the loop)
		var text = "";
		
		// add a header
		table.append("<tr><td><b>Control</b></td><td colspan='2'><b>Field</b></td></tr>");
			
		// show current choices (with delete and move)
		for (var i = 0; i < dataDestinations.length; i++) {
			// get a single reference
			var dataDestination = dataDestinations[i];	
			// if we got one
			if (dataDestination) {
				// get a data item object for this
				var dataItem = getDataItemDetails(dataDestination.itemId);
				// apend to the text
				text += dataItem.name + ",";
				// add a row
				table.append("<tr><td>" + dataItem.name + "</td><td><input value='" + dataDestination.field + "' /></td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
				// get the field
				var editField = table.find("tr").last().children("td:nth(1)").children("input");
				// add a listener
				addListener( editField.keyup( {dataDestinations: dataDestinations}, function(ev) {
					// get the input
					var editField = $(ev.target);
					// update the field
					ev.data.dataDestinations[editField.parent().parent().index()-1].field = editField.val();
				}));
				// get the delete image
				var imgDelete = table.find("tr").last().children().last().children("img.delete");
				// add a listener
				addListener( imgDelete.click( {dataDestinations: dataDestinations}, function(ev) {
					// get the input
					var imgDelete = $(ev.target);
					// remove from parameters
					ev.data.dataDestinations.splice(imgDelete.parent().parent().index()-1,1);
					// remove row
					imgDelete.parent().parent().remove();
				}));
			} else {
				// remove this entry from the collection
				dataDestinations.splice(i,1);
				// set i back 1 position
				i--;
			}			
		}
				
		// add reorder listeners
		addReorder(dataDestinations, table.find("img.reorder"), function() { 
			Property_datacopyDestinations(cell, propertyObject, property, refreshHtml, dialogue); 
		});
		
		// add the add
		table.append("<tr><td colspan='3' style='padding:0px;'><select style='margin:0px'><option value=''>Add destination...</option>" + getOutputOptions() + "</select></td></tr>");
		// find the add
		var destinationAdd = table.find("tr").last().children().last().children().last();
		// listener to add output
		addListener( destinationAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
			// initialise array if need be
			if (!ev.data.propertyObject.dataDestinations) ev.data.propertyObject.dataDestinations = [];
			// get the parameters (inputs or outputs)
			var dataDestinations = ev.data.propertyObject.dataDestinations;
			// add a new one
			dataDestinations.push({itemId: $(ev.target).val(), field: ""});
			// rebuild the dialogue
			Property_datacopyDestinations(ev.data.cell, ev.data.propertyObject, {key: "dataDestinations"}, ev.data.refreshHtml, ev.data.dialogue);	
		}));
		
		// if we got text 
		if (text) {
			// remove the trailing comma
			text = text.substring(0,text.length - 1);
		} else {
			// add friendly message
			text = "Click to add...";
		}
		// put the text into the cell
		cell.text(text);
	}
	
}

var _dataCopyTypes = [[false,"replace"],["append","append"],["row","row merge"]];

function getCopyTypeOptions(type) {
	var options = "";
	for (var i in _dataCopyTypes) {
		options += "<option value='" + _dataCopyTypes[i][0] + "'" + (type == _dataCopyTypes[i][0] ? " selected='selected'" : "") + ">" + _dataCopyTypes[i][1] + "</option>"
	}
	return options;
}

function Property_datacopyCopies(cell, datacopyAction, property, refreshHtml, refreshDialogue) {

	// only if datacopyAction type is bulk
	if (datacopyAction.copyType == "bulk") {	
		
		// retain a reference to the dialogue (if we were passed one)
		var dialogue = refreshDialogue;
		// if we weren't passed one - make what we need
		if (!dialogue) dialogue = createDialogue(cell, 700, "Bulk data copies");		
		// grab a reference to the table
		var table = dialogue.find("table").first();
		// make sure table is empty
		table.children().remove();
		
		// build what we show in the parent cell
		var dataCopies = [];
		// get the value if it exists
		if (datacopyAction[property.key]) dataCopies = datacopyAction[property.key];	
		// make some text for our cell (we're going to build in in the loop)
		var text = "";
		
		// add a header
		table.append("<tr><td><b>Source</b></td><td><b>Source field</b></td><td><b>Destination</b></td><td><b>Destination field</b></td><td colspan='2'><b>Copy type</b></td></tr>");
			
		// show current choices (with delete and move)
		for (var i = 0; i < dataCopies.length; i++) {
			
			// get this data copy
			var dataCopy = dataCopies[i];
						
			// add a row
			table.append("<tr class='nopadding'><td><select class='source'><option value=''>Please select...</option>" + getInputOptions(dataCopy.source) + "</select></td><td><input  class='source' value='" + dataCopy.sourceField + "' /></td><td><select class='destination'><option value=''>Please select...</option>" + getOutputOptions(dataCopy.destination) + "</select></td><td><input class='destination' value='" + dataCopy.destinationField + "' /></td><td><select class='type' style='min-width:60px;'>" + getCopyTypeOptions(dataCopy.type) + "</select></td><td style='width:32px'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
			
			// get the source data item
			var source = getDataItemDetails(dataCopy.source);
			// get the destination data item
			var destination = getDataItemDetails(dataCopy.destination);
			// apend to the text
			text += source.name + " to " + destination.name + ",";
			
		}
		
		// source listeners
		addListener( table.find("select.source").change( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].source = target.val();
		}));		
		// source field listeners
		addListener( table.find("input.source").keyup( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].sourceField = target.val();
		}));
		// destination listeners
		addListener( table.find("select.destination").change( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].destination = target.val();
		}));
		// destination field listeners
		addListener( table.find("input.destination").change( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].destinationField = target.val();
		}));
		// source listeners
		addListener( table.find("select.type").change( {dataCopies: dataCopies}, function(ev) {
			// get the target
			var target = $(ev.target);
			// get the index
			var i = target.closest("tr").index() - 1;
			// update the source
			ev.data.dataCopies[i].type = target.val();
		}));
		
		// get the delete images
		var imgDelete = table.find("img.delete");
		// add a listener
		addListener( imgDelete.click( {dataCopies: dataCopies}, function(ev) {
			// get the input
			var imgDelete = $(ev.target);
			// remove from parameters
			ev.data.dataCopies.splice(imgDelete.parent().parent().index()-1,1);
			// remove row
			imgDelete.parent().parent().remove();
		}));
			
		// add reorder listeners
		addReorder(dataCopies, table.find("img.reorder"), function() { 
			Property_datacopyCopies(cell, datacopyAction, property, refreshHtml, dialogue); 
		});
		
		// add the add
		table.append("<tr><td colspan='8'><a href='#' style='margin-left:5px;'>add...</a></td></tr>");
		// find the add
		var destinationAdd = table.find("a").last();
		// listener to add output
		addListener( destinationAdd.click( {cell: cell, datacopyAction: datacopyAction, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
			// initialise array if need be
			if (!ev.data.datacopyAction.dataCopies) ev.data.datacopyAction.dataCopies = [];
			// get the parameters (inputs or outputs)
			var dataCopies = ev.data.datacopyAction.dataCopies;
			// add a new one
			dataCopies.push({source:"",sourceField:"",destination:"",destinationField:""});
			// rebuild the dialogue
			Property_datacopyCopies(ev.data.cell, ev.data.datacopyAction, {key: "dataCopies"}, ev.data.refreshHtml, ev.data.dialogue);	
		}));
		
		// if we got text 
		if (text) {
			// remove the trailing comma
			text = text.substring(0,text.length - 1);
		} else {
			// add friendly message
			text = "Click to add...";
		}
		// put the text into the cell
		cell.text(text);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}

}

function Property_controlActionType(cell, controlAction, property, refreshHtml, refreshProperties) {
	// if this property has not been set yet
	if (!controlAction.actionType) {
		// update to custom if there is a command property (this is for backwards compatibility)
		if (controlAction.command && controlAction.command != "// Enter JQuery command here. The event object is passed in as \"ev\"") {
			controlAction.actionType = "custom";
		} else {
			controlAction.actionType = "hide";
		}
	}
	// build the options
	Property_select(cell, controlAction, property, refreshHtml, refreshProperties);
	// get a reference
	var select = cell.children().last();
	// add a listener to update the property
	addListener( select.change( function(ev) {
		// get the value
		var value = ev.target.value;
		// update the property
		updateProperty(controlAction, property, value, refreshHtml);
		// rebuild all events/actions
		showEvents(_selectedControl);
	}));
}

function Property_controlActionDuration(cell, controlAction, property, refreshHtml, refreshProperties) {
	// only if controlAction is slide or fade
	if (controlAction.actionType.indexOf("slide") == 0 || controlAction.actionType.indexOf("fade") == 0) {
		// show the duration
		Property_integer(cell, controlAction, property, refreshHtml);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_controlActionClasses(cell, controlAction, property, refreshHtml, refreshProperties) {
	// only if controlAction is custom
	if (controlAction.actionType == "addClass" || controlAction.actionType == "removeClass" || controlAction.actionType == "removeChildClasses") {
		// add thegtext
		Property_select(cell, controlAction, property, refreshHtml);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_controlActionCommand(cell, controlAction, property, refreshHtml, refreshProperties) {
	// only if controlAction is custom
	if (controlAction.actionType == "custom") {
		// add the bigtext
		Property_bigtext(cell, controlAction, property, refreshHtml);
	} else {
		// remove this row
		cell.closest("tr").remove();
	}
}

function Property_slidePanelColour(cell, controlAction, property, refreshHtml, refreshProperties) {
	// add the select with refresh html = true
	Property_select(cell, controlAction, property, true);
}

var _fontawesomeGlyphs = [["&#xf042;","adjust"],["&#xf170;","adn"],["&#xf037;","align-center"],["&#xf039;","align-justify"],["&#xf036;","align-left"],["&#xf038;","align-right"],["&#xf0f9;","ambulance"],["&#xf13d;","anchor"],["&#xf17b;","android"],["&#xf209;","angellist"],["&#xf103;","angle-double-down"],["&#xf100;","angle-double-left"],["&#xf101;","angle-double-right"],["&#xf102;","angle-double-up"],["&#xf107;","angle-down"],["&#xf104;","angle-left"],["&#xf105;","angle-right"],["&#xf106;","angle-up"],["&#xf179;","apple"],["&#xf187;","archive"],["&#xf1fe;","area-chart"],["&#xf0ab;","arrow-circle-down"],["&#xf0a8;","arrow-circle-left"],["&#xf01a;","arrow-circle-o-down"],["&#xf190;","arrow-circle-o-left"],["&#xf18e;","arrow-circle-o-right"],["&#xf01b;","arrow-circle-o-up"],["&#xf0a9;","arrow-circle-right"],["&#xf0aa;","arrow-circle-up"],["&#xf063;","arrow-down"],["&#xf060;","arrow-left"],["&#xf061;","arrow-right"],["&#xf047;","arrows"],["&#xf0b2;","arrows-alt"],["&#xf07e;","arrows-h"],["&#xf07d;","arrows-v"],["&#xf062;","arrow-up"],["&#xf069;","asterisk"],["&#xf1fa;","at"],["&#xf04a;","backward"],["&#xf05e;","ban"],["&#xf080;","bar-chart"],["&#xf02a;","barcode"],["&#xf0c9;","bars"],["&#xf0fc;","beer"],["&#xf1b4;","behance"],["&#xf1b5;","behance-square"],["&#xf0f3;","bell"],["&#xf0a2;","bell-o"],["&#xf1f6;","bell-slash"],["&#xf1f7;","bell-slash-o"],["&#xf206;","bicycle"],["&#xf1e5;","binoculars"],["&#xf1fd;","birthday-cake"],["&#xf171;","bitbucket"],["&#xf172;","bitbucket-square"],["&#xf032;","bold"],["&#xf0e7;","bolt"],["&#xf1e2;","bomb"],["&#xf02d;","book"],["&#xf02e;","bookmark"],["&#xf097;","bookmark-o"],["&#xf0b1;","briefcase"],["&#xf15a;","btc"],["&#xf188;","bug"],["&#xf1ad;","building"],["&#xf0f7;","building-o"],["&#xf0a1;","bullhorn"],["&#xf140;","bullseye"],["&#xf207;","bus"],["&#xf1ec;","calculator"],["&#xf073;","calendar"],["&#xf133;","calendar-o"],["&#xf030;","camera"],["&#xf083;","camera-retro"],["&#xf1b9;","car"],["&#xf0d7;","caret-down"],["&#xf0d9;","caret-left"],["&#xf0da;","caret-right"],["&#xf150;","caret-square-o-down"],["&#xf191;","caret-square-o-left"],["&#xf152;","caret-square-o-right"],["&#xf151;","caret-square-o-up"],["&#xf0d8;","caret-up"],["&#xf20a;","cc"],["&#xf1f3;","cc-amex"],["&#xf1f2;","cc-discover"],["&#xf1f1;","cc-mastercard"],["&#xf1f4;","cc-paypal"],["&#xf1f5;","cc-stripe"],["&#xf1f0;","cc-visa"],["&#xf0a3;","certificate"],["&#xf127;","chain-broken"],["&#xf00c;","check"],["&#xf058;","check-circle"],["&#xf05d;","check-circle-o"],["&#xf14a;","check-square"],["&#xf046;","check-square-o"],["&#xf13a;","chevron-circle-down"],["&#xf137;","chevron-circle-left"],["&#xf138;","chevron-circle-right"],["&#xf139;","chevron-circle-up"],["&#xf078;","chevron-down"],["&#xf053;","chevron-left"],["&#xf054;","chevron-right"],["&#xf077;","chevron-up"],["&#xf1ae;","child"],["&#xf111;","circle"],["&#xf10c;","circle-o"],["&#xf1ce;","circle-o-notch"],["&#xf1db;","circle-thin"],["&#xf0ea;","clipboard"],["&#xf017;","clock-o"],["&#xf0c2;","cloud"],["&#xf0ed;","cloud-download"],["&#xf0ee;","cloud-upload"],["&#xf121;","code"],["&#xf126;","code-fork"],["&#xf1cb;","codepen"],["&#xf0f4;","coffee"],["&#xf013;","cog"],["&#xf085;","cogs"],["&#xf0db;","columns"],["&#xf075;","comment"],["&#xf0e5;","comment-o"],["&#xf086;","comments"],["&#xf0e6;","comments-o"],["&#xf14e;","compass"],["&#xf066;","compress"],["&#xf1f9;","copyright"],["&#xf09d;","credit-card"],["&#xf125;","crop"],["&#xf05b;","crosshairs"],["&#xf13c;","css3"],["&#xf1b2;","cube"],["&#xf1b3;","cubes"],["&#xf0f5;","cutlery"],["&#xf1c0;","database"],["&#xf1a5;","delicious"],["&#xf108;","desktop"],["&#xf1bd;","deviantart"],["&#xf1a6;","digg"],["&#xf192;","dot-circle-o"],["&#xf019;","download"],["&#xf17d;","dribbble"],["&#xf16b;","dropbox"],["&#xf1a9;","drupal"],["&#xf052;","eject"],["&#xf141;","ellipsis-h"],["&#xf142;","ellipsis-v"],["&#xf1d1;","empire"],["&#xf0e0;","envelope"],["&#xf003;","envelope-o"],["&#xf199;","envelope-square"],["&#xf12d;","eraser"],["&#xf153;","eur"],["&#xf0ec;","exchange"],["&#xf12a;","exclamation"],["&#xf06a;","exclamation-circle"],["&#xf071;","exclamation-triangle"],["&#xf065;","expand"],["&#xf08e;","external-link"],["&#xf14c;","external-link-square"],["&#xf06e;","eye"],["&#xf1fb;","eyedropper"],["&#xf070;","eye-slash"],["&#xf09a;","facebook"],["&#xf082;","facebook-square"],["&#xf049;","fast-backward"],["&#xf050;","fast-forward"],["&#xf1ac;","fax"],["&#xf182;","female"],["&#xf0fb;","fighter-jet"],["&#xf15b;","file"],["&#xf1c6;","file-archive-o"],["&#xf1c7;","file-audio-o"],["&#xf1c9;","file-code-o"],["&#xf1c3;","file-excel-o"],["&#xf1c5;","file-image-o"],["&#xf016;","file-o"],["&#xf1c1;","file-pdf-o"],["&#xf1c4;","file-powerpoint-o"],["&#xf0c5;","files-o"],["&#xf15c;","file-text"],["&#xf0f6;","file-text-o"],["&#xf1c8;","file-video-o"],["&#xf1c2;","file-word-o"],["&#xf008;","film"],["&#xf0b0;","filter"],["&#xf06d;","fire"],["&#xf134;","fire-extinguisher"],["&#xf024;","flag"],["&#xf11e;","flag-checkered"],["&#xf11d;","flag-o"],["&#xf0c3;","flask"],["&#xf16e;","flickr"],["&#xf0c7;","floppy-o"],["&#xf07b;","folder"],["&#xf114;","folder-o"],["&#xf07c;","folder-open"],["&#xf115;","folder-open-o"],["&#xf031;","font"],["&#xf04e;","forward"],["&#xf180;","foursquare"],["&#xf119;","frown-o"],["&#xf1e3;","futbol-o"],["&#xf11b;","gamepad"],["&#xf0e3;","gavel"],["&#xf154;","gbp"],["&#xf06b;","gift"],["&#xf1d3;","git"],["&#xf09b;","github"],["&#xf113;","github-alt"],["&#xf092;","github-square"],["&#xf1d2;","git-square"],["&#xf184;","gittip"],["&#xf000;","glass"],["&#xf0ac;","globe"],["&#xf1a0;","google"],["&#xf0d5;","google-plus"],["&#xf0d4;","google-plus-square"],["&#xf1ee;","google-wallet"],["&#xf19d;","graduation-cap"],["&#xf1d4;","hacker-news"],["&#xf0a7;","hand-o-down"],["&#xf0a5;","hand-o-left"],["&#xf0a4;","hand-o-right"],["&#xf0a6;","hand-o-up"],["&#xf0a0;","hdd-o"],["&#xf1dc;","header"],["&#xf025;","headphones"],["&#xf004;","heart"],["&#xf08a;","heart-o"],["&#xf1da;","history"],["&#xf015;","home"],["&#xf0f8;","hospital-o"],["&#xf0fd;","h-square"],["&#xf13b;","html5"],["&#xf20b;","ils"],["&#xf01c;","inbox"],["&#xf03c;","indent"],["&#xf129;","info"],["&#xf05a;","info-circle"],["&#xf156;","inr"],["&#xf16d;","instagram"],["&#xf208;","ioxhost"],["&#xf033;","italic"],["&#xf1aa;","joomla"],["&#xf157;","jpy"],["&#xf1cc;","jsfiddle"],["&#xf084;","key"],["&#xf11c;","keyboard-o"],["&#xf159;","krw"],["&#xf1ab;","language"],["&#xf109;","laptop"],["&#xf202;","lastfm"],["&#xf203;","lastfm-square"],["&#xf06c;","leaf"],["&#xf094;","lemon-o"],["&#xf149;","level-down"],["&#xf148;","level-up"],["&#xf1cd;","life-ring"],["&#xf0eb;","lightbulb-o"],["&#xf201;","line-chart"],["&#xf0c1;","link"],["&#xf0e1;","linkedin"],["&#xf08c;","linkedin-square"],["&#xf17c;","linux"],["&#xf03a;","list"],["&#xf022;","list-alt"],["&#xf0cb;","list-ol"],["&#xf0ca;","list-ul"],["&#xf124;","location-arrow"],["&#xf023;","lock"],["&#xf175;","long-arrow-down"],["&#xf177;","long-arrow-left"],["&#xf178;","long-arrow-right"],["&#xf176;","long-arrow-up"],["&#xf0d0;","magic"],["&#xf076;","magnet"],["&#xf183;","male"],["&#xf041;","map-marker"],["&#xf136;","maxcdn"],["&#xf20c;","meanpath"],["&#xf0fa;","medkit"],["&#xf11a;","meh-o"],["&#xf130;","microphone"],["&#xf131;","microphone-slash"],["&#xf068;","minus"],["&#xf056;","minus-circle"],["&#xf146;","minus-square"],["&#xf147;","minus-square-o"],["&#xf10b;","mobile"],["&#xf0d6;","money"],["&#xf186;","moon-o"],["&#xf001;","music"],["&#xf1ea;","newspaper-o"],["&#xf19b;","openid"],["&#xf03b;","outdent"],["&#xf18c;","pagelines"],["&#xf1fc;","paint-brush"],["&#xf0c6;","paperclip"],["&#xf1d8;","paper-plane"],["&#xf1d9;","paper-plane-o"],["&#xf1dd;","paragraph"],["&#xf04c;","pause"],["&#xf1b0;","paw"],["&#xf1ed;","paypal"],["&#xf040;","pencil"],["&#xf14b;","pencil-square"],["&#xf044;","pencil-square-o"],["&#xf095;","phone"],["&#xf098;","phone-square"],["&#xf03e;","picture-o"],["&#xf200;","pie-chart"],["&#xf1a7;","pied-piper"],["&#xf1a8;","pied-piper-alt"],["&#xf0d2;","pinterest"],["&#xf0d3;","pinterest-square"],["&#xf072;","plane"],["&#xf04b;","play"],["&#xf144;","play-circle"],["&#xf01d;","play-circle-o"],["&#xf1e6;","plug"],["&#xf067;","plus"],["&#xf055;","plus-circle"],["&#xf0fe;","plus-square"],["&#xf196;","plus-square-o"],["&#xf011;","power-off"],["&#xf02f;","print"],["&#xf12e;","puzzle-piece"],["&#xf1d6;","qq"],["&#xf029;","qrcode"],["&#xf128;","question"],["&#xf059;","question-circle"],["&#xf10d;","quote-left"],["&#xf10e;","quote-right"],["&#xf074;","random"],["&#xf1d0;","rebel"],["&#xf1b8;","recycle"],["&#xf1a1;","reddit"],["&#xf1a2;","reddit-square"],["&#xf021;","refresh"],["&#xf18b;","renren"],["&#xf01e;","repeat"],["&#xf112;","reply"],["&#xf122;","reply-all"],["&#xf079;","retweet"],["&#xf018;","road"],["&#xf135;","rocket"],["&#xf09e;","rss"],["&#xf143;","rss-square"],["&#xf158;","rub"],["&#xf0c4;","scissors"],["&#xf002;","search"],["&#xf010;","search-minus"],["&#xf00e;","search-plus"],["&#xf064;","share"],["&#xf1e0;","share-alt"],["&#xf1e1;","share-alt-square"],["&#xf14d;","share-square"],["&#xf045;","share-square-o"],["&#xf132;","shield"],["&#xf07a;","shopping-cart"],["&#xf012;","signal"],["&#xf090;","sign-in"],["&#xf08b;","sign-out"],["&#xf0e8;","sitemap"],["&#xf17e;","skype"],["&#xf198;","slack"],["&#xf1de;","sliders"],["&#xf1e7;","slideshare"],["&#xf118;","smile-o"],["&#xf0dc;","sort"],["&#xf15d;","sort-alpha-asc"],["&#xf15e;","sort-alpha-desc"],["&#xf160;","sort-amount-asc"],["&#xf161;","sort-amount-desc"],["&#xf0de;","sort-asc"],["&#xf0dd;","sort-desc"],["&#xf162;","sort-numeric-asc"],["&#xf163;","sort-numeric-desc"],["&#xf1be;","soundcloud"],["&#xf197;","space-shuttle"],["&#xf110;","spinner"],["&#xf1b1;","spoon"],["&#xf1bc;","spotify"],["&#xf0c8;","square"],["&#xf096;","square-o"],["&#xf18d;","stack-exchange"],["&#xf16c;","stack-overflow"],["&#xf005;","star"],["&#xf089;","star-half"],["&#xf123;","star-half-o"],["&#xf006;","star-o"],["&#xf1b6;","steam"],["&#xf1b7;","steam-square"],["&#xf048;","step-backward"],["&#xf051;","step-forward"],["&#xf0f1;","stethoscope"],["&#xf04d;","stop"],["&#xf0cc;","strikethrough"],["&#xf1a4;","stumbleupon"],["&#xf1a3;","stumbleupon-circle"],["&#xf12c;","subscript"],["&#xf0f2;","suitcase"],["&#xf185;","sun-o"],["&#xf12b;","superscript"],["&#xf0ce;","table"],["&#xf10a;","tablet"],["&#xf0e4;","tachometer"],["&#xf02b;","tag"],["&#xf02c;","tags"],["&#xf0ae;","tasks"],["&#xf1ba;","taxi"],["&#xf1d5;","tencent-weibo"],["&#xf120;","terminal"],["&#xf034;","text-height"],["&#xf035;","text-width"],["&#xf00a;","th"],["&#xf009;","th-large"],["&#xf00b;","th-list"],["&#xf165;","thumbs-down"],["&#xf088;","thumbs-o-down"],["&#xf087;","thumbs-o-up"],["&#xf164;","thumbs-up"],["&#xf08d;","thumb-tack"],["&#xf145;","ticket"],["&#xf00d;","times"],["&#xf057;","times-circle"],["&#xf05c;","times-circle-o"],["&#xf043;","tint"],["&#xf204;","toggle-off"],["&#xf205;","toggle-on"],["&#xf1f8;","trash"],["&#xf014;","trash-o"],["&#xf1bb;","tree"],["&#xf181;","trello"],["&#xf091;","trophy"],["&#xf0d1;","truck"],["&#xf195;","try"],["&#xf1e4;","tty"],["&#xf173;","tumblr"],["&#xf174;","tumblr-square"],["&#xf1e8;","twitch"],["&#xf099;","twitter"],["&#xf081;","twitter-square"],["&#xf0e9;","umbrella"],["&#xf0cd;","underline"],["&#xf0e2;","undo"],["&#xf19c;","university"],["&#xf09c;","unlock"],["&#xf13e;","unlock-alt"],["&#xf093;","upload"],["&#xf155;","usd"],["&#xf007;","user"],["&#xf0f0;","user-md"],["&#xf0c0;","users"],["&#xf03d;","video-camera"],["&#xf194;","vimeo-square"],["&#xf1ca;","vine"],["&#xf189;","vk"],["&#xf027;","volume-down"],["&#xf026;","volume-off"],["&#xf028;","volume-up"],["&#xf18a;","weibo"],["&#xf1d7;","weixin"],["&#xf193;","wheelchair"],["&#xf1eb;","wifi"],["&#xf17a;","windows"],["&#xf19a;","wordpress"],["&#xf0ad;","wrench"],["&#xf168;","xing"],["&#xf169;","xing-square"],["&#xf19e;","yahoo"],["&#xf1e9;","yelp"],["&#xf167;","youtube"],["&#xf16a;","youtube-play"],["&#xf166;","youtube-square"]];

function getGlyphNameByCode(code) {
	for (var i in _fontawesomeGlyphs) {
		if (_fontawesomeGlyphs[i][0] == code) return _fontawesomeGlyphs[i][1];
	}
}

function Property_glyphCode(cell, controlAction, property, refreshHtml, refreshProperties, refreshDialogue) {
	
	// retieve the glyph code
	var code = controlAction[property.key];
	// if we got one
	if (code) {
		// add it and it's name into the cell
		cell.append("<span class='fa'>" + code + "</span>&nbsp;" + getGlyphNameByCode(code));
	} else {
		// add message
		cell.append("Please select...");
	}
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Glyphs");		
	// remove the standard table
	dialogue.find("table").first().remove();
	// add a scrolling div with the table inside
	dialogue.append("<div style='overflow-y:scroll;max-height:400px;'><table></table></div>")
	// get the new table
	table = dialogue.find("table").first();
	// add all of the glyphs, with the current one highlighted
	for (var i in _fontawesomeGlyphs) {
		table.append("<tr><td data-code='" + _fontawesomeGlyphs[i][0].replace("&","&amp;") + "' class='hover'" + (code == _fontawesomeGlyphs[i][0]? " style='background-color:#aaa'" : "") + "><span class='fa'>" + _fontawesomeGlyphs[i][0] + "</span>&nbsp;" + _fontawesomeGlyphs[i][1] + "</td></tr>")
	}
	
	addListener( table.find("td").click( function(ev) {
		// get the code
		var code = $(ev.target).attr("data-code");
		// update the property
		updateProperty(controlAction, property, code, refreshHtml);
	}));
	
}

// this is used by the maps for changing the lat/lng
function Property_mapLatLng(cell, propertyObject, property, refreshHtml, refreshProperties) {
	var value = "";
	// set the value if it exists
	if (propertyObject[property.key] || parseInt(propertyObject[property.key]) == 0) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check decimal match
		if (val.match(new RegExp("^-?((\\d+(\\.\\d*)?)|(\\.\\d+))$"))) {
			// update value (but don't update the html)
			updateProperty(propertyObject, property, ev.target.value, false);
			// get a reference to the iFrame window
			var w = _pageIframe[0].contentWindow;  
			// get the map
			var map = w._maps[propertyObject.id];
			// move the centre
			map.setCenter(new w.google.maps.LatLng(propertyObject.lat, propertyObject.lng));
		} else {
			// restore value
			input.val(propertyObject[property.key]);
		}
	}));
}

function Property_mapZoom(cell, propertyObject, property, refreshHtml, refreshProperties) {
	var value = "";
	// set the value if it exists (or is 0)
	if (propertyObject[property.key] || parseInt(propertyObject[property.key]) == 0) value = propertyObject[property.key];
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	addListener( input.keyup( function(ev) {
		var input = $(ev.target);
		var val = input.val();    
		// check integer match
		if (val.match(new RegExp("^\\d+$"))) {
			// make a number
			val = parseInt(val);
			// update value but not the html
			updateProperty(propertyObject, property, val, false);
			// update the zoom
			// get a reference to the iFrame window
			var w = _pageIframe[0].contentWindow;  
			// get the map
			var map = w._maps[propertyObject.id];
			// move the centre
			map.setZoom(val);
		} else {
			// restore value
			input.val(propertyObject[property.key]);
		}
	}));
}

//this is displayed as a page property but is actually held in local storage
function Property_device(cell, propertyObject, property, refreshHtml, refreshProperties) {
	// holds the options html
	var options = "";
	// loop the array and build the options html
	for (var i in _devices) {
		// if the value is matched add selected
		if (i*1 == _device) {
			options += "<option value='" + i + "' selected='selected'>" + _devices[i].name + "</option>";
		} else {
			options += "<option value='" + i + "'>" + _devices[i].name + "</option>";
		}
	}
		
	// add the select object
	cell.append("<select class='propertiesPanelTable'>" + options + "</select>");
	// get a reference to the object
	var select = cell.children().last();
	// add a listener to update the property
	addListener( select.change( function(ev) {
		// retain the new value
		_device = $(ev.target).val() * 1;
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_device" ,_device);
		// recalculate scale
		_scale = _ppi / _devices[_device].PPI * _devices[_device].scale * _zoom;
		// hide the scroll bars to avoid artifacts during resizing
		$("#scrollV").hide();
		$("#scrollH").hide();
		// windowResize
		windowResize("_device");		
		// iframe resize
		_pageIframe.resize();
	}));
	// if value is not set, set the top value
	if (!propertyObject[property.key]) propertyObject[property.key] = select.val()*1;
	
}

//this is displayed as a page property but is actually held in local storage
function Property_zoom(cell, propertyObject, property, refreshHtml, refreshProperties) {
	// holds the options html
	var options = "";
	var values = [[0.5,"50%"],[1,"100%"],[1.5,"150%"],[2,"200%"],[3,"300%"],[4,"400%"]];
	// loop the array and build the options html
	for (var i in values) {
		// if the value is matched add selected
		if (values[i][0] == _zoom) {
			options += "<option value='" + values[i][0] + "' selected='selected'>" + values[i][1] + "</option>";
		} else {
			options += "<option value='" + values[i][0] + "'>" + values[i][1] + "</option>";
		}
	}
			
	// add the select object
	cell.append("<select class='propertiesPanelTable'>" + options + "</select>");
	// get a reference to the object
	var select = cell.children().last();
	// add a listener to update the property
	addListener( select.change( function(ev) {
		// retain the new value
		_zoom = $(ev.target).val() * 1;
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_zoom" ,_zoom);
		// recalculate scale
		_scale = _ppi / _devices[_device].PPI * _devices[_device].scale * _zoom;
		// hide the scroll bars to avoid artifacts during resizing
		$("#scrollV").hide();
		$("#scrollH").hide();
		// windowResize
		windowResize("_zoom");
	}));	
}

// this is displayed as a page property but is actually held in local storage
function Property_orientation(cell, propertyObject, property, refreshHtml, refreshProperties) {
	// if we're holding a P (this defaulted in designerer.js)
	if (_orientation == "P") {
		cell.text("Portrait");
	} else {
		cell.text("Landscape");
	}
	// add the listener to the cell
	addListener( cell.click(function(ev) {
		// toggle the value
		if (_orientation == "P") {
			_orientation = "L";
			$(ev.target).text("Landscape");
		} else {
			_orientation = "P";
			$(ev.target).text("Portrait");
		}
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_orientation" ,_orientation);
		// hide the scroll bars to avoid artifacts during resizing
		$("#scrollV").hide();
		$("#scrollH").hide();
		// windowResize
		windowResize("_orientation");
	}));
}

// possible mobileActionType values used by the mobileActionType property
var _mobileActionTypes = [["addImage","Add image"],["uploadImages","Upload images"],["showGPS","Send gps position"]];

// this property changes the visibility of other properties according to the chosen type
function Property_mobileActionType(cell, mobileAction, property, refreshHtml, refreshProperties) {
	// the selectHtml
	var selectHtml = "<select>";
	// loop the mobile action types
	for (var i in _mobileActionTypes) {
		selectHtml += "<option value='" + _mobileActionTypes[i][0] + "'" + (mobileAction.actionType == _mobileActionTypes[i][0]? " selected='selected'" : "") + ">" + _mobileActionTypes[i][1] + "</option>";
	}
	selectHtml += "</select>";
	// add the available types and retrieve dropdown
	var actionTypeSelect = cell.append(selectHtml).find("select");	
	// assume all other properties invisible
	setPropertyVisibilty(mobileAction, "galleryControlId", false);
	setPropertyVisibilty(mobileAction, "imageMaxSize", false);
	setPropertyVisibilty(mobileAction, "imageQuality", false);
	setPropertyVisibilty(mobileAction, "successActions", false);
	setPropertyVisibilty(mobileAction, "errorActions", false);
	// adjust required property visibility accordingly
	switch (mobileAction.actionType) {
		case "addImage" :
			setPropertyVisibilty(mobileAction, "galleryControlId", true);
			setPropertyVisibilty(mobileAction, "imageMaxSize", true);
			setPropertyVisibilty(mobileAction, "imageQuality", true);
		break;
		case "uploadImages" :
			setPropertyVisibilty(mobileAction, "galleryControlId", true);
			setPropertyVisibilty(mobileAction, "successActions", true);
			setPropertyVisibilty(mobileAction, "errorActions", true);
		break;
	}
	// listener for changing the type
	addListener( actionTypeSelect.change( function(ev) {
		// set the new value
		mobileAction.actionType = $(ev.target).val();
		// refresh properties (which will update the required visibilities)
		Property_mobileActionType(cell, mobileAction, property, refreshHtml, refreshProperties);
		// refresh events which is where these action properties would be
		showEvents(_selectedControl);		
	}));
}
