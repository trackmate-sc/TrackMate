package fiji.plugin.trackmate.features;

/**
 * A helper class to store a feature filter. It is just made of 3 public fields.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Sep 23, 2010
 *
 */
public class FeatureFilter
{
	public final String feature;

	public final Double value;

	public final boolean isAbove;

	public FeatureFilter( final String feature, final Double value, final boolean isAbove )
	{
		this.feature = feature;
		this.value = value;
		this.isAbove = isAbove;
	}

	public FeatureFilter( final String feature, final double value, final boolean isAbove )
	{
		this( feature, Double.valueOf( value ), isAbove );
	}

	@Override
	public String toString()
	{
		String str = feature.toString();
		if ( isAbove )
			str += " > ";
		else
			str += " < ";
		str += value.toString();
		return str;
	}

}
