package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.function.Consumer;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import net.imglib2.Interval;
import sc.fiji.labkit.ui.bdv.BdvLayer;
import sc.fiji.labkit.ui.bdv.BdvLayerLink;
import sc.fiji.labkit.ui.bdv.BdvShowable;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.utils.Notifier;

/**
 * Cope of the {@link BdvLayerLink} to avoid the shift-1 shortcut in BDV
 * toggling ALL the channels.
 */
public class TMBdvLayerLink implements Holder< BdvStackSource< ? > >
{

	private final BdvHandle handle;

	private final BdvLayer layer;

	private final Notifier notifier = new Notifier();

	private final Runnable onImageChanged = this::onImageChanged;

	private final Runnable onVisibilityChanged = this::updateBdv;

	private final Consumer< Interval > onRequestRepaint = this::onRequestRepaint;

	private BdvStackSource< ? > bdvSource;

	public TMBdvLayerLink( final BdvLayer layer, final BdvHandle handle )
	{
		this.handle = handle;
		this.layer = layer;
		final BdvOptions options = BdvOptions.options().addTo( handle );
		final Holder< BdvShowable > image = layer.image();
		final BdvShowable showable1 = image.get();
		bdvSource = showable1 != null ? showable1.show( layer.title(), options ) : null;
		image.notifier().addWeakListener( onImageChanged );
		layer.listeners().addWeakListener( onRequestRepaint );
		layer.visibility().notifier().addListener( onVisibilityChanged );
	}

	private void onImageChanged()
	{
		final BdvStackSource< ? > source1 = bdvSource;
		bdvSource = null;
		if ( source1 != null )
			source1.removeFromBdv();
		final BdvShowable showable = layer.image().get();
		if ( showable != null )
		{
			bdvSource = showable.show( layer.title(), BdvOptions.options().addTo( handle ) );
			bdvSource.setActive( layer.visibility().get() );
		}
		updateBdv();
		notifier.notifyListeners();
	}

	private void onRequestRepaint( final Interval interval )
	{
		if ( interval == null )
			handle.getViewerPanel().requestRepaint();
		else
			handle.getViewerPanel().requestRepaint( interval );
	}

	private void updateBdv()
	{
		if ( bdvSource != null )
			try
			{
				bdvSource.setActive( layer.visibility().get() );
			}
			catch ( final NullPointerException ignore )
			{

			}
	}

	@Override
	public void set( final BdvStackSource< ? > value )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public BdvStackSource< ? > get()
	{
		return bdvSource;
	}

	@Override
	public Notifier notifier()
	{
		return notifier;
	}
}
