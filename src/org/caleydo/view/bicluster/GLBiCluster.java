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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.media.opengl.GLAutoDrawable;

import org.caleydo.core.data.collection.table.Table;
import org.caleydo.core.data.datadomain.ATableBasedDataDomain;
import org.caleydo.core.data.datadomain.DataSupportDefinitions;
import org.caleydo.core.data.datadomain.IDataDomain;
import org.caleydo.core.data.datadomain.IDataSupportDefinition;
import org.caleydo.core.data.perspective.table.TablePerspective;
import org.caleydo.core.data.perspective.variable.Perspective;
import org.caleydo.core.data.perspective.variable.PerspectiveInitializationData;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.event.EventPublisher;
import org.caleydo.core.event.view.TablePerspectivesChangedEvent;
import org.caleydo.core.id.IDCategory;
import org.caleydo.core.id.IDType;
import org.caleydo.core.serialize.ASerializedView;
import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.view.IMultiTablePerspectiveBasedView;
import org.caleydo.core.view.listener.AddTablePerspectivesEvent;
import org.caleydo.core.view.listener.RemoveTablePerspectiveEvent;
import org.caleydo.core.view.opengl.canvas.AGLView;
import org.caleydo.core.view.opengl.canvas.ATableBasedView;
import org.caleydo.core.view.opengl.canvas.IGLCanvas;
import org.caleydo.core.view.opengl.layout2.AGLElementView;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.view.bicluster.concurrent.ScanProbabilityMatrix;
import org.caleydo.view.bicluster.elem.ClusterElement;
import org.caleydo.view.bicluster.elem.GLRootElement;
import org.caleydo.view.bicluster.event.ToolbarEvent;

/**
 * <p>
 * Sample GL2 view.
 * </p>
 * <p>
 * This Template is derived from {@link ATableBasedView}, but if the view does not use a table, changing that to
 * {@link AGLView} is necessary.
 * </p>
 *
 * @author Michael Gillhofer
 * @author Marc Streit
 */

// TODO Fix Band start and End positions.
// TODO Framerate drop when bands are displayed
// TODO Fix Drag and Drop on specific clusters
// TODO Irrational layouting in some cases

public class GLBiCluster extends AGLElementView implements IMultiTablePerspectiveBasedView {
	public static final String VIEW_TYPE = "org.caleydo.view.bicluster";
	public static final String VIEW_NAME = "BiCluster Visualization";

	private TablePerspective x, l, z;

	private ExecutorService executorService = Executors.newFixedThreadPool(4);

	private float sampleThreshold = 4.5f;
	private float geneThreshold = 0.08f;

	private final List<TablePerspective> perspectives = new ArrayList<>();

	GLRootElement rootElement;
	private boolean setXElements = false;

	public GLBiCluster(IGLCanvas glCanvas) {
		super(glCanvas, VIEW_TYPE, VIEW_NAME);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		super.init(drawable);

		if (this.perspectives.size() >= 3) {
			findXLZ();
			rootElement.setData(initTablePerspectives());
			createBiClusterPerspectives(x, l, z);
			createBiClusterPerspectives(x, l, z);
			rootElement.createBands();
			rootElement.setClusterSizes();
		}
	}

