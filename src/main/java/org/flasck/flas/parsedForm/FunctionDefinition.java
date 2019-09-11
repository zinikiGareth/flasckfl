package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

// TODO: having both nargs & type feels like duplication, but we know about nargs SOOOO much earlier
public class FunctionDefinition implements RepositoryEntry, WithTypeSignature {
	private final FunctionName name;
	private final int nargs;
	private final List<FunctionIntro> intros = new ArrayList<>();
	private Type type;
	private HSITree hsiTree;

	public FunctionDefinition(FunctionName name, int nargs) {
		this.name = name;
		this.nargs = nargs;
	}
	
	public void intro(FunctionIntro next) {
		this.intros.add(next);
	}

	public FunctionName name() {
		return name;
	}
	
	public int argCount() {
		return nargs;
	}

	@Override
	public String signature() {
		return type.signature();
	}

	public List<FunctionIntro> intros() {
		return intros;
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(this.toString());
	}

	@Override
	public String toString() {
		return "FunctionDefinition[" + name.uniqueName() + "/" + nargs + "{" + intros.size() + "}]";
	}

	public void bindType(Type ty) {
		this.type = ty;
	}
	
	public Type type() {
		return type;
	}
	
	public void bindHsi(HSITree hsiTree) {
		this.hsiTree = hsiTree;
	}

	public HSITree hsiTree() {
		return hsiTree;
	}
}
