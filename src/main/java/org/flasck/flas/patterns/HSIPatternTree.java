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
	public HSITree consider(FunctionIntro fi) {
		intros.add(fi);
		return this;
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
	public List<FunctionIntro> intros() {
		return intros;
	}

	@Override
	public void visit(Traverser traverser, HSIVisitor hsi, List<Slot> slots) {
		if (intros.isEmpty())
			return; // not for generation
		else if (slots.isEmpty() && intros.size() == 1)
			handleInline(traverser, hsi, intros.get(0));
		else {
			if (slots.size() != 1)
				throw new NotImplementedException();
			HSIOptions slot = this.slots.get(0); // should select using some kind of "which-is-best" algorithm
			Slot s = slots.get(0);
			if (slot.hasSwitches()) {
				hsi.switchOn(s);
				Set<String> ctors = slot.ctors();
				for (String c : ctors) {
					hsi.withConstructor(c);
					HSITree cm = slot.getCM(c);
					if (cm.intros().size() != 1)
						throw new NotImplementedException();
					handleInline(traverser, hsi, cm.intros().get(0));
				}
			} else {
				if (!slot.vars().isEmpty())
					hsi.bind(s, slot.vars().get(0).var);
				handleInline(traverser, hsi, intros.get(0));
			}
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
