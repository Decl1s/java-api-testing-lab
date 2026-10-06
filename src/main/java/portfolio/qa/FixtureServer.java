package portfolio.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** A deterministic test fixture. Does not scan files or emulate an AV SOFT product. */
public final class FixtureServer implements AutoCloseable {
    private final ObjectMapper json = new ObjectMapper();
    private final Map<Integer, Job> jobs = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();
    private final HttpServer server;
    private final boolean demoBug;
    private record Job(int id, String filename, long sizeKb, String status) {}

    public FixtureServer(int port, boolean demoBug) throws IOException {
        this.demoBug = demoBug;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::handle);
        server.start();
    }
    public String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    public void close() { server.stop(0); }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"demo-token".equals(exchange.getRequestHeaders().getFirst("X-Api-Key"))) {
                reply(exchange, 401, Map.of("error", "unauthorized")); return;
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/jobs") && method.equals("POST")) {
                create(exchange); return;
            }
            if (!path.matches("/jobs/[0-9]+(/cancel)?")) {
                reply(exchange, 404, Map.of("error", "not_found")); return;
            }
            int id;
            try { id = Integer.parseInt(path.split("/")[2]); }
            catch (NumberFormatException ex) { reply(exchange, 404, Map.of("error", "not_found")); return; }
            Job job = jobs.get(id);
            if (job == null) { reply(exchange, 404, Map.of("error", "not_found")); return; }
            if (method.equals("GET") && !path.endsWith("/cancel")) {
                reply(exchange, 200, job); return;
            }
            if (method.equals("POST") && path.endsWith("/cancel")) {
                if (job.status().equals("CANCELLED")) {
                    reply(exchange, 409, Map.of("error", "already_cancelled")); return;
                }
                Job cancelled = new Job(id, job.filename(), job.sizeKb(), "CANCELLED");
                jobs.put(id, cancelled); reply(exchange, 200, cancelled); return;
            }
            reply(exchange, 405, Map.of("error", "method_not_allowed"));
        } finally { exchange.close(); }
    }
    private void create(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";")[0].trim().equalsIgnoreCase("application/json")) {
            reply(exchange, 415, Map.of("error", "unsupported_media_type")); return;
        }
        byte[] body = exchange.getRequestBody().readNBytes(8193);
        if (body.length > 8192) { reply(exchange, 413, Map.of("error", "body_too_large")); return; }
        JsonNode input;
        try { input = json.readTree(body); }
        catch (IOException ex) { reply(exchange, 400, Map.of("error", "invalid_json")); return; }
        if (input == null || !input.isObject()) {
            reply(exchange, 400, Map.of("error", "invalid_json")); return;
        }
        JsonNode filename = input.get("filename");
        JsonNode size = input.get("sizeKb");
        if (filename == null || !filename.isTextual() || filename.asText().isBlank()
                || filename.asText().length() > 255 || size == null || !size.isIntegralNumber()
                || !size.canConvertToLong() || size.asLong() < 1
                || (!demoBug && size.asLong() > 4096)) {
            reply(exchange, 422, Map.of("error", "validation_error")); return;
        }
        int id = sequence.incrementAndGet();
        Job job = new Job(id, filename.asText(), size.asLong(), "QUEUED");
        jobs.put(id, job);
        exchange.getResponseHeaders().set("Location", "/jobs/" + id);
        reply(exchange, 201, job);
    }
    private void reply(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = json.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8088 : Integer.parseInt(args[0]);
        FixtureServer fixture = new FixtureServer(port, Boolean.getBoolean("demoBug"));
        Runtime.getRuntime().addShutdownHook(new Thread(fixture::close));
        System.out.println("Fixture listening at " + fixture.baseUrl());
        Thread.currentThread().join();
    }
}
