package org.flasck.flas.patterns;

import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;

public interface HSITree {
	HSITree consider(FunctionIntro fi);
	int width();
	HSIOptions get(int i);
	List<FunctionIntro> intros();
	boolean containsAny(List<FunctionIntro> intros);
}
