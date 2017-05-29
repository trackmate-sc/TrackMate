/*
 * Copyright 2013 Jason Winnebeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fiji.plugin.trackmate.visualization.trajeditor.plotting;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * JFXUtil contains JavaFX utility methods used in the Gillius jfxutils project.
 *
 * @author Jason Winnebeck
 */
public class JFXUtil {
	/**
	 * Find the X coordinate in ancestor's coordinate system that corresponds to the X=0 axis in
	 * descendant's coordinate system.
	 *
	 * @param descendant a Node that is a descendant (direct or indirectly) of the ancestor
	 * @param ancestor   a Node that is an ancestor of descendant
	 */
	public static double getXShift( Node descendant, Node ancestor ) {
		double ret = 0.0;
		Node curr = descendant;
		while ( curr != ancestor ) {
			ret += curr.getLocalToParentTransform().getTx();
			curr = curr.getParent();
			if ( curr == null )
				throw new IllegalArgumentException( "'descendant' Node is not a descendant of 'ancestor" );
		}

		return ret;
	}

	/**
	 * Find the Y coordinate in ancestor's coordinate system that corresponds to the Y=0 axis in
	 * descendant's coordinate system.
	 *
	 * @param descendant a Node that is a descendant (direct or indirectly) of the ancestor
	 * @param ancestor   a Node that is an ancestor of descendant
	 */
	public static double getYShift( Node descendant, Node ancestor ) {
		double ret = 0.0;
		Node curr = descendant;
		while ( curr != ancestor ) {
			ret += curr.getLocalToParentTransform().getTy();
			curr = curr.getParent();
			if ( curr == null )
				throw new IllegalArgumentException( "'descendant' Node is not a descendant of 'ancestor" );
		}

		return ret;
	}

	/**
	 * Make a best attempt to replace the original component with the replacement, and keep the same
	 * position and layout constraints in the container.
	 * <p>
	 * Currently this method is probably not perfect. It uses three strategies:
	 * <ol>
	 *   <li>If the original has any properties, move all of them to the replacement</li>
	 *   <li>If the parent of the original is a {@link BorderPane}, preserve the position</li>
	 *   <li>Preserve the order of the children in the parent's list</li>
	 * </ol>
	 * <p>
	 * This method does not transfer any handlers (mouse handlers for example).
	 *
	 * @param original    non-null Node whose parent is a {@link Pane}.
	 * @param replacement non-null Replacement Node
	 */
	public static void replaceComponent( Node original, Node replacement ) {
		Pane parent = (Pane) original.getParent();
		//transfer any properties (usually constraints)
		replacement.getProperties().putAll( original.getProperties() );
		original.getProperties().clear();

		ObservableList<Node> children = parent.getChildren();
		int originalIndex = children.indexOf( original );
		if ( parent instanceof BorderPane ) {
			BorderPane borderPane = (BorderPane) parent;
			if ( borderPane.getTop() == original ) {
				children.remove( original );
				borderPane.setTop( replacement );

			} else if ( borderPane.getLeft() == original ) {
				children.remove( original );
				borderPane.setLeft( replacement );

			} else if ( borderPane.getCenter() == original ) {
				children.remove( original );
				borderPane.setCenter( replacement );

			} else if ( borderPane.getRight() == original ) {
				children.remove( original );
				borderPane.setRight( replacement );

			} else if ( borderPane.getBottom() == original ) {
				children.remove( original );
				borderPane.setBottom( replacement );
			}
		} else {
			//Hope that preserving the properties and position in the list is sufficient
			children.set( originalIndex, replacement );
		}
	}

	/**
	 * Creates a "Scale Pane", which is a pane that scales as it resizes, instead of reflowing layout
	 * like a normal pane. It can be used to create an effect like a presentation slide. There is no
	 * attempt to preserve the aspect ratio.
	 * <p>
	 * If the region has an explicitly set preferred width and height, those are used unless
	 * override is set true.
	 * <p>
	 * If the region already has a parent, the returned pane will replace it via the
	 * {@link #replaceComponent(Node, Node)} method. The Region's parent must be a Pane in this case.
	 *
	 * @param region   non-null Region
	 * @param w        default width, used if the region's width is calculated
	 * @param h        default height, used if the region's height is calculated
	 * @param override if true, w,h is the region's "100%" size even if the region has an explicit
	 *                 preferred width and height set.
	 *
	 * @return the created StackPane, with preferred width and height set based on size determined by
	 *         w, h, and override parameters.
	 */
	public static StackPane createScalePane( Region region, double w, double h, boolean override ) {
		//If the Region containing the GUI does not already have a preferred width and height, set it.
		//But, if it does, we can use that setting as the "standard" resolution.
		if ( override || region.getPrefWidth() == Region.USE_COMPUTED_SIZE )
			region.setPrefWidth( w );
		else
			w = region.getPrefWidth();

		if ( override || region.getPrefHeight() == Region.USE_COMPUTED_SIZE )
			region.setPrefHeight( h );
		else
			h = region.getPrefHeight();

		StackPane ret = new StackPane();
		ret.setPrefWidth( w );
		ret.setPrefHeight( h );
		if ( region.getParent() != null )
			replaceComponent( region, ret );

		//Wrap the resizable content in a non-resizable container (Group)
		Group group = new Group( region );
		//Place the Group in a StackPane, which will keep it centered
		ret.getChildren().add( group );

		//Bind the scene's width and height to the scaling parameters on the group
		group.scaleXProperty().bind( ret.widthProperty().divide( w ) );
		group.scaleYProperty().bind( ret.heightProperty().divide( h ) );

		return ret;
	}
}
