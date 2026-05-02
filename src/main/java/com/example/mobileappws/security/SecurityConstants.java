package com.example.mobileappws.security;


import com.example.mobileappws.SpringApplicationContext;
import org.springframework.core.env.Environment;

public class SecurityConstants {

    // token expiration time, # of milliseconds
    public static final long EXPIRATION_TIME = 864000000; // 10 days
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String SIGN_UP_URL = "/users";
    public static final String VERIFICATION_EMAIL_URL = "/users/email-verification";

    // this is at least 64 char, unique to my application
//    public static final String TOKEN_SECRET = "aB3kP9mXqL2nRvYwZjCdEuFgHiOsStUbNpQrTvWxYz0123456789AbCdEfGhIj";

    public static String getTokenSecret() {
        Environment environment = (Environment) SpringApplicationContext.getBean("environment");
        return environment.getProperty("tokenSecret");
    }
}
