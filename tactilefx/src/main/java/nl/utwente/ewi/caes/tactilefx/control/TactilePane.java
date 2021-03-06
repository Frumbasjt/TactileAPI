package nl.utwente.ewi.caes.tactilefx.control;

import com.sun.javafx.css.converters.BooleanConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.PauseTransition;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.util.Duration;
import nl.utwente.ewi.caes.tactilefx.event.TactilePaneEvent;
import nl.utwente.ewi.caes.tactilefx.skin.TactilePaneSkin;

/**
 * <p>
 * A {@code Control} that allows a user to rearrange the position of its children. Acts
 * like a {@code Pane} in that it only resizes its children to its preferred
 * sizes, and also exposes its children list as public. On top of this however,
 * it allows users to layout the children by means of mouse and/or touch input.
 * It also has some basic "physics"-like features, such as collision detection,
 * inertia, gravity, etc.
 * <p>
 *
 * <h3>Dragging Nodes</h3>
 * By default, all of TactilePane's children are draggable, which means that a
 * user can drag them using mouse or touch input. This can be turned off by
 * setting the attached property {@link draggableProperty draggable} to
 * {@code false}. To prevent the user from dragging a node beyond the bounds of
 * the TactilePane, the {@link bordersCollideProperty borderCollide} property
 * can be set to {@code true}.
 * <p>
 * To implement dragging of Nodes, Mouse/Touch events are handled (and consumed)
 * at the draggable node. Note that this includes synthesized MouseEvents that
 * come along with TouchEvents. The first TouchEvent.PRESSED or
 * MouseEvent.PRESSED determines which set of events is used for dragging. This
 * ensures that having multiple touch points on a draggable node won't mess up
 * the drag operation. The other Mouse/Touch events are ignored and not
 * consumed, which means that even though TactilePane consumes the events used
 * to drag a Node, there can still be events that bubble up.
 * <p>
 * Calling {@link getDragContext getDragContext} will provide information
 * relevant to the dragging operation on a node, such as the id of the touch
 * point that is being used for dragging. In {@link DragContext DragContext},
 * it's possible to bind a drag operation to a new touch point, so that another
 * touch point can take the drag operation over.
 * <p>
 * The moment at which Mouse/Touch Events are handled to implement dragging can
 * be altered by setting the
 * {@link dragProcessingModeProperty dragProccesingMode}. This can be set so
 * that handling (and consuming) Mouse/Touch events happens during the filter or
 * the handler stage.
 * <p>
 * <h3>Active Nodes and Events</h3>
 * Apart from making Nodes draggable, TactilePane can also check if given Nodes
 * collide with each other. In order to achieve this, Nodes can be added to
 * {@link getActiveNodes getActiveNodes}. Every Node in this list will be
 * tracked by the TactilePane. When any pair of Nodes from activeNodes get close
 * to each other or collide, a TactilePaneEvent is fired.
 * <p>
 * 
 * <h3>Physics</h3>
 * The last main feature of TactilePane is the physics system. Nodes can be
 * given vectors which give it a force into a certain direction. This can for
 * instance be combined with the Active Node feature by making a Node A "flee"
 * from another Node B when B gets too close to A. See the code listing below:
 * <p>
 * <pre>
 * {@code
 *  TactilePane tp = new TactilePane();
 *  Rectangle r1 = new Rectangle(50, 50, 0, 0);
 *  Rectangle r2 = new Rectangle(50, 50, 100, 0);
 *  tp.getChildren().addAll(r1, r2);
 * 
 *  // Track r1 and r2 for collision/proximity
 *  tp.getActiveNodes().addAll(r1, r2);
 * 
 *  // When r1 and r2 are in eachothers proximity, move r1 away from r2
 *  TactilePane.setOnInProximity(r1, e -> {
 *      TactilePane.moveAwayFrom(r1, r2);
 *  });
 * }
 * </pre>
 * <p>
 * TactilePane can be setup so that Nodes will bounce off its borders when a
 * given force would otherwise result in a Node to be laid out outside of the
 * TactilePane's boundaries.
 *
 * Other features include things such as giving a Node A a 'bond' with another
 * Node B, so that that A will be given a vector such that it always attempts to
 * stay at a certain distance from B.
 * <p>
 */
@DefaultProperty("children")
public class TactilePane extends Control {
    // Attached Properties for Nodes
    static final String IN_USE = "tactile-pane-in-use";
    static final String ANCHOR = "tactile-pane-anchor";
    static final String VECTOR = "tactile-pane-vector";
    static final String GO_TO_FOREGROUND_ON_CONTACT = "tactile-pane-go-to-foreground-on-contact";
    static final String DRAGGABLE = "tactile-pane-draggable";
    static final String SLIDE_ON_RELEASE = "tactile-pane-slide-on-release";
    static final String NODES_COLLIDING = "tactile-pane-nodes-colliding";
    static final String NODES_PROXIMITY = "tactile-pane-nodes-proximity";
    static final String NODES_BOND = "tactile-pane-nodes-bond";
    static final String TRACKER = "tactile-pane-tracker";
    static final String ON_PROXIMITY_ENTERED = "tactile-pane-on-proximity-entered";
    static final String ON_PROXIMITY_LEFT = "tactile-pane-on-proximity-left";
    static final String ON_IN_PROXIMITY = "tactile-pane-on-in-proximity";
    static final String ON_AREA_ENTERED = "tactile-pane-on-area-entered";
    static final String ON_AREA_LEFT = "tactile-pane-on-area-left";
    static final String ON_IN_AREA = "tactile-pane-on-in-area";
    static final String DRAG_CONTEXT = "tactile-pane-drag-context";
    
    // Attached Properties for Nodes that are only used privately
    static final String TOUCH_EVENT_HANDLER = "tactile-pane-touch-event-handler";
    static final String MOUSE_EVENT_HANDLER = "tactile-pane-mouse-event-handler";
    static final String DIRTY = "tactile-pane-dirty";
    
    // ATTACHED PROPERTIES
    private static void setDragContext(Node node, DragContext dragContext) {
        setConstraint(node, DRAG_CONTEXT, dragContext);
    }
    
