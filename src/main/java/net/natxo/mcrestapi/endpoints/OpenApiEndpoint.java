package net.natxo.mcrestapi.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class OpenApiEndpoint implements HttpHandler {

	private final String specJson;

	public OpenApiEndpoint(int port) {
		String template = loadSpec();
		this.specJson = template.replace("{{PORT}}", String.valueOf(port));
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		HttpUtil.sendJson(exchange, 200, specJson);
	}

	private static String loadSpec() {
		try (InputStream is = OpenApiEndpoint.class.getResourceAsStream("/assets/mcrestapi/openapi.json")) {
			if (is == null) {
				return "{\"openapi\":\"3.1.0\",\"info\":{\"title\":\"MCRestAPI\",\"version\":\"1.0.0\"},\"paths\":{}}";
			}
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "{\"openapi\":\"3.1.0\",\"info\":{\"title\":\"MCRestAPI\",\"version\":\"1.0.0\"},\"paths\":{}}";
		}
	}
}
