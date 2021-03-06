package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;

public class TuplePattern implements Pattern {
	public final List<Pattern> args = new ArrayList<Pattern>();
	private final InputPosition loc;
	private LogicHolder definedBy;

	public TuplePattern(InputPosition loc, @SuppressWarnings("rawtypes") List arr) {
		this.loc = loc;
		for (Object o : arr)
			args.add((Pattern) o);
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	public LogicHolder definedBy() {
		return definedBy;
	}

	public void isDefinedBy(LogicHolder definedBy) {
		this.definedBy = definedBy;
	}
}
