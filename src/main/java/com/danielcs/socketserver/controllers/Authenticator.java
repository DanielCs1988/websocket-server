package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.AuthGuard;
import com.danielcs.socketserver.SocketContext;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.*;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

import java.util.Date;

public class Authenticator implements AuthGuard {

    private HttpsJwks httpsJwks;
    private HttpsJwksVerificationKeyResolver resolver;
    private LoggerService logger;

    public Authenticator(HttpsJwks httpsJwks, LoggerService logger) {
        this.httpsJwks = httpsJwks;
        this.logger = logger;
        this.resolver = new HttpsJwksVerificationKeyResolver(httpsJwks);
    }

    @Override
    public boolean authorize(SocketContext ctx, String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setExpectedIssuer(System.getenv("ISSUER"))
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
            logger.log("JWT validation succeeded! " + claims);
            ctx.setProperty("userId", 17);
        } catch (InvalidJwtException | MalformedClaimException e) {
            System.out.println("Invalid token! " + e);
            return false;
        }
        return true;
    }
}
