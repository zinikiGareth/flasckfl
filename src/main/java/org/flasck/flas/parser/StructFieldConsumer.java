package org.flasck.flas.parser;

import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.parsedForm.StructField;

public interface StructFieldConsumer {

	AsString addField(StructField sf);

}
