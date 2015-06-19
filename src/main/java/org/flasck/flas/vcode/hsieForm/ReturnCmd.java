package org.flasck.flas.vcode.hsieForm;

import java.util.List;

import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;

public class ReturnCmd extends PushReturn {
	public final List<Var> deps;

	public ReturnCmd(Var var, List<Var> deps) {
		super(var);
		this.deps = deps;
	}

	public ReturnCmd(int i) {
		super(i);
		this.deps = null;
	}
	
	public ReturnCmd(StringLiteral s) {
		super(s);
		this.deps = null;
	}

	public ReturnCmd(ExternalRef fn) {
		super(fn);
		this.deps = null;
	}

	public ReturnCmd(TemplateListVar tlv) {
		super(tlv);
		this.deps = null;
	}

	@Override
	public String toString() {
		return "RETURN " + textValue();
	}
}
