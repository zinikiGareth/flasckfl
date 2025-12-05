package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationZiwsh;

public interface ApplicationElementConsumer {
	void baseuri(String s);
	void title(String s);
	void routes(ApplicationRouting routing);
	void ziwsh(ApplicationZiwsh ziwshModel);
}
