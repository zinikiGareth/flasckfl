package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.exceptions.NotImplementedException;

public class BasicJVMCreationContext implements JVMCreationContext {
	ByteCodeSink bcc;
	NewMethodDefiner md;
	boolean isTest;
	Var runner;
	
	public BasicJVMCreationContext(ByteCodeEnvironment bce, String pkg, String name, boolean isStatic) {
		if (!isStatic) {
			throw new NotImplementedException();
		} else {
			String clz;
			String fn;
			if (name.startsWith("_ut")) {
				clz = pkg + "." + name;
				fn = "dotest";
				isTest = true;
			} else {
				clz = pkg + ".PACKAGEFUNCTIONS";
				fn = name;
				isTest = false;
			}
			if (bce.hasClass(clz))
				bcc = bce.get(clz);
			else
				bcc = bce.newClass(clz);
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, isStatic, fn);
			if (isTest) {
				PendingVar r1 = ann.argument(J.TESTHELPER, "runner");
				ann.argument(J.FLEVALCONTEXT, "cxt");
				ann.returns(JavaType.void_);
				md = ann.done();
				runner = r1.getVar();
			} else {
				ann.returns(J.OBJECT);
				md = ann.done();
				runner = null;
			}
		}
	}

	@Override
	public void done() {
		if (isTest) {
			md.callInterface("void", runner, "testComplete").flush();
			md.returnVoid().flush();
		}
	}

}
