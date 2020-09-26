package org.flasck.flas.tc3;

import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.VarPattern;

public interface CurrentTCState extends Consolidator {
	UnifiableType createUT(InputPosition pos, String motive);
	UnifiableType createUT(InputPosition pos, String motive, boolean unionNeedsAll);
	UnifiableType requireVarConstraints(InputPosition pos, String fxCxt, String var);
	void recordMember(FunctionName name, List<Type> ats);
	Type getMember(FunctionName name);
	UnifiableType hasVar(String fnCxt, String var);
	PolyType nextPoly(InputPosition pos);
	void bindVarToUT(String fnCxt, String name, UnifiableType ty);
	void bindVarPatternToUT(VarPattern vp, UnifiableType ty);
	void bindVarPatternTypes(ErrorReporter errors);
	void debugInfo(String when);
	boolean hasGroup();
	void bindIntroducedVarTypes(ErrorReporter errors);
	void bindIntroducedVarToUT(IntroduceVar v, UnifiableType ut);
	void groupDone(ErrorReporter errors, Map<TypeBinder, PosType> memberTypes, Map<TypeBinder, PosType> resultTypes);
	void rememberPoly(PolyType pt, UnifiableType pv);
	boolean hasPoly(PolyType pt);
	Type getPoly(PolyType pt);
	void recordPolys(Type ofType);
}
