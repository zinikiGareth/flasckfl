package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.NameOfThing;

public interface PatternsHolder extends Locatable {
	List<Pattern> args();
	NameOfThing name();
}
