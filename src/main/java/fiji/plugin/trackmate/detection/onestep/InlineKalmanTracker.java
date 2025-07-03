package fiji.plugin.trackmate.detection.onestep;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class InlineKalmanTracker< T extends RealType< T > & NativeType< T > > implements SpotDetectorTracker< T >
{

	@Override
	public SpotDetectorTrackerOutput getResult()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkInput()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean process()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getErrorMessage()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNumThreads()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int getNumThreads()
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
