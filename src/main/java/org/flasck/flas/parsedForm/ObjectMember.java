package org.flasck.flas.parsedForm;

public class ObjectMember {
	public static final int CTOR = 1;
	public static final int ACCESSOR = 2;
	public static final int METHOD = 3;
	public static final int INTERNAL = 4;
	public final int type;
	public final Object what;

	public ObjectMember(int type, Object what) {
		this.type = type;
		this.what = what;
	}
}
