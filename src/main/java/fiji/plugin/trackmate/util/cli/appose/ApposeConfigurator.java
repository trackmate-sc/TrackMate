package fiji.plugin.trackmate.util.cli.appose;

import java.util.List;

import fiji.plugin.trackmate.util.cli.Configurator;

public abstract class ApposeConfigurator extends Configurator
{

	/**
	 * Returns the Mamba YML environemnt file that can install the tool we want
	 * to run.
	 *
	 * @return the Mamba YML environment file content.
	 */
	protected abstract String getMambaYML();

	/**
	 * Returns the script template associated with this Appose tool.
	 *
	 * @return the script template.
	 */
	protected abstract String getScriptTemplate();

	/**
	 * Builds the actual script to run by replacing the argument placeholders in
	 * the script template with the selected argument values.
	 *
	 * @return the script to run.
	 */
	protected String makeScript()
	{
		final List< Argument< ?, ? > > args = getSelectedArguments();
		String template = getScriptTemplate();
		for ( final Argument< ?, ? > arg : args )
		{
			final Object value = arg.getValue();
			if ( null == value )
				continue;
			final String key = "${" + arg.getArgument() + "}";
			template = template.replace( key, value.toString() );
		}

		return template;
	}

	/**
	 * Utility method to load an Appose script template from resources.
	 *
	 * @param resourcePath
	 *            the resource path.
	 * @param cls
	 *            the class to use to load the resource.
	 * @return the script template.
	 */
	protected String loadScriptTemplateFromResources( final String resourcePath, final Class< ? > cls )
	{
		try
		{
			return new String( cls.getResourceAsStream( resourcePath ).readAllBytes() );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( "Failed to load Appose script template from resources: " + resourcePath, e );
		}
	}
}
