package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.Comparator;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parser.ut.IntroduceNamer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;

public class IntroduceVar implements Expr, RepositoryEntry {
	public static Comparator<IntroduceVar> comparator = new Comparator<>() {
		@Override
		public int compare(IntroduceVar o1, IntroduceVar o2) {
			return o1.name().compareTo(o2.name());
		}
	};
	public final InputPosition location;
	public final String var;
	private final VarName name;
	private Type introducedAs;

	public IntroduceVar(InputPosition location, IntroduceNamer namer, String var, boolean pkgScope) {
		this.location = location;
		this.var = var;
		this.name = namer.introductionName(location, var, pkgScope);
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void bindType(Type type) {
		this.introducedAs = type;
	}
	
	public Type introducedAs() {
		if (introducedAs instanceof UnifiableType)
			return ((UnifiableType)introducedAs).resolvedTo();
		return introducedAs;
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
