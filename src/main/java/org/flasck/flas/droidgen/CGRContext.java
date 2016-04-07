package org.flasck.flas.droidgen;

import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public class CGRContext {
	final ByteCodeCreator bcc;
	final NewMethodDefiner ctor;
	final Var card;
	final Var parent;
	MethodDefiner currentMethod;

	public CGRContext(ByteCodeCreator bcc, NewMethodDefiner ctor, Var card, Var parent) {
		this.bcc = bcc;
		this.ctor = ctor;
		this.card = card;
		this.parent = parent;
	}

}
