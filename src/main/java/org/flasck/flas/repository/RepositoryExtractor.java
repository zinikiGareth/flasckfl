package org.flasck.flas.repository;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.LogicHolder;

public class RepositoryExtractor extends LeafAdapter {
	public static Set<LogicHolder> nongenFunctions(Repository r) {
		Set<LogicHolder> ret = new TreeSet<>();
		for (RepositoryEntry e : r.dict.values()) {
			if (e instanceof LogicHolder && !((LogicHolder)e).generate())
				ret.add((LogicHolder) e);
		}
		return ret;
	}
}
