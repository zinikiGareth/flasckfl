package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class MatchStep implements TestStep {
	private final WhatToMatch what;
	private final String selector;
	private final String value;

	public MatchStep(InputPosition posn, WhatToMatch what, String selector, String contents) {
		this.what = what;
		this.selector = selector;
		this.value = contents;
	}

	@Override
	public void run(TestRunner runner) throws Exception {
		runner.match(what, selector, value);
	}
}
