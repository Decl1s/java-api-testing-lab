package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AuthorizationStateTest {
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
    @NullSource
    @ValueSource(strings = {"wrong-token", ""})
    @DisplayName("TC-21: unauthorized read and cancellation do not expose or change a job")
    void protectsExistingJobFromUnauthorizedRequests(String token) throws Exception {
        var created = api.post("/jobs", "{\"filename\":\"sample.pdf\",\"sizeKb\":12}");
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();

        assertUnauthorized(api.request("GET", location, null, token, null));
        assertUnauthorized(api.request("POST", location + "/cancel", "", token, null));

        var fetched = api.get(location);
        assertEquals(200, fetched.statusCode());
        assertEquals(json.readTree(created.body()), json.readTree(fetched.body()));
        // A rejected cancellation must not consume the first legitimate cancellation.
        var cancelled = api.post(location + "/cancel", "");
        assertEquals(200, cancelled.statusCode());
        assertEquals("CANCELLED", json.readTree(cancelled.body()).get("status").asText());
    }

    private void assertUnauthorized(HttpResponse<String> response) throws Exception {
        assertEquals(401, response.statusCode());
        assertEquals(json.readTree("{\"error\":\"unauthorized\"}"), json.readTree(response.body()));
        assertEquals("application/json; charset=utf-8",
                response.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(response.headers().firstValue("Location").isEmpty());
    }
}
