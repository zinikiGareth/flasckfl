package org.flasck.flas.hsi;

import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.Repository.Visitor;

public interface TreeOrderVisitor extends Visitor {
	void argSlot(Slot s);
	void matchConstructor(StructDefn ctor);
	void matchField(StructField fld);
}
