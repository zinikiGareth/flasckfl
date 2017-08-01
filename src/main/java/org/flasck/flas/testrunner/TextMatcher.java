package org.flasck.flas.testrunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface TextMatcher {

	public class Exact implements TextMatcher {
		private final String str;

		public Exact(String str) {
			this.str = str;
		}

		@Override
		public boolean matches(String html) {
			return html.equals(str);
		}

		@Override
		public String getDescription() {
			return "'" + str + "'";
		}

	}

	public class Regexp implements TextMatcher {
		private final String pattern;

		public Regexp(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean matches(String html) {
			Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(html);
			return matcher.matches();
		}

		@Override
		public String getDescription() {
			return "matches /" +pattern + "/";
		}
	}

	boolean matches(String html);

	String getDescription();

}
