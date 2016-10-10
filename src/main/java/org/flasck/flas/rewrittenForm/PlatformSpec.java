package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class PlatformSpec implements Serializable {
	public final InputPosition location;
	public final String spec;
	public final List<Object> defns = new ArrayList<Object>();

	public PlatformSpec(InputPosition location, String text) {
		this.location = location;
		this.spec = text;
	}

}
