
/* This file is auto-generated on application load and save */


/* Action methods */


function Action_control(actions) {
	for (var i in actions) {
		var action = actions[i];
		$("#" + action.id)[action["function"]](action.value);
	}	
}

function Action_datacopy(data, outputs) {
	if (outputs) {
		for (var i in outputs) {
			var output = outputs[i];			
			window["setData_" + output.type](output.id, data, output.field, output.details);
		}
	}
}

function Action_database(data, outputs) {
	if (data && outputs) {
		for (var i in outputs) {
			var output = outputs[i];			
			window["setData_" + output.type](output.id, data, output.field, output.details);
		}
	}
}

function Action_navigate(url, dialogue) {
	if (dialogue) {
	
		var bodyHtml = "<center><h1>Page</h1></center>";
		
		// request the page		
		$.ajax({
		   	url: url,
		   	type: "GET",          
		       data: null,        
		       error: function(server, status, error) { 
		       	var bodyHtml = "Error loading page : " + error; 
		       },
		       success: function(page) {
		       	
		       // if the page can't be found a blank response is sent, so only show if we got something
		       if (page) {
		       	
		       		// empty the body html
		       		bodyHtml = "";
		       		script = "";
		       		
		           	// loop the items
		           	var items = $(page);
		           	for (var i in items) {
		           		// check for a script node
		           		switch (items[i].nodeName) {
		           		case "#text" : case "TITLE" : // ignore these types
		           		break;
		           		case "SCRIPT" :
		           			if (items[i].innerHTML) {
		           				script += items[i].outerHTML;
		           			}
		           		break;
		           		default :
		           			if (items[i].outerHTML) {
		           				// retain the script in our body html
		           				bodyHtml += items[i].outerHTML;        				
		           			}
		           		break;
		           		}
		           	}   
		           	
		           	var doc = $(document);
		           	var body = $("body");
		           	var dialogueCover = body.append("<div class='dialogueCover' style='position:absolute;left:0px;top:0px;z-index:100;'></div>").children().last();
		           	// size and show the dialogueCover
	            	dialogueCover.css({
	            		width : doc.width(),
	            		height : doc.height()
	            	}).show();
		           	var dialogue = body.append("<div class='dialogue' style='position:absolute;z-index:101;'></div>").children().last();
		           	// apply the injected html
		           	dialogue.hide().html(bodyHtml);
		           	// add script into the page (if applicable)
		           	if (script) dialogue.append(script);
		           	var win = $(window);
		           	// size and show the dialogue
		           	dialogue.show().css({
		           		position : "fixed",
	            		left : (win.width() - dialogue.outerWidth()) / 2,
	            		top : (win.height() - dialogue.outerHeight()) / 3
	            	}); 
		           	           	        	            	            	            
		    	}        	       	        	        	        	        		
		    }       	        	        
		});	      
	
	} else {
		window.location = url;
	}
}

function Action_validation(ev, validations, showMessages) {
	var valid = true;
	for (var i in validations) {
		var validation = validations[i];
		var value = window["getData_" + validation.controlType](ev, validation.controlId, validation.field, validation.details);
		if (validation.validationType == "javascript") {						
			var validationFunction = new Function("value", validation.javaScript);
			var failMessage = validationFunction.apply(this, [value]);
			if (failMessage) {
				if (showMessages) showValidationMessage(validation.controlId, failMessage);
				valid = false;
			} else {
				if (showMessages) hideValidationMessage(validation.controlId);
			}
		} else {
			if ((value && value.match(new RegExp(validation.regEx)))||(!value && validation.allowNulls)) {
				// passed
				if (showMessages) hideValidationMessage(validation.controlId);				
			} else {
				// failed
				if (showMessages) showValidationMessage(validation.controlId, validation.message);
				valid = false;					
			}	
		}	
	}	
	return valid;
}

function Action_webservice(data, outputs) {
	if (data && outputs) {
		for (var i in outputs) {
			var output = outputs[i];			
			window["setData_" + output.type](output.id, data, output.field, output.details);
		}
	}
}


/* Control initialise methods */


function Init_date(id, details) {
  A_TCALCONF.format = details.dateFormat;	     
  f_tcalAdd (id);
}

function Init_pagePanel(id, details) {
  var bodyHtml = "<center><h1>Page</h1></center>";
  
  // request the page		
  $.ajax({
     	url: "~?a=" + details.appId + "&p=" + details.pageId,
     	type: "GET",          
         data: null,        
         error: function(server, status, error) { 
         	var bodyHtml = "Error loading page : " + error; 
         },
         success: function(page) {
         	
         // if the page can't be found a blank response is sent, so only show if we got something
         if (page) {
         	
         		// empty the body html
         		bodyHtml = "";
         		script = "";
         		
             	// loop the items
             	var items = $(page);
             	for (var i in items) {
             		// check for a script node
             		switch (items[i].nodeName) {
             		case "#text" : case "TITLE" : // ignore these types
             		break;
             		case "SCRIPT" :
             			if (items[i].innerHTML) {
             				script += items[i].outerHTML;
             			}
             		break;
             		default :
             			if (items[i].outerHTML) {
             				// retain the script in our body html
             				bodyHtml += items[i].outerHTML;        				
             			}
             		break;
             		}
             	}   
             	// apply the injected html
             	$("#" + id).html(bodyHtml);
             	// add script into the page (if applicable)
             	if (script) $("#" + id).append(script);
             	           	        	            	            	            
      	}        	       	        	        	        	        		
      }       	        	        
  });
}

