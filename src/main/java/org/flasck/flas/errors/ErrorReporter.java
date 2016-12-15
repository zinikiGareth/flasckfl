package org.flasck.flas.errors;

import org.flasck.flas.blockForm.InputPosition;

public interface ErrorReporter {

	ErrorResult message(InputPosition pos, String msg);

}
