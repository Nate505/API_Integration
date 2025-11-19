import java.io.IOException;
import java.util.List;

import model.PopularityBasedStrategy;
import model.Track;
import model.SpotifyAPIClient;
import model.ArtistSimilarityStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Test class for ArtistSimilarityStrategy
class RecommenderTest {

    private ArtistSimilarityStrategy artiststrategy;
    private PopularityBasedStrategy popularityStrategy;
    private SpotifyAPIClient client;
    private Track seedTrack;

    @BeforeEach
    void setUp() {
        artiststrategy = new ArtistSimilarityStrategy();
        popularityStrategy = new PopularityBasedStrategy();

        client = new SpotifyAPIClient();

        seedTrack = new Track("6hWYYkgRr52crvRPi3U8kg", "Odo", List.of("Ado"), "Kyogen");
    }

    @Test
    void testExcludesSeedTrack() throws IOException {
        List<Track> recommendations = artiststrategy.recommend(seedTrack, client, 5);

        boolean containsSeed = recommendations.stream().anyMatch(track -> track.getId().equals(seedTrack.getId()));

        assertFalse(containsSeed, "Recommendations should not include the seed track");
    }

    @Test
    void testStrategyName() {
        boolean correctName = artiststrategy.getStrategyName().equals("Artist's Top Tracks");

        assertTrue(correctName, "Strategy name is correct");
    }

    @Test
    void testArtistID() throws IOException {
        boolean wrongID = client.getArtistID("Ado").equals(seedTrack.getId());

        assertFalse(wrongID, "Artist ID is wrong");
    }

    @Test
    void testPopularityRecs() throws IOException {
        List<Track> recommendations = popularityStrategy.recommend(seedTrack, client, 5);

        assertFalse(recommendations.isEmpty(), "Recommendations list should not be empty");
    }

    @Test
    void testArtistRecs() throws IOException {
        List<Track> recommendations = artiststrategy.recommend(seedTrack, client, 5);

        assertFalse(recommendations.isEmpty(), "Recommendations list should not be empty");
    }

}
