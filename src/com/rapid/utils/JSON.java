package com.rapid.utils;

import org.json.JSONArray;
import org.json.JSONException;

public class JSON {
	
	public static String[] getStringArray(JSONArray jsonArray) throws JSONException {
		
		String[] strings = null;
		
		if (jsonArray != null) {
			
			strings = new String[jsonArray.length()];
			
			for (int i = 0; i < jsonArray.length(); i++) {
				
				strings[i] = jsonArray.getString(i);
				
			}
			
		}
		
		return strings;
		
	}

}
