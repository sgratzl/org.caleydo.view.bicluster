/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.bicluster.elem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.caleydo.core.data.perspective.table.TablePerspective;
import org.caleydo.core.data.virtualarray.VirtualArray;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.event.EventPublisher;
import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.GLElementDecorator;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.IGLElementContext;
import org.caleydo.core.view.opengl.layout2.basic.GLButton;
import org.caleydo.core.view.opengl.layout2.basic.GLButton.EButtonMode;
import org.caleydo.core.view.opengl.layout2.basic.GLButton.ISelectionCallback;
import org.caleydo.core.view.opengl.layout2.basic.GLSlider;
import org.caleydo.core.view.opengl.layout2.basic.GLSlider.EValueVisibility;
import org.caleydo.core.view.opengl.layout2.layout.GLLayouts;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.manage.ButtonBarBuilder.EButtonBarLayout;
import org.caleydo.core.view.opengl.layout2.renderer.GLRenderers;
import org.caleydo.core.view.opengl.layout2.renderer.IGLRenderer;
import org.caleydo.view.bicluster.BiClusterRenderStyle;
import org.caleydo.view.bicluster.elem.annotation.ALZHeatmapElement;
import org.caleydo.view.bicluster.elem.annotation.ProbabilityLZHeatmapElement;
import org.caleydo.view.bicluster.event.ClusterScaleEvent;
import org.caleydo.view.bicluster.event.LZThresholdChangeEvent;
import org.caleydo.view.bicluster.event.MouseOverClusterEvent;
import org.caleydo.view.bicluster.event.SortingChangeEvent;
import org.caleydo.view.bicluster.event.SortingChangeEvent.SortingType;
import org.caleydo.view.bicluster.sorting.BandSorting;
import org.caleydo.view.bicluster.sorting.FuzzyClustering;
import org.caleydo.view.bicluster.sorting.IntFloat;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * e.g. a class for representing a cluster
 *
 * @author Michael Gillhofer
 * @author Samuel Gratzl
 */
public class NormalClusterElement extends AMultiClusterElement {

	private ToolBar toolBar;
	private ThresholdBar dimThreshBar;
	private ThresholdBar recThreshBar;

	private final FuzzyClustering dimClustering;
	private final FuzzyClustering recClustering;
	/**
	 * elements for showing the probability heatmaps
	 */
	private Collection<ALZHeatmapElement> annotations = new ArrayList<>(4);

	protected boolean showThreshold;

	protected SortingType sortingType = SortingType.BY_PROBABILITY;

	public NormalClusterElement(int bcNr, TablePerspective data, BiClustering clustering) {
		super(bcNr, data, clustering, Predicates.alwaysTrue());
		this.dimClustering = clustering.getDimClustering(bcNr);
		this.recClustering = clustering.getRecClustering(bcNr);

		this.add(createTopToolBar());
		toolBar = new ToolBar(content.createButtonBarBuilder().layoutAs(EButtonBarLayout.SLIDE_LEFT).size(16).build()
				.setSize(16, 16));
		this.add(toolBar); // add a element toolbar
		dimThreshBar = new ThresholdBar(true);
		recThreshBar = new ThresholdBar(false);
		this.add(dimThreshBar);
		this.add(recThreshBar);

		dimThreshBar.updateSliders(dimClustering);
		recThreshBar.updateSliders(recClustering);

		this.add(new HeatMapLabelElement(true, data.getDimensionPerspective(), content));
		this.add(new HeatMapLabelElement(false, data.getRecordPerspective(), content));

		ProbabilityLZHeatmapElement p = new ProbabilityLZHeatmapElement(EDimension.DIMENSION);
		this.annotations.add(p);
		this.add(p);
		p = new ProbabilityLZHeatmapElement(EDimension.RECORD);
		this.annotations.add(p);
		this.add(p);

	}

	@Override
	protected void init(IGLElementContext context) {
		super.init(context);

		resort();
		updateVisibility();
		setZValuesAccordingToState();
	}

