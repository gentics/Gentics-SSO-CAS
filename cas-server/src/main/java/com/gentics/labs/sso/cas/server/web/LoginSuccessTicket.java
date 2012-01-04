package com.gentics.labs.sso.cas.server.web;

import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.ticket.AbstractTicket;
import org.jasig.cas.ticket.ExpirationPolicy;

/**
 * Ticket for a successful ticket.
 * 
 * @author herbert
 */
public class LoginSuccessTicket extends AbstractTicket {
	private static final long serialVersionUID = -8400420271496031593L;
	
	private String username;

	private String rememberMeRequestAttribute;
	
	private String service;
	
	public LoginSuccessTicket() {
	}
	
	public LoginSuccessTicket(String ticketId, String username, String rememberMeRequestAttribute, String service, ExpirationPolicy expirationPolicy) {
		super(ticketId, null, expirationPolicy);
		this.username = username;
		this.rememberMeRequestAttribute = rememberMeRequestAttribute;
		this.service = service;
	}

	@Override
	public Authentication getAuthentication() {
		return null;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getRememberMeRequestAttribute() {
		return rememberMeRequestAttribute;
	}
	
	public String getService() {
		return service;
	}

}
