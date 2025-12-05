package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parsedForm.assembly.ApplicationZiwsh;
import org.flasck.flas.parsedForm.assembly.Assembly;

public interface AssemblyDefinitionConsumer {
	void assembly(Assembly assembly);
	void routingTable(ApplicationRouting routing);
	void ziwshModel(ApplicationZiwsh ziwshModel);
}
