package com.gentics.labs.sso.cas.client.gpn;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jasig.cas.client.authentication.AuthenticationFilter;;

public class AuthenticationFilterHelper implements Filter {

	private AuthenticationFilter casFilter;
	private String ignoreUrlPatterns = "";
	
	public AuthenticationFilterHelper() { 
		 casFilter = new AuthenticationFilter();
	}
	
	public void destroy() {

		casFilter.destroy();
		
	}

	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain filterChain) throws IOException, ServletException {
		
		
		// applying filter-exceptions
        HttpServletRequest httpRequest = ((HttpServletRequest)req);
        StringBuffer requestURL = httpRequest.getRequestURL();

		boolean ignoreFilter = false;
        
		if (ignoreUrlPatterns != null && ignoreUrlPatterns != "") {
			String[] ignoreURL = ignoreUrlPatterns.split(";");
		    for (String pattern : ignoreURL) {
		        if (requestURL.indexOf(pattern) != -1) {
					filterChain.doFilter(req, res);
					ignoreFilter = true;
					break;
		        }
		    }
	    }
	        
		
		if (!ignoreFilter) {
			casFilter.doFilter(req, res, filterChain);
		}
		

		
	}

	public void init(FilterConfig config) throws ServletException {
		
		if (config.getInitParameter("ignoreUrlPatterns") != null) {
			ignoreUrlPatterns = config.getInitParameter("ignoreUrlPatterns");
		}
		casFilter.init(config);
		
	}

	
	
}
