package com.rapid.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Http {
	
	public static String post(String url, String body) throws IOException {
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
		//add request header
		con.setRequestMethod("POST");
 
		// Set up for post request
		con.setDoOutput(true);
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

}
