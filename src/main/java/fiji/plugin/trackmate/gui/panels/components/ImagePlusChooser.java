package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import ij.ImagePlus;
import ij.WindowManager;

public class ImagePlusChooser extends javax.swing.JFrame
{

	private static final long serialVersionUID = 322598397229876595L;

	public final ActionEvent OK_BUTTON_PUSHED = new ActionEvent( this, 0, "OK" );

	public final ActionEvent CANCEL_BUTTON_PUSHED = new ActionEvent( this, 1, "Cancel" );

	private JPanel jPanelMain;

	private JLabel jLabelSelect;

	private JComboBox< String > jComboBoxImage;

	private JButton jButtonCancel;

	private JButton jButtonOK;

	private ArrayList< ImagePlus > images;

	private final ArrayList< ActionListener > listeners = new ArrayList< >();

	private final String windowTitle;

	private final String extraOption;

	private final String message;

	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main( final String[] args )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				final ImagePlusChooser inst = new ImagePlusChooser( "A false choice.", "Pick anything.", "anything" );
				inst.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent event )
					{
						System.out.println( event );
					}
				} );
				inst.setLocationRelativeTo( null );
				inst.setVisible( true );
			}
		} );
	}

	/*
	 * CONSTRUCTOR
	 */

	public ImagePlusChooser( final String windowTitle, final String message, final String extraOption )
	{
		super();
		this.windowTitle = windowTitle;
		this.message = message;
		this.extraOption = extraOption;
		initGUI();
		addWindowListener( new WindowListener()
		{
			@Override
			public void windowOpened( final WindowEvent e )
			{}

			@Override
			public void windowIconified( final WindowEvent e )
			{}

			@Override
			public void windowDeiconified( final WindowEvent e )
			{}

			@Override
			public void windowDeactivated( final WindowEvent e )
			{}

			@Override
			public void windowClosing( final WindowEvent e )
			{}

			@Override
			public void windowClosed( final WindowEvent e )
			{
				fireAction( CANCEL_BUTTON_PUSHED );
			}

			@Override
			public void windowActivated( final WindowEvent e )
			{}
		} );
	}

	/*
	 * METHODS
	 */

	public void addActionListener( final ActionListener listener )
	{
		listeners.add( listener );
	}

	public boolean removeActionListener( final ActionListener listener )
	{
		return listeners.remove( listener );
	}

	/**
	 * Return the selected {@link ImagePlus} in the combo list, or
	 * <code>null</code> if the first choice "3D viewer" was selected.
	 */
	public ImagePlus getSelectedImagePlus()
	{
		final int index = jComboBoxImage.getSelectedIndex();
		if ( index < 1 )
			return null;
		
		return images.get( index - 1 );
	}

	/*
	 * PRIVATE METHODS
	 */

	private void fireAction( final ActionEvent event )
	{
		for ( final ActionListener listener : listeners )
			listener.actionPerformed( event );
	}

	/**
	 * Refresh the name list of images, from the field {@link #images}, and send
	 * it to the {@link JComboBox} that display then.
	 */
	private String[] getImageNames()
	{
		final int[] IDs = WindowManager.getIDList();
		String[] image_names = null;
		if ( null == IDs )
		{
			image_names = new String[] { extraOption };
			images = new ArrayList< >();
			return image_names;
		}
		ImagePlus imp;
		images = new ArrayList< >( IDs.length );
		for ( int i = 0; i < IDs.length; i++ )
		{
			imp = WindowManager.getImage( IDs[ i ] );
			images.add( imp );
		}
		if ( images.size() < 1 )
		{
			image_names = new String[] { extraOption };
		}
		else
		{
			image_names = new String[ images.size() + 1 ];
		}
		image_names[ 0 ] = extraOption;
		for ( int i = 0; i < images.size(); i++ )
		{
			image_names[ i + 1 ] = images.get( i ).getTitle();
		}
		return image_names;
	}

	private void initGUI()
	{
		try
		{
			setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
			{
				jPanelMain = new JPanel();
				getContentPane().add( jPanelMain, BorderLayout.CENTER );
				jPanelMain.setLayout( null );
				{
					jLabelSelect = new JLabel();
					jPanelMain.add( jLabelSelect );
					jLabelSelect.setFont( FONT );
					jLabelSelect.setText( message );
					jLabelSelect.setBounds( 12, 10, 258, 15 );
				}
				{
					final ComboBoxModel< String > jComboBoxImageModel = new DefaultComboBoxModel< >( getImageNames() );
					jComboBoxImage = new JComboBox< >();
					jPanelMain.add( jComboBoxImage );
					jComboBoxImage.setFont( SMALL_FONT );
					jComboBoxImage.setModel( jComboBoxImageModel );
					jComboBoxImage.setBounds( 12, 31, 258, 22 );
				}
				{
					jButtonCancel = new JButton();
					jPanelMain.add( jButtonCancel );
					jButtonCancel.setFont( FONT );
					jButtonCancel.setText( "Cancel" );
					jButtonCancel.setBounds( 12, 65, 64, 26 );
					jButtonCancel.addActionListener( new ActionListener()
					{
						@Override
						public void actionPerformed( final ActionEvent e )
						{
							fireAction( CANCEL_BUTTON_PUSHED );
						}
					} );
				}
				{
					jButtonOK = new JButton();
					jPanelMain.add( jButtonOK );
					jButtonOK.setFont( FONT );
					jButtonOK.setText( "OK" );
					jButtonOK.setBounds( 205, 65, 65, 26 );
					jButtonOK.addActionListener( new ActionListener()
					{
						@Override
						public void actionPerformed( final ActionEvent e )
						{
							fireAction( OK_BUTTON_PUSHED );
						}
					} );
				}
			}
			pack();
			this.setSize( 280, 130 );
			this.setTitle( windowTitle );
			this.setResizable( false );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

}
