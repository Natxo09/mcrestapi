package net.natxo.mcrestapi.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.collectors.EventCollector;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EventStreamEndpoint implements HttpHandler {

	private final EventCollector eventCollector;

	public EventStreamEndpoint(EventCollector eventCollector) {
		this.eventCollector = eventCollector;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		Set<String> types = null;
		String query = exchange.getRequestURI().getQuery();
		if (query != null) {
			for (String param : query.split("&")) {
				String[] kv = param.split("=", 2);
				if (kv.length == 2 && "types".equals(kv[0])) {
					types = Arrays.stream(kv[1].split(","))
							.map(String::trim)
							.filter(s -> !s.isEmpty())
							.collect(Collectors.toSet());
				}
			}
		}

		exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
		exchange.getResponseHeaders().set("Cache-Control", "no-cache");
		exchange.getResponseHeaders().set("Connection", "keep-alive");
		exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
		exchange.sendResponseHeaders(200, 0);

		OutputStream os = exchange.getResponseBody();
		eventCollector.addSseClient(os, types);

		try {
			// Keep the connection open until the client disconnects
			// The EventCollector will write events to this OutputStream
			// We block here so the HttpServer doesn't close the exchange
			while (true) {
				Thread.sleep(1000);
				// Check if the stream is still writable
				os.flush();
			}
		} catch (IOException | InterruptedException e) {
			// Client disconnected or thread interrupted
		} finally {
			eventCollector.removeSseClient(os);
			try {
				os.close();
			} catch (IOException ignored) {
			}
		}
	}
}
