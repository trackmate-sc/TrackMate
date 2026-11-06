package fiji.plugin.trackmate.gui.displaysettings;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.scijava.ui.config.visitors.gui.elements.StyleElement;
import org.scijava.ui.config.visitors.gui.elements.StyleElementVisitor;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.CategoryJComboBox;
import fiji.plugin.trackmate.gui.components.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;

public class TrackMateStyleElements
{

	public static CategoryJComboBox< TrackMateObject, String > linkedFeatureSelector( final FeatureElement element )
	{
		final Settings settings = new Settings();
		settings.addAllAnalyzers();
		final CategoryJComboBox< TrackMateObject, String > selector = FeatureDisplaySelector.createComboBoxSelector( null, settings );
		selector.setSelectedItem( element.getFeature() );
		selector.addActionListener( e -> element.setValue( selector.getSelectedCategory(), selector.getSelectedItem() ) );
		element.onSet( ( type, feature ) -> {
			if ( !feature.equals( selector.getSelectedItem() ) )
				selector.setSelectedItem( feature );
		} );
		return selector;
	}

	public static FeatureElement featureElement( final String label, final Supplier< TrackMateObject > typeGet, final Supplier< String > featureGet, final BiConsumer< TrackMateObject, String > set )
	{
		return new FeatureElement( label )
		{

			@Override
			public void setValue( final TrackMateObject type, final String feature )
			{
				set.accept( type, feature );
			}

			@Override
			public TrackMateObject getType()
			{
				return typeGet.get();
			}

			@Override
			public String getFeature()
			{
				return featureGet.get();
			}

			@Override
			public void accept( final StyleElementVisitor visitor )
			{
				if ( visitor instanceof final TrackMateStyleElementVisitor extendedVisitor )
					extendedVisitor.visit( this );
				else
					throw new UnsupportedOperationException( "Visitor " + visitor.getClass().getName() + " does not support " + this.getClass().getName() );
			}
		};
	}
	
	public static interface TrackMateStyleElement extends StyleElement
	{
		void accept( TrackMateStyleElementVisitor visitor );
	}

	public static interface TrackMateStyleElementVisitor extends StyleElementVisitor
	{
		public default void visit( final FeatureElement element )
		{
			throw new UnsupportedOperationException();
		}
	}

	public static abstract class FeatureElement implements TrackMateStyleElement
	{
		private final ArrayList< BiConsumer< TrackMateObject, String > > onSet = new ArrayList<>();

		private final String label;

		public FeatureElement( final String label )
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final TrackMateStyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public void onSet( final BiConsumer< TrackMateObject, String > set )
		{
			onSet.add( set );
		}

		@Override
		public void update()
		{
			onSet.forEach( c -> c.accept( getType(), getFeature() ) );
		}

		public abstract TrackMateObject getType();

		public abstract String getFeature();

		public abstract void setValue( TrackMateObject type, String feature );
	}
}
