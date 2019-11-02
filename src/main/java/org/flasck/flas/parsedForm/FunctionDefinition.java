package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

// TODO: having both nargs & type feels like duplication, but we know about nargs SOOOO much earlier
public class FunctionDefinition implements RepositoryEntry, Locatable, WithTypeSignature, StandaloneDefn, Comparable<StandaloneDefn> {
	private final FunctionName name;
	private final int nargs;
	private final List<FunctionIntro> intros = new ArrayList<>();
	private Type type;
	private HSITree hsiTree;
	private NestedVarReader nestedVars;

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
	
	@Override
	public InputPosition location() {
		if (intros.isEmpty())
			return null;
		else
			return intros.get(0).location;
	}

	public int argCount() {
		if (nestedVars != null)
			return nargs + nestedVars.size();
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

	public List<Slot> slots() {
		List<Slot> slots = new ArrayList<>();
		for (int i=0;i<argCount();i++) {
			slots.add(new ArgSlot(i, hsiTree.get(i)));
		}
		return slots;
	}

	public void nestedVars(NestedVarReader nestedVars) {
		this.nestedVars = nestedVars;
	}

	public NestedVarReader nestedVars() {
		return nestedVars;
	}

	@Override
	public int compareTo(StandaloneDefn o) {
		return name().compareTo(o.name());
	}
}
