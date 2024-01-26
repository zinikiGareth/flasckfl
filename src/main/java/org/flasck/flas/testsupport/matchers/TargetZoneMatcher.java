package org.flasck.flas.testsupport.matchers;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.parsedForm.TargetZone;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TargetZoneMatcher extends TypeSafeMatcher<TargetZone> {
	private final List<Object> path;

	public TargetZoneMatcher(List<Object> path) {
		this.path = path;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Zone");
		arg0.appendValue(path);
	}

	@Override
	protected boolean matchesSafely(TargetZone tz) {
		if (path.size() != tz.fields.size())
			return false;
		
		for (int i=0;i<path.size();i++) {
			if (!path.get(i).equals(tz.fields.get(i)))
				return false;
		}
		
		return true;
	}

	public static TargetZoneMatcher path(Object... path) {
		return new TargetZoneMatcher(Arrays.asList(path));
	}

}
