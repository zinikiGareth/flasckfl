package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;

public class ContractMethodDecl implements Locatable, RepositoryEntry, Comparable<ContractMethodDecl> {
	public final InputPosition rkw;
	public final InputPosition dkw;
	private final InputPosition pos;
	public final boolean required;
	public final FunctionName name;
	public final List<TypedPattern> args;
	private Type type;
	public final TypedPattern handler;

	public ContractMethodDecl(InputPosition rkw, InputPosition dkw, InputPosition pos, boolean required, FunctionName name, List<TypedPattern> args, TypedPattern handler) {
		this.rkw = rkw;
		this.dkw = dkw;
		this.pos = pos;
		this.required = required;
		this.name = name;
		this.args = args;
		this.handler = handler;
	}

	@Override
	public int compareTo(ContractMethodDecl o) {
		return name.compareTo(o.name);
	}

	public void bindType() {
		if (this.type != null)
			throw new RuntimeException("Type already bound to " + this.type + " cannot rebind");
		List<Type> types = new ArrayList<>();
		for (Pattern p : this.args) {
			types.add(((TypedPattern)p).type());
		}
		if (handler != null)
			types.add(handler.type());
		else
			types.add(LoadBuiltins.idempotentHandler);
		this.type = new Apply(types, LoadBuiltins.send);
	}

	public Type type() {
		return this.type;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(required?"required":"optional");
		sb.append(" ");
		sb.append(name.name);
		for (Object o : args) {
			sb.append(" ");
			sb.append(o.toString());
		}
		return sb.toString();
	}

	@Override
	public InputPosition location() {
		return pos;
	}

	@Override
	public NameOfThing name() {
		return name;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
}
