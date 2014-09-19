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

// this holds all the property listeners so they can be properly detached
var _listeners = [];
// this holds all the dialogue listeners for the same reason
var _dialogueListeners = [];

// this renders all the control properties in the properties panel
function showProperties(control) {
	
	// remove any listeners
	for (var i in _listeners) {
		_listeners[i].unbind();
	}
	
	// grab a reference to the properties div
	var propertiesPanel = $(".propertiesPanelDiv");
	// set the parent height to auto
	propertiesPanel.parent().css("height","auto");
	
	// if there was a control
	if (control) {
	
		// write the properties heading
		propertiesPanel.html("<h2>Properties<img id='helpProperties' class='headerHelp' src='images/help_16x16.png' /></h2>");
		// add the help hint
		addHelp("helpProperties",true);
		// append a table
		propertiesPanel.append("<table class='propertiesPanelTable'><tbody></tbody></table>");		
		// get a reference to the table
		var propertiesTable = propertiesPanel.children().last().children().last();
		// add the properties header
		propertiesTable.append("<tr><td colspan='2'><h3>" + control._class.name + "</h3></td></tr>");
		// add a small break
		propertiesTable.append("<tr><td colspan='2'></td></tr>");
		// show the control id if requested
		if (_app.showControlIds) propertiesTable.append("<tr><td>ID</td><td class='canSelect'>" + control.id + "</td></tr>");
		// check there are class properties
		var properties = control._class.properties;
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
		// create an undo snapshot just before we apply the change
		addUndo();
		// update the object property value
		propertyObject[property.key] = value;
		// if an html refresh is requested
		if (refreshHtml) {
			// in controls.js
			rebuildHtml(propertyObject);
		}	
	}
}

function setPropertyVisibilty(propertyObject, propertyKey, visibile) {
	if (propertyObject && propertyObject._class && propertyObject._class.properties) {
		var properties = propertyObject._class.properties;
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
	// create a dialogue if we need to
	var dialogue = $("#propertiesDialogues").append("<div class='actionsPanelDiv dialogue' style='position:absolute;display:none;width:" + width + "px;z-index:10012;border:1px solid black;background-color:white;font-size:11px;padding:10px;'></div>").children().last();
	// add a close link
	var close = dialogue.append("<b style='float:left;margin-top:-5px;'>" + title + "</b><a href='#' style='float:right;margin-top:-5px;'>close</a></div>").children().last();
	// note that this is not in the listeners collection so it's retained between property updates
	_dialogueListeners.push( close.click( function(ev) {
		// hide this dialogue
		$(ev.target).closest("div.dialogue").remove();
		// update the properties 
		showProperties(_selectedControl);
		// update the events
		showEvents(_selectedControl);
		// update the screen layout
		windowResize("PropertyDialogue");
	}));	
	// listener to show the dialogue
	_listeners.push( cell.click( function(ev) { 
		dialogue.css({
			"left": cell.offset().left + cell.outerWidth() - dialogue.outerWidth() + 1, 
			"top": cell.offset().top			
		});
		// show this drop down
		dialogue.slideDown(500);			
	}));
	// add an options table
	dialogue.append("<br/><table style='width:100%' class='propertiesPanelTable'><tbody></tbody></table>");
	// return
	return dialogue;	
}

//this function clears down the property dialogues
function hideDialogues() {
		
	// grab a reference to any dialogues
	var propertiesDialogues = $("#propertiesDialogues");
	// empty any propertyDialogues that we may have used before
	propertiesDialogues.children().remove();		
	
	// remove any listeners
	for (var i in _dialogueListeners) {
		_dialogueListeners[i].unbind();
	}
	
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
	_listeners.push( input.keyup( function(ev) { updateProperty(propertyObject, property, ev.target.value, refreshHtml); }));
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
	_listeners.push( cell.click( {textarea: textarea}, function(ev) { 
		textarea.css({
			"left": cell.offset().left + cell.outerWidth() - 605, 
			"top": cell.offset().top			
		});
		textarea.slideDown(500);
		// focus it so a click anywhere else fires the unfocus and hides the textbox
		textarea.focus();
	}));	
	// hide the textarea and update the cell on unfocus
	_listeners.push( textarea.blur( {cell : cell, textarea: textarea}, function(ev) {
		cell.text(textarea.val());
		textarea.hide(); 
	}));
	// modify if the text is updated
	_listeners.push( textarea.keyup( {cell : cell, textarea: textarea}, function(ev) { 
		updateProperty(propertyObject, property, textarea.val(), refreshHtml);  
	}));
	
}

function Property_select(cell, propertyObject, property, refreshHtml, refreshProperties) {
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
	_listeners.push( select.change( {refreshProperties: refreshProperties}, function(ev) {
		// apply the property update
		updateProperty(propertyObject, property, ev.target.value, refreshHtml);
		// refresh the properties if requested
		if (ev.data.refreshProperties) showProperties(_selectedControl);
	}));
	// if value is not set, set the top value
	if (!propertyObject[property.key]) propertyObject[property.key] = select.val();
	
}

function Property_checkbox(cell, propertyObject, property, refreshHtml) {
	var checked = "";
	// set the value if it exists
	if (propertyObject[property.key] && propertyObject[property.key] != "false") checked = "checked='checked'";
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' type='checkbox' " + checked + " />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	_listeners.push( input.change( function(ev) { 
		updateProperty(propertyObject, property, ev.target.checked, refreshHtml); 
	}));
}

