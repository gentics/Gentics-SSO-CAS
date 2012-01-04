package com.gentics.labs.sso.cas.client.gpn;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.util.AbstractCasFilter;

/**
 * Filter for reauthentication - works together with the {@link ReauthenticationHelper}
 * @author herbert
 */
public class ReauthenticationHelperFilter implements Filter {
	
	private static String SESSION_ATTR_REAUTH = "com.gentics.labs.sso.cas.client.gpn.reauthids";

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		final String reauthId = req.getParameter(ReauthenticationHelper.REAUTH_ID_PARAMETER);
		if (!(req instanceof HttpServletRequest)) {
			throw new IllegalArgumentException("ReauthenticationHelperFilter only works for http servlet requests.");
		}
		HttpServletRequest httpRequest = ((HttpServletRequest)req);
		StringBuffer requestURL = httpRequest.getRequestURL();
		String queryString = httpRequest.getQueryString();
		if (queryString != null) {
			requestURL.append('?').append(queryString);
		}
		
		HttpSession session = httpRequest.getSession(true);
		List allowedIds = null;
		try {
			allowedIds = (List) session.getAttribute(SESSION_ATTR_REAUTH);
			ReauthenticationHelper.initForThread(allowedIds, requestURL.toString());
			// by overwriting the http request we should support the CAS
			// validation filters whether they redirect after validation or not.
			req = new HttpServletRequestWrapper(httpRequest) {
				@Override
				public void setAttribute(String name, Object o) {
					super.setAttribute(name, o);
					if (AbstractCasFilter.CONST_CAS_ASSERTION.equals(name)) {
						// user has been authenticated ..
						if (reauthId != null) {
							ReauthenticationHelper.allowToken(reauthId);
						}
					}
				}
			};
			chain.doFilter(req, res);
		} finally {
			List end = ReauthenticationHelper.finishForThread();
			session.setAttribute(SESSION_ATTR_REAUTH, end);
		}
	}

	public void init(FilterConfig cfg) throws ServletException {
	}

}
