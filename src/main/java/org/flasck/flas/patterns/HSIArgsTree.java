package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;

public class HSIArgsTree extends HSIPatternTree {
	private List<HSIOptions> slots = new ArrayList<>();

	public HSIArgsTree(int nargs) {
		for (int i=0;i<nargs;i++) {
			slots.add(new HSIPatternOptions());
		}
	}
	
	@Override
	public HSITree consider(FunctionIntro fi) {
		super.consider(fi);
		for (HSIOptions o : slots)
			o.includes(fi);
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
}
