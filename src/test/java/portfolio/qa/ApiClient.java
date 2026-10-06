package portfolio.qa;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Sends real HTTP requests; assertions stay in the tests. */
final class ApiClient {
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    ApiClient(String baseUrl) { this.baseUrl = baseUrl; }
    HttpResponse<String> request(String method, String path, String body, String token, String type)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5));
        if (token != null) request.header("X-Api-Key", token);
        if (type != null) request.header("Content-Type", type);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return request("POST", path, body, "demo-token", "application/json");
    }
    HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return request("GET", path, null, "demo-token", null);
    }
}
