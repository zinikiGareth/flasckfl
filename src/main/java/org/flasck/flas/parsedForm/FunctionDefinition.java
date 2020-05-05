package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.FunctionHSICases;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

// TODO: having both nargs & type feels like duplication, but we know about nargs SOOOO much earlier
public class FunctionDefinition implements RepositoryEntry, Locatable, WithTypeSignature, StandaloneDefn, Comparable<StandaloneDefn>, TypeBinder {
	private final FunctionName name;
	private final int nargs;
	private final List<FunctionIntro> intros = new ArrayList<>();
	private Type type;
	private HSITree hsiTree;
	private NestedVarReader nestedVars;
	private boolean hasState;

	public FunctionDefinition(FunctionName name, int nargs, boolean hasState) {
		this.name = name;
		this.nargs = nargs;
		this.hasState = hasState;
	}
	
	public void intro(FunctionIntro next) {
		this.intros.add(next);
		attachMeToPatternVars(next);
	}

	public FunctionName name() {
		return name;
	}
	
	@Override
	public boolean isMyName(NameOfThing other) {
		if (other == this.name)
			return true;
		for (FunctionIntro fi : intros)
			if (other == fi.name())
				return true;
		return false;
	}

	@Override
	public InputPosition location() {
		if (intros.isEmpty())
			return name.location;
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
	public HSICases hsiCases() {
		return new FunctionHSICases(intros);
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(this.toString());
	}

	@Override
	public String toString() {
		return "FunctionDefinition[" + name.uniqueName() + "/" + nargs + "{" + intros.size() + "}]";
	}

	@Override
	public void bindType(Type ty) {
		this.type = ty;
	}
	
	@Override
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
		for (int i=0;i<hsiTree.width();i++) {
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

	private void attachMeToPatternVars(FunctionIntro fi) {
		for (Pattern p : fi.args) {
			p.isDefinedBy(this);
		}
	}

	public boolean hasState() {
		return hasState;
	}
}
