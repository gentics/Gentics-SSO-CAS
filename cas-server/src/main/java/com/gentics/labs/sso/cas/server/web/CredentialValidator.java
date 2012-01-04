/*
 * @author herbert
 * @date Jul 28, 2009
 * @version $Id: $
 */
package com.gentics.labs.sso.cas.server.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.inspektr.common.ioc.annotation.NotEmpty;
import org.inspektr.common.ioc.annotation.NotNull;
import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.principal.RememberMeCredentials;
import org.jasig.cas.authentication.principal.RememberMeUsernamePasswordCredentials;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.jasig.cas.authentication.principal.WebApplicationService;
import org.jasig.cas.ticket.ExpirationPolicy;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketException;
import org.jasig.cas.ticket.registry.TicketRegistry;
import org.jasig.cas.ticket.support.TimeoutExpirationPolicy;
import org.jasig.cas.util.UniqueTicketIdGenerator;
import org.jasig.cas.web.support.ArgumentExtractor;
import org.jasig.cas.web.support.CookieRetrievingCookieGenerator;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.gentics.labs.sso.cas.server.PasswordExpiredAuthenticationException;


/**
 * Controller which generates login tickets through a RESTful API and
 * validates username/password This is required if you want your own login form
 * embedded in your website/portal/etc instead of using the CAS login form.
 * 
 * @author herbert
 */
public class CredentialValidator extends AbstractController {
    
    private final Log log = LogFactory.getLog(CredentialValidator.class);
    
    private static final String LOGINTICKET_PREFIX = "LT";
    
    private static final String LOGINTICKET_SUCCESS_PREFIX = "LTS";
    
    @NotNull
    private TicketRegistry loginTicketRegistry;
    
    /**
     * @see #setLoginSuccessTicketCookieGenerator(CookieRetrievingCookieGenerator)
     */
    @NotNull
    private TicketRegistry loginSuccessTicketRegistry;
    
    @NotNull
    private UniqueTicketIdGenerator loginTicketUniqueIdGenerator;
    
    @NotNull
    private CentralAuthenticationService centralAuthenticationService;
    
    @NotNull
    private ExpirationPolicy loginTicketExpirationPolicy = new TimeoutExpirationPolicy(6 * 60 * 60 * 1000);
    
    @NotNull
    private ExpirationPolicy loginSuccessTicketExpirationPolicy = new TimeoutExpirationPolicy(6 * 60 * 60 * 1000);
    
    @NotNull
    private CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;
    
    private CookieRetrievingCookieGenerator loginSuccessTicketCookieGenerator;
    
    private PasswordExpiredHandler passwordExpiredHandler;
    
