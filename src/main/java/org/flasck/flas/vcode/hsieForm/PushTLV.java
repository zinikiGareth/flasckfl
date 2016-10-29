package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateListVar;

public class PushTLV extends PushReturn {
	public final TemplateListVar tlv;

	public PushTLV(InputPosition loc, TemplateListVar tlv) {
		super(loc);
		this.tlv = tlv;
	}

	protected Object textValue() {
		return tlv;
	}
}
