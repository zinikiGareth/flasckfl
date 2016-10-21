package org.flasck.flas.stories;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.Scope;

public class StoryRet {
	public final ErrorResult er;
	public final Scope scope;

	public StoryRet(ErrorResult er, Scope scope) {
		this.er = er;
		this.scope = scope;
	}

}
