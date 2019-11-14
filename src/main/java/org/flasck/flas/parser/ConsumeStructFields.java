package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.StructField;

public class ConsumeStructFields implements StructFieldConsumer {
	private final TopLevelDefinitionConsumer consumer;
	private final VarNamer namer;
	private final FieldsDefn sd;

	public ConsumeStructFields(TopLevelDefinitionConsumer consumer, VarNamer namer, FieldsDefn sd) {
		this.consumer = consumer;
		this.namer = namer;
		this.sd = sd;
	}

	@Override
	public void addField(StructField sf) {
		sf.fullName(namer.nameVar(sf.loc, sf.name));
		sd.addField(sf);
		consumer.newStructField(sf);
	}

}
