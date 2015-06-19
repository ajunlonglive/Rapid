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

package com.rapid.server.filter;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RapidRequestWrapper extends HttpServletRequestWrapper {
	
	public static class RapidPrincipal implements Principal {

		private String _userName = null;
		
		public RapidPrincipal(String userName) {
			_userName = userName;
		}
		
		@Override
		public String getName() {
			return _userName;
		}
		
	}

	private Principal _userPrincipal;
	
	public RapidRequestWrapper(HttpServletRequest request, String userName) {
		super(request);
		_userPrincipal = new RapidPrincipal(userName);
	}

	@Override
	public String getRemoteUser() {
		if(_userPrincipal == null) return super.getRemoteUser();
		return _userPrincipal.getName();
	}

	@Override
	public Principal getUserPrincipal() {
		if(_userPrincipal == null) return super.getUserPrincipal();
		return _userPrincipal;
	}
		
}
