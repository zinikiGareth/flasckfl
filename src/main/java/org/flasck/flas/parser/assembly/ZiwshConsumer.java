package org.flasck.flas.parser.assembly;

import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public interface ZiwshConsumer {

	void wsuri(ExprToken tok);

	void security(ValidIdentifierToken mod, ValidIdentifierToken clz);

	void loginflow(ExprToken tok);

}
