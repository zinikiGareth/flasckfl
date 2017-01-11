package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class MatchStep implements TestStep {
	private final HTMLMatcher what;
	private final String selector;

	public MatchStep(InputPosition posn, HTMLMatcher matcher, String selector) {
		this.what = matcher;
		this.selector = selector;
	}

	@Override
	public void run(TestRunner runner) throws Exception {
		runner.match(what, selector);
	}
}
