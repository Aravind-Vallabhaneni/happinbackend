// Security.kt
package com.example

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.http.HttpStatusCode
import java.net.URL
import java.util.concurrent.TimeUnit

fun Application.configureSecurity() {
    val config = environment.config.config("ktor.jwt")
    val realm = config.property("realm").getString()
    val firebaseProjectId = config.property("firebaseProjectId").getString()

    val issuer = "https://securetoken.google.com/$firebaseProjectId"
    val audience = firebaseProjectId

    // --- ADD THESE LOGGING LINES ---
    println("Firebase JWT Config:")
    println("  Project ID from config: $firebaseProjectId")
    println("  Expected Issuer: $issuer")
    println("  Expected Audience: $audience")
    // --- END ADDED LOGGING LINES ---

    val jwkProvider = JwkProviderBuilder(URL(issuer))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    authentication {
        jwt {
            this.realm = realm

            verifier(jwkProvider, issuer) {
                withAudience(audience)
            }

            validate { credential ->
                if (credential.payload.audience.contains(audience) && credential.payload.subject != null) {
                    println("JWT Validation SUCCESS: Subject: ${credential.payload.subject}, Audience: ${credential.payload.audience}")
                    JWTPrincipal(credential.payload)
                } else {
                    println("JWT Validation FAILED: Audience mismatch or subject missing.")
                    println("  Token Audience: ${credential.payload.audience}")
                    println("  Expected Audience: $audience")
                    println("  Token Subject: ${credential.payload.subject}")
                    null
                }
            }

            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Token is not valid or expired"))
            }
        }
    }
}
