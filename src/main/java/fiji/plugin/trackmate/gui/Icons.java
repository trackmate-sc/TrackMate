package fiji.plugin.trackmate.gui;

import java.awt.Image;

import javax.swing.ImageIcon;

public class Icons
{
	public static final ImageIcon SPOT_ICON = new ImageIcon( Icons.class.getResource( "images/Icon1_print_transparency.png" ) );

	public static final ImageIcon EDGE_ICON = new ImageIcon( Icons.class.getResource( "images/Icon2_print_transparency.png" ) );

	public static final ImageIcon TRACK_ICON = new ImageIcon( Icons.class.getResource( "images/Icon3b_print_transparency.png" ) );

	public static final ImageIcon BRANCH_ICON = new ImageIcon( Icons.class.getResource( "images/Icons4_print_transparency.png" ) );

	public static final ImageIcon TRACK_SCHEME_ICON = new ImageIcon( Icons.class.getResource( "images/Icon3a_print_transparency.png" ) );

	public static final ImageIcon TRACKMATE_ICON = new ImageIcon( Icons.class.getResource( "images/Logo50x50-color-nofont-72p.png" ) );

	public static final ImageIcon TRACKMATE_ICON_16x16;

	public static final ImageIcon TRACK_SCHEME_ICON_16x16;

	public static final ImageIcon SPOT_ICON_64x64;

	public static final ImageIcon EDGE_ICON_64x64;

	public static final ImageIcon TRACK_ICON_64x64;

	public static final ImageIcon BRANCH_ICON_16x16;
	static
	{
		final Image image1 = SPOT_ICON.getImage();
		final Image newimg1 = image1.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		SPOT_ICON_64x64 = new ImageIcon( newimg1 );

		final Image image2 = EDGE_ICON.getImage();
		final Image newimg2 = image2.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		EDGE_ICON_64x64 = new ImageIcon( newimg2 );

		final Image image3 = TRACK_ICON.getImage();
		final Image newimg3 = image3.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		TRACK_ICON_64x64 = new ImageIcon( newimg3 );

		final Image image4 = BRANCH_ICON.getImage();
		final Image newimg4 = image4.getScaledInstance( 16, 16, java.awt.Image.SCALE_SMOOTH );
		BRANCH_ICON_16x16 = new ImageIcon( newimg4 );

		final Image image5 = TRACK_SCHEME_ICON.getImage();
		final Image newimg5 = image5.getScaledInstance( 16, 16, java.awt.Image.SCALE_SMOOTH );
		TRACK_SCHEME_ICON_16x16 = new ImageIcon( newimg5 );

		final Image image6 = TRACKMATE_ICON.getImage();
		final Image newimg6 = image6.getScaledInstance( 16, 16, java.awt.Image.SCALE_SMOOTH );
		TRACKMATE_ICON_16x16 = new ImageIcon( newimg6 );
	}

	public static final ImageIcon TRACK_TABLES_ICON = new ImageIcon( Icons.class.getResource( "images/table_multiple.png" ) );

	public static final ImageIcon SPOT_TABLE_ICON = new ImageIcon( Icons.class.getResource( "images/table.png" ) );

	public static final ImageIcon NEXT_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_right.png" ) );

	public static final ImageIcon PREVIOUS_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_left.png" ) );

	public static final ImageIcon SAVE_ICON = new ImageIcon( Icons.class.getResource( "images/page_save.png" ) );

	public static final ImageIcon LOG_ICON = new ImageIcon( Icons.class.getResource( "images/information.png" ) );

	public static final ImageIcon DISPLAY_CONFIG_ICON = new ImageIcon( Icons.class.getResource( "images/wrench_orange.png" ) );

	public static final ImageIcon CANCEL_ICON = new ImageIcon( Icons.class.getResource( "images/cancel.png" ) );

	public static final ImageIcon EXECUTE_ICON = new ImageIcon( Icons.class.getResource( "images/control_play_blue.png" ) );

	public static final ImageIcon EDIT_SETTINGS_ICON = new ImageIcon( Icons.class.getResource( "images/cog_edit.png" ) );

	public static final ImageIcon COG_ICON = new ImageIcon( Icons.class.getResource( "images/cog.png" ) );

	public static final ImageIcon PLOT_ICON = new ImageIcon( Icons.class.getResource( "images/plots.png" ) );

	public static final ImageIcon ADD_ICON = new ImageIcon( Icons.class.getResource( "images/add.png" ) );

	public static final ImageIcon REMOVE_ICON = new ImageIcon( Icons.class.getResource( "images/delete.png" ) );

	public static final ImageIcon PREVIEW_ICON = new ImageIcon( Icons.class.getResource( "images/flag_checked.png" ) );

	public static final ImageIcon CSV_ICON = new ImageIcon( Icons.class.getResource( "images/page_save.png" ) );

	public static final ImageIcon MAGNIFIER_ICON = new ImageIcon( Icons.class.getResource( "images/magnifier.png" ) );

