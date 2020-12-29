package fiji.plugin.trackmate;

public enum Dimension {
	NONE,
	QUALITY,
	COST,
	INTENSITY,
	INTENSITY_SQUARED,
	POSITION,
	VELOCITY,
	LENGTH,   // we separate length and position so that x,y,z are plotted on a different graph from spot sizes
	AREA,
	TIME,
	ANGLE,
	RATE, // count per frames
	ANGLE_RATE,
	STRING; // for non-numeric features
}
