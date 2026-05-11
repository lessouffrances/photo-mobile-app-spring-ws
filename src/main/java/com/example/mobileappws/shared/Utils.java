package com.example.mobileappws.shared;

import com.example.mobileappws.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

@Component
public class Utils {
    private final Random RANDOM = new SecureRandom();
    private final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public String generateUserId(int length) {
        return generateRandomString(length);
    }

    public String generateAddressId(int length) {
        return generateRandomString(length);
    }

    private String generateRandomString(int length)
    {
        StringBuilder returnValue = new StringBuilder(length);

        for (int i=0; i<length; i++) {
            returnValue.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new String(returnValue);
    }

    public static boolean hasTokenExpired(String token)
    {
        SecretKey secretKey = Keys.hmacShaKeyFor(
            SecurityConstants.getTokenSecret().getBytes(StandardCharsets.UTF_8)
        );

        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        Date tokenExpirationDate = claims.getExpiration();
        Date todayDate = new Date();

        return tokenExpirationDate.before(todayDate);
    }

    public static String generateEmailVerificationToken(String userId)
    {
        SecretKey secretKey = Keys.hmacShaKeyFor(
            SecurityConstants.getTokenSecret().getBytes(StandardCharsets.UTF_8)
        );

        String token = Jwts.builder()
            .subject(userId)
            .expiration(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
            .signWith(secretKey)
            .compact();

        return token;
    }

    public static String generatePasswordResetToken(String userId)
    {
        SecretKey secretKey = Keys.hmacShaKeyFor(
            SecurityConstants.getTokenSecret().getBytes(StandardCharsets.UTF_8)
        );

        String token = Jwts.builder()
            .setSubject(userId)
            .setExpiration(new Date(System.currentTimeMillis() + SecurityConstants.PASSWORD_RESET_EXPIRATION_TIME))
            .signWith(secretKey)
            .compact();
        return token;
    }
}
