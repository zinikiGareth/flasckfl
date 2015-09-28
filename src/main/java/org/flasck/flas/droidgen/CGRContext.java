package org.flasck.flas.droidgen;

import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.NewMethodDefiner;

public class CGRContext {
	final ByteCodeCreator bcc;
	final NewMethodDefiner ctor;

	public CGRContext(ByteCodeCreator bcc, NewMethodDefiner ctor) {
		this.bcc = bcc;
		this.ctor = ctor;
	}

}
