package com.example.smartwater;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsageAndAccessTest extends IntegrationTestBase {

    @Test
    void householdCrudAndConflicts() throws Exception {
        String admin = adminToken();
        long apt = call(HttpMethod.POST, "/api/apartments", admin, Map.of("name", unique("A"), "address", "x", "city", "y"), 201).get("id").asLong();
        long hh = call(HttpMethod.POST, "/api/households", admin,
                Map.of("apartmentId", apt, "flatNumber", "B-1", "householdSize", 4, "areaSqft", 900), 201).get("id").asLong();
        call(HttpMethod.POST, "/api/households", admin, Map.of("apartmentId", apt, "flatNumber", "b-1", "householdSize", 2, "areaSqft", 500), 409);
        call(HttpMethod.POST, "/api/households", admin, Map.of("apartmentId", apt, "flatNumber", "B-2", "householdSize", 0, "areaSqft", 500), 400);

        JsonNode updated = call(HttpMethod.PUT, "/api/households/" + hh, admin,
                Map.of("apartmentId", apt, "flatNumber", "B-1", "householdSize", 5, "areaSqft", 950), 200);
        assertEquals(5, updated.get("householdSize").asInt());
        assertEquals(5, call(HttpMethod.GET, "/api/households/" + hh, admin, null, 200).get("householdSize").asInt());

        call(HttpMethod.DELETE, "/api/apartments/" + apt, admin, null, 409);   // still has a household
        call(HttpMethod.DELETE, "/api/households/" + hh, admin, null, 204);
        call(HttpMethod.GET, "/api/households/" + hh, admin, null, 404);
        call(HttpMethod.DELETE, "/api/apartments/" + apt, admin, null, 204);
    }

    @Test
    void meterReadingRules() throws Exception {
        String admin = adminToken();
        Setup s = createSetup(admin, "C-1", "1200");
        LocalDate d1 = LocalDate.now().minusDays(5);

        JsonNode ok = call(HttpMethod.POST, "/api/water-usage", admin,
                Map.of("householdId", s.householdId(), "meterId", s.meterId(), "readingDate", d1.toString(), "meterReading", 1215), 201);
        assertEquals(15.0, ok.get("consumption").asDouble(), 0.0001);

        // duplicate date -> 409
        call(HttpMethod.POST, "/api/water-usage", admin,
                Map.of("householdId", s.householdId(), "meterId", s.meterId(), "readingDate", d1.toString(), "meterReading", 1220), 409);
        // lower than previous -> 400
        call(HttpMethod.POST, "/api/water-usage", admin,
                Map.of("householdId", s.householdId(), "meterId", s.meterId(), "readingDate", d1.plusDays(1).toString(), "meterReading", 1210), 400);
        // negative -> 400 (bean validation)
        call(HttpMethod.POST, "/api/water-usage", admin,
                Map.of("householdId", s.householdId(), "meterId", s.meterId(), "readingDate", d1.plusDays(1).toString(), "meterReading", -4), 400);
        // future date -> 400
        call(HttpMethod.POST, "/api/water-usage", admin,
                Map.of("householdId", s.householdId(), "meterId", s.meterId(), "readingDate", LocalDate.now().plusDays(2).toString(), "meterReading", 1300), 400);
        // unknown household -> 404
        call(HttpMethod.POST, "/api/water-usage", admin,
                Map.of("householdId", 9999999, "meterId", s.meterId(), "readingDate", d1.plusDays(1).toString(), "meterReading", 1300), 404);

        assertEquals(1, call(HttpMethod.GET, "/api/water-usage/" + s.householdId(), admin, null, 200).size());
        assertEquals(1215.0, call(HttpMethod.GET, "/api/meters/" + s.meterId(), admin, null, 200).get("currentReading").asDouble(), 0.0001);
    }

    @Test
    void csvUploadReportsSuccessDuplicatesAndErrors() throws Exception {
        String admin = adminToken();
        Setup s = createSetup(admin, "D-1", "100");
        String meterNo = call(HttpMethod.GET, "/api/meters/" + s.meterId(), admin, null, 200).get("meterNumber").asText();
        LocalDate d = LocalDate.now().minusDays(10);
        String csv = "meterId,householdId,readingDate,meterReading\n"
                + meterNo + "," + s.householdId() + "," + d + ",110\n"                 // ok
                + meterNo + "," + s.householdId() + "," + d.plusDays(1) + ",118\n"     // ok
                + meterNo + "," + s.householdId() + "," + d + ",111\n"                 // duplicate date
                + meterNo + "," + s.householdId() + "," + d.plusDays(2) + ",90\n"      // lower than previous
                + meterNo + "," + s.householdId() + ",not-a-date,120\n"                // bad date
                + "NO-SUCH-METER," + s.householdId() + "," + d.plusDays(3) + ",130\n"; // unknown meter
        MockMultipartFile file = new MockMultipartFile("file", "readings.csv", "text/csv", csv.getBytes());
        MvcResult res = mvc.perform(multipart("/api/water-usage/upload").file(file)
                .header("Authorization", "Bearer " + admin).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        JsonNode r = om.readTree(res.getResponse().getContentAsString());
        assertEquals(6, r.get("totalRecords").asInt());
        assertEquals(2, r.get("successfulRecords").asInt());
        assertEquals(1, r.get("duplicateRecords").asInt());
        assertEquals(3, r.get("failedRecords").asInt());
        assertEquals(4, r.get("errors").size());
    }

    @Test
    void residentsOnlySeeTheirOwnHousehold() throws Exception {
        String admin = adminToken();
        Setup a = createSetup(admin, "E-1", "0");
        Setup b = createSetup(admin, "E-1", "0");
        String tokenA = login(a.residentEmail(), "Resident@123");

        call(HttpMethod.GET, "/api/households/" + a.householdId(), tokenA, null, 200);
        call(HttpMethod.GET, "/api/households/" + b.householdId(), tokenA, null, 403);
        call(HttpMethod.GET, "/api/water-usage/" + b.householdId(), tokenA, null, 403);
        call(HttpMethod.GET, "/api/water-usage/daily?householdId=" + b.householdId(), tokenA, null, 403);
        call(HttpMethod.GET, "/api/invoices?householdId=" + b.householdId(), tokenA, null, 403);
        call(HttpMethod.POST, "/api/water-usage", tokenA,
                Map.of("householdId", a.householdId(), "meterId", a.meterId(), "readingDate", LocalDate.now().minusDays(1).toString(), "meterReading", 5), 403);
        assertEquals(1, call(HttpMethod.GET, "/api/households", tokenA, null, 200).size());
        call(HttpMethod.GET, "/api/dashboard/resident", tokenA, null, 200);
    }
}
