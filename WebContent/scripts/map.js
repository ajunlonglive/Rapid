


function createMapEntry(list, c) {
	// create the list entry
	list.append("<li><span data-id='" + c.id + "'>" + ((c._class.image) ? "<img src='" + c._class.image + "'/>" : "") + c.type + (c.name ? " - " + c.name: "") + "</span></li>");
	// get the list entry
	var li = list.children("li").last();
	// add an onclick listener if the object is visible
	li.click( c, function(ev) {
		// select the control
		selectControl(ev.data);
		// stop bubbling
		return false;
	});	
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
function showPageMap() {	
	// get the map div
	var map = $("#pageMap");
	// only if its visible
	if (map.is(":visible")) {		
		// empty the current map
		map.html("");		
		// check we have a page and childControls
		if (_page) {
			// create the first list
			map.append("<ul></ul>");
			// get the list
			var list = map.find("ul").last();
			// build the map
			createMapEntry(list, _page);
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
		// resize the window
		windowResize("controlsMap hide");
	} else {
		// show controls
		$("#controlsList").hide();
		// show the map
		$("#pageMap").show("slide", {direction: "down"}, 500);
		// rebuild the controls
		showPageMap();
		// resize the window
		windowResize("controlsMap show");
	}
}

//JQuery is ready! 
$(document).ready( function() {
	
	// controls are clicked on
	$("#controlControls").click( function(ev) {
		toggleMap();		
		return false;
	});
	
	// map is clicked on
	$("#controlsMap").click( function(ev) {		
		toggleMap();
		return false;
	});
	
});	
