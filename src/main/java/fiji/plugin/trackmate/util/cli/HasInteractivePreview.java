package fiji.plugin.trackmate.util.cli;

/**
 * Interface for {@link Configurator}s which settings can be previewed with
 * {@link fiji.plugin.trackmate.util.DetectionPreview}.
 */
public interface HasInteractivePreview
{

	/**
	 * Declares the argument key and axis label to be used in the
	 * {@link fiji.plugin.trackmate.util.DetectionPreview} GUI.
	 * 
	 * @return argumentKey the argument key. This is the key used in the
	 *         {@link fiji.plugin.trackmate.util.cli.Configurator.Argument#getKey()}.
	 */
	public default String getPreviewArgumentKey()
	{
		return null;
	}

	/**
	 * Declares the axis label to be used in the
	 * {@link fiji.plugin.trackmate.util.DetectionPreview} GUI.
	 * 
	 * @return axisLabel the label to be used for the axis in the detection
	 *         preview histogram.
	 */
	public default String getPreviewAxisLabel()
	{
		return null;
	}
}
