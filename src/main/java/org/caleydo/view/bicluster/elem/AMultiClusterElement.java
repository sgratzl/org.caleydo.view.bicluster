/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.bicluster.elem;

import gleem.linalg.Vec2f;

import org.caleydo.core.data.perspective.table.TablePerspective;
import org.caleydo.core.view.opengl.canvas.EDetailLevel;
import org.caleydo.core.view.opengl.layout2.GLElementAccessor;
import org.caleydo.core.view.opengl.layout2.basic.ScrollingDecorator.IHasMinSize;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext.Builder;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

/**
 * e.g. a class for representing a cluster
 *
 * @author Michael Gillhofer
 * @author Samuel Gratzl
 */
public abstract class AMultiClusterElement extends ClusterElement {
	protected final ClusterContentElement content;

	private static final Predicate<String> notSingle = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			return !ImmutableSet.of("sbar", "sheatmap").contains(input);
		}
	};


	public AMultiClusterElement(int bcNr, TablePerspective data, BiClustering clustering,
			Predicate<? super String> predicate) {
		super(bcNr, data, clustering);

		content = createContent(predicate);
		this.add(content);
	}

	@Override
	public final String getID() {
		return data.getLabel();
	}

	@Override
	public void relayoutContent() {
		GLElementAccessor.relayoutDown(content);
		content.updateScroller();
	}

	/**
	 * @return
	 */
	protected final ClusterContentElement createContent(Predicate<? super String> filter) {
		Builder builder = GLElementFactoryContext.builder();
		builder.withData(data);
		builder.put(EDetailLevel.class, EDetailLevel.MEDIUM);
		ClusterContentElement c = new ClusterContentElement(builder, Predicates.and(filter, notSingle));

		// trigger a scale event on vis change
		c.onActiveChanged(new GLElementFactorySwitcher.IActiveChangedCallback() {
			@Override
			public void onActiveChanged(int active) {
				relayoutParent();
			}
		});
		c.setMinSizeProvider(new IHasMinSize() {
			@Override
			public Vec2f getMinSize() {
				return getLayoutDataAs(Vec2f.class, AMultiClusterElement.this.getMinSize());
			}
		});
		return c;
	}


	@Override
	public void setFocus(boolean isFocused) {
		super.setFocus(isFocused);
		content.changeFocus(isFocused);
	}

	@Override
	public final float getDimPosOf(int id) {
		if (isFocused()) {
			int ind = getDimVirtualArray().indexOf(id);
			return content.getDimensionPos(ind);
		} else {
			return getDimIndexOf(id) * getSize().x() / getDimVirtualArray().size();
		}
	}

	@Override
	public final float getRecPosOf(int id) {
		if (isFocused()) {
			int ind = getRecVirtualArray().indexOf(id);
			return content.getRecordPos(ind);
		} else {
			return getRecIndexOf(id) * getSize().y() / getRecVirtualArray().size();
		}
	}

	@Override
	public boolean needsUniformScaling() {
		return !content.isShowingHeatMap() && !content.isShowingBarPlot();
	}

	@Override
	public Vec2f getMinSize() {
		return content.getMinSize();
	}
}
