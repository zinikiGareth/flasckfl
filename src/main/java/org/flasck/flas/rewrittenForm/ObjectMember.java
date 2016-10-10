package org.flasck.flas.rewrittenForm;

public class ObjectMember {
	public static final int CTOR = 1;
	public static final int ACOR = 2;
	public final int type;
	public final Object what;

	public ObjectMember(int type, Object what) {
		this.type = type;
		this.what = what;
	}
}
