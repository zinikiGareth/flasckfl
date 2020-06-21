package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.UnboundTypeException;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.FunctionHSICases;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

// TODO: having both nargs & type feels like duplication, but we know about nargs SOOOO much earlier
public class FunctionDefinition implements RepositoryEntry, Locatable, WithTypeSignature, LogicHolder, Comparable<LogicHolder>, TypeBinder, AccessRestrictions {
	private final FunctionName name;
	private final int nargs;
	private final StateHolder holder;
	private final List<FunctionIntro> intros = new ArrayList<>();
	private Type type;
	private HSITree hsiTree;
	private NestedVarReader nestedVars;
	private boolean reportHolder;
	private AccessRestrictions restricted;
	private boolean isObjAccessor;
	public boolean generate = true;

	public FunctionDefinition(FunctionName name, int nargs, StateHolder holder) {
		this.name = name;
		this.nargs = nargs;
		this.holder = holder;
	}
	
	public void dontGenerate() {
		this.generate = false;
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
		int ret = nargs;
		if (reportHolder && holder != null)
			ret++;
		if (nestedVars != null)
			ret += nestedVars.size();
		return ret;
	}

	public int argCountWithoutHolder() {
		int ret = nargs;
		if (nestedVars != null)
			ret += nestedVars.size();
		return ret;
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
		return name.uniqueName() + "(" + (holder != null?"+":"") + nargs + ")";
	}

	@Override
	public void bindType(Type ty) {
		this.type = ty;
	}

	public boolean hasType() {
		return this.type != null;
	}
	
	@Override
	public Type type() {
		if (type == null)
			throw new UnboundTypeException(this);
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
	public int compareTo(LogicHolder o) {
		return name().compareTo(o.name());
	}

	private void attachMeToPatternVars(FunctionIntro fi) {
		for (Pattern p : fi.args) {
			p.isDefinedBy(this);
		}
	}

	public boolean hasState() {
		return holder != null;
	}

	public StateHolder state() {
		return holder;
	}

	public void reportHolderInArgCount() {
		reportHolder = true;
	}

	public void restrict(AccessRestrictions r) {
		this.restricted = r;
	}
	
	@Override
	public void check(ErrorReporter errors, InputPosition pos, NameOfThing inContext) {
		if (restricted != null)
			restricted.check(errors, pos, inContext);
	}

	public boolean isObjAccessor() {
		return isObjAccessor;
	}
	
	public void isObjAccessor(boolean b) {
		this.isObjAccessor = b;
	}
}
