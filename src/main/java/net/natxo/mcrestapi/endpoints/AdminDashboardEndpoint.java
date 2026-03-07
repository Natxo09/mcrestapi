package net.natxo.mcrestapi.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AdminDashboardEndpoint implements HttpHandler {

	private static final String RESOURCE_PATH = "/assets/mcrestapi/admin/";
	private static final Map<String, String> CONTENT_TYPES = Map.of(
			"html", "text/html; charset=utf-8",
			"css", "text/css; charset=utf-8",
			"js", "application/javascript; charset=utf-8",
			"svg", "image/svg+xml",
			"png", "image/png"
	);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();

		if (path.equals("/admin")) {
			exchange.getResponseHeaders().set("Location", "/admin/");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
			return;
		}

		String fileName;
		if (path.equals("/admin/")) {
			fileName = "index.html";
		} else {
			fileName = path.substring("/admin/".length());
		}

		if (fileName.contains("..")) {
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
