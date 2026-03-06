package net.natxo.mcrestapi.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerIconEndpoint implements HttpHandler {

	private final Path iconPath;

	public ServerIconEndpoint(Path serverDir) {
		this.iconPath = serverDir.resolve("server-icon.png");
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		if (!Files.exists(iconPath)) {
			HttpUtil.sendJson(exchange, 404, "{\"error\":\"No server icon configured\"}");
			return;
		}

		byte[] icon = Files.readAllBytes(iconPath);
		exchange.getResponseHeaders().set("Content-Type", "image/png");
		exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
		exchange.sendResponseHeaders(200, icon.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(icon);
		}
	}
}
