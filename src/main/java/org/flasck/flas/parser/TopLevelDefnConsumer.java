package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.BCEReceiver;
import org.flasck.flas.compiler.JSReceiver;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;

public interface TopLevelDefnConsumer extends ParsedLineConsumer {
	SolidName qualifyName(String base);
	void jsTo(JSReceiver sendTo);
	void bceTo(BCEReceiver sendTo);
	void scopeTo(ScopeReceiver sendTo);
	void functionCase(FunctionCaseDefn o);
	void newStruct(StructDefn sd);
	void functionIntro(FunctionIntro o);
}
