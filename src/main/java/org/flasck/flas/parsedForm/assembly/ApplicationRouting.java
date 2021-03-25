package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.assembly.ApplicationElementConsumer;
import org.flasck.flas.parser.assembly.MainRoutingActionConsumer;

public class ApplicationRouting extends SubRouting implements MainRoutingActionConsumer {
	private final ErrorReporter errors;
	private final ApplicationElementConsumer consumer;
	public boolean sawMainCard;

	public ApplicationRouting(ErrorReporter errors, ApplicationElementConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public void provideMainCard(TypeReference main) {
		if (sawMainCard) {
			errors.message(main.location(), "duplicate assignment to main card");
			return;
		}
		sawMainCard = true;
		consumer.mainCard(main.name());
	}

}
