
package com.rapid.server.filter;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletInputStream;
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
