package org.flasck.flas.flim;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.types.Type;
import org.zinutils.utils.Justification;
import org.zinutils.utils.StringComparator;

public class ImportedContract implements Comparable<ImportedContract> {
	private transient final StringComparator comp = new StringComparator();
	public final NameOfThing name;
	public final Map<String, Type> fns = new TreeMap<String, Type>(comp);

	public ImportedContract(NameOfThing name) {
		this.name = name;
	}
	
	public void add(String fn, Type type) {
		fns.put(fn, type);
	}
	
	public void dump(int ind) {
		for (Entry<String, Type> f : fns.entrySet()) {
			System.out.print(Justification.LEFT.format("", ind));
			System.out.print(Justification.PADRIGHT.format(f.getKey(), 16-ind));
			System.out.println(" :: " + f.getValue());
		}
	}

	@Override
	public int compareTo(ImportedContract o) {
		return this.name.compareTo(o.name);
	}
}
