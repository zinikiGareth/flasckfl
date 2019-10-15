package org.flasck.flas.hsi;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.Type;

public interface TreeOrderVisitor extends Visitor {
	void argSlot(Slot s);
	void matchConstructor(StructDefn ctor);
	void matchField(StructField fld);
	void matchType(Type ty, VarName var, FunctionIntro intro);
	void varInIntro(VarPattern vp, FunctionIntro intro);
}
