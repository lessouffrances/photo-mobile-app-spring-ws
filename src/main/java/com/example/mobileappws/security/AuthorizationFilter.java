package com.example.mobileappws.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

// let authorized clients to change info
// enable the api endpoint being protected by the Jwts token

public class AuthorizationFilter extends BasicAuthenticationFilter {

    public AuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        String header = request.getHeader(SecurityConstants.HEADER_STRING);

        // check if Jwts token is valid
        if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            // pass execution to the next filter in chain
            chain.doFilter(request, response);
            return;
        }

        // if is valid, return an authentication token object
        UsernamePasswordAuthenticationToken authenticationToken = getAuthentication(request);
        // put the obj into Spring Application Context holder
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        // pass execution to the next filter in chain
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {

        String authorizationHeader = request.getHeader(SecurityConstants.HEADER_STRING);
        if (authorizationHeader == null) {
            return null;
        }

        // Strip the "Bearer " prefix
        String token = authorizationHeader.replace(SecurityConstants.TOKEN_PREFIX, "");

        // encode the token secret
        byte[] secretBytes = Base64.getEncoder().encode(SecurityConstants.TOKEN_SECRET.getBytes());
        SecretKey secretKey = new SecretKeySpec(secretBytes, io.jsonwebtoken.SignatureAlgorithm.HS512.getJcaName());

        // use the encoded token secret to verify the token
        // parse token, get a key/value pairs
        Jws<Claims> parsedToken = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token);
        
        String subject = parsedToken.getPayload().getSubject();

        if (subject == null) {
            return null;
        }

        return new UsernamePasswordAuthenticationToken(subject, null, new ArrayList<>());
    }
}
