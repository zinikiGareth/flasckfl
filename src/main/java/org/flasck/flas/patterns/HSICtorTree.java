package org.flasck.flas.patterns;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class HSICtorTree extends HSIPatternTree {
	protected Map<String, HSIOptions> slots = new TreeMap<>();

	public HSICtorTree() {
	}
	
	@Override
	public int width() {
		return slots.size();
	}
	
	public HSIOptions field(String field) {
		if (!slots.containsKey(field))
			slots.put(field, new HSIPatternOptions());
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
