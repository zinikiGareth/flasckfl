package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;

public class BlockLocationTracker implements LocationTracker {
	private final LocationTracker parent;
	protected final ErrorReporter errors;
	private InputPosition lastInner;

	public BlockLocationTracker(ErrorReporter errors, LocationTracker parent) {
		this.errors = errors;
		this.parent = parent;
	}
	
	@Override
	public void updateLoc(InputPosition location) {
		if (location != null && (lastInner == null || location.compareTo(lastInner) > 0))
			lastInner = location;
	}
	
	protected LocationTracker parentTracker() {
		return parent;
	}

	@Override
	public InputPosition lastInner() {
		return lastInner;
	}

	public void reduce(InputPosition from, String rule) {
		updateLoc(from);
		errors.logReduction(rule, from, lastInner);
		if (parent != null)
			parent.updateLoc(from);
	}
	
	public Runnable reduction(InputPosition from, String rule) {
		return new Runnable() {
			@Override
			public void run() {
				reduce(from, rule);
			}
			
		};
	}

	protected void tellParent(InputPosition location) {
		if (parent != null)
			parent.updateLoc(location);
	}

}
