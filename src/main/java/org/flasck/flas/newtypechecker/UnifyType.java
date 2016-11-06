package org.flasck.flas.newtypechecker;

import java.util.HashSet;
import java.util.Set;

public class UnifyType extends TypeInfo {
	public final Set<TypeInfo> types = new HashSet<TypeInfo>();
	
	public UnifyType() {
	}
	
	public UnifyType(TypeInfo a) {
		types.add(a);
	}
	
	public void add(TypeInfo a) {
		types.add(a);
	}

	@Override
	public String toString() {
		return "Unify" + types;
	}
}
