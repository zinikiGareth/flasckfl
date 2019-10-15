package test.tc3;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PolyTypeMatcher extends TypeSafeMatcher<Type>{
	private final StructDefn sd;
	private final List<Matcher<Type>> asList;

	public PolyTypeMatcher(StructDefn sd, List<Matcher<Type>> list) {
		this.sd = sd;
		this.asList = list;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendValue(sd.name.uniqueName());
		arg0.appendValue(asList);
	}

	@Override
	protected boolean matchesSafely(Type arg0) {
		if (!(arg0 instanceof PolyInstance))
			return false;
		PolyInstance pi = (PolyInstance) arg0;
		List<Type> polys = pi.getPolys();
		if (polys.size() != asList.size())
			return false;
		for (int i=0;i<polys.size();i++) {
			if (!asList.get(i).matches(polys.get(i)))
				return false;
		}
		return true;
	}

	public static PolyTypeMatcher of(StructDefn sd, Matcher<Type>... args) {
		return new PolyTypeMatcher(sd, Arrays.asList(args));
	}
}
