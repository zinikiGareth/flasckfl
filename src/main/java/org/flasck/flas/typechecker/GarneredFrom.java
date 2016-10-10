package org.flasck.flas.typechecker;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.zinutils.exceptions.UtilException;

public class GarneredFrom {
	private enum Which { INPUT, EXTREF, STRUCT, TYPEARG, FNARG };
	private final Which which;
	public final InputPosition posn;
	public final ExternalRef exref;
	public final Object type;
	public final String fnName;
	int arg = -1;

	public GarneredFrom(InputPosition posn) {
		// This has to be commented out for unit tests to pass since they pass null around like crazy people
		if (posn == null)
			throw new UtilException("position cannot be null");
		this.which = Which.INPUT;
		this.posn = posn;
		this.exref = null;
		this.type = null;
		this.fnName = null;
	}

	public GarneredFrom(ExternalRef fn, Object te) {
		this.which = Which.EXTREF;
		this.posn = fn.location();
		if (posn == null)
			System.out.println("posn should not be null");
		this.exref = fn;
		this.type = te;
		this.fnName = null;
	}

	public GarneredFrom(RWStructField f) {
		this.which = Which.STRUCT;
		this.posn = f.location();
		if (posn == null)
			System.out.println("posn should not be null");
		this.exref = null;
		this.type = f.type;
		this.fnName = null;
	}

	public GarneredFrom(Type type, int i) {
		this.which = Which.TYPEARG;
		this.posn = type.location();
		if (posn == null)
			System.out.println("posn should not be null");
		this.exref = null;
		this.type = type;
		this.arg = i;
		this.fnName = null;
	}

	public GarneredFrom(String fnName, int i, InputPosition loc) {
		this.which = Which.FNARG;
		this.posn = loc;
		if (posn == null)
			System.out.println("posn should not be null");
		this.exref = null;
		this.type = null;
		this.fnName = fnName;
		this.arg = i;
	}
	
	@Override
	public String toString() {
		switch (which) {
		case INPUT:
			return (posn == null ? "unknown" : posn.toString());
		case TYPEARG:
			return "Function("+type+":"+arg+")";
		case EXTREF:
			return exref.uniqueName() + " [" + type + "]";
		case FNARG:
			return fnName + ":" + arg;
		case STRUCT:
			return type.toString();
		default:
			throw new UtilException("Unknown GF type");
		}
	}
}
