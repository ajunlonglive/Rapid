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

package com.rapid.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.soa.SOAData;
import com.rapid.soa.Webservice;
import com.rapid.soa.SOADataReader.SOAJSONReader;
import com.rapid.soa.SOADataReader.SOAXMLReader;
import com.rapid.soa.SOADataWriter.SOAJSONWriter;
import com.rapid.soa.SOADataWriter.SOAXMLWriter;

public class SOA extends RapidHttpServlet {
			
	private static final long serialVersionUID = 3L;
	
	private static Logger _logger = Logger.getLogger(SOA.class);
	
    public SOA() { super(); }
            
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
										
		PrintWriter out = response.getWriter();
		
		String appId = request.getParameter("a");
		String appVersion = request.getParameter("v");
		String serviceId = request.getParameter("s");
		
		Application app = getApplications().get(appId, appVersion);
						
		if (app == null) {
			
			// tell user app could not be found
			sendException(response, new Exception("Application could not be found"));
			
		} else if (serviceId == null) {
			
			out.print("<html><head><title>Rapid SOA Webservices</title></head><script type='text/javascript' src='scripts/jquery-1.10.2.js'></script><script type='text/javascript' src='scripts/soa.js'></script><body>Wsdls:<p/>");
			
			String dropDown = "<select id='action'>";
			
			// print a list of webservices for this app
			List<Webservice> webservices = app.getWebservices();
			if (webservices == null) {
				out.print("This application has no webservices");
			} else {
				for (Webservice webservice : webservices) {
					dropDown += "<option value='" + app.getId() + "." + webservice.getId() + "'>" + webservice.getName() + "</option>";
					out.print("<a href='soa?a=" + app.getId() + "&s=" + webservice.getId() + "'>" + webservice.getName() + "</a><p/>");
				}
			}
			
			dropDown += "</select>";
			
			out.print("<p>&nbsp;</p>Request:<p/>" + dropDown + "<input type='radio' name='contentType' value='text/xml' checked='checked'/>soap<input type='radio' name='contentType' value='application/json' />json<p/><textarea id='request' style='width:100%;height:200px;'></textarea><p/><button onclick='submitWebservice();'>Submit</button><p/>Response:<p/><div id='response' style='min-height:200px;border:1px solid grey;'></div>");
			
			out.print("</body></html>");
			
		} else {
			
			// try and avoid caching
			response.setHeader("Expires", "Sat, 15 March 1980 12:00:00 GMT");

			// Set standard HTTP/1.1 no-cache headers.
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

			// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
			response.addHeader("Cache-Control", "post-check=0, pre-check=0");

			// Set standard HTTP/1.0 no-cache header.
			response.setHeader("Pragma", "no-cache");
			
			Webservice webservice = app.getWebservice(serviceId);
			
			if (webservice == null) {
				
				sendException(response, new Exception("Webservice could not be found"));
				
			} else {
				
				response.setContentType("text/xml");
				
				URL url = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), request.getRequestURI());
				
