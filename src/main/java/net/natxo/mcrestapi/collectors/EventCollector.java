package net.natxo.mcrestapi.collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.natxo.mcrestapi.MCRestAPI;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventCollector {

	private static final Gson GSON = new GsonBuilder().create();
	private static final int MAX_HISTORY = 200;

	private final Deque<ServerEvent> history = new ConcurrentLinkedDeque<>();
	private final CopyOnWriteArrayList<SseClient> sseClients = new CopyOnWriteArrayList<>();

	public void push(ServerEvent event) {
		history.addLast(event);
		while (history.size() > MAX_HISTORY) {
			history.pollFirst();
		}

		String json = GSON.toJson(event);
		byte[] sseData = formatSse(event.type(), json);

		for (SseClient client : sseClients) {
			if (!client.accepts(event.type())) {
				continue;
			}
			try {
				client.outputStream().write(sseData);
				client.outputStream().flush();
			} catch (IOException e) {
				sseClients.remove(client);
			}
		}
	}

	public List<ServerEvent> getHistory(int limit, Set<String> types) {
		List<ServerEvent> result = new ArrayList<>();
		for (ServerEvent event : history) {
			if (types != null && !types.isEmpty() && !types.contains(event.type())) {
				continue;
			}
			result.add(event);
		}

		if (result.size() > limit) {
			return result.subList(result.size() - limit, result.size());
		}
		return result;
	}

	public void addSseClient(OutputStream os, Set<String> types) {
		sseClients.add(new SseClient(os, types));
	}

	public void removeSseClient(OutputStream os) {
		sseClients.removeIf(c -> c.outputStream() == os);
	}

	public void sendPingToAll() {
		byte[] ping = ":ping\n\n".getBytes(StandardCharsets.UTF_8);
		for (SseClient client : sseClients) {
			try {
				client.outputStream().write(ping);
				client.outputStream().flush();
			} catch (IOException e) {
				sseClients.remove(client);
			}
		}
	}

	public void closeAll() {
		for (SseClient client : sseClients) {
			try {
				client.outputStream().close();
			} catch (IOException ignored) {
			}
		}
		sseClients.clear();
	}

	private static byte[] formatSse(String eventType, String json) {
		String sse = "event: " + eventType + "\ndata: " + json + "\n\n";
		return sse.getBytes(StandardCharsets.UTF_8);
	}

	private record SseClient(OutputStream outputStream, Set<String> types) {
		boolean accepts(String type) {
			return types == null || types.isEmpty() || types.contains(type);
		}
	}
}
