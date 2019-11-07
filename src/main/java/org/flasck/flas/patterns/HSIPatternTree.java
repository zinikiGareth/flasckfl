package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.HSICases;

public abstract class HSIPatternTree implements HSITree {
	protected List<FunctionIntro> intros = new ArrayList<>();

	protected HSIPatternTree() {
	}

	@Override
	public HSITree consider(FunctionIntro fi) {
		intros.add(fi);
		return this;
	}

	@Override
	public List<FunctionIntro> intros() {
		return intros;
	}

	public boolean containsAny(HSICases curr) {
		for (FunctionIntro fi : intros)
			if (curr.contains(fi))
				return true;
		return false;
	}
}
