package config;

public class Config {
    // Replace with your Spotify credentials
    public static final String CLIENT_ID = "89de062ff3aa4c1ab0f3832412fab056";
    public static final String CLIENT_SECRET = "d6dd0c9e500044b2b602fd30ebc4279c";

    // API Endpoints
    public static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    public static final String API_BASE_URL = "https://api.spotify.com/v1";

    // Server Configuration
    public static final int SERVER_PORT = 5050;
    public static final String SERVER_HOST = "https://127.0.0.1";

    // Recommendation Settings
    public static final int TRACK_POOL_SIZE = 50; // Tracks to analyze for recommendations
    public static final int DEFAULT_RECOMMENDATIONS = 10;
}
