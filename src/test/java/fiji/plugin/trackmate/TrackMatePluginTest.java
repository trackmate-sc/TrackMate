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
package fiji.plugin.trackmate;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.object.ObjectService;

import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;

public class TrackMatePluginTest {

	static
	{
		net.imagej.patcher.LegacyInjector.preinit();
	}

	@Test
	public void testTrackMateRegistration() {
		final TestTrackMatePlugin testPlugin = new TestTrackMatePlugin();
		testPlugin.setUp();
		final ObjectService objectService = testPlugin.getLocalContext().service(ObjectService.class);
		
		final List<TrackMate> trackMateInstances = objectService.getObjects(TrackMate.class);
		assertTrue(trackMateInstances.size() == 1);
		assertTrue(trackMateInstances.get(0) instanceof TrackMate);
	}

	private class TestTrackMatePlugin extends TrackMatePlugIn {

		@SuppressWarnings("unused")
		public void setUp() {
			final ImagePlus imp = IJ.createImage("Test Image", 256, 256, 10, 8);
			final Settings settings = createSettings(imp);
			final Model model = createModel(imp);
			final TrackMate trackMate = createTrackMate(model, settings);
		}

		public Context getLocalContext() {
			return TMUtils.getContext();
		}

	}
}
