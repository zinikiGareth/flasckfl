package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;

public interface ParsedLineConsumer extends FunctionIntroConsumer {

	SolidName qualifyName(String base);
	CardName cardName(String name);

	void newCard(CardDefinition card);
	void newStruct(StructDefn sd);
	void newContract(ContractDecl decl);
	void newObject(ObjectDefn od);
	void tupleDefn(List<LocatedName> vars, FunctionName leadName, Expr expr);

	void scopeTo(ScopeReceiver sendTo);
}
