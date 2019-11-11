package org.flasck.flas.testrunner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.flasck.jvm.builtin.Send;
import org.ziniki.ziwsh.json.FLEvalContext;
import org.zinutils.exceptions.NotImplementedException;

public class MockContract implements InvocationHandler {
	private final Class<?> ctr;

	public MockContract(Class<?> ctr) {
		this.ctr = ctr;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// Handle "special" things ...
		switch (method.getName()) {
		case "areYouA":
			return ctr.getName().equals(((String)args[0]) + "$Up");
		case "equals":
			throw new NotImplementedException("no equals");
		case "toString":
			return "MockContract[" + ctr.getName() + "]";
		}
		// Anything else must be on the contract ...
		FLEvalContext cx = (FLEvalContext) args[0];
		List<Object> as = new ArrayList<>();
		for (int i=1;i<args.length-1;i++)
			as.add(args[i]);
		Object handle = args[args.length-1];
		String meth = method.getName(); 
		return Send.eval(cx, new Object[] { proxy, meth, as, handle });
	}
	
	@Override
	public String toString() {
		return "NotIHMockContract[" + ctr + "]";
	}
}
