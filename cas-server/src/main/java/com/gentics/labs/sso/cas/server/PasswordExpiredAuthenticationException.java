package com.gentics.labs.sso.cas.server;

import org.jasig.cas.authentication.handler.AuthenticationException;

/**
 * authentication exception indicating an expired password - it MUST ONLY be
 * thrown if the credentials have been successfully validated!!
 * 
 * @author herbert
 */
public class PasswordExpiredAuthenticationException extends
		AuthenticationException {
	private static final long serialVersionUID = 1L;
	private String message;

	public PasswordExpiredAuthenticationException(String code, String message) {
		super(code);
		this.message = message;
	}
	
	
	@Override
	public String getMessage() {
		return message;
	}

}
