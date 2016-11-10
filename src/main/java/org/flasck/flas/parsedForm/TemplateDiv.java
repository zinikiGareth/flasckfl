package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateDiv extends TemplateFormatEvents {
	public final InputPosition customTagLoc;
	public final String customTag;
	public final InputPosition customTagVarLoc;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<TemplateLine> nested = new ArrayList<TemplateLine>();
	public List<String> droppables;

	public TemplateDiv(InputPosition kw, InputPosition ctLoc, String customTag, InputPosition ctvLoc, String customTagVar, List<Object> attrs, List<Object> formats) {
		super(kw, formats);
		this.customTagLoc = ctLoc;
		this.customTag = customTag;
		this.customTagVarLoc = ctvLoc;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
	}
}
