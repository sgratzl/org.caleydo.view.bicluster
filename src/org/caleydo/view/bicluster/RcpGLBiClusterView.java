/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.bicluster;

import org.caleydo.core.gui.SimpleEventAction;
import org.caleydo.core.view.ARcpGLViewPart;
import org.caleydo.view.bicluster.event.ShowToolBarEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * TODO: DOCUMENT ME!
 *
 * @author <INSERT_YOUR_NAME>
 */
public class RcpGLBiClusterView extends ARcpGLViewPart {

	public RcpGLBiClusterView() {
		super(SerializedBiClusterView.class);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		view = new GLBiCluster(glCanvas, serializedView);
		initializeView();

		createPartControlGL();
	}

	@Override
	public void addToolBarContent() {
		super.addToolBarContent();
		toolBarManager.add(new SimpleEventAction("Show Parameter Settings", BiClusterRenderStyle.ICON_TOOLS,
				Activator.getResourceLoader(), new ShowToolBarEvent(true)));
		toolBarManager.add(new SimpleEventAction("Show Layout Settings", BiClusterRenderStyle.ICON_LAYOUT, Activator
				.getResourceLoader(),
				new ShowToolBarEvent(false)));
	}

	@Override
	public void createDefaultSerializedView() {
		serializedView = new SerializedBiClusterView();
	}

	@Override
	public String getViewGUIID() {
		return GLBiCluster.VIEW_TYPE;
	}

}