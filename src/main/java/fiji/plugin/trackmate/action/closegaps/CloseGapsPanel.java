package fiji.plugin.trackmate.action.closegaps;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import fiji.plugin.trackmate.gui.Fonts;

/**
 * A basic UI to let a TrackMate user choose between several techniques for gap
 * closing.
 * 
 * @author Jean-Yves Tinevez
 *
 */
public class CloseGapsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	final JComboBox< GapClosingMethod > cmbboxMethod;

	final JButton btnRun;

	public CloseGapsPanel( final Collection< GapClosingMethod > gapClosingMethods )
	{
		setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Close gap" );
		lblTitle.setFont( Fonts.BIG_FONT );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblTitle = new GridBagConstraints();
		gbcLblTitle.gridwidth = 2;
		gbcLblTitle.insets = new Insets( 0, 0, 5, 0 );
		gbcLblTitle.fill = GridBagConstraints.BOTH;
		gbcLblTitle.gridx = 0;
		gbcLblTitle.gridy = 0;
		add( lblTitle, gbcLblTitle );

		final JLabel lblDoc = new JLabel( CloseGapsAction.INFO_TEXT );
		lblDoc.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblDoc = new GridBagConstraints();
		gbcLblDoc.fill = GridBagConstraints.BOTH;
		gbcLblDoc.insets = new Insets( 0, 0, 5, 0 );
		gbcLblDoc.gridwidth = 2;
		gbcLblDoc.gridx = 0;
		gbcLblDoc.gridy = 1;
		add( lblDoc, gbcLblDoc );

		final JLabel lblMethod = new JLabel( "Gap-closing method" );
		lblMethod.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblMethod = new GridBagConstraints();
		gbcLblMethod.anchor = GridBagConstraints.EAST;
		gbcLblMethod.insets = new Insets( 0, 0, 5, 5 );
		gbcLblMethod.gridx = 0;
		gbcLblMethod.gridy = 2;
		add( lblMethod, gbcLblMethod );

		this.cmbboxMethod = new JComboBox<>( new Vector<>( gapClosingMethods ) );
		cmbboxMethod.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcCmbboxMethod = new GridBagConstraints();
		gbcCmbboxMethod.insets = new Insets( 0, 0, 5, 0 );
		gbcCmbboxMethod.fill = GridBagConstraints.HORIZONTAL;
		gbcCmbboxMethod.gridx = 1;
		gbcCmbboxMethod.gridy = 2;
		add( cmbboxMethod, gbcCmbboxMethod );

		final JLabel lblMethodDoc = new JLabel();
		lblMethodDoc.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblMethodDoc = new GridBagConstraints();
		gbcLblMethodDoc.fill = GridBagConstraints.BOTH;
		gbcLblMethodDoc.insets = new Insets( 0, 0, 5, 0 );
		gbcLblMethodDoc.gridwidth = 2;
		gbcLblMethodDoc.gridx = 0;
		gbcLblMethodDoc.gridy = 3;
		add( lblMethodDoc, gbcLblMethodDoc );

		this.btnRun = new JButton( "Run" );
		final GridBagConstraints gbcBtnRun = new GridBagConstraints();
		gbcBtnRun.anchor = GridBagConstraints.EAST;
		gbcBtnRun.gridx = 1;
		gbcBtnRun.gridy = 4;
		add( btnRun, gbcBtnRun );

		/*
		 * Listeners.
		 */

		cmbboxMethod.addActionListener( e -> lblMethodDoc.setText( ( ( GapClosingMethod ) cmbboxMethod.getSelectedItem() ).getInfoText() ) );
		cmbboxMethod.setSelectedIndex( 0 );
	}
}
