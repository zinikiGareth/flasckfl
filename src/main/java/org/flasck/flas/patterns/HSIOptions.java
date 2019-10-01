package org.flasck.flas.patterns;

import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.Type;

public interface HSIOptions {
	public class IntroVarName {
		public final FunctionIntro intro;
		public final VarName var;
		public IntroVarName(FunctionIntro intro, VarName var) {
			this.intro = intro;
			this.var = var;
		}
	}
	
	void addVar(VarName varName, FunctionIntro fi);
	void addTyped(TypeReference tr, VarName varName, FunctionIntro fi);
	HSICtorTree requireCM(String ctor);
	HSITree getCM(String constructor);
	void includes(FunctionIntro current);
	List<FunctionIntro> getIntrosForType(String ty);
	List<FunctionIntro> getDefaultIntros(List<FunctionIntro> intros);
	Type minimalType(CurrentTCState state, RepositoryReader repository);
	Set<String> ctors();
	List<IntroVarName> vars(List<FunctionIntro> intros);
	Set<String> types(List<FunctionIntro> intros);
	boolean hasSwitches(List<FunctionIntro> intros);
	int score();
}
