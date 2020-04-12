package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parser.ut.IntroduceNamer;
import org.flasck.flas.repository.RepositoryEntry;

public class IntroduceVar implements Expr, RepositoryEntry {
	public final InputPosition location;
	public final String var;
	private final VarName name;

	public IntroduceVar(InputPosition location, IntroduceNamer namer, String var) {
		this.location = location;
		this.var = var;
		this.name = namer.introductionName(location, var);
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "_" + var;
	}

	public NameOfThing name() {
		return name;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("Introduce[" + name + "]");
	}
}
