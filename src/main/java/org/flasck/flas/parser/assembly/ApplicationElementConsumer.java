package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.assembly.ApplicationRouting;

public interface ApplicationElementConsumer {
	void baseuri(String s);
	void title(String s);
	void routes(ApplicationRouting routing);
}
