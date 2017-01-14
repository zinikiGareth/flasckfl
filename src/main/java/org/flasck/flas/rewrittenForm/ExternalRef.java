package org.flasck.flas.rewrittenForm;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;

public interface ExternalRef extends Locatable, Comparable<Object>{
	public class Comparator implements java.util.Comparator<ExternalRef> {
		@Override
		public int compare(ExternalRef o1, ExternalRef o2) {
			return o1.compareTo(o2);
		}
	}

	public NameOfThing myName();
	
	public String uniqueName();

	public boolean fromHandler();
}
