package org.flasck.flas.vcode.hsieForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;

public class ReturnCmd extends PushReturn {
	public final List<Var> deps;

	public ReturnCmd(InputPosition loc, Var var, List<Var> deps) {
		super(loc, var);
		this.deps = deps;
	}

	public ReturnCmd(InputPosition loc, int i) {
		super(loc, i);
		this.deps = null;
	}
	
	public ReturnCmd(InputPosition loc, StringLiteral s) {
		super(loc, s);
		this.deps = null;
	}

	public ReturnCmd(InputPosition loc, ExternalRef fn) {
		super(loc, fn);
		this.deps = null;
	}

	public ReturnCmd(InputPosition loc, TemplateListVar tlv) {
		super(loc, tlv);
		this.deps = null;
	}

	@Override
	public String toString() {
		return "RETURN " + textValue();
	}
}
