package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class GuardedMessagesMatcher extends TypeSafeMatcher<GuardedMessages> {
	private InputPosition pos;

	public static GuardedMessagesMatcher of(final ExprMatcher matcher) {
		return new GuardedMessagesMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("| ");
				if (matcher != null)
					matcher.describeTo(desc);
						
				if (super.pos != null) {
					desc.appendText("pos");
					desc.appendValue(super.pos);
				}
			}

			@Override
			protected boolean matchesSafely(GuardedMessages msg) {
				if (matcher == null && msg.guard != null)
					return false;
				if (matcher != null && !matcher.matches(msg.guard))
					return false;
				if (super.pos != null) {
					if (msg.location() == null)
						return false;
					if (super.pos.compareTo(msg.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public GuardedMessagesMatcher location(String file, int line, int off, int end) {
		pos = new InputPosition(file, line, off, null, "");
		pos.endAt(end);
		return this;
	}
}
