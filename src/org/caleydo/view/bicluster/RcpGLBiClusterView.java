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
package org.caleydo.view.bicluster;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.caleydo.core.view.ARcpGLViewPart;
import org.caleydo.view.bicluster.view.Toolbar;
import org.eclipse.swt.widgets.Composite;

/**
 * TODO: DOCUMENT ME!
 *
 * @author <INSERT_YOUR_NAME>
 */
public class RcpGLBiClusterView extends ARcpGLViewPart {


	/**
	 * Constructor.
	 */
	public RcpGLBiClusterView() {
		super();

		try {
			viewContext = JAXBContext.newInstance(SerializedBiClusterView.class);
		} catch (JAXBException ex) {
			throw new RuntimeException("Could not create JAXBContext", ex);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		view = new GLBiCluster(glCanvas, parentComposite, serializedView.getViewFrustum());
		initializeView();
		createPartControlGL();
	}


	@Override
	public void addToolBarContent() {
		toolBarManager.add(new Toolbar());
		toolBarManager.update(true);
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