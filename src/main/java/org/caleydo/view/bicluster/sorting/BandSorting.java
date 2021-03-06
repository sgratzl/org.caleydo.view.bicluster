/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.bicluster.sorting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Michael Gillhofer
 *
 */
public class BandSorting implements Iterable<Integer> {

	private List<Collection<Integer>> nonEmptyBands;
	private Set<BandConflict> conflicts = new TreeSet<>();
	private Set<Integer> finalSorting = new LinkedHashSet<>();

	public BandSorting(List<Collection<Integer>> nonEmptyBands) {
		this.nonEmptyBands = nonEmptyBands;
		init();
	}

	private void init() {
		for (int i = 0; i < nonEmptyBands.size(); i++) {
			for (int j = i + 1; j < nonEmptyBands.size(); j++) {
				List<Integer> potentialConflict = new ArrayList<Integer>(nonEmptyBands.get(i));
				potentialConflict.retainAll(nonEmptyBands.get(j));
				conflicts.add(new BandConflict(nonEmptyBands.get(i), nonEmptyBands.get(j), potentialConflict));

			}
		}
		for (BandConflict bc : conflicts) {
			List<Integer> firstWithoutConflicts = new ArrayList<>(bc.getFirst());
			if (!bc.getConflict().isEmpty())
				firstWithoutConflicts.removeAll(bc.getConflict());
			finalSorting.addAll(firstWithoutConflicts);
			finalSorting.addAll(bc.getConflict());
			finalSorting.addAll(bc.getSecond());
		}
	}

	@Override
	public Iterator<Integer> iterator() {
		return finalSorting.iterator();
	}




	/**
	 * @author Michael Gillhofer
	 *
	 */
	public class BandConflict implements Comparable<BandConflict> {
		private final Collection<Integer> first;
		private final Collection<Integer> second;
		private final List<Integer> conflict;

		public BandConflict(Collection<Integer> first, Collection<Integer> second, List<Integer> conflict) {
			this.first = first;
			this.second = second;
			this.conflict = conflict;
		}
		/**
		 * @return the first, see {@link #first}
		 */
		public Collection<Integer> getFirst() {
			return first;
		}

		/**
		 * @return the second, see {@link #second}
		 */
		public Collection<Integer> getSecond() {
			return second;
		}

		/**
		 * @return the conflict, see {@link #conflict}
		 */
		public List<Integer> getConflict() {
			return conflict;
		}

		@Override
		public int compareTo(BandConflict o) {
			return new Integer(conflict.size()).compareTo(o.conflict.size());
		}

	}

}
