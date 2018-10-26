package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.StructDefn;

public interface TopLevelDefnConsumer extends ParsedLineConsumer {

	void newStruct(StructDefn sd);

}
