package org.flasck.flas.method;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.StackVisitor;

public class MethodConvertor extends LeafAdapter {
	private FunctionName fname;
	private ObjectMethod e;

	public MethodConvertor(StackVisitor sv) {
		sv.push(this);
	}

	@Override
	public void visitObjectMethod(ObjectMethod e) {
		this.e = e;
		fname = e.name();
	}

	// I think this is a COMPLETE hack
	@Override
	public void visitSendMessage(SendMessage msg) {
		// I actually claim the arguments are irrelevant because we just want the "case" really ...
		FunctionIntro fi = new FunctionIntro(fname, new ArrayList<>());
		// Obviously this should get wrapped up in "Send ..." unless it already is that
		// Hopefully we will get some help from TC (which should already have been performed)
		fi.functionCase(new FunctionCaseDefn(null, msg.expr));
		e.conversion(Arrays.asList(fi));
	}
}
