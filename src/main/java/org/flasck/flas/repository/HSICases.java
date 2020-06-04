package org.flasck.flas.repository;

import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;

public interface HSICases {

	boolean noRemainingCases();
	boolean singleton();
	FunctionIntro onlyIntro();
	boolean contains(FunctionIntro fi);
	HSICases retain(List<FunctionIntro> intros);
	void remove(HSICases forConst);
	List<String> introNames();
	HSICases alsoConsider(List<FunctionIntro> bi);

}
