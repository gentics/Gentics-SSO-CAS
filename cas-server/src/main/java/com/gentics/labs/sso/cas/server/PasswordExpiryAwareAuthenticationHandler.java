package com.gentics.labs.sso.cas.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.inspektr.common.ioc.annotation.NotNull;
import org.jasig.cas.adaptors.jdbc.AbstractJdbcUsernamePasswordAuthenticationHandler;
import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.handler.AuthenticationHandler;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.springframework.dao.EmptyResultDataAccessException;
import com.gentics.labs.sso.cas.server.web.LoginSuccessTicket;
import com.gentics.labs.sso.cas.server.web.LoginSuccessUsernameCredentials;

/**
 * Authentication handler wrapper which checks if a password is expired after
 * the wrapped authentication handler has successfully validated the
 * credentials.<BR>
 * <BR>
 * it requires a sql statement which receives the username and returns an
 * integer with either 0 = valid password, 1 = expired password.
 */
public class PasswordExpiryAwareAuthenticationHandler extends
		AbstractJdbcUsernamePasswordAuthenticationHandler {
	
	private static Logger logger = Logger.getLogger(PasswordExpiryAwareAuthenticationHandler.class.getName());
	
	@NotNull
	private AuthenticationHandler wrappedAuthenticationHandler;
	
	@NotNull
	private String sql;
	
	public AuthenticationHandler getWrappedAuthenticationHandler() {
		return wrappedAuthenticationHandler;
	}
	
	public void setWrappedAuthenticationHandler(
			AuthenticationHandler wrappedAuthenticationHandler) {
		this.wrappedAuthenticationHandler = wrappedAuthenticationHandler;
	}

	@Override
	protected boolean authenticateUsernamePasswordInternal(
			UsernamePasswordCredentials credentials) throws AuthenticationException {
		boolean loginSuccess = false;
		if (credentials instanceof LoginSuccessUsernameCredentials) {
			// well .. the caller already told us that credentials were alerady successful.
			loginSuccess = true;
		} else {
			// otherwise check the wrapped authentication handler
			loginSuccess = wrappedAuthenticationHandler.authenticate(credentials);
		}
		if (loginSuccess) {
			// if credentials could be authenticated check if password is expired.
			Integer isValid = null;
			try {
				isValid = getJdbcTemplate().queryForObject(sql, Integer.class, credentials.getUsername());
			} catch (EmptyResultDataAccessException e) {
				logger.log(Level.WARNING, "query {" + sql + "} returned empty result set - assume password is expired.", e);
			}
			if (isValid == null || isValid != 0) {
				throw new PasswordExpiredAuthenticationException("password.expired", "Password is expired. (Return Value: {" + isValid + "})");
			}
			return true;
		}
		return false;
	}
	
	public void setSql(String sql) {
		this.sql = sql;
	}
	
}
