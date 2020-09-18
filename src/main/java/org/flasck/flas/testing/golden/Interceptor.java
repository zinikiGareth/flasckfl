package org.flasck.flas.testing.golden;

import java.util.List;

public interface Interceptor {
	void before(String dir);
	void addIncludes(List<String> args);
}
