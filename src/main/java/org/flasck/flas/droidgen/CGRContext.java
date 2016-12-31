package org.flasck.flas.droidgen;

import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public class CGRContext {
	final ByteCodeSink bcc;
	public final NewMethodDefiner ctor;
	final Var card;
	final Var parent;
	MethodDefiner currentMethod;

	public CGRContext(ByteCodeSink bcc, NewMethodDefiner ctor, Var card, Var parent) {
		this.bcc = bcc;
		this.ctor = ctor;
		this.card = card;
		this.parent = parent;
	}

}
