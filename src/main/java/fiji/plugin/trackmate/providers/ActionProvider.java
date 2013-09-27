package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.action.CopyOverlayAction;
import fiji.plugin.trackmate.action.ExportTracksToXML;
import fiji.plugin.trackmate.action.ExtractTrackStackAction;
import fiji.plugin.trackmate.action.ISBIChallengeExporter;
import fiji.plugin.trackmate.action.LinkNew3DViewerAction;
import fiji.plugin.trackmate.action.MergeFileAction;
import fiji.plugin.trackmate.action.PlotNSpotsVsTimeAction;
import fiji.plugin.trackmate.action.RadiusToEstimatedAction;
import fiji.plugin.trackmate.action.RecalculateFeatureAction;
import fiji.plugin.trackmate.action.ResetRadiusAction;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.TrackMateGUIController;

public class ActionProvider {

	/**
	 * The action names, in the order they will appear in the GUI. These names
	 * will be used as keys to access relevant action classes.
	 */
	protected List<String> names;
	protected CopyOverlayAction			copyOverlayAction;
	protected PlotNSpotsVsTimeAction	plotNSpotsVsTimeAction;
	protected CaptureOverlayAction		captureOverlayAction;
	protected ResetSpotTimeFeatureAction	resetSpotTimeFeatureAction;
	protected RecalculateFeatureAction		recalculateFeatureAction;
	protected RadiusToEstimatedAction		radiusToEstimatedAction;
	protected ResetRadiusAction				resetRadiusAction;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the TrackMate actions currently
	 * available in TrackMate. Each action is identified by a key String, which
	 * can be used to retrieve new instance of the action.
	 * <p>
	 * If you want to add custom actions to TrackMate, a simple way is to extend
	 * this factory so that it is registered with the custom actions and provide
	 * this extended factory to the {@link TrackMate} trackmate.
	 *
	 * @param trackmate
	 *            the {@link TrackMate} instance these actions will operate on.
	 * @param guiController
	 *            the {@link TrackMateGUIController} controller that controls
	 *            the GUI these actions are launched from.
	 */
	public ActionProvider() {
		registerActions();
	}

	/*
	 * METHODS
	 */

	/**
	 * Registers the standard actions shipped with TrackMate, and instantiates
	 * some of them.
	 */
	protected void registerActions() {
		this.copyOverlayAction = new CopyOverlayAction();
		this.plotNSpotsVsTimeAction = new PlotNSpotsVsTimeAction();
		this.captureOverlayAction = new CaptureOverlayAction();
		this.resetSpotTimeFeatureAction = new ResetSpotTimeFeatureAction();
		this.recalculateFeatureAction = new RecalculateFeatureAction();
		this.radiusToEstimatedAction = new RadiusToEstimatedAction();
		this.resetRadiusAction = new ResetRadiusAction();

		// Names
		names = new ArrayList<String>(10);
		names.add(ExportTracksToXML.NAME);
		names.add(ExtractTrackStackAction.NAME);
		names.add(LinkNew3DViewerAction.NAME);
		names.add(CopyOverlayAction.NAME);
		names.add(PlotNSpotsVsTimeAction.NAME);
		names.add(CaptureOverlayAction.NAME);
		//		names.add(ResetSpotTimeFeatureAction.NAME);
		names.add(RecalculateFeatureAction.NAME);
		names.add(RadiusToEstimatedAction.NAME);
		names.add(ResetRadiusAction.NAME);
		names.add(MergeFileAction.NAME);
		//		names.add(fiji.plugin.trackmate.action.ISBIChallengeExporter.NAME);
	}

