package org.flasck.flas.parser.ut;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;

public interface IntroductionConsumer {
	void newIntroduction(ErrorReporter errors, IntroduceVar var);
}
