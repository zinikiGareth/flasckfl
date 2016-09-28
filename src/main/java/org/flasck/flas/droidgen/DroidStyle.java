package org.flasck.flas.droidgen;

import java.util.HashMap;
import java.util.Map;

/** An extracted representation of a CSS style which we know how to handle.
 * We only go after a certain subset of the style.
 *
 * <p>
 * &copy; 2016 Ziniki Infrastructure Software, LLC.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class DroidStyle {
	private Map<String, Object> props = new HashMap<>();
	
	public boolean isEmpty() {
		return props.isEmpty();
	}

	public void set(String prop, Object val) {
		props.put(prop, val);
	}

	public Object get(String prop) {
		if (!props.containsKey(prop))
			return null;
		return props.get(prop);
	}

	public Object getFlasck(String fprop) {
		return get("x-flasck-" + fprop);
	}

}
