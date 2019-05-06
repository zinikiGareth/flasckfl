package org.flasck.flas.parsedForm;

import java.io.Writer;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.ScopeName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.parser.ParsedLineConsumer;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class FunctionCaseDefn implements ContainsScope, Locatable, ParsedLineConsumer {
	public final FunctionIntro intro;
	public final Object expr;
	private Scope scope;
	private ScopeName caseName;

	public FunctionCaseDefn(FunctionName name, List<Object> args, Object expr) {
		intro = new FunctionIntro(name, args);
		if (expr == null)
			throw new UtilException("Cannot build function case with null expr");
		this.expr = expr;
	}

	@Override
	public IScope innerScope() {
		return scope;
	}

	@Override
	public InputPosition location() {
		return intro.location;
	}

	public CodeType mytype() {
		return intro.name().codeType;
	}

	public int nargs() {
		return intro.args.size();
	}

	public FunctionName functionName() {
		return intro.name();
	}

	public void provideCaseName(int caseNum) {
		this.caseName = new ScopeName(this.intro.name().inContext, this.intro.name().name+"_"+caseNum);
		this.scope = new Scope(this.caseName);
	}

	public ScopeName caseName() {
		if (caseName == null)
			throw new UtilException("Asked for caseName when none provided");
		return caseName;
	}
	
	public void dumpTo(Writer pw) throws Exception {
		pw.append(" ");
		for (Object o : intro.args) {
			pw.append(" ");
			pw.append(o.toString());
		}
		pw.append(" =\n");
		pw.append("    ");
		pw.append(expr.toString());
		pw.append("\n");
	}
	
	@Override
	public String toString() {
		return "FCD[" + intro.name().uniqueName() + "/" + intro.args.size() + "]";
	}

	// TODO: I think we should extract these to a base class that recognizes scoped things
	// TODO: it shouldn't allow cards to be defined internally
	@Override
	public FunctionName functionName(InputPosition location, String base) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void functionIntro(FunctionIntro o) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void functionCase(FunctionCaseDefn o) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public SolidName qualifyName(String base) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public CardName cardName(String name) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void newCard(CardDefinition card) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void newStruct(StructDefn sd) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void newContract(ContractDecl decl) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void newObject(ObjectDefn od) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void tupleDefn(List<LocatedName> vars, FunctionName leadName, Expr expr) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void scopeTo(ScopeReceiver sendTo) {
	}
}
