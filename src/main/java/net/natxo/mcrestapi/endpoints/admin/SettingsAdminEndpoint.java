package net.natxo.mcrestapi.endpoints.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsAdminEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();
	private final ApiConfig config;

	public SettingsAdminEndpoint(ApiConfig config) {
		this.config = config;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		switch (exchange.getRequestMethod()) {
			case "GET" -> handleGet(exchange);
			case "PUT" -> handleUpdate(exchange);
			default -> HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
		}
	}

	private void handleGet(HttpExchange exchange) throws IOException {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("port", config.getPort());
		response.put("bind_address", config.getBindAddress());
		response.put("max_connections", config.getMaxConnections());
		response.put("swagger_enabled", config.isSwaggerEnabled());
		HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
	}

	private void handleUpdate(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject json = JsonParser.parseString(body).getAsJsonObject();

		if (json.has("swagger_enabled")) {
			config.setSwaggerEnabled(json.get("swagger_enabled").getAsBoolean());
		}

		config.save();

		handleGet(exchange);
	}
}
