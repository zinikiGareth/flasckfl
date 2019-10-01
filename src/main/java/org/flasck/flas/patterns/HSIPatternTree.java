package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;

public abstract class HSIPatternTree implements HSITree {
	private List<FunctionIntro> intros = new ArrayList<>();

	protected HSIPatternTree() {
	}

	@Override
	public HSITree consider(FunctionIntro fi) {
		if (fi == null) {
			throw new NullPointerException("Cannot consider a null intro");
		}
		intros.add(fi);
		return this;
	}

	@Override
	public List<FunctionIntro> intros() {
		return intros;
	}

	public boolean containsAny(List<FunctionIntro> curr) {
		for (FunctionIntro fi : intros)
			if (curr.contains(fi))
				return true;
		return false;
	}
}
