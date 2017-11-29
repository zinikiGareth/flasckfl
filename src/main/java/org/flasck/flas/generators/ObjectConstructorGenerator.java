package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.MethodDefiner;

public class ObjectConstructorGenerator<T> implements CodeGenerator<T> {

	@Override
	public void begin(GenerationContext<T> cxt) {
		NameOfThing clzContext = cxt.nameContext();
		final String inClz = clzContext.javaName();
		cxt.selectClass(inClz);
		cxt.staticMethod();
		cxt.trampoline(inClz);
		
		// create eval method in object class directly
		ByteCodeSink bcc = cxt.getSink();
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, true, "eval");
		PendingVar cx = gen.argument(J.OBJECT, "cx");
		PendingVar args = gen.argument("[" + J.OBJECT, "args");
		gen.returns(J.OBJECT);
		MethodDefiner meth = gen.done();
		meth.returnObject(meth.makeNew(inClz, cx.getVar(), meth.arrayElt(args.getVar(), meth.intConst(0)))).flush();
	}

}
