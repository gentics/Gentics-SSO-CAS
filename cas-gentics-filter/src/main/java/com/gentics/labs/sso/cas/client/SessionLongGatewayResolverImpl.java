package com.gentics.labs.sso.cas.client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.authentication.GatewayResolver;

public class SessionLongGatewayResolverImpl implements GatewayResolver {
	
    public static final String CONST_CAS_GATEWAY = "_const_cas_gateway_";

	public boolean hasGatewayedAlready(HttpServletRequest request, String serviceUrl) {
		final HttpSession session = request.getSession(false);
		
		if (session == null) {
			return false;
		}
		
		final boolean result = session.getAttribute(CONST_CAS_GATEWAY) != null;
		return result;
	}

	public String storeGatewayInformation(final HttpServletRequest request,
			final String serviceUrl) {
		request.getSession(true).setAttribute(CONST_CAS_GATEWAY, "yes");
		return serviceUrl;
	}

}
