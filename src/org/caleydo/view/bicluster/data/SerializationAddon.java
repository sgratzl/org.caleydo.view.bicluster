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
package org.caleydo.view.bicluster.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.caleydo.core.data.datadomain.IDataDomain;
import org.caleydo.core.serialize.ISerializationAddon;
import org.caleydo.core.serialize.SerializationData;
import org.caleydo.core.util.logging.Logger;
import org.caleydo.view.bicluster.SerializedBiClusterView;

/**
 * addon to the project serialization / deserialization to persist data, if you want to persist view specific data, use
 * the {@link SerializedBiClusterView} object
 * 
 * @author Samuel Gratzl
 * 
 */
public class SerializationAddon implements ISerializationAddon {
	private static final String ADDON_KEY = "BiCluster";
	private final static Logger log = Logger.create(SerializationAddon.class);

	@Override
	public Collection<Class<?>> getJAXBContextClasses() {
		Collection<Class<?>> tmp = new ArrayList<>(1);
		tmp.add(BiClusterSerializationData.class);
		return tmp;
	}

	@Override
	public void serialize(Collection<? extends IDataDomain> toSave, Marshaller marshaller, String dirName) {
		BiClusterSerializationData data = new BiClusterSerializationData();
		// TODO

		try {
			marshaller.marshal(data, new File(dirName, "bicluster.xml"));
		} catch (JAXBException e) {
			log.error("can't serialize bicluster data", e);
		}
	}

	@Override
	public void deserialize(String dirName, Unmarshaller unmarshaller, SerializationData data) {
		File f = new File(dirName, "bicluster.xml");
		if (!f.exists())
			return;
		BiClusterSerializationData bicluster;
		try {
			bicluster = (BiClusterSerializationData) unmarshaller.unmarshal(new File(dirName, "bicluster.xml"));
			data.setAddonData(ADDON_KEY, bicluster);
		} catch (JAXBException e) {
			log.error("can't deserialize bicluster data", e);
		}
	}

	@Override
	public void load(SerializationData data) {
		BiClusterSerializationData desc = (BiClusterSerializationData) data.getAddonData(ADDON_KEY);
		if (desc == null)
			return;
		// TODO
	}

}