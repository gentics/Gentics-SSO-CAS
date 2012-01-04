/*
 * @author herbert
 * @date Jul 28, 2009
 * @version $Id: $
 */
package com.gentics.labs.sso.cas.server;

import org.jasig.cas.authentication.handler.DefaultPasswordEncoder;
import org.jasig.cas.authentication.handler.PasswordEncoder;

/**
 * The Password Component and similar methods of storing an md5 encoded password uses upper case 
 * hex characters. Unfortunately the {@link DefaultPasswordEncoder} only uses lower case letters.
 * This is a simple class to wrap it and output upper case hex characters.
 */
public class UpperCaseMD5PasswordEncoder implements PasswordEncoder {
    
    DefaultPasswordEncoder wrappedPasswordEncoder = new DefaultPasswordEncoder("MD5");

    @Override
    public String encode(String password) {
        return wrappedPasswordEncoder.encode(password).toUpperCase();
    }

}
