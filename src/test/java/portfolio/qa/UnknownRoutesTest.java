package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UnknownRoutesTest {
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
    @ValueSource(strings = {"/missing", "/jobs/", "/jobs/not-a-number", "/jobs/-1",
            "/jobs/2147483648", "/jobs/1/cancel/extra"})
    @DisplayName("TC-17: unknown routes return JSON 404 and leave existing jobs unchanged")
    void rejectsUnknownRoutesWithoutChangingJob(String path) throws Exception {
        // Create a job first so a broken prefix match cannot hide behind an empty store.
        var created = api.post("/jobs", "{\"filename\":\"sample.pdf\",\"sizeKb\":12}");
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        assertEquals("/jobs/1", location);
        assertNotFound(api.get(path));
        assertNotFound(api.post(path, "{}"));

        var fetched = api.get(location);
        assertEquals(200, fetched.statusCode());
        assertEquals(json.readTree(created.body()), json.readTree(fetched.body()));
    }

    private void assertNotFound(HttpResponse<String> response) throws Exception {
        assertEquals(404, response.statusCode());
        assertEquals("application/json; charset=utf-8",
                response.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("not_found", json.readTree(response.body()).get("error").asText());
        assertTrue(response.headers().firstValue("Location").isEmpty());
    }
}
