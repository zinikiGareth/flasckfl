package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.hsi.Slot;
import org.flasck.flas.tc3.NamedType;

public class DontConsiderAgain {
	public class Considered {
		public final Slot s;
		public final NamedType ctor;
		public Considered(Slot s, NamedType ty) {
			this.s = s;
			this.ctor = ty;
		}
		
		@Override
		public String toString() {
			return s + "==" + ctor.name().uniqueName();
		}
	}

	private List<Considered> considered = new ArrayList<>();
	
	public DontConsiderAgain() {
	}

	public DontConsiderAgain considered(Slot s, NamedType ty) {
		for (Considered cs : considered) {
			if (cs.s == s && cs.ctor == ty)
				return null;
		}
		DontConsiderAgain ret = new DontConsiderAgain();
		ret.considered.addAll(considered);
		ret.considered.add(new Considered(s,ty));
		return ret;
	}
	
	@Override
	public String toString() {
		return "DCA" + considered;
	}

}
