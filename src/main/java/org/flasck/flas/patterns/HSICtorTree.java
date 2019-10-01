package org.flasck.flas.patterns;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.parsedForm.FunctionIntro;

public class HSICtorTree extends HSIPatternTree {
	protected Map<String, HSIOptions> slots = new TreeMap<>();

	public HSICtorTree() {
	}
	
	@Override
	public int width() {
		return slots.size();
	}
	
	public HSIOptions field(String field) {
		if (!slots.containsKey(field)) {
			HSIPatternOptions hpo = new HSIPatternOptions();
			for (FunctionIntro fi : intros)
				hpo.includes(fi);
			slots.put(field, hpo);
		}
		return slots.get(field);
	}

	@Override
	public HSIOptions get(int i) {
		Iterator<HSIOptions> it = slots.values().iterator();
		while (true) {
			HSIOptions ret = it.next();
			if (i-- == 0)
				return ret;
		}
	}
}
