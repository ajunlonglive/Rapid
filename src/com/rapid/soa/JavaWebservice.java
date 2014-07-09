package com.rapid.soa;

import com.rapid.core.Application;
import com.rapid.server.RapidHttpServlet.RapidRequest;

public class JavaWebservice extends Webservice {
	
	// private variables
	private String _className;
	
	// properties
	
	public String getClassName() { return _className; }
	public void setClassName(String className) { _className = className; }
	
	// constructors
	public JavaWebservice() {}
	
	public JavaWebservice(String name) {
		setName(name);
	}

	@Override
	public SOAData getResponseData(RapidRequest rapidRequest, Application application, SOAData requestData)	throws WebserviceException {
		return null;
	}

}
