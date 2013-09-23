/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.bicluster.elem.band;

import gleem.linalg.Vec2f;
import gleem.linalg.Vec3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.caleydo.core.data.selection.SelectionManager;
import org.caleydo.core.data.selection.SelectionType;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.event.EventPublisher;
import org.caleydo.core.id.IIDTypeMapper;
import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.IGLElementContext;
import org.caleydo.core.view.opengl.layout2.PickableGLElement;
import org.caleydo.core.view.opengl.layout2.util.PickingPool;
import org.caleydo.core.view.opengl.picking.IPickingLabelProvider;
import org.caleydo.core.view.opengl.picking.IPickingListener;
import org.caleydo.core.view.opengl.picking.Pick;
import org.caleydo.core.view.opengl.picking.PickingListenerComposite;
import org.caleydo.core.view.opengl.util.gleem.ColoredVec3f;
import org.caleydo.core.view.opengl.util.spline.Band;
import org.caleydo.view.bicluster.elem.ClusterElement;
import org.caleydo.view.bicluster.elem.EDimension;
import org.caleydo.view.bicluster.elem.Edge;
import org.caleydo.view.bicluster.event.MouseOverBandEvent;
import org.caleydo.view.bicluster.event.MouseOverClusterEvent;
import org.caleydo.view.bicluster.util.SetUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

/**
 * @author Michael Gillhofer
 *
 */
public class BandElement extends PickableGLElement implements IPickingLabelProvider {

	private static final Color highlightColor = SelectionType.SELECTION.getColor();
	private static final Color hoveredColor = SelectionType.MOUSE_OVER.getColor();

	private static final float HIGH_OPACITY_FACTPOR = 1;
	private static final float LOW_OPACITY_FACTOR = 0.15f;

	private static final float DEFAULT_Z_DELTA = -4;
	private static final float HOVERED_BACKGROUND_Z_DELTA = -3;
	private static final float SELECTED_Z_DELTA = -2;
	private static final float HOVERED_Z_DELTA = -1;

	protected final Edge edge;
	private final EDimension dimension;
	protected List<Integer> overlap;
	private boolean hasSharedElementsWithSelection;
	private boolean hasSharedElementsWithHover;

	protected final SelectionManager selectionManager;
	private final IIDTypeMapper<Integer, String> id2label;

	protected Map<List<Integer>, Band> splittedBands, nonSplittedBands;
	protected Map<Integer, List<Vec2f>> splines;
	protected boolean isMouseOver = false;
	protected boolean isAnyThingHovered = false;

	protected PickingPool pickingPool;
	protected IPickingListener pickingListener;
	private int actSelectedSplineID = -1;

	private float targetOpacityFactor = 1;

	public BandElement(Edge edge, EDimension dimension, SelectionManager selectionManager,
			IIDTypeMapper<Integer, String> id2label) {
		this.edge = edge;
		this.dimension = dimension;
		this.id2label = id2label;
		this.selectionManager = selectionManager;

		hasSharedElementsWithSelection = false;
		hasSharedElementsWithHover = false;

		setZDeltaAccordingToState();
		initBand();
		setPicker(null);
	}

	/**
	 * @return the dimension, see {@link #dimension}
	 */
	public EDimension getDimension() {
		return dimension;
	}

	protected static ImmutableList<Integer> toFastOverlap(Collection<Integer> list) {
		return ImmutableSortedSet.copyOf(list).asList();
	}

	/**
	 * @return the first, see {@link #first}
	 */
	public final ClusterElement getFirst() {
		return edge.getA();
	}

	/**
	 * @return the second, see {@link #second}
	 */
	public final ClusterElement getSecond() {
		return edge.getB();
	}

	@Override
	protected void init(IGLElementContext context) {
		pickingListener = new IPickingListener() {
			@Override
			public void pick(Pick pick) {
				onSplinePicked(pick);
			}
		};
		pickingPool = new PickingPool(context, PickingListenerComposite.concat(pickingListener, context.getSWTLayer()
				.createTooltip(this)));
		super.init(context);
	}
	@Override
	protected void takeDown() {
		pickingPool.clear();
		super.takeDown();
	}

	private void setZDeltaAccordingToState() {
		float oldZ = getzDelta();
		float z = DEFAULT_Z_DELTA;
		if (hasSharedElementsWithHoveredBand())
			z = HOVERED_BACKGROUND_Z_DELTA;
		if (hasSharedElementsWithSelectedBand())
			z = SELECTED_Z_DELTA;
		if (isMouseOver)
			z = HOVERED_Z_DELTA;
		if (oldZ != z) {
			setzDelta(z);
			final AllBandsElement p = findAllBands();
			if (p != null)
				p.triggerResort();
		}
	}

