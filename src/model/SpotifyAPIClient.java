package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.Config;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST API Client for Spotify Web API
 * Demonstrates REST API calls and JSON parsing
 */
public class SpotifyAPIClient {
    private String accessToken;
    private String refreshToken;
    private long tokenExpirationTime;
    private final CloseableHttpClient httpClient;
    private HashMap <String, String> deviceName = new HashMap<>();

    public SpotifyAPIClient() {
        this.httpClient = HttpClients.createDefault();
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setTokenExpirationTime(long tokenExpirationTime) {
        this.tokenExpirationTime = tokenExpirationTime;
    }

    /**
     * Authenticate with Spotify API using Client Credentials Flow
     */
    public void authenticate() throws IOException {
        String auth = Config.CLIENT_ID + ":" + Config.CLIENT_SECRET;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpPost httpPost = new HttpPost(Config.TOKEN_URL);
        httpPost.setHeader("Authorization", "Basic " + encodedAuth);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setEntity(new StringEntity("grant_type=client_credentials"));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            try{
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                this.accessToken = jsonObject.get("access_token").getAsString();
                int expiresIn = jsonObject.get("expires_in").getAsInt();
                this.tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000);

                System.out.println("Successfully authenticated with Spotify API");
            } catch (ParseException e){
                throw new IOException("Failed to parse JSON response", e);
            }

        }
    }

    private void ensureValidToken() throws IOException {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpirationTime) {
            authenticate();
        }
    }

    /**
     * Search for tracks by query string
     */
    public List<Track> searchTracks(String query, int limit) throws IOException {
        ensureValidToken();

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s/search?q=%s&type=track&limit=%d",
                Config.API_BASE_URL, encodedQuery, limit);

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            try{
                String jsonResponse = EntityUtils.toString(response.getEntity());
                return parseTracksFromSearchResponse(jsonResponse);
            } catch (ParseException e){
                throw new IOException("Failed to parse JSON response", e);
            }

        }
    }

    /**
     * Search for Artist's top tracks
     */
    public List<Track> getArtistsTopTracks(String artistID) throws IOException {
        ensureValidToken();

        List<Track> tracks = new ArrayList<>();

        String url = String.format("%s/artists/%s/top-tracks?market=US", Config.API_BASE_URL, artistID);

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            try {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

                if (!root.has("tracks")) {
                    return tracks;
                }

                JsonArray items = root.getAsJsonArray("tracks");

                for (JsonElement element : items) {
                    JsonObject item = element.getAsJsonObject();
                    Track track = parseTrackFromJson(item);
                    tracks.add(track);
                }

                return tracks;

            } catch (ParseException e) {
                throw new IOException("Failed to parse JSON response", e);
            }
        }
    }

    public String getArtistID(String artist) throws IOException {
        ensureValidToken();

        String encodedQuery = URLEncoder.encode(artist, StandardCharsets.UTF_8);
        String url = String.format("%s/search?q=%s&type=artist&limit=%d",
                Config.API_BASE_URL, encodedQuery, Config.TRACK_POOL_SIZE);

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            try{
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

                if (!root.has("artists")) {
                    return "No Artist Found";
                }

                JsonObject item = root.getAsJsonObject("artists").getAsJsonArray("items").get(0).getAsJsonObject();

                return item.get("id").getAsString();

            } catch (ParseException e){
                throw new IOException("Failed to parse JSON response", e);
            }

        }
    }

    public String getDeviceID(String name) {
        return deviceName.get(name);
    }

    public String[] getDeviceName(String token) throws IOException {
        ensureValidToken();

        String url = String.format("%s/me/player/devices", Config.API_BASE_URL);

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + token);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            try{
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

                JsonArray devices = root.getAsJsonArray("devices");
                int deviceSize = devices.size();

                String[] deviceNames = new String[deviceSize];

                for(int i = 0; i < deviceSize; i++) {
                    JsonObject device = devices.get(i).getAsJsonObject();
                    deviceNames[i] = device.get("name").getAsString();
                    deviceName.put(deviceNames[i], device.get("id").getAsString());
                }

                return deviceNames;

            } catch (ParseException e){
                throw new IOException("Failed to parse JSON response", e);
            }

        }
    }

    public void playSong(String deviceID, String songID, String token) throws IOException {
        ensureValidToken();

        String trackUri = "spotify:track:" + songID;

        String url = Config.API_BASE_URL + "/me/player/play?device_id=" + deviceID;

        HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader("Authorization", "Bearer " + token);
        httpPut.setHeader("Content-Type", "application/json");

        String jsonBody = "{ \"uris\": [\"" + trackUri + "\"], \"position_ms\": 0 }";
        httpPut.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            int status = response.getCode();
            System.out.println("Status: " + status);
        }
    }

    public void pauseSong(String token) throws IOException {
        ensureValidToken();

        String url = String.format("%s/me/player/pause", Config.API_BASE_URL);

        HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader("Authorization", "Bearer " + token);

        String jsonBody = "";
        httpPut.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            int status = response.getCode();
            System.out.println("Status: " + status);
        }
    }


    /**
     * Parse Track objects from JSON search response
     */
    private List<Track> parseTracksFromSearchResponse(String jsonResponse) {
        List<Track> tracks = new ArrayList<>();
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

        if (!root.has("tracks")) {
            return tracks;
        }

        JsonArray items = root.getAsJsonObject("tracks").getAsJsonArray("items");

        for (JsonElement element : items) {
            JsonObject item = element.getAsJsonObject();
            Track track = parseTrackFromJson(item);
            tracks.add(track);
        }

        return tracks;
    }

    private Track parseTrackFromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();

        List<String> artists = new ArrayList<>();
        JsonArray artistsArray = json.getAsJsonArray("artists");
        for (JsonElement artistElement : artistsArray) {
            artists.add(artistElement.getAsJsonObject().get("name").getAsString());
        }

        String albumName = json.getAsJsonObject("album").get("name").getAsString();

        Track track = new Track(id, name, artists, albumName);
        track.setDurationMs(json.get("duration_ms").getAsInt());
        track.setPopularity(json.get("popularity").getAsInt());

        if (json.has("preview_url") && !json.get("preview_url").isJsonNull()) {
            track.setPreviewUrl(json.get("preview_url").getAsString());
        }

        return track;
    }


    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}