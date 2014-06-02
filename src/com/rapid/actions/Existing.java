package com.rapid.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Event;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

public class Existing extends Action {
		
	// constructors
	
	public Existing() {}	
	public Existing(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws JSONException { super(rapidServlet, jsonAction); }

	// methods
	
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		// get the action id
		String actionId = getProperty("action");
		// check we got something
		if (actionId == null) {
			return "";
		} else {
			// get the control for this action
			Control actionControl = page.getActionControl(actionId);			
			// if there is no control this is a page event
			if (actionControl == null) {
				// get the event
				Event existingEvent = page.getActionEvent(actionId);
				// if we got an existing event update the type name
				if (existingEvent == null) {
					return "";
				} else {
					// return
					return "Event_" + existingEvent.getType() + "_" + page.getId() + "(ev)";
				}								
			} else {				
				// get the action property
				Action existingAction = page.getAction(actionId);
				// check we got something
				if (existingAction == null) {
					return "";
				} else {
					return existingAction.getJavaScript(application, page, actionControl);
				}
			}
		}		
	}
	
}
