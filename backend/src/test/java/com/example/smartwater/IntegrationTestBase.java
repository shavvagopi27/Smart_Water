package com.example.smartwater;

import com.example.smartwater.entity.Role;
import com.example.smartwater.entity.User;
import com.example.smartwater.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the full application on in-memory H2 (PostgreSQL mode) and offers small HTTP helpers. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
    protected static final String ADMIN_EMAIL = "it-admin@test.local";
    protected static final String ADMIN_PASSWORD = "Admin@12345";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper om;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;

    protected synchronized String adminToken() throws Exception {
        if (!users.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            User admin = new User();
            admin.setName("IT Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(encoder.encode(ADMIN_PASSWORD));
            admin.setRole(Role.ADMIN);
            users.save(admin);
        }
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        return call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "password", password), 200).get("token").asText();
    }

    /** Performs a request, asserts the status and returns the parsed JSON body (or null when empty). */
    protected JsonNode call(HttpMethod method, String url, String token, Object body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder b = request(method, url).accept(MediaType.APPLICATION_JSON);
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        MvcResult res = mvc.perform(b).andExpect(status().is(expectedStatus)).andReturn();
        String content = res.getResponse().getContentAsString();
        return content == null || content.isBlank() ? null : om.readTree(content);
    }

    protected static String unique(String prefix) {
        return prefix + System.nanoTime();
    }

    /** Creates apartment + one household + resident + meter; returns ids. */
    protected record Setup(long apartmentId, long householdId, long meterId, String residentEmail) {}

    protected Setup createSetup(String adminToken, String flat, String initialReading) throws Exception {
        long apt = call(HttpMethod.POST, "/api/apartments", adminToken,
                Map.of("name", unique("Apt"), "address", "1 Test Road", "city", "Testville"), 201).get("id").asLong();
        long hh = call(HttpMethod.POST, "/api/households", adminToken,
                Map.of("apartmentId", apt, "flatNumber", flat, "householdSize", 3, "areaSqft", 1000), 201).get("id").asLong();
        String email = unique("res") + "@test.local";
        call(HttpMethod.POST, "/api/households/" + hh + "/residents", adminToken,
                Map.of("name", "Test Resident", "email", email, "password", "Resident@123"), 201);
        long meter = call(HttpMethod.POST, "/api/meters", adminToken,
                Map.of("meterNumber", unique("WM-"), "householdId", hh, "installationDate", java.time.LocalDate.now().minusYears(1).toString(),
                        "initialReading", initialReading), 201).get("id").asLong();
        return new Setup(apt, hh, meter, email);
    }
}
