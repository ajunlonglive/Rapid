/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

AFTER MAKING CHANGES TO THIS FILE DELETE /scripts_min/extras.min.js IN ORDER TO REGENERATE IT

*/

// extend String to have a trim function for IE8
if (typeof String.prototype.trim !== 'function') {
	String.prototype.trim = function() {
		return this.replace(/^\s+|\s+$/g, ''); 
	};
}

// extend String to have a replaceAll function
String.prototype.replaceAll = function( find, replace ) {
    return this.split( find ).join( replace );        
};

// trigger attached show and hide functions when control is shown or hidden
// http://stackoverflow.com/questions/15232688/jquery-how-to-call-function-when-element-shows
(function ($) {
	  $.each(['show', 'hide'], function (i, ev) {
	    var el = $.fn[ev];
	    $.fn[ev] = function () {
	    	el.apply(this, arguments);
	    	this.trigger(ev);	 
	      return this;
	    };
	  });
	})(jQuery);

// extend JQuery to have functions for retrieving url parameter values
$.extend({
  getUrlVars: function(){
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++) {
      hash = hashes[i].split('=');
      vars.push(hash[0]);
      if (hash[1]) vars[hash[0]] = decodeURIComponent(hash[1].replace('#',''));
    }
    return vars;
  },
  getUrlVar: function(name){
    return $.getUrlVars()[name];
  }
});

// position loading cover - we can call this for controls with covers when the page morphs like when resizing or loading dialogues
function positionLoadingCover(control, loadingCover) {
	loadingCover.css({
		left: control.offset().left,
		top: control.offset().top,
		width: control.outerWidth(),
		height: control.outerHeight()
	}).show();
	loadingCover.children("div").css({
		height: control.outerHeight()
	});
	if (control.width() > 0) {
		var image = loadingCover.children("span");
		image.css({
			left: (control.outerWidth() - image.outerWidth())/2,
			top: (control.outerHeight() - image.outerHeight())/2
		}).show();
	}
}

// uses the above to position all visible loading covers
function positionLoadingCovers() {
	// get all visible loading covers and loop
	var loadingCovers = $("div.loadingCover:visible").each( function() {
		// get the cover
		var loadingCover = $(this);
		// get the control
		var control = $("#" + loadingCover.attr("data-id"));
		// position!
		positionLoadingCover(control, loadingCover);
	});	
}

// extend JQuery object methods
$.fn.extend({
  enable: function() {
	return this.removeAttr("disabled").find("button,input,select,textarea").removeAttr("disabled");  
  },
  disable: function() {
	return this.attr("disabled","disabled").find("button,input,select,textarea").attr("disabled","disabled");
  },
  focus: function() {
	var e = this[0];
	if (e) {
		e.focus();
		var c = this.find("button:visible,input:visible,select:visible,textarea:visible")[0];
		if (c) c.focus();
	}	
	return this;
  },
  showLoading: function() {
	 var id = this.attr("id");
	 var loadingCover = $("div.loadingCover[data-id=" + id + "]");
	 if (this[0] && this.is(":visible")) {		
		if (!loadingCover[0]) {
			$("body").after("<div class='loadingCover' data-id='" + id + "'><div class='loading'></div><span class='loading'></span></div>");
			loadingCover = $("div.loadingCover[data-id=" + id + "]");
		}				
		positionLoadingCover(this, loadingCover);
	 } else {
		loadingCover.hide();
	 }
	 return this;
  },
  hideLoading: function() {
	  if (this[0]) $("div.loadingCover[data-id=" + this.attr("id") + "]").hide();	
	  return this;
  },
  hideDialogue: function(reload, id) {
	  // get a reference to closest dialogue
	  var dialogue = $(this).closest("div.dialogue");
	  // if we didn't find one but we have an id, use that
	  if (dialogue.length == 0 && id) dialogue = $(".dialogue").last();
	  // remove the cover
	  dialogue.prev("div.dialogueCover").remove();
	  // remove the dialogue
	  dialogue.remove();
	  // check reload
	  if (reload) {
		  var pageId = $("body").attr("id");
		  if (window["Event_pageload_" + pageId]) window["Event_pageload_" + pageId]();		  
	  } else {
		  // reset the tabs in the body
		  $("body").find("input, select, textarea, button, a").each( function() {
      		// get the element
      		var e = $(this);
      		// get any existing data tab index
      		var t = e.attr("data-tabindex");
      		// if there was one add a proper attribute for it
      		if (t > 0) {
	  			e.attr("tabindex",t);
	  			e.removeAttr("data-tabindex");
      		} else {
      			// remove negative tab index
      			e.removeAttr("tabindex");
      		}
      	});
	  }
	  return this;
  },
  hideAllDialogues: function(reload) {	  
	  $("div.dialogue").remove();
	  $("div.dialogueCover").remove();
	  if (reload) {
		  var pageId = $("body").attr("id");
		  if (window["Event_pageload_" + pageId]) window["Event_pageload_" + pageId]();		  
	  } else {
		// reset the tabs in the body
		  $("body").find("input, select, textarea, button, a").each( function() {
			  // get the element
			  var e = $(this);
			  // get any existing data tab index
			  var t = e.attr("data-tabindex");
			  // if there was one add a proper attribute for it
			  if (t > 0) {
				  e.attr("tabindex",t);
				  e.removeAttr("data-tabindex");
			  } else {
				  // remove negative tab index
				  e.removeAttr("tabindex");
			  }
		  });
	  }  
	  return this;
  },
  showError: function(server, status, message) {
	if (server) {
		var message = server.responseText||message;
		var b = message.indexOf("\n\n");
		if (b > 0) message = message.substring(0, b);
		alert(message);
	}
    return this;
  }
});

