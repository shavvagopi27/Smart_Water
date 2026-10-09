package com.example.smartwater;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthSecurityTest extends IntegrationTestBase {

    @Test
    void registerLoginAndMe() throws Exception {
        String email = unique("new") + "@test.local";
        JsonNode reg = call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "New Resident", "email", email, "password", "StrongPass#1"), 201);
        assertEquals("RESIDENT", reg.get("user").get("role").asText());
        assertFalse(reg.toString().contains("password"), "password data must never be returned");

        String token = login(email, "StrongPass#1");
        JsonNode me = call(HttpMethod.GET, "/api/auth/me", token, null, 200);
        assertEquals(email, me.get("email").asText());

        // stored value is a bcrypt hash, not the plain password
        assertTrue(users.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash().startsWith("$2"));
    }

    @Test
    void duplicateEmailAndBadInputAreRejected() throws Exception {
        String email = unique("dup") + "@test.local";
        Map<String, Object> body = Map.of("name", "Dup", "email", email, "password", "StrongPass#1");
        call(HttpMethod.POST, "/api/auth/register", null, body, 201);
        JsonNode conflict = call(HttpMethod.POST, "/api/auth/register", null, body, 409);
        assertFalse(conflict.get("success").asBoolean());
        assertEquals(409, conflict.get("status").asInt());

        JsonNode invalid = call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "", "email", "not-an-email", "password", "short"), 400);
        assertTrue(invalid.get("errors").has("email"));
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", "nobody@test.local", "password", "whatever1"), 401);
    }

    @Test
    void protectedEndpointsNeedAValidJwt() throws Exception {
        call(HttpMethod.GET, "/api/auth/me", null, null, 401);
        call(HttpMethod.GET, "/api/households", "garbage.token.value", null, 401);
    }

    @Test
    void residentCannotUseAdminApis() throws Exception {
        String email = unique("res") + "@test.local";
        call(HttpMethod.POST, "/api/auth/register", null, Map.of("name", "R", "email", email, "password", "StrongPass#1"), 201);
        String token = login(email, "StrongPass#1");
        call(HttpMethod.GET, "/api/apartments", token, null, 403);
        call(HttpMethod.POST, "/api/tariffs", token, Map.of("apartmentId", 1, "name", "x", "effectiveFrom", "2020-01-01",
                "tiers", java.util.List.of(Map.of("fromKl", 0, "ratePerKl", 10))), 403);
        call(HttpMethod.GET, "/api/billing-cycles", token, null, 403);
        call(HttpMethod.GET, "/api/dashboard/admin", token, null, 403);
        call(HttpMethod.GET, "/api/apartments", adminToken(), null, 200);
    }
}
