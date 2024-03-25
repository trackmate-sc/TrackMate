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
package fiji.plugin.trackmate.action;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class CaptureOverlayPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private int firstFrame;

	private int lastFrame;

	private boolean hideImage;

	private boolean whiteBackground;

	public CaptureOverlayPanel( final int firstFrame, final int lastFrame, final boolean hideImage, boolean whiteBackground )
	{
		this.firstFrame = firstFrame;
		this.lastFrame = lastFrame;
		this.hideImage = hideImage;
		this.whiteBackground = whiteBackground;

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblFirstFrame = new JLabel( "First frame:" );
		final GridBagConstraints gbcLblFirstFrame = new GridBagConstraints();
		gbcLblFirstFrame.anchor = GridBagConstraints.EAST;
		gbcLblFirstFrame.insets = new Insets( 0, 0, 5, 5 );
		gbcLblFirstFrame.gridx = 0;
		gbcLblFirstFrame.gridy = 0;
		add( lblFirstFrame, gbcLblFirstFrame );

		final JFormattedTextField tftFirst = new JFormattedTextField( NumberFormat.getIntegerInstance() );
		tftFirst.setValue( Integer.valueOf( firstFrame ) );
		tftFirst.setColumns( 5 );
		final GridBagConstraints gbcTftFirst = new GridBagConstraints();
		gbcTftFirst.anchor = GridBagConstraints.NORTH;
		gbcTftFirst.insets = new Insets( 0, 0, 5, 0 );
		gbcTftFirst.fill = GridBagConstraints.HORIZONTAL;
		gbcTftFirst.gridx = 1;
		gbcTftFirst.gridy = 0;
		add( tftFirst, gbcTftFirst );

		final JLabel lblLastFrame = new JLabel( "Last frame:" );
		final GridBagConstraints gbcLblLastFrame = new GridBagConstraints();
		gbcLblLastFrame.anchor = GridBagConstraints.EAST;
		gbcLblLastFrame.insets = new Insets( 0, 0, 5, 5 );
		gbcLblLastFrame.gridx = 0;
		gbcLblLastFrame.gridy = 1;
		add( lblLastFrame, gbcLblLastFrame );

		final JFormattedTextField tftLast = new JFormattedTextField( NumberFormat.getIntegerInstance() );
		tftLast.setValue( Integer.valueOf( lastFrame ) );
		tftLast.setColumns( 5 );
		final GridBagConstraints gbcTftLast = new GridBagConstraints();
		gbcTftLast.insets = new Insets( 0, 0, 5, 0 );
		gbcTftLast.fill = GridBagConstraints.HORIZONTAL;
		gbcTftLast.gridx = 1;
		gbcTftLast.gridy = 1;
		add( tftLast, gbcTftLast );

		final JLabel lblHideImage = new JLabel( "Hide image:" );
		final GridBagConstraints gbcLblHideImage = new GridBagConstraints();
		gbcLblHideImage.anchor = GridBagConstraints.EAST;
		gbcLblHideImage.insets = new Insets( 0, 0, 5, 5 );
		gbcLblHideImage.gridx = 0;
		gbcLblHideImage.gridy = 2;
		add( lblHideImage, gbcLblHideImage );

		final JCheckBox chckbxHideImage = new JCheckBox();
		chckbxHideImage.setSelected( hideImage );
		final GridBagConstraints gbcChckbxHideImage = new GridBagConstraints();
		gbcChckbxHideImage.anchor = GridBagConstraints.WEST;
		gbcChckbxHideImage.insets = new Insets( 0, 0, 5, 0 );
		gbcChckbxHideImage.gridx = 1;
		gbcChckbxHideImage.gridy = 2;
		add( chckbxHideImage, gbcChckbxHideImage );

		final JLabel lblWhiteBackground = new JLabel( "White background:" );
		lblWhiteBackground.setEnabled( hideImage );
		final GridBagConstraints gbcLblWhiteBackground = new GridBagConstraints();
		gbcLblWhiteBackground.anchor = GridBagConstraints.EAST;
		gbcLblWhiteBackground.insets = new Insets( 0, 0, 5, 5 );
		gbcLblWhiteBackground.gridx = 0;
		gbcLblWhiteBackground.gridy = 3;
		add( lblWhiteBackground, gbcLblWhiteBackground );

		final JCheckBox chckbxWhiteBackground = new JCheckBox();
		chckbxWhiteBackground.setSelected( whiteBackground );
		chckbxWhiteBackground.setEnabled( hideImage );
		final GridBagConstraints gbcChckbxWhiteBackground = new GridBagConstraints();
		gbcChckbxWhiteBackground.anchor = GridBagConstraints.WEST;
		gbcChckbxWhiteBackground.insets = new Insets( 0, 0, 5, 0 );
		gbcChckbxWhiteBackground.gridx = 1;
		gbcChckbxWhiteBackground.gridy = 3;
		add( chckbxWhiteBackground, gbcChckbxWhiteBackground );

		final FocusListener fl = new FocusAdapter()
		{
			@Override
			public void focusGained( final FocusEvent e )
			{
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						( ( JFormattedTextField ) e.getSource() ).selectAll();
					}
				} );
			}
		};
		tftFirst.addFocusListener( fl );
		tftLast.addFocusListener( fl );

		tftFirst.addPropertyChangeListener( "value", ( e ) -> this.firstFrame = ( ( Number ) tftFirst.getValue() ).intValue() );
		tftLast.addPropertyChangeListener( "value", ( e ) -> this.lastFrame = ( ( Number ) tftLast.getValue() ).intValue() );
		chckbxHideImage.addActionListener( e -> {
			this.hideImage = chckbxHideImage.isSelected();
			chckbxWhiteBackground.setEnabled( this.hideImage );
			lblWhiteBackground.setEnabled( this.hideImage );
		} );
		chckbxWhiteBackground.addActionListener( e -> this.whiteBackground = chckbxWhiteBackground.isSelected() );
	}

	public int getFirstFrame()
	{
		return firstFrame;
	}

	public int getLastFrame()
	{
		return lastFrame;
	}

	public boolean isHideImage()
	{
		return hideImage;
	}

	public boolean isWhiteBackground()
	{
		return whiteBackground;
	}
}