				out.print(webservice.getWSDL(appId, appVersion, url.toString()));
								
			}
			
		}
		
		out.close();
			
	}
		

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		RapidRequest rapidRequest = new RapidRequest(this, request);
		
		PrintWriter out = response.getWriter();
		
		String contentType = request.getContentType().toLowerCase();
		
		try {
			
			if (contentType.indexOf("xml") > 0) {
				
				String soapAction = request.getHeader("SOAPAction");
				
				if (soapAction == null) {
					
					throw new Exception("SOAPAction must be provided");
					
				} else {
					
					// split the soapAction using forward slashes
					String[] actionParts = soapAction.trim().split("/");
					if (actionParts.length == 3) {
						
						String appId = actionParts[0];
						String versionId = actionParts[1];
						String serviceId = actionParts[2];
						
						Application application = getApplications().get(appId, versionId);
						
						if (application == null) {
							
							throw new Exception("Application could not be found");
							
						} else {
							
							Webservice webservice = application.getWebservice(serviceId);
							
							if (webservice == null) {
								
								throw new Exception("SOA webservice could not be found");
								
							} else {
								
								StringBuilder requestBody = new StringBuilder();
								BufferedReader bufferedReader = null;					
								InputStream inputStream = request.getInputStream();
								if (inputStream != null) {
									bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
									char[] charBuffer = new char[128];
									int bytesRead = -1;
									while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
										requestBody.append(charBuffer, 0, bytesRead);
									}
								} 					
								bufferedReader.close();
											
								SOAXMLReader xmlReader = new SOAXMLReader(webservice.getRequestSchema());
								
								String xmlRequest = requestBody.toString();
								
								_logger.debug("SOA xml request : " + xmlRequest);
								
								SOAData requestData = xmlReader.read(requestBody.toString());
									
								SOAData soaResponseData = webservice.getResponseData(rapidRequest, application, requestData);
																
								response.setContentType("text/xml");
								
								SOAXMLWriter xmlWriter = new SOAXMLWriter(soaResponseData);
								
								String xmlResponse = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soa=\"http://soa.rapid-is.co.uk\"><soapenv:Body>";
										
								xmlResponse += xmlWriter.write();
								
								xmlResponse += "</soapenv:Body></soapenv:Envelope>";
								
								_logger.debug("SOA xml response : " + xmlResponse);
								
								out.print(xmlResponse);
																							
							}
							
						}
						
					} else {
						
						 throw new Exception("SOAPAction must contain app id, version, and SOA webservice id, seperated by forward slashes. Received " + soapAction);
						 
					}
					
				}
							
			} else if (contentType.indexOf("json") > 0) {
				
				String action = request.getHeader("Action");
				
				if (action == null) {
					
					throw new Exception("\"Action\" request header must be provided");
					
				} else {
					
					// split the soapAction using spaces 
					String[] actionParts = action.replace("\"", "").split(" ");
					if (actionParts.length == 3) {
						
						String appId = actionParts[0];
						String appVersion = actionParts[1];
						String serviceId = actionParts[2];
						
						Application application = getApplications().get(appId, appVersion);
						
						if (application == null) {
							
							throw new Exception("Application could not be found");
							
						} else {
							
							Webservice webservice = application.getWebservice(serviceId);
							
							if (webservice == null) {
								
								throw new Exception("SOA webservice could not be found");
								
							} else {
								
								StringBuilder requestBody = new StringBuilder();
								BufferedReader bufferedReader = null;					
								InputStream inputStream = request.getInputStream();
								if (inputStream != null) {
									bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
									char[] charBuffer = new char[128];
									int bytesRead = -1;
									while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
										requestBody.append(charBuffer, 0, bytesRead);
									}
								} 					
								bufferedReader.close();
									
								SOAJSONReader jsonReader = new SOAJSONReader(webservice.getRequestSchema());
								
								String jsonRequest = requestBody.toString();
								
								_logger.debug("SOA json request : " + jsonRequest);
								
								SOAData requestData = jsonReader.read(jsonRequest);
									
								SOAData soaResponseData = webservice.getResponseData(rapidRequest, application, requestData);
																
								response.setContentType("application/json");
								
								SOAJSONWriter jsonWriter = new SOAJSONWriter(soaResponseData);
								
								String jsonResponse = jsonWriter.write();
								
								_logger.debug("SOA json response : " + jsonResponse);
								
								out.print(jsonResponse);
																							
							}
							
						}
						
					} else {
						
						 throw new Exception("Action must contain app id and SOA webservice id, seperated by \".\"");
					}
					
				}
				
			} else {
				
				throw new Exception("Content type not recognised");
				
			}
			
		} catch (Exception ex) {
			
			sendException(response, ex);
			
		} finally {
		
			out.close();
			
		}												
		
	}

}
