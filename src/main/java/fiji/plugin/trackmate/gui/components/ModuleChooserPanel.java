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

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
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
			info.setText( factory.getInfoText() );
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

		@Override
		public Component getListCellRendererComponent( final JList< ? > list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
		{
			final JLabel lbl = ( JLabel ) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			final String key = ( String ) value;
			final K factory = provider.getFactory( key );
			lbl.setIcon( factory.getIcon() );
			lbl.setText( factory.getName() );
			return lbl;
		}
	}
}
