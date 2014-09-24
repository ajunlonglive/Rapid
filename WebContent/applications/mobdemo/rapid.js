
/* This file is auto-generated on application load and save - it is minified when in production */


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

function Action_database(actionId, data, outputs) {
	// check we got data and somewhere to put it
	if (data && outputs) {
		// check the returned sequence is higher than any others received so far
		if (data.sequence > getDatabaseActionMaxSequence(actionId)) {
			// retain this sequence as the new highest
			_databaseActionMaxSequence[actionId] = data.sequence;
			for (var i in outputs) {
				var output = outputs[i];			
				window["setData_" + output.type](output.id, data, output.field, output.details);
			}
		}
	}
}

function Action_mobile(actionId, type) {
	// action callback
	alert("Callback for " + actionId + "." + type);
}

//JQuery is ready! 
$(document).ready( function() {
	
	$(window).resize(function(ex) {
	
		var doc = $(document);
		var win = $(window);
				
		// resize the cover
		$(".dialogueCover").css({
       		width : doc.width(),
       		height : doc.height()
       	});
       	      	
       	// resize the dialogues
       	$(".dialogue").each(function() {
       		var dialogue = $(this);
	       	dialogue.css({
	       		left : (win.width() - dialogue.outerWidth()) / 2,
	       		top : (win.height() - dialogue.outerHeight()) / 3
	       	}); 
	    });
	
	});
	
});		        
	        
