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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.soa.Webservice.WebserviceException;
import com.rapid.utils.Classes;

public class JavaWebservice extends Webservice {
	
	// all classes used by JavaWebservices are expected to implement this interface 
	public interface Response {
		
		public Object getResponse(RapidRequest rapidRequest) throws WebserviceException;

	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
	public @interface XSDchoice {}
			
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDname { public String name(); }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDtype { public String name(); }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDorder { public int value(); }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminOccurs { public int value(); }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxOccurs { public int value(); }	
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDnillable { public boolean value(); }	
		
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminLength { public int value(); }	
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxLength { public int value(); }	
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDpattern { public String value(); }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDenumeration { public String value(); }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminInclusive{ public String value(); }	
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxInclusive { public String value(); }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminExclusive{ public String value(); }	
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxExclusive { public String value(); }
		
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
	
	// private methods
	private Class getNamedClass() throws Exception {		
		// get the class
		Class c = Class.forName(_className);		
		// make sure it implements JavaWebservice.Response
		if (!Classes.implementsClass(c, com.rapid.soa.JavaWebservice.Response.class)) throw new WebserviceException("Webservice action class " + c.getCanonicalName() + " must implement com.rapid.soa.JavaWebservice.Response.");						
		return c;		
	}
	
	private SOASchema getClassSchema(Class schemaClass) {
		SOASchema classSchema = new SOASchema(schemaClass.getSimpleName());
		///////////////////////////////////////////////////////////////////////////////// here we build a schema from a class ////////////////////////////////////////////////////////////////////////////////////////
		return classSchema;
	}
	
	@Override
	public SOASchema getRequestSchema() {
		if (_requestSchema == null) {
			try {
				// get the request class
				Class requestClass = getNamedClass();
				// now make a schema from it
				_requestSchema = getClassSchema(requestClass);
			} catch (Exception ex) {
				
			}
		}
		return _requestSchema; 
	}
	
	@Override
	public SOASchema getResponseSchema() {
		if (_responseSchema == null) {
			try {
				// get the request class
				Class requestClass = getNamedClass();
				// get the response method
				Method responseMethod = requestClass.getMethod("getResponse", RapidRequest.class);
				// get the response class
				Class responseClass = responseMethod.getReturnType();
				// now make a schema from it
				_responseSchema = getClassSchema(responseClass);
			} catch (Exception ex) {
				
			}
		}
		return _responseSchema;
	}

	@Override
	public SOAData getResponseData(RapidRequest rapidRequest, SOAData requestData)	throws WebserviceException {
		
		// instantiate an instance of _className
		// use requestData to inflate it's properties (or public instance variables)
		// invoke it's JavaWebservice.Response.getResponse() method
		// marshal the response object to an SOAData object
		// return the SOAData object (and the printer will take care of the rest)
		
		try {
			
			// get the request class
			Class requestClass = getNamedClass();
			
			// get the parameterless constructor
			Constructor constructor = requestClass.getConstructor();
			
			// get an instance of the object
			JavaWebservice.Response requestObject = (JavaWebservice.Response) constructor.newInstance();
			
			/////////////////////////////////////////////////////////////////////////////////////// here we use requestData to populate the requestObject //////////////////////////////////////////////////////////////////////////////////////////////
			
			// get the response method
			Method responseMethod = requestClass.getMethod("getResponse", RapidRequest.class);
			
			// invoke the response method
			Object responseObject = responseMethod.invoke(requestObject, rapidRequest);
						
			SOAData responseData = new SOAData(new SOAElement(requestClass.getSimpleName(),"values coming soon"));
			
			/////////////////////////////////////////////////////////////////////////////////////// here we use responseObject to populate the responseData object //////////////////////////////////////////////////////////////////////////////////////////////
			
			// return the response data
			return responseData;
			
		} catch (Exception ex) {

			throw new WebserviceException(ex);
			
		} 
				
	}

}
