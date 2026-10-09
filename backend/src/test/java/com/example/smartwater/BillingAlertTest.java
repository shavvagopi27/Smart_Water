package com.example.smartwater;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingAlertTest extends IntegrationTestBase {

    private void tariff(String admin, long apt) throws Exception {
        call(HttpMethod.POST, "/api/tariffs", admin, Map.of("apartmentId", apt, "name", "Test", "effectiveFrom", "2020-01-01",
                "tiers", List.of(
                        Map.of("fromKl", 0, "toKl", 10, "ratePerKl", 20),
                        Map.of("fromKl", 10, "toKl", 20, "ratePerKl", 30),
                        Map.of("fromKl", 20, "ratePerKl", 40))), 201);
    }

    @Test
    void tariffValidationRejectsGaps() throws Exception {
        String admin = adminToken();
        Setup s = createSetup(admin, "T-1", "0");
        call(HttpMethod.POST, "/api/tariffs", admin, Map.of("apartmentId", s.apartmentId(), "name", "Bad", "effectiveFrom", "2020-01-01",
                "tiers", List.of(Map.of("fromKl", 0, "toKl", 10, "ratePerKl", 20), Map.of("fromKl", 15, "ratePerKl", 30))), 400);
    }

    @Test
    void fullBillingFlowProducesExpectedInvoice() throws Exception {
        String admin = adminToken();
        Setup s = createSetup(admin, "F-1", "1200");
        tariff(admin, s.apartmentId());

        LocalDate start = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        // 10 kL on day 1, 5 kL on day 2  => 15 kL
        call(HttpMethod.POST, "/api/water-usage", admin, Map.of("householdId", s.householdId(), "meterId", s.meterId(),
                "readingDate", start.toString(), "meterReading", 1210), 201);
        call(HttpMethod.POST, "/api/water-usage", admin, Map.of("householdId", s.householdId(), "meterId", s.meterId(),
                "readingDate", start.plusDays(1).toString(), "meterReading", 1215), 201);
        // 10 kL x Rs.12 = Rs.120 shared cost (single household bears all of it)
        long purchase = call(HttpMethod.POST, "/api/water-purchases", admin, Map.of("apartmentId", s.apartmentId(), "supplier", "Tanker Co",
                "supplyType", "TANKER", "purchaseDate", start.plusDays(2).toString(), "volumeKl", 10, "unitCost", 12), 201).get("id").asLong();

        long cycle = call(HttpMethod.POST, "/api/billing-cycles", admin, Map.of("apartmentId", s.apartmentId(), "name", "Cycle",
                "startDate", start.toString(), "endDate", start.plusDays(10).toString()), 201).get("id").asLong();
        // overlapping cycle -> 409
        call(HttpMethod.POST, "/api/billing-cycles", admin, Map.of("apartmentId", s.apartmentId(), "name", "Overlap",
                "startDate", start.plusDays(5).toString(), "endDate", start.plusDays(20).toString()), 409);

        JsonNode invoices = call(HttpMethod.POST, "/api/billing-cycles/" + cycle + "/generate", admin, null, 200);
        assertEquals(1, invoices.size());
        JsonNode inv = invoices.get(0);
        assertEquals(15.0, inv.get("consumption").asDouble(), 0.0001);
        assertEquals(1200.0, inv.get("previousReading").asDouble(), 0.0001);
        assertEquals(1215.0, inv.get("currentReading").asDouble(), 0.0001);
        assertEquals(350.0, inv.get("waterCharge").asDouble(), 0.0001);
        assertEquals(120.0, inv.get("sharedCost").asDouble(), 0.0001);
        assertEquals(470.0, inv.get("totalAmount").asDouble(), 0.0001);
        assertTrue(inv.get("items").size() >= 3);
        long invoiceId = inv.get("id").asLong();

        // adjustment re-generation
        JsonNode adjusted = call(HttpMethod.POST, "/api/billing-cycles/" + cycle + "/generate", admin,
                Map.of("adjustments", List.of(Map.of("householdId", s.householdId(), "amount", -20, "reason", "Goodwill credit"))), 200);
        assertEquals(450.0, adjusted.get(0).get("totalAmount").asDouble(), 0.0001);
        invoiceId = adjusted.get(0).get("id").asLong();

        // PDF download
        MvcResult pdf = mvc.perform(get("/api/invoices/" + invoiceId + "/pdf").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn();
        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 500);
        assertEquals("%PDF", new String(bytes, 0, 4));

        // resident sees own invoice, not others'
        String resident = login(s.residentEmail(), "Resident@123");
        assertEquals(1, call(HttpMethod.GET, "/api/invoices", resident, null, 200).size());
        call(HttpMethod.GET, "/api/invoices/" + invoiceId, resident, null, 200);
        Setup other = createSetup(admin, "F-2", "0");
        call(HttpMethod.GET, "/api/invoices/" + invoiceId, login(other.residentEmail(), "Resident@123"), null, 403);

        // finalize locks the cycle
        assertEquals("FINALIZED", call(HttpMethod.PUT, "/api/billing-cycles/" + cycle + "/finalize", admin, null, 200).get("status").asText());
        call(HttpMethod.POST, "/api/billing-cycles/" + cycle + "/generate", admin, null, 409);
        call(HttpMethod.PUT, "/api/billing-cycles/" + cycle + "/finalize", admin, null, 409);
        call(HttpMethod.DELETE, "/api/water-purchases/" + purchase, admin, null, 409);
        call(HttpMethod.POST, "/api/water-usage", admin, Map.of("householdId", s.householdId(), "meterId", s.meterId(),
                "readingDate", start.plusDays(3).toString(), "meterReading", 1220), 400);
        assertEquals("ARCHIVED", call(HttpMethod.PUT, "/api/billing-cycles/" + cycle + "/archive", admin, null, 200).get("status").asText());
    }

    @Test
    void unmeteredHouseholdsFallBackToAreaBasedSharing() throws Exception {
        String admin = adminToken();
        Setup s = createSetup(admin, "G-1", "0");          // will have readings
        tariff(admin, s.apartmentId());
        long second = call(HttpMethod.POST, "/api/households", admin,
                Map.of("apartmentId", s.apartmentId(), "flatNumber", "G-2", "householdSize", 2, "areaSqft", 1000), 201).get("id").asLong();
        LocalDate start = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        call(HttpMethod.POST, "/api/water-usage", admin, Map.of("householdId", s.householdId(), "meterId", s.meterId(),
                "readingDate", start.toString(), "meterReading", 8), 201);
        call(HttpMethod.POST, "/api/water-purchases", admin, Map.of("apartmentId", s.apartmentId(), "supplier", "Board",
                "supplyType", "MUNICIPAL", "purchaseDate", start.toString(), "volumeKl", 10, "unitCost", 100), 201);   // Rs.1000
        long cycle = call(HttpMethod.POST, "/api/billing-cycles", admin, Map.of("apartmentId", s.apartmentId(), "name", "C",
                "startDate", start.toString(), "endDate", start.plusDays(9).toString()), 201).get("id").asLong();

        JsonNode invoices = call(HttpMethod.POST, "/api/billing-cycles/" + cycle + "/generate", admin, null, 200);
        for (JsonNode i : invoices) {
            assertEquals(500.0, i.get("sharedCost").asDouble(), 0.0001);   // equal areas -> 50/50
            if (i.get("householdId").asLong() == second) assertEquals("AREA_FALLBACK", i.get("sharedCostBasis").asText());
            else assertEquals("METERED", i.get("sharedCostBasis").asText());
        }
    }

    @Test
    void abnormalUsageIsFlaggedAsPotentialNotAsDefiniteLeak() throws Exception {
        String admin = adminToken();
        Setup s = createSetup(admin, "H-1", "0");
        LocalDate day = LocalDate.now().minusDays(12);
        double reading = 0;
        double[] normal = {0.5, 0.6, 0.55, 0.5, 0.45, 0.6, 0.5, 0.55, 0.5, 0.6, 0.5};
        for (double v : normal) {
            reading += v;
            call(HttpMethod.POST, "/api/water-usage", admin, Map.of("householdId", s.householdId(), "meterId", s.meterId(),
                    "readingDate", day.toString(), "meterReading", reading), 201);
            day = day.plusDays(1);
        }
        reading += 3.0;   // sudden spike yesterday
        call(HttpMethod.POST, "/api/water-usage", admin, Map.of("householdId", s.householdId(), "meterId", s.meterId(),
                "readingDate", day.toString(), "meterReading", reading), 201);

        call(HttpMethod.POST, "/api/alerts/run-detection", admin, null, 200);
        JsonNode alerts = call(HttpMethod.GET, "/api/alerts?householdId=" + s.householdId(), admin, null, 200);
        boolean abnormal = false;
        for (JsonNode a : alerts) {
            if (a.get("alertType").asText().equals("ABNORMAL_USAGE")) {
                abnormal = true;
                assertTrue(a.get("message").asText().startsWith("Potential abnormal usage detected."));
                assertFalse(a.get("message").asText().toLowerCase().contains("definitely"));
            }
        }
        assertTrue(abnormal, "expected an ABNORMAL_USAGE alert");

        // detection is idempotent
        int before = alerts.size();
        call(HttpMethod.POST, "/api/alerts/run-detection", admin, null, 200);
        assertEquals(before, call(HttpMethod.GET, "/api/alerts?householdId=" + s.householdId(), admin, null, 200).size());

        // resident can read and mark own alert read
        String resident = login(s.residentEmail(), "Resident@123");
        JsonNode mine = call(HttpMethod.GET, "/api/alerts", resident, null, 200);
        assertEquals(before, mine.size());
        assertTrue(call(HttpMethod.PUT, "/api/alerts/" + mine.get(0).get("id").asLong() + "/read", resident, null, 200).get("read").asBoolean());
    }
}