	/**
	 * Returns a new instance of the target action identified by the key
	 * parameter. If the key is unknown to this factory, <code>null</code> is
	 * returned.
	 *
	 * @return a new {@link TrackMateAction}.
	 */
	public TrackMateAction getAction(final String key, final TrackMateGUIController controller) {

		if (ExtractTrackStackAction.NAME.equals(key)) {
			return new ExtractTrackStackAction(controller.getSelectionModel());

		} else if (LinkNew3DViewerAction.NAME.equals(key)) {
			return new LinkNew3DViewerAction(controller);

		} else if (CopyOverlayAction.NAME.equals(key)) {
			return copyOverlayAction;

		} else if (PlotNSpotsVsTimeAction.NAME.equals(key)) {
			return plotNSpotsVsTimeAction;

		} else if (CaptureOverlayAction.NAME.equals(key)) {
			return captureOverlayAction;

		} else if (ResetSpotTimeFeatureAction.NAME.equals(key)) {
			return resetSpotTimeFeatureAction;

		} else if (RecalculateFeatureAction.NAME.equals(key)) {
			return recalculateFeatureAction;

		} else if (RadiusToEstimatedAction.NAME.equals(key)) {
			return radiusToEstimatedAction;

		} else if (ResetRadiusAction.NAME.equals(key)) {
			return resetRadiusAction;

		} else if (ExportTracksToXML.NAME.equals(key)) {
			return new ExportTracksToXML(controller);

		} else if (ISBIChallengeExporter.NAME.equals(key)) {
			return new ISBIChallengeExporter(controller);

		} else if (MergeFileAction.NAME.equals(key)) {
			return new MergeFileAction(controller);
		}

		return null;
	}

	/**
	 * @return the html String containing a descriptive information about the
	 *         target action, or <code>null</code> if it is unknown to this
	 *         factory.
	 */
	public String getInfoText(final String key) {
		if (ExtractTrackStackAction.NAME.equals(key)) {
			return ExtractTrackStackAction.INFO_TEXT;

		} else if (LinkNew3DViewerAction.NAME.equals(key)) {
			return LinkNew3DViewerAction.INFO_TEXT;

		} else if (CopyOverlayAction.NAME.equals(key)) {
			return CopyOverlayAction.INFO_TEXT;

		} else if (PlotNSpotsVsTimeAction.NAME.equals(key)) {
			return PlotNSpotsVsTimeAction.INFO_TEXT;

		} else if (CaptureOverlayAction.NAME.equals(key)) {
			return CaptureOverlayAction.INFO_TEXT;

		} else if (ResetSpotTimeFeatureAction.NAME.equals(key)) {
			return ResetSpotTimeFeatureAction.INFO_TEXT;

		} else if (RecalculateFeatureAction.NAME.equals(key)) {
			return RecalculateFeatureAction.INFO_TEXT;

		} else if (RadiusToEstimatedAction.NAME.equals(key)) {
			return RadiusToEstimatedAction.INFO_TEXT;

		} else if (ResetRadiusAction.NAME.equals(key)) {
			return ResetRadiusAction.INFO_TEXT;

		} else if (ExportTracksToXML.NAME.equals(key)) {
			return ExportTracksToXML.INFO_TEXT;

		} else if (MergeFileAction.NAME.equals(key)) {
			return MergeFileAction.INFO_TEXT;
		}

		return null;
	}

	/**
	 * @return the descriptive icon for target action, or <code>null</code> if
	 *         it is unknown to this factory.
	 */
	public ImageIcon getIcon(final String key) {
		if (ExtractTrackStackAction.NAME.equals(key)) {
			return ExtractTrackStackAction.ICON;

		} else if (LinkNew3DViewerAction.NAME.equals(key)) {
			return LinkNew3DViewerAction.ICON;

		} else if (CopyOverlayAction.NAME.equals(key)) {
			return CopyOverlayAction.ICON;

		} else if (PlotNSpotsVsTimeAction.NAME.equals(key)) {
			return PlotNSpotsVsTimeAction.ICON;

		} else if (CaptureOverlayAction.NAME.equals(key)) {
			return CaptureOverlayAction.ICON;

		} else if (ResetSpotTimeFeatureAction.NAME.equals(key)) {
			return ResetSpotTimeFeatureAction.ICON;

		} else if (RecalculateFeatureAction.NAME.equals(key)) {
			return RecalculateFeatureAction.ICON;

		} else if (RadiusToEstimatedAction.NAME.equals(key)) {
			return RadiusToEstimatedAction.ICON;

		} else if (ResetRadiusAction.NAME.equals(key)) {
			return ResetRadiusAction.ICON;

		} else if (ExportTracksToXML.NAME.equals(key)) {
			return ExportTracksToXML.ICON;

		} else if (MergeFileAction.NAME.equals(key)) {
			return MergeFileAction.ICON;
		}

		return null;
	}

	/**
	 * @return a list of the detector names available through this factory.
	 */
	public List<String> getAvailableActions() {
		return names;
	}

}
