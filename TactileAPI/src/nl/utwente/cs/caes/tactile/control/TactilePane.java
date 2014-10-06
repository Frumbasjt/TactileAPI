package nl.utwente.cs.caes.tactile.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import nl.utwente.cs.caes.tactile.skin.TactilePaneSkin;

@DefaultProperty("children")
public class TactilePane extends Control {
    // Keys for Attached Properties
    static final String IN_USE = "tactile-pane-in-use";
    static final String ANCHOR = "tactile-pane-anchor";
    static final String ANCHOR_OFFSET = "tactile-pane-anchor-offset";
    static final String VECTOR = "tactile-pane-vector";
    static final String GO_TO_FOREGROUND_ON_CONTACT = "tactile-pane-go-to-foreground-on-contact";
    static final String DRAGGABLE = "tactile-pane-draggable";
    static final String SLIDE_ON_RELEASE = "tactile-pane-slide-on-release";
    static final String NODES_COLLIDING = "tactile-pane-nodes-colliding";
    static final String NODES_PROXIMITY = "tactile-pane-nodes-proximity";
    static final String TRACKER = "tactile-pane-tracker";
    static final String ON_PROXIMITY_ENTERED = "tactile-pane-on-proximity-entered";
    static final String ON_PROXIMITY_LEFT = "tactile-pane-on-proximity-left";
    static final String ON_IN_PROXIMITY = "tactile-pane-on-in-proximity";
    static final String ON_AREA_ENTERED = "tactile-pane-on-area-entered";
    static final String ON_AREA_LEFT = "tactile-pane-on-area-left";
    static final String ON_IN_AREA = "tactile-pane-on-in-area";
    
    // Attached Properties that are only used privately
    static final String MOUSE_EVENT_FILTER = "tactile-pane-mouse-event-filter";
    static final String TOUCH_EVENT_HANDLER = "tactile-pane-touch-event-handler";
    static final String MOUSE_EVENT_HANDLER = "tactile-pane-mouse-event-handler";
    
    // IDs to keep track which finger/cursor started dragging a Node
    static final int NULL_ID = -1;
    static final int MOUSE_ID = -2;
    
    // STATIC METHODS
    
    static void setInUse(Node node, boolean inUse) {
        inUsePropertyImpl(node).set(inUse);
    }
    
    public static boolean isInUse(Node node) {
        return inUsePropertyImpl(node).get();
    }
    
    static BooleanProperty inUsePropertyImpl(Node node) {
        BooleanProperty property = (BooleanProperty) getConstraint(node, IN_USE);
        if (property == null) {
            property = new SimpleBooleanProperty(false);
            setConstraint(node, IN_USE, property);
        }
        return property;
    }
    
    /**
     * Whether this {@code Node} is being dragged by the user. If the {@code Node}
     * is not a child of a {@code TactilePane}, it will always return {@code false}.
     */
    public static ReadOnlyBooleanProperty inUseProperty(Node node) {
        return inUsePropertyImpl(node);
    }
    
    public static void setAnchor(Node node, Node anchor) {
        anchorProperty(node).set(anchor);
    }
    
    public static Node getAnchor(Node node) {
        return anchorProperty(node).get();
    }
    
    /**
     * The {@code Node} the given {@code node} is anchored to. When a 
     * {@code Node} is anchored to another {@code Node} (called {@code anchor}), it will move
     * wherever the {@code anchor} moves to, provided that both the {@code node}
     * and the {@code anchor} have the same {@code TactilePane} as ancestor. The actual
     * position the anchored {@code Node} will move to is the sum of the position of the
     * {@code anchor} and the {@code anchorOffset}.
     * 
     * When anchored, the {@code node} will not respond to physics. It will however 
     * still respond to user input. When a user tries to drag an anchored {@code Node}, its {@anchor}
     * will automatically be set to {@code null}.
     */
    public static ObjectProperty<Node> anchorProperty(Node node) {
        ObjectProperty<Node> property = (ObjectProperty<Node>) getConstraint(node, ANCHOR);
        if (property == null) {
            property = new SimpleObjectProperty<>(null);
            setConstraint(node, ANCHOR, property);
        }
        return property;
    }
    
