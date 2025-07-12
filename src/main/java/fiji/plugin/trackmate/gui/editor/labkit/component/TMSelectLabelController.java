package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.util.BdvHandle;
import bdv.viewer.ViewerPanel;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.brush.BdvMouseBehaviourUtils;
import sc.fiji.labkit.ui.brush.SelectLabelController;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.ImageLabelingModel;

/**
 * Copied from {@link SelectLabelController}.
 */
public class TMSelectLabelController
{

	private static final double[] PIXEL_CENTER_OFFSET = { 0.5, 0.5, 0.5 };

	private final ClickBehaviour behaviour = this::click;

	private final BdvHandle bdv;

	private final ViewerPanel viewer;

	private final ImageLabelingModel model;

	public TMSelectLabelController( final BdvHandle bdv, final ImageLabelingModel model )
	{
		this.bdv = bdv;
		this.viewer = bdv.getViewerPanel();
		this.model = model;
	}

	public void setActive( final boolean active )
	{
		BdvMouseBehaviourUtils.setMouseBehaviourActive( bdv, behaviour, active );
	}

	private void click( final int x, final int y )
	{
		final RealPoint globalPosition = new RealPoint( 3 );
		viewer.displayToGlobalCoordinates( x, y, globalPosition );
		model.labelTransformation().applyInverse( globalPosition, globalPosition );
		globalPosition.move( PIXEL_CENTER_OFFSET );
		final RandomAccess< LabelingType< Label > > ra = labeling().randomAccess();
		ra.setPosition( roundAndReduceDimension( globalPosition, ra.numDimensions() ) );
		final Optional< Label > label = nextLabel( ra.get(), model.selectedLabel().get() );
		label.ifPresent( model.selectedLabel()::set );
	}

	private Optional< Label > nextLabel( final LabelingType< Label > labels, final Label label )
	{
		final List< Label > visibleLabels = labels.stream().filter( Label::isVisible )
				.collect( Collectors.toList() );
		visibleLabels.sort( Comparator.comparing( model.labeling().get()
				.getLabels()::indexOf ) );
		if ( visibleLabels.contains( label ) )
		{
			final int index = visibleLabels.indexOf( label );
			return Optional.of( visibleLabels.get( ( index + 1 ) % visibleLabels.size() ) );
		}
		return visibleLabels.stream().findFirst();
	}

	private RandomAccessibleInterval< LabelingType< Label > > labeling()
	{
		final RandomAccessibleInterval< LabelingType< Label > > label = model.labeling()
				.get();
		if ( model.isTimeSeries() )
			return Views.hyperSlice( label, label
					.numDimensions() - 1, viewer.state().getCurrentTimepoint() );
		return label;
	}

	private Point roundAndReduceDimension( final RealLocalizable realLocalizable,
			final int numDimesions )
	{
		final Point point = new Point( numDimesions );
		for ( int i = 0; i < point.numDimensions(); i++ )
			point.setPosition( ( long ) realLocalizable.getDoublePosition( i ), i );
		return point;
	}

	public void install( final Behaviours behaviours )
	{
		behaviours.behaviour( behaviour, SELECT_LABEL, SELECT_LABEL_KEYS );
	}

	private static final String SELECT_LABEL = "select label";

	private static final String[] SELECT_LABEL_KEYS = new String[] { "shift button1" };

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KEY_CONFIG_SCOPE, KEY_CONFIG_CONTEXT );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( SELECT_LABEL, SELECT_LABEL_KEYS, "Select the label under the cursor." );
		}
	}
}
