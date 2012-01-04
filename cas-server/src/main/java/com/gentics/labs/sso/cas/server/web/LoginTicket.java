/*
 * @author herbert
 * @date Jul 28, 2009
 * @version $Id: $
 */
package com.gentics.labs.sso.cas.server.web;

import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.ticket.AbstractTicket;
import org.jasig.cas.ticket.ExpirationPolicy;

public class LoginTicket extends AbstractTicket {

    private static final long serialVersionUID = 4754203541724337454L;
    
    public LoginTicket() {
    }
    
    public LoginTicket(String id, ExpirationPolicy expirationPolicy) {
        super(id, null, expirationPolicy);
    }

    @Override
    public Authentication getAuthentication() {
        return null;
    }

}