    public static DragContext getDragContext(Node node) {
        return (DragContext) getConstraint(node, DRAG_CONTEXT);
    }
    
    static void setInUse(Node node, boolean inUse) {
        inUsePropertyImpl(node).set(inUse);
    }
    
    /**
     * Gets the value of the property inUse
     */
    public static boolean isInUse(Node node) {
        return inUsePropertyImpl(node).get();
    }
    
    static BooleanProperty inUsePropertyImpl(Node node) {
        BooleanProperty property = (BooleanProperty) getConstraint(node, IN_USE);
        if (property == null) {
            property = new StyleableBooleanProperty(false) {

                @Override 
                public void invalidated() {
                    node.pseudoClassStateChanged(ATTACHED_IN_USE_STATE, get());
                }
                
                @Override
                public Object getBean() {
                    return null;
                }

                @Override
                public String getName() {
                    return "inUse";
                }

                @Override
                public CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
                    return StyleableProperties.Attached.IN_USE;
                }
            };
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
    
    /**
     * Sets the value of the property anchor
     */
    public static void setAnchor(Node node, Anchor anchor) {
        anchorProperty(node).set(anchor);
    }
    
    /**
     * Gets the value of the property anchor
     */
    public static Anchor getAnchor(Node node) {
        return anchorProperty(node).get();
    }
    
    /**
     * The anchor for a given node. When not <code>null</code>, this Node's
     * location will be bound to another Node.
     */
    public static ObjectProperty<Anchor> anchorProperty(Node node) {
        ObjectProperty<Anchor> property = (ObjectProperty<Anchor>) getConstraint(node, ANCHOR);
        if (property == null) {
            property = new SimpleObjectProperty<>(null);
            setConstraint(node, ANCHOR, property);
        }
        return property;
    }
    
    /**
     * Gets the value of the property vector
     */
    public static void setVector(Node node, Point2D vector) {
        vectorProperty(node).set(vector);
    }
    
    /**
     * Sets the value of the property vector
     */
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
    
    /**
     * Gets the value of the property goToForegroundOnContact
     */
    public static void setGoToForegroundOnContact(Node node, boolean goToForegroundOnContact) {
        goToForegroundOnContactProperty(node).set(goToForegroundOnContact);
    }
    
    /**
     * Sets the value of the property goToForegroundOnContact
     */
    public static boolean isGoToForegroundOnContact(Node node) {
        return goToForegroundOnContactProperty(node).get();
    }
    
    /**
     * Whether this {@code node} will go to the foreground when the user starts
     * a drag gesture with it.
     */
    public static BooleanProperty goToForegroundOnContactProperty(Node node) {
        BooleanProperty property = (BooleanProperty) getConstraint(node, GO_TO_FOREGROUND_ON_CONTACT);
        if (property == null) {
            property = new SimpleBooleanProperty(true);
            setConstraint(node, GO_TO_FOREGROUND_ON_CONTACT, property);
        }
        return property;
    }
    
    /**
     * Sets the value of the property draggable
     */
    public static void setDraggable(Node node, boolean draggable) {
        draggableProperty(node).set(draggable);
    }
    
    /**
     * Gets the value of the property draggable
     */
    public static boolean isDraggable(Node node) {
        return draggableProperty(node).get();
    }
    
    /**
     * Whether the given node can be dragged by the user. Only nodes that are a direct child of
     * a {@code TactilePane} can be dragged.
     */
    public static BooleanProperty draggableProperty(Node node) {
        BooleanProperty property = (BooleanProperty) getConstraint(node, DRAGGABLE);
        if (property == null) {
            property = new StyleableBooleanProperty(true) {
                @Override
                public void set(boolean draggable) {
                    if (!draggable) {
                        // A node that is not draggable cannot be in use
                        setInUse(node, false);
                    }
                    super.set(draggable);
                }

                @Override
                public void invalidated() {
                    node.pseudoClassStateChanged(ATTACHED_DRAGGABLE_STATE, get());
                }
                
                @Override
                public Object getBean() {
                    return null;
                }

                @Override
                public String getName() {
                    return "draggable";
                }

                @Override
                public CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
                    return StyleableProperties.Attached.DRAGGABLE;
                }
            };
            setConstraint(node, DRAGGABLE, property);
        }
        return property;
    }
    
    /**
     * Sets the value of the property setSlideOnRelease
     */
    public static void setSlideOnRelease(Node node, boolean slideOnRelease) {
        slideOnReleaseProperty(node).set(slideOnRelease);
    }
    
    /**
     * Gets the value of the property setSlideOnRelease
     */
    public static boolean isSlideOnRelease(Node node) {
        return slideOnReleaseProperty(node).get();
    }
    
    /**
     * Whether the given {@code Node} will get a vector in the direction it was
     * moving when the user stops dragging that {@code Node}
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
     * Returns the set of {@code Nodes} that are registered to the same
     * {@code TactilePane} as the given {@code node}, and are currently
     * colliding with that {@code node}
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
     * Returns the set of {@code Nodes} that are registered to the same
     * {@code TactilePane} as the given {@code node}, and are currently in the
     * proximity of that {@code node}
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
     * Returns the set of {@code Bonds} associated with the given {@code Node}. If the set
     * already contains a {@code Bond} with the same {@code bondNode}, the old {@code Bond}
     * is replaced.
     */
    public static ObservableSet<Bond> getBonds(Node node) {
	ObservableSet<Bond> result = (ObservableSet<Bond>) getConstraint(node, NODES_BOND);
        if (result == null) {
            result = FXCollections.observableSet(new HashSet<Bond>() {
                @Override
                public boolean add(Bond bond) {
                    Optional<Bond> opt = stream().filter(b -> b.getBondNode() == bond.getBondNode()).findAny();
                    if (opt.isPresent()) {
                        remove(opt.get());
                    }
                    return super.add(bond);
                }
            });
            setConstraint(node, NODES_BOND, result);
        }
        return result;
    }
    
    
    /**
     * Sets the value of the property onInProximity
     */
    public static void setOnInProximity(Node node, EventHandler<? super TactilePaneEvent> handler) {
        onInProximityProperty(node).set(handler);
    }
    
