/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.bicluster.elem.layout;

import gleem.linalg.Vec2f;

import java.util.List;

import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.event.EventPublisher;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout2;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.view.bicluster.elem.AllClustersElement;
import org.caleydo.view.bicluster.elem.ClusterElement;
import org.caleydo.view.bicluster.elem.EDimension;
import org.caleydo.view.bicluster.event.ForceChangeEvent;
import org.caleydo.view.bicluster.event.UpdateBandsEvent;

/**
 * @author Samuel Gratzl
 *
 */
public abstract class AForceBasedLayout implements IGLLayout2 {
	protected final AllClustersElement parent;

	protected float repulsion = 100000f;
	protected float attractionFactor = 100f;
	protected float borderForceFactor = 200f;

	public AForceBasedLayout(AllClustersElement parent) {
		this.parent = parent;
	}

	@Override
	public final boolean doLayout(List<? extends IGLLayoutElement> children, float w, float h, IGLLayoutElement parent,
			int deltaTimeMs) {
		setSizes(children, w, h);
		boolean r = forceBasedLayout(children, w, h, deltaTimeMs);
		EventPublisher.trigger(new UpdateBandsEvent());
		return r;
	}


	private void setSizes(List<? extends IGLLayoutElement> children, float w, float h) {
		for (IGLLayoutElement child : children) {
			GLElement g = child.asElement();
			if (!(g instanceof ClusterElement)) {
				child.hide();
				continue;
			}

			ClusterElement elem = (ClusterElement) g;
			if (!elem.isVisible()) {
				child.hide();
				continue;
			}

			float scaleX = elem.getZoom(EDimension.DIMENSION);
			float scaleY = elem.getZoom(EDimension.RECORD);
			if (elem.needsUniformScaling())
				scaleX = scaleY = (scaleX + scaleY) * 0.5f; // mean

			Vec2f minSize = elem.getMinSize();
			child.setSize(minSize.x() * scaleX, minSize.y() * scaleY);
		}
	}

	protected abstract boolean forceBasedLayout(List<? extends IGLLayoutElement> children, float w, float h,
			int deltaTimeMs);

	@ListenTo
	private void listenTo(ForceChangeEvent e) {
		repulsion = e.getRepulsionForce();
		attractionFactor = e.getAttractionForce();
		borderForceFactor = e.getBoarderForce();
		parent.relayout();
	}

	protected static int computeNumberOfIterations(int deltaTimeMs) {
		final float iterationFactor = 1000;

		int iterations = (int) ((float) 1 / deltaTimeMs * iterationFactor) + 1;
		return iterations;
	}
}