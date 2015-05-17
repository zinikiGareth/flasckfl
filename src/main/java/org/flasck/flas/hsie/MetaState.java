package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;

public class MetaState {
	final List<State> allStates = new ArrayList<State>();
	final List<Var> vars = new ArrayList<Var>();

	public void add(State s) {
		allStates.add(s);
	}

	public boolean allDone() {
		return allStates.isEmpty();
	}

	public State first() {
		return allStates.remove(0);
	}

	public Var allocateVar() {
		Var ret = new Var(vars.size());
		vars.add(ret);
		return ret;
	}
}