    /**
     * Gets the value of the property onInProximity
     */
    public static EventHandler<? super TactilePaneEvent> getOnInProximity(Node node) {
        return onInProximityProperty(node).get();
    }
    
    /**
     * Defines a function to be called continuously when another {@code Node} is
     * in the proximity of this {@code node}.
     */
    public static ObjectProperty<EventHandler<? super TactilePaneEvent>> onInProximityProperty(Node node) {
        ObjectProperty<EventHandler<? super TactilePaneEvent>> property = (ObjectProperty<EventHandler<? super TactilePaneEvent>>) getConstraint(node, ON_IN_PROXIMITY);
        if (property == null) {
            property = new SimpleObjectProperty<EventHandler<? super TactilePaneEvent>>(null) {
                @Override
                public void set(EventHandler<? super TactilePaneEvent> handler) {
                    EventHandler<? super TactilePaneEvent> oldHandler = get();
                    if (oldHandler != null) {
                        node.removeEventHandler(TactilePaneEvent.IN_PROXIMITY, oldHandler);
                    }
                    if (handler != null) {
                        node.addEventHandler(TactilePaneEvent.IN_PROXIMITY, handler);
                    }
                    super.set(handler);
                }
            };
            setConstraint(node, ON_IN_PROXIMITY, property);
        }
        return property;
    }
    
    /**
     * Sets the value of the property onProximityEntered
     */
    public static void setOnProximityEntered(Node node, EventHandler<? super TactilePaneEvent> handler) {
        onProximityEnteredProperty(node).set(handler);
    }
    
    /**
     * Gets the value of the property onProximityEntered
     */
    public static EventHandler<? super TactilePaneEvent> getOnProximityEntered(Node node) {
        return onProximityEnteredProperty(node).get();
    }
    
    /**
     * Defines a function to be called when another {@code Node} enters the
     * proximity of this {@code node}.
     */
    public static ObjectProperty<EventHandler<? super TactilePaneEvent>> onProximityEnteredProperty(Node node) {
        ObjectProperty<EventHandler<? super TactilePaneEvent>> property = (ObjectProperty<EventHandler<? super TactilePaneEvent>>) getConstraint(node, ON_PROXIMITY_ENTERED);
        if (property == null) {
            property = new SimpleObjectProperty<EventHandler<? super TactilePaneEvent>>(null) {
                @Override
                public void set(EventHandler<? super TactilePaneEvent> handler) {
                    EventHandler<? super TactilePaneEvent> oldHandler = get();
                    if (oldHandler != null) {
                        node.removeEventHandler(TactilePaneEvent.PROXIMITY_ENTERED, oldHandler);
                    }
                    if (handler != null) {
                        node.addEventHandler(TactilePaneEvent.PROXIMITY_ENTERED, handler);
                    }
                    super.set(handler);
                }
            };
            setConstraint(node, ON_PROXIMITY_ENTERED, property);
        }
        return property;
    }
    
    /**
     * Sets the value of the property onProximityLeft
     */
    public static void setOnProximityLeft(Node node, EventHandler<? super TactilePaneEvent> handler) {
        onProximityLeftProperty(node).set(handler);
    }
    
    /**
     * Gets the value of the property onProximityLeft
     */
    public static EventHandler<? super TactilePaneEvent> getOnProximityLeft(Node node) {
        return onProximityLeftProperty(node).get();
    }
    
    /**
     * Defines a function to be called when another {@code Node} leaves the
     * proximity of this {@code node}.
     */
    public static ObjectProperty<EventHandler<? super TactilePaneEvent>> onProximityLeftProperty(Node node) {
        ObjectProperty<EventHandler<? super TactilePaneEvent>> property = (ObjectProperty<EventHandler<? super TactilePaneEvent>>) getConstraint(node, ON_PROXIMITY_LEFT);
        if (property == null) {
            property = new SimpleObjectProperty<EventHandler<? super TactilePaneEvent>>(null) {
                @Override
                public void set(EventHandler<? super TactilePaneEvent> handler) {
                    EventHandler<? super TactilePaneEvent> oldHandler = get();
                    if (oldHandler != null) {
                        node.removeEventHandler(TactilePaneEvent.PROXIMITY_LEFT, oldHandler);
                    }
                    if (handler != null) {
                        node.addEventHandler(TactilePaneEvent.PROXIMITY_LEFT, handler);
                    }
                    super.set(handler);
                }
            };
            setConstraint(node, ON_PROXIMITY_LEFT, property);
        }
        return property;
    }
    
    /**
     * Sets the value of the property onInArea
     */
    public static void setOnInArea(Node node, EventHandler<? super TactilePaneEvent> handler) {
        onInAreaProperty(node).set(handler);
    }
    
    /**
     * Gets the value of the property onInArea
     */
    public static EventHandler<? super TactilePaneEvent> getOnInArea(Node node) {
        return onInAreaProperty(node).get();
    }
    
    /**
     * Defines a function to be called continuously when another {@code Node} is
     * in the bounds of this {@code node}.
     */
    public static ObjectProperty<EventHandler<? super TactilePaneEvent>> onInAreaProperty(Node node) {
        ObjectProperty<EventHandler<? super TactilePaneEvent>> property = (ObjectProperty<EventHandler<? super TactilePaneEvent>>) getConstraint(node, ON_IN_AREA);
        if (property == null) {
            property = new SimpleObjectProperty<EventHandler<? super TactilePaneEvent>>(null) {
                @Override
                public void set(EventHandler<? super TactilePaneEvent> handler) {
                    EventHandler<? super TactilePaneEvent> oldHandler = get();
                    if (oldHandler != null) {
                        node.removeEventHandler(TactilePaneEvent.IN_AREA, oldHandler);
                    }
                    if (handler != null) {
                        node.addEventHandler(TactilePaneEvent.IN_AREA, handler);
                    }
                    super.set(handler);
                }
            };
            setConstraint(node, ON_IN_AREA, property);
        }
        return property;
    }
    
