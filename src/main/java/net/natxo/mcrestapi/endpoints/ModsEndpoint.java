package net.natxo.mcrestapi.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModsEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		List<Map<String, Object>> mods = new ArrayList<>();

		for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
			ModMetadata meta = container.getMetadata();
			mods.add(buildMod(meta));
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", mods.size());
		response.put("mods", mods);

		HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
	}

	private Map<String, Object> buildMod(ModMetadata meta) {
		Map<String, Object> mod = new LinkedHashMap<>();
		mod.put("id", meta.getId());
		mod.put("name", meta.getName());
		mod.put("version", meta.getVersion().getFriendlyString());
		mod.put("description", meta.getDescription());

		List<String> authors = new ArrayList<>();
		meta.getAuthors().forEach(person -> authors.add(person.getName()));
		mod.put("authors", authors);

		List<String> license = new ArrayList<>(meta.getLicense());
		mod.put("license", license);

		mod.put("environment", meta.getEnvironment().name());

		ContactInformation contact = meta.getContact();
		Map<String, String> contactMap = new LinkedHashMap<>();
		contact.get("homepage").ifPresent(v -> contactMap.put("homepage", v));
		contact.get("sources").ifPresent(v -> contactMap.put("sources", v));
		contact.get("issues").ifPresent(v -> contactMap.put("issues", v));
		if (!contactMap.isEmpty()) {
			mod.put("contact", contactMap);
		}

		return mod;
	}
}
