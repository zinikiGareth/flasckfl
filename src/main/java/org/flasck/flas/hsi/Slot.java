package org.flasck.flas.hsi;

import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIOptions;

public interface Slot {
	HSIOptions getOptions();
	int score();
	String id();
	List<FunctionIntro> lessSpecific();
}
