/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.PLOT_ICON;

import java.awt.Frame;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.ExportableChartPanel;

public class PlotNSpotsVsTimeAction extends AbstractTMAction {

	public static final String NAME = "Plot N spots vs time";

	public static final String KEY = "PLOT_NSPOTS_VS_TIME";
	public static final String INFO_TEXT =  "<html>" +
			"Plot the number of spots in each frame as a function <br>" +
			"of time. Only the filtered spots are taken into account. " +
			"</html>";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		// Collect data
		final Model model = trackmate.getModel();
		final Settings settings = trackmate.getSettings();
		final SpotCollection spots = model.getSpots();
		final int nFrames = spots.keySet().size();
		final double[][] data = new double[2][nFrames];
		int index = 0;
		for (final int frame : spots.keySet()) {
			data[1][index] = spots.getNSpots(frame, true);
			if (data[1][index] > 0) {
				data[0][index] = spots.iterator(frame, false).next().getFeature(Spot.POSITION_T);
			} else {
				data[0][index] = frame * settings.dt;
			}
			index++;
		}

		// Plot data
		final String xAxisLabel = "Time ("+trackmate.getModel().getTimeUnits()+")";
		final String yAxisLabel = "N spots";
		final String title = "Nspots vs Time for "+trackmate.getSettings().imp.getShortTitle();
		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Nspots", data);

		final JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		chart.getTitle().setFont(FONT);
		chart.getLegend().setItemFont(SMALL_FONT);

		// The plot
		final XYPlot plot = chart.getXYPlot();
//		plot.setRenderer(0, pointRenderer);
		plot.getRangeAxis().setLabelFont(FONT);
		plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
		plot.getDomainAxis().setLabelFont(FONT);
		plot.getDomainAxis().setTickLabelFont(SMALL_FONT);

		final ExportableChartPanel panel = new ExportableChartPanel(chart);

		final JFrame frame = new JFrame(title);
		frame.setSize(500, 270);
		frame.getContentPane().add(panel);
		frame.setVisible(true);
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return PLOT_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new PlotNSpotsVsTimeAction();
		}
	}
}
