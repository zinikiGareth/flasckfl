package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.HandlerImplements;

public interface HandlerConsumer {
	void newHandler(ErrorReporter errors, HandlerImplements hi);
}
