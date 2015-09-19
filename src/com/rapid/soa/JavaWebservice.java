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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.rapid.core.Application;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.soa.SOAElementRestriction.*;
import com.rapid.soa.Webservice.WebserviceException;
import com.rapid.utils.Classes;

public class JavaWebservice extends Webservice {
	
	// all classes used by JavaWebservices are expected to implement this interface 
	public interface Request {
		
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
	
	// schema element properties can come from fields or methods so we collect them before sorting
	public static class ElementProperty {
		
		// private instance variables
		private String _name;
		private Class _class; 
		private Annotation[] _annotations;
		
		// properties
		public String getName() { return _name; }
		public Class getPropertyClass() { return _class; }
		public Annotation[] getAnnotations() { return _annotations; }
						
		// constructor
		public ElementProperty(String name, Class propertyClass, Annotation[] annotations) {
			_name = name;
			_class = propertyClass;
			_annotations = annotations;
		}
		
		// methods
		public int getOrder() {
			if (_annotations != null) {
				for (Annotation a : _annotations) {
					if (a instanceof XSDorder) {
						XSDorder o = (XSDorder) a;
						return o.value();
					}
				}
			}
			return -1;
		}
		
	}
			
	// private variables
	private String _className;
	private Logger _logger;
	
	// properties
	
	// this is the request object, it must implement the JavaWebserviceRequest interface
	public String getClassName() { return _className; }
	public void setClassName(String className) { _className = className; }
	
	// constructors
	
	// used by Jaxb
	public JavaWebservice() {
		_logger = Logger.getLogger(this.getClass());
	}
	// used by Designer
	public JavaWebservice(String name) {
		this();
		setName(name);
	}
	
	// private methods
	
	// is this an arry type
	private static boolean isArray(Class c) {				
		return c.isArray() || 
				Classes.implementsClass(c, java.util.List.class)  
				? true : false;		
	}
			
	// is this a java class or any of the primitives
	private static boolean isSimpleType(Class c) {				
		return c.getName().indexOf("java") == 0 || 
				c.getName().equals("int") || 
				c.getName().equals("boolean") || 
				c.getName().equals("float") 
				? true : false;		
	}
	
	// what is the simple type
	private static int getSimpleType(Class c) {		
		int type = SOASchema.STRING;
		if (c.getSimpleName().equals("boolean")) { type = SOASchema.BOOLEAN; }
		else if (c.getSimpleName().equals("Date")) { type = SOASchema.DATE; }
		else if (c.getSimpleName().equals("Timestamp")) { type = SOASchema.DATETIME; }
		else if (c.getSimpleName().equals("float") || c.getSimpleName().equals("Float")) { type = SOASchema.DECIMAL; }
		else if (c.getSimpleName().equals("int") || c.getSimpleName().equals("Integer")) { type = SOASchema.INTEGER; }		 						
		return type;		
	}
	
	private boolean isXSDAnnotation(Annotation a) {		
		if (a instanceof XSDchoice || 
				a instanceof XSDname || 
				a instanceof XSDtype || 
				a instanceof XSDorder || 
				a instanceof XSDminOccurs || 
				a instanceof XSDmaxOccurs || 
				a instanceof XSDnillable || 
				a instanceof XSDminLength || 
				a instanceof XSDmaxLength|| 
				a instanceof XSDpattern || 
				a instanceof XSDenumeration || 
				a instanceof XSDminInclusive || 
				a instanceof XSDmaxInclusive || 
				a instanceof XSDminExclusive || 
				a instanceof XSDmaxExclusive ) {			
			return true;			
		} else {			
			return false;			
		}				
	}
	
	// whether there is a set method for a corresponding get
	private boolean containsSetMethod(Method[] methods, Method method) {
		for (Method m : methods) {
			if (m.getName().equals("set" + method.getName().substring(3))) return true;
		}
		return false;
	}
	
	private List<SOASchemaElement> getChildClassSchemaElements(Class c, String parentId) {
		
		// a list of properties in the class		
		List<ElementProperty> elementProperties = new ArrayList<ElementProperty>();
		
		// get all class methods
		Method[] methods = c.getMethods();	
		// if we got some
		if (methods != null) {
			// loop all retained method names looking for get/set pairs
			for (Method m : methods) {
				// work with the get methods as we need the return type for our class
				if (m.getName().startsWith("get")) {
					// if we have a corresponding set method in the list
					if (containsSetMethod(methods, m)) {
						// get the return class
						Class rc = m.getReturnType();
						// check if array
						elementProperties.add(new ElementProperty(m.getName().substring(3), rc, m.getAnnotations()));
					}
				}						
			}
		}
		
		// get all fields
		Field[] fields = c.getFields();
		// if we got some
		if (fields != null) {
			// loop them
			for (Field f : fields) {
				// get the annotations
				Annotation[] annotations = f.getAnnotations();
				// if we got some
				if (annotations != null) {
					// loop the annotations
					for (Annotation a : annotations) {
						// if this is an XSD annotation
						if (isXSDAnnotation(a)) {
							// put our property collection
							elementProperties.add(new ElementProperty(f.getName(), f.getType(), f.getAnnotations()));
							// we're done with the annotations
							break;
						}						
					}														
				}								
			}									
		}
		
		// sort the properties
		Collections.sort(elementProperties, new Comparator<ElementProperty>() {
			@Override
			public int compare(ElementProperty p1, ElementProperty p2) {
				return p1.getOrder() - p2.getOrder();
			}			
		});
						
		// the list we're making
		List<SOASchemaElement> elements = new ArrayList<SOASchemaElement>();
		// first id
		int id = 0;
		// loop the properties
		for (ElementProperty p : elementProperties) {
			// add a schema element for the property class
			elements.add(getClassSchemaElement(p.getName(), p.getPropertyClass(), p.getAnnotations(), parentId + "." + id));
			// increment the id
			id++;
		}		
		// return 
		return elements;
	}
	