    /** Extractors for finding the service. */
    @NotEmpty
    private List<ArgumentExtractor> argumentExtractors;

    
    public CredentialValidator() {
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest req,
            HttpServletResponse res) throws Exception {
        String cmd = req.getParameter("cmd");
        String onError = req.getParameter("on_error");
        if ("fetchlt".equals(cmd)) {
            // Requestor wants to generate a login ticket. return one.
            String ticketId = loginTicketUniqueIdGenerator.getNewTicketId(LOGINTICKET_PREFIX);
            LoginTicket loginTicket = new LoginTicket(ticketId, loginTicketExpirationPolicy);
            loginTicketRegistry.addTicket(loginTicket);
            return new ModelAndView("credentialValidatorFetchLT", "loginTicket", loginTicket.getId());
        } else if ("passwordChanged".equals(cmd)) {
        	// password was changed, user already successfully authenticated, just create TGT and ST
    		String ticketId = loginSuccessTicketCookieGenerator.retrieveCookieValue(req);
    		final LoginSuccessTicket successTicket = (LoginSuccessTicket) loginSuccessTicketRegistry.getTicket(ticketId, LoginSuccessTicket.class);
    		if (successTicket == null || successTicket.isExpired()) {
    			logger.debug("Invalid ticket after password change.");
    			res.sendRedirect(onError);
    			return null;
    		}
    		RememberMeUsernamePasswordCredentials c = new LoginSuccessUsernameCredentials();
    		c.setUsername(successTicket.getUsername());
    		c.setRememberMe(StringUtils.hasText(successTicket.getRememberMeRequestAttribute()));
    		
    		// we need to wrap the request to provide the right 'rememberMe' parameter
    		HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(req) {
    			@Override
    			public String getParameter(String name) {
    				if (RememberMeCredentials.REQUEST_PARAMETER_REMEMBER_ME.equals(name)) {
    					return successTicket.getRememberMeRequestAttribute();
    				} else if ("service".equals(name)) {
    					return successTicket.getService();
    				}
    				return super.getParameter(name);
    			}
    		};
    		generateTicketGeneratingTicket(wrappedRequest, res, onError, c);
        } else if ("reauthentication".equals(cmd)) {
        	// we only want re authentcation .. ie. a ticket granting ticket must already be available.
        	
        	// construct the credentials
        	final RememberMeUsernamePasswordCredentials c = new RememberMeUsernamePasswordCredentials();
            c.setUsername(req.getParameter("username"));
            c.setPassword(req.getParameter("password"));
            c.setRememberMe(StringUtils.hasText(req.getParameter(RememberMeCredentials.REQUEST_PARAMETER_REMEMBER_ME)));
            
            // retrieve the TGT from the request
            String tgt = ticketGrantingTicketCookieGenerator.retrieveCookieValue(req);
            if (tgt == null) {
            	logger.warn("Got login request from user who is not logged in (ie. no TGT)");
            	res.sendRedirect(onError);
            	return null;
            }
            WebApplicationService service = getServiceFromRequest(req, res, onError);
            String serviceTicket;
            try {
            	// validate credentials and generate service ticket ..
            	serviceTicket = centralAuthenticationService.grantServiceTicket(tgt, service, c);
            } catch (TicketException e) {
            	res.sendRedirect(onError);
            	return null;
            }
            res.sendRedirect(service.getResponse(serviceTicket).getUrl());
        } else {
            // this was a validation request.
            String lt = req.getParameter("lt");
            if (onError == null) {
                throw new IllegalArgumentException("View requires an on_error parameter.");
            }
            try {
                Ticket ticket = loginTicketRegistry.getTicket(lt, LoginTicket.class);
                if (ticket == null || ticket.isExpired()) {
                    logger.debug("Invalid login ticket {" + lt + "} or ticket is expired. {" + ticket + "}");
                    // ticket is already expired, or invalid. send user to the error URL.
                    res.sendRedirect(onError);
                    return null;
                }
                // ticket was validated. now validate username/password ...
                final RememberMeUsernamePasswordCredentials c = new RememberMeUsernamePasswordCredentials();
                c.setUsername(req.getParameter("username"));
                c.setPassword(req.getParameter("password"));
                c.setRememberMe(StringUtils.hasText(req.getParameter(RememberMeCredentials.REQUEST_PARAMETER_REMEMBER_ME)));
                generateTicketGeneratingTicket(req, res, onError, c);
            } finally {
                loginTicketRegistry.deleteTicket(lt);
            }
        }
        return null;
    }

