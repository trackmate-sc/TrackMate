package fiji.plugin.trackmate.gui.displaysettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import bdv.ui.settings.style.AbstractStyleManager;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;

public class DisplaySettingsManager extends AbstractStyleManager< DisplaySettingsManager, DisplaySettings >
{

	private static final String DISPLAY_SETTINGS_FOLDER = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "displaysettings" ).getAbsolutePath();

	private static final String SELECTED_STYLE_FILENAME = "selected.txt";

	private final DisplaySettings forwardDefaultStyle;

	private final DisplaySettings.UpdateListener updateForwardDefaultListeners;

	public DisplaySettingsManager()
	{
		this( null, true );
	}

	/**
	 * Creates a new DisplaySettingsManager.
	 * 
	 * @param styleToManage
	 *            the style that will be managed by this manager. If
	 *            <code>null</code>, the default style will be used.
	 * @param loadStyles
	 *            if <code>true</code>, the styles will be loaded from the
	 *            {@link #DISPLAY_SETTINGS_FOLDER} folder. If styleToManage is
	 *            <code>null</code>, the main style will be set to the style
	 *            that was loaded from the folder.
	 */
	public DisplaySettingsManager( final DisplaySettings styleToManage, final boolean loadStyles )
	{
		final boolean loadSelectedStyle = ( null == styleToManage );
		if ( loadSelectedStyle )
			forwardDefaultStyle = DisplaySettings.defaultStyle().copy();
		else
			forwardDefaultStyle = styleToManage;

		updateForwardDefaultListeners = () -> forwardDefaultStyle.set( selectedStyle );
		selectedStyle.listeners().add( updateForwardDefaultListeners );
		if ( loadStyles )
			loadStyles( loadSelectedStyle );
	}

	public DisplaySettings getForwardDefaultStyle()
	{
		return forwardDefaultStyle;
	}

	@Override
	public synchronized void setSelectedStyle( final DisplaySettings ds )
	{
		selectedStyle.listeners().remove( updateForwardDefaultListeners );
		selectedStyle = ds;
		forwardDefaultStyle.set( selectedStyle );
		selectedStyle.listeners().add( updateForwardDefaultListeners );
	}

	@Override
	protected List< DisplaySettings > loadBuiltinStyles()
	{
		final DisplaySettings ds1 = DisplaySettings.defaultStyle();
		final DisplaySettings ds2 = ds1.copy( "Dragon tail" );
		ds2.setLineThickness( 2. );
		ds2.setTrackDisplayMode( TrackDisplayMode.LOCAL_BACKWARD );
		return List.of( ds1, ds2 );
	}

	public void loadStyles( final boolean loadSelectedStyle )
	{
		loadStyles( DISPLAY_SETTINGS_FOLDER, loadSelectedStyle );
	}

	@Override
	public void saveStyles()
	{
		saveStyles( DISPLAY_SETTINGS_FOLDER );
	}

	public void loadStyles( final String folder, final boolean loadSelectedStyle )
	{
		// Load the selected style name from the text file
		final File selectedFile = new File( folder, SELECTED_STYLE_FILENAME );
		String selectedName = null;
		try
		{
			selectedName = Files.readString( selectedFile.toPath() ).trim();
		}
		catch ( final IOException e )
		{}

		if ( loadSelectedStyle )
			setSelectedStyle( builtinStyles.get( 0 ) );

		userStyles.clear();
		final Set< String > names = builtinStyles.stream().map( DisplaySettings::getName ).collect( Collectors.toSet() );
		
		// Get all JSon files in the folder
		final File[] files = new File( folder ).listFiles( ( dir, name ) -> name.toLowerCase().endsWith( ".json" ) );
		if ( files == null )
			return;

		// Read each file and add it if it is valid and has a unique name.
		for ( final File file : files )
		{
			final DisplaySettings ds = DisplaySettingsIO.read( file.getAbsolutePath() );
			if ( ds == null )
				continue;
			if ( names.contains( ds.getName() ) )
			{
				System.err.println( "Discarded settings with duplicate name \"" + ds.getName() + "\"." );
				continue;
			}
			userStyles.add( ds );
			if ( ds.getName().equals( selectedName ) && loadSelectedStyle )
				setSelectedStyle( ds );
		}

		if ( loadSelectedStyle )
		{
			for ( final DisplaySettings ds : builtinStyles )
			{
				if ( ds.getName().equals( selectedName ) )
					setSelectedStyle( ds );
			}
		}
		else
		{
			final DisplaySettings copy = forwardDefaultStyle.copy();
			userStyles.removeIf( ds -> ds.getName().equals( copy.getName() ) );
			userStyles.add( copy );
			setSelectedStyle( copy );
		}
	}

	public void saveStyles( final String folder )
	{
		new File( folder ).mkdirs();

		// Save what style is selected in a text file
		final File selectedFile = new File( folder, SELECTED_STYLE_FILENAME );
		try
		{
			Files.writeString( selectedFile.toPath(), selectedStyle.getName() );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}

		// List all json files in the folder and delete those that do not
		// correspond to a user style.
		final File[] files = new File( folder ).listFiles( ( dir, name ) -> name.toLowerCase().endsWith( ".json" ) );
		if ( files != null )
		{
			final Set< String > userStyleNames = userStyles.stream().map( DisplaySettings::getName ).collect( Collectors.toSet() );
			for ( final File file : files )
			{
				final String filename = file.getName().substring( 0, file.getName().length() - 5 );
				if ( !userStyleNames.contains( filename ) )
					file.delete();
			}
		}

		// Save all user styles to the folder
		for ( final DisplaySettings ds : userStyles )
			DisplaySettingsIO.write( ds, new File( folder, ds.getName() + ".json" ).getAbsolutePath() );
	}
}
