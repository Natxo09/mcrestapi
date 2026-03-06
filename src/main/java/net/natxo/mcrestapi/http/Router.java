package net.natxo.mcrestapi.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.http.middleware.AuthMiddleware;
import net.natxo.mcrestapi.http.middleware.CorsMiddleware;
import net.natxo.mcrestapi.http.middleware.MasterKeyMiddleware;

public class Router {

	private final HttpServer httpServer;
	private final ApiConfig config;

	public Router(HttpServer httpServer, ApiConfig config) {
		this.httpServer = httpServer;
		this.config = config;
	}

	public void register(String path, HttpHandler handler, String permission) {
		HttpHandler chain = new AuthMiddleware(config, permission, handler);
		chain = new CorsMiddleware(config.getCors(), chain);
		httpServer.createContext(path, chain);
	}

	public void registerPublic(String path, HttpHandler handler) {
		HttpHandler chain = new CorsMiddleware(config.getCors(), handler);
		httpServer.createContext(path, chain);
	}

	public void registerAdmin(String path, HttpHandler handler) {
		HttpHandler chain = new MasterKeyMiddleware(config, handler);
		chain = new CorsMiddleware(config.getCors(), chain);
		httpServer.createContext(path, chain);
	}
}
