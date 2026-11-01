package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RestartStateTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("TC-23: a new server has no jobs or ID sequence from the previous instance")
    void startsWithEmptyStateAfterRestart() throws Exception {
        String previousLocation;
        try (var first = new FixtureServer(0, false)) {
            var api = new ApiClient(first.baseUrl());
            var created = api.post("/jobs", "{\"filename\":\"old.pdf\",\"sizeKb\":12}");
            assertEquals(201, created.statusCode());
            previousLocation = created.headers().firstValue("Location").orElseThrow();
            var cancelled = api.post(previousLocation + "/cancel", "");
            assertEquals(200, cancelled.statusCode());
            assertEquals("CANCELLED", json.readTree(cancelled.body()).get("status").asText());
        }

        // Use the new instance's address: connection failure at the old port proves no state rule.
        try (var restarted = new FixtureServer(0, false)) {
            var api = new ApiClient(restarted.baseUrl());
            var missing = api.get(previousLocation);
            assertEquals(404, missing.statusCode());
            assertEquals("not_found", json.readTree(missing.body()).get("error").asText());

            var created = api.post("/jobs", "{\"filename\":\"new.pdf\",\"sizeKb\":1}");
            assertEquals(201, created.statusCode());
            var body = json.readTree(created.body());
            assertEquals(1, body.get("id").asInt());
            assertEquals("new.pdf", body.get("filename").asText());
            assertEquals(1, body.get("sizeKb").asInt());
            assertEquals("QUEUED", body.get("status").asText());
            var fetched = api.get(created.headers().firstValue("Location").orElseThrow());
            assertEquals(200, fetched.statusCode());
            assertEquals(body, json.readTree(fetched.body()));
        }
    }
}
