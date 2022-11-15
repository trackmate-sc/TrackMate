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
