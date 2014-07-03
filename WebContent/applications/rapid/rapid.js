
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
		           	
		           	// if this is the login page go to the real thing, requesting to come back to this location
            		if (bodyHtml.indexOf("<input type=\"submit\" value=\"log in\">") > 0) window.location = "login.jsp?requestPath=" + window.location; 
            	
		           	
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
		           	
		           	// size the dialogue
		           	dialogue.css({
		           		position : "fixed",
	            		left : (win.width() - dialogue.outerWidth()) / 2,
	            		top : (win.height() - dialogue.outerHeight()) / 3
	            	}); 
	            	
	            	// this seems to be the best way to avoid the resizing/flicker when showing
	            	window.setTimeout( function() {
	            		dialogue.show();
	            	}, 5);
		           	           	        	            	            	            
		    	}        	       	        	        	        	        		
		    }       	        	        
		});	      
	
	} else {
		window.location = url;
	}
}

function Action_rapid(ev, appId, pageId, controlId, actionId, actionType, successCallback, errorCallback) {

	var type = "GET";
	
	var data = null;
	var callback = null;

	// some special types require data and callbacks	
	switch (actionType) {
		case "GETAPPS" :		
			data = { actionType: actionType, appId: "rapid" };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C210', data, null, {storageType:"S", id:"rapidrapid_P0_C210"});
			};
		break;
		case "GETAPP" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val() };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C127', data, "application", {storageType:"S", id:"rapidrapid_P0_C127"});
			};
		break;	
		case "GETDBCONN" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), index: $("#rapid_P0_C311").find("tr.rowSelect").index()-1 };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C361', data, "databaseConnection", {storageType:"S", id:"rapidrapid_P0_C361"});
			};
		break;
		case "GETSOA" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), index: $("#rapid_P0_C483_").find("tr.rowSelect").index()-1 };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C528_', data, "webservice", {storageType:"S", id:"rapidrapid_P0_C528_"});
				loadSOA(data.webservice);
			};
		break;
		case "GETSEC" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), securityAdapter: $("#rapid_P0_C81").val() };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C469_', data, "security", {storageType:"S", id:"rapidrapid_P0_C469_"});
			};
		break;
		case "GETUSER" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), userName: getData_grid(ev, "rapid_P0_C216", "userName", {columns:[{field:"userName"}]}) };	
			callback = function(data) {
				setData_dataStore('rapid_P0_C243', data, "user", {storageType:"S", id:"rapidrapid_P0_C243"});
			};
		break;
		case "SAVEAPP" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				name: $("#rapid_P0_C392_").val(),
				title: $("#rapid_P0_C394_").val(),
				description: $("#rapid_P0_C393_").val(),
				showControlIds: $("#rapid_P0_C381_").prop("checked"),
				showActionIds: $("#rapid_P0_C382_").prop("checked"),
				startPageId: $("#rapid_P0_C644_").val()
			};	
		break;
		case "SAVESTYLES" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), styles: $("#rapid_P0_C116").val() };	
		break;		
		case "SAVEDBCONN" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				index: $("#rapid_P0_C311").find("tr.rowSelect").index()-1,
				name: $("#rapid_P0_C360").val(),
				driver: $("#rapid_P0_C338").val(),
				connectionString: $("#rapid_P0_C374").val(),
				connectionAdapter: $("#rapid_P0_C339").val(),
				userName: $("#rapid_P0_C340").val(),
				password: $("#rapid_P0_C341").val()
			};	
			callback = function() {
				Event_click_rapid_P0_C311({target: $("#rapid_P0_C311").find("tr.rowSelect").children().first()[0] });
			};
		break;
		case "SAVESOASQL" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				index: $("#rapid_P0_C483_").find("tr.rowSelect").index()-1,
				name: $("#rapid_P0_C496_").val(), 
				databaseConnectionIndex: $("#rapid_P0_C536_")[0].selectedIndex,
				details: _soaDetails
			};	
		break;
		case "SAVESECURITYADAPT" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				securityAdapter: $("#rapid_P0_C81").val()
			};	
		break;
		case "SAVEACTIONS" :
			var actionTypes = [];
			$("#rapid_P0_C288").find("input:checked").each( function(){
				actionTypes.push($(this).closest("tr").children().first().html());
			});		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), actionTypes: actionTypes };	
		break;
		case "SAVECONTROLS" :	
			var controlTypes = [];
			$("#rapid_P0_C289").find("input:checked").each( function(){
				controlTypes.push($(this).closest("tr").children().first().html());
			});		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), controlTypes: controlTypes };	
		break;
		case "REBUILDPAGES" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val() };	
		break;
		case "NEWAPP" :
			data = {
				actionType: actionType,
				appId: "rapid",
				name: $("#rapid_P1_C7").val(),
				title: $("#rapid_P1_C11").val(),
				description: $("#rapid_P1_C15").val()
			}
			callback = function(response) {
				window.location = "~?a=rapid&p=P0&appId=" + response.appId;
			}; 
		break;
		case "DELAPP" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val() 
			};	
			callback = function() {
				window.location = "~?a=rapid&p=P0";
			}; 			
		break;		
		case "DUPAPP" :		
			data = {
				actionType: actionType,
				appId: $("#rapid_P0_C43").val(),
				name: $("#rapid_P8_C7_").val(),
				title: $("#rapid_P8_C12_").val(),
				description: $("#rapid_P8_C17_").val()
			}
			callback = function(response) {
				location.reload();
			}; 			
		break;
		case "NEWPAGE" :	
			data = {
				actionType: actionType,
				appId: _app.id,
				id: "P" + _nextPageId, 
				name: $("#rapid_P3_C17").val(),
				title: $("#rapid_P3_C18").val(),
				description: $("#rapid_P3_C19").val()
			}
			callback = function(data) {
				hideDialogue();
				loadPages(data.id, true);
			}; 
		break;
		case "DELPAGE" :
			data = { actionType: actionType, appId: _app.id, id: _page.id }; 
			callback = function() {
				hideDialogue();
				loadPages(null, true);
			}; 
		break;	
		case "NEWDBCONN" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				name: $("#rapid_P7_C7").val(),
				driver: $("#rapid_P7_C37").val(),
				connectionString: $("#rapid_P7_C44").val(),
				connectionAdapter: $("#rapid_P7_C38").val(),
				userName: $("#rapid_P7_C30").val(),
				password: $("#rapid_P7_C35").val()
			};	
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "DELDBCONN" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), index: $(ev.target).parent().parent().index()-1 };
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "NEWSOA" :		
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				name: $("#rapid_P10_C8_").val(),
				type: $("#rapid_P10_C23_").val()
			};	
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "DELSOA" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), index: $(ev.target).parent().parent().index()-1 };
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};	
		break;
		case "NEWROLE" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), role: $("#rapid_P5_C7").val(), description: $("#rapid_P5_C41_").val() };
			callback = function() {
				// fake an adapter change
				$("#rapid_P0_C81").change();
				// fake a tab click
				$("#rapid_P0_C74").click(); 
			};								
		break;
		case "DELROLE" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), role: $(ev.target).closest("tr").find("td").first().html() };
			callback = function() {
				// fake an adapter change
				$("#rapid_P0_C81").change(); 
				// fake a tab click
				$("#rapid_P0_C74").click(); 
			};								
		break;
		case "NEWUSER" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), userName: $("#rapid_P6_C7").val(), description: $("#rapid_P6_C42_").val() , password: $("#rapid_P6_C18").val() };
			callback = function() {
				Event_change_rapid_P0_C43(ev);
			};								
		break;
		case "DELUSER" :		
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), userName: $(ev.target).closest("tr").find("td").first().html() };
			callback = function() {
				// fake an adapter change
				$("#rapid_P0_C81").change(); 
				// fake a tab click
				$("#rapid_P0_C74").click();    
			};								
		break;
		case "NEWUSERROLE" : 
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), userName: $("#rapid_P0_C216").find("tr.rowSelect").children().first().html(), role: $("#rapid_P0_C254").val() };
			callback = function() {
				Event_click_rapid_P0_C216({target: $("#rapid_P0_C216").find("tr.rowSelect").children().first()[0] });
			};																
		break;
		case "DELUSERROLE" :	
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), userName: $("#rapid_P0_C216").find("tr.rowSelect").children().first().html(), role: $(ev.target).parent().prev().html() };
			callback = function() {
				Event_click_rapid_P0_C216({target: $("#rapid_P0_C216").find("tr.rowSelect").children().first()[0] });
			};																
		break;
		case "SAVEUSER" :	
			data = { actionType: actionType, appId: $("#rapid_P0_C43").val(), userName: $("#rapid_P0_C216").find("tr.rowSelect").children().first().html(), description: $("#rapid_P0_C717_").val(), password: $("#rapid_P0_C231").val() };
			callback = function() {
				// fake an adapter change
				$("#rapid_P0_C81").change(); 
				// fake a tab click
				$("#rapid_P0_C74").click();    
			};																
		break;
		case "TESTDBCONN" :	
			data = { 
				actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				index: $("#rapid_P0_C311").find("tr.rowSelect").index()-1,
				driver: $("#rapid_P0_C338").val(),
				connectionString: $("#rapid_P0_C374").val(),
				connectionAdapter: $("#rapid_P0_C339").val(),
				userName: $("#rapid_P0_C340").val(),
				password: $("#rapid_P0_C341").val()
			};	
			callback = function() {
				alert("Database connection OK");
			};															
		break;
		case "DELAPPBACKUP" : case "RESTOREAPPBACKUP" : 
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				backupId: $("#rapid_P0_C663_").find("tr.rowSelect").children("td").first().text() 
			};	
			callback = function(data) {
				$(data.controlId).hideDialogue();
				Event_change_rapid_P0_C43();
				$("#rapid_P0_C662_").click();
			};														
		break;
		case "DELPAGEBACKUP" : case "RESTOREPAGEBACKUP" :	
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				backupId: $("#rapid_P0_C678_").find("tr.rowSelect").children("td").first().text()  
			};	
			callback = function(data) {
				$(data.controlId).hideDialogue();
				Event_change_rapid_P0_C43();
				$("#rapid_P0_C662_").click();
			};															
		break;
		case "SAVEAPPBACKUPSIZE" :			
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				backupMaxSize: $("#rapid_P0_C685_").val()  
			};
		break;
		case "SAVEPAGEBACKUPSIZE" :
			data = { actionType: actionType, 
				appId: $("#rapid_P0_C43").val(), 
				backupMaxSize: $("#rapid_P0_C686_").val()  
			};
		break;
		default :
			data = { actionType: actionType, appId: "rapid" };
	}
	
	// stringify any data
	if (data) data = JSON.stringify(data);
	
	// run the action on the server	
	$.ajax({
    	url: "~?a=rapid&p=" + pageId + "&act=" + actionId,
    	type: "POST",          
    	dataType: "json",
        data: data,            
        error: function(server, status, error) { 
        	if (server && server.status && server.status == 401) {
        		window.location = "login.jsp";
        	} else {
        		errorCallback(server, status, error);
        	} 
        },
        success: function(data) {
       		if (callback) callback(data);
       		if (successCallback) successCallback(data);        	
        }
	});
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