    /**
     * Sets the value of the property onAreaEntered
     */
    public static void setOnAreaEntered(Node node, EventHandler<? super TactilePaneEvent> handler) {
        onAreaEnteredProperty(node).set(handler);
    }
    
    /**
     * Gets the value of the property onAreaEntered
     */
    public static EventHandler<? super TactilePaneEvent> getOnAreaEntered(Node node) {
        return onAreaEnteredProperty(node).get();
    }
    
    /**
     * Defines a function to be called when another {@code Node} enters the
     * bounds of this {@code node}.
     */
    public static ObjectProperty<EventHandler<? super TactilePaneEvent>> onAreaEnteredProperty(Node node) {
        ObjectProperty<EventHandler<? super TactilePaneEvent>> property = (ObjectProperty<EventHandler<? super TactilePaneEvent>>) getConstraint(node, ON_AREA_ENTERED);
        if (property == null) {
            property = new SimpleObjectProperty<EventHandler<? super TactilePaneEvent>>(null) {
                @Override
                public void set(EventHandler<? super TactilePaneEvent> handler) {
                    EventHandler<? super TactilePaneEvent> oldHandler = get();
                    if (oldHandler != null) {
                        node.removeEventHandler(TactilePaneEvent.AREA_ENTERED, oldHandler);
                    }
                    if (handler != null) {
                        node.addEventHandler(TactilePaneEvent.AREA_ENTERED, handler);
                    }
                    super.set(handler);
                }
            };
            setConstraint(node, ON_AREA_ENTERED, property);
        }
        return property;
    }
    
    /**
     * Sets the value of the property onAreaLeft
     */
    public static void setOnAreaLeft(Node node, EventHandler<? super TactilePaneEvent> handler) {
        onAreaLeftProperty(node).set(handler);
    }
    
    /**
     * Gets the value of the property onAreaLeft
     */
    public static EventHandler<? super TactilePaneEvent> getOnAreaLeft(Node node) {
        return onAreaLeftProperty(node).get();
    }
    
    /**
     * Defines a function to be called when another {@code Node} leaves the
     * bounds of this {@code node}.
     */
    public static ObjectProperty<EventHandler<? super TactilePaneEvent>> onAreaLeftProperty(Node node) {
        ObjectProperty<EventHandler<? super TactilePaneEvent>> property = (ObjectProperty<EventHandler<? super TactilePaneEvent>>) getConstraint(node, ON_AREA_LEFT);
        if (property == null) {
            property = new SimpleObjectProperty<EventHandler<? super TactilePaneEvent>>(null) {
                @Override
                public void set(EventHandler<? super TactilePaneEvent> handler) {
                    EventHandler<? super TactilePaneEvent> oldHandler = get();
                    if (oldHandler != null) {
                        node.removeEventHandler(TactilePaneEvent.AREA_LEFT, oldHandler);
                    }
                    if (handler != null) {
                        node.addEventHandler(TactilePaneEvent.AREA_LEFT, handler);
                    }
                    super.set(handler);
                }
            };
            setConstraint(node, ON_AREA_LEFT, property);
        }
        return property;
    }
    
    /**
     * Calls {@code register} on the given TactilePane with {@code node} as
     * argument. If {@code tactilePane} is {@code null}, {@code node} will be
     * deregistered at its previous {@code TactilePane}, if one exists.
     */
    public static void setTracker(Node node, TactilePane tactilePane) {
        if (tactilePane == null) {
            TactilePane oldPane = getTracker(node);
            if (oldPane != null) {
                oldPane.getActiveNodes().remove(node);
            }
        } else {
            tactilePane.getActiveNodes().add(node);
        }
    }
    
    /**
     * The {@code TactilePane} which is currently tracking {@code node}.
     */
    public static TactilePane getTracker(Node node) {
        return (TactilePane) getConstraint(node, TRACKER);
    }
    
    static boolean isDirty(Node node) {
        Boolean dirty = (Boolean) getConstraint(node, DIRTY);
        return dirty == null || dirty;
    }
    
