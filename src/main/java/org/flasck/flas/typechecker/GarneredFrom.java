package org.flasck.flas.typechecker;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.StructField;

public class GarneredFrom {
	public final InputPosition posn;
	public final ExternalRef exref;
	public final Object type;
	int arg = -1;

	public GarneredFrom(InputPosition posn) {
		this.posn = posn;
		this.exref = null;
		this.type = null;
	}

	public GarneredFrom(ExternalRef fn, Object te) {
		this.posn = null;
		this.exref = fn;
		this.type = te;
	}

	public GarneredFrom(StructField f) {
		this.posn = null;
		this.exref = null;
		this.type = f.type;
	}

	public GarneredFrom(Type type, int i) {
		this.posn = null;
		this.exref = null;
		this.type = type;
		this.arg = i;
	}
}
