/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.bicluster.elem;

import java.util.List;

import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.layout.GLLayouts;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactories;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactories.GLElementSupplier;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext.Builder;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher.ELazyiness;
import org.caleydo.view.heatmap.v2.HeatMapElement;
import org.caleydo.view.heatmap.v2.HeatMapElement.EShowLabels;
import org.caleydo.view.heatmap.v2.SpacingStrategies;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 * @author Samuel Gratzl
 *
 */
public class ClusterContentElement extends GLElementContainer implements IGLLayout {
	private final HeatMapElement heatmap;

	/**
	 * @param builder
	 */
	public ClusterContentElement(Builder builder) {
		setLayout(GLLayouts.flowVertical(2));
		builder.set("histogram.showColorMapper", false);
		GLElementFactoryContext context = builder.build();
		ImmutableList<GLElementSupplier> extensions = GLElementFactories.getExtensions(context, "bicluster",
				Predicates.alwaysTrue());
		GLElementFactorySwitcher content = new GLElementFactorySwitcher(extensions, ELazyiness.NONE);
		heatmap = (HeatMapElement) content.get("heatmap");
		assert heatmap != null;
		this.add(content);
		this.add(content.createButtonBar());
	}

	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		g.color(Color.WHITE).fillRect(0, 0, w, h);
		super.renderImpl(g, w, h);
	}

	@Override
	public void doLayout(List<? extends IGLLayoutElement> children, float w, float h) {
		// TODO
	}
	/**
	 * @param right
	 */
	public void showLabels(EShowLabels right) {
		heatmap.setDimensionLabels(EShowLabels.RIGHT);
		heatmap.setRecordLabels(EShowLabels.RIGHT);
		heatmap.setRecordSpacingStrategy(SpacingStrategies.fishEye(18));
		heatmap.setDimensionSpacingStrategy(SpacingStrategies.fishEye(18));
	}

	/**
	 *
	 */
	public void hideLabels() {
		heatmap.setDimensionLabels(EShowLabels.NONE);
		heatmap.setRecordLabels(EShowLabels.NONE);
		heatmap.setRecordSpacingStrategy(SpacingStrategies.UNIFORM);
		heatmap.setDimensionSpacingStrategy(SpacingStrategies.UNIFORM);
	}

	/**
	 * @param ind
	 * @return
	 */
	public float getDimensionPos(int index) {
		return heatmap.getDimensionCellSpace(index).getPosition();
	}

	/**
	 * @param ind
	 * @return
	 */
	public float getRecordPos(int index) {
		return heatmap.getRecordCellSpace(index).getPosition();
	}
}
