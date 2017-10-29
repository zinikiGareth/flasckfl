package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.jvm.container.FlasckService;
import org.ziniki.ziwsh.postbox.HandleDirectly;
import org.zinutils.exceptions.UtilException;

import netscape.javascript.JSObject;

public class MockService extends FlasckService implements HandleDirectly {
	private final String ctr;
	private final List<String> errors;
	private final List<Expectation> expectations;
	private final List<Invocation> invocations;

	public MockService(String called, List<String> errors, List<Invocation> invocations, List<Expectation> allExpected) {
		this.ctr = called;
		this.errors = errors;
		this.invocations = invocations;
		this.expectations = allExpected;
	}

	// This is what processing looks like coming from the JVM world
	@Override
	public Object handleRequest(String method, Object[] args) {
		checkInvocation(method, Arrays.asList(args));
		return null; // it's OK to return null since this is a Mock object - the test will provide any additional actions
	}

	// This is called from the JS world
	public Object process(final JSObject msg) {
		try {
			String method = (String) msg.getMember("method");
			JSObject args = (JSObject) msg.getMember("args");
			int alen = (int)args.getMember("length");
			List<Object> ao = new ArrayList<>();
			for (int i=0;i<alen;i++)
				ao.add(args.getSlot(i)); // TODO: we probably also need to do further JS -> Java conversion
			checkInvocation(method, ao);
		} catch (Throwable t) {
			t.printStackTrace();
			errors.add(t.getMessage());
		}
		return null;
	}

	protected void checkInvocation(String method, List<Object> ao) {
		invocations.add(new Invocation(ctr, method, ao));
		for (Expectation e : expectations) {
			if (e.contract.equals(ctr) && e.method.equals(method)) {
				expectations.remove(e);
				return;
			}
		}
		throw new UtilException("Unexpected call of " + method + " in " + ctr + " with " + ao.size() + " args: " + ao.get(0).getClass());
	}
}
