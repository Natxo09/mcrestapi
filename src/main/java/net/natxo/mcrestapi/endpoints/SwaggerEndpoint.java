package net.natxo.mcrestapi.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SwaggerEndpoint implements HttpHandler {

	private static final String RESOURCE_PATH = "/assets/mcrestapi/swagger/";
	private static final Map<String, String> CONTENT_TYPES = Map.of(
			"html", "text/html; charset=utf-8",
			"css", "text/css; charset=utf-8",
			"js", "application/javascript; charset=utf-8"
	);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();

		String fileName;
		if (path.equals("/api/docs") || path.equals("/api/docs/")) {
			fileName = "index.html";
		} else {
			fileName = path.substring("/api/docs/".length());
		}

		if (fileName.contains("..") || fileName.contains("/")) {
			sendError(exchange, 400, "Bad request");
			return;
		}

		String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
		String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");

		try (InputStream is = getClass().getResourceAsStream(RESOURCE_PATH + fileName)) {
			if (is == null) {
				sendError(exchange, 404, "Not found");
				return;
			}

			byte[] bytes = is.readAllBytes();
			exchange.getResponseHeaders().set("Content-Type", contentType);
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}
	}

	private void sendError(HttpExchange exchange, int code, String message) throws IOException {
		byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(code, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}
}
