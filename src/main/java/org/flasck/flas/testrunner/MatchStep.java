package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class MatchStep implements TestStep {
	public enum WhatToMatch {
		ELEMENT, CONTENTS, COUNT
	};
	private final WhatToMatch what;
	private final String onCard;
	private final String selector;
	private final String value;

	public MatchStep(InputPosition posn, WhatToMatch what, String onCard, String selector, String contents) {
		this.what = what;
		this.onCard = onCard;
		this.selector = selector;
		this.value = contents;
	}

	@Override
	public void run(TestRunner runner) throws Exception {
		runner.match(what, onCard, selector, value);
	}
}
