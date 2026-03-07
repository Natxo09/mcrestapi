package net.natxo.mcrestapi.http.middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.http.HttpUtil;
import net.natxo.mcrestapi.security.ApiKey;

import java.io.IOException;

public class AuthMiddleware implements HttpHandler {

	private final ApiConfig config;
	private final String requiredPermission;
	private final HttpHandler next;

	public AuthMiddleware(ApiConfig config, String requiredPermission, HttpHandler next) {
		this.config = config;
		this.requiredPermission = requiredPermission;
		this.next = next;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String rawKey = extractBearerToken(exchange);

		if (rawKey == null) {
			HttpUtil.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
			return;
		}

		if (config.isMasterKey(rawKey)) {
			next.handle(exchange);
			return;
		}

		ApiKey key = config.findKeyByRawValue(rawKey);
		if (key == null) {
			HttpUtil.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
			return;
		}

		if (!key.hasPermission(requiredPermission)) {
			HttpUtil.sendJson(exchange, 403, "{\"error\":\"Forbidden: missing permission '" + requiredPermission + "'\"}");
			return;
		}

		next.handle(exchange);
	}

	private static String extractBearerToken(HttpExchange exchange) {
		String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}

		// Fallback: query parameter (needed for SSE/EventSource which can't set headers)
		String query = exchange.getRequestURI().getQuery();
		if (query != null) {
			for (String param : query.split("&")) {
				String[] kv = param.split("=", 2);
				if (kv.length == 2 && "_auth".equals(kv[0])) {
					return kv[1];
				}
			}
		}

		return null;
	}
}
