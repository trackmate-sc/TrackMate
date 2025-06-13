/*-
 * #%L
 * The Labkit image segmentation tool for Fiji.
 * %%
 * Copyright (C) 2017 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.function.Consumer;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerStateChange;
import net.imglib2.Interval;
import sc.fiji.labkit.ui.bdv.BdvLayer;
import sc.fiji.labkit.ui.bdv.BdvShowable;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.utils.Notifier;

/**
 * BdvLayerLink links a {@link BdvLayer} to a Big Data Viewer
 * ({@link BdvHandle}).
 * <p>
 * The link involves for {@link BdvLayer#image()}, {@link BdvLayer#visibility()}
 * and {@link BdvLayer#listeners()}. The {@link BdvLayer#image() image} in
 * BdvLayer will be show in BDV and is updated whenever a change is indicated by
 * {@code BdvLayer.image().notifier()}. The {@link BdvLayer#visibility()
 * visibility} in BdvLayer is synchronized with the visibility of the respective
 * {@link BdvStackSource}. A call to {@link BdvLayer#listeners()} will trigger a
 * repaint of the BDV.
 */
public class BdvLayerLink implements Holder<BdvStackSource<?>> {

	private final BdvHandle handle;

	private final SynchronizedViewerState viewerState;

	private final BdvLayer layer;

	private final Notifier notifier = new Notifier();

	private final Runnable onImageChanged = this::onImageChanged;

	private final Runnable onVisibilityChanged = this::updateBdv;

	private final Consumer<Interval> onRequestRepaint = this::onRequestRepaint;

	private BdvStackSource<?> bdvSource;

	public BdvLayerLink(final BdvLayer layer, final BdvHandle handle) {
		this.handle = handle;
		this.viewerState = handle.getViewerPanel().state();
		this.layer = layer;
		final BdvOptions options = BdvOptions.options().addTo(handle);
		final Holder<BdvShowable> image = layer.image();
		final BdvShowable showable1 = image.get();
		bdvSource = showable1 != null ? showable1.show(layer.title(), options) : null;
		image.notifier().addWeakListener(onImageChanged);
		layer.listeners().addWeakListener(onRequestRepaint);
		layer.visibility().notifier().addWeakListener(onVisibilityChanged);

		// NB: Listen to ViewerState changes, to get notified about visibility changes
		// in Big Data Viewer.
		viewerState.changeListeners().add(this::onViewerStateChanged);
	}

	private void onImageChanged() {
		final BdvStackSource<?> source1 = bdvSource;
		bdvSource = null;
		if (source1 != null)
			source1.removeFromBdv();
		final BdvShowable showable = layer.image().get();
		if (showable != null) {
			bdvSource = showable.show(layer.title(), BdvOptions.options().addTo(handle));
			bdvSource.setActive(layer.visibility().get());
		}
		updateBdv();
		notifier.notifyListeners();
	}

	private void onRequestRepaint(final Interval interval) {
		if (interval == null)
			handle.getViewerPanel().requestRepaint();
		else
			handle.getViewerPanel().requestRepaint(interval);
	}

	private void updateBdv() {
		if (bdvSource != null)
			try
			{
				bdvSource.setActive(layer.visibility().get());
			}
			catch (final NullPointerException ignore) {

			}
	}

	private void onViewerStateChanged(final ViewerStateChange change) {
//		final BdvStackSource<?> source1 = bdvSource;
//		if (source1 != null && change == ViewerStateChange.VISIBILITY_CHANGED) {
//			final boolean visible = viewerState.isSourceActive(source1.getSources().get(0));
//			layer.visibility().set(visible);
//		}
	}

	@Override
	public void set(final BdvStackSource<?> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BdvStackSource<?> get() {
		return bdvSource;
	}

	@Override
	public Notifier notifier() {
		return notifier;
	}
}
