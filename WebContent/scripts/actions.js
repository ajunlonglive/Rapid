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

// a reference to the control we're currently seeing the actions for
var _eventsControl = null;

// this holds the option values with all available options for adding to selects
var _actionOptions = "";

//this object function serves as a closure for holding the static values required to construct each type of action - they're created and assigned globally when the designer loads and originate from the .action.xml files
function ActionClass(actionClass) {
	// retain all values passed in the json for the action (from the .action.xml file)
	for (var i in actionClass) this[i] = actionClass[i];	
}

//this object function will create the control as specified in the controlClass
function Action(actionClass, jsonAction, paste, undo) {
			
	// if controlClass is a string find the object (so bespoke contructJavaScript can avoid the "ActionClass_" prefix and use code like: this.childControls.push(new Control("tableRow", this));)
	if ($.type(actionClass) == "string") actionClass = window["ActionClass_" + actionClass];
	// check controlClass exists
	if (actionClass) {
		
		// retain the action class
		this._class = actionClass;
		
		// retain the type
		this.type = actionClass.type;
		
		// retain the version
		this.version = actionClass.version;
				
		// if we were given properties retain then and avoid initialisation
		if (jsonAction) {
			
			// if we're pasting we don't have a properties collection
			if (paste || undo) {
				// copy all of the properties into the new action
				for (var i in jsonAction) this[i] = jsonAction[i];
				// when pasting use incremented ids
				if (paste) {
					// set a unique ID for this control (the under score is there to stop C12 being replaced by C1)
					this.id = _page.id + "_A" + _nextId + "_";
					// check the pasteMap
					if (_pasteMap[jsonAction.id]) {
						this.id = jsonAction.id;
					} else {
						// add an entry in the pastemap
						_pasteMap[this.id] = jsonAction.id;
					}
					// inc the next id
					_nextId++;
				}
				// when undoing make sure the next id is higher than all others
				if (undo) {
					// get the id value into a variable
					var id = this.id;
					// the id will be something like P99_C12, find the number after the _C
					var idInt = parseInt(id.substr(id.indexOf("_C") + 2));
					// set the next id to one past this if it is less
					if (idInt >= _nextId) _nextId = idInt + 1;	
				}
			} else {
				// copy all of the properties into the new action (except for the id, type, and properties collection), childActions need instanitating too
				for (var i in jsonAction) {
					// these three properties are special and are ignored
					if (i != "id" && i != "type" && i != "properties") {				
						// check whether we have a Rapid Object 
						if (jsonAction[i].type && window["ActionClass_" + jsonAction[i].type]) {
							// this is a simple object, instantiate here
							this[i] = new Action(window["ActionClass_" + jsonAction[i].type], jsonAction[i]);
						} else if ($.isArray(jsonAction[i]) && jsonAction[i].length > 0 && jsonAction[i][0].type && window["ActionClass_" + jsonAction[i][0].type]) {
							// this is an array of objects
							this[i] = [];
							// loop array
							for (var j in jsonAction[i]) {
								this[i].push(new Action(window["ActionClass_" + jsonAction[i][j].type], jsonAction[i][j]) );
							}
						} else {							
							// simple property copy
							this[i] = jsonAction[i];
						}	
					} // not id, type, properties
				}
				// loop and retain the properties in the save control properties collection directly
				for (var i in jsonAction.properties) {	
					// get the property
					var p = jsonAction.properties[i];
					// if it looks like an array parse it with JSON
					if (p && p.length >= 2 && p.substr(0,1) == "[" && p.substr(p.length-1,1) == "]") p = JSON.parse(p);						
					// retain the property in the control class
					this[i] = p;
					// make sure the id's are always unique 
					if (i == "id") {	
						// get the id value into a variable
						var id = jsonAction.properties["id"];
						// the id will be something like P99_A12, find the number after the _C
						var idInt = parseInt(id.substr(id.indexOf("_A") + 2));
						// set the next id to one past this if it is less
						if (idInt >= _nextId) _nextId = idInt + 1;					
					}
				}
			} // if paste
						
		} else {
			
			// set a unique ID for this action (with the final underscore the stops C12 being replaced by C1)
			this.id = _page.id + "_A" + _nextId + "_";
			
			// set a name for this action (eventually we will count the number of each type and use that)
			this.name = actionClass.name + " " + _nextId;	
			
			// if the action class has properties set them here
			if (actionClass.properties) {
				// get a reference to the properties
				var properties = actionClass.properties;
				// due to the JSON library this is the array
				if ($.isArray(properties.property)) properties = properties.property;
				for (var i in properties) {
					// get a reference to the property
					var property = properties[i];
					// if there is a setConstruct value function
					if (property.setConstructValueFunction) {
						// run the function
						var setValueFunction = new Function(property.setConstructValueFunction);
						this[property.key] = setValueFunction.apply(this,[]);
					} else if (!this[property.key]) {
						// set empty property if not already there
						this[property.key] = null;
					}
				}
			}														
			
			// inc the next id
			_nextId++;
							 																						
		} 
						
	} else {
		alert("ActionClass could not be found");
	}
	
	return this;
}

