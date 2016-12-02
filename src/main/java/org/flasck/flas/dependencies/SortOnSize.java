package org.flasck.flas.dependencies;

import java.util.Comparator;
import java.util.Set;

/** I want to sort a set on size, but you need to be careful because if you say they're equal size, it throws one away
 * <p>
 * &copy; 2015 Ziniki Infrastructure Software, LLC.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class SortOnSize implements Comparator<Set<?>> {

	@Override
	public int compare(Set<?> o1, Set<?> o2) {
		if (o1.size() != o2.size())
			return Integer.compare(o1.size(), o2.size());
		return Integer.compare(o1.hashCode(), o2.hashCode());
	}

}