    public static void setAnchorOffset(Node node, Point2D offset) {
        anchorOffsetProperty(node).set(offset);
    }
    
    public static Point2D getAnchorOffset(Node node) {
        return anchorOffsetProperty(node).get();
    }
    
    /**
     * Defines the position of this {@code node} relative to its {@anchor}, if it
     * has one.
     */
    public static ObjectProperty<Point2D> anchorOffsetProperty(Node node) {
        ObjectProperty<Point2D> property = (ObjectProperty<Point2D>) getConstraint(node, ANCHOR_OFFSET);
        if (property == null) {
            property = new SimpleObjectProperty<>(Point2D.ZERO);
            setConstraint(node, ANCHOR_OFFSET, property);
        }
        return property;
    }
    
    public static void setVector(Node node, Point2D vector) {
        vectorProperty(node).set(vector);
    }
    
    public static Point2D getVector(Node node) {
        return vectorProperty(node).get();
    }
    
    /**
     * The 2D velocity vector for this {@code node}. Primarily intended for physics.
     */
    public static ObjectProperty<Point2D> vectorProperty(Node node) {
        ObjectProperty<Point2D> property = (ObjectProperty<Point2D>) getConstraint(node, VECTOR);
        if (property == null) {
            property = new SimpleObjectProperty<>(Point2D.ZERO);
            setConstraint(node, VECTOR, property);
        }
        return property;
    }
    
    public static void setGoToForegroundOnContact(Node node, boolean goToForegroundOnContact) {
        goToForegroundOnContactProperty(node).set(goToForegroundOnContact);
    }
    
    public static boolean isGoToForegroundOnContact(Node node) {
        return goToForegroundOnContactProperty(node).get();
    }
    
    /**
     * Whether this {@code node} will go to the foreground when the user starts
     * a drag gesture with it.
     * 
     * @defaultvalue true
     */
    public static BooleanProperty goToForegroundOnContactProperty(Node node) {
        BooleanProperty property = (BooleanProperty) getConstraint(node, GO_TO_FOREGROUND_ON_CONTACT);
        if (property == null) {
            property = new SimpleBooleanProperty(true);
            setConstraint(node, GO_TO_FOREGROUND_ON_CONTACT, property);
        }
        return property;
    }
    
    public static void setDraggable(Node node, boolean draggable) {
        draggableProperty(node).set(draggable);
    }
    
    public static boolean isDraggable(Node node) {
        return draggableProperty(node).get();
    }
    
    /**
     * Whether the given node can be dragged by the user. Only nodes that are a direct child of
     * a {@code TactilePane} can be dragged.
     * 
     * @defaultvalue true
     */
    public static BooleanProperty draggableProperty(Node node) {
        BooleanProperty property = (BooleanProperty) getConstraint(node, DRAGGABLE);
        if (property == null) {
            property = new SimpleBooleanProperty(true) {
                @Override
                public void set(boolean draggable) {
                    if (!draggable) {
                        // A node that is not draggable cannot be in use
                        setInUse(node, false);
                    }
                    super.set(draggable);
                }
            };
            setConstraint(node, DRAGGABLE, property);
        }
        return property;
    }
    
    public static void setSlideOnRelease(Node node, boolean slideOnRelease) {
        slideOnReleaseProperty(node).set(slideOnRelease);
    }
    
    public static boolean isSlideOnRelease(Node node) {
        return slideOnReleaseProperty(node).get();
    }
    
