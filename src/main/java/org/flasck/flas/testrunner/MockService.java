package org.flasck.flas.testrunner;

import org.flasck.jvm.FlasckService;
import org.flasck.jvm.HandleDirectly;
import org.flasck.jvm.post.DeliveryAddress;

public class MockService extends FlasckService implements HandleDirectly {

	public MockService(String called) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object process(DeliveryAddress from, String method, Object[] args) {
		System.out.println("MockService.process() called for " + method);
		return null;
	}

}
