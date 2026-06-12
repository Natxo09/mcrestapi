package net.natxo.mcrestapi.http.middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;

public class MasterKeyMiddleware implements HttpHandler {

	private final ApiConfig config;
	private final HttpHandler next;

	public MasterKeyMiddleware(ApiConfig config, HttpHandler next) {
		this.config = config;
		this.next = next;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		// When auth is disabled the reverse proxy is the trust boundary, so admin
		// endpoints are open too (consistent with the regular API layer).
		if (!config.isAuthEnabled()) {
			next.handle(exchange);
			return;
		}

		String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			HttpUtil.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
			return;
		}

		String rawKey = authHeader.substring(7);
		if (!config.isMasterKey(rawKey)) {
			HttpUtil.sendJson(exchange, 403, "{\"error\":\"Forbidden: master key required\"}");
			return;
		}

		next.handle(exchange);
	}
}