	private SOASchemaElement getClassSchemaElement(String name, Class c, Annotation[] annotations, String id) {
		// the schema element we're making
		SOASchemaElement e = new SOASchemaElement();
		// set it's name
		e.setName(name.substring(0,1).toLowerCase() + name.substring(1));
		// set it's id
		e.setId(id);
		// simple type check
		if (isSimpleType(c)) {
			// set it's type
			e.setDataType(getSimpleType(c));			
		} else {
			// if it's an array type
			if (isArray(c)) {
				// set flag
				e.setIsArray(true);
				// if list
				if (Classes.implementsClass(c, java.util.List.class)) {
					// get the generic super class type
					ParameterizedType pt = (ParameterizedType) c.getGenericSuperclass();
					// get it's name
					Type t = pt.getActualTypeArguments()[0];
					// get the name, trimming out the prefix
					String n = t.toString().replace("class ", "");
					// get the class
					try {
						c = Class.forName(n);
					} catch (ClassNotFoundException ex) {
						_logger.error("Error creating schema element for class " + n, ex);
					}
				} else {
					// get the array type
					c = c.getComponentType();
				}
			}			
			// add child elements
			e.setChildElements(getChildClassSchemaElements(c, id));
		}						
		
		// if there are some annotations
		if (annotations != null) {
			// loop them
			for (Annotation a : annotations) {
				// if this is an xsd annotation
				if (isXSDAnnotation(a)) {
					// add appropriate restriction
					if (a instanceof XSDchoice) {
						XSDchoice x = (XSDchoice) a;

					} else if (a instanceof XSDminOccurs) {
						XSDminOccurs x = (XSDminOccurs) a;
						e.addRestriction(new MinOccursRestriction(x.value()));
					} else if (a instanceof XSDmaxOccurs) {
						XSDmaxOccurs x = (XSDmaxOccurs) a;
						e.addRestriction(new MaxOccursRestriction(x.value()));
					} else if (a instanceof XSDnillable) {
						XSDnillable x = (XSDnillable) a;
						
					} else if (a instanceof XSDminLength) {
						XSDminLength x = (XSDminLength) a;
						
					} else if (a instanceof XSDmaxLength) {
						XSDmaxLength x = (XSDmaxLength) a;
						
					} else if (a instanceof XSDpattern) {
						XSDpattern x = (XSDpattern) a;
						
					} else if (a instanceof XSDenumeration) {
						XSDenumeration x = (XSDenumeration) a;
						e.addRestriction(new EnumerationRestriction(x.value()));
					} else if (a instanceof XSDminInclusive) {
						XSDminInclusive x = (XSDminInclusive) a;
						
					} else if (a instanceof XSDmaxInclusive) {
						XSDmaxInclusive x = (XSDmaxInclusive) a;
						
					} else if (a instanceof XSDminExclusive) {
						XSDminExclusive x = (XSDminExclusive) a;
						
					} else if (a instanceof XSDmaxExclusive ) {
						XSDmaxExclusive x = (XSDmaxExclusive) a;
					} 					
				}
			}
		}
		// return it
		return e;
	}
			
	private SOASchema getClassSchema(Class c) {
		// the schema we're making
		SOASchema classSchema = new SOASchema();
		// get the element for the root class
		SOASchemaElement rootSchemaElement = getClassSchemaElement(c.getSimpleName(), c, c.getAnnotations(),"0");
		// add it to the schema
		classSchema.setRootElement(rootSchemaElement);
		// return it
		return classSchema;
	}
	
	// get the class of our named request object
	private Class getRequestClass() throws Exception {		
		// get the class
		Class c = Class.forName(_className);		
		// make sure it implements JavaWebservice.Response
		if (!Classes.implementsClass(c, com.rapid.soa.JavaWebservice.Request.class)) throw new WebserviceException("Webservice action class " + c.getCanonicalName() + " must implement com.rapid.soa.JavaWebservice.Response.");						
		return c;		
	}
	
	@Override
	public SOASchema getRequestSchema() {
		//if (_requestSchema == null) {
			try {
				// get the request class
				Class requestClass = getRequestClass();
				// now make a schema from it
				_requestSchema = getClassSchema(requestClass);
			} catch (Exception ex) {
				_logger.error("Error creating request schema for Java webservice", ex);
			}
		//}
		return _requestSchema; 
	}
	
	@Override
	public SOASchema getResponseSchema() {
		//if (_responseSchema == null) {
			try {
				// get the request class
				Class requestClass = getRequestClass();
				// get the response method
				Method responseMethod = requestClass.getMethod("getResponse", RapidRequest.class);
				// get the response class
				Class responseClass = responseMethod.getReturnType();
				// now make a schema from it
				_responseSchema = getClassSchema(responseClass);
			} catch (Exception ex) {
				_logger.error("Error creating response schema for Java webservice", ex);
			}
		//}
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
			Class requestClass = getRequestClass();
			
			// get the parameterless constructor
			Constructor constructor = requestClass.getConstructor();
			
			// get an instance of the request object
			JavaWebservice.Request requestObject = (JavaWebservice.Request) constructor.newInstance();
			
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
