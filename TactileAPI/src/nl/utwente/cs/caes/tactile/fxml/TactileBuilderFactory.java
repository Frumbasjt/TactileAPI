package nl.utwente.cs.caes.tactile.fxml;

import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import nl.utwente.cs.caes.tactile.control.Anchor;

public class TactileBuilderFactory implements BuilderFactory {
    private final JavaFXBuilderFactory defaultBuilderFactory = new JavaFXBuilderFactory();
    
    @Override
    public Builder<?> getBuilder(Class<?> type) {
        if (type == Anchor.class) {
            return new TactilePaneAnchorBuilder();
        } else {
            return defaultBuilderFactory.getBuilder(type);
        }
    }
}
