package org.flasck.flas.testrunner;

import java.util.List;

public class Expectation {
	final String contract;
	final String method;
	final List<Object> args;

	public Expectation(String ctr, String method, List<Object> args) {
		this.contract = ctr;
		this.method = method;
		this.args = args;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Expectation))
			return false;
		
		Expectation e = (Expectation) o;
		if (!e.contract.equals(contract))
			return false;
		if (!e.method.equals(method))
			return false;
		if (args.size() != e.args.size())
			return false;

		// TODO: this is not right
		// We need to test that evaluating "args_"a(i) is the same as "eargs_"ea(i)
		// not that a(i) == ea(i), which is irrelevant
		for (int i=0; i<args.size(); i++)
			if (!args.get(i).equals(e.args.get(i)))
				return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "expect(" + contract + "." + method + args +")";
	}
}
