/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import nl.utwente.cs.caes.tactile.control.TactilePane;
import nl.utwente.cs.caes.tactile.event.TactilePaneEvent;

/**
 *
 * @author Richard
 */
public class Uitproberen3 extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        TactilePane root = new TactilePane();
        //root.setPrefSize(600, 400);
        root.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        root.setBordersCollide(true);
        
        Circle c1 = new Circle(50);
        Circle c2 = new Circle(50);
        Circle c3 = new Circle(50);
        Rectangle r1 = new Rectangle(70, 120);
        c1.relocate(0, 80);
        c2.relocate(200, 140);
        c3.relocate(140, 0);
        r1.relocate(240, 300);
        
        Label label = new Label("Niet Draggable");
        TactilePane.setDraggable(label, false);
        
        root.getChildren().addAll(c1, c2, c3, r1, label);
        root.register(c1, c2, c3, r1, label);
        
        root.addEventFilter(TactilePaneEvent.ANY, event -> {
            System.out.println(event.getEventType());
        });
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
}
