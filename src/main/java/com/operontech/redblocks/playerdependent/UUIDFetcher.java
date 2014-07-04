package com.operontech.redblocks.playerdependent;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ImmutableList;

public class UUIDFetcher implements Callable<Map<String, UUID>> {
	private static final double PROFILES_PER_REQUEST = 100;
	private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
	private final JSONParser jsonParser = new JSONParser();
	private final List<String> names;
	private final boolean rateLimiting;

	public UUIDFetcher(final List<String> names, final boolean rateLimiting) {
		this.names = ImmutableList.copyOf(names);
		this.rateLimiting = rateLimiting;
	}

	public UUIDFetcher(final List<String> names) {
		this(names, true);
	}

	@Override
	public Map<String, UUID> call() {
		try {
			final Map<String, UUID> uuidMap = new HashMap<String, UUID>();
			final int requests = (int) Math.ceil(names.size() / PROFILES_PER_REQUEST);
			for (int i = 0; i < requests; i++) {
				final HttpURLConnection connection = createConnection();
				writeBody(connection, JSONArray.toJSONString(names.subList(i * 100, Math.min((i + 1) * 100, names.size()))));
				final JSONArray array = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
				JSONObject jsonProfile;
				for (final Object profile : array) {
					jsonProfile = (JSONObject) profile;
					uuidMap.put((String) jsonProfile.get("name"), UUIDFetcher.getUUID((String) jsonProfile.get("id")));
				}
				if (rateLimiting && (i != (requests - 1))) {
					Thread.sleep(100L);
				}
			}
			return uuidMap;
		} catch (final Exception e) {
			return null;
		}
	}

	private static void writeBody(final HttpURLConnection connection, final String body) throws Exception {
		final OutputStream stream = connection.getOutputStream();
		stream.write(body.getBytes());
		stream.flush();
		stream.close();
	}

	private static HttpURLConnection createConnection() throws Exception {
		final HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL).openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		return connection;
	}

	private static UUID getUUID(final String id) {
		return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
	}

	public static byte[] toBytes(final UUID uuid) {
		return ByteBuffer.wrap(new byte[16]).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
	}

	public static UUID fromBytes(final byte[] array) {
		if (array.length != 16) {
			throw new IllegalArgumentException("Illegal byte array length: " + array.length);
		}
		final ByteBuffer byteBuffer = ByteBuffer.wrap(array);
		final long mostSignificant = byteBuffer.getLong();
		final long leastSignificant = byteBuffer.getLong();
		return new UUID(mostSignificant, leastSignificant);
	}

	public static UUID getUUIDOf(final String name) {
		return new UUIDFetcher(Arrays.asList(name)).call().get(name);
	}
}