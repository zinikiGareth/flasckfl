package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;

public class D3Section {
	public final InputPosition location;
	public final String name;
	public final List<MethodMessage> actions = new ArrayList<MethodMessage>();
	public final Map<String, PropertyDefn> properties = new TreeMap<String, PropertyDefn>();

	public D3Section(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}
}
