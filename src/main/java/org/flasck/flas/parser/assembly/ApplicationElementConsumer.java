package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;

public interface ApplicationElementConsumer {

	void title(String s);
	void mainCard(TypeReference main);
	void routes(ApplicationRouting routing);

}
