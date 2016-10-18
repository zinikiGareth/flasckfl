package org.flasck.flas.commonBase;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class PlatformSpec {
	public final InputPosition location;
	public final String spec;
	public final List<Object> defns = new ArrayList<Object>();

	public PlatformSpec(InputPosition location, String text) {
		this.location = location;
		this.spec = text;
	}

}
