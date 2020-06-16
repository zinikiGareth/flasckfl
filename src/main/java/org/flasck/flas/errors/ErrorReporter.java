package org.flasck.flas.errors;

import java.io.Writer;
import java.util.Collection;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;

public interface ErrorReporter {

	ErrorReporter message(InputPosition pos, String msg);
	ErrorReporter message(InputPosition pos, Collection<InputPosition> locs, String msg);
	ErrorReporter message(Tokenizable line, String msg);
	ErrorReporter message(FLASError e);
	ErrorReporter reportException(Throwable ex);

	void merge(ErrorReporter o);

	boolean hasErrors();
	ErrorMark mark();
	void showFromMark(ErrorMark mark, Writer pw, int ind);
}
