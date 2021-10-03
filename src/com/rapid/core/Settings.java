/*

Copyright (C) 2021 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
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

package com.rapid.core;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.security.RapidSecurityAdapter;
import com.rapid.server.RapidHttpServlet;
import com.rapid.utils.Files;

// a class of useful application settings that will be stored in a separate file and can be easily switched between in Rapid Admin
@XmlRootElement
public class Settings {

	// private instance variables

	private static Logger _logger = LogManager.getLogger(RapidSecurityAdapter.class);
	private String _id, _name;

	// properties - getters and settings required for full JAXB marshaling

	public String getId() {	return _id; }
	public void setId(String id) { _id = id; }

	public String getName() { return _name;	}
	public void setName(String name) { _name = name; }

	// parameterless constructor for JAXB
	public Settings() {}

	// helper constructor for use from Rapid action
	public Settings(Application application, String name) {
		// get the id by simplifying the name
		_id = Files.safeName(name).toLowerCase();
		// retain the name
		_name = name;
	}


	// instance methods

	private File getFile(ServletContext servletContext, Application application) {
		return  new File(servletContext.getRealPath("") + "/WEB-INF/applications/" + application.getId() + "/" + application.getVersion() + "/" + _id + ".settings.xml");

	}

	public void save(ServletContext servletContext, Application application) throws JAXBException {

		JAXBContext jaxb = JAXBContext.newInstance(Settings.class);
		Marshaller marshaller = jaxb.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setAdapter(RapidHttpServlet.getEncryptedXmlAdapter());

		File file = getFile(servletContext, application);

		marshaller.marshal(this, file);

	}

	public void delete(ServletContext servletContext, Application application) {

		File file = getFile(servletContext, application);

		file.delete();

	}

	// static methods

	public static List<Settings> list(ServletContext servletContext, Application application) {

		// the settings we want to return - we'll check for any nulls from error when using it
		List<Settings> settings = new ArrayList<>();

		// get the directory in which the control xml files are stored
		File dir = new File(servletContext.getRealPath("") + "/WEB-INF/applications/" + application.getId() + "/" + application.getVersion());

		// create a filter for finding .settings.xml files
		FilenameFilter xmlFilenameFilter = new FilenameFilter() {
	    	@Override
			public boolean accept(File dir, String name) {
	    		return name.toLowerCase().endsWith(".settings.xml");
	    	}
	    };

	    // get any files
	    File[] files = dir.listFiles(xmlFilenameFilter);

	    // if we got some
	    if (files != null) {

		    // loop the xml files in the folder
			for (File xmlFile : files) {
				// load and add this settings
				settings.add(load(xmlFile));
			}
	    }

		return settings;

	}

	public static Settings get(ServletContext servletContext, Application application, String id) {

		// get all settings for the app
		List<Settings> settingsList = list(servletContext, application);

		// if we got some
		if (settingsList != null) {
			// loop them
			for (Settings settings : settingsList) {
				// if this is the one, return it!
				if (settings.getId().equals(id)) return settings;
			}
		}

		return null;
	}

	public static Settings load(File file) {

		// the settings we want to return - we'll check for any nulls from error when using it
		Settings settings = null;

		// init the marshaller and ununmarshaller
		try {

			JAXBContext jaxb = JAXBContext.newInstance(Settings.class);

			Unmarshaller unmarshaller = jaxb.createUnmarshaller();
			unmarshaller.setAdapter(RapidHttpServlet.getEncryptedXmlAdapter());

			// unmarshal the xml into an object
			settings = (Settings) unmarshaller.unmarshal(file);

		} catch (JAXBException ex) {
			_logger.error(ex);
		}

		return settings;

	}

}
