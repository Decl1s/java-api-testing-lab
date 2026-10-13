package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ContentTypeTest {
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
    @ValueSource(strings = {"application/json; charset=utf-8", "Application/JSON; charset=UTF-8"})
    @DisplayName("TC-15: JSON with UTF-8 charset preserves the request data")
    void acceptsJsonWithCharset(String contentType) throws Exception {
        String request = json.writeValueAsString(Map.of("filename", "отчёт.pdf", "sizeKb", 12));
        var created = api.request("POST", "/jobs", request, "demo-token", contentType);
        assertEquals(201, created.statusCode());
        var body = json.readTree(created.body());
        assertEquals("отчёт.pdf", body.get("filename").asText());
        assertEquals(12, body.get("sizeKb").asInt());
        assertEquals("QUEUED", body.get("status").asText());
        var fetched = api.get(created.headers().firstValue("Location").orElseThrow());
        assertEquals(200, fetched.statusCode());
        assertEquals(body, json.readTree(fetched.body()));
    }

    @Test
    @DisplayName("TC-16: missing Content-Type rejects valid JSON without creating a job")
    void rejectsMissingContentType() throws Exception {
        String request = "{\"filename\":\"sample.pdf\",\"sizeKb\":12}";
        // A valid payload isolates header validation from JSON parsing and field validation.
        var rejected = api.request("POST", "/jobs", request, "demo-token", null);
        assertEquals(415, rejected.statusCode());
        assertEquals("unsupported_media_type", json.readTree(rejected.body()).get("error").asText());
        assertTrue(rejected.headers().firstValue("Location").isEmpty());
        var missing = api.get("/jobs/1");
        assertEquals(404, missing.statusCode());
        assertEquals("not_found", json.readTree(missing.body()).get("error").asText());
        var created = api.post("/jobs", request);
        assertEquals(201, created.statusCode());
        assertEquals(1, json.readTree(created.body()).get("id").asInt());
    }
}
