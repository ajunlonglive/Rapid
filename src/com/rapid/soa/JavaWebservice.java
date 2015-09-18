/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as 
published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version. The terms require you 
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.soa;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Classes;

public class JavaWebservice extends Webservice {
	
	// all classes used by JavaWebservices are expected to implement this interface 
	public interface Response {
		
		public Object getResponse();

	}
		
	// private variables
	private String _className;
	
	// properties
	
	// this is the request object, it must implement the JavaWebserviceRequest interface
	public String getClassName() { return _className; }
	public void setClassName(String className) { _className = className; }
	
	// constructors
	
	// used by Jaxb
	public JavaWebservice() {}
	// used by Designer
	public JavaWebservice(String name) {
		setName(name);
	}

	@Override
	public SOAData getResponseData(RapidRequest rapidRequest, Application application, SOAData requestData)	throws WebserviceException {
		
		// instantiate an instance of _className
		// use requestData to inflate it's properties (or public instance variables)
		// invoke it's JavaWebservice.Response.getResponse() method
		// marshal the response object to an SOAData object
		// return the SOAData object (and the printer will take care of the rest)
		
		try {
			
			// get the class
			Class c = Class.forName(_className);
			
			// make sure it implements JavaWebservice.Response
			if (!Classes.implementsClass(c, com.rapid.soa.JavaWebservice.Response.class)) throw new WebserviceException("Webservice action class " + c.getCanonicalName() + " must implement com.rapid.soa.JavaWebservice.Response.");
			
			// get the parameterless constructor
			Constructor constructor = c.getConstructor();
			
			// get an instance of the object
			JavaWebservice.Response requestObject = (JavaWebservice.Response) constructor.newInstance();
			
			/////////////////////////////////////////////////////////////////////////////////////// here we use requestData to populate the requestObject //////////////////////////////////////////////////////////////////////////////////////////////
			
			// get the response method
			Method method = c.getMethod("getResponse");
			
			// invoke the response method
			Object responseObject = method.invoke(requestObject);
						
			SOAData responseData = new SOAData(new SOAElement(c.getSimpleName(),"values coming soon"));
			
			/////////////////////////////////////////////////////////////////////////////////////// here we use responseObject to populate the responseData object //////////////////////////////////////////////////////////////////////////////////////////////
			
			// return the response data
			return responseData;
			
		} catch (Exception ex) {

			throw new WebserviceException(ex);
			
		} 
				
	}

}
