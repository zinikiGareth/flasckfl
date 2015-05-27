package org.flasck.flas.typechecker;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;

public class TypeChecker {
	public final ErrorResult errors = new ErrorResult();

	public TypeChecker() {
		
	}
	
	public void typecheck() {
		
	}

	public Object tcExpr(Object expr) {
		if (expr instanceof ItemExpr) {
			ExprToken tok = ((ItemExpr) expr).tok;
			if (tok.type == ExprToken.NUMBER)
				return new TypeExpr("Number", null);
		}
		return null;
	}
}
