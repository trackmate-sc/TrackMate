package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_HEIGHT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_WIDTH;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_COLOR;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxStyleUtils;
import com.mxgraph.view.mxPerimeter;
import com.mxgraph.view.mxStylesheet;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;

public class TrackSchemeStylist
{
	private final Model model;

	private final DisplaySettings displaySettings;

	private final JGraphXAdapter graphx;

	private String globalStyle = DEFAULT_STYLE_NAME;

	private final Map< String, Object > simpleStyle;

	private final Map< String, Object > fullStyle;

	private final Map< String, Object > edgeStyle;

	static final List< String > VERTEX_STYLE_NAMES = new ArrayList<>();

	private static final String FULL_STYLE_NAME = "full";

	private static final String SIMPLE_STYLE_NAME = "simple";

	private static final String DEFAULT_STYLE_NAME = SIMPLE_STYLE_NAME;

	private static final Map< String, Object > FULL_VERTEX_STYLE = new HashMap<>();

	private static final Map< String, Object > SIMPLE_VERTEX_STYLE = new HashMap<>();

	private static final Map< String, Object > BASIC_EDGE_STYLE = new HashMap<>();

	static
	{
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_FILLCOLOR, "white" );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_FONTCOLOR, "black" );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_SHAPE, mxScaledLabelShape.SHAPE_NAME );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_ROUNDED, true );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_PERIMETER, mxPerimeter.RectanglePerimeter );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR );
		FULL_VERTEX_STYLE.put( mxConstants.STYLE_NOLABEL, false );

		SIMPLE_VERTEX_STYLE.put( mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE );
		SIMPLE_VERTEX_STYLE.put( mxConstants.STYLE_NOLABEL, true );

		BASIC_EDGE_STYLE.put( mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR );
		BASIC_EDGE_STYLE.put( mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER );
		BASIC_EDGE_STYLE.put( mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE );
		BASIC_EDGE_STYLE.put( mxConstants.STYLE_STARTARROW, mxConstants.NONE );
		BASIC_EDGE_STYLE.put( mxConstants.STYLE_ENDARROW, mxConstants.NONE );
		BASIC_EDGE_STYLE.put( mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR );
		BASIC_EDGE_STYLE.put( mxConstants.STYLE_NOLABEL, true );

		VERTEX_STYLE_NAMES.add( SIMPLE_STYLE_NAME );
		VERTEX_STYLE_NAMES.add( FULL_STYLE_NAME );
	}

	public TrackSchemeStylist( final Model model, final JGraphXAdapter graphx, final DisplaySettings displaySettings )
	{
		this.model = model;
		this.graphx = graphx;
		this.displaySettings = displaySettings;
		// Copy styles.
		this.simpleStyle = new HashMap<>( SIMPLE_VERTEX_STYLE );
		this.fullStyle = new HashMap<>( FULL_VERTEX_STYLE );
		this.edgeStyle = new HashMap<>( BASIC_EDGE_STYLE );

		// Prepare styles
		final mxStylesheet styleSheet = graphx.getStylesheet();
		styleSheet.setDefaultEdgeStyle( edgeStyle );
		styleSheet.setDefaultVertexStyle( simpleStyle );
		styleSheet.putCellStyle( FULL_STYLE_NAME, fullStyle );
		styleSheet.putCellStyle( SIMPLE_STYLE_NAME, simpleStyle );
	}

	public void setStyle( final String styleName )
	{
		if ( !graphx.getStylesheet().getStyles().containsKey( styleName ) )
			throw new IllegalArgumentException( "Unknown TrackScheme style: " + styleName );

		this.globalStyle = styleName;
	}

	/**
	 * Change the style of the edge cells to reflect the currently set color
	 * generator.
	 *
	 * @param edgeMap
	 *            the {@link mxCell} ordered by the track IDs they belong to.
	 */
	public synchronized void updateEdgeStyle( final Collection< mxCell > edges )
	{
		final FeatureColorGenerator< DefaultWeightedEdge > trackColorGenerator = FeatureUtils.createTrackColorGenerator( model, displaySettings );
		final Color missingValueColor = displaySettings.getMissingValueColor();
		graphx.getModel().beginUpdate();
		try
		{
			for ( final mxCell cell : edges )
			{
				final DefaultWeightedEdge edge = graphx.getEdgeFor( cell );
				Color color = trackColorGenerator.color( edge );
				if ( color == null )
					color = missingValueColor;

				final String colorstr = Integer.toHexString( color.getRGB() ).substring( 2 );
				String style = cell.getStyle();
				style = mxStyleUtils.setStyle( style, mxConstants.STYLE_STROKECOLOR, colorstr );
				style = mxStyleUtils.setStyle( style, mxConstants.STYLE_STROKEWIDTH, Float.toString( ( float ) displaySettings.getLineThickness() ) );
				graphx.getModel().setStyle( cell, style );
			}
		}
		finally
		{
			graphx.getModel().endUpdate();
		}
	}

	public void updateVertexStyle( final Collection< mxCell > vertices )
	{
		final Font font = displaySettings.getFont();
		fullStyle.put( mxConstants.STYLE_FONTFAMILY, font.getFamily() );
		fullStyle.put( mxConstants.STYLE_FONTSIZE, "" + font.getSize() );
		fullStyle.put( mxConstants.STYLE_FONTSTYLE, "" + font.getStyle() );
		
		final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, displaySettings );
		final Color missingValueColor = displaySettings.getMissingValueColor();

		graphx.getModel().beginUpdate();
		try
		{
			for ( final mxCell vertex : vertices )
			{
				final Spot spot = graphx.getSpotFor( vertex );
				if ( spot != null )
				{
					Color color = spotColorGenerator.color( spot );
					if ( color == null )
						color = missingValueColor;

					final String colorstr = Integer.toHexString( color.getRGB() ).substring( 2 );
					final String fillcolorstr = displaySettings.isTrackSchemeFillBox() ? colorstr : "white";
					setVertexStyle( vertex, colorstr, fillcolorstr );
				}
			}
		}
		finally
		{
			graphx.getModel().endUpdate();
		}
	}

	private void setVertexStyle( final mxICell vertex, final String colorstr, final String fillcolorstr )
	{
		String targetStyle = vertex.getStyle();
		targetStyle = mxStyleUtils.removeAllStylenames( targetStyle );
		targetStyle = mxStyleUtils.setStyle( targetStyle, mxConstants.STYLE_STROKECOLOR, colorstr );

		// Style specifics
		int width, height;
		if ( globalStyle.equals( SIMPLE_STYLE_NAME ) )
		{
			targetStyle = mxStyleUtils.setStyle( targetStyle, mxConstants.STYLE_FILLCOLOR, colorstr );
			width = DEFAULT_CELL_HEIGHT;
			height = width;
		}
		else
		{
			targetStyle = mxStyleUtils.setStyle( targetStyle, mxConstants.STYLE_FILLCOLOR, fillcolorstr );
			width = DEFAULT_CELL_WIDTH;
			height = DEFAULT_CELL_HEIGHT;
		}
		targetStyle = globalStyle + ";" + targetStyle;

		graphx.getModel().setStyle( vertex, targetStyle );
		graphx.getModel().getGeometry( vertex ).setWidth( width );
		graphx.getModel().getGeometry( vertex ).setHeight( height );
	}
}
