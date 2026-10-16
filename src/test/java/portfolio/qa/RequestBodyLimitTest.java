package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RequestBodyLimitTest {
    private FixtureServer fixture;
    private ApiClient api;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void start() throws Exception {
        fixture = new FixtureServer(0, false);
        api = new ApiClient(fixture.baseUrl());
    }

    @AfterEach void stop() {
        if (fixture != null) fixture.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {8191, 8192, 8193})
    @DisplayName("TC-18: enforce the 8192-byte UTF-8 request boundary")
    void enforcesBodyLimitInBytes(int byteCount) throws Exception {
        String payload = json.writeValueAsString(Map.of("filename", "отчёт.pdf", "sizeKb", 12));
        // JSON permits trailing spaces; keep the fields valid to isolate the transport limit.
        String body = payload + " ".repeat(byteCount - payload.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(byteCount, body.getBytes(StandardCharsets.UTF_8).length);
        assertTrue(body.length() < byteCount, "Cyrillic must distinguish bytes from Java characters");

        var response = api.post("/jobs", body);
        if (byteCount <= 8192) {
            assertEquals(201, response.statusCode());
            var created = json.readTree(response.body());
            assertEquals("отчёт.pdf", created.get("filename").asText());
            assertEquals(12, created.get("sizeKb").asInt());
            var fetched = api.get(response.headers().firstValue("Location").orElseThrow());
            assertEquals(200, fetched.statusCode());
            assertEquals(created, json.readTree(fetched.body()));
        } else {
            assertEquals(413, response.statusCode());
            assertEquals("body_too_large", json.readTree(response.body()).get("error").asText());
            assertTrue(response.headers().firstValue("Location").isEmpty());
            assertEquals(404, api.get("/jobs/1").statusCode());
        }
    }
}
