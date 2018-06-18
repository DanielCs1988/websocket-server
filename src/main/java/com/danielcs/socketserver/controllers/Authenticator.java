package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.annotations.AuthGuard;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.*;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

import java.util.Date;

public class Authenticator {

    private static HttpsJwks httpsJwks = new HttpsJwks(System.getenv("JWKS_ENDPOINT"));
    private static HttpsJwksVerificationKeyResolver resolver = new HttpsJwksVerificationKeyResolver(httpsJwks);


    public static boolean validate(String token) {

        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setExpectedIssuer(System.getenv("ISSUER"))
                    .setExpectedSubject(System.getenv("SUBJECT"))
                    .setExpectedAudience(System.getenv("AUDIENCE"))
                    .setVerificationKeyResolver(resolver)
                    .setJweAlgorithmConstraints(
                            new AlgorithmConstraints(
                                    AlgorithmConstraints.ConstraintType.WHITELIST,
                                    AlgorithmIdentifiers.RSA_USING_SHA256
                            )
                    )
                    .build();
            JwtClaims claims = jwtConsumer.processToClaims(token);
            if (claims.getExpirationTime().getValueInMillis() < new Date().getTime()) {
                System.out.println("Token has already expired!");
                return false;
            }
            System.out.println("JWT validation succeeded! " + claims);
        } catch (InvalidJwtException | MalformedClaimException e) {
            System.out.println("Invalid token! " + e);
            return false;
        }
        return true;
    }

}
