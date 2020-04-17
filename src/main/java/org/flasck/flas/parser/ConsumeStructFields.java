package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.StructField;

public class ConsumeStructFields implements StructFieldConsumer {
	private final ErrorReporter errors;
	private final TopLevelDefinitionConsumer consumer;
	private final VarNamer namer;
	private final FieldsDefn sd;

	public ConsumeStructFields(ErrorReporter errors, TopLevelDefinitionConsumer consumer, VarNamer namer, FieldsDefn sd) {
		this.errors = errors;
		this.consumer = consumer;
		this.namer = namer;
		this.sd = sd;
	}

	@Override
	public void addField(StructField sf) {
		sf.fullName(namer.nameVar(sf.loc, sf.name));
		sd.addField(sf);
		consumer.newStructField(errors, sf);
	}

}
