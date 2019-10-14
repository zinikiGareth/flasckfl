package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;

public interface HSIOptions {
	public class IntroVarName {
		public final FunctionIntro intro;
		public final VarPattern vp;
		public final VarName var;
		public IntroVarName(FunctionIntro intro, VarPattern vp) {
			this.intro = intro;
			this.vp = vp;
			this.var = vp.name();
		}
		public IntroVarName(FunctionIntro intro, VarName vn) {
			this.intro = intro;
			this.vp = null;
			this.var = vn;
		}
	}
	
	public class IntroTypeVar {
		public final FunctionIntro intro;
		public final TypedPattern tp;
		public IntroTypeVar(FunctionIntro intro, TypedPattern tp) {
			this.intro = intro;
			this.tp = tp;
		}
	}
	
	void addVar(VarPattern vp, FunctionIntro fi);
	void addVarWithType(TypeReference tr, VarName varName, FunctionIntro fi);
	void addTyped(TypedPattern tr, FunctionIntro fi);
	HSICtorTree requireCM(StructDefn ctor);
	void addConstant(Primitive type, String value, FunctionIntro fi);
	HSITree getCM(StructDefn constructor);
	void includes(FunctionIntro current);
	List<FunctionIntro> getIntrosForType(NamedType ty);
	List<FunctionIntro> getDefaultIntros(List<FunctionIntro> intros);
	Type minimalType(CurrentTCState state, RepositoryReader repository);
	Set<StructDefn> ctors();
	List<IntroTypeVar> typedVars(NamedType ty);
	List<IntroVarName> vars();
	List<IntroVarName> vars(List<FunctionIntro> intros);
	Set<NamedType> types();
	Set<Integer> numericConstants(ArrayList<FunctionIntro> intersect);
	Set<String> stringConstants(ArrayList<FunctionIntro> intersect);
	boolean hasSwitches(List<FunctionIntro> intros);
	int score();
}
