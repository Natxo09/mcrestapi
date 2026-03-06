package net.natxo.mcrestapi.http.middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.config.ApiConfig;

import java.io.IOException;
import java.util.List;

public class CorsMiddleware implements HttpHandler {

	private final ApiConfig.CorsConfig corsConfig;
	private final HttpHandler next;

	public CorsMiddleware(ApiConfig.CorsConfig corsConfig, HttpHandler next) {
		this.corsConfig = corsConfig;
		this.next = next;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!corsConfig.isEnabled()) {
			next.handle(exchange);
			return;
		}

		String origin = exchange.getRequestHeaders().getFirst("Origin");

		if (origin != null && isAllowedOrigin(origin)) {
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
			exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
			exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
			exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
		}

		if ("OPTIONS".equals(exchange.getRequestMethod())) {
			exchange.sendResponseHeaders(204, -1);
			exchange.close();
			return;
		}

		next.handle(exchange);
	}

	private boolean isAllowedOrigin(String origin) {
		List<String> allowed = corsConfig.getAllowedOrigins();
		return allowed.contains("*") || allowed.contains(origin);
	}
}
