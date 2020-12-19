package fiji.plugin.trackmate.features;

/**
 * A helper class to store a feature filter. It is just made of 3 public fields.
 * <p>
 * Sep 23, 2010, revised in December 2020.
 * 
 * @author Jean-Yves Tinevez
 */
public class FeatureFilter
{
	public final String feature;

	public final double value;

	public final boolean isAbove;

	public FeatureFilter( final String feature, final double value, final boolean isAbove )
	{
		this.feature = feature;
		this.value = value;
		this.isAbove = isAbove;
	}

	@Override
	public String toString()
	{
		String str = feature.toString();
		if ( isAbove )
			str += " > ";
		else
			str += " < ";
		str += "" + value;
		return str;
	}

}
