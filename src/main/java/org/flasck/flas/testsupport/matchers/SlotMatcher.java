package org.flasck.flas.testsupport.matchers;

import java.util.List;

import org.flasck.flas.hsi.Slot;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.zinutils.support.jmock.CaptureAction;

public class SlotMatcher extends TypeSafeMatcher<Slot> {
	private CaptureAction slots;
	private int slotNum;
	private String id;

	public SlotMatcher(String id) {
		this.id = id;
	}
	
	public SlotMatcher(CaptureAction slots, int slotNum) {
		this.slots = slots;
		this.slotNum = slotNum;
	}

	public static SlotMatcher from(CaptureAction slots, int slot) {
		return new SlotMatcher(slots, slot);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Slot[");
		if (id != null)
			arg0.appendValue(id);
		else if (slots == null)
			arg0.appendValue(slotNum);
		else if (!slots.hasCaptured())
			arg0.appendValue(slotNum + " not captured");
		else {
			List<Slot> s = (List<Slot>)slots.get(0);
			if (s == null)
				arg0.appendValue(slotNum + " null");
			else
				arg0.appendValue(s.get(slotNum));
		}
		arg0.appendText("]");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean matchesSafely(Slot arg0) {
		if (id != null)
			return id.equals(arg0.id());
		return ((List<Slot>)slots.get(0)).get(slotNum) == arg0;
	}

	public static SlotMatcher id(String id) {
		return new SlotMatcher(id);
	}

}
