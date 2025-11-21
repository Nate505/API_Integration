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

        seedTrack = new Track("37bNBNB332HXbSy6079cws", "Odo", List.of("Ado"), "Kyougen");
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

    @Test
    void testSearchTracks() throws IOException {
        List<Track> song = client.searchTracks("Odo - Ado", 50);

        boolean sameSong = seedTrack.toString().equals(song.get(0).toString());

        assertTrue(sameSong, "Tracks should be same");
    }

    @Test
    void testTopTracksSearch() throws IOException {
        String artistID = client .getArtistID("Ado");
        List<Track> song = client.getArtistsTopTracks(artistID);
        boolean sameArtist = true;

        for(Track track : song) {
            if(!track.getArtists().toString().contains(seedTrack.getArtists().toString())) {
                sameArtist = false;
                if(track.getArtists().get(0).toString().equals("Imagine Dragons") && track.getArtists().get(1).toString().equals(seedTrack.getArtists().get(0).toString())) {
                    sameArtist = true;
                }
            }
        }

        assertTrue(sameArtist, "Top tracks should be same");
    }

    @Test
    void testSearchID() throws IOException {
        List<Track> song = client.searchTracks("Odo - Ado", 50);
        boolean sameID = song.get(0).getId().toString().equals(seedTrack.getId().toString());

        assertTrue(sameID, "Track ID should be same");
    }

    @Test
    void testGetArtistID() throws IOException {
        List<Track> song = client.searchTracks("Odo - Ado", 50);
        String artist = song.get(0).getArtists().get(0).toString();
        String artistID = client.getArtistID(artist);

        String correctArtistID = client.getArtistID(seedTrack.getArtists().get(0).toString());

        assertTrue(artistID.equals(correctArtistID));
    }

    @Test
    void testToStringTracks() {
        boolean correntToString = seedTrack.toString().equals("Ado - Odo (Kyougen)");

        assertTrue(correntToString, "Tracks should be same");
    }
}
