package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.FieldsHolder;
import org.flasck.flas.parsedForm.StructField;

public interface StructFieldConsumer {
	void addField(StructField sf);
	FieldsHolder holder();
}
