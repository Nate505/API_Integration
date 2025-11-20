import javafx.application.Application;
import javafx.stage.Stage;
import model.SpotifyAPIClient;
import model.RecommendationEngine;
import model.RecommendationStrategy;
import model.PopularityBasedStrategy;
import service.APIService;
import view.MainFrame;
import view.RecommendationPanel;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            SpotifyAPIClient spotifyClient = new SpotifyAPIClient();

            RecommendationStrategy defaultStrategy = new PopularityBasedStrategy();

            RecommendationEngine recommendationEngine = new RecommendationEngine(defaultStrategy, spotifyClient);

            APIService apiService = new APIService(spotifyClient, recommendationEngine);

            RecommendationPanel recommendationPanel = new RecommendationPanel(apiService);

            MainFrame mainFrame = new MainFrame(stage, recommendationPanel);
            mainFrame.init();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
