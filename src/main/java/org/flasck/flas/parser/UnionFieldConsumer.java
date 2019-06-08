package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;

public interface UnionFieldConsumer {

	UnionTypeDefn addCase(TypeReference tr);

}
