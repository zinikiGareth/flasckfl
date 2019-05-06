package org.flasck.flas.parser;

import org.flasck.flas.compiler.BCEReceiver;
import org.flasck.flas.compiler.JSReceiver;
import org.flasck.flas.parsedForm.StructDefn;

public interface TopLevelDefnConsumer extends ParsedLineConsumer {
	void jsTo(JSReceiver sendTo);
	void bceTo(BCEReceiver sendTo);
}
