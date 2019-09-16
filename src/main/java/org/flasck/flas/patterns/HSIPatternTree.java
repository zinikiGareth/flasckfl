package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser;
import org.zinutils.exceptions.NotImplementedException;

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
	public void visit(Traverser traverser, HSIVisitor hsi, List<Slot> slots) {
		if (intros.isEmpty())
			return; // not for generation
		else if (slots.isEmpty() && intros.size() == 1)
			handleInline(traverser, hsi, intros.get(0));
		else {
			if (slots.size() != 1 || intros.size() != 1)
				throw new NotImplementedException();
			HSIOptions slot = this.slots.get(0); // should select using some kind of "which-is-best" algorithm
			Slot s = slots.get(0);
			if (slot.hasSwitches()) {
				hsi.switchOn(s);
				Set<String> ctors = slot.ctors();
				for (String c : ctors) {
					hsi.withConstructor(c);
					handleInline(traverser, hsi, intros.get(0));
				}
			} else
				handleInline(traverser, hsi, intros.get(0));
			if (slot.hasSwitches()) {
				hsi.errorNoCase();
				hsi.endSwitch();
			}
		}
	}

	private void handleInline(Traverser traverser, HSIVisitor hsi, FunctionIntro i) {
		hsi.startInline(i);
		for (FunctionCaseDefn c : i.cases())
			traverser.visitCase(c);
		hsi.endInline(i);
	}
	
	
}
