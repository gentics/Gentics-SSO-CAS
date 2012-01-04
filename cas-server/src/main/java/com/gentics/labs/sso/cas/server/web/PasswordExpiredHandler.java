package com.gentics.labs.sso.cas.server.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.authentication.principal.Credentials;

/**
 * interface which is responsible for handling validation requests which were
 * unsuccessful because the password was expired.
 * 
 * @author herbert
 */
public interface PasswordExpiredHandler {
	/**
	 * should handle the expiration of a password. if for any reason it is not possible to handle it, it should return false,
	 * otherwise true (usually this will be a redirection to a 'password change' dialog.)
	 * 
	 * @param credentials which where used to log in - it is guaranteed that they are valid.
	 */
	public boolean handlePasswordExpiration(Credentials credentials, HttpServletRequest req, HttpServletResponse res);
}
