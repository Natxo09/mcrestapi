package net.natxo.mcrestapi.http.middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;

public class AuthMiddleware implements HttpHandler {

	private final String apiKey;
	private final HttpHandler next;

	public AuthMiddleware(String apiKey, HttpHandler next) {
		this.apiKey = apiKey;
		this.next = next;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

		if (authHeader == null || !authHeader.equals("Bearer " + apiKey)) {
			HttpUtil.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
			return;
		}

		next.handle(exchange);
	}
}
