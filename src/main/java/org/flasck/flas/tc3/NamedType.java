package org.flasck.flas.tc3;

import java.util.Comparator;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface NamedType extends Type {
	public static Comparator<NamedType> nameComparator = new Comparator<NamedType>() {
		@Override
		public int compare(NamedType l, NamedType r) {
			return l.name().uniqueName().compareTo(r.name().uniqueName());
		}
	};
	NameOfThing name();
}
