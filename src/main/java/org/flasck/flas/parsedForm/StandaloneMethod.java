package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class StandaloneMethod implements RepositoryEntry, LogicHolder, TypeBinder, WithTypeSignature, Locatable {
	public final ObjectMethod om;

	public StandaloneMethod(ObjectMethod om) {
		this.om = om;
		for (Pattern p : om.args())
			p.isDefinedBy(this);
	}

	public FunctionName name() {
		return om.name();
	}

	@Override
	public boolean isMyName(NameOfThing other) {
		return om.isMyName(other);
	}

	@Override
	public InputPosition location() {
		return om.location();
	}

	@Override
	public boolean isObjAccessor() {
		return false;
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	public int argCount() {
		return om.argCount();
	}
	
	public void bindType(Type ty) {
		om.bindType(ty);
	}
	
	public boolean hasType() {
		return om.hasType();
	}
	
	public Type type() {
		return om.type();
	}

	public void bindHsi(HSITree hsiTree) {
		om.bindHsi(hsiTree);
	}

	public HSITree hsiTree() {
		return om.hsiTree();
	}

	@Override
	public HSICases hsiCases() {
		throw new NotImplementedException();
	}
	
	public List<Slot> slots() {
		return om.slots();
	}

	public void nestedVars(NestedVarReader nestedVars) {
		om.nestedVars(nestedVars);
	}

	public NestedVarReader nestedVars() {
		return om.nestedVars();
	}

	public void conversion(List<FunctionIntro> convertedIntros) {
		om.conversion(convertedIntros);
	}

	public List<FunctionIntro> converted() {
		return om.converted();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	@Override
	public int compareTo(LogicHolder o) {
		return name().compareTo(o.name());
	}

	@Override
	public String toString() {
		return "StandaloneMethod[" + om + "]";
	}

	public void reportHolderInArgCount() {
		om.reportHolderInArgCount();
	}

	@Override
	public boolean hasState() {
		return om.hasState();
	}
	
	@Override
	public StateHolder state() {
		return om.state();
	}
}
