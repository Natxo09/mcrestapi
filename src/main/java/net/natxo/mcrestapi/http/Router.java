package net.natxo.mcrestapi.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.natxo.mcrestapi.http.middleware.AuthMiddleware;

public class Router {

	private final HttpServer httpServer;
	private final String apiKey;

	public Router(HttpServer httpServer, String apiKey) {
		this.httpServer = httpServer;
		this.apiKey = apiKey;
	}

	public void register(String path, HttpHandler handler) {
		httpServer.createContext(path, new AuthMiddleware(apiKey, handler));
	}
}
