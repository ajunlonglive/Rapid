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

// this class is referenced by both the database and webservice action classes
public class Parameter {
			
	private String _itemId, _field;
	
	public String getItemId() { return _itemId; }
	public void setItemId(String itemId) { _itemId = itemId; }
	
	public String getField() { return _field; }
	public void setField(String field) { _field = field; }
	
	public Parameter() {};
	public Parameter(String itemId, String field) {
		_itemId = itemId;
		_field = field;
	}
	
}
