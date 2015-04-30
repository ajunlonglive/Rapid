package com.rapid.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Http {
	
	public static class Header {
		
		// private instance variables
		String _name, _value;
		
		// properties
		public String getName() { return _name; }
		public String getValue() { return _value; }
		
		// constructor
		public Header(String name, String value) {
			_name = name;
			_value = value;
		}		
		
	}
	
	// main method
	
	public static String post(String url, List<Header> headers, String body) throws IOException {
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		//add request header
		con.setRequestMethod("POST");
 
		// Set up for post request
		con.setDoOutput(true);
		
		// look for any headers
		if (headers != null) {
			// loop them
			for (Header header : headers) {
				con.setRequestProperty(header.getName(), header.getValue());
			}
		}
		
		// get the writer
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		// write the request
		wr.writeBytes(body);
		// flush
		wr.flush();
		// close
		wr.close();
 
		// get the repsonse code
		int responseCode = con.getResponseCode();
		// placeholder for response body
		String response = null;
		
		// check if all ok
		if (responseCode == 200) {
			// all ok get response from input stream
			response = Strings.getString(con.getInputStream());
		} else {
			// not ok get response from error stream
			response = Strings.getString(con.getErrorStream());
		}
 
		// return response
		return response;
	}
	
	// overloads
	
	public static String post(String url, String body) throws IOException {
		
		return post(url, null, body);
		
	}
	
	public static String postSOAP(String url, String soapAction, String body) throws IOException {
		
		List<Header> headers = new ArrayList<Header>();
		headers.add(new Header("SOAPAction",soapAction));
		headers.add(new Header("Content-Type","text/xml"));
		
		return post(url, headers, body);
		
	}

}
