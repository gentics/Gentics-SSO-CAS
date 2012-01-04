package com.gentics.labs.sso.cas.client.gpn;

import java.io.IOException;

import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.portalnode.genericmodules.GenticsContentModule;

/**
 * Simple subclass of GenticsContentModule to handle reauthentication against a CAS server for special security needs.
 * 
 * @author herbert
 */
public class ContentModuleWithReauthentication extends GenticsContentModule {
	private final static String SHOWPASSWORDFIELD_CMD = "showpasswordfield";

	private final static String REAUTH_URL_PARAM = "reauth_url";

	private final static String REAUTH_NAME_PARAM = "reauth_name";

	private static NodeLogger logger = NodeLogger.getNodeLogger(ContentModuleWithReauthentication.class.getName());

	public ContentModuleWithReauthentication(String moduleId) throws PortletException {
		super(moduleId);
	}

	@Override
	protected void doView(RenderRequest req, RenderResponse res) throws PortletException, IOException {
		PortletSession session = req.getPortletSession();
		// check whether we were redirected from the method serveResource to show the password field
		if (SHOWPASSWORDFIELD_CMD.equals(req.getParameter("cmd"))) {
			String casServerUrlPrefix = getGenticsPortletContext().getStringModuleParameter("casServerUrlPrefix");
			// put the redirect url into the portlet session
			session.setAttribute(REAUTH_URL_PARAM, req.getParameter(REAUTH_URL_PARAM), PortletSession.PORTLET_SCOPE);
			session.setAttribute(REAUTH_NAME_PARAM, req.getParameter(REAUTH_NAME_PARAM), PortletSession.PORTLET_SCOPE);

			if (logger.isDebugEnabled()) {
				logger.debug("Showing password field to reauthenticate for a resource");
			}

			ReauthenticationHelper.getInstance(casServerUrlPrefix).reauthenticateOrShowPasswordField(req, res,
					getGenticsPortletContext().getModuleId() + "_r", req.getParameter(REAUTH_NAME_PARAM));
		} else {
			// when a redirect url is in the session, we fetch it, redirect to it and remove it from the session
			String redirectUrl = ObjectTransformer.getString(session.getAttribute(REAUTH_URL_PARAM, PortletSession.PORTLET_SCOPE), null);
			String redirectName = ObjectTransformer.getString(session.getAttribute(REAUTH_NAME_PARAM, PortletSession.PORTLET_SCOPE), null);
			if (redirectUrl != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Redirecting to the resource (should be authenticated now)");
				}
				session.removeAttribute(REAUTH_URL_PARAM, PortletSession.PORTLET_SCOPE);

				PortletURL renderURL = res.createRenderURL();
				// show the download link
				res.getWriter().println(
						"Download " + redirectName + " <a href=\"" + StringUtils.encodeWithEntities(redirectUrl) + "\">here</a><br/>Go <a href=\""
								+ renderURL + "\">back</a>");
				return;
			}

			if (ObjectTransformer.getBoolean(getProperty(getReauthAttributeName()), false)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Accessed content (" + getProperty("contentid") + ") requires reauthentication");
				}
				// this content has to be extra secured.
				String casServerUrlPrefix = getGenticsPortletContext().getStringModuleParameter("casServerUrlPrefix");
				if (!ReauthenticationHelper.getInstance(casServerUrlPrefix).reauthenticateOrShowPasswordField(req, res,
						getGenticsPortletContext().getModuleId() + "_v", ObjectTransformer.getString(getProperty("name"), null))) {
					if (logger.isDebugEnabled()) {
						logger.debug("Showing password field to reauthenticate for content {" + getProperty("contentid") + "}");
					}
					return;
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("User successfully reauthenticated for content {" + getProperty("contentid") + "}");
					}
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Rendering content {" + getProperty("contentid") + "} now");
			}
			super.doView(req, res);
		}
	}

	@Override
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
		try {
			GenticsContentObject obj = getObjectFromRequest(request);
			if (obj != null && ObjectTransformer.getBoolean(obj.get(getReauthAttributeName()), false)) {
				String resourceName = ObjectTransformer.getString(obj.get("name"), null);
				if (logger.isDebugEnabled()) {
					logger.debug("Accessed resource " + obj.getContentId() + (resourceName != null ? "(" + resourceName + ")" : "")
							+ " requires reauthentication");
				}
				// this content has to be extra secured - redirect user to a login form ..
				PortletURL renderUrl = response.createRenderURL();
				renderUrl.setParameter("cmd", SHOWPASSWORDFIELD_CMD);

				// pass the original resourceURL to the request
				ResourceURL thisResourceURL = response.createResourceURL();
				thisResourceURL.setResourceID(request.getResourceID());
				thisResourceURL.setParameters(request.getParameterMap());
				thisResourceURL.setParameter("ticket", (String) null);
				thisResourceURL.setProperty("com.gentics.portalnode.hostabsolute", "true");

				renderUrl.setParameter(REAUTH_URL_PARAM, thisResourceURL.toString());
				renderUrl.setParameter(REAUTH_NAME_PARAM, resourceName);
				renderUrl.setProperty("com.gentics.portalnode.hostabsolute", "true");

				String casServerUrlPrefix = getGenticsPortletContext().getStringModuleParameter("casServerUrlPrefix");
				if (ReauthenticationHelper.getInstance(casServerUrlPrefix).reauthenticateOrRedirect(request, response,
						getGenticsPortletContext().getModuleId() + "_r", renderUrl.toString())) {
					if (logger.isDebugEnabled()) {
						logger.debug("User successfully reauthenticated for resource {" + obj.getContentId() + "}");
					}
					writeResourceIntoResponse(request, response, obj);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Request will be redirected to the password form");
					}
				}
			} else {
				writeResourceIntoResponse(request, response, obj);
			}
		} catch (CMSUnavailableException e) {
			logger.error("Error while serving resource", e);
		} catch (NodeIllegalArgumentException e) {
			logger.error("Error while serving resource", e);
		} catch (InsufficientPrivilegesException e) {
			handleInsufficentPermissionsForServeResource(response);
		}
	}

	/**
	 * Get the name of the attribute that marks the objects which require reauthentication every time they are shown
	 * 
	 * @return name of the requirereauthentication attribute
	 */
	protected String getReauthAttributeName() {
		return ObjectTransformer.getString(getGenticsPortletContext().getStringModuleParameter("reauthentication_attribute_name"),
				"requirereauthentication");
	}
}