function Init_tabGroup(id, details) {
  $("#" + id).find("li").each( function() {
  	$(this).click( function(ev, index) {
  		// remove selected from all tab header items
  		$("#" + id + " li").removeClass("selected");
  		// remove selected from all tab body items
  		$("#" + id + " div").removeClass("selected");
  		// add selected to the li we just clicked on, also get it's index, plus 2, 1 to go from zero to 1 based, the other 1 because of the headers
  		var index = $(this).addClass("selected").index() + 2;
  		// apply selected to the correct body
  		$("#" + id + " div:nth-child(" + index + ")").addClass("selected");
  	});
  });
}


/* Control getData and setData methods */


function getData_checkbox(ev, id, field, details) {
  return $("#" + id).prop("checked") ? "true" : "false";
}

function setData_checkbox(id, data, field, details) {
  var control = $("#" + id);	        
  if (data) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					control.prop('checked', data.rows[0][i]);
  					break;
  				}
  			}
  		} else {
  			control.prop('checked', data.rows[0][0]);
  		}
  	} else {
  		control.prop('checked', false);
  	}
  } else {
  	control.prop('checked', false);
  }
}

function getData_dataStore(ev, id, field, details) {
  var dataStore;
  switch (details.storageType) {
  	case "S": 
  	dataStore = sessionStorage;
  	break;
  	case "L":
  	dataStore = localStorage;
  	break;
  }  
  if (dataStore) {
  	if (details.id) id = details.id;
  	var data = null;
  	var dataString = dataStore[id];
  	if (dataString) {
  		var data = JSON.parse(dataString);
  		if (data) {		
  			if (data.rows && data.fields) {
  				if (data.rows[0] && field) {
  					for (var i in data.fields) {
  						if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  							return data.rows[0][i];
  						}
  					}
  				} else {
  					return data;
  				}
  			} else if (field && data[field]) {
  				return data[field];
  			}
  		}	 
  	} 
  	return null;		
  }
}

function setData_dataStore(id, data, field, details) {
  var dataStore;
  switch (details.storageType) {
  	case "S": 
  	dataStore = sessionStorage;
  	break;
  	case "L":
  	dataStore = localStorage;
  	break;
  } 	   
  if (dataStore) {
  	if (details.id) id = details.id;
  	if (data != null && data !== undefined) {
  		data = makeDataObject(data, field);
  		if (dataStore[id]) data = mergeDataObjects(data, JSON.parse(dataStore[id]));		
  		dataStore[id] = JSON.stringify(data);
  	} else {
  		dataStore[id] = null;
  	}
  }
}

function getData_date(ev, id, field, details) {
  return $("#" + id).val();
}

function setData_date(id, data, field, details) {
  var control = $("#" + id);
  var value = "";
  if (data) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];
  					break;
  				}
  			}
  		} else {
  			value = data.rows[0][0];
  		}
  	} 
  }      
  if (value) {
  	var date = f_tcalParseDate(value,'Y-m-d');
  	if (date) value = f_tcalGenerateDate(date, details.dateFormat);
  }
  control.val(value);
}

function getData_dropdown(ev, id, field, details) {
  return $("#" + id).val();
}

function setData_dropdown(id, data, field, details) {
  if (data) {
  	var control = $("#" + id);
  	data = makeDataObject(data, field);
  	if (data.rows && data.fields) {
  		if (field && data.rows[0]) {	      
  			var foundField = false;  	
  			for (var i in data.fields) {
  				if (field.toLowerCase() == data.fields[i].toLowerCase()) {
  					control.val(data.rows[0][i]);
  					foundField = true;
  					break;
  				}
  			}				
  			if (!foundField) control.val(data.rows[0][0]);
  		} else {
  			for (var i in data.rows) {
  				var row = data.rows[i];		
  				var text = "";
  				var value = "";
  				if (data.fields) {
  					for (var j in data.fields) {
  						if (data.fields[j].toLowerCase() == "text") text = data.rows[i][j];
  						if (data.fields[j].toLowerCase() == "value") value = data.rows[i][j];
  					}
  				}
  				if (!text) text = row[0];
  				if (!value && row[1]) value = row[1];
  				if (value || value == "0") value = 	" value='" + value + "'";
  				control.append("<option " + value + ">" + text + "</option>");
  			}	
  		}
  	} 
  }
}

