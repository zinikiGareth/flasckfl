package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.SendMessage;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class SendMessageMatcher extends TypeSafeMatcher<SendMessage> {
	private InputPosition pos;

	public static SendMessageMatcher of(final ExprMatcher matcher, ExprMatcher handlerMatcher) {
		return new SendMessageMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("<- ");
				matcher.describeTo(desc);
				if (handlerMatcher != null) {
					desc.appendText("->");
					handlerMatcher.describeTo(desc);
				}
						
				if (super.pos != null) {
					desc.appendText("pos");
					desc.appendValue(super.pos);
				}
			}

			@Override
			protected boolean matchesSafely(SendMessage msg) {
				if (!matcher.matches(msg.expr))
					return false;
				if (handlerMatcher == null && msg.handlerExpr() != null) {
					return false;
				}
				if (handlerMatcher != null) {
					if (msg.handlerExpr() == null)
						return false;
					if (!handlerMatcher.matches(msg.handlerExpr()))
						return false;
				}
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

	public SendMessageMatcher location(String file, int line, int off, int end) {
		pos = new InputPosition(file, line, off, null, "");
		pos.endAt(end);
		return this;
	}
}
