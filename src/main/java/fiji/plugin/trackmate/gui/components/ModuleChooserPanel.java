/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.providers.AbstractProvider;

public class ModuleChooserPanel< K extends TrackMateModule > extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final AbstractProvider< K > provider;

	private final JComboBox< String > cmbbox;

	public ModuleChooserPanel( final AbstractProvider< K > provider, final String typeName, final String selectedKey )
	{
		this.provider = provider;

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 430, 0 };
		gridBagLayout.rowHeights = new int[] { 16, 27, 209, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblHeader = new JLabel();
		final GridBagConstraints gbcLblHeader = new GridBagConstraints();
		gbcLblHeader.fill = GridBagConstraints.BOTH;
		gbcLblHeader.insets = new Insets( 5, 5, 5, 5 );
		gbcLblHeader.gridx = 0;
		gbcLblHeader.gridy = 0;
		this.add( lblHeader, gbcLblHeader );
		lblHeader.setFont( BIG_FONT );
		lblHeader.setText( "<html>"
				+ "Select "
				+ ( startWithVowel( typeName ) ? "an " : "a " )
				+ "<b>" + typeName + "</b>"
				+ "</html>" );

		cmbbox = new JComboBox<>();
		cmbbox.setModel( new DefaultComboBoxModel<>( provider.getVisibleKeys().toArray( new String[] {} ) ) );
		cmbbox.setRenderer( new MyListCellRenderer() );
		final GridBagConstraints gbcCmbbox = new GridBagConstraints();
		gbcCmbbox.fill = GridBagConstraints.BOTH;
		gbcCmbbox.insets = new Insets( 5, 5, 5, 5 );
		gbcCmbbox.gridx = 0;
		gbcCmbbox.gridy = 1;
		this.add( cmbbox, gbcCmbbox );
		cmbbox.setFont( FONT );

		final JEditorPane info = GuiUtils.infoDisplay();
		final GridBagConstraints gbcLblInfo = new GridBagConstraints();
		gbcLblInfo.insets = new Insets( 5, 5, 5, 5 );
		gbcLblInfo.fill = GridBagConstraints.BOTH;
		gbcLblInfo.gridx = 0;
		gbcLblInfo.gridy = 2;
		this.add( GuiUtils.textInScrollPanel( info ), gbcLblInfo );

		cmbbox.addActionListener( e -> {
			final K factory = provider.getFactory( ( String ) cmbbox.getSelectedItem() );
			String infoText = factory.getInfoText();
			final String url = factory.getUrl();
			if ( url != null )
			{
				final String urlSpaced = url.replaceAll( "/", "/<wbr>" );
				infoText = infoText.replace( "</html>", "<p>"
						+ "Documentation online: "
						+ "<br >"
						+ "<a href=" + url + ">"
						+ urlSpaced
						+ "</a>"
						+ "</html>" );
			}
			info.setText( infoText );
		} );

		cmbbox.setSelectedItem( selectedKey );
	}

	public void setSelectedModuleKey( final String moduleKey )
	{
		cmbbox.setSelectedItem( moduleKey );
	}

	public String getSelectedModuleKey()
	{
		return ( String ) cmbbox.getSelectedItem();
	}

	private static final boolean startWithVowel( final String word )
	{
		return "eaiouEAIOU".indexOf( word.charAt( 0 ) ) >= 0;
	}

	private final class MyListCellRenderer extends DefaultListCellRenderer
	{

		private static final long serialVersionUID = 1L;

		private final int fixedHeight;

		public MyListCellRenderer()
		{
			final JLabel label = new JLabel( "Sample Text" );
			this.fixedHeight = label.getPreferredSize().height;
		}

		@Override
		public Component getListCellRendererComponent( final JList< ? > list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
		{
			final JLabel lbl = ( JLabel ) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			final String key = ( String ) value;
			final K factory = provider.getFactory( key );
			final ImageIcon icon = factory.getIcon();
			if ( icon != null )
				lbl.setIcon( new CroppedIcon( icon, fixedHeight ) );
			lbl.setText( factory.getName() );
			lbl.setPreferredSize( new Dimension( getPreferredSize().width, fixedHeight ) );
			return lbl;
		}
	}

	/**
	 * Displays the icon in the center of the label, cropped to a fixed height.
	 */
	private static class CroppedIcon implements Icon
	{
		private final Icon icon;

		private final int height;

		public CroppedIcon( final Icon icon, final int height )
		{
			this.icon = icon;
			this.height = height;
		}

		@Override
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
			final int iconWidth = icon.getIconWidth();
			final int iconHeight = icon.getIconHeight();

			// With 1 pixel padding top and bottom
			final int cropY = ( iconHeight - height + 2 ) / 2;
			final int cropHeight = height - 1;
			g.drawImage( ( ( ImageIcon ) icon ).getImage(), x, y + 1, x + iconWidth, y + cropHeight, 0, cropY, iconWidth, cropY + cropHeight, null );
		}

		@Override
		public int getIconWidth()
		{
			return icon.getIconWidth();
		}

		@Override
		public int getIconHeight()
		{
			return height;
		}
	}
}
