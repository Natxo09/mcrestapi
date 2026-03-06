package net.natxo.mcrestapi.security;

import java.time.Instant;
import java.util.List;

public class ApiKey {

	private String id;
	private String name;
	private String hash;
	private List<String> permissions;
	private String createdAt;

	public ApiKey() {
	}

	public ApiKey(String id, String name, String hash, List<String> permissions) {
		this.id = id;
		this.name = name;
		this.hash = hash;
		this.permissions = permissions;
		this.createdAt = Instant.now().toString();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHash() {
		return hash;
	}

	public List<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public boolean hasPermission(String permission) {
		return permissions.contains("*") || permissions.contains(permission);
	}
}