	private List<TablePerspective> initTablePerspectives() {

		List<TablePerspective> persp = new ArrayList<>();
		if (x.getDataDomain().getAllTablePerspectives().size() == 1) {
			int bcCountData = l.getDataDomain().getTable().getColumnIDList().size(); // Nr of BCs in L & Z
			ATableBasedDataDomain xDataDomain = x.getDataDomain();
			Table xtable = xDataDomain.getTable();
			IDType xdimtype = xDataDomain.getDimensionIDType();
			IDType xrectype = xDataDomain.getRecordIDType();
			for (Integer i = 0; i < bcCountData; i++) {
				Perspective dim = new Perspective(xDataDomain, xdimtype);
				Perspective rec = new Perspective(xDataDomain, xrectype);
				PerspectiveInitializationData dim_init = new PerspectiveInitializationData();
				PerspectiveInitializationData rec_init = new PerspectiveInitializationData();
				dim_init.setData(new ArrayList<Integer>());
				rec_init.setData(new ArrayList<Integer>());
				dim.init(dim_init);
				rec.init(rec_init);
				xtable.registerDimensionPerspective(dim, false);
				xtable.registerRecordPerspective(rec, false);
				String dimKey = dim.getPerspectiveID();
				String recKey = rec.getPerspectiveID();
				TablePerspective custom = xDataDomain.getTablePerspective(recKey, dimKey, false);
				custom.setLabel(l.getDataDomain().getDimensionLabel(i));
				persp.add(custom);
			}
		} else {
			int i = 0;
			int bcCount = l.getDataDomain().getTable().getColumnIDList().size();
			for (i = 0; i < bcCount; i++) {
				biClusterLabels.add(l.getDataDomain().getDimensionLabel(i));
			}
			System.out.println(biClusterLabels);
			for (TablePerspective p : x.getDataDomain().getAllTablePerspectives()) {
				System.out.println(p.getLabel());
				if (biClusterLabels.contains(p.getLabel())) {
					persp.add(p);
				}
			}
		}
		return persp;
	}

	private List<String> biClusterLabels = new ArrayList<>();

	protected void createBiClusterPerspectives(TablePerspective x, TablePerspective l, TablePerspective z) {
		System.out.println("Erstelle Cluster mit SampleTH: " + sampleThreshold);
		System.out.println("                     RecordTH: " + geneThreshold);

		Table L = l.getDataDomain().getTable();
		Table Z = z.getDataDomain().getTable();
		int bcCountData = L.getColumnIDList().size(); // Nr of BCs in L & Z

		// Tables indices for Genes and Tables of a specific BiCluster.
		Map<Integer, Future<List<Integer>>> bcDimScanFut = new HashMap<>();
		Map<Integer, Future<List<Integer>>> bcRecScanFut = new HashMap<>();
		for (int bcNr = 0; bcNr < bcCountData; bcNr++) {
			Future<List<Integer>> recList = executorService.submit(new ScanProbabilityMatrix(geneThreshold, L, bcNr));
			Future<List<Integer>> dimList = executorService.submit(new ScanProbabilityMatrix(sampleThreshold, Z, bcNr));

			bcRecScanFut.put(bcNr, recList);
			bcDimScanFut.put(bcNr, dimList);
		}

		// actually alter the cluster perspectives
		for (Integer i : bcDimScanFut.keySet()) {
			try {
				List<Integer> dimIndices = bcDimScanFut.get(i).get();
				List<Integer> recIndices = bcRecScanFut.get(i).get();
				ClusterElement el = (ClusterElement) rootElement.getClusters().get(i);

				biClusterLabels.add(l.getDataDomain().getDimensionLabel(i));
				el.setIndices(dimIndices, recIndices, setXElements, l.getDataDomain().getDimensionLabel(i));
				// el.setPerspectiveLabel(dimensionName, recordName)
			} catch (InterruptedException | ExecutionException | NullPointerException e) {
				e.printStackTrace();
			}
		}
		rootElement.createBands();
		rootElement.resetDamping();
	}

	/**
	 * determines which of the given {@link TablePerspective} is L and Z, given the known X table
	 *
	 * @param a
	 * @param b
	 */
	private Pair<TablePerspective, TablePerspective> findLZ(TablePerspective x, TablePerspective a, TablePerspective b) {
		// row: gene, row: gene
		if (a.getDataDomain().getRecordIDCategory().equals(x.getDataDomain().getRecordIDCategory())) {
			return Pair.make(a, b);
		} else {
			return Pair.make(b, a);
		}
	}

	/**
	 * infers the X,L,Z tables from the dimension and record ID categories, as L and Z has the same Dimension ID
	 * Category, the remaining one will be X
	 */
	private void findXLZ() {
		TablePerspective a = perspectives.get(0);
		TablePerspective b = perspectives.get(1);
		TablePerspective c = perspectives.get(2);
		IDCategory a_d = a.getDataDomain().getDimensionIDCategory();
		IDCategory b_d = b.getDataDomain().getDimensionIDCategory();
		IDCategory c_d = c.getDataDomain().getDimensionIDCategory();
		Pair<TablePerspective, TablePerspective> lz;
		if (a_d.equals(b_d)) {
			x = c;
			lz = findLZ(x, a, b);
		} else if (a_d.equals(c_d)) {
			x = b;
			lz = findLZ(x, a, c);
		} else {
			x = a;
			lz = findLZ(x, b, c);
		}
		l = lz.getFirst();
		z = lz.getSecond();
	}

