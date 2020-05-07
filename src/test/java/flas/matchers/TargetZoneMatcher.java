package flas.matchers;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.parsedForm.TargetZone;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TargetZoneMatcher extends TypeSafeMatcher<TargetZone> {
	private final List<String> path;

	public TargetZoneMatcher(List<String> path) {
		this.path = path;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Zone");
		arg0.appendValue(path);
	}

	@Override
	protected boolean matchesSafely(TargetZone tz) {
		return path.size() == 1 && path.get(0).equals(tz.text);
	}

	public static TargetZoneMatcher path(String... path) {
		return new TargetZoneMatcher(Arrays.asList(path));
	}

}
