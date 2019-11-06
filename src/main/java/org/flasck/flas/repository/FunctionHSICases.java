package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionIntro;

public class FunctionHSICases implements HSICases {
	// TODO: this is a hack during refactoring
	public List<FunctionIntro> intros;

	public FunctionHSICases(List<FunctionIntro> intros) {
		this.intros = intros;
	}

	@Override
	public boolean noRemainingCases() {
		return this.intros.isEmpty();
	}

	@Override
	public boolean singleton() {
		return this.intros.size() == 1;
	}

	@Override
	public boolean isFunction() {
		return true;
	}

	@Override
	public FunctionIntro onlyIntro() {
		if (!singleton())
			throw new RuntimeException("Can only get intro for singleton");
		return this.intros.get(0);
	}

	@Override
	public boolean contains(FunctionIntro fi) {
		return this.intros.contains(fi);
	}

	@Override
	public HSICases retain(List<FunctionIntro> intros) {
		List<FunctionIntro> intersect = new ArrayList<>(this.intros);
		intersect.retainAll(intros);
		return new FunctionHSICases(intersect);
	}

	@Override
	public void remove(HSICases unwanted) {
		List<FunctionIntro> other = ((FunctionHSICases)unwanted).intros;
		this.intros.removeAll(other);
	}

}
