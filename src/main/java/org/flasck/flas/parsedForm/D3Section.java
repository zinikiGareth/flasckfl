package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class D3Section {
	public final String name;
	public final List<MethodMessage> actions = new ArrayList<MethodMessage>();
	public final Map<String, PropertyDefn> properties = new TreeMap<String, PropertyDefn>();

	public D3Section(String name) {
		this.name = name;
	}
}