	@Override
	public boolean isDataView() {
		return true;
	}

	@Override
	public ASerializedView getSerializableRepresentation() {
		SerializedBiClusterView serializedForm = new SerializedBiClusterView(this);
		serializedForm.setViewID(this.getID());
		return serializedForm;
	}

	@Override
	public String toString() {
		return "BiCluster";
	}

	@ListenTo
	private void onAddTablePerspective(AddTablePerspectivesEvent event) {
		List<TablePerspective> validTablePerspectives = new ArrayList<TablePerspective>(event.getTablePerspectives()
				.size());
		{// filter invalid
			final IDataSupportDefinition dsd = getDataSupportDefinition();
			for (TablePerspective tablePerspective : event.getTablePerspectives()) {
				if (dsd.apply(tablePerspective.getDataDomain())) {
					validTablePerspectives.add(tablePerspective);
				}
			}
		}

		if (validTablePerspectives.isEmpty()) {
			// Make clear for (e.g. for DVI) that no perspectives have been added.
			fireTablePerspectiveChanged();
		} else {
			addTablePerspectives(validTablePerspectives);
		}
	}

	@ListenTo(sendToMe = true)
	private void onRemoveTablePerspective(RemoveTablePerspectiveEvent event) {
		removeTablePerspective(event.getTablePerspective());
	}

	@Override
	public IDataSupportDefinition getDataSupportDefinition() {
		return DataSupportDefinitions.tableBased;
	}

	@Override
	public void addTablePerspective(TablePerspective newTablePerspective) {
		if (this.perspectives.contains(newTablePerspective))
			return;
		this.perspectives.add(newTablePerspective);
		if (getRoot() != null && perspectives.size() == 3) {
			findXLZ();
			rootElement.setData(initTablePerspectives());
			createBiClusterPerspectives(x, l, z);
		}
		fireTablePerspectiveChanged();
	}

	@Override
	public void addTablePerspectives(List<TablePerspective> newTablePerspectives) {
		this.perspectives.addAll(newTablePerspectives);
		if (getRoot() != null && perspectives.size() == 3) {
			findXLZ();
			rootElement.setData(initTablePerspectives());
			createBiClusterPerspectives(x, l, z);
			createBiClusterPerspectives(x, l, z);
			rootElement.createBands();
			rootElement.setClusterSizes();
		}
		fireTablePerspectiveChanged();
	}

	@Override
	public List<TablePerspective> getTablePerspectives() {
		return perspectives;
	}

	@Override
	public void removeTablePerspective(TablePerspective tablePerspective) {
		for (Iterator<TablePerspective> it = perspectives.iterator(); it.hasNext();) {
			if (it.next() == tablePerspective)
				it.remove();
		}
		if (rootElement != null && this.perspectives.size() < 3) {
			rootElement.setData(null);
		}
		fireTablePerspectiveChanged();
	}

	@Override
	public Set<IDataDomain> getDataDomains() {
		Set<IDataDomain> dataDomains = new HashSet<IDataDomain>();
		for (TablePerspective tablePerspective : perspectives) {
			dataDomains.add(tablePerspective.getDataDomain());
		}
		return dataDomains;
	}

	@Override
	protected GLElement createRoot() {
		rootElement = new GLRootElement();
		return rootElement;
	}

	private void fireTablePerspectiveChanged() {
		EventPublisher.trigger(new TablePerspectivesChangedEvent(this).from(this));
	}

	@ListenTo
	private void handleUpdate(ToolbarEvent event) {
		geneThreshold = event.getGeneThreshold();
		sampleThreshold = event.getSampleThreshold();
		if ((x != null && l != null && z != null) || setXElements != event.isFixedClusterCount()) {
			setXElements = event.isFixedClusterCount();
			createBiClusterPerspectives(x, l, z);
		}
		rootElement.setClusterSizes();

	}
}
