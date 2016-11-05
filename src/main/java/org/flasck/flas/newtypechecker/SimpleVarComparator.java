package org.flasck.flas.newtypechecker;

import java.util.Comparator;

import org.flasck.flas.vcode.hsieForm.Var;

public class SimpleVarComparator implements Comparator<Var> {

	@Override
	public int compare(Var o1, Var o2) {
		return Integer.compare(o1.idx, o2.idx);
	}

}
