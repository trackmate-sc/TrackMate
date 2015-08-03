package fiji.plugin.trackmate.detection.findmaxima;

import net.imglib2.Localizable;
import net.imglib2.Point;

/**
 * Only purpose: overwriting the equal method...
 * @author Thorsten Wagner (twa@posteo.de)
 */
public class MyPoint extends Point {
	
	

	public MyPoint(final long... position ) {
		super(position, true );
	}
	
	public MyPoint(final Localizable localizable ) {
		super( localizable.numDimensions() );
		localizable.localize( position );
	}

	@Override
	public boolean equals(Object obj) {
		MyPoint p = (MyPoint) obj;
		boolean equal = true;
		for(int i = 0; i < p.n; i++){
			if(this.getIntPosition(i)!=p.getIntPosition(i)){
				equal=false;
				break;
			}
		}
		return equal;
	}

}
