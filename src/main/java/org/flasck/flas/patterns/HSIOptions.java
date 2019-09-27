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
	void addVar(VarName varName, FunctionIntro fi);
	void addTyped(TypeReference tr, VarName varName, FunctionIntro fi);
	HSITree requireCM(String ctor, int nargs);
	HSITree getCM(String constructor);
	List<FunctionIntro> getIntrosForType(String ty);
	Type minimalType(CurrentTCState state, RepositoryReader repository);
	Set<String> ctors();
	List<VarName> vars(List<FunctionIntro> intros);
	Set<String> types(List<FunctionIntro> intros);
	boolean hasSwitches(List<FunctionIntro> intros);
	int score();
}
