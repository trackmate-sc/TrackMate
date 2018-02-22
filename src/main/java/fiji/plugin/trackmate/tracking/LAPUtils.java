package fiji.plugin.trackmate.tracking;

import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import fiji.plugin.trackmate.Spot;

public class LAPUtils {

	private static final Border RED_BORDER = new LineBorder(Color.RED);

	/*
	 * STATIC KEYS Mainly used for marshalling.
	 */

	public static final String XML_ELEMENT_NAME_LINKING = "Linking";

	public static final String XML_ELEMENT_NAME_GAP_CLOSING = "GapClosing";

	public static final String XML_ELEMENT_NAME_MERGING = "TrackMerging";

	public static final String XML_ELEMENT_NAME_SPLITTING = "TrackSplitting";

	public static final String XML_ELEMENT_NAME_FEATURE_PENALTIES = "FeaturePenalties";

	/*
	 * STATIC METHODS - UTILS
	 */

	/**
	 * Utility method to put a value in a map, contained in a mother map. Here it is mainly use to
	 * feed feature penalties to LAP tracker settings map.
	 * @param motherMap  the mother map
	 * @param motherKey the key in the mother map that points to the child map
	 * @param childKey  the key in the child map to put
	 * @param childValue  the value in the child map to put
	 * @param errorHolder  an error holder that will be appended with an error message if modifying the child
	 * map is unsuccessful.
	 * @return  true if the child map could be modified correctly.
	 */
	public static final boolean addFeaturePenaltyToSettings(final Map<?, ?> motherMap, final Object motherKey, final Object childKey, final Object childValue, final StringBuilder errorHolder) {
		final Object childObj = motherMap.get(motherKey);
		if (null == childObj) {
			errorHolder.append("Mother map has no value for key "+motherKey+".\n");
			return false;
		}
		if (!(childObj instanceof Map<?, ?>)) {
			errorHolder.append("Value for key "+motherKey+" is not a map.\n");
			return false;
		}
		@SuppressWarnings("unchecked")
		final
		Map<Object, Object> childMap = (Map<Object, Object>) childObj;
		childMap.put(childKey, childValue);
		return true;
	}

