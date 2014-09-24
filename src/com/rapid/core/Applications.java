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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.rapid.utils.Comparators;

public class Applications {

	// private instance variables
	private HashMap<String, HashMap<Integer, Application>> _applications;
	
	// constructor
	public Applications() {
		_applications = new HashMap<String, HashMap<Integer, Application>>();
	}
	
	// methods
	
	// add an application with a known version
	public void put(String id, int version, Application application) {
		// get the versions of this app
		HashMap<Integer, Application> appVersions = _applications.get(id);
		// instantiate if need be
		if (appVersions == null) appVersions = new HashMap<Integer, Application>();
		// put the application amongst the appVersions
		appVersions.put(version, application);
		// put the versions amongst the applications
		_applications.put(id, appVersions);
	}
	
	// add an application using it's own id and version
	public void put(Application application) {
		put(application.getId(), application.getVersion(), application);
	}
	
	// remove an application by id an version
	public void remove(String id, int version) {
		// get the versions of this app
		HashMap<Integer, Application> appVersions = _applications.get(id);
		// if we have some
		if (appVersions != null) appVersions.remove(version);
	}
	
	// remove an application using it's own id and version
	public void remove(Application application) {
		remove(application.getId(), application.getVersion());
	}
	
	// fetch an application with a known version
	public Application get(String id, int version) {
		// get the versions of this app
		HashMap<Integer, Application> appVersions = _applications.get(id);
		// return null if we don't have any
		if (appVersions == null) return null;
		// return version
		return appVersions.get(id);
	}
	
	// fetch the highest version for an id
	public int getHighestVersion(String id) {
		int version = 0;
		// get the versions of this app
		HashMap<Integer, Application> appVersions = _applications.get(id);
		// if we got some
		if (appVersions != null) {
			// loop them and retain highest version
			for (int v : appVersions.keySet()) if (v > version) version = v;
		}
		return version;
	}
	
	// fetch the highest live version, or first
	public Application get(String id) {
		// get the versions of this app
		HashMap<Integer, Application> appVersions = _applications.get(id);
		// return null if we don't have any
		if (appVersions == null) return null;
		// assume version 1
		Application application = appVersions.get(1);
		// loop the keys
		for (int version : appVersions.keySet()) {
			// fetch this version
			Application versionApplication = appVersions.get(version);
			// assign if higher and live
			if (versionApplication.getVersion() > application.getVersion() && versionApplication.getStatus() == Application.STATUS_LIVE) application = versionApplication;
		}
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
				if ("rapid".equals(a1.getId())) return -1;
				if ("rapid".equals(a2.getId())) return 1;
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
