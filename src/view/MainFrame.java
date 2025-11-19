package view;


import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class MainFrame {
    private final Stage stage;
    private final RecommendationPanel recommendationPanel;


    public MainFrame(Stage stage, RecommendationPanel recommendationPanel) {
        this.stage = stage;
        this.recommendationPanel = recommendationPanel;
    }


    public void init() {
        BorderPane root = new BorderPane();
        root.setCenter(recommendationPanel.getRoot());


        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("Music Recommender");
        stage.setScene(scene);
        stage.show();
    }
}