package com.gentics.labs.sso.cas.client.gpn;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.portalnode.formatter.GenticsStringFormatter;
import com.gentics.portalnode.portal.GenticsPortletRequest;
import com.gentics.portalnode.portal.Portal;

/**
 * helper class for reauthentication. e.g. displays the password field, etc.
 * 
 * in addition it works together with the {@link ReauthenticationHelperFilter} - which is required to PUSH all allowed "reauth-tokens"
 * into this helper {@link #initForThread(List)} and at the end of the request retrieve all 
 * remaining "reauth-tokens" {@link #finishForThread()} and store them back into the session.
 * 
 * @author herbert
 */
public class ReauthenticationHelper {
	
	private static Logger logger = Logger.getLogger(ReauthenticationHelper.class.getName());
	
	private String casServerUrlPrefix;
	
	private static GenticsStringFormatter formatter = new GenticsStringFormatter();
	
	public static final String REAUTH_ID_PARAMETER = "gentics.reauthid";
	
	private static ThreadLocal reauthTokenStore = new ThreadLocal();
	
	protected ReauthenticationHelper(String casServerUrlPrefix) {
		this.casServerUrlPrefix = casServerUrlPrefix;
	}
	
	
	public static ReauthenticationHelper getInstance(String casServerUrlPrefix) {
		return new ReauthenticationHelper(casServerUrlPrefix);
	}
	
	
	public static void initForThread(List allowedIds, String requestURL) {
		if (allowedIds == null) {
			allowedIds = new ArrayList();
		}
		reauthTokenStore.set(allowedIds);
	}
	
	
	public static List finishForThread() {
		try {
			return (List) reauthTokenStore.get();
		} finally {
			reauthTokenStore.remove();
		}
	}
	
	/**
	 * checks the given reauthId token and if it is found, remove it from the store. (returns true if a token exists.)
	 * (caller must make sure, that we have been initialized.)
	 * @return true
	 */
	private static boolean checkToken(String reauthId) {
		return ((List)reauthTokenStore.get()).remove(reauthId);
	}
	
	
	/**
	 * asserts that the authentication helper was initialized by the {@link ReauthenticationHelperFilter}
	 * will throw a {@link IllegalStateException} if it was not yet initialized.
	 */
	private static void assertInitialized() {
		if (reauthTokenStore.get() == null) {
			throw new IllegalStateException(
					"ReauthenticationHelper was not initialized - have you correctly configured the ReauthenticationHelperFilter?");
		}
	}

	/**
	 * Redirects the request to the given redirectURL - or returns 'true' if the user just passed authentication.
	 * @param request resource request
	 * @param response resource response
	 * @param id authentication id
	 * @param redirectURL URL to redirect if the user did not pass authentication
	 * @return true when the user passed the authentication or false if not
	 */
	public boolean reauthenticateOrRedirect(ResourceRequest request,
			ResourceResponse response, String id, String redirectURL) {
		assertInitialized();
		if (checkToken(id)) {
			return true;
		}

		response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "307");
		response.setProperty("Location", redirectURL);
		return false;
	}
	
	/**
	 * renders the password input form into the response - or returns 'true' if the user just passed authentication.
	 * @param id the "instance id" which identifies the password reauthentication (usually the portlet entity name or portlet name.)
	 */
	public boolean reauthenticateOrShowPasswordField(PortletRequest request, RenderResponse response, String id, String name) {
		assertInitialized();
		if (checkToken(id)) {
			return true;
		}
		
		boolean showError = ObjectTransformer.getBoolean(request.getParameter("autherror"), false);

        PortletURL errorurl = response.createRenderURL();
        // we require a full url including protocol://host (this is only possible vendor specific)
        errorurl.setProperty("com.gentics.portalnode.hostabsolute", "true");
        errorurl.setParameter("autherror", "1");
        
        PortletURL successurl = response.createRenderURL();
        successurl.setProperty("com.gentics.portalnode.hostabsolute", "true");
        // this is actually just a dummy property.. we obviously need the service token to signal success :)
        successurl.setParameter("success", "1");
        
		try {
			showPasswordField(response.getWriter(), showError, successurl.toString() + "&" + REAUTH_ID_PARAMETER + "=" + URLEncoder.encode(id, "UTF-8"), errorurl.toString(), name);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error while retrieving writer from response.", e);
		}
		return false;
	}

	private void showPasswordField(PrintWriter writer, boolean showError, String serviceUrl, String errorUrl, String name) {
		// we do it inline, because it wouldn't make much sense to use templates or whatever.. just keep HTML simple so it can be easily customized
		// using CSS
		Portal portal = Portal.getCurrentPortal();
		String username = portal.getUser().getId();
		writer.print("<div class=\"gtx_cas_reauthentication\"><form method=\"post\" action=\"");
		writer.print(casServerUrlPrefix);
		writer.print("/credentialValidator?cmd=reauthentication&amp;service=");
		try {
			writer.print(URLEncoder.encode(serviceUrl, "UTF-8"));
			writer.print("&amp;on_error=");
			writer.print(URLEncoder.encode(errorUrl, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "Error while url encoding parameters for reauthentication.", e);
		}
		writer.print("\"><input type=\"hidden\" name=\"username\" value=\"");
		writer.print(formatter.escapeHTML(username));
		writer.print("\" />");
		if (showError) {
			writer.print("<span style=\"color:red\" class=\"error\">");
			writer.print(portal.i18n("Invalid password"));
			writer.print("</span>");
		}
		writer.print("<label for=\"gtx_cas_reauthpassword\">");
		I18nString enterPassword = portal.i18n("Enter Password for $name");
		enterPassword.setParameter("name", name);
		writer.print(enterPassword);
		writer.print("</label><input type=\"password\" name=\"password\" id=\"gtx_cas_reauthpassword\" /> <input type=\"submit\" value=\"");
		writer.print(portal.i18n("Submit"));
		writer.print("\" /></form></div>");
	}


	public static void allowToken(String reauthId) {
		((List)reauthTokenStore.get()).add(reauthId);
	}
}
