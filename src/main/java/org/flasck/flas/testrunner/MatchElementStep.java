package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;

public class MatchElementStep implements TestStep {
	private final String onCard;
	private final String selector;
	private final String contents;

	public MatchElementStep(InputPosition posn, String onCard, String selector, String contents) {
		this.onCard = onCard;
		this.selector = selector;
		this.contents = contents;
	}

	@Override
	public void run(TestRunner runner) throws Exception {
		runner.matchElement(onCard, selector, contents);
	}
}
