package net.natxo.mcrestapi.http;

import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.natxo.mcrestapi.MCRestAPI;
import net.natxo.mcrestapi.collectors.PlayerTracker;
import net.natxo.mcrestapi.collectors.TpsCollector;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.endpoints.PlayersEndpoint;
import net.natxo.mcrestapi.endpoints.ServerEndpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ApiServer {

	private final HttpServer httpServer;

	public ApiServer(ApiConfig config, TpsCollector tpsCollector, PlayerTracker playerTracker, DedicatedServer server) throws IOException {
		InetSocketAddress address = new InetSocketAddress(config.getBindAddress(), config.getPort());
		this.httpServer = HttpServer.create(address, config.getMaxConnections());
		this.httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

		Router router = new Router(httpServer, config.getApiKey());
		router.register("/api/server", new ServerEndpoint(tpsCollector, playerTracker, server));
		router.register("/api/players", new PlayersEndpoint(playerTracker));
	}

	public void start() {
		httpServer.start();
		MCRestAPI.LOGGER.info("[MCRestAPI] HTTP server started on {}:{}",
				httpServer.getAddress().getHostString(), httpServer.getAddress().getPort());
	}

	public void stop() {
		httpServer.stop(1);
		MCRestAPI.LOGGER.info("[MCRestAPI] HTTP server stopped");
	}
}
