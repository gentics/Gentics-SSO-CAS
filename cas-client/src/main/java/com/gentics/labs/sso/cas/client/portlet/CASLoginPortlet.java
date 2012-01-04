/*
 * @author herbert
 * @date Jul 28, 2009
 * @version $Id: $
 */
package com.gentics.labs.sso.cas.client.portlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.gentics.labs.sso.cas.client.CASIntegrationFilter;

public class CASLoginPortlet extends GenericPortlet {
    
    final static Logger logger = Logger.getLogger(CASLoginPortlet.class.getName());
    
    @Override
    protected void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        PortletPreferences prefs = request.getPreferences();

        // first see if the serviceUrl is a request parameter (e.g. if the user has entered a wrong username/password)
        String serviceUrl = request.getParameter("service");
        if (serviceUrl == null) {
            // next take the service url from the filter
            serviceUrl = CASIntegrationFilter.getCurrentServiceUrl();
        }
        
        if (serviceUrl == null) {
            // if service url could not be determiend automatically, fall back to configuration.
            serviceUrl = prefs.getValue("service", null);
        }
        String serverUrlPrefix = prefs.getValue("casServerUrlPrefix", null);
        // backUrlPrefix is only used if URL cannot be generated host-absolute.
        String backUrlPrefix = prefs.getValue("backUrlPrefix", null);
        
        if (serverUrlPrefix == null) {
            logger.severe("Invalid configuration: serverUrlPrefix is null.");
            return;
        }
        if (serviceUrl == null) {
            logger.severe("Invalid configuration: service is null.");
            return;
        }
        
        if (request.getUserPrincipal() != null) {
            // we are already logged in. no need to display login form.
            PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/views/loggedin.jsp");
            
            PortletURL logoutUrl = response.createActionURL();
            logoutUrl.setParameter("proxyLogoutUrl", serverUrlPrefix + "/logout?url=" + URLEncoder.encode(serviceUrl, "UTF-8"));
            request.setAttribute("logoutUrl", logoutUrl.toString());
            dispatcher.include(request, response);
            return;
        }

        
        PortletURL errorurl = response.createRenderURL();
        // we require a full url including protocol://host (this is only possible vendor specific)
        errorurl.setProperty("com.gentics.portalnode.hostabsolute", "true");
        errorurl.setParameter("error", "true");
        errorurl.setParameter("service", serviceUrl);
        String onError = errorurl.toString();
        if (!onError.contains("://")) {
            // url was not generated absolute.
            onError = backUrlPrefix + onError;
        }
        
        String credentialValidate = serverUrlPrefix + "/credentialValidator";
        
        String lt = fetchLoginToken(credentialValidate);
        
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/views/loginform.jsp");
        
        request.setAttribute("credentialValidateUrl", credentialValidate + "?service=" + URLEncoder.encode(response.encodeURL(serviceUrl),"UTF-8"));
        request.setAttribute("loginToken", lt);
        request.setAttribute("onErrorUrl", onError);
        request.setAttribute("error", request.getParameter("error"));
        request.setAttribute("service", serviceUrl);
        
        try {
        	dispatcher.include(request, response);
        } catch (Exception e) {
            throw new PortletException("Error while dispatching to jsp.", e);
        }
    }
    
    @Override
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        String logoutUrl = request.getParameter("proxyLogoutUrl");
        if (logoutUrl != null) {
            PortletSession session = request.getPortletSession();
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(logoutUrl);
        }
    }
    
    private String fetchLoginToken(String serviceValidate) {
        try {
            URL url = new URL(serviceValidate + "?cmd=fetchlt");
            InputStream stream = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            stream.close();
            return line;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while fetching login token.", e);
        }
        return null;
    }
}