    /**
     * Whether the given {@code Node} will get a vector in the direction it was moving
     * when the user stops dragging that {@code Node}
     * 
     * @defaultvalue false
     */
    public static BooleanProperty slideOnReleaseProperty(Node node) {
        BooleanProperty property = (BooleanProperty) getConstraint(node, SLIDE_ON_RELEASE);
        if (property == null) {
            property = new SimpleBooleanProperty(false);
            setConstraint(node, SLIDE_ON_RELEASE, property);
        }
        return property;
    }
    
    /**
     * Returns the set of {@code Nodes} that are registered to the same {@code TactilePane}
     * as the given {@code node}, and are currently colliding with that {@code node}
     */
    public static ObservableSet<Node> getNodesColliding(Node node) {
        ObservableSet<Node> result = (ObservableSet<Node>) getConstraint(node, NODES_COLLIDING);
        if (result == null) {
            result = FXCollections.observableSet(new HashSet<Node>());
            setConstraint(node, NODES_COLLIDING, result);
        }
        return result;
    }
    
    /**
     * Returns the set of {@code Nodes} that are registered to the same {@code TactilePane}
     * as the given {@code node}, and are currently in the proximity of that {@code node}
     */
    public static ObservableSet<Node> getNodesInProximity(Node node) {
        ObservableSet<Node> result = (ObservableSet<Node>) getConstraint(node, NODES_PROXIMITY);
        if (result == null) {
            result = FXCollections.observableSet(new HashSet<Node>());
            setConstraint(node, NODES_PROXIMITY, result);
        }
        return result;
    }
    
    /**
     * Calls {@code register} on the given TactilePane with {@code node} as argument.
     * If {@code tactilePane} is {@code null}, {@code node} will be deregistered
     * at its previous {@code TactilePane}, if one exists.
     */
    public static void setTracker(Node node, TactilePane tactilePane) {
        if (tactilePane == null) {
            TactilePane oldPane = getTracker(node);
            if (oldPane != null) {
                oldPane.deregister(node);
            }
        } else {
            tactilePane.register(node);
        }
    }
    
    /**
     * The {@code TactilePane} where {@code node} is currently registered.
     */
    public static TactilePane getTracker(Node node) {
        return (TactilePane) getConstraint(node, TRACKER);
    }
    
    // Used to attach a Property to a Node
    static void setConstraint(Node node, Object key, Object value) {
        if (value == null) {
            node.getProperties().remove(key);
        } else {
            node.getProperties().put(key, value);
        }
        if (node.getParent() != null) {
            node.getParent().requestLayout();
        }
    }

    static Object getConstraint(Node node, Object key) {
        if (node.hasProperties()) {
            Object value = node.getProperties().get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
    
    // INSTANCE VARIABLES
    private Physics physics;
    
    // CONSTRUCTORS
    
    public TactilePane() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // Since this Control is more or less a Pane, focusTraversable should be false by default
        ((StyleableProperty<Boolean>)focusTraversableProperty()).applyStyle(null, false);
        
        getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
            c.next();
            for (Node node: c.getRemoved()) {
                removeDragEventHandlers(node);
            }
            for (Node node: c.getAddedSubList()) {
                addDragEventHandlers(node);
            }
        });
        