	public static final ImageIcon ORANGE_ASTERISK_ICON = new ImageIcon( Icons.class.getResource( "images/spot_icon.png" ) );

	public static final ImageIcon CALCULATOR_ICON = new ImageIcon( Icons.class.getResource( "images/calculator.png" ) );

	public static final ImageIcon ICY_ICON = new ImageIcon( Icons.class.getResource( "images/icy16.png" ) );

	public static final ImageIcon ISBI_ICON = new ImageIcon( Icons.class.getResource( "images/ISBIlogo.png" ) );

	public static final ImageIcon LABEL_IMG_ICON = new ImageIcon( Icons.class.getResource( "images/picture_key.png" ) );

	public static final ImageIcon MERGE_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_merge.png" ) );

	public static final ImageIcon TIME_ICON = new ImageIcon( Icons.class.getResource( "images/time.png" ) );

	public static final ImageIcon BIN_ICON = new ImageIcon( Icons.class.getResource( "images/bin_empty.png" ) );

	public static final ImageIcon CAMERA_ICON = new ImageIcon( Icons.class.getResource( "images/camera_go.png" ) );

	public static final ImageIcon APPLY_ICON = new ImageIcon( Icons.class.getResource( "images/page_save.png" ) );

	public static final ImageIcon REVERT_ICON = new ImageIcon( Icons.class.getResource( "images/page_refresh.png" ) );

	public static final ImageIcon RESET_ICON = new ImageIcon( Icons.class.getResource( "images/page_white.png" ) );

	public final static ImageIcon SELECT_TRACK_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_updown.png" ) );

	public final static ImageIcon SELECT_TRACK_ICON_UPWARDS = new ImageIcon( Icons.class.getResource( "images/arrow_up.png" ) );

	public final static ImageIcon SELECT_TRACK_ICON_DOWNWARDS = new ImageIcon( Icons.class.getResource( "images/arrow_down.png" ) );

	public static final ImageIcon CAMERA_EXPORT_ICON = new ImageIcon( Icons.class.getResource( "images/camera_export.png" ) );

	public static final ImageIcon RESET_ZOOM_ICON = new ImageIcon( Icons.class.getResource( "images/zoom.png" ) );

	public static final ImageIcon ZOOM_IN_ICON = new ImageIcon( Icons.class.getResource( "images/zoom_in.png" ) );

	public static final ImageIcon ZOOM_OUT_ICON = new ImageIcon( Icons.class.getResource( "images/zoom_out.png" ) );

	public static final ImageIcon EDIT_ICON = new ImageIcon( Icons.class.getResource( "images/tag_blue_edit.png" ) );

	public static final ImageIcon HOME_ICON = new ImageIcon( Icons.class.getResource( "images/control_start.png" ) );

	public static final ImageIcon END_ICON = new ImageIcon( Icons.class.getResource( "images/control_end.png" ) );

	public static final ImageIcon ARROW_UP_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_up.png" ) );

	public static final ImageIcon ARROW_DOWN_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_down.png" ) );

	public static final ImageIcon ARROW_LEFT_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_left.png" ) );

	public static final ImageIcon ARROW_RIGHT_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_right.png" ) );

	public static final ImageIcon ARROW_UPLEFT_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_nw.png" ) );

	public static final ImageIcon ARROW_DOWNLEFT_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_sw.png" ) );

	public static final ImageIcon ARROW_UPRIGHT_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_ne.png" ) );

	public static final ImageIcon ARROW_DOWNRIGHT_ICON = new ImageIcon( Icons.class.getResource( "images/arrow_se.png" ) );

	public static final ImageIcon LINKING_ON_ICON = new ImageIcon( Icons.class.getResource( "images/connect.png" ) );

	public static final ImageIcon LINKING_OFF_ICON = new ImageIcon( Icons.class.getResource( "images/connect_bw.png" ) );

	public static final ImageIcon THUMBNAIL_ON_ICON = new ImageIcon( Icons.class.getResource( "images/images.png" ) );

	public static final ImageIcon THUMBNAIL_OFF_ICON = new ImageIcon( Icons.class.getResource( "images/images_bw.png" ) );

	public static final ImageIcon REFRESH_ICON = new ImageIcon( Icons.class.getResource( "images/refresh.png" ) );

	public static final ImageIcon CAPTURE_UNDECORATED_ICON = new ImageIcon( Icons.class.getResource( "images/camera_go.png" ) );

	public static final ImageIcon CAPTURE_DECORATED_ICON = new ImageIcon( Icons.class.getResource( "images/camera_edit.png" ) );

	public static final ImageIcon DISPLAY_DECORATIONS_ON_ICON = new ImageIcon( Icons.class.getResource( "images/application_view_columns.png" ) );

	public static final ImageIcon SELECT_STYLE_ICON = new ImageIcon( Icons.class.getResource( "images/theme.png" ) );

}
