package org.flasck.flas.parser;

import org.flasck.flas.compiler.BCEReceiver;
import org.flasck.flas.compiler.JSReceiver;

public interface TopLevelDefnConsumer extends TopLevelDefinitionConsumer {
	void jsTo(JSReceiver sendTo);
	void bceTo(BCEReceiver sendTo);
}
