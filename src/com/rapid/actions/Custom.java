package com.rapid.actions;

import org.json.JSONObject;

import com.rapid.core.Action;
import com.rapid.core.Application;
import com.rapid.core.Control;
import com.rapid.core.Page;
import com.rapid.server.RapidHttpServlet;

public class Custom extends Action {
	
	// parameterless constructor (required for jaxb)
	Custom() { super(); }
	// designer constructor
	public Custom(RapidHttpServlet rapidServlet, JSONObject jsonAction) throws Exception { 
		super(rapidServlet, jsonAction);				
	}
		
	// methods
	
	@Override
	public String getJavaScript(Application application, Page page, Control control) {
		// get the JavaScript
		String javaScript = getProperty("javascript");
		// if we have some javascript		
		if (javaScript == null) {
			return "";
		} else {
			// get the name of what it's applied to (start with the page as the control can be null)
			String name = "page \"" + page.getTitle() + "\"";
			// if the control isn't null add it to the name too
			if (control != null) name += " control \"" + control.getName() + "\"";
			// send a the JavaScript wrapped in a try/catch
			return "try {\n  " + javaScript.trim() + "\n  } catch (ex) { alert('Error with custom action from " + name + " : ' + ex); }\n";
		}
	}
	
}
