package org.flasck.flas.patterns;

import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;

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
		public final NamedType type;
		public IntroTypeVar(FunctionIntro intro, TypedPattern tp) {
			this.intro = intro;
			this.tp = tp;
			this.type = (NamedType) tp.type.defn();
		}
		public IntroTypeVar(FunctionIntro intro, NamedType type) {
			this.intro = intro;
			this.tp = null;
			this.type = type;
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
	List<FunctionIntro> getDefaultIntros(HSICases intros);
	Set<StructDefn> ctors();
	List<IntroTypeVar> typedVars(NamedType ty);
	List<IntroVarName> vars();
	List<IntroVarName> vars(HSICases intros);
	Set<NamedType> types();
	Set<Integer> numericConstants(HSICases intersect);
	Set<String> stringConstants(HSICases intersect);
	boolean hasSwitches(HSICases intros);
	int score();
	boolean isContainer();
	NamedType containerType();
	void dump(String indent);
	List<String> introNames();
	List<NamedType> unionsIncluding(StructDefn c);
}
