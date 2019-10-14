package test.tc3;

import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TypeMatcher extends TypeSafeMatcher<Type> {
	private final String tn;

	public TypeMatcher(String ty) {
		this.tn = ty;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("MatchType[");
		arg0.appendValue(tn);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(Type arg0) {
		if (tn != null) {
			RepositoryEntry nt = (RepositoryEntry) arg0;
			if (!tn.equals(nt.name().uniqueName()))
				return false;
		}
		return true;
	}

	public static TypeMatcher named(String ty) {
		return new TypeMatcher(ty);
	}
}
