package test.repository;

import org.flasck.flas.hsi.Slot;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.zinutils.support.jmock.CaptureAction;

public class SlotMatcher extends TypeSafeMatcher<Slot> {

	public static SlotMatcher from(CaptureAction slots, int i) {
		return new SlotMatcher();
	}

	@Override
	public void describeTo(Description arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean matchesSafely(Slot arg0) {
		return true;
	}

}
