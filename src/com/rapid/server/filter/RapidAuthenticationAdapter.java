package com.rapid.server.filter;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public abstract class RapidAuthenticationAdapter {
	
	protected ServletContext _servletContext;
	
	public ServletContext getServletContext() { return _servletContext; }
	
	public RapidAuthenticationAdapter(ServletContext servletContext) {
		_servletContext = servletContext;
	}
	
	public abstract ServletRequest process(ServletRequest request, ServletResponse response) throws IOException, ServletException;

}