	private void generateTicketGeneratingTicket(HttpServletRequest req,
			HttpServletResponse res, String onError,
			final RememberMeUsernamePasswordCredentials credentials) throws IOException,
			TicketException {
		String tgt;
		try {
		    tgt = centralAuthenticationService.createTicketGrantingTicket(credentials);
		} catch (TicketException e) {
			if (e.getCause() instanceof PasswordExpiredAuthenticationException
					&& passwordExpiredHandler != null) {
				// password is expired ... but credentials are correct.
				logger.debug("Password for user {" + credentials.getUsername() + "} was expired.");
				String ticketId = loginTicketUniqueIdGenerator.getNewTicketId(LOGINTICKET_SUCCESS_PREFIX);
				loginSuccessTicketRegistry.addTicket(new LoginSuccessTicket(ticketId, credentials.getUsername(), req.getParameter(RememberMeCredentials.REQUEST_PARAMETER_REMEMBER_ME), req.getParameter("service"), loginSuccessTicketExpirationPolicy));
				loginSuccessTicketCookieGenerator.addCookie(res, ticketId);
				if (passwordExpiredHandler.handlePasswordExpiration(credentials, req, res)) {
					return;
				}
			}
		    // invalid username/password - send to error page.
		    log.debug("Unable to create ticket granting ticket - invalid credentials?", e);
		    res.sendRedirect(onError);
		    return;
		}
		ticketGrantingTicketCookieGenerator.addCookie(req, res, tgt);
		
		// Ticket Granting Ticket was added. Now we need to create a service ticket.
		WebApplicationService service = getServiceFromRequest(req, res,
				onError);
		String st = centralAuthenticationService.grantServiceTicket(tgt, service);
		// successful login. redirect to service.
		res.sendRedirect(service.getResponse(st).getUrl());
	}

	private WebApplicationService getServiceFromRequest(
			HttpServletRequest req, HttpServletResponse res, String onError)
			throws IOException {
		WebApplicationService service = null;
		for (ArgumentExtractor argumentExtractor : argumentExtractors) {
		    service = argumentExtractor.extractService(req);
		    if (service != null) {
		        break;
		    }
		}
		if (service == null) {
		    log.error("Unable to retrieve service from request.");
		    res.sendRedirect(onError);
		}
		return service;
	}
    
	/**
	 * defines a password expired handler which will be used when a user has
	 * successfully entered his credentials, but password was detected to be
	 * expired.
	 */
    public void setPasswordExpiredHandler(
			PasswordExpiredHandler passwordExpiredHandler) {
		this.passwordExpiredHandler = passwordExpiredHandler;
	}

    public TicketRegistry getLoginTicketRegistry() {
        return loginTicketRegistry;
    }

    public void setLoginTicketRegistry(TicketRegistry loginTicketRegistry) {
        this.loginTicketRegistry = loginTicketRegistry;
    }

    public CentralAuthenticationService getCentralAuthenticationService() {
        return centralAuthenticationService;
    }

    public void setCentralAuthenticationService(
            CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }
    
    public void setTicketGrantingTicketCookieGenerator(
            CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator) {
        this.ticketGrantingTicketCookieGenerator = ticketGrantingTicketCookieGenerator;
    }

    public void setLoginTicketExpirationPolicy(ExpirationPolicy loginTicketExpirationPolicy) {
        this.loginTicketExpirationPolicy = loginTicketExpirationPolicy;
    }
    
    public void setArgumentExtractors(List<ArgumentExtractor> argumentExtractors) {
        this.argumentExtractors = argumentExtractors;
    }
    
    /**
	 * unique id generator for login tickets. will also be used to generate
	 * unique "login success" tickets if password is expired.
	 */
    public void setLoginTicketUniqueIdGenerator(
            UniqueTicketIdGenerator loginTicketUniqueIdGenerator) {
        this.loginTicketUniqueIdGenerator = loginTicketUniqueIdGenerator;
    }
    
    public void setLoginSuccessTicketRegistry(
			TicketRegistry loginSuccessTicketRegistry) {
		this.loginSuccessTicketRegistry = loginSuccessTicketRegistry;
	}
    
    public void setLoginSuccessTicketExpirationPolicy(
			ExpirationPolicy loginSuccessTicketExpirationPolicy) {
		this.loginSuccessTicketExpirationPolicy = loginSuccessTicketExpirationPolicy;
	}
    
    /**
     * registry which stores tokens for successful logins which had expired passwords.
     */
    public void setLoginSuccessTicketCookieGenerator(
			CookieRetrievingCookieGenerator loginSuccessTicketCookieGenerator) {
		this.loginSuccessTicketCookieGenerator = loginSuccessTicketCookieGenerator;
	}
}
