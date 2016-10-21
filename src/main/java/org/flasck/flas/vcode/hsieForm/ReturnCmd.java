package org.flasck.flas.vcode.hsieForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.zinutils.utils.Justification;

public class ReturnCmd extends PushReturn {
	public final List<CreationOfVar> deps;

	public ReturnCmd(InputPosition loc, CreationOfVar var, List<CreationOfVar> deps) {
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
		return Justification.LEFT.format("RETURN " + textValue() + (deps == null? "" : " " + deps), 60) + " " + location + " - this appears to be wrong for closures; wants to be the apply expr point";
	}
}
