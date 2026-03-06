package net.natxo.mcrestapi.collectors;

import java.time.Instant;

public record ServerEvent(
		String type,
		String player,
		String content,
		String timestamp
) {
	public static ServerEvent chat(String player, String content) {
		return new ServerEvent("chat", player, content, Instant.now().toString());
	}

	public static ServerEvent command(String player, String content) {
		return new ServerEvent("command", player, content, Instant.now().toString());
	}

	public static ServerEvent join(String player) {
		return new ServerEvent("join", player, player + " joined the game", Instant.now().toString());
	}

	public static ServerEvent leave(String player) {
		return new ServerEvent("leave", player, player + " left the game", Instant.now().toString());
	}

	public static ServerEvent death(String player, String message) {
		return new ServerEvent("death", player, message, Instant.now().toString());
	}

	public static ServerEvent game(String content) {
		return new ServerEvent("game", null, content, Instant.now().toString());
	}
}
