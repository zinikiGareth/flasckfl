package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class MethodConvertor extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final List<Expr> results = new ArrayList<>();

	public MethodConvertor(NestedVisitor sv) {
		this.sv = sv;
	}

	@Override
	public void visitMessage(ActionMessage msg) {
		sv.push(new MessageConvertor(sv));
	}
	
	@Override
	public void result(Object r) {
		results.add((Expr)r);
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		List<FunctionIntro> convertedIntros = new ArrayList<FunctionIntro>();
		meth.conversion(convertedIntros);
		FunctionIntro fi = new FunctionIntro(meth.name(), new ArrayList<>());
		fi.functionCase(new FunctionCaseDefn(null, new Messages(meth.location(), results)));
		meth.conversion(Arrays.asList(fi));
		sv.result(null);
	}
}
