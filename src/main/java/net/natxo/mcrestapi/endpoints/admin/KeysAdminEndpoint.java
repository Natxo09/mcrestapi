package net.natxo.mcrestapi.endpoints.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.http.HttpUtil;
import net.natxo.mcrestapi.security.ApiKey;
import net.natxo.mcrestapi.security.KeyUtil;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeysAdminEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();
	private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
	private final ApiConfig config;

	public KeysAdminEndpoint(ApiConfig config) {
		this.config = config;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		switch (exchange.getRequestMethod()) {
			case "GET" -> handleList(exchange);
			case "POST" -> handleCreate(exchange);
			case "PUT" -> handleUpdate(exchange);
			case "DELETE" -> handleDelete(exchange);
			default -> HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
		}
	}

	private void handleList(HttpExchange exchange) throws IOException {
		List<Map<String, Object>> result = config.getKeys().stream()
				.map(k -> {
					Map<String, Object> map = new LinkedHashMap<>();
					map.put("id", k.getId());
					map.put("name", k.getName());
					map.put("permissions", k.getPermissions());
					map.put("created_at", k.getCreatedAt());
					return map;
				})
				.toList();

		HttpUtil.sendJson(exchange, 200, GSON.toJson(Map.of("keys", result)));
	}

	private void handleCreate(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject json = JsonParser.parseString(body).getAsJsonObject();

		if (!json.has("name") || !json.has("permissions")) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Missing 'name' and/or 'permissions'\"}");
			return;
		}

		String name = json.get("name").getAsString();
		List<String> permissions = GSON.fromJson(json.get("permissions"), STRING_LIST_TYPE);

		String rawKey = KeyUtil.generateKey();
		ApiKey apiKey = new ApiKey(KeyUtil.generateId(), name, KeyUtil.hash(rawKey), permissions);

		config.addKey(apiKey);
		config.save();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("id", apiKey.getId());
		response.put("name", apiKey.getName());
		response.put("key", rawKey);
		response.put("permissions", apiKey.getPermissions());
		response.put("created_at", apiKey.getCreatedAt());
		response.put("warning", "Save this key now. It will not be shown again.");

		HttpUtil.sendJson(exchange, 201, GSON.toJson(response));
	}

	private void handleUpdate(HttpExchange exchange) throws IOException {
		String id = extractQueryParam(exchange, "id");
		if (id == null) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Missing 'id' query parameter\"}");
			return;
		}

		ApiKey key = config.findKeyById(id);
		if (key == null) {
			HttpUtil.sendJson(exchange, 404, "{\"error\":\"Key not found\"}");
			return;
		}

		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject json = JsonParser.parseString(body).getAsJsonObject();

		if (json.has("name")) {
			key.setName(json.get("name").getAsString());
		}
		if (json.has("permissions")) {
			List<String> permissions = GSON.fromJson(json.get("permissions"), STRING_LIST_TYPE);
			key.setPermissions(permissions);
		}

		config.save();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("id", key.getId());
		response.put("name", key.getName());
		response.put("permissions", key.getPermissions());
		response.put("created_at", key.getCreatedAt());

		HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
	}

	private void handleDelete(HttpExchange exchange) throws IOException {
		String id = extractQueryParam(exchange, "id");
		if (id == null) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Missing 'id' query parameter\"}");
			return;
		}

		if (!config.removeKey(id)) {
			HttpUtil.sendJson(exchange, 404, "{\"error\":\"Key not found\"}");
			return;
		}

		config.save();
		HttpUtil.sendJson(exchange, 200, "{\"message\":\"Key revoked\"}");
	}

	private static String extractQueryParam(HttpExchange exchange, String name) {
		String query = exchange.getRequestURI().getQuery();
		if (query == null) return null;
		for (String param : query.split("&")) {
			String[] kv = param.split("=", 2);
			if (kv.length == 2 && name.equals(kv[0])) {
				return kv[1];
			}
		}
		return null;
	}
}
