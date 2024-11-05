package edu.curtin.saed.assignment1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application 
{
    public static void main(String[] args) 
    {
        launch();        
    }
    
    @Override
    public void start(Stage stage) 
    {
        stage.setTitle("Citadel Defense Game");
        JFXArena arena = new JFXArena();
        arena.addListener((x, y) ->
        {   
            System.out.println("Arena click at (" + x + "," + y + ")");
            if (x < 9 && y < 9) {
                // Place a wall at the clicked grid position
                arena.placeWall(x, y);
            }
        });
        
        ToolBar toolbar = new ToolBar();
        Label label = new Label("Score: 0");
        Label label2 = new Label("Queued-up wall-building: 0");
        arena.setLabel(label);
        arena.setLabel2(label2);
        toolbar.getItems().addAll(label, label2);
         
        TextArea logger = new TextArea();
        arena.setLogger(logger);
        
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(arena, logger);
        arena.setMinWidth(300.0);
        
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);
        
        Scene scene = new Scene(contentPane, 800, 800);
        stage.setScene(scene);
        stage.show();
    }
}
