package com.gentics.labs.sso.cas.client.gpn;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.log.NodeLogger;
import com.gentics.portalnode.portal.PortalWrapper;

/**
 * this has to be the first servlet filter - before AuthenticationFilter of CAS
 * 
 * 
 * 	  <filter>
	  	<filter-name>Password Change Token Generator</filter-name>
	  	<filter-class>com.gentics.labs.sso.cas.client.gpn.PasswordChangeTokenFilter</filter-class>
	  	<init-param>
	  		<param-name>secret</param-name>
	  		<param-value>verySecret</param-value>
	  	</init-param>
	  	<init-param>
	  		<param-name>datasource</param-name>
	  		<param-value>ers</param-value>
	  	</init-param>
	  	<init-param>
	  		<param-name>token_obj_type</param-name>
	  		<param-value>30000</param-value>
	  	</init-param>
	  </filter>
	  
	  <filter-mapping>
	  	<filter-name>Password Change Token Generator</filter-name>
	  	<url-pattern>/*</url-pattern>
	  </filter-mapping>
 * 
 * @author herbert
 */
public class PasswordChangeTokenFilter implements Filter {
	private static NodeLogger logger = NodeLogger.getNodeLogger(PasswordChangeTokenFilter.class);
	
	private static final String REQUEST_PARAMETER = "GENTICS_FETCHPWTOKEN";
	private String checkSecret = null;
	private String datasourceName;
	private Integer tokenObjType;
	

	private Random randomGenerator = new Random();

	public void init(FilterConfig config) throws ServletException {
		checkSecret = config.getInitParameter("secret");
		datasourceName = config.getInitParameter("datasource");
		tokenObjType = ObjectTransformer.getInteger(config.getInitParameter("token_obj_type"), null);
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		if (req.getParameter(REQUEST_PARAMETER) != null) {
			WriteableDatasource ds = (WriteableDatasource) PortalWrapper.getInstance().createDatasource(datasourceName);
			String username = req.getParameter("username");
			String secret = req.getParameter("secret");
			if (checkSecret == null || !checkSecret.equals(secret)) {
				logger.error("invalid secret provided in URL.");
				res.getWriter().println("Invalid secret provided in request url.");
				return;
			}
			if (ObjectTransformer.isEmpty(username)) {
				logger.error("No username given");
				res.getWriter().println("No username given in request url.");
				return;
			}
			
			String token = Long.toHexString(randomGenerator.nextLong());
			Map tokenStore = new HashMap();
			tokenStore.put("token", token);
			tokenStore.put("username", username);
			tokenStore.put("obj_type", tokenObjType);
			try {
				Changeable created = ds.create(tokenStore);
				ds.store(Collections.singleton(created));
				res.getWriter().print(token);
				return;
			} catch (DatasourceException e) {
				logger.error("Error while storing token in the datasource.", e);
				return;
			}

		}
		chain.doFilter(req, res);
	}

}
