package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class TrailingJsonTest {
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
    @ValueSource(strings = {" {}", " true", " garbage"})
    @DisplayName("TC-20: reject data after the root JSON object without creating a job")
    void rejectsTrailingJsonData(String suffix) throws Exception {
        String valid = "{\"filename\":\"sample.pdf\",\"sizeKb\":12}";
        var response = api.post("/jobs", valid + suffix);
        assertEquals(400, response.statusCode());
        assertEquals("invalid_json", json.readTree(response.body()).get("error").asText());
        assertTrue(response.headers().firstValue("Location").isEmpty());
        assertEquals(404, api.get("/jobs/1").statusCode());

        // Trailing whitespace is legal JSON and must remain accepted after rejection.
        var created = api.post("/jobs", valid + " \n\t");
        assertEquals(201, created.statusCode());
        assertEquals(1, json.readTree(created.body()).get("id").asInt());
    }
}