// this overrides the focus so that if focus is fired when the page is invisible, focus can be set once the page is made visible by Rapid
(function($) {
	// a reference to the original focus method
    var focus_orignal = $.fn.focus; // maintain a reference to the existing function
    // override the focus method
    $.fn.focus = function(type) {
    	// if there was an event of type DOMContentLoaded
    	if (type == "rapid") {
	    	// get a reference to the control
			var c = this[0];
			// if there is one
			if (c) {		  
				// if the page or dialogue are hidden
				if ($("body").css("visibility") == "hidden" || this.closest("div.dialogue").css("visibility") == "hidden" || this.closest("div.dialogue").css("display") == "none") {
					// mark the element with data-focus=true (the page display will then set the focus on this)
					this.attr("data-focus","true");			  
				}
			}		 
			// reset the arguments
			arguments = [];
    	}
		// apply and return the original method
        return focus_orignal.apply(this, arguments);
    };
})(jQuery);

// thanks to http://stackoverflow.com/questions/2200494/jquery-trigger-event-when-an-element-is-removed-from-the-dom/10172676#10172676
(function($) {
  $.event.special.destroyed = {
    remove: function(o) {
      if (o.handler) {
        o.handler()
      }
    }
  }
})(jQuery);

/*
 
 http://code.accursoft.com/caret
 
 Copyright (c) 2009, Gideon Sireling

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of Gideon Sireling nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
(function($) {
  $.fn.caret = function(pos) {
    var target = this[0];
	var isContentEditable = target.contentEditable === 'true';
    //get
    if (arguments.length == 0) {
      //HTML5
      if (window.getSelection) {
        //contenteditable
        if (isContentEditable) {
          target.focus();
          var range1 = window.getSelection().getRangeAt(0),
              range2 = range1.cloneRange();
          range2.selectNodeContents(target);
          range2.setEnd(range1.endContainer, range1.endOffset);
          return range2.toString().length;
        }
        //textarea
        return target.selectionStart;
      }
      //IE<9
      if (document.selection) {
        target.focus();
        //contenteditable
        if (isContentEditable) {
            var range1 = document.selection.createRange(),
                range2 = document.body.createTextRange();
            range2.moveToElementText(target);
            range2.setEndPoint('EndToEnd', range1);
            return range2.text.length;
        }
        //textarea
        var pos = 0,
            range = target.createTextRange(),
            range2 = document.selection.createRange().duplicate(),
            bookmark = range2.getBookmark();
        range.moveToBookmark(bookmark);
        while (range.moveStart('character', -1) !== 0) pos++;
        return pos;
      }
      //not supported
      return 0;
    }
    //set
    if (pos == -1)
      pos = this[isContentEditable? 'text' : 'val']().length;
    //HTML5
    if (window.getSelection) {
      //contenteditable
      if (isContentEditable) {
        target.focus();
        window.getSelection().collapse(target.firstChild, pos);
      }
      //textarea
      else
        target.setSelectionRange(pos, pos);
    }
    //IE<9
    else if (document.body.createTextRange) {
      var range = document.body.createTextRange();
      range.moveToElementText(target)
      range.moveStart('character', pos);
      range.collapse(true);
      range.select();
    }
    if (!isContentEditable)
      target.focus();
    return pos;
  }
})(jQuery);

// this function can be used on the keydown of textareas to prevent changing to the next control
function textareaOverride(ev) {
	// get the text area
	var textarea = $(ev.target);
	// if this is the tab key
	if (textarea.is("textarea") && ev.keyCode == 9) {
		// get the current cursor position
		var pos = textarea.caret();
		// get the current value
		var val = textarea.val();
		// add a tab at the current postion and re-assign to the textarea
		textarea.val(val.substr(0,pos) + "\t" + val.substr(pos));
		// advance the cursor position by 1
		textarea.caret(pos + 1);
		// stop any further events
		ev.stopPropagation();
		// stop any further behaviour from the tab key, like loosing focus
		return false;
	}
}

// function for applying the maxlength rules to a textarea (reused when dialogue loads)
function textarea_maxlength() {
	
	// ignore these keys
	var ignore = [8,9,13,33,34,35,36,37,38,39,40,46];
	
	// wrap the textarea
	$(this)
	
	// use keypress instead of keydown as that's the only
  	// place keystrokes could be canceled in Opera
    .on('keypress', function(event) {
      var self = $(this),
          maxlength = self.attr('maxlength'),
          code = $.data(this, 'keycode');

      // check if maxlength has a value.
      // The value must be greater than 0
      if (maxlength && maxlength > 0) {

        // continue with this keystroke if maxlength
        // not reached or one of the ignored keys were pressed.
        return ( self.val().length < maxlength || $.inArray(code, ignore) !== -1 );

      }
    })

    // store keyCode from keydown event for later use
    .on('keydown', function(event) {
      $.data(this, 'keycode', event.keyCode || event.which);
    });
	
}

// function 
function textarea_autoheight_size(textarea, resetHeight) {
	// reset height if required on setData and blur
	if (resetHeight) textarea.height(textarea.attr("data-height"));
	// if scroll height is already at the max
	if (textarea[0].scrollHeight > 1000) {
		// set height to max
		textarea.height(1000);
	} else {
		// max times
		var i = 0;
		// grow 
		while(i < 1000 && Math.round(textarea.outerHeight()) < textarea[0].scrollHeight + parseFloat(textarea.css("borderTopWidth")) + parseFloat(textarea.css("borderBottomWidth"))) {
			textarea.height(textarea.height() + 1);
			i ++;
	    };
	}
}

// function for apply the autoheight to a textarea  (reused when dialogue loads)
function textarea_autoheight(textarea) {
	
	// get a reference to the textarea if we weren't given one
	if (!textarea || !$(textarea).is("textarea")) textarea = $(this);
	
	// get the starting height
	var height = Math.max(textarea.height(), 10);
		
	// retain original height
	textarea.attr("data-height", height);
	
	// use keypress instead of keydown as that's the only place keystrokes could be cancelled in Opera
	textarea
	.on('keypress', function(ev) {
		textarea_autoheight_size(textarea);
    })
    .on('show', function(ev) {
    	// size it
		textarea_autoheight_size(textarea, true);
    })   
    .on('blur', function(ev) {
    	// small delay to allow any click events to fire
    	setTimeout(function(){ 
    		// get the body element
        	var b = $("body");
        	// get the html height
        	var h = b.height();
        	// set it in css (this stops the document resizing and bouncing us around)
        	b.css("height",h);        	
        	// do the sizing after resetting the height
        	textarea_autoheight_size(textarea, true);
            // set the body height back to auto
            b.css("height","auto");
    	}, 500);
    });
	
}

// this applies and enforces the maxlength attribute on textareas
jQuery(function($) {
  
	// handle textareas with maxlength attribute
	$('textarea[maxlength]').each( textarea_maxlength );
  
	// handle textareas with autoheight class
	$('textarea.autoheight').each( textarea_autoheight ); 
  
});

// override the much-used ajax call for both normal and Rapid Mobile 
if (window["_rapidmobile"]) {
	
	console.log("Rapid Mobile detected");
	
	// add save page html function for saving
	_rapidmobile.savePage = function() {
		// explicitly push each input val in as an attribute
		$("input").each( function() {
			// get a reference to this input
			var i = $(this);
			// if it's a radio or checkbox
			if (i.is("[type=radio],[type=checkbox]")) {
				// if checked set selected property
				if (i.prop("checked")) i.attr("checked","checked");
			} else {
				// set value attribute
				i.attr("value", $(this).val());
			}
		});
		// explicitly push textarea value into it's html
		$("textarea").each( function() {
			$(this).html($(this).val());
		});
		// explicitly set each selected option val
		$("select").each( function() {
			// get the select
			var s = $(this);
			// get the value
			var v = s.val();
			// if there was a value
			if (v) {
				// find the option by the value
				var o = s.find("option[value='" + v.replace("'","\\'") + "']");
				// if no option found, find an option by the text
				if (o.length == 0) o = s.find("option").filter(function() { return $(this).text() == v; });
				// select it
				o.attr("selected","selected");
			}
		});
		// remove any script reference to the google maps api as they cause problems on reload
		$("head").find("script[src*='//maps.gstatic.com']").remove();
		// on this one we want to keep the first node so remove all but index = 0
		$("head").find("script[src*='googleapis.com/']").filter( function(idx) {
			return idx > 0;
		}).remove();		
								
		// get the html
		var html = $('html').html();
		// now call into the bridge
		_rapidmobile.savePageToDevice(html);		
	}
			
	// retain the original JQuery ajax function
	var ajax = $.ajax;
	
	// override it
	$.ajax = function(settings) {
		
		// only on old (unversioned clients with _rapidMobile), or with multithreaded local requests turned off
		if (!_rapidmobile.getVersion || !_rapidmobile.isMultiThreaded || !_rapidmobile.isMultiThreaded()) {
			// the shouldInterceptRequest method only works for GET, so if there is data add it to the url
			if (settings.data) {
				// add data to the url
				settings.url += "&data=" + encodeURIComponent(settings.data);
				// remove it from the body
				settings.data = null;
				// change the action to a GET
				settings.method = "GET";
			}
		}
		// retain the original success function
		var success = settings.success;
		// override it (and pass in the orginal)
		settings.success = function(data, textStatus, jqXHR) {
			// if there is a json object in the response
			if (jqXHR.responseJSON) {
				// if it contains an error object
				if (jqXHR.responseJSON.error) {					
					// get the error object
					var error = jqXHR.responseJSON.error;
					// check the status code
					switch (error.status) {
					case (401) :
						// the user failed authentication show them a message in the ui
						_rapidmobile.showMessage(error.responseText);
						// bail
						return false;
					default :
						// run the error function
						settings.error(error, error.status, error.responseText);
						// bail
						return false;
					}					
				}
			}
			// run the original function if all good
			if (success) success(data, textStatus, jqXHR);
		}
		// now run the original ajax with our modified settings
		ajax(settings);
	}
				
} else {
	
	// retain the original JQuery ajax function
	var ajax = $.ajax;
	// substitute our own
	$.ajax = function(url, settings) {
		// retain original error handler
		var error = url.error;
		// override error
		url.error = function(jqXHR, textStatus, errorThrown) {
			// if this is a 401 (unauthorised) redirect the user to the login page and set requestApp so we'll come straight back
			if (jqXHR.status == 401) {
				// start with a basic login page url
				var location = "login.jsp";
				// if an errorThrown was provided this might contain the location from a custom login
				if (errorThrown && errorThrown.indexOf("location=") == 0) location = errorThrown.substr(9);
				// if we're viewing an app we want to go back to it once logged in
				if (window.location.href.indexOf("/~?a=") > -1) {
					// look for an application parameter
					var appId = $.getUrlVar("a");
					// look for an application version parameter
					var appVersion = $.getUrlVar("v");
					// append escaped requestPath if there's an app, and version if it exists
					if (appId) location += "?requestApp=" + appId + (appVersion ? "&requestVersion=" + appVersion : "");
				}				
				// redirect to login page
				window.location = location;
			} else {
				// call the original error
				if (error) error(jqXHR, textStatus, errorThrown);
			}
		}
		// call the original
		ajax(url, settings);
	}
}


function showControlValidation(controlId, message) {
	var control = $("#" + controlId);
	control.addClass("validation");
	if (message) {
		var element = control.next("div.validation.validationMessage")[0];
		if (element) {
			$(element).html(message);
		} else {
			control.after("<div class='validation validationMessage'>" + message + "</div>");
		}
	}
}

function hideControlValidation(controlId) {
	var control = $("#" + controlId);
	control.removeClass("validation");
	control.next("div.validation.validationMessage").remove();
}

function getDataObjectFieldIndex(data, field, caseSensitive) {
	if (data && data.fields && data.fields.length > 0 && field) {
		for (var i in data.fields) {
			if (data.fields[i]) {
				if (caseSensitive) {
					if (data.fields[i] == field) return i;
				} else {
					if (data.fields[i].toLowerCase() == field.toLowerCase()) return i;
				}
			}
		}
		return -1;
	}
	return null;
}

// makes a Rapid data object, can drill into objects by field, and convert JSON
function makeDataObject(data, field) {
	// check we were passed something to work with
	if (data != null && data !== undefined) {
		// convert any json strings to json
		if ($.type(data) == "string" && data.indexOf("{") == 0 && data.indexOf("}") == data.length - 1) data = JSON.parse(data);
		// return immediately if all well (we have rows and fields already and there is nothing to promote)
		if (data.rows && data.fields && !(field && (data[field]))) return data;		
		// initialise fields
		var fields = [];
		// initialise rows
		var rows = [];
		// initialise a fieldmap (as properties aren't always in the same position each time)
		var fieldMap = [];
		// if the field is what we're after move it into the data
		if (field && data[field] && !$.isFunction(data[field])) data = data[field];			
		// if the data is an array
		if ($.isArray(data)) {
			// loop the array
			for (var i in data) {
				// retrieve the item
				var item = data[i];
				// prepare a row
				var row = [];
				// if it's an object build data object from it's properties
				if ($.isPlainObject(item)) {
					// if we were given a field and it's a property of the object, promote that property value into the object (this is useful for JSON arrays of objects)
					if (field && item[field]) item = item[field];
					// start at first field
					var fieldPos = 0;
					// loop the properties
					for (var j in item) {
						// check for a field mapping
						if (fieldMap[fieldPos]) {
							// if the mapping is different
							if (fieldMap[fieldPos] != j) {
								// assume field isn't there
								fieldPos = -1;
								// loop the map
								for (var k in fieldMap) {
									if (j == fieldMap[k]) {
										fieldPos =k;
										break;
									}
								}
								// field pos wasn't found
								if (fieldPos == -1) {
									fieldMap.push(j);
									fields.push(j);
									fieldPos = fields.length - 1;
								}
							}
						} else {
							// we don't have a mapping for this field (this is good, store field at this position in map and fields array)
							fieldMap.push(j);
							fields.push(j);
							fieldPos = fields.length - 1;
						}
						// store the data in the row at the field position 
						row[fieldPos] = item[j];
						// all being well the next property is in the next position, if it wraps it'll assume it's an unseen field
						if (fieldPos < fields.length - 1) fieldPos++;
					}								
				} else {
					// retain the field
					if (i == 0) fields.push(field);
					// make a row with the item
					row = [ item ];
				}
				// add the row
				rows.push(row);
			}				
		} else {
			var row = [];
			if ($.isPlainObject(data)) {
				for (var i in data) {
					fields.push(i);
					row.push(data[i]);
				}
			} else {
				fields.push(field);
				row.push(data);
			}		
			rows.push(row);
		}
		data = { fields: fields, rows: rows};
	}
	return data;
}

function makeObjectFromData(data, fields) {
	var object = data;
	if (data && data.rows && data.fields) {
		object = {};
		if (data.rows.length > 0 && data.fields.length > 0) {
			for (var i = 0; i < data.fields.length; i++) {
				if (fields && fields.length > 0) {
					for (var j = 0; j < fields.length; j++) {
						if (data.fields[i] == fields[j]) {
							object[data.fields[i]] = data.rows[0][i];
							break;
						}
					}
				} else {
					object[data.fields[i]] = data.rows[0][i];
				}
			}
		}
	}
	return object;
}

function mergeDataObjects(data1, data2, mergeType, field, maxRows, details) {
	var data = null;
	if (data1 || (data1 == "" && mergeType == "search")) {
		data1 = makeDataObject(data1);
		if (data2) {
			data2 = makeDataObject(data2);
			switch (mergeType) {
				case "append" : case "row" :
					var fields = [];
					for (var i in data1.fields) fields.push(data1.fields[i]);
					for (var i in data2.fields) {
						var gotField = false;
						for (var j in fields) {
							if (data2.fields[i] !== undefined && fields[j] !== undefined && data2.fields[i].toLowerCase() == fields[j].toLowerCase()) {
								gotField = true;
								break;
							}
						}
						if (!gotField) fields.push(data2.fields[i]);
					}
					data = {};
					data.fields = fields;
					if (mergeType == "append") {						
						if (data1.rows.length == 1 && data1.fields.length == 1 && data2.rows.length == 1 && data2.fields.length == 1 && !(details && (details.type == "grid" || details.type == "gallery" ))) {
							data = data1.rows[0][0] + data2.rows[0][0]; 
						} else {
							data.rows = data1.rows;
							for (var i = 0; i < data2.rows.length; i++) {
								var row = [];
								for (var j in fields) {
									var value = null;
									for (var k in data2.fields) {
										if (fields[j] !== undefined && data2.fields[k] !== undefined && fields[j].toLowerCase() == data2.fields[k].toLowerCase()) {
											value = data2.rows[i][k];
											break;
										}
									}
									row.push(value);
								}
								data.rows.push(row);
							}
						}
					} else {
						data.rows = [];
						var totalRows = data2.rows.length;
						if (data1.rows.length > totalRows) totalRows = data1.rows.length;			
						for (var i = 0; i < totalRows; i++) {
							var row = [];
							for (var j in fields) {
								var value = null;
								
								// we need to know if we find a value
								var valueFound = false;
								if (i < data2.rows.length) {
									for (var k in data2.fields) {
										if (fields[j] !== undefined && data2.fields[k] !== undefined && fields[j].toLowerCase() == data2.fields[k].toLowerCase()) {
											value = data2.rows[i][k];
											
											// record that we found a value in the source data
											valueFound = true;
											break;
										}
									}
								}
								// only do this if we didn't find a value in the source data
								if (i < data1.rows.length && value == null && !valueVound) {
									for (var k in data1.fields) {
										if (fields[j] !== undefined && data1.fields[k] !== undefined && fields[j].toLowerCase() == data1.fields[k].toLowerCase()) {
											value = data1.rows[i][k];
											break;
										}
									}
								}
								row.push(value);
							}
							data.rows.push(row);
						}		
					}
				break;
				case "child" :
					var fieldMap = {};
					var fieldCount = 0;
					var fieldIndex = -1;
					// make a map of positions for fields common to parent and child data objects
					for (var i in data1.fields) {
						// if noFieldMatch is set don't build the map
						if (!details || !details.noFieldMatch) {
							for (var j in data2.fields) {
								if (data1.fields[i] !== undefined && data2.fields[j] !== undefined && data1.fields[i].toLowerCase() == data2.fields[j].toLowerCase()) {
									fieldMap[i] = j;
									fieldCount ++;
								}
							}
						}
						if (field && field.toLowerCase() == data1.fields[i].toLowerCase()) fieldIndex = i;
					}
					// if the field we want the child in is not present in the parent
					if (fieldIndex < 0) {
						// add the field
						data1.fields.push(field);
						// remember the position
						fieldIndex = data1.fields.length - 1;
					}
					// use the fields from the child
					var fields = data2.fields;											
					// loop all of the parent (destination) rows
					for (var i in data1.rows) {
						// get the parent row
						var r1 = data1.rows[i];
						// make sure we have as many cells in our row as there are fields
						while (r1.length < data1.fields.length) r1.push(null);							
						// if there are matching fields between the parent (destination) and child (source)
						if (fieldCount > 0) {
							// create a child rows array 
							var childRows = []; 
							// loop the child (source) rows
							for (var j in data2.rows) {
								// get the row
								var r2 = data2.rows[j];
								// assume no matches
								var matches = 0;
								// loop the parent/child field map
								for (var k in fieldMap) {
									// if there is a match of values between the mapped fields in parent/child, count the match
									if (r1[k] == r2[fieldMap[k]]) matches ++; 
								}								
								// if values in all common fields between parent and child have been matched
								if (matches == fieldCount) {															
									// get the matched row from the source
									var row = data2.rows[j];
									// add the matched row to the child data object
									childRows.push(row);									
								}
							}
							// if data was put in the child rows
							if (childRows.length > 0) {
								// use it to create a child data object in the parent
								r1[fieldIndex] = {fields:fields,rows:childRows};
							} else {
								// empty it
								r1[fieldIndex] = null;
							}
						} else {
							// if there is data to merge in and it has rows
							if (data2 && data2.rows && data2.rows.length > 0) {
								// assign the whole child data object to the parent row cell
								r1[fieldIndex] = data2;
							} else {
								// empty the parent row cell
								r1[fieldIndex] = null;
							}
						} // has fields to match on
					} // parent row loop
					data = data1;
				break;
				case "search" :
					var fieldIndexes = [];
					if (field) {
						var fields = field.split(",");
						for (var i in fields) {
							if (fields[i] !== undefined) {
								var f = fields[i].trim().toLowerCase();
								for (var i in data2.fields) {
									if (data2.fields[i] !== undefined && data2.fields[i].toLowerCase() ==  f) {
										fieldIndexes.push(i);
									}
								}
							}
						}
					}
					var data = {fields: data2.fields, rows: []};
					var value = data1.rows[0][0];
					if ((value || value == "") && fieldIndexes.length > 0) {
						value = value.toLowerCase();
						for (var i in data2.rows) {
							for (var j in fieldIndexes) {
								var v = data2.rows[i][fieldIndexes[j]];
								if (v != null && typeof v !== "undefined") {
									if (v !== "" && v.toLowerCase().indexOf(value) > -1) {
										data.rows.push(data2.rows[i]);
										break;
									}
								}
							}
							if (data.rows.length >= maxRows) break;
						}
					}
				break;
			}

		} else {
			data = data1;
		}
	} else {
		data = data2;
	}
	return data;
}

// assume css values are in pixels but if em is recognised convert (other units to follow)
function toPixels(size) {
	if (size) {
		if (size.indexOf("em") == size.length - 2) {
			var emSize = parseFloat($("body").css("font-size"));
		    return (emSize * parseFloat(size));
		} else {
			return parseFloat(size);
		}	
	} else {
		return 0;
	}
}

function getPageVariableValue(name, pageId) {
	
	// assume variable not found
	var value = null;

	// if there was a pageId
	if (pageId) {
		
		// check client-side parameters from navigate action first
		if (window["_pageParams"] && window["_pageParams"][pageId] !== undefined) {
			value = window["_pageParams"][pageId][name];
		}
			
		// if we didn't find anything use the _pageVariables server-populated object
		if (!value && window["_pageVariables_" + pageId] !== undefined) {
			value = window["_pageVariables_" + pageId][name];
		}

	}
	
	// if we still haven't found a value (which should be more current from navigation actions, etc.) look for the variable in the query string
	if (value == null) value = $.getUrlVar(name);

	// we're done
	return value;
}

function Event_initForm(id) {
	$("#" + id + "_form").submit( function() {		
		$("[id][disabled]").each( function () {
			$(this).removeAttr("disabled");
		});
		var hiddenControls = "";
		$(":hidden[id]:not([type=hidden])").each( function(i) {
			var id = $(this).attr("id");
			if (id.indexOf(_pageId) == 0) {
				if (hiddenControls) hiddenControls += ",";
				hiddenControls += id;
			}
		});
		$("#" + id + "_hiddenControls").val(hiddenControls);
	});
}

// disables all form controls and links for submitted forms
function Event_checkForm() {
	// if the form has been submitted and its not a post-submit page
	if (_formSubmitted && !_formPageType == 1) {
		// disable controls
		$("input").disable();
		$("select").disable();
		$("textarea").disable();
		// disable links, except for designer show ones
		$("a:not(#designLink):not(#designLinkNewTab)").unbind("click").click( function(ev){
			return false;
		}).addClass("disabled");
	}
}

function formatDatetime(format, date) {
	// check if format contains a time as well (split on " ")
	var formatParts = format.split(" ");
	var result = "";

	// if we have date and time parts
	if (formatParts.length > 1) {
		
		var dateFormatPart = formatParts[0];
		var timeFormatPart = formatParts[1];
		
		var dateString = $.datepicker.formatDate(dateFormatPart, date);
		var timeString = formatTime(timeFormatPart, date);
		
		result = dateString + " " + timeString;
		
	} else { // we only have one part - either time or date
		
		if (formatParts[0].indexOf("yy") > -1) {
			result = $.datepicker.formatDate(formatParts[0], date);
		} else { // its just a time
			result = formatTime(formatParts[0], date);
		}
		
	}
	
	return result;
}

function formatTime(timeFormat, date) {
	var timePart = "";
	if (timeFormat == "24") {
		timePart = padNumberWithZeros(date.getHours()) + ":" + padNumberWithZeros(date.getMinutes(), 2);
	} else {
		timePart = format12hour(date);
	} 
	
	return timePart;
}

function format12hour(date) {
	  var hours = date.getHours();
	  var minutes = date.getMinutes();
	  var ampmString = hours >= 12 ? 'PM' : 'AM';
	  hours = hours % 12;
	  hours = hours ? hours : 12; // the hour '0' should be '12'
	  minutes = minutes < 10 ? '0' + minutes : minutes;
	  var strTime = hours + ':' + minutes + ' ' + ampmString;
	  return strTime;
}

function padNumberWithZeros(number, length) {
	number += "";
	while (number.length < length) {
		number = "0" + number;
	}
	return number;
}

function promiseSupported() {
	return typeof Promise !== "undefined" && Promise.toString().indexOf("[native code]") !== -1;
}

// font awesome 4 icons codes that have changed - and their changes
var _fontAwesome4to5 = {"f170":"b","f17b":"b","f209":"b","f179":"b","f01a":"r.f358","f190":"r.f359","f18e":"r.f35a","f01b":"r.f35b","f047":"f0b2","f0b2":"f31e","f07e":"f337","f07d":"f338","f1b4":"b","f1b5":"b","f0a2":"r.f0f3","f1f7":"r.f1f6","f171":"b","f172":"b.f171","f097":"r.f02e","f15a":"b","f0f7":"r.f1ad","f133":"r.f133","f150":"r.f150","f191":"r.f191","f152":"r.f152","f151":"r.f151","f20a":"r","f1f3":"b","f1f2":"b","f1f1":"b","f1f4":"b","f1f5":"b","f1f0":"b","f05d":"r.f058","f046":"r.f14a","f10c":"r.f111","f017":"r","f1db":"r.f111","f0ed":"f381","f0ee":"f382","f1cb":"b","f0e5":"r.f075","f0e6":"r.f086","f14e":"r.f14e","f066":"f422","f1f9":"r.f1f9","f09d":"r.f09d","f13c":"b","f0f5":"f2e7","f1a5":"b","f1bd":"b","f1a6":"b","f192":"r","f17d":"b","f16b":"b","f1a9":"b","f1d1":"b","f003":"r.f0e0","f0ec":"f362","f065":"f424","f08e":"f35d","f14c":"f360","f09a":"b","f082":"b","f1c6":"r","f1c7":"r","f1c9":"r","f1c3":"r","f1c5":"r","f016":"r.f15b","f1c1":"r","f1c4":"r","f0c5":"r","f0f6":"r.f15c","f1c8":"r","f1c2":"r","f11d":"r.f024","f16e":"b","f0c7":"r","f114":"r.f07b","f115":"r.f07c","f180":"b","f119":"r","f1d3":"b","f09b":"b","f113":"b","f092":"b","f1d2":"b","f184":"b","f0ac":"f57d","f1a0":"b","f0d5":"b","f0d4":"b","f1ee":"b","f1d4":"b","f0a7":"r","f0a5":"r","f0a4":"r","f0a6":"r","f0a0":"r","f08a":"r.f004","f0f8":"r","f13b":"b","f16d":"b","f208":"b","f1aa":"b","f1cc":"b","f11c":"r","f202":"b","f203":"b","f094":"r","f149":"f3be","f148":"f3bf","f0eb":"r","f0e1":"b","f08c":"b","f17c":"b","f175":"f309","f177":"f30a","f178":"f30b","f176":"f30c","f041":"f3c5","f136":"b","f20c":"b.f2b4","f11a":"r","f147":"r.f146","f10b":"f3cd","f0d6":"r.f3d1","f06e":"r.f06e","f070":"r","f186":"r","f1ea":"r","f19b":"b","f18c":"b","f1d9":"r.f1d8","f1ed":"b","f040":"f303","f03e":"r.f302","f1a7":"b","f1a8":"b","f0d2":"b","f0d3":"b","f01d":"r.f144","f196":"r.f0fe","f1d6":"b","f1d0":"b","f1a1":"b","f1a2":"b","f18b":"b","f112":"f3e5","f045":"r.f14d","f132":"f3ed","f090":"f2f6","f08b":"f2f5","f17e":"b","f198":"b","f1e7":"b","f118":"r","f1be":"b","f1b1":"f2e5","f1bc":"b","f096":"r.f0c8","f18d":"b","f16c":"b","f123":"r.f089","f006":"r.f005","f1b6":"b","f1b7":"b","f1a4":"b","f1a3":"b","f185":"r","f10a":"f3fa","f0e4":"f3fd","f1d5":"b","f088":"r.f165","f087":"r.f164","f145":"f3ff","f05c":"r.f057","f1f8":"f2ed","f014":"r.f2ed","f181":"b","f173":"b","f174":"b","f1e8":"b","f099":"b","f081":"b","f194":"b","f1ca":"b","f189":"b","f18a":"b","f1d7":"b","f17a":"b","f19a":"b","f168":"b","f169":"b","f19e":"b","f1e9":"b","f167":"b","f16a":"b.f167","f166":"b.f431","f022":"r"};
// font awesome 4 icon names that have changed
var _fontAwesome4to5Name = {"headerToggle":"s"};

// return an object with data for a new glyph for the fontawesome 4 to 5 upgrade
function newGlyph(code, control) {
	
	if (code && code != "NaN") {
	
		// replace leading bits
		if (code.indexOf("&#x") == 0) code = code.substring(3, 7);
		
		if (code === "e900") { // rapid glyph
			return {
				code: code,
				letter: "",
				html: "&#x" + code + ";",
				class: "fr",
				toString: function() {
					return this.code;
				}
			}
		} else { // got a code
			var parts = code.split(".");
			
			if (parts.length === 2) { // modern code: s.f1d8;
				var letter = parts[0];
				return {
					code: parts[1],
					letter: letter,
					html: "&#x" + parts[1] + ";",
					class: "fa" + letter,
					toString: function() {
						return letter + "." + this.code;
					}
				};
			} else { // old code: f1d8;
				var letter = "s";
				var difference = _fontAwesome4to5[code];
				if (difference) {
					var differenceParts = difference.split(".");
					if (differenceParts.length === 2) {
						letter = differenceParts[0];
						code = differenceParts[1];
					} else { // 1
						var difference = differenceParts[0];
						if (difference.length === 1) { // letter
							letter = difference;
						} else { // code
							code = difference;
						}
					}
				}
				return {
					code: code,
					letter: letter,
					html: "&#x" + code + ";",
					class: "fa" + letter,
					toString: function() {
						return letter + "." + this.code;
					}
				};
			}
		}
		
	} else {
		
		// assume far
		var cssClass = "far";
		// if we have a control
		if (control) {
			// get cssClasses
			var cssClasses = control.attr("class");
			// if we got some
			if (cssClasses) {
				// split to array
				cssClasses = cssClasses.split(" ");
				// loop them
				for (var i=0; i< cssClasses.length; i++) {
					// get the class
					var c = cssClasses[i];
					// ignore fa
					if (c != "fa") {
						// get this letter					
						var letter = _fontAwesome4to5Name[c];
						// if we got a letter
						if (letter) {
							cssClass = "fa" + letter;
							break;
						}
					}
				} 
			}
		}
		
		return {
			code: "",
			letter: "",
			html: "",
			class: cssClass,
			toString: function() {
				return "";
			}
		}
		
	}
}

// upgrade older app fontawesome 4 glyphs to 5
function upgadePageFontAwesome4to5() {
	
	// elements with "fa" in their class attribute that isn't a div or named class, i.e. fa-next, nor one of our known version 5 classes 
	$(".fa:not(div, [class*='fa-'], .fas, .far, .fab, .updated-fa)").each(function() {
		var control = $(this);
		var code = control.html().charCodeAt(0).toString(16);
		var glyph = newGlyph(code, control);
		if (glyph.class) {
			control.removeClass("fa")
				.addClass(glyph.class)
				.addClass("updated-fa")
				.html(glyph.html);
		}
	});
	// elements with a named font awesome class that haven't been upgraded yet
	$("[class*='fa-']:not(div, .fa, .fas, .far, .fab, .updated-fa)").each(function() {
		var control = $(this, control);
		var code = control.html().charCodeAt(0).toString(16);
		var glyph = newGlyph(code);
		if (glyph.html) {
			control.html(glyph.html)
				.addClass("updated-fa");
		}
	});
	// elements where fontawesome was put directly onto the style attribute
	$("span[style^=\"font-family:'fontawesome'\"]:not(div, .updated-fa)").each(function() {
		var control = $(this);
		var code = control.html().charCodeAt(0).toString(16);
		var glyph = newGlyph(code, control);
		if (glyph.class) {
			control.css({"font-family":""})
				.addClass(glyph.class)
				.addClass("updated-fa")
				.html(glyph.html);
		}
	});
	
	console.log("Updated FA");
}

// Function-call coalescing
function limitCallFrequency(f, period) {
	
	var inPeriod = false;
	var callWithinPeriod = false;
	
	var afterPeriod = function() {
		inPeriod = callWithinPeriod;
		if (callWithinPeriod) {
			callWithinPeriod = false;
			f();
			setTimeout(afterPeriod, period);
		}
	};
	return function() {
		if (inPeriod) {
			callWithinPeriod = true;
		} else {
			f();
			setTimeout(afterPeriod, period);
			inPeriod = true;
		}
	};
}

// do the upgrade when jquery is ready, also used in some controls
$(function() {
	// set a function to time-limit the calls to, and how often
	var f = limitCallFrequency(upgadePageFontAwesome4to5, 1000);
	// run the function as the page loads
	f();
	// create the observer which will call our limited function
	var observer = new MutationObserver(function(ev) {
		f();
	});
	// start the observation
	observer.observe(document.body, {subtree:true, childList:true});
});
