package org.flasck.flas.tc3;

import java.util.Collection;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.VarPattern;

public interface CurrentTCState extends Consolidator {
	UnifiableType createUT(InputPosition pos, String motive);
	UnifiableType requireVarConstraints(InputPosition pos, String var);
	UnifiableType hasVar(String var);
	PolyType nextPoly(InputPosition pos);
	void bindVarToUT(String name, UnifiableType ty);
	@Deprecated
	void resolveAll(ErrorReporter errors, boolean hard);
	void bindVarPatternToUT(VarPattern vp, UnifiableType ty);
	void bindVarPatternTypes(ErrorReporter errors);
	void debugInfo(String when);
	boolean hasGroup();
	void bindIntroducedVarTypes(ErrorReporter errors);
	void bindIntroducedVarToUT(IntroduceVar v, UnifiableType ut);
	void groupDone(ErrorReporter errors, Map<TypeBinder, PosType> memberTypes);
}