	@Override
	public String getLabel(Pick pick) {
		int id = pick.getObjectID() - 1;
		Set<String> labels = id2label.apply(id);
		return StringUtils.join(labels, ",");
	}

	protected void onSplinePicked(Pick pick) {
		switch (pick.getPickingMode()) {
		case CLICKED:
			selectElement(SelectionType.SELECTION, pick.getObjectID(), true);
			repaint();
			break;
		case MOUSE_OVER:
			this.actSelectedSplineID = pick.getObjectID();
			repaint();
			break;
		case MOUSE_OUT:
			this.actSelectedSplineID = -1;
			repaint();
			break;
		default:
			break;
		}
	}
	@Override
	protected void onPicked(Pick pick) {
		switch (pick.getPickingMode()) {
		case CLICKED:
			onClicked(pick);
			break;
		case MOUSE_OUT:
			onMouseOut(pick);
			break;
		case MOUSE_OVER:
			onMouseOver(pick);
			break;
		default:
			return;
		}
		setZDeltaAccordingToState();
	}

	protected void initBand() {
		updateStructure();
	}

	@Override
	public void layout(int deltaTimeMs) {
		updateOpacticy(deltaTimeMs);
		super.layout(deltaTimeMs);
	}

	private void updateOpacticy(int deltaTimeMs) {
		float delta = Math.abs(actOpacityFactor - targetOpacityFactor);
		if (delta < 0.01f) // done
			return;
		final float speed = 0.003f; // [units/ms]
		float back = actOpacityFactor;

		final float change = deltaTimeMs * speed;

		if (targetOpacityFactor < actOpacityFactor)
			actOpacityFactor = Math.max(targetOpacityFactor, actOpacityFactor - change);
		else
			actOpacityFactor = Math.min(targetOpacityFactor, actOpacityFactor + change);
		if (back != actOpacityFactor) {
			repaint();
		}
	}

