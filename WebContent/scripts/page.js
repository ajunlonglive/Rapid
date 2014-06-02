
// a fake root control for starting the object tree
var _page = {};
// this doesn't do anything here but is referred to in the designer code
var _nextId;

// this loads the control constructors into the page from a json string (used with envJS and Rhino when rebuilding the page server side)
function loadControls(controlsString) {
	
	var controls = JSON.parse(controlsString);
	
	var count = 0;
	
	for (var i in controls) {
		
		// get a reference to a single control
		var c = controls[i];
		// create a new control ControlClass object/function (this is a closure)
		var f = new ControlClass(c);        		        		     			
		// assign the control controlClass function function globally
		window["ControlClass_" + c.type] = f; 
		
		// inc counter
		count ++;
		
	}
	
	return count + " controls loaded into script engine";
	
}

// this loads the controls and rebuilds the page (used with envJS and Rhino when rebuilding the page server side)
function getHtmlBody(controlsString) {
			
	// give _page a child array
	_page.childControls = new Array();
	// make the root control's object the html body
	_page.object = $('body');
	// remove any existing markup
	_page.object.children().remove();
	
	// parse our json page string into the object
	var controls = JSON.parse(controlsString);
	
	// if we have controls
	if (controls) {
    	// loop the page childControls and create
    	for (var i = 0; i < controls.length; i++) {
    		// get an instance of the control properties (which is what we really need from the JSON)
    		var control = controls[i];
    		// create and add (without actions)
    		_page.childControls.push(loadControl(control, _page, false));
    	}
	}
	
	return _page.object.html();
	
}