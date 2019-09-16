package org.flasck.flas.patterns;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser;

public interface HSITree {
	void consider(FunctionIntro fi);
	int width();
	HSIOptions get(int i);
	void visit(Traverser traverser, HSIVisitor hsi);
}
