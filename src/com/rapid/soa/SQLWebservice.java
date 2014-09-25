/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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

*/

package com.rapid.soa;

import java.sql.Date;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import com.rapid.core.Application;
import com.rapid.core.Application.DatabaseConnection;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DataFactory.Parameters;
import com.rapid.server.RapidRequest;
import com.rapid.soa.SOASchema.SOASchemaElement;

public class SQLWebservice extends Webservice {
		
	// private variables
	
	private int _databaseConnectionIndex = -1;
	private String _sql;
		
	// properties
	
	public int getDatabaseConnectionIndex() { return _databaseConnectionIndex; }
	public void setDatabaseConnectionIndex(int databaseConnectionIndex) { _databaseConnectionIndex = databaseConnectionIndex; }
	
	public String getSQL() { return _sql; }
	public void setSQL(String sql) { _sql = sql; }
	
	// constructors
		
	public SQLWebservice() {}
	public SQLWebservice(String name) {
		setName(name);
	}
	
	// overrides
	
	@Override
	public SOAData getResponseData(RapidRequest rapidRequest, Application application, SOAData requestData) throws WebserviceException {
		
		try {
			
			SimpleDateFormat dateFormatter = null;
			
			Date date = null;
												
			DatabaseConnection databaseConnection = application.getDatabaseConnections().get(_databaseConnectionIndex);
			
			ConnectionAdapter ca = databaseConnection.getConnectionAdapter(rapidRequest.getRapidServlet().getServletContext());			
			
			DataFactory df = new DataFactory(ca);
			
			Parameters parameters = new Parameters();
			
			SOASchemaElement requestSchemaElement = _requestSchema.getRootElement();
			
			SOAElement requestElement = requestData.getRootElement();
						
			for (int i = 0; i < requestSchemaElement.getChildElements().size(); i++) {
				
				SOASchemaElement childRequestSchemaElement = requestSchemaElement.getChildElements().get(i);
				
				SOAElement childRequestElement = requestElement.getChildElements().get(i);
				
				switch (childRequestSchemaElement.getDataType()) {
					case SOASchema.INTEGER :							
						parameters.add(Integer.parseInt(childRequestElement.getValue()));
						break;							
					case SOASchema.DECIMAL :							
						parameters.add(Float.parseFloat(childRequestElement.getValue()));							
						break;							
					case SOASchema.DATE :	
						dateFormatter = rapidRequest.getRapidServlet().getXMLDateFormatter();
						long dateLong = dateFormatter.parse(childRequestElement.getValue()).getTime();
						date = new Date(dateLong);
						parameters.add(date);
						break;						
					case SOASchema.DATETIME :
						dateFormatter = rapidRequest.getRapidServlet().getXMLDateTimeFormatter();
						long dateTimeLong = dateFormatter.parse(childRequestElement.getValue()).getTime();
						date = new Date(dateTimeLong);
						parameters.add(date);
						break;						
					default:
						parameters.add(childRequestElement.getValue());
				}
				
			}
									
			// get the resultset!
			
			ResultSet rs = df.getPreparedResultSet(rapidRequest, _sql, parameters);
									
			SOASchemaElement responseSchemaElement = _responseSchema.getRootElement();
			
			SOAElement responseElement = new SOAElement(responseSchemaElement.getName(), responseSchemaElement.getIsArray());
															
			while (rs.next()) {
				
				for (SOASchemaElement responseChildElementSchema : responseSchemaElement.getChildElements()) {
					
					String elementName = responseChildElementSchema.getName();
					
					int elementType = responseChildElementSchema.getDataType();
															
					String fieldName = responseChildElementSchema.getField();
					
					String elementValue = null;
																																			
					switch (elementType) {
						case SOASchema.INTEGER :							
							elementValue = Integer.toString(rs.getInt(fieldName));							
							break;							
						case SOASchema.DECIMAL :							
							elementValue = Float.toString(rs.getFloat(fieldName));							
							break;							
						case SOASchema.DATE :							
							dateFormatter = rapidRequest.getRapidServlet().getXMLDateFormatter();							
							date = rs.getDate(fieldName);							
							if (date != null) elementValue = dateFormatter.format(date);							
							break;							
						case SOASchema.DATETIME :							
							dateFormatter = rapidRequest.getRapidServlet().getXMLDateTimeFormatter();							
							date = rs.getDate(fieldName);							
							if (date != null) elementValue = dateFormatter.format(date);							
							break;
						default:
							elementValue = rs.getString(fieldName);
					}
					
					SOAElement responseChildElement = new SOAElement(elementName, elementValue);
					
					responseElement.addChildElement(responseChildElement);
					
				}
												
				// check whether the parent element is an array
				if (responseSchemaElement.getIsArray()) {
					// if so close the array, which means further child elements are now added to a new parent element in the array collection
					responseElement.closeArray();
				} else {
					// only use the first row in the record set if this is not an array
					break;
				}
				
			}
					
			SOAData responseData = new SOAData(responseElement);
			
			return responseData;
			
		} catch (Exception ex) {
			
			throw new WebserviceException(ex);
			
		}
		
		
	}
		
}
