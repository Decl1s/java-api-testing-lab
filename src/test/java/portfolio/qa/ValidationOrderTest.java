package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ValidationOrderTest {
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
    @DisplayName("TC-22: authentication takes priority over request body validation")
    void rejectsUnauthorizedRequestsBeforeBodyValidation(String token) throws Exception {
        String[][] invalidRequests = {
            {"text/plain", "{}", "415"},
            {"application/json", "{broken", "400"},
            {"application/json", "x".repeat(8193), "413"},
        };
        for (String[] request : invalidRequests) {
            var unauthorized = api.request("POST", "/jobs", request[1], token, request[0]);
            assertEquals(401, unauthorized.statusCode());
            assertEquals("unauthorized", json.readTree(unauthorized.body()).get("error").asText());
            assertTrue(unauthorized.headers().firstValue("Location").isEmpty());

            // Same payload with a valid key proves that another validation error really exists.
            var authorized = api.request("POST", "/jobs", request[1], "demo-token", request[0]);
            assertEquals(Integer.parseInt(request[2]), authorized.statusCode());
        }
        assertEquals(404, api.get("/jobs/1").statusCode());
        var created = api.post("/jobs", "{\"filename\":\"sample.pdf\",\"sizeKb\":1}");
        assertEquals(201, created.statusCode());
        assertEquals(1, json.readTree(created.body()).get("id").asInt());
    }
}
