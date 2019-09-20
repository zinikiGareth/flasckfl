package org.flasck.flas.patterns;

import java.util.List;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser;

public interface HSITree {
	HSITree consider(FunctionIntro fi);
	int width();
	HSIOptions get(int i);
	void visit(Traverser traverser, HSIVisitor hsi, List<Slot> slots);
	List<FunctionIntro> intros();
}
