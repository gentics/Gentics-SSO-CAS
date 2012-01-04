package com.gentics.labs.sso.cas.server.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.inspektr.common.ioc.annotation.NotNull;
import org.jasig.cas.authentication.principal.Credentials;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;

/**
 * default implementation of the password expired handler which will fetch a token from an URL (appended with the username) and then
 * redirects the user to the changePasswordUrl which will be appended by the previously obtained token. 
 */
public class PasswordExpiredHandlerImpl implements PasswordExpiredHandler {
	
	private static Logger logger = Logger.getLogger(PasswordExpiredHandlerImpl.class.getName());
	
	@NotNull
	private String tokenFetchUrl;
	
	@NotNull
	private String changePasswordUrl;

	@Override
	public boolean handlePasswordExpiration(Credentials credentials,
			HttpServletRequest req, HttpServletResponse res) {
		if (!(credentials instanceof UsernamePasswordCredentials)) {
			// unsupported ..
			return false;
		}
		String username = ((UsernamePasswordCredentials)credentials).getUsername();
		try {
			String fetchUrl = tokenFetchUrl + URLEncoder.encode(username, "utf-8");
			URL url = new URL(fetchUrl);
            InputStream stream = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            stream.close();
            if (line != null) {
            	line = line.trim();
            	if (!"".equals(line)) {
            		res.sendRedirect(changePasswordUrl + URLEncoder.encode(line, "utf-8"));
            		return true;
            	}
            }
            logger.warning("Unable to fetch token from url {" + fetchUrl + "}");
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while handling expired password.", e);
			return false;
		}
		return false;
	}
	
	/**
	 * URL from which to fetch a password-change token - URL will be appended with the username.
	 */
	public void setTokenFetchUrl(String tokenFetchUrl) {
		this.tokenFetchUrl = tokenFetchUrl;
	}
	
	/**
	 * URL to which the user will be redirected - will be appended by the token fetched from {@link #setTokenFetchUrl(String)}
	 */
	public void setChangePasswordUrl(String changePasswordUrl) {
		this.changePasswordUrl = changePasswordUrl;
	}

}
