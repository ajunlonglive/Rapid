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

package com.rapid.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.rapid.utils.Comparators;

public class Applications {
	
	public static class Versions extends HashMap<String, Application> {
				
	}

	// private instance variables
	private HashMap<String, Versions> _applications;
	
	// constructor
	public Applications() {
		_applications = new HashMap<String, Versions>();
	}
	
	// methods
	
	private Versions getVersions(String id, boolean createIfNull) {
		Versions versions = _applications.get(id);
		if (createIfNull && versions == null) {
			versions = new Versions();
			_applications.put(id, versions);
		}
		return versions;
	}
	
	public Versions getVersions(String id) {
		return getVersions(id, false);
	}
	
	// add an application with a known version
	public void put(String id, String version, Application application) {
		// get the versions of this app
		Versions versions = getVersions(id, true);
		// put the application amongst the appVersions
		versions.put(version, application);
		// put the versions amongst the applications
		_applications.put(id, versions);
	}
	
	// add an application using it's own id and version
	public void put(Application application) {
		put(application.getId(), application.getVersion(), application);
	}
	
	// remove an application by id an version
	public void remove(String id, String version) {
		// get the versions of this app
		Versions versions = getVersions(id);
		// if we have some versions
		if (versions != null) {
			// remove if the app is present
			if (versions.containsKey(version)) versions.remove(version);
		}
	}
	
	// remove an application using it's own id and version
	public void remove(Application application) {
		remove(application.getId(), application.getVersion());
	}
	
	// fetch an application with a known version
	public Application get(String id, int version) {
		// get the versions of this app
		Versions versions = getVersions(id);
		// return null if we don't have any
		if (versions == null) return null;
		// return version
		return versions.get(version);
	}
	
	// fetch the highest version for an id by status
	public Application getLatestVersion(String id, int status) {
		// assume there are no applications
		Application application = null;
		// start with a very old date!
		Date oldestDate = new Date(1);
		// get the versions of this app
		Versions versions = getVersions(id);
		// if we got some
		if (versions != null) {
			// loop them and retain highest version
			for (String appId : versions.keySet()) {
				// get the application version
				Application applicationVersion = versions.get(appId);
				// if this application created date is later and the right status
				if (applicationVersion.getCreatedDate().after(oldestDate) && (applicationVersion.getStatus() == status || status < 0)) {
					// update oldest date
					oldestDate = application.getCreatedDate();
					// retain version
					application = applicationVersion;
				}
			}
		}
		return application;
	}
	
	// fetch the highest version for an id
	public Application getLatestVersion(String id) {
		return getLatestVersion(id, -1);
	}
	
	// fetch the highest version for an id
	public Application getEarliestVersion(String id) {
		// assume there are no applications
		Application application = null;
		// start with a very recent date!
		Date earliestDate = new GregorianCalendar(3000, 1, 1, 0, 0).getTime();
		// get the versions of this app
		Versions versions = getVersions(id);
		// if we got some
		if (versions != null) {
			// loop them and retain highest version
			for (String appId : versions.keySet()) {
				// get the application
				Application applicationVersion = versions.get(appId);
				// if this application created date is later
				if (applicationVersion.getCreatedDate().before(earliestDate)) {
					// update oldest date
					earliestDate = applicationVersion.getCreatedDate();
					// retain version
					application = applicationVersion;
				}
			}
		}
		return application;
	}
	
	// fetch the most recent live version, or first
	public Application get(String id) {
		// get the latest live application
		Application application = getLatestVersion(id, Application.STATUS_LIVE);
		// set to earliest if no versions are live
		if (application == null) application = getEarliestVersion(id);
		// return our highest application
		return application;
	}
	
	// get the highest live version of each application
	public List<Application> get() {
		// create the list we're about to sort
		ArrayList<Application> applications = new ArrayList<Application>();
		// add the highest of each
		for (String appId : _applications.keySet()) applications.add(get(appId));
		// return
		return applications;
	}
			
	public List<Application> sort() {
		// create the list we're about to sort
		List<Application> applications = get();
		// sort them
		Collections.sort(applications, new Comparator<Application>() {

			@Override
			public int compare(Application a1, Application a2) {
				if ("rapid".equals(a1.getId())) return 1;
				if ("rapid".equals(a2.getId())) return -1;
				return Comparators.AsciiCompare(a1.getId(), a2.getId());
			}
			
		});
		return applications;
	}
	
	// return whether an application exists
	public boolean exists(String id) {
		return _applications.containsKey(id);
	}
		
	// return the number of applications
	public int size() {
		return _applications.size();
	}
	
}
