package test.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class PDPMatcher extends TypeSafeMatcher<PublishDiagnosticsParams> {
	public class WithDiagnostic {
		private int line;
		private int from;
		private int to;
		private String message;

		public WithDiagnostic(int line, int from, int to, String message) {
			this.line = line;
			this.from = from;
			this.to = to;
			this.message = message;
		}
		
		public boolean compareTo(Diagnostic actual) {
			Range range = actual.getRange();
			Position start = range.getStart();
			if (start.getLine() != line) {
				return false;
			}
			if (start.getCharacter() != from) {
				return false;
			}
			Position end = range.getEnd();
			if (end.getLine() != line) {
				return false;
			}
			if (end.getCharacter() != to) {
				return false;
			}
			if (!actual.getMessage().equals(message)) {
				return false;
			}
			return true;
		}
	}

	private String matchUri;
	private List<WithDiagnostic> diagnostics = new ArrayList<>();

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("PublishDiagnostics[");
		arg0.appendValue(matchUri);
		arg0.appendText("]");
	}

	public PDPMatcher withUri(String uri) {
		this.matchUri = uri;
		return this;
	}
	
	public PDPMatcher diagnostic(int line, int from, int to, String message) {
		diagnostics.add(new WithDiagnostic(line, from, to, message));
		return this;
	}
	
	@Override
	protected boolean matchesSafely(PublishDiagnosticsParams pdp) {
		if (!pdp.getUri().equals(matchUri)) {
			return false;
		}
		if (pdp.getDiagnostics().size() != diagnostics.size()) {
			return false;
		}
		for (int i=0;i<diagnostics.size();i++) {
			if (!diagnostics.get(i).compareTo(pdp.getDiagnostics().get(i))) {
				return false;
			}
		}
		return true;
	}

	public static PDPMatcher uri(String uri) {
		return new PDPMatcher().withUri(uri);
	}

}