    static void setDirty(Node node, boolean dirty) {
        setConstraint(node, DIRTY, dirty);
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
            return node.getProperties().get(key);
        }
        return null;
    }
    
    
    // STATIC METHODS
    
    /**
     * Gives the {@code move} Node a velocity vector with a direction so that it will move
     * away from the {@code from} Node. The speed with which the Node moves away
     * depends on {@code force}, which is the magnitude of the vector.
     * @param move  The Node that should move away
     * @param from  The Node it should move away from
     * @param force The magnitude of the vector that the Node gets
     */
    public static void moveAwayFrom(Node move, Node from, double force) {
        if (move.getParent() == null) return;
        
        Node moveDraggable = move;
        while(!(moveDraggable.getParent() instanceof TactilePane)) {
            moveDraggable = moveDraggable.getParent();
            if (move.getParent() == null) return;
        }
        
        while (getAnchor(moveDraggable) != null) {
            moveDraggable = getAnchor(moveDraggable).getAnchorNode();
            
            while(!(moveDraggable.getParent() instanceof TactilePane)) {
                moveDraggable = moveDraggable.getParent();
                if (move.getParent() == null) return;
            }
        }
        
        Bounds moveBounds = move.localToScene(move.getBoundsInLocal());
        Bounds fromBounds = from.localToScene(from.getBoundsInLocal());

        double moveX = moveBounds.getMinX() + moveBounds.getWidth() / 2;
        double moveY = moveBounds.getMinY() + moveBounds.getHeight() / 2;
        double fromX = fromBounds.getMinX() + fromBounds.getWidth() / 2;
        double fromY = fromBounds.getMinY() + fromBounds.getHeight() / 2;
        
        Point2D vector = new Point2D(moveX - fromX, moveY - fromY).normalize().multiply(force);
        TactilePane.setVector(moveDraggable, TactilePane.getVector(move).add(vector));
    }
    
    /**
     * Gives the {@code move} Node a velocity vector with a direction so that it will move
     * away from the {@code from} Node. Moves with a default level of force.
     * @param move  The Node that should move away
     * @param from  The Node it should move away from
     */
    public static void moveAwayFrom(Node move, Node from) {
    	moveAwayFrom(move, from, 100d);
    }
    
    // INSTANCE VARIABLES
    private final PhysicsTimer physics;
    final QuadTree quadTree;
    private final ObservableSet<Node> activeNodes;
    
    private final Map<Node, List<Node>> ancestorsByNode = new HashMap<>();
    private final Map<Node, ChangeListener<Bounds>> boundsListenerByNode = new HashMap<>();
    private final Map<Node, ChangeListener<Parent>> parentListenerByNode = new HashMap<>();
    
    // CONSTRUCTORS
    
    /**
     * Creates a TactilePane control 
     */
    public TactilePane() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // Pseudo class with default value needs to be set from constructor
        pseudoClassStateChanged(BORDERS_COLLIDE_STATE, false);
        // Since this Control is more or less a Pane, focusTraversable should be false by default
        ((StyleableProperty<Boolean>)focusTraversableProperty()).applyStyle(null, false);
        
        // Add EventHandlers for dragging to children when they are added
        super.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
            while(c.next()) {
                for (Node node: c.getRemoved()) {
                    // Delay removal of drag event handlers, just in case all that
                    // happened is a node.toFront() call. Ugly workaround, but I can't find a prettier solution
                    PauseTransition holdTimer = new PauseTransition(Duration.millis(500));
                    holdTimer.setOnFinished(e -> { 
                        if (node.getParent() != TactilePane.this) {
                            removeDragEventHandlers(node);
                        }
                    });
                    holdTimer.playFromStart();
                }
                for (Node node: c.getAddedSubList()) {
                    addDragEventHandlers(node);
                }
            }
        });
        
        // Initialise quadTree
        quadTree = new QuadTree(this.localToScene(this.getBoundsInLocal()));
        this.widthProperty().addListener((observableValue, oldWidth, newWidth) -> {
            quadTree.setBounds(this.localToScene(this.getBoundsInLocal()));
        });
        this.heightProperty().addListener((observableValue, oldHeight, newHeight) -> {
            quadTree.setBounds(this.localToScene(this.getBoundsInLocal()));
        });
        
        // Initialise activeNodes
        activeNodes = FXCollections.observableSet(Collections.newSetFromMap(new ConcurrentHashMap<>()));
        activeNodes.addListener((SetChangeListener.Change<? extends Node> change) -> {
            if (change.wasAdded()) {
                Node node = change.getElementAdded();
                TactilePane oldPane = getTracker(node);
                if (oldPane != null) {
                    oldPane.getActiveNodes().remove(node);
                }
                quadTree.insert(node);
                setConstraint(node, TRACKER, TactilePane.this);
                
                startTrackingLocation(node);
            }
            else {
                Node node = change.getElementRemoved();
                quadTree.insert(node);
                
                for (Node colliding : TactilePane.getNodesColliding(node)) {
                    node.fireEvent(new TactilePaneEvent(TactilePaneEvent.AREA_LEFT, node, colliding));
                    colliding.fireEvent(new TactilePaneEvent(TactilePaneEvent.AREA_LEFT, colliding, node));
                }
                TactilePane.getNodesColliding(node).clear();
                
                for (Node colliding : TactilePane.getNodesInProximity(node)) {
                    node.fireEvent(new TactilePaneEvent(TactilePaneEvent.PROXIMITY_LEFT, node, colliding));
                    colliding.fireEvent(new TactilePaneEvent(TactilePaneEvent.PROXIMITY_LEFT, colliding, node));
                }
                TactilePane.getNodesInProximity(node).clear();
                
                setConstraint(node, TRACKER, null);
                
                stopTrackingLocation(node);
            }
        });
        
        // Initialise Physics
        physics = new PhysicsTimer(this);
        physics.start();
    }
    
    /**
     * Creates a TactilePane control
     * @param children The initial set of children for this TactilePane
     */
    public TactilePane(Node... children) {
        this();
        super.getChildren().addAll(children);
    }
    
    
    // HELP METHODS FOR CONSTRUCTOR
    
    private void addDragEventHandlers(final Node node) {
        if (getDragContext(node) != null) {
            // The node already has drag event handlers
            return;
        }
        
        final DragContext dragContext = new DragContext(node);
        
        EventHandler<TouchEvent> touchHandler = event -> {
            if (!isDraggable(node)) return;
            
            EventType type = event.getEventType();
            
            if (type == TouchEvent.TOUCH_PRESSED) {
                if (dragContext.touchId == DragContext.NULL_ID) {
                    dragContext.touchId = event.getTouchPoint().getId();
                    handleTouchPressed(node, event.getTouchPoint().getX(), event.getTouchPoint().getY());
                    event.consume();
                }
            } else if (type == TouchEvent.TOUCH_MOVED) {
                if (dragContext.touchId == event.getTouchPoint().getId()) {
                    handleTouchMoved(node, event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY());
                    event.consume();
                }
            } else if (type == TouchEvent.TOUCH_RELEASED) {
                if (dragContext.touchId == event.getTouchPoint().getId()) {
                    handleTouchReleased(node);
                    dragContext.touchId = DragContext.NULL_ID;
                    event.consume();
                }
            }
        };
        
        EventHandler<MouseEvent> mouseHandler = event -> {
            if (!isDraggable(node) || event.isSynthesized()) return;
            
            EventType type = event.getEventType();
            
            if (type == MouseEvent.MOUSE_PRESSED) {
                if (dragContext.touchId == DragContext.NULL_ID) {
                    dragContext.touchId = DragContext.MOUSE_ID;
                    handleTouchPressed(node, event.getX(), event.getY());
                    event.consume();
                }
            } else if (type == MouseEvent.MOUSE_DRAGGED) {
                
                if (dragContext.touchId == DragContext.MOUSE_ID) {
                    handleTouchMoved(node, event.getSceneX(), event.getSceneY());
                    event.consume();
                }
            } else if (type == MouseEvent.MOUSE_RELEASED) {
                if (dragContext.touchId == DragContext.MOUSE_ID) {
                    handleTouchReleased(node);
                    dragContext.touchId = DragContext.NULL_ID;
                    event.consume();
                }
            }
        };
        
        setDragContext(node, dragContext);
        setConstraint(node, TOUCH_EVENT_HANDLER, touchHandler);
        setConstraint(node, MOUSE_EVENT_HANDLER, mouseHandler);
        
        if (getDragProcessingMode() == EventProcessingMode.FILTER) {
            node.addEventFilter(TouchEvent.ANY, touchHandler);
            node.addEventFilter(MouseEvent.ANY, mouseHandler);
        } else {
            node.addEventHandler(TouchEvent.ANY, touchHandler);
            node.addEventHandler(MouseEvent.ANY, mouseHandler);
        }
    }

    private void removeDragEventHandlers(Node node) {
        EventHandler<TouchEvent> touchHandler = (EventHandler<TouchEvent>) getConstraint(node, TOUCH_EVENT_HANDLER);
        EventHandler<MouseEvent> mouseHandler = (EventHandler<MouseEvent>) getConstraint(node, MOUSE_EVENT_HANDLER);
        
        // Assuming that mouseHandler will be null if touchHandler is null
        if (touchHandler == null) return;
        
        if (getDragProcessingMode() == EventProcessingMode.FILTER) {
            node.removeEventFilter(TouchEvent.ANY, touchHandler);
            node.removeEventFilter(MouseEvent.ANY, mouseHandler);
        } else {
            node.removeEventHandler(TouchEvent.ANY, touchHandler);
            node.removeEventHandler(MouseEvent.ANY, mouseHandler);
        }
        
        setDragContext(node, null);
        setConstraint(node, TOUCH_EVENT_HANDLER, null);
        setConstraint(node, MOUSE_EVENT_HANDLER, null);
    }
    
    private void handleTouchPressed(Node node, double localX, double localY) {
        DragContext dragContext = getDragContext(node);
        setAnchor(node, null);
        setInUse(node, true);
        setVector(node, Point2D.ZERO);

        dragContext.localX = localX;
        dragContext.localY = localY;

        if (isGoToForegroundOnContact(node)) {
            node.toFront();
        }
    }

    private void handleTouchMoved(Node node, double sceneX, double sceneY) {
        DragContext dragContext = getDragContext(node);
        if (getAnchor(node) == null) {
            Point2D thisP = sceneToLocal(new Point2D(sceneX, sceneY));
            double x = thisP.getX() - dragContext.localX - node.getTranslateX();
            double y = thisP.getY() - dragContext.localY - node.getTranslateY();

            if (isBordersCollide()) {
                // TODO: This fails when a pane has a shadow effect
                Bounds paneBounds = this.getLayoutBounds();
                Bounds nodeBounds = node.getBoundsInParent();
                
                double deltaX = node.getLayoutX() - nodeBounds.getMinX();
                double deltaY = node.getLayoutY() - nodeBounds.getMinY();
                
                if (x - deltaX < paneBounds.getMinX()) {
                    x = paneBounds.getMinX() + deltaX;
                } else if (x - deltaX + nodeBounds.getWidth() > paneBounds.getMaxX()) {
                    x = paneBounds.getMaxX() - nodeBounds.getWidth() + deltaX;
                }
                if (y - deltaY < paneBounds.getMinY()) {
                    y = paneBounds.getMinY() + deltaY;
                } else if (y - deltaY + nodeBounds.getHeight() > paneBounds.getMaxY()) {
                    y = paneBounds.getMaxY() - nodeBounds.getHeight() + deltaY;
                }
            }
            
            node.setLayoutX(x); 
            node.setLayoutY(y);
        }
    }

    private void handleTouchReleased(Node node) {
        setInUse(node, false);
    }
    
    private void startTrackingLocation(Node node) {
        List<Node> ancestors = getAncestors(node);
        ancestorsByNode.put(node, ancestors);
        
        for (Node ancestor : ancestors) {
            ancestor.boundsInParentProperty().addListener(getBoundsListener(node));
            ancestor.parentProperty().addListener(getParentListener(node));
        }
        node.boundsInParentProperty().addListener(getBoundsListener(node));
        node.parentProperty().addListener(getParentListener(node));
    }
    
    private void stopTrackingLocation(Node node) {
        for (Node ancestor : ancestorsByNode.get(node)) {
            ancestor.boundsInParentProperty().removeListener(getBoundsListener(node));
            ancestor.parentProperty().removeListener(getParentListener(node));
        }
        node.boundsInParentProperty().removeListener(getBoundsListener(node));
        node.parentProperty().removeListener(getParentListener(node));
        
        ancestorsByNode.remove(node);
        boundsListenerByNode.remove(node);
        parentListenerByNode.remove(node);
    }
    
    // INSTANCE PROPERTIES
    
   /**
     *
     * @return modifiable list of children.
     */
    @Override public ObservableList<Node> getChildren() {
        return super.getChildren();
    }
    
    /**
     * 
     * @return modifiable list of {@code Nodes} that are tracked by this {@code TactilePane}
     */
    public ObservableSet<Node> getActiveNodes() {
        return activeNodes;
    }
    
    /**
     * Whether Mouse/Touch events at this TactilePane's children should be processed and consumed
     * at the filtering stage or the handling stage.
     */
    private ObjectProperty<EventProcessingMode> dragProcessingMode;
    
    public void setDragProcessingMode(EventProcessingMode mode) {
        dragProcessingModeProperty().set(mode);
    }
    
    public EventProcessingMode getDragProcessingMode() {
        return dragProcessingModeProperty().get();
    }
    
    public ObjectProperty<EventProcessingMode> dragProcessingModeProperty() {
        if (dragProcessingMode == null) {
            dragProcessingMode = new SimpleObjectProperty<EventProcessingMode>(EventProcessingMode.HANDLER) {
                
                @Override
                public void set(EventProcessingMode value) {
                    for (Node node : TactilePane.this.getChildren()) {
                        removeDragEventHandlers(node);
                    }
                    super.set(value);
                    for (Node node : TactilePane.this.getChildren()) {
                        addDragEventHandlers(node);
                    }
                }
            };
        }
        return dragProcessingMode;
    }
    
    /**
     * Whether children will collide with the borders of this
     * {@code TactilePane}. If set to true the {@code TactilePane} will prevent
     * children that are moving because of user input or physics to
     * move outside of the {@code TactilePane's} boundaries.
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
            bordersCollide = new StyleableBooleanProperty(false) {

                @Override
                public void invalidated() {
                    pseudoClassStateChanged(BORDERS_COLLIDE_STATE, get());
                }
                
                @Override
                public Object getBean() {
                    return TactilePane.this;
                }

                @Override
                public String getName() {
                    return PSEUDO_CLASS_BORDERS_COLLIDE;
                }

                @Override
                public CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
                    return StyleableProperties.BORDERS_COLLIDE;
                }
                
            };
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
     * considered in each others proximity. When set to 0, TactilePane won't fire
     * {@code PROXIMITY_ENTERED} or {@code IN_PROXIMITY} events at all.
     * {@code PROXIMITY_LEFT} events will still be fired for any pair of
     * {@code Nodes} that entered each other's proximity before the threshold
     * was set to 0. When set to a negative value, an IllegalArgumentException
     * is thrown.
     */
    public final DoubleProperty proximityThresholdProperty() {
        return quadTree.proximityThresholdProperty();
    }
    
    /**
     * A scalar by which a vector is multiplied during every physics calculation.
     * Must between 0 and 1. Influences how fast a node stops moving after it has
     * been given a vector.
     */
    private DoubleProperty frictionMultiplier;
    
    public final double getFrictionMultiplier() {
        return frictionMultiplierProperty().get();
    }
    
    public final void setFrictionMultiplier(double frictionMultiplier) {
        frictionMultiplierProperty().set(frictionMultiplier);
    }
    
    public final DoubleProperty frictionMultiplierProperty() {
        if (frictionMultiplier == null) {
            frictionMultiplier = new SimpleDoubleProperty(0.95) {
                @Override
                public void set(double value) {
                    if (value < 0 || value > 1) {
                        throw new IllegalArgumentException("FrictionMultiplier must be between 0 and 1");
                    }
                    super.set(value);
                }
            };
        }
        return frictionMultiplier;
    }
   
    /**
     * A scalar by which a node's vector is multiplied whenever it collides with
     * a border of the TactilePane. Must be a value between 0 and 1. Influences
     * how much energy it loses retains after bouncing off a border.
     */
    private DoubleProperty bounceMultiplier;
    
    public final double getBounceMultiplier() {
        return bounceMultiplierProperty().get();
    }
    
    public final void setBounceMultiplier(double bounceMultiplier) {
        bounceMultiplierProperty().set(bounceMultiplier);
    }
    
    public final DoubleProperty bounceMultiplierProperty() {
        if (bounceMultiplier == null) {
            bounceMultiplier = new SimpleDoubleProperty(0.70) {
                @Override
                public void set(double value) {
                    if (value < 0 || value > 1) {
                        throw new IllegalArgumentException("BounceMultiplier must be between 0 and 1");
                    }
                    super.set(value);
                }
            };
        }
        return bounceMultiplier;
    }

    /**
     * A scalar by which a vector is multiplied if its node is configured to
     * slide on release. May not be a negative value. Influences how much speed the
     * node will have when it is released.
     */
    private DoubleProperty slideMultiplier;
    
    public double getSlideMultiplier() {
        return slideMultiplierProperty().get();
    }
    
    public void setSlideMultiplier(double slideMultiplier) {
        slideMultiplierProperty().set(slideMultiplier);
    }
    
    public DoubleProperty slideMultiplierProperty() {
        if (slideMultiplier == null) {
            slideMultiplier = new SimpleDoubleProperty(1.6) {
                @Override
                public void set(double value) {
                    if (value < 0) {
                        throw new IllegalArgumentException("SlideMultiplier may not be a negative number");
                    }
                    super.set(value);
                }
            };
        }
        return slideMultiplier;
    }
    
    /**
     * The minimum magnitude a vector must have before it is reset to a zero vector.
     */
    private DoubleProperty vectorThreshold;
    
    public double getVectorThreshold() {
        return vectorThresholdProperty().get();
    }
    
    public void setVectorThreshold(double vectorThreshold) {
        vectorThresholdProperty().set(vectorThreshold);
    }
    
    public DoubleProperty vectorThresholdProperty() {
        if (vectorThreshold == null) {
            vectorThreshold = new SimpleDoubleProperty(1.6) {
                @Override
                public void set(double value) {
                    if (value < 0) {
                        throw new IllegalArgumentException("VectorThreshold may not be a negative number");
                    }
                    super.set(value);
                }
            };
        }
        return vectorThreshold;
    }
    
    // HELPER METHODS
    
    // Returns all ancestors of a given node
    private List<Node> getAncestors(Node node) {
        List<Node> ancestors = new ArrayList<>();
        Node parent = node.getParent();
        
        while (parent != null) {
            ancestors.add(parent);
            parent = parent.getParent();
        }
        
        return ancestors;
    }
    
    // Returns the bounds listener for the given node, or creates on if it doesn't exist
    private ChangeListener<Bounds> getBoundsListener(Node node) {
        ChangeListener<Bounds> result = boundsListenerByNode.get(node);
        if (result == null) {
            result = new ChangeListener<Bounds>() {
                @Override
                public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                    setDirty(node, true);
                }
            };
            boundsListenerByNode.put(node, result);
        }
        return result;
    }
    
    // Returns the parent listener for the given node, or creates on if it doesn't exist
    private ChangeListener<Parent> getParentListener(Node node) {
        ChangeListener<Parent> result = parentListenerByNode.get(node);
        if (result == null) {
            result = new ChangeListener<Parent>() {
                @Override
                public void changed(ObservableValue<? extends Parent> observable, Parent oldValue, Parent newValue) {
                    stopTrackingLocation(node);
                    startTrackingLocation(node);
                }
            };
            parentListenerByNode.put(node, result);
        }
        return result;
    }
    
    // STYLESHEET HANDLING
    
    // The selector class
    private static final String DEFAULT_STYLE_CLASS = "tactilefx-tactile-pane";
    // Only property that I feel makes sense as a pseudo class
    private static final String PSEUDO_CLASS_BORDERS_COLLIDE = "bordersCollide";
    // Attached pseudo class for draggable nodes
    private static final String ATTACHED_PSEUDO_CLASS_DRAGGABLE = "tactilePaneDraggable";
    private static final String ATTACHED_PSEUDO_CLASS_IN_USE = "tactilePaneInUse";
    
    private static final class StyleableProperties {
        
        private static final CssMetaData<TactilePane, Boolean> BORDERS_COLLIDE =
                new CssMetaData<TactilePane, Boolean>("-tactilefx-borders-collide", BooleanConverter.getInstance(), Boolean.FALSE) {
                    
            @Override
            public boolean isSettable(TactilePane styleable) {
                return styleable.bordersCollide == null || !styleable.bordersCollide.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(TactilePane styleable) {
                return (StyleableProperty<Boolean>) styleable.bordersCollideProperty();
            }
        };
        
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
                final List<CssMetaData<? extends Styleable, ?>> styleables = 
                    new ArrayList<>(Control.getClassCssMetaData());
                styleables.add(BORDERS_COLLIDE);
                STYLEABLES = Collections.unmodifiableList(styleables);
        }
        
        
        // Attached Styleable Properties. It does not actually seem to be possible
        // to style Nodes with them, but they can still be used for pseudo classes.
        private static final class Attached {

            private static final CssMetaData<Node, Boolean> DRAGGABLE
                    = new CssMetaData<Node, Boolean>("-tactilefx-tactilepane-draggable", BooleanConverter.getInstance(), Boolean.FALSE) {

                @Override
                public boolean isSettable(Node styleable) {
                    return TactilePane.draggableProperty(styleable).isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(Node styleable) {
                    return (StyleableProperty<Boolean>) TactilePane.draggableProperty(styleable);
                }
            };

            private static final CssMetaData<Node, Boolean> IN_USE
                    = new CssMetaData<Node, Boolean>("-tactilefx-tactilepane-in-use", BooleanConverter.getInstance(), Boolean.FALSE) {

                @Override
                public boolean isSettable(Node styleable) {
                    return false;
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(Node styleable) {
                    return (StyleableProperty<Boolean>) TactilePane.inUseProperty(styleable);
                }
            };
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
    
    // Pseudo classes
    private static final PseudoClass BORDERS_COLLIDE_STATE =
            PseudoClass.getPseudoClass(PSEUDO_CLASS_BORDERS_COLLIDE);
    // Attached pseudo classes
    private static final PseudoClass ATTACHED_DRAGGABLE_STATE =
            PseudoClass.getPseudoClass(ATTACHED_PSEUDO_CLASS_DRAGGABLE);
    private static final PseudoClass ATTACHED_IN_USE_STATE =
            PseudoClass.getPseudoClass(ATTACHED_PSEUDO_CLASS_IN_USE);
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Skin<TactilePane> createDefaultSkin() {
        return new TactilePaneSkin(this);
    }
    
    // ENUMS
    
    /**
     * Defines whether an Event is processed at the filter stage or the handler stage.
     */
    public enum EventProcessingMode {
        /**
         * Represents processing events at the handler stage
         */
        HANDLER, 
        
        /**
         * Represents processing events at the filter stage.
         */
        FILTER
    }
    
    // NESTED CLASSES

    /**
     * Help class used for dragging TactilePane's children.
     */
    public class DragContext {
        public static final int NULL_ID = -1;
        public static final int MOUSE_ID = -2;
        
        final Node draggable;         // Node that is being dragged
        double localX, localY;  // The x,y position of the Event in the Node
        int touchId;            // The id of the finger/cursor that is currently dragging the Node
        
        private DragContext(Node draggable) {
            this.draggable = draggable;
            touchId = -1;
        }
        
        /**
         * The Node that is being dragged
         */
        public Node getDraggable() {
            return draggable;
        }
        
        /**
         * The x location of the touchpoint/cursor that is currently dragging the Node
         */
        public double getLocalX() {
            return localX;
        }
        
        /**
         * The y location of the touchpoint/cursor that is currently dragging the Node
         */
        public double getLocalY() {
            return localY;
        }
        
        /**
         * The id of the TouchPoint that is responsible for dragging the Node.
         * Returns NULL_ID if the Node is not being dragged, or MOUSE_ID if the
         * Node is dragged by a mouse cursor.
         */
        public int getTouchId() {
            return touchId;
        }
        
        /**
         * Binds the DragContext to a different TouchEvent. This allows a TouchPoint other than
         * the one that started the drag operation to take over the drag gesture.
         * 
         * @throws IllegalArgumentException Thrown when the TouchEvent is not of 
         * type TouchEvent.TOUCH_PRESSED, or when the event's target is not the Node that this
         * DragContext belongs to, or have that Node as ancestor.
         */
        public void bind(TouchEvent event) {
            if (event.getTouchPoint().getId() == touchId) return;
            
            Node target = (Node) event.getTarget();
            while (target.getParent() != draggable) {
                target = target.getParent();
                if (target == null) {
                    throw new IllegalArgumentException("TouchEvent's target should be draggable, or have draggable as ancestor");
                }
            }
            if (event.getEventType() != TouchEvent.TOUCH_PRESSED) {
                throw new IllegalArgumentException("TouchEvent should be of type TOUCH_PRESSED");
            }
            
            touchId = event.getTouchPoint().getId();
            handleTouchPressed(draggable, event.getTouchPoint().getX(), event.getTouchPoint().getY());
        }
        
        @Override
        public String toString() {
            return String.format("DragContext [draggable = %s, ,touchId = %d, localX = %f, localY = %f]", draggable.toString(), touchId, localX, localY);
        }
    }
}
