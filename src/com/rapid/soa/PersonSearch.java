/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.soa;

import java.sql.Date;
import java.util.ArrayList;

// this class performs the same task as the sql webservice in the demo application
public class PersonSearch implements JavaWebservice.Response {
	
	// private instance variables
	private String _forename, _surname;
	
	// properties
	public String getForename() { return _forename; }
	public void setForename(String forename) { _forename = forename; }
	
	public String getSurname() { return _surname; }
	public void setSurname(String surname) { _surname = surname; }
	
	// a pojo, made even simpler by the public instance variables
	public class Person {
		
		// public instance variables
		public String forename;
		public String surname;
		public Date birthday;
		public String gender;
		
	}

	// an intermediate collection class, could be a traditional simple class too
	public class PersonSearchResponse extends ArrayList<Person> {}

	@Override
	public PersonSearchResponse getResponse() {
		
		PersonSearchResponse response = new PersonSearchResponse();
		
		Person person1 = new Person();
		person1.forename = "Gareth";
		person1.surname = "Edwards";
		person1.birthday = new Date(1);
		person1.gender = "M";
		response.add(person1);
		
		Person person2 = new Person();
		person2.forename = "Alex";
		person2.surname = "Edwards";
		person2.birthday = new Date(2);
		person2.gender = "F";
		
		response.add(person2);
		
		return response;
		
	}

}
