package org.flasck.flas.patterns;

import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.HSICases;

public interface HSITree {
	HSITree consider(FunctionIntro fi);
	int width();
	HSIOptions get(int i);
	List<FunctionIntro> intros();
	boolean containsAny(HSICases intros);
	void dump(String indent);
}
