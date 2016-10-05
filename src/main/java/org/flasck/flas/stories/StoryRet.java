package org.flasck.flas.stories;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;

public class StoryRet {
	public final ErrorResult er;
	public final ScopeEntry top;

	public StoryRet(ErrorResult er, ScopeEntry top) {
		this.er = er;
		this.top = top;
	}

}
