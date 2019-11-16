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
		// it's the same hack
		if (fi == null)
			return true;
		if (this.intros.size() == 1 && intros.get(0) == null)
			return true;
		return this.intros.contains(fi);
	}

	@Override
	public HSICases retain(List<FunctionIntro> intros) {
		List<FunctionIntro> intersect = new ArrayList<>(this.intros);
		// this is somewhat of a hack, but valid ... 
		// basically, for methods, we can't provide the real intro at conversion time, we need to provide null
		// so now, return "the thing" if we are asked to retain null
		if (intros.size() == 1 && intros.get(0) == null) {
			// it's a method ... all good
		} else {
			intersect.retainAll(intros);
		}
		return new FunctionHSICases(intersect);
	}

	@Override
	public void remove(HSICases unwanted) {
		List<FunctionIntro> other = ((FunctionHSICases)unwanted).intros;
		this.intros.removeAll(other);
	}

}