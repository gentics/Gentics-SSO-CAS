package com.gentics.labs.sso.cas.server.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.authentication.principal.Credentials;

/**
 * dummy implementation which does nothing. if you require password expiration handling see {@link PasswordExpiredHandlerImpl}.
 * @author herbert
 */
public class PasswordExpiredHandlerDummy implements PasswordExpiredHandler {

	@Override
	public boolean handlePasswordExpiration(Credentials credentials,
			HttpServletRequest req, HttpServletResponse res) {
		return false;
	}

}