// this shows the events for the control and eventually the actions
function showEvents(control) {	
	// get a reference to the div we are writing in to
	var actionsPanel = $("#actionsPanelDiv");	
	// empty it
	actionsPanel.html("");	
	// only if there is a control and there are events in the control class
	if (control && control._class.events) {
		// get a reference to the events
		var events = control._class.events;
		// JSON library single member check
		if ($.isArray(control._class.events.event)) events = control._class.events.event;		
		// loop them
		for (var i in events) {
			// get a reference
			var event = events[i];
			// append a table
			actionsPanel.append("<table class='propertiesPanelTable'><tbody></tbody></table>");	
			// get a reference to the table
			var actionsTable = actionsPanel.children().last().children().last();
			// add a heading for the event
			actionsTable.append("<tr><td colspan='2'><h3>" + event.name + " event</h3></td></tr>");
			// show any actions
			showActions(control, event);	
			// add a small break
			actionsTable.append("<tr><td colspan='2'></td></tr>");
			// add an add facility
			actionsTable.append("<tr><td>Add action : </td><td><select data-event='" + event.type + "'><option value='_'>Please select...</option>" + _actionOptions + "</select></td></tr>");
			// get a reference to the select
			var addAction = actionsTable.children().last().children().last().children().last();
			// add a change listener
			_listeners.push( addAction.change( { control: control, event: event }, function(ev) {
				// get a reference to the control
				var control = ev.data.control;
				// get a reference to the eventType
				var eventType = ev.data.event.type;
				// look for the events collection in the control
				for (var i in control.events) {
					// check whether this is the event we want
					if (control.events[i].type == eventType) {
						// get the type of action we selected
						var actionType = $(ev.target).val();
						// add a new action of this type to the event
						control.events[i].actions.push( new Action(window["ActionClass_" + actionType]));
						// rebuild actions
						showEvents(_selectedControl);
						// we're done
						break;
					}
				}				
			}));
		}			
		
	}
	
}

// this renders a single action into a table (used by events and childActions)
function showAction(actionsTable, action, collection, refreshFunction) {
	
	// for some reason actions can lose their class, for now we'll just look them up and but the class back
	if (!action._class && action.type) {
		action._class = window["ActionClass_" + action.type];
	}
	
	// write action name into the table						
	actionsTable.append("<tr><td colspan='2'><h4>" + window["ActionClass_"+ action._class.type].name + " action</h4><img class='delete' src='images/bin_16x16.png' title='Delete this action'/><img class='reorder' src='images/moveUpDown_16x16.png' title='Reorder this action'/></td></tr>");
	// get a reference to the delete image
	var deleteImage = actionsTable.find("img.delete").last(); 
	// add a click listener to the delete image
	_listeners.push( deleteImage.click( {action: action, collection: collection, refreshFunction: refreshFunction}, function(ev) {
		// loop the collection
		for (var i in collection) {
			// if we've found the object
			if (action === collection[i]) {
				// add an undo snapshot
				addUndo();
				// remove from collection
				collection.splice(i,1);
				// refresh (if provided)
				if (refreshFunction) refreshFunction();
				// rebuild actions
				showEvents(_selectedControl);
				// we're done
				break;
			}
		}		
	}));
	// show the id if this is the rapid application
	if (_app.showActionIds) actionsTable.append("<tr><td>ID</td><td class='canSelect'>" + action.id + "</td></tr>");
	// get the action class properties
	var properties = action._class.properties;
	// check
	if (properties) {
		// (if a single it's a class not an array due to JSON class conversionf from xml)
		if ($.isArray(properties.property)) properties = properties.property; 
		// loop them
		for (var k in properties) {
			// add a row
			actionsTable.append("<tr></tr>");
			// get a reference to the row
			var propertiesRow = actionsTable.children().last();
			// retrieve a property object from the control class
			var property = properties[k];
			// check that visibility is not explicitly false
			if (property.visible === undefined || !property.visible === false) {
				// get the property itself from the action
				propertiesRow.append("<td>" + property.name + "</td><td></td>");
				// get the cell the property update control is going in
				var cell = propertiesRow.children().last();
				// apply the property function if it starts like a function or look for a known Property_[type] function and call that
				if (property.changeValueJavaScript.trim().indexOf("function(") == 0) {
					try {
						var changeValueFunction = new Function(property.changeValueJavaScript);
						changeValueFunction.apply(this,[cell, action, property]);
					} catch (ex) {
						alert("Error - Couldn't apply changeValueJavaScript for " + action.name + "." + property.name + " " + ex);
					}
				} else {
					if (window["Property_" + property.changeValueJavaScript]) {
						window["Property_" + property.changeValueJavaScript](cell, action, property);
					} else {
						alert("Error - There is no known Property_" + property.changeValueJavaScript + " function");
					}
				}			
			} // visibility check
		} // properties loop
	} // properties check
}

// this renders the actions for a control's event into a properties panel
function showActions(control, event) {
	// if this control has events
	if (control.events) {
		// get a reference to the div we are writing in to
		var actionsPanel = $("#actionsPanelDiv");		
		// loop control events
		for (var i in control.events) {
			// if we've found the event we want
			if (control.events[i].type == event.type) {
				// get actions
				var actions = control.events[i].actions;
				// check there are actions				
				if (actions) {
					// get a reference to the table
					var actionsTable = actionsPanel.children().last().children().last();
					// remember how many actions we have
					var actionsCount = 0;
					// loop the actions
					for (var j in actions) {
						// inc the count
						actionsCount ++;
						// get the action
						var action = actions[j];											
						// add a small break
						actionsTable.append("<tr><td colspan='2'></td></tr>");
						// show the action
						showAction(actionsTable, action, actions);
					} // actions loop
					// if there was more than 1 action
					if (actionsCount > 1) {
						// add reorder listeners
						addReorder(actions, actionsTable.find("img.reorder"), function() { showEvents(control); });
					}
				}						
				// no need to keep looping events
				break;
			} // event match
		} // event loop		
	} //events

}