	protected final void updateVisibilityByOverlap() {
		if (!overlap.isEmpty())
			setVisibility(EVisibility.PICKABLE);
		else
			setVisibility(EVisibility.NONE);
	}

	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		Color bandColor;
		if (isVisible()) {
			if (hasSharedElementsWithSelectedBand())
				bandColor = highlightColor;
			else if (hasSharedElementsWithHoveredBand())
				bandColor = hoveredColor;
			else
				bandColor = dimension.getBandColor();
			if (isMouseOver) {
				g.color(bandColor.r, bandColor.g, bandColor.b, 0.8f * actOpacityFactor);
				for (Band b : splittedBands.values()) {
					g.drawPath(b);
				}
				g.color(bandColor.r, bandColor.g, bandColor.b, 0.5f * actOpacityFactor);
				for (Band b : splittedBands.values()) {
					g.fillPolygon(b);
				}

				g.color(bandColor.r, bandColor.g, bandColor.b, 0.25f * actOpacityFactor);
				List<Vec2f> currSelectedSpline = splines.get(actSelectedSplineID - 1);
				for (List<Vec2f> b : splines.values()) {
					if (b == currSelectedSpline)
						continue;
					g.drawPath(b, false);
				}
				if (currSelectedSpline != null) {
					g.color(0, 0, 0, 0.85f * actOpacityFactor).lineWidth(2);
					g.drawPath(currSelectedSpline, false);
					g.lineWidth(1);
				}
			} else {
				Color col = bandColor.clone();
				col.a = 0.8f;
				g.color(bandColor.r, bandColor.g, bandColor.b, 0.8f * actOpacityFactor);
				Collection<Band> stubBands;
				if (!hasSelections())
					// stub only if we haven't any highlights
					stubBands = stubify(nonSplittedBands.values(), col, actOpacityFactor, HIGH_OPACITY_FACTPOR);
				else {
					stubBands = nonSplittedBands.values();
				}
				for (Band b : stubBands) {
					g.drawPath(b);
				}
				g.color(bandColor.r, bandColor.g, bandColor.b, 0.5f * actOpacityFactor);
				for (Band b : stubBands) {
					g.fillPolygon(b);
				}
			}
		}
	}


	protected boolean isVisible() {
		return getFirst().isVisible() && getSecond().isVisible() && overlap != null && !overlap.isEmpty();
	}

	protected boolean hasSharedElementsWithSelectedBand() {
		return hasSharedElementsWithSelection;
	}

	protected boolean hasSharedElementsWithHoveredBand() {
		return hasSharedElementsWithHover;
	}

	@Override
	protected void renderPickImpl(GLGraphics g, float w, float h) {
		if (getVisibility() == EVisibility.PICKABLE && !isAnyThingHovered && isVisible()) {
			g.color(dimension.getBandColor());
			if (isMouseOver) {
				for (Band b : splittedBands.values())
					g.fillPolygon(b);
				g.incZ();
				for (Integer elementIndex : splines.keySet()) {
					g.pushName(pickingPool.get(elementIndex + 1));
					g.fillPolygon(splines.get(elementIndex));
					g.popName();
				}
				g.decZ();
			} else {
				if (nonSplittedBands != null)
					for (Band b : nonSplittedBands.values())
						g.fillPolygon(b);
			}
		}
	}

	@Override
	protected void onClicked(Pick pick) {
		if (hasSharedElementsWithSelectedBand()) { // disable the selection again
			hasSharedElementsWithSelection = false;
			findAllBands().setSelection(null);
			selectElement(SelectionType.SELECTION, -1, true);
		} else {
			hasSharedElementsWithSelection = true;
			findAllBands().setSelection(this);
			selectElement(SelectionType.SELECTION, -1, true);
		}
	}

	protected AllBandsElement findAllBands() {
		return findParent(AllBandsElement.class);
	}

	protected void selectElement(SelectionType type, int objectId, boolean select) {
		if (selectionManager == null)
			return;
		findAllBands().clearAll(type);
		if (select) {
			if (objectId <= 0)
				selectionManager.addToType(type, overlap);
			else
				selectionManager.addToType(type, objectId - 1);
		}
		findAllBands().fireAllSelections(this);
	}

	public void deselect() {
		hasSharedElementsWithSelection = false;
		setZDeltaAccordingToState();
	}

	@Override
	protected void onMouseOver(Pick pick) {
		isMouseOver = true;
		selectElement(SelectionType.MOUSE_OVER, -1, true);
		hasSharedElementsWithHover = true;
		EventPublisher.trigger(new MouseOverBandEvent(this, true));
		repaintAll();
	}

	@Override
	protected void onMouseOut(Pick pick) {
		isMouseOver = false;
		hasSharedElementsWithHover = false;
		this.actSelectedSplineID = -1;
		EventPublisher.trigger(new MouseOverBandEvent(this, false));
		selectElement(SelectionType.MOUSE_OVER, -1, false);
		repaintAll();
	}

	public void updateStructure() {
		if (!edge.anyVisible())
			return;
		overlap = toFastOverlap(edge.getOverlapIndices(dimension));
		updateVisibilityByOverlap();
		ClusterElement first = edge.getA();
		ClusterElement second = edge.getB();
		List<List<Integer>> firstSubIndices = first.getListOfContinousSequences(dimension, overlap);
		List<List<Integer>> secondSubIndices = second.getListOfContinousSequences(dimension, overlap);
		if (firstSubIndices.size() == 0)
			return;
		BandFactory bandFactory = createFactory(dimension, first, second, firstSubIndices, secondSubIndices, overlap);
		nonSplittedBands = bandFactory.getNonSplitableBands();

		splittedBands = bandFactory.getSplitableBands();
		splines = bandFactory.getConnectionsSplines();

		if (pickingPool != null) {
			pickingPool.clear();
		}

		// RECORD special
		// nonSplittedBands = bandFactory.getNonSplitableBands();
		// // splittedBands = bandFactory.getSplitableBands();
		//
		// // splitted bands are not looking really helpfull for records
		// splittedBands= nonSplittedBands;
		//
		// // splines = bandFactory.getConnectionsSplines();
		// splines = new HashMap<>(); // create empty hashmap .. splines are not looking very good

		repaintAll();
	}

	private static BandFactory createFactory(EDimension dim, ClusterElement cluster, ClusterElement other,
			List<List<Integer>> firstSubIndices, List<List<Integer>> secondSubIndices, List<Integer> overlap) {
		return dim == EDimension.DIMENSION ? new DimensionBandFactory(cluster, other, firstSubIndices, secondSubIndices,
				overlap) : new RecordBandFactory(cluster, other, firstSubIndices, secondSubIndices, overlap);
	}

	public void updatePosition() {
		updateStructure();
	}

	public void onSelectionUpdate(SelectionManager manager) {
		if (manager != selectionManager)
			return;
		hasSharedElementsWithHover = SetUtils.containsAny(overlap,
				selectionManager.getElements(SelectionType.MOUSE_OVER));
		hasSharedElementsWithSelection = SetUtils.containsAny(overlap,
				selectionManager.getElements(SelectionType.SELECTION));
		setZDeltaAccordingToState();
	}


	protected float actOpacityFactor = 1;

	@ListenTo
	private void listenTo(MouseOverClusterEvent event) {
		isAnyThingHovered = event.isMouseOver();
		if (!event.isMouseOver() || hasSelections()) {
			targetOpacityFactor = HIGH_OPACITY_FACTPOR;
		} else if (isNearEnough((ClusterElement) event.getSender()))
			targetOpacityFactor = HIGH_OPACITY_FACTPOR;
		else
			targetOpacityFactor = LOW_OPACITY_FACTOR;
		setZDeltaAccordingToState();
	}

	/**
	 * @param sender
	 * @return
	 */
	private boolean isNearEnough(ClusterElement c) {
		if (c == getFirst() || c == getSecond())
			return true;
		return c.nearEnough(getFirst(), getSecond()); // as symmetric
	}

	@ListenTo
	private void listenTo(MouseOverBandEvent event) {
		if (event.getSender() == this)
			return;
		isAnyThingHovered = event.isMouseOver();
		boolean fadeOut = event.isMouseOver() && !hasSelections()
				&& !isNearEnough(event.getFirst(), event.getSecond());
		if (fadeOut) {
			targetOpacityFactor = LOW_OPACITY_FACTOR;
			// if (isOtherType(event.getBand()))
			// setVisibility(EVisibility.NONE);
		} else {
			targetOpacityFactor = HIGH_OPACITY_FACTPOR;
			// updateVisibilityByOverlap();
		}
		setZDeltaAccordingToState();
	}

	/**
	 * @param first2
	 * @param second2
	 * @return
	 */
	private boolean isNearEnough(ClusterElement first, ClusterElement second) {
		ClusterElement f = getFirst();
		ClusterElement s = getSecond();
		if (first == f || first == s || second == f || second == s)
			return true;
		return first.nearEnough(f, s) || second.nearEnough(f, s);
	}

	// /**
	// * whether the bands are from different types
	// *
	// * @param band
	// * @return
	// */
	// private boolean isOtherType(BandElement band) {
	// return !band.getClass().equals(this.getClass());
	// }

	private boolean hasSelections() {
		return hasSharedElementsWithHoveredBand() || hasSharedElementsWithSelectedBand();
	}

	protected Pair<Vec3f, Vec3f> pair(float x1, float y1, float x2, float y2) {
		Vec3f _1 = new Vec3f(x1, y1, 0);
		Vec3f _2 = new Vec3f(x2, y2, 0);
		return Pair.make(_1, _2);
	}

	/**
	 * @param values
	 * @param bandColor
	 * @param curOpacityFactor2
	 * @return
	 */
	private static Collection<Band> stubify(Collection<Band> bands, Color color, float centerAlpha, float maxAlpha) {
		if (bands.isEmpty() || centerAlpha >= 1)
			return bands;

		Collection<Band> result = new ArrayList<>(bands.size());

		for (Band band : bands) {
			stubify(result, band, color, centerAlpha, maxAlpha);
		}
		return result;
	}

	private static void stubify(Collection<Band> result, Band band, Color color, float centerAlpha, float maxAlpha) {
		float r = color.r;
		float g = color.g;
		float b = color.b;
		float a = color.a;

		List<Vec3f> curveTop = band.getCurveTop();
		List<Vec3f> curveBottom = band.getCurveBottom();
		assert curveTop.size() == curveBottom.size();

		final int size = curveTop.size();

		List<Vec3f> cOut = new ArrayList<>(size);
		List<Vec3f> bOut = new ArrayList<>(size);

		boolean even = size % 2 == 0;
		int center = size % 2 == 0 ? (size / 2 - 1) : size / 2;

		float delta_a = (centerAlpha - maxAlpha) / (center * 0.3f); // artificial enlarge delta for better fading effect
		float act_a = 1;
		int firstAlpha = center;
		for (int i = 0; i < size; ++i) {
			Vec3f top = curveTop.get(i);
			Vec3f bottom = curveBottom.get(i);

			float a_i = Math.max(a * act_a, 0);
			if (a_i <= 0)
				firstAlpha = Math.min(firstAlpha, i);
			Color act = new Color(r, g, b, a_i);
			// System.out.println(i + " " + act_a);
			// manipulate act
			if (even && i == center) {
				// nothing
			} else if (i <= center)
				act_a += delta_a;
			else
				act_a -= delta_a;
			cOut.add(new ColoredVec3f(top, act));
			bOut.add(new ColoredVec3f(bottom, act));
		}

		// split the band into two to avoid tesselation effects
		result.add(new Band(cOut.subList(0, firstAlpha + 2), bOut.subList(0, firstAlpha + 2)));
		result.add(new Band(cOut.subList(size - firstAlpha - 1, size), bOut.subList(size - firstAlpha - 1, size)));
	}
}
