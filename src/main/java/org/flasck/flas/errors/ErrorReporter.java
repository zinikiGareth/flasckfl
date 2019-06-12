package org.flasck.flas.errors;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;

public interface ErrorReporter {

	ErrorReporter message(InputPosition pos, String msg);
	ErrorReporter message(Tokenizable line, String msg);
	ErrorReporter message(FLASError e);
	ErrorReporter reportException(Throwable ex);

	void merge(ErrorReporter o);

	boolean hasErrors();
	ErrorMark mark();
}
