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
package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Icons.EXECUTE_ICON;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.providers.ActionProvider;
import fiji.plugin.trackmate.util.Threads;

public class ActionChooserPanel extends ModuleChooserPanel< TrackMateActionFactory >
{

	private static final long serialVersionUID = 1L;

	public ActionChooserPanel( final ActionProvider actionProvider, final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		super( actionProvider, "action", CaptureOverlayAction.KEY );
		
		final GridBagLayout gridBagLayout = ( GridBagLayout ) getLayout();
		gridBagLayout.rowHeights = new int[] { 16, 27, 209, 200 };

		final LogPanel logPanel = new LogPanel();
		final GridBagConstraints gbcLogPanel = new GridBagConstraints();
		gbcLogPanel.insets = new Insets( 5, 5, 5, 5 );
		gbcLogPanel.fill = GridBagConstraints.BOTH;
		gbcLogPanel.gridx = 0;
		gbcLogPanel.gridy = 3;
		this.add( logPanel, gbcLogPanel );

		final JButton executeButton = new JButton( "Execute", EXECUTE_ICON );
		executeButton.setFont( FONT );
		final GridBagConstraints gbcExecBtn = new GridBagConstraints();
		gbcExecBtn.insets = new Insets( 5, 5, 5, 5 );
		gbcExecBtn.fill = GridBagConstraints.NONE;
		gbcExecBtn.anchor = GridBagConstraints.EAST;
		gbcExecBtn.gridx = 0;
		gbcExecBtn.gridy = 4;
		this.add( executeButton, gbcExecBtn );

		final Logger logger = logPanel.getLogger();
		executeButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				Threads.run( "TrackMate action thread", () ->
				{
					try
					{
						executeButton.setEnabled( false );
						final String actionKey = ActionChooserPanel.this.getSelectedModuleKey();
						final TrackMateAction action = actionProvider.getFactory( actionKey ).create();
						if ( null == action )
						{
							logger.error( "Unknown action: " + actionKey + ".\n" );
						}
						else
						{
							action.setLogger( logger );
							action.execute(
									trackmate,
									selectionModel,
									displaySettings,
									( JFrame ) SwingUtilities.getWindowAncestor( ActionChooserPanel.this ) );
						}
					}
					finally
					{
						executeButton.setEnabled( true );
					}
				} );
			}
		} );
	}
}
