package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;

public class HSIPatternTree implements HSITree {
	private List<HSIOptions> slots = new ArrayList<>();

	public HSIPatternTree(int nargs) {
		for (int i=0;i<nargs;i++) {
			slots.add(new HSIPatternOptions());
		}
	}
	
	@Override
	public int width() {
		return slots.size();
	}
}
