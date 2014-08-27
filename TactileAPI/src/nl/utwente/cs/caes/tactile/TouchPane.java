package nl.utwente.cs.caes.tactile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

public class TouchPane extends Pane {
	private Set<ActionGroup> actionGroups = new HashSet<ActionGroup>();
	private QuadTree quadTree;

	public TouchPane() {
		super();
		initialise();
	}

	public TouchPane(Node... children) {
		super(children);
		initialise();
	}

	// Called by all constructors
	private void initialise() {
		TouchPane thisPane = this;

		// Add resize listeners (needs optimisation)
		widthProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(
					ObservableValue<? extends Number> observableValue,
					Number oldWidth, Number newWidth) {
				quadTree = new QuadTree(thisPane
						.localToScene(getBoundsInLocal()));
				for (Node ag : actionGroups) {
					quadTree.insert(ag);
				}
			}
		});

		heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(
					ObservableValue<? extends Number> observableValue,
					Number oldHeight, Number newHeight) {
				quadTree = new QuadTree(thisPane
						.localToScene(getBoundsInLocal()));
				for (Node ag : actionGroups) {
					quadTree.insert(ag);
				}
			}
		});

		// Initialise QuadTree
		quadTree = new QuadTree(this.localToScene(getBoundsInLocal()));

		// Create AnimationTimer for collision checking
		new AnimationTimer() {

			@Override
			public void handle(long now) {
				// Update QuadTree
				quadTree.update();

				for (ActionGroup thisObject : actionGroups) {
					Bounds thisBounds = thisObject.localToScene(thisObject.getBoundsInLocal());
					double proximityThreshold = quadTree.getProximityThreshold();
					double x = thisBounds.getMinX() - proximityThreshold;
					double y = thisBounds.getMinY() - proximityThreshold;
					double w = thisBounds.getWidth() + proximityThreshold * 2;
					double h = thisBounds.getHeight() + proximityThreshold * 2;
					Bounds proximityBounds = new BoundingBox(x, y, w, h);
					
					List<Node> otherObjects = quadTree.retrieve(thisObject);
					for (Node otherNode : otherObjects) {
						ActionGroup otherObject = (ActionGroup) otherNode;
						
						if (thisObject == otherObject) {
							continue;
						}
						
						Bounds otherBounds = otherObject.localToScene(otherObject.getBoundsInLocal());
						if (thisBounds.intersects(otherBounds)){
							if (thisObject.getActionGroupsColliding().add(otherObject)) {
								otherObject.getActionGroupsColliding().add(thisObject);
								System.out.println("Collision started");
							}
						}
						else if (proximityBounds.intersects(otherBounds)){
							if (thisObject.getActionGroupsColliding().remove(otherObject)) {
								otherObject.getActionGroupsColliding().remove(thisObject);
								System.out.println("Collision stopped");
							}
							if (thisObject.getActionGroupsInProximity().add(otherObject)) {
								otherObject.getActionGroupsInProximity().add(thisObject);
								System.out.println("Proximity entered");
							}
						}
						else {
							if (thisObject.getActionGroupsInProximity().remove(otherObject)) {
								otherObject.getActionGroupsInProximity().remove(thisObject);
								System.out.println("Proximity left");
							} 
						}
					}
				}
			}
		}.start();
	}

	/**
	 * Registers an ActionGroup to the TouchPane. The TouchPane will track the
	 * position of the ActionGroup and check for collisions / proximity events.
	 * The ActionGroup should have this TouchPane as (indirect) ancestor.
	 * 
	 * @param actionGroup
	 *            The ActionGroup that is to be tracked
	 * @throws IllegalArgumentException
	 *             If the ActionGroup does not have this TouchPane as (indirect)
	 *             ancestor
	 */
	public void register(ActionGroup actionGroup) {
		if (actionGroups.add(actionGroup)) {
			Parent ancestor = actionGroup.getParent();
			while (ancestor != this) {
				try {
					ancestor = ancestor.getParent();
				} catch (NullPointerException e) {
					throw new IllegalArgumentException(
							"The provided ActionGroup does not have this TouchPane as ancestor!");
				}
			}
			quadTree.insert(actionGroup);
		}
	}

	/**
	 * Deregisters an ActionGroup from the TouchPane.
	 * 
	 * @param actionGroup
	 *            The ActionGroup that shoud be deregistered
	 */
	public void deregister(ActionGroup actionGroup) {
		actionGroups.remove(actionGroup);
		quadTree.remove(actionGroup);
	}
}
