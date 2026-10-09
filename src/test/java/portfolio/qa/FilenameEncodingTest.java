package portfolio.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class FilenameEncodingTest {
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
    @ValueSource(strings = {"отчёт.pdf", "Заявка №42.txt"})
    @DisplayName("TC-14: preserve Cyrillic filenames through create and read")
    void preservesCyrillicFilename(String filename) throws Exception {
        // Check both responses: correct creation does not guarantee correct retrieval.
        String request = json.writeValueAsString(Map.of("filename", filename, "sizeKb", 12));
        var created = api.post("/jobs", request);
        assertEquals(201, created.statusCode());
        assertEquals("application/json; charset=utf-8",
                created.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(filename, json.readTree(created.body()).get("filename").asText());

        var fetched = api.get(created.headers().firstValue("Location").orElseThrow());
        assertEquals(200, fetched.statusCode());
        assertEquals("application/json; charset=utf-8",
                fetched.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(filename, json.readTree(fetched.body()).get("filename").asText());
    }
}
