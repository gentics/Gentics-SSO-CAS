				USAGE OF CAS SINGLE SIGN ON
					for Gentics Content.Node and Gentics Portal.Node


1.) create a customized 'cas-server' web application:

in the config/ subdirectory create a new directory - this will be copied over the .war file.
The only thing you should be required to customize is WEB-INF/deployerConfigContext.xml



	<filter>
		<filter-name>CAS Single Sign Out Filter</filter-name>
		<filter-class>
			org.jasig.cas.client.session.SingleSignOutFilter
		</filter-class>
	</filter>

	<filter>
		<filter-name>CASAuthFilter</filter-name>
		<filter-class>
			org.jasig.cas.client.authentication.AuthenticationFilter
		</filter-class>
		<init-param>
			<param-name>casServerLoginUrl</param-name>
			<param-value>
				https://herbert.office:8443/cas-server-webapp-3.3.3/login
			</param-value>
		</init-param>
		<init-param>
			<param-name>renew</param-name>
			<param-value>false</param-value>
		</init-param>
		<init-param>
			<param-name>gateway</param-name>
			<param-value>true</param-value>
		</init-param>
		<init-param>
			<param-name>serverName</param-name>
			<!-- param-value>http://10.43.94.249:42880</param-value -->
			<param-value>http://10.43.93.252:8080</param-value>
		</init-param>
	</filter>

	<filter>
		<filter-name>CASValidationFilter</filter-name>
		<filter-class>
			org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter
		</filter-class>
		<init-param>
			<param-name>casServerUrlPrefix</param-name>
			<param-value>
				https://herbert.office:8443/cas-server-webapp-3.3.3
			</param-value>
		</init-param>
		<init-param>
			<param-name>serverName</param-name>
			<!-- param-value>http://10.43.94.249:42880</param-value -->
			<param-value>http://10.43.93.252:8080</param-value>
		</init-param>
	</filter>

	<filter>
		<filter-name>CAS HttpServletRequest Wrapper Filter</filter-name>
		<filter-class>
			org.jasig.cas.client.util.HttpServletRequestWrapperFilter
		</filter-class>
	</filter>

	<filter>
		<filter-name>PN CAS Integration Filter</filter-name>
		<filter-class>
			com.gentics.portals.gep.auth.cas.CASIntegrationFilter
		</filter-class>
	</filter>

	<filter-mapping>
		<filter-name>CAS Single Sign Out Filter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

	<filter-mapping>
		<filter-name>CASAuthFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

	<filter-mapping>
		<filter-name>CASValidationFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

	<filter-mapping>
		<filter-name>CAS HttpServletRequest Wrapper Filter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

	<filter-mapping>
		<filter-name>PN CAS Integration Filter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
