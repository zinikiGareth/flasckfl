package org.flasck.flas.testrunner;

import org.flasck.jvm.FlasckService;

public class CachedService {
	public final int chan;

	public CachedService(int chan, FlasckService service) {
		this.chan = chan;
	}

}
