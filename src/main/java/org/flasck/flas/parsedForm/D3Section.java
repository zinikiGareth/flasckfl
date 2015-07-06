package org.flasck.flas.parsedForm;

import java.util.List;
import java.util.ArrayList;

public class D3Section {
	public final String name;
	public final List<MethodMessage> actions = new ArrayList<MethodMessage>();

	public D3Section(String name) {
		this.name = name;
	}
}
