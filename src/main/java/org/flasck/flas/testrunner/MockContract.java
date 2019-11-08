package org.flasck.flas.testrunner;

import org.flasck.jvm.fl.AreYouA;

public class MockContract implements AreYouA {
	private final Class<?> ctr;

	public MockContract(Class<?> ctr) {
		this.ctr = ctr;
	}

	@Override
	public boolean areYouA(String name) {
		return ctr.getName().equals(name);
	}
}