function Action_navigate(url, dialogue) {
	if (dialogue) {
	
		var bodyHtml = "<center><h1>Page</h1></center>";
		
		// request the page		
		$.ajax({
		   	url: url,
		   	type: "GET",          
		       data: null,        
		       async: false,
		       error: function(error, status, message) { 
		       		alert("Error loading dialogue : " + error.responseText||message); 
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
		           			// exclude the design link
		           			if (items[i].innerHTML && items[i].innerHTML.indexOf("/* designLink */") < 0) {
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
		           	
		           	// if this is the login page go to the real thing, requesting to come back to this location
            		if (bodyHtml.indexOf("<input type=\"submit\" value=\"log in\">") > 0) window.location = "login.jsp?requestPath=" + window.location; 
            	
		           	// get a reference to the body		           	
		           	var body = $("body");
		           	// add the cover and return reference
		           	var dialogueCover = body.append("<div class='dialogueCover' style='position:absolute;left:0px;top:0px;z-index:99;'></div>").children().last();
		           			      
		           	// get a reference to the document for the entire height and width     	
		           	var doc = $(document);
		           	
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
		           	
		           	// get a reference to the window for the visible area
		           	var win = $(window);
		           			           	
		           	// size the dialogue
		           	dialogue.css({
		           		position : "fixed",
	            		left : (win.width() - dialogue.outerWidth()) / 2,
	            		top : (win.height() - dialogue.outerHeight()) / 3
	            	}); 
	            	
	            	// this seems to be the best way to avoid the resizing/flicker when showing
	            	window.setTimeout( function() {
	            		dialogue.show();
	            	}, 200);
		           	           	        	            	            	            
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

function Action_webservice(actionId, data, outputs) {
	// only if there are data and outputs
	if (data && outputs) {
		// only if this is the latest sequence
		if (data.sequence > getWebserviceActionMaxSequence(actionId)) {
			// retain this as the lastest sequence
			_webserviceActionMaxSequence[actionId] = data.sequence;
			// loop the outputs
			for (var i in outputs) {
				var output = outputs[i];			
				window["setData_" + output.type](output.id, data, output.field, output.details);
			}
		}
	}
}


/* Control initialise methods */


function Init_date(id, details) {
  A_TCALCONF.format = details.dateFormat;	     
  f_tcalAdd (id);
}

function Init_gallery(id, details) {
  $("#" + id).children("img").click( function(ev) {
  	Gallery_removeImage(ev, id);
  });
}

function Init_hints(id, details) {
  var body = $("body");
  	    	
  for (var i in details.controlHints) {
  
  	var controlHint = details.controlHints[i];
  	
  	if (!$("#" + controlHint.controlId + "hint")[0]) {
  	
  		var style = controlHint.style;
  		if (style) {
  			style = " style='" + style + "'";
  		} else {
  			style = "";
  		}
  		
  		body.append("<span class='hint' id='" + controlHint.controlId + "hint'" + style + ">" + controlHint.text + "</span>");
  		
  		$("#" + controlHint.controlId + "hint").hide();
  		
  	}
  		
  	$("#" + controlHint.controlId).mouseout({controlId: controlHint.controlId}, function(ev) {
  		$("#" + ev.data.controlId + "hint").hide();
  	});
  		
  	switch (controlHint.type) {		
  		case "click" :
  			$("#" + controlHint.controlId).click({controlId: controlHint.controlId}, function(ev) { 
  				$("#" + ev.data.controlId + "hint").css({
  					left: ev.pageX + 5,
  					top: ev.pageY + 5
  				}).show(); 
  			});
  			break;
  		case "hover" :
  			$("#" + controlHint.controlId).mouseover({controlId: controlHint.controlId}, function(ev) { 
  				$("#" + ev.data.controlId + "hint").css({
  					left: ev.pageX + 5,
  					top: ev.pageY + 5
  				}).show();  
  			});
  		break;
  	}
  	
  }
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
             	// fire the window resize event
             	$(window).resize();
             	           	        	            	            	            
      	}        	       	        	        	        	        		
      }       	        	        
  });
}

function Init_slidePanel(id, details) {
  // get a reference to the body
  var body = $("body");
  // get the pageCover
  var pageCover = body.find(".slidePanelCover");
  // if we don't have one
  if (!pageCover[0]) {
  	// add one
  	body.append("<div class='slidePanelCover'></div>");
  	// set the reference
  	pageCover = body.find(".slidePanelCover");
  	// get a reference to the window
  	var win = $(window);	
  	// size the cover
  	pageCover.css({
      	width : win.width(),
         	height : win.height()
      });
  }
  
  // get a reference to the slidePanel
  var slidePanel = $("#" + id);
  // get the slidePanelPaneId
  var slidePanelPaneId = slidePanel.attr("data-pane");
  // get the pane
  var slidePanelPane = $("#" + slidePanelPaneId);
  
  // show or hide the page cover if panel is visible
  if (slidePanelPane.is(":visible")) {
  	pageCover.show();
  } else {
  	pageCover.hide();
  }
  
  // add the opener listener	       
  slidePanel.click({width: slidePanelPane.css("width"),left: slidePanelPane.css("margin-left")}, function(ev) {
  	// get the stored width
  	var width = ev.data.width
  	// get any existing left margin
  	var left = ev.data.left;
  	// check visibility
  	if (slidePanelPane.is(":visible")) {
  		// animate off-screen
  		slidePanelPane.animate({"margin-left": "-" + width}, 500, function() {
  			// hide when complete
  			slidePanelPane.hide();
  			// toggle open closed
  			slidePanel.removeClass("slidePanelOpen");
  			slidePanel.addClass("slidePanelClosed");	
  			// hide the page cover
  			pageCover.hide();		
  		});		
  	} else {
  		// set off screen
  		slidePanelPane.css({"margin-left": "-" + width}).show();
  		// animate to full width
  		slidePanelPane.animate({"margin-left": 0}, 500);		
  		// toggle open closed
  		slidePanel.removeClass("slidePanelClosed");
  		slidePanel.addClass("slidePanelOpen");
  		// show the page cover	
  		pageCover.show();
  	}
  });	        
  
  // add the cover listener
  pageCover.click({width: slidePanelPane.css("width"),left: slidePanelPane.css("margin-left")}, function(ev){
  	// get the stored width
  	var width = ev.data.width
  	// get any existing left margin
  	var left = ev.data.left;
  	// animate off-screen
  	slidePanelPane.animate({"margin-left": "-" + width}, 500, function() {
  		// hide when complete
  		slidePanelPane.hide();
  		// toggle open closed
  		slidePanel.removeClass("slidePanelOpen");
  		slidePanel.addClass("slidePanelClosed");	
  		// hide the page cover
  		pageCover.hide();		
  	});
  });
}

function Init_tabGroup(id, details) {
  $("#" + id).children("ul").children("li").each( function() {
  	$(this).click( function(ev, index) {
  		// get a reference to the tabs group
  		var tabs = $("#" + id);
  		// remove selected from all tab header items
  		tabs.children("ul").children("li").removeClass("selected");
  		// remove selected from all tab body items
  		tabs.children("div").removeClass("selected");
  		// add selected to the li we just clicked on, also get it's index, plus 2, 1 to go from zero to 1 based, the other 1 because of the headers
  		var index = $(this).addClass("selected").index() + 2;
  		// apply selected to the correct body
  		tabs.children("div:nth-child(" + index + ")").addClass("selected");
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
  	return data;		
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

function getData_gallery(ev, id, field, details) {
  var data = {fields:["url"],rows:[]};
  var control = $("#" + id);
  var images = control.children();
  images.each( function(i) {
  	data.rows.push([$(this).attr("src")]);
  });
  return data;
}

function setData_gallery(id, data, field, details) {
  if (data) {
  	var control = $("#" + id);
  	data = makeDataObject(data, field);
  	if (data.rows) {	
  		// remove the no pictures message
  		control.find("span").remove();
  		// look for url or urls in the fields or use the first column if not found	
  		var urlIndex = 0;
  		if (data.fields) {
  			for (var i in data.fields) {
  				if (data.fields[i] == "url" || data.fields[i] == "urls") {
  					urlIndex = i;
  					break;
  				}
  			}
  		}			
  		// loop the rows
  		for (var i in data.rows) {
  			// allow comma seperated list of urls in single field too
  			var urls = data.rows[i][urlIndex].split(",");
  			// loop the urls
  			for (var j in urls) {
  				var url = urls[j];
  				control.append("<img src='" + url  + "'/>");
  				control.find("img").last().click( function(ev) {
  					Gallery_removeImage(ev, id);				
  				});
  				// look for our custom imageAddedEvent handler for this control
  				var imageAdded = window["Event_imageAdded_" + id];
  				// fire it if we found it
  				if (imageAdded) window["Event_imageAdded_" + id]();
  			}			
  		}
  	} 
  }
}

function getProperty_gallery_imageCount(ev, id, field, details) {
  return ($("#" + id).children("img").size());
}

function getProperty_gallery_urls(ev, id, field, details) {
  var urls = "";
  var control = $("#" + id);
  var images = control.children();
  images.each( function(i) {
  	urls += $(this).attr("src");
  	if (i < images.length - 1) urls += ",";
  });
  return urls;
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
  					var style = "";
  					if (!details.columns[j].visible) style += "display:none;";
  					if (details.columns[j].style) style += details.columns[j].style;
  					if (style) style = " style='" + style + "'";				
  					var cellObject = rowObject.append("<td" + style + ">" + ((columnMap[j]) ? row[columnMap[j]] : "") + "</td>").find("td:last");
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

function getProperty_grid_columnCount(ev, id, field, details) {
  return ($("#" + id).find("tr").first().children("td").size());
}

function getProperty_grid_rowCount(ev, id, field, details) {
  return ($("#" + id).find("tr").size() - 1);
}

function getData_input(ev, id, field, details) {
  return $("#" + id).val();
}

function setData_input(id, data, field, details) {
  var control = $("#" + id);
  if (data !== undefined) {	
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


/* Gallery control resource JavaScript */

function Gallery_removeImage(ev, id) {
	// get the image
	var img = $(ev.target);
	// tell Rapid Mobile an image has been removed
	if (typeof _rapidmobile != "undefined") _rapidmobile.removeImage(id, img.attr("src")); 
	// remove it
	img.remove();
	// look for our custom imageRemoved handler for this control
	var imageRemoved = window["Event_imageRemoved_" + id];
	// fire it if we found it
	if (imageRemoved) window["Event_imageRemoved_" + id]();
}

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

/* Slide panel control resource JavaScript */

//JQuery is ready! 
$(document).ready( function() {
	
	$(window).resize(function(ex) {
	
		if (typeof(window.parent._pageIframe) === "undefined") {
	
			var win = $(window);
			
			// resize the page cover
			$(".slidePanelCover").css({
	       		width : win.width(),
	       		height : win.height()
	       	});
	       	
	    } else {
	    
	    	// get the page iframe
	    	var _pageIframe = window.parent._pageIframe;
	    	// get the scale
	    	var _scale = window.parent._scale;
	    		    		    
	    	// resize the page cover
			$(".slidePanelCover").css({
	       		width : _pageIframe.width() / _scale,
	       		height : _pageIframe.height() / _scale
	       	});
	    
	    }
       	      		
	});
	
});

/* Database action resource JavaScript */

// this global associative array tracks the databaseAction call sequences for each action	    			
var _databaseActionSequence = [];	    

// this global associative array holds the greates sequence received back     			
var _databaseActionMaxSequence = [];	

// this function returns an incrementing sequence for each database action call so long-running slow queries don't overrwrite fast later queries
function getDatabaseActionSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _databaseActionSequence[actionId];
	// if null set to 0
	if (!sequence) sequence = 0
	// increment
	sequence++;
	// store
	_databaseActionSequence[actionId] = sequence;
	// pass back
	return sequence;
}		

// this function sets the max to 0 if null
function getDatabaseActionMaxSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _databaseActionMaxSequence[actionId];
	// if undefined
	if (sequence === undefined) {
		// set to 0
		sequence = 0;
		// retain for next time
		_databaseActionMaxSequence[actionId] = sequence;
	}
	// pass back
	return sequence;
}

/* Webservice action resource JavaScript */

// this global associative array tracks the webserviceAction call sequences for each action	    			
var _webserviceActionSequence = [];	    

// this global associative array holds the greates sequence received back     			
var _webserviceActionMaxSequence = [];	

// this function returns an incrementing sequence for each database action call so long-running slow queries don't overrwrite fast later queries
function getWebserviceActionSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _webserviceActionSequence[actionId];
	// if null set to 0
	if (!sequence) sequence = 0
	// increment
	sequence++;
	// store
	_webserviceActionSequence[actionId] = sequence;
	// pass back
	return sequence;
}	

// this function sets the max to 0 if null
function getWebserviceActionMaxSequence(actionId) {
	// retrieve the current sequence for the action
	var sequence = _webserviceActionMaxSequence[actionId];
	// if undefined
	if (sequence === undefined) {
		// set to 0
		sequence = 0;
		// retain for next time
		_webserviceActionMaxSequence[actionId] = sequence;
	}
	// pass back
	return sequence;
}