function Property_galleryImages(cell, gallery, property, refreshHtml, refreshDialogue) {
	
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
	_listeners.push( table.find("input").keyup( function (ev) {
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
	_listeners.push( table.find("img.delete").click( function (ev) {
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
	_listeners.push( table.find("a").click( function (ev) {
		// add an image
		images.push({url:""});
		// refresh dialogue
		Property_galleryImages(cell, gallery, property, refreshHtml, dialogue);
	}));
	
}

function Property_imageFile(cell, propertyObject, property, refreshHtml, refreshDialogue) {
	
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
	if (_app.images) {
		
		// append the drop down for existing images
		table.append("<tr><td><select><option>Please select...</option></select></td></tr>");
		
		// get a reference to the drop down
		var dropdown = table.find("select");
		
		// loop the images and add to select
		for (var i in _app.images) {
			var selected = "";
			if (_app.images[i] == propertyObject.file) selected = " selected='selected'";
			dropdown.append("<option" + selected + ">" + _app.images[i] + "</option>");
		}
		
		// add change listener
		_listeners.push( dropdown.change( function (ev) {
			// get the file
			var file = $(this).val();
			// update the reference and rebuild the html
			updateProperty(propertyObject, property, file, refreshHtml); 			
		}));
		
	}
	
	// append the  form control and the submit button
	table.append("<tr><td><form id='form_" + propertyObject.id + "' method='post' enctype='multipart/form-data' target='uploadIFrame' action='designer?action=uploadImage&a=" + _app.id + "&p=" + _page.id + "&c=" + propertyObject.id + "'><input id='file_" + propertyObject.id + "' name='file' type='file'></input></form></td></tr><tr><td><input type='submit' value='Upload' /></td></tr>");
	
	// get a reference to the submit button
	_listeners.push( table.find("input[type=submit]").click( {id : propertyObject.id}, function (ev) {
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

function Property_pageName(cell, page, property, refreshHtml, refreshDialogue) {
	// get the value from the page name
	var value = page.name;
	// append the adjustable form control
	cell.append("<input class='propertiesPanelTable' value='" + value + "' />");
	// get a reference to the form control
	var input = cell.children().last();
	// add a listener to update the property
	_listeners.push( input.keyup( function(ev) {
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

function Property_validationControls(cell, propertyObject, property, refreshHtml, refreshDialogue) {
		
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
	_listeners.push( table.find("img.delete").click( function(ev) {
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
	_listeners.push( addControl.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, refreshDialogue: dialogue}, function(ev) {
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

function Property_childActions(cell, propertyObject, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Actions");		
	// grab a reference to the table
	var table = dialogue.children().last().children().last();
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
	
	_listeners.push( addAction.change( { cell: cell, propertyObject : propertyObject, property : property, refreshHtml : refreshHtml, dialogue: dialogue }, function(ev) {
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
			var action = new Action(window["ActionClass_" + actionType]);
			// add it to the array
			propertyObject[property.key].push(action);
			// set the drop down back to "Please select..."
			dropdown.val("");
			// rebuild the dialgue
			Property_childActions(ev.data.cell, propertyObject, property, ev.data.refreshHtml, ev.data.dialogue);			
		}		
	}));
			
}

// this is a dialogue to specify the inputs, sql, and outputs for the database action
function Property_databaseQuery(cell, propertyObject, property, refreshHtml, refreshDialogue) {
	
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
	table.append("<tr><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%'><tr><td><b>Input</b></td><td colspan='2'><b>Field</b></td></tr></table></td><td colspan='2' style='width:500px;padding:0px 6px 0px 0px;'><b>SQL</b><br/><textarea style='width:100%;min-height:300px;'></textarea></td><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%'><tr><td><b>Field</b></td><td colspan='2'><b>Output</b></td></tr></table></td></tr>");
	
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
		_listeners.push( fieldInput.keyup( {parameters: query.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = inputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		_listeners.push( fieldDelete.click( {parameters: query.inputs}, function(ev) {
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
	_listeners.push( inputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
	_listeners.push( sqlControl.keyup( {query: query}, function(ev) {
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
		_listeners.push( fieldOutput.keyup( {parameters: query.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = outputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		_listeners.push( fieldDelete.click( {parameters: query.outputs}, function(ev) {
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
	_listeners.push( outputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
	_listeners.push( dbConnection.change( {query: query}, function(ev) {
		// set the index value
		ev.data.query.databaseConnectionIndex = ev.target.selectedIndex;
	}));
	
	// get a reference to the test button
	var testSQL = table.find("tr").last().find("button");
	// add a listener for the database connection
	_listeners.push( testSQL.click( {query: query}, function(ev) {
		
		var data = JSON.stringify(ev.data.query);
		
		$.ajax({
	    	url: "designer?a=" + _app.id + "&action=testSQL",
	    	type: "POST",          
	    	contentType: "application/json",
	    	dataType: "json",    
	    	data: data,
	        error: function(server, status, error) { 
	        	alert(error + " : " + server.responseText); 
	        },
	        success: function(response) {
	        	alert(response.message); 		       		        	
	        }
		});
		
	}));
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
	table.append("<tr><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%'><tr><td><b>Input</b></td><td colspan='2'><b>Field</b></td></tr></table></td><td colspan='2' style='width:500px;padding:0px 6px 0px 0px;'><b>Type</b><br/><input type='radio' name='WSType' value='SOAP'/>SOAP<input type='radio' name='WSType' value='JSON'/>JSON<input type='radio' name='WSType' value='XML'/>XML/Restfull</br><b>URL</b><br/><input class='WSUrl'/></br><b>Action</b><br/><input class='WSAction'/></br><b>Body</b><br/><textarea style='width:100%;min-height:300px;' class='WSBody'></textarea></td><td colspan='2' rowspan='3' style='padding:0px;vertical-align: top;'><table style='width:100%'><tr><td><b>Field</b></td><td colspan='2'><b>Output</b></td></tr></table></td></tr>");
	
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
		_listeners.push( fieldInput.keyup( {parameters: request.inputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = inputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		_listeners.push( fieldDelete.click( {parameters: request.inputs}, function(ev) {
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
	_listeners.push( inputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
	_listeners.push( typeControls.click( {request: request}, function(ev) {
		request.type = $(ev.target).val();
	}));
	
	// find the url input box
	var actionControl = table.find("input.WSUrl");
	actionControl.val(request.url);
	// listener for the action
	_listeners.push( actionControl.keyup( {request: request}, function(ev) {
		request.url = $(ev.target).val();
	}));
	
	// find the action input box
	var actionControl = table.find("input.WSAction");
	actionControl.val(request.action);
	// listener for the action
	_listeners.push( actionControl.keyup( {request: request}, function(ev) {
		request.action = $(ev.target).val();
	}));
	
	// find the request body textarea
	var bodyControl = table.find("textarea");
	bodyControl.text(request.body);
	// listener for the body
	_listeners.push( bodyControl.keyup( {request: request}, function(ev) {
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
		_listeners.push( fieldOutput.keyup( {parameters: request.outputs}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update field value
			ev.data.parameters[input.parent().parent().index()-1].field = input.val();
		}));
		// get the delete
		var fieldDelete = outputsTable.find("tr").last().children().last().children("img.delete");
		// add a listener
		_listeners.push( fieldDelete.click( {parameters: request.outputs}, function(ev) {
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
	_listeners.push( outputAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
	_listeners.push( pageDropDown.change( {navigationAction: navigationAction}, function(ev) {
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
		_listeners.push( valueEdit.keyup( {variables: variables}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.variables[input.parent().parent().index()] = input.val();
		}));
				
		// find the delete
		var optionDelete = table.find("tr").last().children().last().children().last();
		// add a listener
		_listeners.push( optionDelete.click( {variables: variables}, function(ev) {
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
	_listeners.push( add.click( {cell: cell, page: page, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
		_listeners.push( optionDelete.click( {roles: roles, cell: cell, control: control, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
	_listeners.push( add.change( {cell: cell, control: control, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
			_listeners.push( itemEdit.change( {sessionVariables: sessionVariables}, function(ev) {
				// get the input
				var input = $(ev.target);
				// update value
				ev.data.sessionVariables[input.parent().parent().index()-1].itemId = input.val();
			}));
			
			// find the input
			var fieldEdit = table.find("input").last();
			// add a listener
			_listeners.push( fieldEdit.keyup( {sessionVariables: sessionVariables}, function(ev) {
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
		_listeners.push( valueEdit.keyup( {radiobuttons : radiobuttons, buttons: buttons}, function(ev) {
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
		_listeners.push( textEdit.keyup( {radiobuttons : radiobuttons, buttons: buttons}, function(ev) {
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
		_listeners.push( buttonDelete.click( {cell: cell, radiobuttons: radiobuttons, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
	_listeners.push( add.click( {cell: cell, radiobuttons: radiobuttons, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
		_listeners.push( optionsCodes.change( {cell: cell, radiobuttons: radiobuttons, buttons: buttons, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
			// get the value
			ev.data.radiobuttons.codes = ev.target.checked;
			// refresh
			Property_radiobuttons(ev.data.cell, ev.data.radiobuttons, {key: "buttons"}, ev.data.refreshHtml, ev.data.dialogue);
		
		}));
		
	}
	
}

//possible system values used by the Logic property
var _systemValues = ["true","false","null","online"];

//this is a dialogue to define radio buttons
function Property_logicValue(cell, action, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Value");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	// get the value
	var value = action[property.key];
	// instantiate if required
	if (!value) value = {};
	
	// make some text
	var text = "";
	// check the type
	switch (value.type) {
		case "CTL" :
			// assume there is no control
			var control = null;
			// look for the control
			if (value.controlId) getControlById(value.controlId);
			// if we don't find one just show id (could be page variable)
			text = (control ? control.name : value.controlId);
			// add the field if present
			if (value.controlField) text += "." + value.controlField;
		break;
		case "CNT" :
			if (value.constant) text = value.constant;
		break;		
		case "SYS" :
			if (value.system) text = value.system;
		break;	
	}
	// default
	if (!text) text = "Click to add...";
	// append the adjustable form control
	cell.text(text);
	
	// add a heading
	table.append("<tr><td colspan='2'><input type='radio' name='" + action.id + property.key + "' value='CTL' " + (value.type == "CTL" ? "checked='checked'" : "") + "/>Control<input type='radio' name='" + action.id + property.key + "' value='CNT' " + (value.type == "CNT" ? "checked='checked'" : "") + "/>Constant<input type='radio' name='" + action.id + property.key + "' value='SYS' " + (value.type == "SYS" ? "checked='checked'" : "") + "/>System</td></tr>");
	
	// add listers
	_listeners.push( table.find("input").change({cell: cell, action: action, property: property, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {		
		// instantiate if required
		if (!ev.data.action[ev.data.property.key]) ev.data.action[ev.data.property.key] = {};
		// set the new type 
		ev.data.action[ev.data.property.key].type = $(ev.target).val();
		// refresh the property
		Property_logicValue(ev.data.cell, ev.data.action, ev.data.property, ev.data.refreshHtml, ev.data.dialogue); 
	}));
	
	switch (value.type) {
		case "CTL" :
			// set the html
			table.append("<tr><td>Control</td><td><select>" + getDataOptions(value.controlId) + "</select></td></tr>");
			// get a reference to the select
			var select = table.find("select");
			// retain the value if we don't have one yet
			if (!value.controlId) value.controlId = select.val();			
			// add the listener
			_listeners.push( select.change( function(ev) {		
				// set the new value 
				value.controlId = $(ev.target).val();
			}));
			table.append("<tr><td>Field</td><td><input /></td></tr>");			
			// get the field
			var input = table.find("input").last();
			// set any current value
			if (value.field) input.val(value.field);
			// add the listener
			_listeners.push( input.keyup( function(ev) {		
				// set the new value 
				value.controlField = $(ev.target).val();
			}));
		break;
		case "CNT" :
			// set the html
			table.append("<tr><td>Constant</td><td><input /></td></tr>");
			// get the input
			var input = table.find("input").last();
			// set any current value
			if (value.constant) input.val(value.constant);
			// add the listeners
			_listeners.push( input.keyup( function(ev) {		
				// set the new value 
				value.constant = $(ev.target).val();
			}));
		break;
		case "SYS" :
			// assume there're no sys options
			var sysOptions = "";
			// loop those that are available
			for (var i in _systemValues) {
				// build the options string
				sysOptions += "<option" + (value.system == _systemValues[i] ? " select='selected'" : "") + ">" + _systemValues[i] + "</option>";
			}
			// set the html
			table.append("<tr><td>System value</td><td><select>" + sysOptions + "</select></td></tr>");
			// get the dropdown
			var dropdown = table.find("select").last();
			// add the listeners
			_listeners.push( dropdown.change( function(ev) {		
				// set the new value 
				value.system = $(ev.target).val();
			}));
		break;
	}
	
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
		_listeners.push( textEdit.keyup( {dropdown : dropdown, options: options}, function(ev) {
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
		_listeners.push( valueEdit.keyup( {options: options}, function(ev) {
			// get the input
			var input = $(ev.target);
			// update value
			ev.data.options[input.parent().parent().index()-1].value = input.val();
		}));
				
	}
	
	// find the deletes
	var deleteImages = table.find("img.delete");
	// add a listener
	_listeners.push( deleteImages.click( {options: options}, function(ev) {
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
	_listeners.push( add.click( {cell: cell, dropdown: dropdown, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
		_listeners.push( optionsCodes.change( {cell: cell, dropdown: dropdown, options: options, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
		table.append("<tr><td style='text-align:center;'><input type='checkbox' " + (columns[i].visible ? "checked='checked'" : "")  + " /></td><td style='padding-left:0px'><input class='text' value='" + columns[i].title + "' /></td><td style='padding-left:0px'><input class='text' value='" + columns[i].titleStyle + "' /></td><td style='padding-left:0px'><input class='text' value='" + columns[i].field + "' /></td><td style='padding-left:0px'><input class='text' value='" + columns[i].fieldStyle + "' /></td><td style='width:20px;padding-left:5px'>" + cellFunctionText.replaceAll("<","&lt;") + "</td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		
		// find the checkbox
		var visibleEdit = table.find("tr").last().children(":nth(0)").first().children().first();
		// add a listener
		_listeners.push( visibleEdit.change( {grid: grid}, function(ev) {
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
		_listeners.push( titleEdit.keyup( {grid: grid}, function(ev) {
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
		_listeners.push( titleStyleEdit.keyup( {grid: grid}, function(ev) {
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
		_listeners.push( fieldEdit.keyup( {grid: grid}, function(ev) {
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
		_listeners.push( fieldStyleEdit.keyup( {grid: grid}, function(ev) {
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
		_listeners.push( fieldStyleEdit.click( {grid: grid}, function(ev) {
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
	_listeners.push( cellFunctionTextArea.blur( function(ev) {		
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
	_listeners.push( cellFunctionTextArea.keyup( {grid: grid}, function(ev) {
		// get the index
		var index = cellFunctionTextArea.attr("data-index")*1;
		// update the object value
		if (index >= 0) ev.data.grid.columns[index].cellFunction = cellFunctionTextArea.val();		
	}));
	
	// add delete listeners
	var deleteImages = table.find("img.delete");
	// add a listener
	_listeners.push( deleteImages.click( {columns: columns}, function(ev) {
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
	_listeners.push( add.click( {cell: cell, grid: grid, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// add a blank option
		ev.data.grid.columns.push({visible: true, title: "", titleStyle: "", field: "", fieldStyle: "", cellFunction: ""});
		// refresh
		Property_gridColumns(ev.data.cell, ev.data.grid, {key: "columns"}, ev.data.refreshHtml, ev.data.dialogue);		
	}));

}


function Property_dataDestinations(cell, propertyObject, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 200, "Destinations");		
	// grab a reference to the table
	var table = dialogue.find("table").first();
	// make sure table is empty
	table.children().remove();
	
	// build what we show in the parent cell
	var dataDestinations = [];
	// get the value if it exists
	if (propertyObject[property.key]) dataDestinations = propertyObject[property.key];	
	// make some text
	var text = "";
	for (var i = 0; i < dataDestinations.length; i++) {
		var itemControl = getControlById(dataDestinations[i].itemId);
		text += (itemControl ? itemControl.name : dataDestinations[i].itemId);
		if (i < dataDestinations.length - 1) text += ",";
	}
	// if nothing add friendly message
	if (!text) text = "Click to add...";
	// put the text into the cell
	cell.text(text);
	
	// add a header
	table.append("<tr><td><b>Control</b></td><td colspan='2'><b>Field</b></td></tr>");
		
	// show current choices (with delete and move)
	for (var i = 0; i < dataDestinations.length; i++) {
		// get a single reference
		var dataDestination = dataDestinations[i];
		// derive the name
		var itemControl = getControlById(dataDestination.itemId);
		// add a row
		table.append("<tr><td>" + (itemControl ? itemControl.name : dataDestination.itemId) + "</td><td><input value='" + dataDestination.field + "' /></td><td><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
		// get the field
		var editField = table.find("tr").last().children("td:nth(1)").children("input");
		// add a listener
		_listeners.push( editField.keyup( {dataDestinations: dataDestinations}, function(ev) {
			// get the input
			var editField = $(ev.target);
			// update the field
			ev.data.dataDestinations[editField.parent().parent().index()-1].field = editField.val();
		}));
		// get the delete image
		var imgDelete = table.find("tr").last().children().last().children("img.delete");
		// add a listener
		_listeners.push( imgDelete.click( {dataDestinations: dataDestinations}, function(ev) {
			// get the input
			var imgDelete = $(ev.target);
			// remove from parameters
			ev.data.dataDestinations.splice(imgDelete.parent().parent().index()-1,1);
			// remove row
			imgDelete.parent().parent().remove();
		}));
	}
	
	// add reorder listeners
	addReorder(dataDestinations, table.find("img.reorder"), function() { 
		Property_dataDestinations(cell, propertyObject, property, refreshHtml, dialogue); 
	});
	
	// add the add
	table.append("<tr><td colspan='3' style='padding:0px;'><select style='margin:0px'><option value=''>Add destination...</option>" + getOutputOptions() + "</select></td></tr>");
	// find the add
	var destinationAdd = table.find("tr").last().children().last().children().last();
	// listener to add output
	_listeners.push( destinationAdd.change( {cell: cell, propertyObject: propertyObject, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
		// initialise array if need be
		if (!ev.data.propertyObject.dataDestinations) ev.data.propertyObject.dataDestinations = [];
		// get the parameters (inputs or outputs)
		var dataDestinations = ev.data.propertyObject.dataDestinations;
		// add a new one
		dataDestinations.push({itemId: $(ev.target).val(), field: ""});
		// rebuild the dialgue
		Property_dataDestinations(ev.data.cell, ev.data.propertyObject, {key: "dataDestinations"}, ev.data.refreshHtml, ev.data.dialogue);	
	}));
}

// this is a dialogue to choose controls and specify their hints
function Property_controlHints(cell, hints, property, refreshHtml, refreshDialogue) {
	
	// retain a reference to the dialogue (if we were passed one)
	var dialogue = refreshDialogue;
	// if we weren't passed one - make what we need
	if (!dialogue) dialogue = createDialogue(cell, 400, "Control hints");		
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
		table.append("<tr><td><select class='control'><option value=''>Please select...</option>" + getControlOptions(controlHint.controlId) + "</select></td><td><select class='type'>" + typeOptions + "</select></td><td><span>" + controlHint.text + "</span></td><td><input value='" + controlHint.style + "'/></td><td style='width:32px;'><img class='delete' src='images/bin_16x16.png' style='float:right;' /><img class='reorder' src='images/moveUpDown_16x16.png' style='float:right;' /></td></tr>");
	
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
	_listeners.push( controlSelects.change( {controlHints: controlHints}, function(ev) {
		// get the select
		var select = $(ev.target);
		// update the control id
		ev.data.controlHints[select.parent().parent().index()-1].controlId = select.val();
	}));
	
	// add type listeners
	var typeSelects = table.find("select.type");
	// add a listener
	_listeners.push( typeSelects.change( {controlHints: controlHints}, function(ev) {
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
	_listeners.push( styles.change( {controlHints: controlHints}, function(ev) {
		// get the input
		var input = $(ev.target);
		// update the control id
		ev.data.controlHints[input.parent().parent().index()-1].style = input.val();
	}));
				
	// add delete listeners
	var deleteImages = table.find("img.delete");
	// add a listener
	_listeners.push( deleteImages.click( {controlHints: controlHints}, function(ev) {
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
	_listeners.push( add.click( {cell: cell, hints: hints, refreshHtml: refreshHtml, dialogue: dialogue}, function(ev) {
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
	_listeners.push( cell.click( function(ev) {
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
	_listeners.push( select.change( function(ev) {
		// retain the new value
		_device = $(ev.target).val() * 1;
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_device" ,_device);
		// recalculate scale
		_scale = _ppi / _devices[_device].ppi * _devices[_device].scale * _zoom;
		// windowResize
		windowResize("_device");
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
	_listeners.push( select.change( function(ev) {
		// retain the new value
		_zoom = $(ev.target).val() * 1;
		// store it
		if (typeof(localStorage) !== "undefined") localStorage.setItem("_zoom" ,_zoom);
		// recalculate scale
		_scale = _ppi / _devices[_device].ppi * _devices[_device].scale * _zoom;
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
	_listeners.push( cell.click(function(ev) {
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
	// adjust required property visibility accordingly
	switch (mobileAction.actionType) {
		case "addImage" :
			setPropertyVisibilty(mobileAction, "galleryControlId", true);
		break;
	}
	// listener for changing the type
	_listeners.push( actionTypeSelect.change( function(ev) {
		// set the new value
		mobileAction.actionType = $(ev.target).val();
		// refresh properties (which will update the required visibilities)
		Property_mobileActionType(cell, mobileAction, property, refreshHtml, refreshProperties);
		// refresh events which is where these action properties would be
		showEvents(_selectedControl);		
	}));
}
