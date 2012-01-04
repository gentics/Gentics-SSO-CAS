package com.gentics.labs.sso.cas.client.gpn;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.NoSuchElementException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.portlet.AbstractGenticsPortlet;
import com.gentics.lib.etc.StringUtils;

/**
 * Example implementation for a portlet providing a "password change" dialog.
 * 
 * Example Configuration:
 * <pre><![CDATA[
	<pnode type="cas-client-gpn/PasswordChangePortlet" id="PasswordChangePortlet">
		<parameters>
			<parameter name="datasource">ers</parameter>
			<parameter name="secret">hehe</parameter>
			<parameter name="token_obj_type">30000</parameter>
			
			<parameter name="user_obj_type">50000</parameter>
			<parameter name="user_name_attribute">login</parameter>
			<parameter name="user_password_attribute">pwd</parameter>
			<parameter name="user_passwordchange_attribute">passwordChangeDate</parameter>
			
			<parameter name="post_password_changed_url">http://localhost:42880/cas-server/credentialValidator?cmd=passwordChanged</parameter>
		</parameters>
	</pnode>
 ]]></pre>
 * 
 * @author herbert
 */
public class PasswordChangePortlet extends AbstractGenticsPortlet {

	private Expression findTokenExpression;
	
	private Expression findUserExpression;

	public PasswordChangePortlet(String moduleId) throws PortletException {
		super(moduleId);
	}
	
	@Override
	public void init() throws PortletException {
		super.init();
		
		ExpressionParser parser = ExpressionParser.getInstance();
		try {
			findTokenExpression = parser.parse("object.obj_type == data.token_obj_type && object.token == data.token");
			String nameAttr = getGenticsPortletContext().getStringModuleParameter("user_name_attribute");
			findUserExpression = parser.parse("object.obj_type == data.user_obj_type && object." + nameAttr + " == data.username");
		} catch (ParserException e) {
			throw new PortletException("Error while parsing expression.", e);
		}
	}
	
	protected Integer getTokenObjType() {
		return ObjectTransformer.getInteger(getGenticsPortletContext().getStringModuleParameter("token_obj_type"), null);
	}
	
	@Override
	protected void doView(RenderRequest request, RenderResponse response)
			throws PortletException, IOException {
		final String token = request.getParameter("token");
		try {
			String username = validateToken(token);
			
			
			// dispatch to form.
	        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/views/passwordchangeform.jsp");
	        
	        request.setAttribute("username", username);
	        PortletURL actionurl = response.createActionURL();
	        actionurl.setParameter("cmd", "changepassword");
	        actionurl.setParameter("username", username);
	        actionurl.setParameter("token", token);
	        request.setAttribute("error", request.getParameter("error"));
	        request.setAttribute("actionurl", actionurl);
	        try {
	        	dispatcher.include(request, response);
	        } catch (Exception e) {
	            throw new PortletException("Error while dispatching to jsp.", e);
	        }

		} catch (NoSuchElementException e) {
			logger.error("User passed in an invalid token. {" + token + "}");
			response.getWriter().print("Invalid token provided.");
		} catch (Exception e) {
			throw new PortletException("error while looking for token.", e);
		}
	}

	private String validateToken(final String token)
			throws PortletException, ExpressionParserException,
			DatasourceException {
		Datasource ds = getGenticsPortletContext().getDatasource();
		Resolvable r = getResolvableForToken(token, ds);
		String username = (String) r.get("username");
		return username;
	}

	private Resolvable getResolvableForToken(final String token, Datasource ds)
			throws PortletException, ExpressionParserException,
			DatasourceException {
		if (token == null) {
			throw new PortletException("No token given in request.");
		}
		DatasourceFilter findToken = ds.createDatasourceFilter(findTokenExpression);
		findToken.addBaseResolvable("data", new Resolvable() {
			public Object getProperty(String key) {
				return get(key);
			}
			public Object get(String key) {
				if ("token_obj_type".equals(key)) {
					return getTokenObjType();
				} else if ("token".equals(key)) {
					return token;
				}
				return null;
			}
			public boolean canResolve() {
				return true;
			}
		});
		Collection res = ds.getResult(findToken, new String[] { "username" });
		Resolvable r = (Resolvable) res.iterator().next();
		return r;
	}
	
	@Override
	public void processAction(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {
		if (!"changepassword".equals(request.getParameter("cmd"))) {
			logger.error("invalid cmd given in request.");
			return;
		}
		WriteableDatasource ds = (WriteableDatasource) getGenticsPortletContext().getDatasource();
		String token = request.getParameter("token");
		try {
			Resolvable r = getResolvableForToken(token, ds);
			final String username = (String) r.get("username");
			// find user with the given name
			DatasourceFilter filter = ds.createDatasourceFilter(findUserExpression);
			filter.addBaseResolvable("data", new Resolvable() {
				public Object getProperty(String key) {
					return get(key);
				}
				public Object get(String key) {
					if ("user_obj_type".equals(key)) {
						return ObjectTransformer.getInteger(getGenticsPortletContext().getStringModuleParameter("user_obj_type"), null);
					} else if ("username".equals(key)) {
						return username;
					}
					return null;
				}
				public boolean canResolve() {
					return true;
				}
			});
			Collection res = ds.getResult(filter, null);
			try {
				String passwordChangeAttribute = getGenticsPortletContext().getStringModuleParameter("user_passwordchange_attribute");
				String passwordAttribute = getGenticsPortletContext().getStringModuleParameter("user_password_attribute");
				Changeable user = (Changeable) res.iterator().next();
				// todo .. encryption ?
				String password = request.getParameter("newpassword");
				String repassword = request.getParameter("repassword");
				if (repassword == null || !repassword.equals(password)) {
					response.setRenderParameter("token", token);
					response.setRenderParameter("error", "true");
					return;
				}
				
				user.setProperty(passwordAttribute, StringUtils.md5(password));
				user.setProperty(passwordChangeAttribute, new Date());
				ds.update(Collections.singleton(user));
				
				response.sendRedirect(getGenticsPortletContext().getStringModuleParameter("post_password_changed_url"));
			} catch (NoSuchElementException e) {
				throw new PortletException("Unable to find user with name {" + username + "}");
			}
		} catch (Exception e) {
			throw new PortletException("Error while storing changed password.", e);
		}
	}

}
