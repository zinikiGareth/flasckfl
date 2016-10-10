package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;

public class RWD3Section {
	public final InputPosition location;
	public final String name;
	public final List<RWMethodMessage> actions = new ArrayList<RWMethodMessage>();
	public final Map<String, RWPropertyDefn> properties = new TreeMap<String, RWPropertyDefn>();

	public RWD3Section(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}
}
