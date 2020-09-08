package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;

public class FlimApplyReader extends FlimTypeReader implements TDAParsing, PendingType {
	private final FlimTypeReader collector;
	private final List<PendingType> args = new ArrayList<>();

	public FlimApplyReader(ErrorReporter errors, FlimTypeReader collector) {
		super(errors);
		this.collector = collector;
	}

	@Override
	public void collect(PendingType ty) {
		args.add(ty);
	}

	@Override
	public Type resolve(ErrorReporter errors, Repository repository) {
		List<Type> as = new ArrayList<>();
		for (PendingType pa : args) {
			as.add(pa.resolve(errors, repository));
		}
		return new Apply(as);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		collector.collect(this);
	}

}
