package com.rapid.utils;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JAXB {
	
	public static class SecureAdapter extends XmlAdapter<String, String>
	{
	    @Override
	    public String unmarshal( String s )
	    {
	        try {
				return s == null ? null : Encryption.decrypt(s);
			} catch (Exception e) {
				return null;
			}
			
	    }

	    @Override
	    public String marshal( String s )
	    {
	        try {
				return s == null ? null : Encryption.encrypt(s);
			} catch (Exception e) {
				return null;
			}
	    }
	}

}
