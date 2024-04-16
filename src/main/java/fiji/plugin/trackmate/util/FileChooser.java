/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.util;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class FileChooser
{
	public static boolean useJFileChooser = !isMac();

	public static enum DialogType
	{
		LOAD, SAVE
	}

	public static enum SelectionMode
	{
		FILES_ONLY, DIRECTORIES_ONLY, FILES_AND_DIRECTORIES
	}

	public static File chooseFile(
			final Component parent,
			final String selectedFile,
			final DialogType dialogType )
	{
		return chooseFile( parent, selectedFile, null, null, dialogType );
	}

	public static File chooseFile(
			final Component parent,
			final String selectedFile,
			final FileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType )
	{
		return chooseFile( parent, selectedFile, fileFilter, dialogTitle, dialogType, SelectionMode.FILES_ONLY );
	}

	public static File chooseFile(
			final Component parent,
			final String selectedFile,
			final FileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType,
			final SelectionMode selectionMode )
	{
		return chooseFile( useJFileChooser, parent, selectedFile, fileFilter, dialogTitle, dialogType, selectionMode );
	}

	public static File chooseFile(
			boolean useJFileChooser,
			final Component parent,
			final String selectedFile,
			final FileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType,
			final SelectionMode selectionMode )
	{
		final boolean isSaveDialog = ( dialogType == DialogType.SAVE );
		final boolean isDirectoriesOnly = ( selectionMode == SelectionMode.DIRECTORIES_ONLY );

		if ( isSaveDialog && isDirectoriesOnly )
			useJFileChooser = true; // FileDialog cannot handle this

		/*
		 * Determine dialog title:
		 *
		 * If a dialogTitle is given, just use that.
		 *
		 * Otherwise, use "Open" or "Save", depending on DialogType. If a
		 * FileFilter is provided, append the FileFilter description,
		 * leading to "Open xml files" or similar.
		 */
		String title = dialogTitle;
		if ( title == null )
			title = ( isSaveDialog ? "Save" : "Open" )
					+ ( fileFilter == null ? "" : " " + fileFilter.getDescription() );

		File file = null;
		if ( useJFileChooser )
		{
			final JFileChooser fileChooser = new JFileChooser();

			fileChooser.setDialogTitle( title );

			if ( selectedFile != null )
				fileChooser.setSelectedFile( new File( selectedFile ) );

			switch ( selectionMode )
			{
			case FILES_ONLY:
				fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
				break;
			case DIRECTORIES_ONLY:
				fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
				break;
			case FILES_AND_DIRECTORIES:
				fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
				break;
			}

			fileChooser.setFileFilter( fileFilter );

			final int returnVal = isSaveDialog
					? fileChooser.showSaveDialog( parent )
					: fileChooser.showOpenDialog( parent );
			if ( returnVal == JFileChooser.APPROVE_OPTION )
				file = fileChooser.getSelectedFile();
		}
		else // use FileDialog
		{
			final int fdMode = isSaveDialog ? FileDialog.SAVE : FileDialog.LOAD;

			/*
			 * If provided parent is a Frame or a Dialog, we can use it.
			 * Otherwise use null as parent.
			 */
			final FileDialog fd;
			if ( parent != null && parent instanceof Frame )
				fd = new FileDialog( ( Frame ) parent, title, fdMode );
			else if ( parent != null && parent instanceof Dialog )
				fd = new FileDialog( ( Dialog ) parent, title, fdMode );
			else
				fd = new FileDialog( ( Frame ) null, title, fdMode );

			/*
			 * If a selectedFile path was provided, set it.
			 */
			if ( selectedFile != null )
			{
				if ( isDirectoriesOnly )
				{
					fd.setDirectory( selectedFile );
					fd.setFile( null );
				}
				else
				{
					fd.setDirectory( new File( selectedFile ).getParent() );
					fd.setFile( new File( selectedFile ).getName() );
				}
			}

			/*
			 * Handle SelectionMode DIRECTORIES_ONLY.
			 */
			System.setProperty( "apple.awt.fileDialogForDirectories", isDirectoriesOnly ? "true" : "false" );

			/*
			 * Try with a FilenameFilter (may silently fail).
			 */
			final AtomicBoolean workedWithFilenameFilter = new AtomicBoolean( false );
			if ( fileFilter != null )
			{
				final FilenameFilter filenameFilter = new FilenameFilter()
				{
					private boolean firstTime = true;

					@Override
					public boolean accept( final File dir, final String name )
					{
						if ( firstTime )
						{
							workedWithFilenameFilter.set( true );
							firstTime = false;
						}

						return fileFilter.accept( new File( dir, name ) );
					}
				};
				fd.setFilenameFilter( filenameFilter );
				fd.setVisible( true );
			}
			if ( fileFilter == null || ( isMac() && !workedWithFilenameFilter.get() ) )
			{
				fd.setFilenameFilter( null );
				fd.setVisible( true );
			}

			final String filename = fd.getFile();
			if ( filename != null )
			{
				file = new File( fd.getDirectory() + filename );
			}
		}

		return file;
	}

	private static boolean isMac()
	{
		final String OS = System.getProperty( "os.name", "generic" ).toLowerCase( Locale.ENGLISH );
		return ( OS.indexOf( "mac" ) >= 0 ) || ( OS.indexOf( "darwin" ) >= 0 );
	}
}
