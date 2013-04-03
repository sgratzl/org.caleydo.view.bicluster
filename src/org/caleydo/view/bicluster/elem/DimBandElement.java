/*******************************************************************************
 * Caleydo - visualization for molecular biology - http://caleydo.org
 *
 * Copyright(C) 2005, 2012 Graz University of Technology, Marc Streit, Alexander
 * Lex, Christian Partl, Johannes Kepler University Linz </p>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.caleydo.view.bicluster.elem;

import gleem.linalg.Vec2f;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GLContext;

import org.caleydo.core.data.selection.EventBasedSelectionManager;
import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.util.color.Colors;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.picking.Pick;

/**
 * @author Michael Gillhofer
 *
 */
public class DimBandElement extends BandElement {

	private static float[] color = Colors.NEUTRAL_GREY.getRGBA();

	/**
	 * @param view
	 */
	public DimBandElement(GLElement first, GLElement second, AllBandsElement root) {
		super(first, second, ((ClusterElement) first).getDimensionIDCategory(), ((ClusterElement) first)
				.getDimOverlap(second), ((ClusterElement) first).getDimensionIDType(), root);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.caleydo.core.view.opengl.layout2.GLElementAdapter#renderImpl(org.caleydo.core.view.opengl.layout2.GLGraphics,
	 * float, float)
	 */
	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		if (visible) {
			bandRenderer.renderComplexBand(GLContext.getCurrentGL().getGL2(), bandPoints, highlight,
					highlight ? Colors.RED.getRGBA() : color, .5f);
			if (highlightOverlap.size() > 0)
				bandRenderer.renderComplexBand(GLContext.getCurrentGL().getGL2(), highlightPoints, highlight,
						Colors.RED.getRGBA(), .5f);
		}
		// super.renderImpl(g, w, h);
		// System.out.println(first.getId() + "/" + second.getId());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.caleydo.core.view.opengl.layout2.GLElement#renderPickImpl(org.caleydo.core.view.opengl.layout2.GLGraphics,
	 * float, float)
	 */
	@Override
	protected void renderPickImpl(GLGraphics g, float w, float h) {
		if (visible) {
			bandRenderer.renderComplexBand(GLContext.getCurrentGL().getGL2(), bandPoints, false, color, .5f);
			bandRenderer.renderComplexBand(GLContext.getCurrentGL().getGL2(), highlightPoints, false, color, .5f);
		}
	}

	@Override
	protected void onClicked(Pick pick) {
		highlight = !highlight;
		if (highlight)
			root.setSelection(this);
		else
			root.setSelection(null);
		selectElements();
		super.onClicked(pick);
	}

	@Override
	public void updatePosition() {
		overlap = first.getDimOverlap(second);
		int overlapSize = overlap.size();
		if (overlapSize > 0 && first.isVisible() && second.isVisible()) {
			visible = true;
			double endDimBandScaleFactor = second.getSize().x() / (double) second.getNumberOfDimElements();
			double startDimBandScaleFactor = first.getSize().x() / (double) first.getNumberOfDimElements();
			addPointsToBand(startDimBandScaleFactor, endDimBandScaleFactor);
		} else
			visible = false;
		repaintAll();

	}

	private void addPointsToBand(double firDimScaFac, double secDimScFac) {
		Vec2f fLoc = first.getLocation();
		Vec2f sLoc = second.getLocation();
		Vec2f fSize = first.getSize();
		Vec2f sSize = second.getSize();
		bandPoints = new ArrayList<>();
		highlightPoints = new ArrayList<>();
		int os = overlap.size();
		int hOS = highlightOverlap.size();
		if (fLoc.y() < sLoc.y()) {
			// first on top
			if (fLoc.y() + fSize.y() < sLoc.y()) {
				// second far at the bottom
				if (hOS > 0) {
					highlightPoints.add(pair(fLoc.x(), fLoc.y() + fSize.y(), (float) (fLoc.x() + firDimScaFac * hOS),
							fLoc.y() + fSize.y()));
					highlightPoints.add(pair(sLoc.x(), sLoc.y(), (float) (sLoc.x() + secDimScFac * hOS), sLoc.y()));
					bandPoints.add(pair((float) (fLoc.x() + firDimScaFac * hOS), fLoc.y() + fSize.y(),
							(float) (fLoc.x() + firDimScaFac * os), fLoc.y() + fSize.y()));
					bandPoints.add(pair((float) (sLoc.x() + secDimScFac * hOS), sLoc.y(),
							(float) (sLoc.x() + secDimScFac * os), sLoc.y()));
				} else {
					bandPoints.add(pair(fLoc.x(), fLoc.y() + fSize.y(), (float) (fLoc.x() + firDimScaFac * os),
							fLoc.y() + fSize.y()));
					bandPoints.add(pair(sLoc.x(), sLoc.y(), (float) (sLoc.x() + secDimScFac * os), sLoc.y()));
				}
			} else {
				// second in between
				if (hOS > 0) {
					highlightPoints.add(pair(fLoc.x(), fLoc.y(), (float) (fLoc.x() + firDimScaFac * hOS), fLoc.y()));
					highlightPoints.add(pair(sLoc.x(), sLoc.y(), (float) (sLoc.x() + secDimScFac * hOS), sLoc.y()));
					bandPoints.add(pair((float) (fLoc.x() + firDimScaFac * hOS), fLoc.y(),
							(float) (fLoc.x() + firDimScaFac * os), fLoc.y()));
					bandPoints.add(pair((float) (sLoc.x() + secDimScFac * hOS), sLoc.y(),
							(float) (sLoc.x() + secDimScFac * os), sLoc.y()));

				} else {
					bandPoints.add(pair(fLoc.x(), fLoc.y(), (float) (fLoc.x() + firDimScaFac * os), fLoc.y()));
					bandPoints.add(pair(sLoc.x(), sLoc.y(), (float) (sLoc.x() + secDimScFac * os), sLoc.y()));
				}
			}

		} else {
			// second on top
			if (sLoc.y() + sSize.y() < fLoc.y()) {
				// second far at the top
				if (hOS > 0) {
					highlightPoints.add(pair(sLoc.x(), sLoc.y() + sSize.y(), (float) (sLoc.x() + secDimScFac * hOS),
							sLoc.y() + sSize.y()));
					highlightPoints.add(pair(fLoc.x(), fLoc.y(), (float) (fLoc.x() + firDimScaFac * hOS), fLoc.y()));
					bandPoints.add(pair((float) (sLoc.x() + secDimScFac * hOS), sLoc.y() + sSize.y(),
							(float) (sLoc.x() + secDimScFac * os), sLoc.y() + sSize.y()));
					bandPoints.add(pair((float) (fLoc.x() + firDimScaFac * hOS), fLoc.y(),
							(float) (fLoc.x() + firDimScaFac * os), fLoc.y()));
				} else {
					bandPoints.add(pair(sLoc.x(), sLoc.y() + sSize.y(), (float) (sLoc.x() + secDimScFac * os), sLoc.y()
							+ sSize.y()));
					bandPoints.add(pair(fLoc.x(), fLoc.y(), (float) (fLoc.x() + firDimScaFac * os), fLoc.y()));
				}
			} else {
				if (hOS > 0) {
					highlightPoints.add(pair(fLoc.x(), fLoc.y(), (float) (fLoc.x() + firDimScaFac * hOS), fLoc.y()));
					highlightPoints.add(pair(sLoc.x(), sLoc.y(), (float) (sLoc.x() + secDimScFac * hOS), sLoc.y()));
					bandPoints.add(pair((float) (fLoc.x() + firDimScaFac * hOS), fLoc.y(),
							(float) (fLoc.x() + firDimScaFac * os), fLoc.y()));
					bandPoints.add(pair((float) (sLoc.x() + secDimScFac * hOS), sLoc.y(),
							(float) (sLoc.x() + secDimScFac * os), sLoc.y()));
				} else {
					bandPoints.add(pair(fLoc.x(), fLoc.y(), (float) (fLoc.x() + firDimScaFac * os), fLoc.y()));
					bandPoints.add(pair(sLoc.x(), sLoc.y(), (float) (sLoc.x() + secDimScFac * os), sLoc.y()));
				}
			}
		}
	}

	private Pair<Point2D, Point2D> pair(float x1, float y1, float x2, float y2) {
		Point2D _1 = new Point2D.Float(x1, y1);
		Point2D _2 = new Point2D.Float(x2, y2);
		return Pair.make(_1, _2);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.caleydo.core.data.selection.IEventBasedSelectionManagerUser#notifyOfSelectionChange(org.caleydo.core.data
	 * .selection.EventBasedSelectionManager)
	 */
	@Override
	public void notifyOfSelectionChange(EventBasedSelectionManager selectionManager) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.caleydo.view.bicluster.elem.BandElement#highlightOverlapWith(org.caleydo.view.bicluster.elem.BandElement)
	 */
	@Override
	public void highlightOverlapWith(BandElement b) {
		highlightOverlap = new ArrayList<>();
		if (b instanceof DimBandElement) {
			List<Integer> highList = new LinkedList<>(overlap);
			highList.retainAll(b.overlap);
			highlightOverlap = highList;
		}
		updatePosition();

	}

}
