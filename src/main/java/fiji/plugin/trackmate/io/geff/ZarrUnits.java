package fiji.plugin.trackmate.io.geff;

public class ZarrUnits
{

	/**
	 * Maps common unit abbreviations to OME-Zarr compliant names. See
	 * https://ngff.openmicroscopy.org/latest/#axes-md for the valid set.
	 */
	public static String normalizeSpaceUnit( final String unit )
	{
		if ( unit == null || unit.isEmpty() )
			return "pixel";
		switch ( unit.trim() )
		{
		case "um":
		case "µm":
		case "μm":
		case "micron":
		case "microns":
			return "micrometer";
		case "nm":
			return "nanometer";
		case "mm":
			return "millimeter";
		case "cm":
			return "centimeter";
		case "m":
			return "meter";
		case "km":
			return "kilometer";
		case "pm":
			return "picometer";
		case "Å":
			return "angstrom";
		default:
			return unit.trim();
		}
	}

	/**
	 * Maps common unit abbreviations to OME-Zarr compliant names. See
	 * https://ngff.openmicroscopy.org/latest/#axes-md for the valid set.
	 */
	public static String normalizeTimeUnit( final String unit )
	{
		if ( unit == null || unit.isEmpty() )
			return "frame";
		switch ( unit.trim() )
		{
		case "as":
			return "attosecond";
		case "cs":
			return "centisecond";
		case "ds":
			return "decisecond";
		case "Es":
			return "exasecond";
		case "fs":
			return "femtosecond";
		case "Gs":
			return "gigasecond";
		case "hs":
			return "hectosecond";
		// hour
		case "h":
		case "hr":
		case "hrs":
			return "hour";
		case "ks":
			return "kilosecond";
		case "Ms":
			return "megasecond";
		// microsecond
		case "us":
		case "µs":
		case "μs":
			return "microsecond";
		case "ms":
			return "millisecond";
		// minute
		case "min":
		case "mins":
			return "minute";
		case "ns":
			return "nanosecond";
		case "Ps":
			return "petasecond";
		case "ps":
			return "picosecond";
		// second
		case "s":
		case "sec":
		case "secs":
			return "second";
		case "Ts":
			return "terasecond";
		case "ys":
			return "yoctosecond";
		case "Ys":
			return "yottasecond";
		case "zs":
			return "zeptosecond";
		case "Zs":
			return "zettasecond";
		default:
			return unit.trim();
		}
	}

}
