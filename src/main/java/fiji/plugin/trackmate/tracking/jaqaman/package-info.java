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
/**
 * Classes that implements the tracking logic of the framework described first
 * in:
 * 
 * <pre>
Robust single-particle tracking in live-cell time-lapse sequences.
Khuloud Jaqaman, Dinah Loerke, Marcel Mettlen, Hirotaka Kuwata, Sergio Grinstein, Sandra L Schmid, Gaudenz Danuser.
Nat Methods. 2008 Aug;5(8):695-702. doi: 10.1038/nmeth.1237. Epub 2008 Jul 20.
 * </pre>
 * 
 * The trackers implemented here have some substantial changes compared to the
 * tracker described in the paper above. There changes are summarized <a href=
 * "https://imagej.net/plugins/trackmate/algorithms#main-differences-with-the-jaqaman-paper">here</a>.
 * The general framework is also used elsewhere in TrackMate for other trackers.
 * 
 * @author Jean-Yves Tinevez
 *
 */
package fiji.plugin.trackmate.tracking.jaqaman;