	/**
	 * Returns a new settings map filled with default values suitable for the
	 * LAP trackers.
	 * 
	 * @return a new map.
	 */
	public static final Map<String, Object> getDefaultLAPSettingsMap() {
		final Map<String, Object> settings = new HashMap<>();
		// Linking
		settings.put(KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE);
		settings.put(KEY_LINKING_FEATURE_PENALTIES, new HashMap<>(DEFAULT_LINKING_FEATURE_PENALTIES));
		// Gap closing
		settings.put(KEY_ALLOW_GAP_CLOSING, DEFAULT_ALLOW_GAP_CLOSING);
		settings.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP);
		settings.put(KEY_GAP_CLOSING_MAX_DISTANCE, DEFAULT_GAP_CLOSING_MAX_DISTANCE);
		settings.put(KEY_GAP_CLOSING_FEATURE_PENALTIES, new HashMap<>(DEFAULT_GAP_CLOSING_FEATURE_PENALTIES));
		// Track splitting
		settings.put(KEY_ALLOW_TRACK_SPLITTING, DEFAULT_ALLOW_TRACK_SPLITTING);
		settings.put(KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE);
		settings.put(KEY_SPLITTING_FEATURE_PENALTIES, new HashMap<>(DEFAULT_SPLITTING_FEATURE_PENALTIES));
		// Track merging
		settings.put(KEY_ALLOW_TRACK_MERGING, DEFAULT_ALLOW_TRACK_MERGING);
		settings.put(KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE);
		settings.put(KEY_MERGING_FEATURE_PENALTIES, new HashMap<>(DEFAULT_MERGING_FEATURE_PENALTIES));
		// Others
		settings.put(KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE);
		settings.put(KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR);
		settings.put(KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE);
		// return
		return settings;
	}


	public static String echoFeaturePenalties(final Map<String, Double> featurePenalties) {
		String str = "";
		if (featurePenalties.isEmpty())
			str += "    - no feature penalties\n";
		else {
			str += "    - with feature penalties:\n";
			for (final String feature : featurePenalties.keySet()) {
				str += "      - "+feature.toString() + ": weight = " + String.format("%.1f", featurePenalties.get(feature)) + '\n';
			}
		}
		return str;

	}

	/**
	 * Compute the cost to link two spots, in the default way for the TrackMate trackmate.
	 * <p>
	 * This cost is calculated as follow:
	 * <ul>
	 * 	<li> The distance between the two spots <code>D</code> is calculated
	 * 	<li> If the spots are separated by more than the distance cutoff, the cost is
	 * set to be the blocking value. If not,
	 * 	<li> For each feature in the map, a penalty <code>p</code> is calculated as
	 * <code>p = 3 × α × |f1-f2| / (f1+f2)</code>, where <code>α</code> is the factor
	 * associated to the feature in the map. This expression is such that:
	 * 	<ul>
	 * 	<li> there is no penalty if the 2 feature values <code>f1</code> and <code>f2</code>
	 * are the same;
	 * 	<li> that, with a factor of 1, the penalty if 1 is one value is the double of the other;
	 * 	<li>the penalty is 2 if one is 5 times the other one.
	 * 	</ul>
	 * 	<li> All penalties are summed, to form <code>P = (1 + ∑ p )</code>
	 *  <li> The cost is set to the square of the product: <code>C = ( D × P )²</code>
	 * </ul>
	 * For instance: if 2 spots differ by twice the value in a feature which is
	 * in the penalty map with a factor of 1, they will <i>look</i> as if they were
	 * twice as far.
	 */
	public static final double computeLinkingCostFor(final Spot s0, final Spot s1,
			final double distanceCutOff, final double blockingValue, final Map<String, Double> featurePenalties) {
		final double d2 = s0.squareDistanceTo(s1);

		// Distance threshold
		if (d2 > distanceCutOff * distanceCutOff) {
			return blockingValue;
		}

		double penalty = 1;
		for (final String feature : featurePenalties.keySet()) {
			final double ndiff = s0.normalizeDiffTo(s1, feature);
			if (Double.isNaN(ndiff))
				continue;
			final double factor = featurePenalties.get(feature);
			penalty += factor * 1.5 * ndiff;
		}

		// Set score
		return d2 * penalty * penalty;
	}



	/**
	 * @return true if the settings map can be used with the LAP trackers. We do
	 *         not check that all the spot features used in penalties are indeed
	 *         found in all spots, because if such a feature is absent from one
	 *         spot, the LAP trackers simply ignores the penalty and does not
	 *         generate an error.
	 * @param settings
	 *            the map to test.
	 * @param errorHolder
	 *            a {@link StringBuilder} that will contain an error message if
	 *            the check is not successful.
	 */
	public static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder errorHolder )
	{
		if (null == settings) {
			errorHolder.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Linking
		ok = ok & checkParameter( settings, KEY_LINKING_MAX_DISTANCE, Double.class, errorHolder );
		ok = ok & checkFeatureMap( settings, KEY_LINKING_FEATURE_PENALTIES, errorHolder );
		// Gap-closing
		ok = ok & checkParameter( settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, errorHolder );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, errorHolder );
		// Splitting
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SPLITTING_MAX_DISTANCE, Double.class, errorHolder );
		ok = ok & checkFeatureMap( settings, KEY_SPLITTING_FEATURE_PENALTIES, errorHolder );
		// Merging
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_MERGING_MAX_DISTANCE, Double.class, errorHolder );
		ok = ok & checkFeatureMap( settings, KEY_MERGING_FEATURE_PENALTIES, errorHolder );
		// Others
		ok = ok & checkParameter( settings, KEY_CUTOFF_PERCENTILE, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_BLOCKING_VALUE, Double.class, errorHolder );

		// Check keys
		final List<String> mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add(KEY_LINKING_MAX_DISTANCE);
		mandatoryKeys.add(KEY_ALLOW_GAP_CLOSING);
		mandatoryKeys.add(KEY_GAP_CLOSING_MAX_DISTANCE);
		mandatoryKeys.add(KEY_GAP_CLOSING_MAX_FRAME_GAP);
		mandatoryKeys.add(KEY_ALLOW_TRACK_SPLITTING);
		mandatoryKeys.add(KEY_SPLITTING_MAX_DISTANCE);
		mandatoryKeys.add(KEY_ALLOW_TRACK_MERGING);
		mandatoryKeys.add(KEY_MERGING_MAX_DISTANCE);
		mandatoryKeys.add(KEY_ALTERNATIVE_LINKING_COST_FACTOR);
		mandatoryKeys.add(KEY_CUTOFF_PERCENTILE);
		mandatoryKeys.add(KEY_BLOCKING_VALUE);
		final List<String> optionalKeys = new ArrayList<>();
		optionalKeys.add(KEY_LINKING_FEATURE_PENALTIES);
		optionalKeys.add(KEY_GAP_CLOSING_FEATURE_PENALTIES);
		optionalKeys.add(KEY_SPLITTING_FEATURE_PENALTIES);
		optionalKeys.add(KEY_MERGING_FEATURE_PENALTIES);
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, errorHolder );

		return ok;
	}


	/**
	 * Check the validity of a feature penalty map in a settings map.
	 * <p>
	 * A feature penalty setting is valid if it is either <code>null</code> (not
	 * here, that is) or an actual <code>Map&lt;String, Double&gt;</code>. Then,
	 * all its keys must be Strings and all its values as well.
	 *
	 * @param map
	 *            the map to inspect.
	 * @param featurePenaltiesKey
	 *            the key that should map to a feature penalty map.
	 * @param errorHolder
	 *            will be appended with an error message.
	 * @return true if the feature penalty map is valid.
	 */
	@SuppressWarnings("rawtypes")
	public static final boolean checkFeatureMap(final Map<String, Object> map, final String featurePenaltiesKey, final StringBuilder errorHolder) {
		final Object obj = map.get(featurePenaltiesKey);
		if (null == obj) {
 return true; // Not here is acceptable
		}
		if (!(obj instanceof Map)) {
			errorHolder.append("Feature penalty map is not of the right class. Expected a Map, got a "+obj.getClass().getName()+".\n");
			return false;
		}
		boolean ok = true;
		final Map fpMap = (Map) obj;
		final Set fpKeys = fpMap.keySet();
		for(final Object fpKey : fpKeys) {
			if (!(fpKey instanceof String)) {
				ok = false;
				errorHolder.append("One key ("+fpKey.toString()+") in the map is not of the right class.\n" +
						"Expected String, got "+fpKey.getClass().getName()+".\n");
			}
			final Object fpVal = fpMap.get(fpKey);
			if (!(fpVal instanceof Double)) {
				ok = false;
				errorHolder.append("The value for key "+fpVal.toString()+" in the map is not of the right class.\n" +
						"Expected Double, got "+fpVal.getClass().getName()+".\n");
			}
		}
		return ok;
	}

	public static final void echoMatrix(final double[][] m) {
		final int nlines = m.length;
		if (nlines == 0) {
			System.out.println("0x0 empty matrix");
			return;
		}
		final int nrows = m[0].length;
		double val;
		System.out.print("L\\C\t");
		for (int j = 0; j < nrows; j++) {
			System.out.print(String.format("%7d: ", j));
		}
		System.out.println();
		for (int i = 0; i < nlines; i++) {
			System.out.print(i+":\t");
			for (int j = 0; j < nrows; j++) {
				val = m[i][j];
				if (val > Double.MAX_VALUE/2)
					System.out.print("     B   ");
				else
					System.out.print(String.format("%7.1f  ", val));
			}
			System.out.println();
		}
	}

	/**
	 * Display the cost matrix solved by the Hungarian algorithm in the LAP approach.
	 * @param costs  the cost matrix
	 * @param nSegments  the number of track segments found in the first step of the LAP tracking
	 * @param nSpots  the number of middle spots to consider
	 * @param blockingValue  the blocking value for cost
	 * @param solutions  the Hungarian assignment couple
	 */
	public static final void displayCostMatrix(final double[][] costs, final int nSegments, final int nSpots, final double blockingValue, final int[][]solutions) {
		final int width = costs.length;
		final int height = costs[0].length;
		double val;
		String txt;
		System.out.println(String.format("Displaying table with: Width = %d, Height = %d", width, height));

		// Set column header
		final TableModel model = new DefaultTableModel(height, width) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getColumnName(final int i) {
				if (i < nSegments)
					return "Ts "+i;
				else if (i < nSegments + nSpots)
					return "Sp "+(i-nSegments);
				else
					return "ø";
			}
		};

		// Create table with specific coloring
		final JTable debugTable = new JTable(model) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int col) {

				final JLabel label = (JLabel) super.prepareRenderer(renderer, row, col);
				// Change font color according to matrix parts
				if (col < nSegments) {

					if (row < nSegments)
						label.setForeground(Color.BLUE);  // Gap closing
					else if (row < nSegments + nSpots)
						label.setForeground(Color.GREEN.darker()); // Splitting
					else
						label.setForeground(Color.BLACK); // Initiating

				} else if (col < nSegments + nSpots) {

					if (row < nSegments)
						label.setForeground(Color.CYAN.darker());  // Merging
					else if (row < nSegments + nSpots)
						label.setForeground(Color.RED.darker()); // Middle block
					else
						label.setForeground(Color.BLACK); // Initiating

				} else {
					if (row < nSegments + nSpots)
						label.setForeground(Color.BLACK); // Terminating
					else
						label.setForeground(Color.GRAY); // Bottom right block
				}
				label.setHorizontalAlignment(SwingConstants.CENTER);

				// Change border color according to Hungarian solution
				label.setBorder(null);
				for (int i = 0; i < solutions.length; i++) {
					final int srow = solutions[i][0];
					final int scol = solutions[i][1];
					if (row == srow && col == scol)
						label.setBorder(RED_BORDER);
				}

				return label;
			}
		};


		// Set values
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				val = costs[row][col];
				if (val == blockingValue)
					txt = "B";
				else
					txt = String.format("%.1f", val);
				model.setValueAt(txt, row, col);
			}
		}

		// Row headers
		final TableModel rhm = new AbstractTableModel() {
			private static final long serialVersionUID = 1L;
			String headers[] = new String[2*(nSegments + nSpots)];
			{
				for (int i = 0; i < nSegments; i++)
					headers[i] = "Te "+i;
				for (int i = nSegments; i < nSegments + nSpots; i++)
					headers[i] = "Sp "+(i-nSegments);
				for (int i = nSegments + nSpots; i < headers.length; i++)
					headers[i] = "ø";
			}

			@Override
			public int getColumnCount() {return 1;	}
			@Override
			public int getRowCount() { return headers.length; }
			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				return headers[rowIndex];
			}
		};
		final JTable rowHeader = new JTable(rhm);
		final Dimension d = rowHeader.getPreferredScrollableViewportSize();
		d.width = rowHeader.getPreferredSize().width;
		rowHeader.setPreferredScrollableViewportSize(d);

		// Set column width
		debugTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		for (int i = 0; i < debugTable.getColumnCount(); i++) {
			debugTable.getColumnModel().getColumn(i).setPreferredWidth(50);
		}

		// Embed table in scroll pane
		final JScrollPane scrollPane = new JScrollPane(debugTable);
		debugTable.setFillsViewportHeight(true);
		scrollPane.setRowHeaderView(rowHeader);
		final JFrame frame = new JFrame("Segment cost matrix");
		frame.getContentPane().add(scrollPane);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}


	public static void echoSolutions(final int[][] solutions) {
		for (int i = 0; i < solutions.length; i++)
			System.out.println(String.format("%3d: %3d -> %3d", i, solutions[i][0], solutions[i][1]));
	}

	public static void displayLAPresults(final int[][] array) {
		final Object[][] data = new Object[array.length][array[0].length];
		final Object[] headers = new Object[array[0].length];
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				data[i][j] = ""+array[i][j];
			}
		}
		for (int i = 0; i < headers.length; i++) {
			headers[i] = ""+i;
		}
		final JTable table = new JTable(data, headers);

		final JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		final JFrame frame = new JFrame("Hungarian solution");
		frame.getContentPane().add(scrollPane);
		frame.setVisible(true);
	}

	/*
	 * MARSHALLING
	 */

}
