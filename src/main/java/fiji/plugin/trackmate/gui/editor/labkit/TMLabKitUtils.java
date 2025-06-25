package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.concurrent.atomic.AtomicBoolean;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedIntType;

public class TMLabKitUtils
{

	public static final boolean isDifferent(
			final RandomAccessibleInterval< UnsignedIntType > previousIndexImg,
			final RandomAccessibleInterval< UnsignedIntType > indexImg )
	{
		final AtomicBoolean modified = new AtomicBoolean( false );
		LoopBuilder.setImages( previousIndexImg, indexImg )
				.multiThreaded()
				.forEachChunk( chunk -> {
					if ( modified.get() )
						return null;
					chunk.forEachPixel( ( p1, p2 ) -> {
						if ( p1.getInteger() != p2.getInteger() )
						{
							modified.set( true );
							return;
						}
					} );
					return null;
				} );
		return modified.get();
	}
}
