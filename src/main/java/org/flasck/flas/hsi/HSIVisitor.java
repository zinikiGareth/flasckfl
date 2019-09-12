package org.flasck.flas.hsi;

import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Repository.Visitor;

public interface HSIVisitor extends Visitor {
	void hsiArgs(Slot with);
	void startInline(FunctionIntro fi);
	void endInline(FunctionIntro fi);
}
