package com.rapid.core;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.server.RapidHttpServlet;

public abstract class Process extends Thread {

	// protected instance variables	
	protected ServletContext _servletContext;
	protected String _name;
	protected int _interval;
	protected boolean _stopped;
		
	// protected static variables
	protected Logger _logger;
	
	// constructor
	public Process(ServletContext servletContext, String name, int interval) {
		// store values
		_servletContext = servletContext;
		_name = name;
		_interval = interval;
		// get a logger
		_logger = LogManager.getLogger(RapidHttpServlet.class);
	}
	
	// abstract methods
	public abstract void doProcess();
	
	// protected methods
	protected Applications getApplications() {
		return (Applications) _servletContext.getAttribute("applications");
	}
	
	// override methods
	@Override
	public void start() {
		super.start();
		// log that we've started
		_logger.info("Process " + _name + " has started, checking every " + _interval + " seconds");
	}
		
	@Override
	public void run() {
		// loop until stopped
		while (!_stopped) {
			// run the abstract method
			doProcess();							
			// sleep for set interval
			try {
				Thread.sleep(_interval * 1000);
			} catch (InterruptedException ex) {
				_logger.error("Process " + _name + " was interrupted!", ex);
				_stopped = true;
			}
		}		
		// log stopped
		_logger.error("Process " + _name + " has stopped");
	}
	
	@Override
	public void interrupt() {
		_stopped = true;
		super.interrupt();		
	}
	
}
