/*

Copyright (C) 2022 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import com.rapid.utils.Comparators;

public class Applications {

	public static class Versions extends TreeMap<String, Application> {

		private static final long serialVersionUID = 5010L;

		public Versions() {
			// note the use of the tree map with case insensitive order - this means that keys are all case insensitive, including checking them
			super(String.CASE_INSENSITIVE_ORDER);
		}

		public List<Application> sort() {
			// create the list we're going to return
			List<Application> versions = new ArrayList<Application>();
			// loop the versions
			for (String version : keySet()) {
				// add version to collection
				versions.add(get(version));
			}
			// sort the collection
			Collections.sort(versions, new VersionComparator().thenComparing(new CreatedDateComparator()));
			// return the list
			return versions;
		}
		
		private static final class CreatedDateComparator implements Comparator<Application> {
			@Override
			public int compare(Application a1, Application a2) {
				if (a1.getCreatedDate().after(a2.getCreatedDate())) {
					return 1;
				} else if (a2.getCreatedDate().after(a1.getCreatedDate())) {
					return -1;
				} else {
					return 0;
				}
			}
		}
		
		private static final class VersionComparator implements Comparator<Application> {
			@Override
			public int compare(Application v1, Application v2) {
				return compare(v1.getVersion(), v2.getVersion());
			}
			public int compare(String s1, String s2) {
				if (s1 == null) s1 = "";
				if (s2 == null) s2 = "";
				if (s1.isEmpty() && s2.isEmpty()) return 0;
				int s1PartEnd = s1.indexOf(".");
				if (s1PartEnd == -1) s1PartEnd = s1.length();
				int s2PartEnd = s2.indexOf(".");
				if (s2PartEnd == -1) s2PartEnd = s2.length();
				String s1Part = s1.substring(0, s1PartEnd);
				String s2Part = s2.substring(0, s2PartEnd);
				int comparison = 0;
				try {
					String s1LetterFree = s1Part.replaceAll("\\D", "");
					String s2LetterFree = s2Part.replaceAll("\\D", "");
					int n1 = Integer.parseInt(s1LetterFree);
					int n2 = Integer.parseInt(s2LetterFree);
					comparison = Integer.compare(n1, n2);
				} catch (NumberFormatException ex) {
					comparison = s1Part.compareToIgnoreCase(s2Part);
				}
				if (comparison == 0) {
					String s1Tail = null;
					String s2Tail = null;
					try { s1Tail = s1.substring(s1PartEnd + 1); } catch (StringIndexOutOfBoundsException ex) {}
					try { s2Tail = s2.substring(s2PartEnd + 1); } catch (StringIndexOutOfBoundsException ex) {}
					return compare(s1Tail, s2Tail);
				} else {
					return comparison;
				}
			}
		};
		
	}

	// private instance variables
	private TreeMap<String, Versions> _applications;

	// constructor
	public Applications() {
		// note the use of the tree map with case insensitive order - this means that keys are all case insensitive, including checking them
		_applications = new TreeMap<String, Versions>(String.CASE_INSENSITIVE_ORDER);
	}

	// methods

	private Versions getVersions(String id, boolean createIfNull) {
		// return null if id is null
		if (id == null) return null;
		// get the versions
		Versions versions = _applications.get(id);
		// create an entry if required
		if (createIfNull && versions == null) {
			versions = new Versions();
			_applications.put(id, versions);
		}
		// return
		return versions;
	}

	public Versions getVersions(String id) {
		// get the versions without creating an entry
		return getVersions(id, false);
	}

	public List<String> getIds() {
		// create the array we're going to return
		ArrayList<String> ids = new ArrayList<String>();
		// loop the id keys
		for (String id : _applications.keySet()) ids.add(id);
		// sort them by case insensitive id
		Collections.sort(ids, new Comparator<String>() {
			@Override
			public int compare(String id1, String id2) {
				if ("rapid".equals(id1)) return 1;
				if ("rapid".equals(id2)) return -1;
				return Comparators.AsciiCompare(id1, id2, false);
			}
		});
		return ids;
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

	// remove all application versions by id an version
	public void remove(String id, String version) {
		// get the versions of this app
		Versions versions = getVersions(id);
		// if we have some versions
		if (versions != null) {
			// remove if the version is present
			if (versions.containsKey(version)) versions.remove(version);
			// remove from applications too if all versions are gone
			if (versions.size() == 0) _applications.remove(id);
		}
	}

	// remove an application using it's own id and version
	public void remove(Application application) {
		remove(application.getId(), application.getVersion());
	}

	// fetch the highest version for an id by status
	public Application getLatestVersion(String id, int status) {
		// assume there are no applications
		Application application = null;
		// get the versions of this app
		Versions versions = getVersions(id);
		// if we got some
		if (versions != null) {
			List<Application> sortedVersions = versions.sort();
			// loop them and retain highest version
			for (Application applicationVersion : sortedVersions) {
				// if this application created date is later and the right status
				if (applicationVersion.getStatus() == status || status < 0) {
					// retain version
					application = applicationVersion;
				}
			}
		}
		return application;
	}

	// fetch the highest version for an id regardless of status
	public Application getLatestVersion(String id) {
		return getLatestVersion(id, -1);
	}

	// fetch the most recent live version, then most recent dev
	public Application get(String id) {
		// get the latest live application
		Application application = getLatestVersion(id, Application.STATUS_LIVE);
		// get the very latest if no versions are live
		if (application == null) application = getLatestVersion(id);
		// return our highest application
		return application;
	}

	// fetch an application with a known version, resorting to highest live if not version provided
	public Application get(String id, String version) {
		// return null if not app id
		if (id == null) return null;
		// get the versions of this app
		Versions versions = getVersions(id);
		// return null if we don't have any
		if (versions == null) return null;
		// check we were given a version
		if (version == null) {
			// if not return highest live version
			return get(id);
		} else {
			// return version
			return versions.get(version);
		}
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
				return Comparators.AsciiCompare(a1.getId(), a2.getId(), false);
			}

		});
		return applications;
	}

	// return whether an application exists
	public boolean exists(String id) {
		return _applications.containsKey(id);
	}

	// return whether an application and version exists
	public boolean exists(String id, String version) {
		if (_applications.containsKey(id)) {
			if (_applications.get(id).containsKey(version)) return true;
		}
		return false;
	}

	// return the number of applications
	public int size() {
		return _applications.size();
	}

}
