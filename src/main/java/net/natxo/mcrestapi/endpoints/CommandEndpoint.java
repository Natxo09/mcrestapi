package net.natxo.mcrestapi.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CommandEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();
	private final DedicatedServer server;

	public CommandEndpoint(DedicatedServer server) {
		this.server = server;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject json;
		try {
			json = JsonParser.parseString(body).getAsJsonObject();
		} catch (Exception e) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Invalid JSON\"}");
			return;
		}

		if (!json.has("command")) {
			HttpUtil.sendJson(exchange, 400, "{\"error\":\"Missing 'command' field\"}");
			return;
		}

		String command = json.get("command").getAsString().trim();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}

		try {
			String finalCommand = command;
			CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

			server.execute(() -> {
				StringBuilder output = new StringBuilder();

				CommandSource capturingSource = new CommandSource() {
					@Override
					public void sendSystemMessage(Component message) {
						if (!output.isEmpty()) {
							output.append("\n");
						}
						output.append(message.getString());
					}

					@Override
					public boolean acceptsSuccess() {
						return true;
					}

					@Override
					public boolean acceptsFailure() {
						return true;
					}

					@Override
					public boolean shouldInformAdmins() {
						return false;
					}
				};

				CommandSourceStack source = server.createCommandSourceStack()
						.withSource(capturingSource);

				server.getCommands().performPrefixedCommand(source, finalCommand);

				Map<String, Object> response = new LinkedHashMap<>();
				response.put("success", true);
				response.put("output", output.toString());
				future.complete(response);
			});

			Map<String, Object> response = future.get(5, TimeUnit.SECONDS);
			HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
		} catch (TimeoutException e) {
			HttpUtil.sendJson(exchange, 503, "{\"error\":\"Server tick timeout\"}");
		} catch (Exception e) {
			HttpUtil.sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
		}
	}
}
