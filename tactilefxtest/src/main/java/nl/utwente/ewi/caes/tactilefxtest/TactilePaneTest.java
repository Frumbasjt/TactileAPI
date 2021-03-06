package nl.utwente.ewi.caes.tactilefxtest;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import nl.utwente.ewi.caes.tactilefx.control.Bond;
import nl.utwente.ewi.caes.tactilefx.control.TactilePane;
import nl.utwente.ewi.caes.tactilefx.debug.DebugParent;

public class TactilePaneTest extends Application {
    static final int RECTANGLES = 2;
    static final int CIRCLES = 5;
    static final int WIDTH = 800;
    static final int HEIGHT = 600;
    
    DebugParent debug;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
    	BorderPane root = new BorderPane();
        
    	//Init TactilePane
        TactilePane tactilePane = new TactilePane();
        tactilePane.setPrefSize(WIDTH, HEIGHT);
        tactilePane.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        tactilePane.setBordersCollide(true);
        
        for (int i = 0; i < RECTANGLES; i++) {
            Rectangle rectangle = new Rectangle(80, 80);
            rectangle.relocate(Math.random() * (WIDTH - 80), Math.random() * (HEIGHT - 80));
            //TactilePane.setDraggable(rectangle, false);
            tactilePane.getChildren().add(rectangle);
        }
        for (int i = 0; i < CIRCLES; i++) {
            Circle circle = new Circle(50);
            circle.relocate(Math.random() * (WIDTH - 100), Math.random() * (HEIGHT - 100));
            TactilePane.setSlideOnRelease(circle, true);
            TactilePane.setOnInProximity(circle, event -> {
                if (!TactilePane.isInUse(circle) && event.getOther() instanceof Circle) {
                    TactilePane.moveAwayFrom(circle, event.getOther(), 20);
                }
            });
            TactilePane.setOnInArea(circle, event -> {
                if (!TactilePane.isInUse(circle) && event.getOther() instanceof Circle) {
                    TactilePane.moveAwayFrom(circle, event.getOther(), 50);
                }
            });
            TactilePane.setOnAreaEntered(circle, event -> {
                if (TactilePane.isInUse(circle) && event.getOther() instanceof Rectangle) {
                    TactilePane.getBonds(circle).add(new Bond(event.getOther(), 150, 0.5));
                }
            });
            tactilePane.getChildren().add(circle);
        }
        
        for (Node node: tactilePane.getChildren()) {
            tactilePane.getActiveNodes().add(node);
        }
        
        // Set proximity threshhold
        tactilePane.proximityThresholdProperty().set(75);
        
        // Init Control Pane
        FlowPane controlLayout = new FlowPane();
        CheckBox enableDebug = new CheckBox("Enable Debug Mode");
        enableDebug.setSelected(false);
        controlLayout.getChildren().add(enableDebug);
        
        // Debug layer
        debug = new DebugParent(tactilePane);
        debug.overlayVisibleProperty().bindBidirectional(enableDebug.selectedProperty());
        
        root.setCenter(debug);
        root.setBottom(controlLayout);
        
        // Key bindings
        debug.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
        	@Override
        	public void handle(KeyEvent keyEvent){
                if (keyEvent.getCode() == KeyCode.F11) {
                	if (primaryStage.isFullScreen()){
                		primaryStage.setFullScreen(false);
                	} else {
                		primaryStage.setFullScreen(true);
                	}
                	keyEvent.consume();
                }
            }
        	
        });
        
        Scene scene = new Scene(root);
        primaryStage.setFullScreen(false);
        primaryStage.setOnCloseRequest(event -> { Platform.exit(); });
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
}
