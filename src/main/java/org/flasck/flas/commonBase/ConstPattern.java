package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.LogicHolder;

public class ConstPattern implements Pattern {
	public static final int INTEGER = 1;
	public static final int BOOLEAN = 2;
	public static final int STRING = 3;

	public final int type;
	public final String value;
	private InputPosition location;
	private LogicHolder definedBy;

	public ConstPattern(InputPosition loc, int type, String value) {
		this.location = loc;
		this.type = type;
		this.value = value;
	}
	
	public LogicHolder definedBy() {
		return definedBy;
	}

	public void isDefinedBy(LogicHolder definedBy) {
		this.definedBy = definedBy;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public Pattern locatedAt(InputPosition location) {
		this.location = location;
		return this;
	}

	@Override
	public String toString() {
		return value;
	}
}
