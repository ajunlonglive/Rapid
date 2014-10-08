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

// these are for loading into the validation type drop down and looking up the regEx afterwards to store in the validation object
var _validationTypes = [
  {value:'',text:'none',regex:''},                       
  {value:'value',text:'any value',regex:'[\\s\\S]'},
  {value:'number',text:'number',regex:'^\\d+\\.?\\d*$'},
  {value:'integer',text:'integer',regex:'^\\d+$'},
  {value:'date',text:'any date',regex:'[\\s\\S]'}, 
  {value:'currency',text:'currency',regex:'^\\d+\\.?\\d{2}$'},
  {value:'email',text:'email',regex:'^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$'},
  {value:'custom',text:'custom',regex:''},
  {value:'javascript',text:'javascript',regex:''}
  ];

function getValidationOptions(type) {
	var options = "";
	for (var i in _validationTypes) {
		options += "<option value='" + _validationTypes[i].value + "' " + (_validationTypes[i].value == type ? "selected='selected'" : "") + ">" + _validationTypes[i].text + "</option>";
	}
	return options;
}

function getRegEx(type) {
	for (var i in _validationTypes) {
		if (_validationTypes[i].value == type) return _validationTypes[i].regex; 
	}
}

// this function iteratively gets the select options for all controls with validation 
function getValidationControlOptions(control) {
	var options = "";
	if (control) {
		if (control.validation && control.validation.type) options += "<option value='" + control.id + "'>" + control.name + "</option>";
		if (control.childControls && control.childControls.length > 0) {
			for (var i in control.childControls) {
				options += getValidationControlOptions(control.childControls[i]);
			}
		}
	} else {
		for (var i in _page.childControls) {
			options += getValidationControlOptions(_page.childControls[i]);
		}
	}
	return options;
}

function showValidation(control) {
	
	// get a reference to the div we are writing in to
	var validationPanel = $(".validationPanelDiv");	
	// empty it
	validationPanel.html("");	
	// only if a control and it can validate
	if (control && _controlTypes[control.type].canValidate) {
		// create a validation object if we don't have one
		if (!control.validation) {
			control.validation = {
				type: "",
				allowNulls: false,
				regEx: "",
				message: "",
				javaScript: null
			};
		}
		// retain a reference to the validation object
		var validation = control.validation;
		// retrieve the type
		var type = validation.type;
		// set the regEx just for good measure (as long as not custom)
		if (type != "custom") validation.regEx = getRegEx(type);
				
		// append a table
		validationPanel.append("<table class='propertiesPanelTable'><tbody></tbody></table>");	
		// get a reference to the table
		var validationTable = validationPanel.children().last().children().last();
		// add a heading for the event
		validationTable.append("<tr><td colspan='2'><h3>Validation</h3></td></tr>");
		// add a small break
		validationTable.append("<tr><td colspan='2'></td></tr>");
		// add a type drop down
		validationTable.append("<tr><td>Type</td><td><select>" + getValidationOptions(type) + "</select></td></tr>");
		// get a reference to the type drop down
		var typeDropDown = validationTable.children().last().children().last().children().last();
		// add a listener
		_listeners.push( typeDropDown.change( function(ev) {
			// get the selected type
			var type = $(ev.target).val();
			// set the validation type
			_selectedControl.validation.type = type;
			// set the regex (this takes into account custom and javascript have no regex)
			_selectedControl.validation.regEx = getRegEx(type); 			
			// refresh the validation
			showValidation(_selectedControl);
			}
		));		
		// if the type is not "none"
		if (type) {
			switch (type) {
			case "custom" :
				// add a javascript box
				validationTable.append("<tr><td>RegEx</td><td>" + validation.regEx + "</td></tr>");
				// get a reference to  the cell
				var cell = validationTable.children().last().children().last();
				// add a bigtext property listener	
				Property_bigtext(cell, validation, {key: "regEx"});
			break;			
			case "javascript" :
				// add a javascript box
				validationTable.append("<tr><td>JavaScript</td><td></td></tr>");
				// get a reference to  the cell
				var cell = validationTable.children().last().children().last();
				// set a helpful default value for the
				if (!validation.javaScript) validation.javaScript = "// Enter your JavaScript here, return a message if the validation fails. The control value is available through the \"value\" variable";
				// add a bigtext property listener	
				Property_bigtext(cell, validation, {key: "javaScript"});
			break;
			}
			// message is in the javascript so no need for it here (can null check there too)
			if (type != "javascript") {
				// add a message box
				validationTable.append("<tr><td>Message</td><td></td></tr>");
				// get a reference to  the cell
				var cell = validationTable.children().last().children().last();
				// add a bigtext property listener	
				window["Property_bigtext"](cell, validation, {key: "message"});
				// add a allowNulls checkbox
				validationTable.append("<tr><td>Allow empty value</td><td></td></tr>");
				// get a reference to  the cell
				var cell = validationTable.children().last().children().last();
				// add a checkbox property listener	
				Property_checkbox(cell, validation, {key: "allowNulls"});
			}
			
		}
							
	}
		
}