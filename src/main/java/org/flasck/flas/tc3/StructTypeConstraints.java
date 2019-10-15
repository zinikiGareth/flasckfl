package org.flasck.flas.tc3;

import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.StructField;

public interface StructTypeConstraints {

	UnifiableType field(CurrentTCState state, InputPosition pos, StructField fld);

	Set<StructField> fields();

	UnifiableType get(StructField f);

}
