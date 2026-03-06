package net.natxo.mcrestapi.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.collectors.EventCollector;
import net.natxo.mcrestapi.collectors.ServerEvent;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();
	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 200;

	private final EventCollector eventCollector;

	public ChatEndpoint(EventCollector eventCollector) {
		this.eventCollector = eventCollector;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		int limit = DEFAULT_LIMIT;
		Set<String> types = null;

		String query = exchange.getRequestURI().getQuery();
		if (query != null) {
			for (String param : query.split("&")) {
				String[] kv = param.split("=", 2);
				if (kv.length == 2) {
					if ("limit".equals(kv[0])) {
						try {
							limit = Math.min(Math.max(1, Integer.parseInt(kv[1])), MAX_LIMIT);
						} catch (NumberFormatException ignored) {
						}
					} else if ("type".equals(kv[0])) {
						types = Arrays.stream(kv[1].split(","))
								.map(String::trim)
								.filter(s -> !s.isEmpty())
								.collect(Collectors.toSet());
					}
				}
			}
		}

		List<ServerEvent> events = eventCollector.getHistory(limit, types);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", events.size());
		response.put("events", events);

		HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
	}
}
