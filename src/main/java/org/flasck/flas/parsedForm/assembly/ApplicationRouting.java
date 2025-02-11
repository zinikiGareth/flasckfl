package org.flasck.flas.parsedForm.assembly;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.assembly.MainRoutingGroupConsumer;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ApplicationRouting extends SubRouting implements MainRoutingGroupConsumer, NamedType {
	private final InputPosition location;
	private final NameOfThing packageName;
	private final NameOfThing name;
	public boolean sawMainCard;

	public ApplicationRouting(ErrorReporter errors, InputPosition location, NameOfThing packageName, AssemblyName name) {
		super(errors, null, null, null, name);
		this.location = location;
		this.packageName = packageName;
		this.name = name;
	}

	public NameOfThing packageName() {
		return packageName;
	}

	@Override
	public NameOfThing name() {
		return name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public void provideMainCard(TypeReference main) {
		if (sawMainCard) {
			errors.message(main.location(), "duplicate assignment to main card");
			return;
		}
		sawMainCard = true;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("routing " + name);
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		throw new NotImplementedException();
	}

}
