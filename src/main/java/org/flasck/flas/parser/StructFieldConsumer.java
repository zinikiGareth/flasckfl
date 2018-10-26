package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;

public interface StructFieldConsumer {

	StructDefn addField(StructField sf);

}
