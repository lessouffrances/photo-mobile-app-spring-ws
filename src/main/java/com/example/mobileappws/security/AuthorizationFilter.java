package com.example.mobileappws.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

/**
 * AuthorizationFilter runs on every incoming HTTP request AFTER the user has already logged in.
 *
 * Its job is to:
 *   1. Check if the request contains a valid JWT token in the Authorization header.
 *   2. If valid, extract the user's identity from the token and register it with
 *      Spring Security so the request is treated as authenticated.
 *   3. If missing or invalid, pass the request through unauthenticated — Spring Security
 *      will then reject it with 403 if the endpoint requires authentication.
 *
 * This is different from AuthenticationFilter, which handles the initial login (issuing the token).
 * This filter handles all subsequent requests (verifying the token).
 *
 * Extends BasicAuthenticationFilter so that Spring Security automatically includes
 * it in the filter chain for every request.
 */
public class AuthorizationFilter extends BasicAuthenticationFilter {

    /**
     * Constructor — passes the AuthenticationManager up to the parent class.
     * The AuthenticationManager is required by BasicAuthenticationFilter internally.
     * It is provided by Spring Security and injected via the SecurityConfig.
     */
    public AuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    /**
     * Called automatically by Spring Security on every incoming HTTP request.
     *
     * Think of this as the "gatekeeper" — it checks the request's Authorization header
     * before allowing it to reach any controller.
     *
     * @param request  the incoming HTTP request from the client
     * @param response the outgoing HTTP response to the client
     * @param chain    the remaining filters to execute after this one
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        // Read the "Authorization" header from the incoming request.
        // The client is expected to include the JWT here on every request, e.g.:
        //   Authorization: Bearer eyJhbGci...
        String header = request.getHeader(SecurityConstants.HEADER_STRING);

        // If the header is missing entirely, or doesn't start with "Bearer ",
        // this request has no JWT token — skip authentication and move on.
        // Spring Security will handle access control: if the endpoint requires
        // authentication, it will respond with 403 Forbidden.
        if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            chain.doFilter(request, response); // pass to the next filter
            return;
        }

        // The header looks like it contains a JWT — try to parse and verify it.
        // If valid, this returns a UsernamePasswordAuthenticationToken containing the user's identity.
        // If the token is expired or tampered with, JJWT will throw an exception.
        UsernamePasswordAuthenticationToken authenticationToken = getAuthentication(request);

        // Register the authenticated user's identity into the SecurityContextHolder.
        // This is Spring Security's in-memory store for the current request's security state.
        // Once set here, any part of the app (controllers, services) can call
        // SecurityContextHolder.getContext().getAuthentication() to know who is making the request.
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // Pass execution to the next filter in the chain.
        // The request is now marked as authenticated and will reach the controller.
        chain.doFilter(request, response);
    }

    /**
     * Parses and validates the JWT token from the request's Authorization header.
     *
     * Steps:
     *   1. Extract the raw token string by stripping the "Bearer " prefix.
     *   2. Rebuild the same SecretKey that was originally used to SIGN the token.
     *   3. Use JJWT to verify the token's signature and parse its claims (payload).
     *   4. Extract the subject (the user's email/username) from the claims.
     *   5. Return a UsernamePasswordAuthenticationToken representing the authenticated user.
     *
     * @param request the incoming HTTP request containing the Authorization header
     * @return a UsernamePasswordAuthenticationToken if the token is valid, or null if not
     */
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {

        // Re-read the Authorization header value, e.g. "Bearer eyJhbGci..."
        String authorizationHeader = request.getHeader(SecurityConstants.HEADER_STRING);
        if (authorizationHeader == null) {
            return null;
        }

        // Strip the "Bearer " prefix to get just the raw JWT token string.
        // e.g. "Bearer eyJhbGci..." → "eyJhbGci..."
        String token = authorizationHeader.replace(SecurityConstants.TOKEN_PREFIX, "");

        // Rebuild the SecretKey using the same secret and algorithm that was used
        // to sign the token in AuthenticationFilter.
        // Base64-encode the raw secret bytes first, then wrap in a SecretKeySpec for HS512.
        byte[] secretBytes = Base64.getEncoder().encode(SecurityConstants.TOKEN_SECRET.getBytes());
        SecretKey secretKey = new SecretKeySpec(secretBytes, io.jsonwebtoken.SignatureAlgorithm.HS512.getJcaName());

        // Use JJWT to verify the token's signature and parse its payload (claims).
        // - verifyWith(secretKey): ensures the token was signed with our secret key.
        //   If someone tampered with the token, signature verification fails and an exception is thrown.
        // - parseSignedClaims(token): validates the token (signature + expiry) and returns the claims.
        Jws<Claims> parsedToken = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token);

        // Extract the "subject" claim from the token payload.
        // The subject was set to the user's email/username when the token was created in AuthenticationFilter.
        String subject = parsedToken.getPayload().getSubject();

        // If there's no subject in the token, we can't identify the user — return null.
        if (subject == null) {
            return null;
        }

        // Return an authenticated token representing the user.
        // - 1st arg (subject): the user's identity (email/username), used as the principal.
        // - 2nd arg (null): credentials — not needed post-authentication.
        // - 3rd arg (new ArrayList<>()): granted authorities/roles — empty for now.
        // The 3-argument constructor signals to Spring Security that this token IS authenticated.
        return new UsernamePasswordAuthenticationToken(subject, null, new ArrayList<>());
    }
}