function getData_grid(ev, id, field, details) {
  var data = null;
  if (details && details.columns) {
  	if (field) {
  		var row = $(ev.target).closest("tr");
  		var rowIndex = row.index() - 1;
  		if (rowIndex >= 0) {
  			for (var i in details.columns) {
  				if (details.columns[i].field.toLowerCase() == field.toLowerCase()) {
  					data = row.children(":nth(" + i + ")").html();
  					break;
  				}
  			}
  		}	    
  	} else {
  		var data = {};
  		data.fields = [];		
  		for (var i in details.columns) {	
  			data.fields.push(details.columns[i].field);		
  		}
  		data.rows = [];
  		$("#" + id).find("tr:not(:first)").each(function(i) {
  			var row = [];
  			$(this).children().each(function(i) {
  				row.push($(this).html());
  			});
  			data.rows.push(row);
  		});
  	}	
  }
  return data;
}

function setData_grid(id, data, field, details) {
  var control = $("#" + id);
  control.find("tr:not(:first)").remove();	        
  if (data) {	
  	data = makeDataObject(data, field);
  	if (data.rows) {	        		
  		if (details && details.columns && data.fields) {
  			var columnMap = [];
  			for (var i in details.columns) {				
  				for (var j in data.fields) {
  					if (details.columns[i].field.toLowerCase() == data.fields[j].toLowerCase()) {
  						columnMap.push(j);
  						break;
  					}
  				}
  				if (columnMap.length == i)
  					columnMap.push("");
  				if (details.columns[i].cellFunction) 
  					details.columns[i].cellFunction = new Function(details.columns[i].cellFunction);
  			}
  			for (var i in data.rows) {
  				var row = data.rows[i];
  				var rowObject = control.append("<tr class='rowStyle" + (i % 2 + 1) + "'></tr>").find("tr:last");
  				for (var j in details.columns) {
  					var cellObject = rowObject.append("<td style='" + (details.columns[j].visible ? "" : "display:none;") + details.columns[j].style + "'>" + ((columnMap[j]) ? row[columnMap[j]] : "") + "</td>").find("td:last");
  					if (details.columns[j].cellFunction) 
  						details.columns[j].cellFunction.apply(cellObject,[id, data, field, details]);
  				}				
  			}
  		} else {
  			for (var i in data.rows) {
  				var row = data.rows[i];
  				var rowHtml = "<tr>";
  				for (var j in row) {
  					rowHtml += "<td>" + row[j] + "</td>";
  				}
  				rowHtml += "</tr>";
  				control.append(rowHtml);
  			}
  		}	
  	} 
  	
  	control.children().last().children("tr:not(:first)").click( function() { 
  		var row = $(this);
  		row.parent().find("tr.rowSelect").each( function() {
  			var row = $(this);
  			row.removeClass("rowSelect");
  		});
  		row.addClass("rowSelect"); 
  	});
  	
  }
}

function getData_input(ev, id, field, details) {
  return $("#" + id).val();
}

function setData_input(id, data, field, details) {
  var control = $("#" + id);
  if (data) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					control.val(data.rows[0][i]);
  					break;
  				}
  			}
  		} else {
  			control.val(data.rows[0][0]);
  		}
  	} else {
  		control.val("");
  	}
  } else {
  	control.val("");
  }
}

function getData_radiobuttons(ev, id, field, details) {
  return $("#" + id).children("input[type=radio]:checked").val();
}

function setData_radiobuttons(id, data, field, details) {
  if (data != null) {
  	var radiobuttons = $("#" + id);
  	radiobuttons.children("input[type=radio]").prop('checked',false);
  	data = makeDataObject(data, field);
  	var value = null;
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					value = data.rows[0][i];					
  					break;
  				}
  			}
  		} else {
  			value = data.rows[0][0];
  		}
  	} 
  	if (value) {
  		radiobuttons.children("input[type=radio][value=" + value + "]").prop('checked',true);
  	}
  }
}

function getData_text(ev, id, field, details) {
  return $("#" + id).html();
}

function setData_text(id, data, field, details) {
  var control = $("#" + id);	        
  if (data) {	
  	data = makeDataObject(data, field);
  	if (data.rows && data.rows[0]) {	        		
  		if (field && data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i].toLowerCase() == field.toLowerCase()) {
  					control.html(data.rows[0][i]);
  					break;
  				}
  			}
  		} else {
  			control.html(data.rows[0][0]);
  		}
  	} else {
  		control.html("");
  	}
  } else {
  	control.html("");
  }
}


/* Control and Action resource JavaScript */


/* Link control resource JavaScript */

function linkClick(url, sessionVariablesString) {
	
	var sessionVariables = JSON.parse(sessionVariablesString);
	
	for (var i in sessionVariables) {
	
		var item = sessionVariables[i];
		
		if (item.type) {
		
			var value = window["getData_" + item.type](null, item.itemId, item.field, item.details);
			
		} else {
		
			var value = $.getUrlVar(item.itemId);
		
		}
	
		if (value !== undefined) url += "&" + item.name + "=" + value;
	}
	
	window.location = url;
	
}

/* Database action resource JavaScript */

var _pageUnloading = false;

window.onbeforeunload = function(ev) {
       _pageUnloading = true;
       return null;
};
