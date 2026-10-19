package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SizeLongBoundaryTest {
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
    @ValueSource(strings = {"9223372036854775807", "9223372036854775808",
            "18446744073709551617", "-9223372036854775808", "-9223372036854775809"})
    @DisplayName("TC-19: reject sizeKb at and beyond long boundaries without creating a job")
    void rejectsSizesAtLongBoundaries(String size) throws Exception {
        // Keep the numeric token verbatim: converting it to long here would test the client.
        // 2^64 + 1 would wrap to the valid size 1 if the server narrowed it unchecked.
        String body = "{\"filename\":\"sample.pdf\",\"sizeKb\":" + size + "}";
        var rejected = api.post("/jobs", body);
        assertEquals(422, rejected.statusCode());
        assertEquals("validation_error", json.readTree(rejected.body()).get("error").asText());
        assertTrue(rejected.headers().firstValue("Location").isEmpty());
        assertEquals(404, api.get("/jobs/1").statusCode());

        var created = api.post("/jobs", "{\"filename\":\"sample.pdf\",\"sizeKb\":1}");
        assertEquals(201, created.statusCode());
        assertEquals(1, json.readTree(created.body()).get("id").asInt());
        assertEquals(1, json.readTree(created.body()).get("sizeKb").asInt());
    }
}