        physics = new Physics(this);
        physics.start();
    }
    
    public TactilePane(Node... children) {
        this();
        getChildren().addAll(children);
    }
    
    // MAKING CHILDREN DRAGGABLE
    
    // Help class used for dragging Nodes
    private class DragContext {
        final Node draggable;   // The Node that is dragged around
        double localX, localY;  // The x,y position of the Event in the Node
        int touchId;            // The id of the finger/cursor that is currently dragging the Node
        
        public DragContext(Node draggable) {
            this.draggable = draggable;
            touchId = -1;
        }
        
        @Override
        public String toString() {
            return String.format("DragContext [draggable = %s, ,touchId = %d, localX = %d, localY = %d]", draggable.toString(), touchId, localX, localY);
        }
    }
    
    private void addDragEventHandlers(Node node) {
        final DragContext dragContext = new DragContext(node);
        
        EventHandler<MouseEvent> mouseFilter = event -> {
            if (isDraggable(node) && event.isSynthesized() && event.getTarget() == node) {
                event.consume();
            }
        };
        
        EventHandler<TouchEvent> touchHandler = event -> {
            EventType type = event.getEventType();
            
            if (type == TouchEvent.TOUCH_PRESSED) {
                if (dragContext.touchId == NULL_ID) {
                    dragContext.touchId = event.getTouchPoint().getId();
                    handleTouchPressed(dragContext, event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY());
                }
            } else if (type == TouchEvent.TOUCH_MOVED) {
                if (dragContext.touchId == event.getTouchPoint().getId()) {
                    handleTouchMoved(dragContext, event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY());
                }
            } else if (type == TouchEvent.TOUCH_RELEASED) {
                if (dragContext.touchId == event.getTouchPoint().getId()) {
                    handleTouchReleased(dragContext);
                    dragContext.touchId = NULL_ID;
                }
            } else return;
            
            event.consume();
        };
        
        EventHandler<MouseEvent> mouseHandler = event -> {
            EventType type = event.getEventType();
            
            if (type == MouseEvent.MOUSE_PRESSED) {
                if (dragContext.touchId == NULL_ID) {
                    dragContext.touchId = MOUSE_ID;
                    handleTouchPressed(dragContext, event.getSceneX(), event.getSceneY());
                }
            } else if (type == MouseEvent.MOUSE_DRAGGED) {
                
                if (dragContext.touchId == MOUSE_ID) {
                    handleTouchMoved(dragContext, event.getSceneX(), event.getSceneY());
                }
            } else if (type == MouseEvent.MOUSE_RELEASED) {
                if (dragContext.touchId == MOUSE_ID) {
                    handleTouchReleased(dragContext);
                    dragContext.touchId = NULL_ID;
                }
            } else return;
            event.consume();
        };
        
        setConstraint(node, MOUSE_EVENT_FILTER, mouseFilter);
        setConstraint(node, TOUCH_EVENT_HANDLER, touchHandler);
        setConstraint(node, MOUSE_EVENT_HANDLER, mouseHandler);
        
        node.addEventFilter(MouseEvent.ANY, mouseFilter);
        node.addEventHandler(TouchEvent.ANY, touchHandler);
        node.addEventHandler(MouseEvent.ANY, mouseHandler);
    }
    
    private void removeDragEventHandlers(Node node) {
        EventHandler<MouseEvent> mouseFilter = (EventHandler<MouseEvent>) getConstraint(node, MOUSE_EVENT_FILTER);
        EventHandler<TouchEvent> touchHandler = (EventHandler<TouchEvent>) getConstraint(node, TOUCH_EVENT_HANDLER);
        EventHandler<MouseEvent> mouseHandler = (EventHandler<MouseEvent>) getConstraint(node, MOUSE_EVENT_HANDLER);
        
        node.removeEventFilter(MouseEvent.ANY, mouseFilter);
        node.removeEventHandler(TouchEvent.ANY, touchHandler);
        node.removeEventHandler(MouseEvent.ANY, mouseHandler);
        
        setConstraint(node, MOUSE_EVENT_FILTER, null);
        setConstraint(node, TOUCH_EVENT_HANDLER, null);
        setConstraint(node, MOUSE_EVENT_HANDLER, null);
    }
    
    private void handleTouchPressed(final DragContext dragContext, double sceneX, double sceneY) {
        Node node = dragContext.draggable;
        if (isDraggable(node)) {
            setAnchor(node, null);
            setInUse(node, true);
            setVector(node, Point2D.ZERO);
            
            Bounds nodeBounds = node.getBoundsInParent();
            
            dragContext.localX = sceneX - nodeBounds.getMinX();
            dragContext.localY = sceneY - nodeBounds.getMinY();

            if (isGoToForegroundOnContact(node)) {
                node.toFront();
            }
        }
    }

    private void handleTouchMoved(final DragContext dragContext, double sceneX, double sceneY) {
        Node node = dragContext.draggable;
        if (isDraggable(node) && getAnchor(node) == null) {

            double x = sceneX - dragContext.localX;
            double y = sceneY - dragContext.localY;

            if (isBordersCollide()) {
                Bounds paneBounds = this.getBoundsInLocal();
                Bounds nodeBounds = node.getBoundsInParent();

                if (x < paneBounds.getMinX()) {
                    x = paneBounds.getMinX();
                } else if (x + nodeBounds.getWidth() > paneBounds.getMaxX()) {
                    x = paneBounds.getMaxX() - nodeBounds.getWidth();
                }
                if (y < paneBounds.getMinY()) {
                    y = paneBounds.getMinY();
                } else if (y + nodeBounds.getHeight() > paneBounds.getMaxY()) {
                    y = paneBounds.getMaxY() - nodeBounds.getHeight();
                }
            }
            node.relocate(x, y);
        }
    }

    private void handleTouchReleased(final DragContext dragContext) {
        Node node = dragContext.draggable;
        setInUse(node, false);
    }
    
    
    // PROPERTIES
    
   /**
     *
     * @return modifiable list of children.
     */
    @Override public ObservableList<Node> getChildren() {
        return super.getChildren();
    }
    
    /**
     * Whether children will collide with the borders of this
     * {@code TactilePane}. If set to true the {@code TactilePane} will prevent
     * children that are moving because of user input or physics to
     * move outside of the {@code TactilePane's} boundaries.
     *
     * @defaultvalue false
     */
    private BooleanProperty bordersCollide;

    public final void setBordersCollide(boolean value) {
        bordersCollideProperty().set(value);
    }

    public final boolean isBordersCollide() {
        return bordersCollideProperty().get();
    }

    public final BooleanProperty bordersCollideProperty() {
        if (bordersCollide == null) {
            bordersCollide = new SimpleBooleanProperty(false);
        }
        return bordersCollide;
    }
    
    public final void setProximityThreshold(double threshold) {
        proximityThresholdProperty().set(threshold);
    }

    public final double getProximityThreshold() {
        return proximityThresholdProperty().get();
    }

    /**
     * Specifies how close two {@code Nodes} have to be to each other to be
     * considered in eachothers proximity. When set to 0, TactilePane won't fire
     * {@code PROXIMITY_ENTERED} or {@code IN_PROXIMITY} events at all.
     * {@code PROXIMITY_LEFT} events will still be fired for any pair of
     * {@code Nodes} that entered each other's proximity before the threshold
     * was set to 0. When set to a negative value, an IllegalArgumentException
     * is thrown.
     *
     * @defaultvalue 25.0
     */
    public final DoubleProperty proximityThresholdProperty() {
        return physics.getQuadTree().proximityThresholdProperty();
    }
    
    // INSTANCE METHODS
    
    public void register(Node... nodes) {
        for (Node node: nodes) {
            TactilePane oldPane = getTracker(node);
            if (oldPane != null) {
                oldPane.deregister(node);
            }
            physics.startTracking(node);
            setConstraint(node, TRACKER, this);
        }
    }
    
    public void deregister(Node... nodes) {
        for (Node node: nodes) {
            physics.stopTracking(node);
            setConstraint(node, TRACKER, null);
        }
    }
    
    // STYLESHEET HANDLING
    
    // The selector class
    private static String DEFAULT_STYLE_CLASS = "tactile-pane";
    // TODO PseudoClasses maken
    
    private static final class StyleableProperties {
        // TODO CSSMetaData maken voor properties

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
                final List<CssMetaData<? extends Styleable, ?>> styleables = 
                    new ArrayList<>(Control.getClassCssMetaData());

                STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
    
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Skin<TactilePane> createDefaultSkin() {
        return new TactilePaneSkin(this);
    }
}
