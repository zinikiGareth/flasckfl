package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class D3Section implements Locatable, Comparable<D3Section> {
	public final InputPosition location;
	public final String name;
	public final List<MethodMessage> actions = new ArrayList<MethodMessage>();
	public final List<PropertyDefn> properties = new ArrayList<PropertyDefn>();

	public D3Section(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public int compareTo(D3Section o) {
		return name.compareTo(o.name);
	}
}
