package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

public class SpotEditToolParams
{

	/*
	 * Semi-auto tracking parameters
	 */
	/**
	 * The fraction of the initial quality above which we keep new spots.
	 * The highest, the more intolerant.
	 */
	double qualityThreshold = 0.5;

	/**
	 * How close must be the new spot found to be accepted, in radius units.
	 */
	double distanceTolerance = 2d;

	/**
	 * We process at most nFrames. Make it 0 or negative to have no bounds.
	 */
	int nFrames = 10;

	/**
	 * By how many frames to jump when we do step-wide time browsing.
	 */
	int stepwiseTimeBrowsing = 1;

	@Override
	public String toString()
	{
		return super.toString() + ": " + "QualityThreshold = " + qualityThreshold + ", DistanceTolerance = " + distanceTolerance + ", nFrames = " + nFrames;
	}
}
