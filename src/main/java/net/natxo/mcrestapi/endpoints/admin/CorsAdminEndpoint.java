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

public class CorsAdminEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();
	private final ApiConfig config;

	public CorsAdminEndpoint(ApiConfig config) {
		this.config = config;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		switch (exchange.getRequestMethod()) {
			case "GET" -> handleGet(exchange);
			case "POST" -> handleAddOrigin(exchange);
			case "PUT" -> handleToggle(exchange);
			case "DELETE" -> handleRemoveOrigin(exchange);
			default -> HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
		}
	}

	private void handleGet(HttpExchange exchange) throws IOException {
		ApiConfig.CorsConfig cors = config.getCors();
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("enabled", cors.isEnabled());
		response.put("allowed_origins", cors.getAllowedOrigins());
		HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
	}

	private void handleAddOrigin(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject json = JsonParser.parseString(body).getAsJsonObject();

		if (!json.has("origin")) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Missing 'origin'\"}");
			return;
		}

		String origin = json.get("origin").getAsString();
		config.getCors().addOrigin(origin);
		config.save();

		HttpUtil.sendJson(exchange, 201, GSON.toJson(Map.of(
				"message", "Origin added",
				"origin", origin
		)));
	}

	private void handleToggle(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject json = JsonParser.parseString(body).getAsJsonObject();

		if (!json.has("enabled")) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Missing 'enabled'\"}");
			return;
		}

		boolean enabled = json.get("enabled").getAsBoolean();
		config.getCors().setEnabled(enabled);
		config.save();

		HttpUtil.sendJson(exchange, 200, GSON.toJson(Map.of(
				"message", "CORS " + (enabled ? "enabled" : "disabled"),
				"enabled", enabled
		)));
	}

	private void handleRemoveOrigin(HttpExchange exchange) throws IOException {
		String query = exchange.getRequestURI().getQuery();
		String origin = null;
		if (query != null) {
			for (String param : query.split("&")) {
				String[] kv = param.split("=", 2);
				if (kv.length == 2 && "origin".equals(kv[0])) {
					origin = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
				}
			}
		}

		if (origin == null) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Missing 'origin' query parameter\"}");
			return;
		}

		if (!config.getCors().removeOrigin(origin)) {
			HttpUtil.sendJson(exchange, 404, "{\"error\":\"Origin not found\"}");
			return;
		}

		config.save();
		HttpUtil.sendJson(exchange, 200, "{\"message\":\"Origin removed\"}");
	}
}
