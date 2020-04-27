package org.flasck.flas.hsi;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.tc3.Type;

public interface TreeOrderVisitor extends RepositoryVisitor {
	void argSlot(Slot s);
	void matchConstructor(StructDefn ctor);
	void matchField(StructField fld);
	void matchType(Type ty, VarName var, FunctionIntro intro);
	void varInIntro(VarName vn, VarPattern vp, FunctionIntro intro);
	void endField(StructField fld);
	void endConstructor(StructDefn ctor);
	void endArg(Slot s);
}