	private GLElementContainer createTopToolBar() {
		GLElementContainer topToolBar = new GLElementContainer(GLLayouts.flowVertical(3));
		topToolBar.add(createHideClusterButton());
		GLButton thresholds = new GLButton(EButtonMode.CHECKBOX);
		thresholds.setCallback(new ISelectionCallback() {
			@Override
			public void onSelectionChanged(GLButton button, boolean selected) {
				showThreshold = selected;
				relayout();
			}
		});
		thresholds.setRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_TOOLS));
		thresholds.setSelectedRenderer(new IGLRenderer() {
			@Override
			public void render(GLGraphics g, float w, float h, GLElement parent) {
				g.fillImage(g.getTexture(BiClusterRenderStyle.ICON_TOOLS), 0, 0, w, h, new Color(1, 1, 1, 0.5f));
			}
		});
		thresholds.setTooltip("Set cluster specific thresholds");
		thresholds.setSize(16, 16);
		topToolBar.add(thresholds);
		return topToolBar;
	}

	@Override
	public void doLayout(List<? extends IGLLayoutElement> children, float w, float h) {
		// if (isHidden) return;
		IGLLayoutElement top = children.get(0);
		IGLLayoutElement content = children.get(1);
		IGLLayoutElement corner = children.get(2);
		IGLLayoutElement left = children.get(3);
		IGLLayoutElement dimthreshbar = children.get(4);
		IGLLayoutElement recthreshbar = children.get(5);
		IGLLayoutElement dimLabel = children.get(6);
		IGLLayoutElement recLabel = children.get(7);

		// shift for probability heat maps
		final int firstAnnotation = 8;
		final int dimAnnotations = count(children.subList(firstAnnotation, children.size()), EDimension.DIMENSION);
		final int recAnnotations = count(children.subList(firstAnnotation, children.size()), EDimension.RECORD);

		final int annotationWidth = isFocused() ? 20 : 6;
		final float shift_x = annotationWidth * dimAnnotations;
		final float shift_y = annotationWidth * recAnnotations;

		if (isHovered || isShowAlwaysToolBar()) { // depending whether we are hovered or not, show hide the toolbar's
			corner.setBounds(-18 - shift_x, -18 - shift_y, 18, 18 * 2);
			if (showThreshold) {
				top.setBounds(0, -shift_y, w < 50 ? 50 : w, 0);
				left.setBounds(-18 - shift_x, 18, 0, left.getSetHeight());
				dimthreshbar.setBounds(0, -18 - shift_y, 180, 17);
				recthreshbar.setBounds(-18 - shift_x, 18, 17, 180);
			} else {
				top.setBounds(0, -18 - shift_y, w < 50 ? 50 : w, 17);
				left.setBounds(-18 - shift_x, 18, 18, left.getSetHeight());
				dimthreshbar.hide();
				recthreshbar.hide();
			}
		} else {
			// hide by setting the width to 0
			corner.setBounds(-18 - shift_x, -shift_y, 0, 18 * 2);
			top.setBounds(0, -18 - shift_y, w < 50 ? 50 : w, 17);
			left.setBounds(-18 - shift_x, 20, 0, left.getSetHeight());

			dimthreshbar.hide();
			recthreshbar.hide();
		}


		content.setBounds(0, 0, w, h);

		if (isFocused()) {
			dimLabel.setBounds(0, h, w, 80);
			recLabel.setBounds(w, 0, 80, h);
		} else {
			dimLabel.setBounds(0, h, w, 0);
			recLabel.setBounds(w, 0, 0, h);
		}

		int actDimAnnotation = 0;
		int actRecAnnotation = 0;
		for (IGLLayoutElement elem : children.subList(firstAnnotation, children.size())) {
			EDimension dim = elem.getLayoutDataAs(EDimension.class, null);
			if (dim == null)
				continue;
			if (dim.isHorizontal()) {
				elem.setBounds(-1, -annotationWidth * actDimAnnotation, w + 2, annotationWidth);
				actDimAnnotation++;
			} else {
				elem.setBounds(-annotationWidth * actRecAnnotation, -1, annotationWidth, h + 2);
				actRecAnnotation++;
			}
		}


	}

	/**
	 * @param subList
	 * @param dimension
	 * @return
	 */
	private static int count(List<? extends IGLLayoutElement> l, EDimension dimension) {
		int c = 0;
		for (IGLLayoutElement elem : l)
			if (dimension == elem.getLayoutDataAs(EDimension.class, null))
				c++;
		return c;
	}

	@ListenTo
	private void listenTo(SortingChangeEvent e) {
		if (e.getSender() == this) {
			// only local change
		} else if (this.sortingType != e.getType()) {
			this.sortingType = e.getType();
			resort();
		}
	}

	protected class ThresholdBar extends GLElementDecorator implements
			org.caleydo.core.view.opengl.layout2.basic.GLSlider.ISelectionCallback {

		private final boolean isHorizontal;
		private final GLSlider slider;
		// float globalMaxThreshold;
		private float localMaxSliderValue;

		protected ThresholdBar(boolean layout) {
			isHorizontal = layout;
			// move to the top
			setzDelta(DEFAULT_Z_DELTA);

			// create buttons
			float max = 0;
			this.slider = new GLSlider(0, max, max / 2);
			slider.setCallback(this);
			slider.setHorizontal(isHorizontal);
			slider.setMinMaxVisibility(EValueVisibility.VISIBLE_HOVERED);
			setContent(slider);
			setVisibility(EVisibility.PICKABLE); // for parent
		}

		@Override
		public void onSelectionChanged(GLSlider slider, float value) {
			if (value >= localMaxSliderValue)
				return;
			setThresholdImpl(isHorizontal, value);
		}

		/**
		 * @param dimProbabilities
		 */
		public void updateSliders(FuzzyClustering clustering) {
			localMaxSliderValue = clustering.getAbsMaxValue();
			this.slider.setMinMax(0, localMaxSliderValue);
		}

		// @ListenTo
		// public void listenTo(MaxThresholdChangeEvent event) {
		// globalMaxThreshold = (float) (isHorizontal ? event.getDimThreshold() : event.getRecThreshold());
		// createButtons();
		// }

		@ListenTo
		public void listenTo(LZThresholdChangeEvent event) {
			if (event.isGlobalEvent()) {
				setValue(isHorizontal ? event.getDimensionThreshold() : event.getRecordThreshold());
			}
		}

		/**
		 * @param value
		 */
		public void setValue(float value) {
			slider.setCallback(null); // to avoid that we will be callbacked
			slider.setValue(value);
			slider.setCallback(this);
		}
	}

	/**
	 * @param isDimension
	 * @param value
	 */
	final void setThresholdImpl(boolean isDimension, float value) {
		if ((isDimension && dimThreshold == value) || (!isDimension && recThreshold == value))
			return;
		if (isDimension)
			dimThreshold = value;
		else
			recThreshold = value;
		refilter(false);
	}

	public final void setThreshold(boolean isDimension, float value) {
		setThresholdImpl(isDimension, value);
		if (isDimension && dimThreshBar != null)
			dimThreshBar.setValue(value);
		else if (!isDimension && recThreshBar != null)
			recThreshBar.setValue(value);
	}

	@Override
	protected VirtualArray getDimVirtualArray() {
		return data.getDimensionPerspective().getVirtualArray();
	}

	@Override
	protected VirtualArray getRecVirtualArray() {
		return data.getRecordPerspective().getVirtualArray();
	}

	@Override
	public int getDimSize() {
		return getDimVirtualArray().size();
	}

	@Override
	public int getRecSize() {
		return getRecVirtualArray().size();
	}

	@Override
	public void setFocus(boolean isFocused) {
		super.setFocus(isFocused);
		if (isFocused) {
			for (ALZHeatmapElement annotation : annotations)
				annotation.nonUniformLayout(content);
		} else {
			for (ALZHeatmapElement annotation : annotations)
				annotation.uniformLayout();
		}
	}

	@Override
	public final boolean shouldBeVisible() {
		final int dim = getDimSize();
		final int rec = getRecSize();
		if (isHidden || dim == 0 || rec == 0)
			return false;
		GLRootElement root = findRootElement();
		if (root == null)
			return true;
		float clusterSizeThreshold = root.getClusterSizeThreshold();
		float biggest = root.getBiggestDimSize();
		if ((dim / biggest) < clusterSizeThreshold && (rec / biggest) < clusterSizeThreshold)
			return false;
		return true;
	}

	// FIXME when me or any of my neigbors updates its overlap, we need to do a resort
	// @Override
	// void calculateOverlap(boolean dimBandsEnabled, boolean recBandsEnabled) {
	// super.calculateOverlap(dimBandsEnabled, recBandsEnabled);
	// if (getVisibility() == EVisibility.PICKABLE && sortingType == SortingType.bandSorting)
	// resort();
	// }

	@ListenTo
	private void listenTo(LZThresholdChangeEvent event) {
		if (!event.isGlobalEvent()) {
			return;
		}
		recThreshold = event.getRecordThreshold();
		dimThreshold = event.getDimensionThreshold();
		recNumberThreshold = event.getRecordNumberThreshold();
		dimNumberThreshold = event.getDimensionNumberThreshold();
		refilter(event.isGlobalEvent());
	}


	@Override
	protected void setLabel(String id) {
		data.setLabel(id);
	}


	private void resort() {
		switch (sortingType) {
		case BY_PROBABILITY:
			probabilitySorting();
			break;
		case BY_BAND:
			bandSorting();
			break;
		default:
		}
	}

	private void bandSorting() {
		// Pair<List<IntFloat>, List<IntFloat>> p = filterData();
		//
		// Set<IntFloat> finalDimSorting = bandSort(p.getFirst(), dimOverlap.values());
		// Set<IntFloat> finalRecSorting = bandSort(p.getSecond(), recOverlap.values());
		//
		// updateTablePerspective(new ArrayList<>(finalDimSorting), new ArrayList<>(finalRecSorting));
		fireTablePerspectiveChanged();
	}

	private Set<IntFloat> bandSort(List<IntFloat> indices, Collection<List<Integer>> bands) {
		List<List<Integer>> nonEmptyDimBands = new ArrayList<>();
		for (List<Integer> dimBand : bands) {
			if (dimBand.size() > 0)
				nonEmptyDimBands.add(dimBand);
		}
		BandSorting dimConflicts = new BandSorting(nonEmptyDimBands);

		Set<IntFloat> finalDimSorting = new LinkedHashSet<IntFloat>();

		ImmutableMap<Integer, IntFloat> byIndex = Maps.uniqueIndex(indices, IntFloat.TO_INDEX);
		for (Integer i : dimConflicts) {
			finalDimSorting.add(byIndex.get(i));
		}
		// fill up rest
		finalDimSorting.addAll(indices);
		return finalDimSorting;
	}

	private void probabilitySorting() {
		Pair<List<IntFloat>, List<IntFloat>> p = filterData();

		updateTablePerspective(p.getFirst(), p.getSecond());

		fireTablePerspectiveChanged();
	}

	protected void refilter(boolean isGlobal) {
		if (isLocked && isGlobal)
			return;

		Pair<List<IntFloat>, List<IntFloat>> p = filterData();

		updateTablePerspective(p.getFirst(), p.getSecond());

		updateVisibility();
		triggerDataUpdated(isGlobal);
	}

	private Pair<List<IntFloat>, List<IntFloat>> filterData() {
		List<IntFloat> dims = dimClustering.filter(dimThreshold, dimNumberThreshold);
		List<IntFloat> recs = recClustering.filter(recThreshold, recNumberThreshold);

		Pair<List<IntFloat>, List<IntFloat>> p = Pair.make(dims, recs);
		return p;
	}


	private void updateTablePerspective(List<IntFloat> dims, List<IntFloat> recs) {
		fill(getDimVirtualArray(), dims);
		fill(getRecVirtualArray(), recs);

		this.data.invalidateContainerStatistics();

		for (ALZHeatmapElement annotation : annotations)
			if (annotation.getDim().isHorizontal())
				annotation.update(dims);
			else
				annotation.update(recs);
	}

	public void addAnnotation(ALZHeatmapElement annotation) {
		this.add(annotation);
		this.annotations.add(annotation);
	}

	/**
	 * @return the annotations, see {@link #annotations}
	 */
	public Collection<ALZHeatmapElement> getAnnotations() {
		return Collections.unmodifiableCollection(annotations);
	}

	public void removeAnnotation(ALZHeatmapElement annotation) {
		if (this.annotations.remove(annotation))
			this.remove(annotation);
	}

	private void fill(VirtualArray va, List<IntFloat> values) {
		va.clear();
		va.addAll(Lists.transform(values, IntFloat.TO_INDEX));
	}

	private void triggerDataUpdated(boolean isGlobal) {
		EventPublisher.trigger(new ClusterScaleEvent(this));
		if (!isGlobal)
			EventPublisher.trigger(new MouseOverClusterEvent(this, true));
		// FIXME
		// EventPublisher.trigger(new RecalculateOverlapEvent(this, isGlobal, dimBandsEnabled, recBandsEnabled));
		// update bands
	}

	private GLButton createHideClusterButton() {
		GLButton hide = new GLButton();
		hide.setRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_CLOSE));
		hide.setTooltip("Close");
		hide.setSize(18, 18);
		hide.setCallback(new ISelectionCallback() {

			@Override
			public void onSelectionChanged(GLButton button, boolean selected) {
				hideThisCluster();
			}

		});
		return hide;
	}

	protected class ToolBar extends GLElementContainer implements ISelectionCallback {
		private GLButton enlarge, smaller, focus, lock;

		public ToolBar(GLElement switcher) {
			super(GLLayouts.flowVertical(3));
			setzDelta(-0.1f);
			this.add(switcher);
			createButtons();
			setSize(Float.NaN, this.size() * (16 + 3));
			setVisibility(EVisibility.PICKABLE);
		}

		protected void createButtons() {
			focus = new GLButton(GLButton.EButtonMode.CHECKBOX);
			focus.setRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_FOCUS));
			focus.setSelectedRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_FOCUS_OUT));
			focus.setSize(16, 16);
			focus.setTooltip("Focus this Cluster");
			focus.setCallback(this);
			this.add(focus);
			lock = new GLButton(GLButton.EButtonMode.CHECKBOX);
			lock.setTooltip("Lock this cluster. It will not recieve threshold updates.");
			lock.setSize(16, 16);
			lock.setCallback(this);
			lock.setRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_LOCK));
			lock.setSelectedRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_UNLOCK));
			this.add(lock);
			enlarge = new GLButton();
			enlarge.setSize(16, 16);
			enlarge.setTooltip("Enlarge");
			enlarge.setRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_ZOOM_IN));
			enlarge.setCallback(this);
			this.add(enlarge);
			smaller = new GLButton();
			smaller.setTooltip("Reduce");
			smaller.setSize(16, 16);
			smaller.setRenderer(GLRenderers.fillImage(BiClusterRenderStyle.ICON_ZOOM_OUT));
			smaller.setCallback(this);
			this.add(smaller);

		}

		@Override
		public void onSelectionChanged(GLButton button, boolean selected) {
			if (button == enlarge) {
				upscale();
				resize();
			} else if (button == smaller) {
				reduceScaleFactor();
				resize();
			} else if (button == focus) {
				changeFocus(selected);
			} else if (button == lock) {
				toggleLocked();
				lock.setTooltip(isLocked ? "UnLock this cluster. It will again recieve threshold updates."
						: "Lock this cluster. It will not recieve threshold updates.");
			}
		}

	}

	void changeFocus(boolean selected) {
		findAllClustersElement().setFocus(selected ? this : null);
	}

	public void toggleLocked() {
		isLocked = !isLocked;
	}
}
