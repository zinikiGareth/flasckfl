package org.flasck.flas.hsi;

import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Repository.Visitor;

public interface HSIVisitor extends Visitor {
	void hsiArgs(List<Slot> slots);
	void switchOn(Slot slot);
	void withConstructor(String string);
	void errorNoCase();
	void startInline(FunctionIntro fi);
	void endInline(FunctionIntro fi);
	void endSwitch();
}
