package org.flasck.flas.parser;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.tokenizers.Tokenizable;

// This tracks errors and expects a Mock ErrorReporter
// It will pass through errors being raised, but will answer
// the question of "have errors been raised" purely locally,
// thus allowing tests to handle that
public class LocalErrorTracker implements ErrorReporter {
	private final ErrorReporter other;
	boolean seenLocalErrors = false;
	
	public LocalErrorTracker(ErrorReporter other) {
		this.other = other;
	}

	public ErrorReporter message(InputPosition pos, String msg) {
		seenLocalErrors = true;
		return other.message(pos, msg);
	}

	public ErrorReporter message(Block b, String msg) {
		seenLocalErrors = true;
		return other.message(b, msg);
	}

	public ErrorReporter message(Tokenizable line, String msg) {
		seenLocalErrors = true;
		return other.message(line, msg);
	}

	public ErrorReporter message(FLASError e) {
		seenLocalErrors = true;
		return other.message(e);
	}

	@Override
	public ErrorReporter reportException(Throwable ex) {
		seenLocalErrors = true;
		return other.reportException(ex);
	}

	public void merge(ErrorReporter o) {
		seenLocalErrors = true;
		other.merge(o);
	}

	public void fakeErrorWithoutNeedingAssertion() {
		seenLocalErrors = true;
	}
	
	public boolean hasErrors() {
		return seenLocalErrors;
	}
}
