package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class JobsApiTest {
    private FixtureServer fixture;
    private ApiClient api;
    private final ObjectMapper json = new ObjectMapper();
    @BeforeEach void start() throws Exception {
        fixture = new FixtureServer(0, Boolean.getBoolean("demoBug"));
        api = new ApiClient(fixture.baseUrl());
    }
    @AfterEach void stop() { if (fixture != null) fixture.close(); }

    @Test @DisplayName("TC-01: create and read a job; verify headers and JSON fields")
    void createAndRead() throws Exception {
        var created = api.post("/jobs", "{\"filename\":\"sample.pdf\",\"sizeKb\":12}");
        assertEquals(201, created.statusCode());
        assertTrue(created.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
        var body = json.readTree(created.body());
        assertTrue(body.get("id").asInt() > 0);
        assertEquals("sample.pdf", body.get("filename").asText());
        assertEquals(12, body.get("sizeKb").asInt());
        assertEquals("QUEUED", body.get("status").asText());
        var fetched = api.get(created.headers().firstValue("Location").orElseThrow());
        assertEquals(200, fetched.statusCode());
        assertEquals(body, json.readTree(fetched.body()));
    }
    @ParameterizedTest @ValueSource(ints = {1, 2, 4095, 4096})
    @DisplayName("TC-02: valid size boundaries")
    void validSize(int size) throws Exception {
        assertEquals(201, api.post("/jobs", "{\"filename\":\"a.txt\",\"sizeKb\":" + size + "}").statusCode());
    }
    @ParameterizedTest @ValueSource(ints = {-1, 0, 4097, Integer.MAX_VALUE})
    @DisplayName("TC-03: invalid size boundaries")
    void invalidSize(int size) throws Exception {
        var response = api.post("/jobs", "{\"filename\":\"a.txt\",\"sizeKb\":" + size + "}");
        assertEquals(422, response.statusCode());
        assertEquals("validation_error", json.readTree(response.body()).get("error").asText());
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"filename\":\"\",\"sizeKb\":1}",
        "{\"filename\":\"   \",\"sizeKb\":1}", "{\"filename\":null,\"sizeKb\":1}",
        "{\"filename\":123,\"sizeKb\":1}", "{\"filename\":\"a.txt\"}",
        "{\"filename\":\"a.txt\",\"sizeKb\":\"1\"}", "{\"filename\":\"a.txt\",\"sizeKb\":1.5}"})
    @DisplayName("TC-04: missing fields and wrong types")
    void invalidFields(String body) throws Exception { assertEquals(422, api.post("/jobs", body).statusCode()); }
    @ParameterizedTest @ValueSource(strings = {"", "{broken", "null", "[]"})
    @DisplayName("TC-05: malformed or non-object JSON")
    void invalidJson(String body) throws Exception { assertEquals(400, api.post("/jobs", body).statusCode()); }
    @ParameterizedTest @NullSource @ValueSource(strings = {"wrong-token", ""})
    @DisplayName("TC-06: missing or incorrect API key")
    void unauthorized(String token) throws Exception {
        assertEquals(401, api.request("POST", "/jobs", "{}", token, "application/json").statusCode());
    }
    @Test @DisplayName("TC-07: unsupported content type")
    void contentType() throws Exception {
        assertEquals(415, api.request("POST", "/jobs", "{}", "demo-token", "text/plain").statusCode());
    }
    @Test @DisplayName("TC-08: unknown job")
    void unknownJob() throws Exception { assertEquals(404, api.get("/jobs/999999").statusCode()); }
    @Test @DisplayName("TC-09: cancel, persisted state and repeated cancel")
    void cancel() throws Exception {
        var created = api.post("/jobs", "{\"filename\":\"a.txt\",\"sizeKb\":1}");
        String location = created.headers().firstValue("Location").orElseThrow();
        var cancelled = api.post(location + "/cancel", "");
        assertEquals(200, cancelled.statusCode());
        assertEquals("CANCELLED", json.readTree(cancelled.body()).get("status").asText());
        assertEquals("CANCELLED", json.readTree(api.get(location).body()).get("status").asText());
        assertEquals(409, api.post(location + "/cancel", "").statusCode());
    }
    @Test @DisplayName("TC-10: unique IDs; cancellation does not affect another job")
    void independentJobs() throws Exception {
        String body = "{\"filename\":\"a.txt\",\"sizeKb\":1}";
        String first = api.post("/jobs", body).headers().firstValue("Location").orElseThrow();
        String second = api.post("/jobs", body).headers().firstValue("Location").orElseThrow();
        assertNotEquals(first, second);
        api.post(first + "/cancel", "");
        assertEquals("QUEUED", json.readTree(api.get(second).body()).get("status").asText());
    }
    @ParameterizedTest @ValueSource(ints = {255, 256})
    @DisplayName("TC-11: filename length boundary")
    void filenameLength(int length) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("filename", "a".repeat(length), "sizeKb", 1));
        assertEquals(length == 255 ? 201 : 422, api.post("/jobs", body).statusCode());
    }
    @Test @DisplayName("TC-12: unsupported HTTP method")
    void method() throws Exception {
        String location = api.post("/jobs", "{\"filename\":\"a.txt\",\"sizeKb\":1}")
                .headers().firstValue("Location").orElseThrow();
        assertEquals(405, api.request("DELETE", location, null, "demo-token", null).statusCode());
    }
    @Test @DisplayName("TC-13: limit for JSON body size")
    void oversizedBody() throws Exception { assertEquals(413, api.post("/jobs", "x".repeat(8193)).statusCode()); }
}
