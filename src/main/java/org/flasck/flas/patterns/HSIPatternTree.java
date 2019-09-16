package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser;

public class HSIPatternTree implements HSITree {
	private List<HSIOptions> slots = new ArrayList<>();
	private List<FunctionIntro> intros = new ArrayList<>();

	public HSIPatternTree(int nargs) {
		for (int i=0;i<nargs;i++) {
			slots.add(new HSIPatternOptions());
		}
	}
	
	@Override
	public void consider(FunctionIntro fi) {
		intros.add(fi);
	}

	@Override
	public int width() {
		return slots.size();
	}

	@Override
	public HSIOptions get(int i) {
		return slots.get(i);
	}

	@Override
	public void visit(Traverser traverser, HSIVisitor hsi) {
		// TODO: this is a hack !!!
		for (FunctionIntro i : intros) {
			hsi.startInline(i);
			for (FunctionCaseDefn c : i.cases())
				traverser.visitCase(c);
			hsi.endInline(i);
		}
	}
	
	
}
