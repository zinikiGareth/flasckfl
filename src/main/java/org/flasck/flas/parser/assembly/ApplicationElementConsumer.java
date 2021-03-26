package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.assembly.ApplicationRouting;

public interface ApplicationElementConsumer {

	void title(String s);
	void mainCard(String s);
	void routes(ApplicationRouting routing);

}
