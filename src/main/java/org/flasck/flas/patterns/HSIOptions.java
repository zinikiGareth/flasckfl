package org.flasck.flas.patterns;

import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.Type;

public interface HSIOptions {
	void addVar(VarName varName);
	void addTyped(TypeReference tr, VarName varName);
	void addCM(String ctor, HSITree nested);
	HSITree getCM(String constructor);
	Type minimalType(RepositoryReader repository);
	Set<String> ctors();
	List<String> vars();
	List<Type> types();
	boolean hasSwitches();
}
