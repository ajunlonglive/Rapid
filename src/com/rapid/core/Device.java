package com.rapid.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

public class Device {

	@XmlRootElement
	public static class Devices extends ArrayList<Device> {
		
		// properties (for marshelling / unmarshelling)
		public Devices getDevices() { return this; }
		public void setDevices(Devices devices) { this.addAll(devices); }
		
		// static methods
		public static Devices load(ServletContext servletContext) throws FileNotFoundException, JAXBException {
			
			// create the list
			Devices devices = null;
			
			// get the file in which the control xml files are stored
			File file = new File(servletContext.getRealPath("/WEB-INF/devices/devices.xml"));
			
			// if it exists
			if (file.exists()) {
				try {
					// get the unmarshaller from the context
					Unmarshaller unmarshaller = (Unmarshaller) servletContext.getAttribute("unmarshaller");	
					// unmarshall the devices
					devices = (Devices) unmarshaller.unmarshal(file);
				} catch (JAXBException ex) {
					// log
					Logger.getLogger(Device.class).error("Error loading devices", ex);
				}
			} 
			
			// create a new list if we haven't got one yet
			if (devices == null) devices = new Devices();
					
			// assume the top device is not a normal monitor (called screen)
			boolean gotScreen = false;
			// check device count
			if (devices.size() > 0) {
				// get top device
				Device device = devices.get(0);
				// if this is the screen
				if ("Desktop".equals(device.getName()) && device.getPPI() == 96 && device.getScale() == 1d) gotScreen = true;
			}
			
			// if we don't have a screen
			if (!gotScreen) {
				// add it to the top of the collection
				devices.add(0, new Device("Desktop", 0, 0, 96, 1d));
				// save the file
				save(servletContext, devices);
			}
			
			// add them to the servlet context
			servletContext.setAttribute("devices", devices);
		    	
			// return the devices	
			return devices;
			
		}
		
		public static void save(ServletContext servletContext, Devices devices) throws FileNotFoundException, JAXBException {
			
			// get the file in which the control xml files are stored
			File file = new File(servletContext.getRealPath("/WEB-INF/devices/devices.xml"));
			// make dirs if need be
			if (!file.exists()) file.getParentFile().mkdirs();
			
			// get the marshaller from the context
			Marshaller marshaller = (Marshaller) servletContext.getAttribute("marshaller");	
			// marshall the devices
			marshaller.marshal(devices, new FileOutputStream(file));
					
		}
		
	}
	
	// private instance variables
	private String _name;
	private int _width, _height, _ppi;
	private double _scale;
	
	// properties
	public String getName() { return _name; }
	public void setName(String name) { _name = name; }
	
	public int getWidth() { return _width; }
	public void setWidth(int width) { _width = width; }
	
	public int getHeight() { return _height; }
	public void setHeight(int height) { _height = height; }
	
	public int getPPI() { return _ppi; }
	public void setPPI(int ppi) { _ppi = ppi; }
	
	public double getScale() { return _scale; }
	public void setScale(double scale) { _scale = scale; }
		
	// constructors
	public Device() {}
	public Device(String name, int width, int height, int ppi, double scale) {
		_name = name;
		_width = width;
		_height = height;
		_ppi = ppi;
		_scale = scale;
	}
				
}
