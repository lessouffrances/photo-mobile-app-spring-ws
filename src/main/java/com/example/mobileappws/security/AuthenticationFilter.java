package com.example.mobileappws.security;

import com.example.mobileappws.ui.model.request.UserLoginRequestModel;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public AuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res) throws AuthenticationException
    {
        try {
            // Step 1: Deserialize the request body
            // The client sends a POST /login with a JSON body like:
            // { "email": "a@b.com", "password": "123" }
            // ObjectMapper reads the raw input stream and maps the JSON fields
            // into a UserLoginRequestModel object, so we can access creds.getEmail()
            // and creds.getPassword() as typed Java fields.
            UserLoginRequestModel creds = new ObjectMapper().readValue(req.getInputStream(), UserLoginRequestModel.class);

            // Step 2: Build an authentication token
            // UsernamePasswordAuthenticationToken is Spring Security's standard
            // credential container. It wraps the email and password into an object
            // that the AuthenticationManager understands.
            // The third argument is an empty list of authorities — at this stage
            // we are not granting any roles yet, just verifying identity.
            // Once authenticated, Spring will populate the authorities from UserDetails.
            UsernamePasswordAuthenticationToken authToken
                = new UsernamePasswordAuthenticationToken(creds.getEmail(), creds.getPassword(), new ArrayList<>());

            // Step 3: Delegate to the AuthenticationManager
            // AuthenticationManager hands the token to your UserDetailsService,
            // which loads the user from the DB by email, then compares the
            // provided password against the BCrypt-hashed password in the DB.
            // If they match, it returns a fully populated Authentication object.
            // If they don't match, it throws an AuthenticationException,
            // which Spring Security catches and returns a 401 Unauthorized response.
            return getAuthenticationManager().authenticate(authToken);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        byte[] secretKeyBytes = Base64.getEncoder().encode(SecurityConstants.TOKEN_SECRET.getBytes());
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, io.jsonwebtoken.SignatureAlgorithm.HS512.getJcaName());
        Instant now = Instant.now();

        String userName = ((User) auth.getPrincipal()).getUsername();
        String token = Jwts.builder()
            .setSubject(userName)
            .setExpiration(
                Date.from(now.plusMillis(SecurityConstants.EXPIRATION_TIME)))
            .setIssuedAt(Date.from(now))
            .signWith(secretKey, io.jsonwebtoken.SignatureAlgorithm.HS512).compact();

        res.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
    }
 }
