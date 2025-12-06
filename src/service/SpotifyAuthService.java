package service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.Config;
import model.SpotifyAPIClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpotifyAuthService {
    private String accessToken;
    private String refreshToken;
    private long tokenExpirationTime;

    public String getAccessToken() {
        System.out.println("Access TOKEN:" + accessToken);
        return accessToken;
    }

    public boolean isLoggedIn(){
        return accessToken != null && System.currentTimeMillis() < tokenExpirationTime;
    }

    public void login() throws Exception{
        String state = UUID.randomUUID().toString();

        String scope = "user-read-playback-state user-modify-playback-state";
        scope = scope.replace(" ", "%20");

        String url = Config.AUTH_URL + "?client_id=" + Config.CLIENT_ID + "&response_type=code" + "&redirect_uri=" + URLEncoder.encode(Config.REDIRECT_URI, "UTF-8") + "&scope=" + scope + "&state=" + state;

        java.awt.Desktop.getDesktop().browse(new URI(url));

        startCallbackServer();
    }

    private void startCallbackServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(5050), 0);

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();

            Map<String, String> params = parseQuery(query);
            String code = params.get("code");

            String html = "<h1>Login successful. You can return to the app.</h1>";
            exchange.sendResponseHeaders(200, html.length());
            exchange.getResponseBody().write(html.getBytes());
            exchange.close();

            exchangeCodeForToken(code);


            server.stop(0);
        });

        server.start();
    }

    private void exchangeCodeForToken(String code) throws IOException {
        System.out.println("exchangeCodeForToken() CALLED with: " + code);

        HttpPost post = new HttpPost(Config.TOKEN_URL);

        String body = "grant_type=authorization_code" + "&code=" + code + "&redirect_uri=" + URLEncoder.encode(Config.REDIRECT_URI, "UTF-8");

        post.setEntity(new StringEntity(body));
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((Config.CLIENT_ID + ":" + Config.CLIENT_SECRET).getBytes()));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            try{
                String json = EntityUtils.toString(response.getEntity());
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                accessToken = obj.get("access_token").getAsString();
                refreshToken = obj.get("refresh_token").getAsString();
                int expiresIn = obj.get("expires_in").getAsInt();

                tokenExpirationTime = System.currentTimeMillis() + expiresIn * 1000L;

                System.out.println("Logged in! Token stored.");

            }catch (IOException | ParseException e){
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> parseQuery(String q) {
        return Arrays.stream(q.split("&")).map(s -> s.split("=")).collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }
}
