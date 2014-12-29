package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import fiji.plugin.trackmate.visualization.MinMaxAdjustable;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class SetColorScaleDialog extends JDialog implements MinMaxAdjustable
{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final JPanel contentPanel = new JPanel();

	private final ButtonGroup buttonGroup = new ButtonGroup();

	private final JNumericTextField textFieldMin;

	private final JNumericTextField textFieldMax;

	private final JRadioButton rdbtnAutomaticScale;

	protected boolean userPressedOk;

	public SetColorScaleDialog( final Frame parent, final String title, final MinMaxAdjustable toAdjust )
	{
		super( parent, true );
		setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );

		setResizable( false );
		setBounds( 100, 100, 300, 300 );
		getContentPane().setLayout( new BorderLayout() );
		contentPanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		getContentPane().add( contentPanel, BorderLayout.CENTER );
		contentPanel.setLayout( new BorderLayout( 0, 0 ) );

		final JLabel lblTitle = new JLabel( title );
		lblTitle.setFont( BIG_FONT );
		contentPanel.add( lblTitle, BorderLayout.NORTH );

		final JPanel panel = new JPanel();
		panel.setBorder( null );
		contentPanel.add( panel, BorderLayout.CENTER );
		panel.setLayout( null );

		rdbtnAutomaticScale = new JRadioButton( "automatic scale" );
		rdbtnAutomaticScale.setFont( BIG_FONT );
		rdbtnAutomaticScale.setBounds( 6, 24, 278, 23 );
		panel.add( rdbtnAutomaticScale );
		rdbtnAutomaticScale.setSelected( toAdjust.isAutoMinMaxMode() );
		buttonGroup.add( rdbtnAutomaticScale );

		final JRadioButton rdbtnManualScale = new JRadioButton( "manual scale" );
		rdbtnManualScale.setFont( BIG_FONT );
		rdbtnManualScale.setBounds( 6, 77, 278, 23 );
		rdbtnManualScale.setSelected( !toAdjust.isAutoMinMaxMode() );
		panel.add( rdbtnManualScale );
		buttonGroup.add( rdbtnManualScale );


		final JLabel lblMin = new JLabel( "Min:" );
		lblMin.setHorizontalAlignment( SwingConstants.TRAILING );
		lblMin.setFont( FONT.deriveFont( 11f ) );
		lblMin.setBounds( 35, 11, 90, 16 );

		textFieldMin = new JNumericTextField( toAdjust.getMin() );
		textFieldMin.setFont( FONT.deriveFont( 11f ) );
		textFieldMin.setHorizontalAlignment( SwingConstants.TRAILING );
		textFieldMin.setBounds( 137, 8, 50, 20 );
		textFieldMin.setColumns( 5 );

		final JLabel lblMax = new JLabel( "Max:" );
		lblMax.setHorizontalAlignment( SwingConstants.TRAILING );
		lblMax.setFont( FONT.deriveFont( 11f ) );
		lblMax.setBounds( 35, 60, 90, 16 );

		textFieldMax = new JNumericTextField( toAdjust.getMax() );
		textFieldMax.setFont( FONT.deriveFont( 11f ) );
		textFieldMax.setHorizontalAlignment( SwingConstants.TRAILING );
		textFieldMax.setBounds( 137, 57, 50, 20 );
		textFieldMax.setColumns( 5 );

		final JPanel panelManualScale = new JPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void setEnabled( final boolean enabled )
			{
				super.setEnabled( enabled );
				lblMin.setEnabled( enabled );
				lblMax.setEnabled( enabled );
				textFieldMin.setEnabled( enabled );
				textFieldMax.setEnabled( enabled );
			}
		};
		panelManualScale.setBorder( null );
		panelManualScale.setBounds( 50, 112, 234, 95 );
		panel.add( panelManualScale );
		panelManualScale.setLayout( null );
		panelManualScale.add( lblMin );
		panelManualScale.add( textFieldMin );
		panelManualScale.add( lblMax );
		panelManualScale.add( textFieldMax );

		rdbtnManualScale.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				panelManualScale.setEnabled( rdbtnManualScale.isSelected() );
			}
		} );
		rdbtnAutomaticScale.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				panelManualScale.setEnabled( rdbtnManualScale.isSelected() );
			}
		} );
		panelManualScale.setEnabled( !toAdjust.isAutoMinMaxMode() );

		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout( new FlowLayout( FlowLayout.RIGHT ) );
			getContentPane().add( buttonPane, BorderLayout.SOUTH );
			{
				final JButton okButton = new JButton( "OK" );
				okButton.setActionCommand( "OK" );
				okButton.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						userPressedOk = true;
						toAdjust.setFrom( SetColorScaleDialog.this );
						SetColorScaleDialog.this.dispose();
					}
				} );
				buttonPane.add( okButton );
				getRootPane().setDefaultButton( okButton );
			}
			{
				final JButton cancelButton = new JButton( "Cancel" );
				cancelButton.setActionCommand( "Cancel" );
				cancelButton.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						userPressedOk = false;
						SetColorScaleDialog.this.dispose();
					}
				} );

				buttonPane.add( cancelButton );
			}
		}
	}

	@Override
	public double getMin()
	{
		final double min = textFieldMin.getValue();
		final double max = textFieldMax.getValue();
		return max > min ? min : max;
	}

	@Override
	public double getMax()
	{
		final double min = textFieldMin.getValue();
		final double max = textFieldMax.getValue();
		return max > min ? max : min;
	}

	@Override
	public boolean isAutoMinMaxMode()
	{
		return rdbtnAutomaticScale.isSelected();
	}

	public boolean hasUserPressedOK()
	{
		return userPressedOk;
	}

	@Override
	public String toString()
	{
		final String str = super.toString() + " - min = " + getMin() + ", max = " + getMax() + ", isAuto = " + isAutoMinMaxMode();
		return str;
	}

	@Override
	public void autoMinMax()
	{}

	@Override
	public void setAutoMinMaxMode( final boolean autoMode )
	{
		rdbtnAutomaticScale.setSelected( autoMode );
	}

	@Override
	public void setFrom( final MinMaxAdjustable minMaxAdjustable )
	{
		setAutoMinMaxMode( minMaxAdjustable.isAutoMinMaxMode() );
		setMinMax( minMaxAdjustable.getMin(), minMaxAdjustable.getMax() );
	}

	@Override
	public void setMinMax( final double min, final double max )
	{
		textFieldMin.setText( "" + min );
		textFieldMax.setText( "" + max );
	}
}
