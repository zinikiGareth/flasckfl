package org.flasck.flas.tc3;

import java.util.HashSet;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StructDefn;
import org.zinutils.graphs.DirectedAcyclicGraph;

public interface UnifiableType extends Type {
	// Its id
	String id();
	
	// Why was it created?
	String motive();
	
	// Ultimately we need to come up with some description of what this type is
	// It could be "Top" (Any), it could be a polymorphic var (eg A) or it could be a concrete type (such as Number)
	// It could also be something more complex, such as List[A]
	Type resolve(ErrorReporter errors);

	// Return the resolved type
	Type resolvedTo();

	// Assert that this UT must be a message
	void mustBeMessage();

	// particularly for the pattern-matching case, but also if an expression is created which returns this type,
	// say that this slot can be represented by a particular struct defn
	// In this case, we allow the struct defn to be further constrained on its fields
	StructTypeConstraints canBeStruct(InputPosition pos, FunctionName fn, StructDefn sd);

	// We can represent the notion that a variable is typed in an argument
	void canBeType(InputPosition pos, Type ofType);

	// This makes the statement that whatever the ultimate type is, it cannot be "bigger than" or "different to" incorporator
	// e.g. if it is incorporated by List, it can be Nil, Cons or List, but it cannot be Number
	// if it is incorporated by Nil, it cannot be Cons, List or Number
	void incorporatedBy(InputPosition pos, Type incorporator);

	// This says that the value is returned from a sub-expression
	// Anyway, the upshot is that it is almost undoubtedly used more than once (it must have come from somewhere)
	// and thus needs to be polymorphic
	void isReturned(InputPosition pos);

	// The value is used as an argument in an expression where the function is also an unknown
	// This causes the two to be bound together, requiring a polymorphic variable
	void isUsed(InputPosition pos);

	// We conclude that this is being used in a function application and as such must be a function
	// able to be applied to these types
	UnifiableType canBeAppliedTo(InputPosition pos, List<PosType> results);

	// When a function has polymorphic args, a UT is instantiated to handle that
	// This is called when one of those is passed a variable
	void isPassed(InputPosition loc, Type ai);

	// When processing groups, it is necessary to introduce a function type variable in case the function is used
	// At the end of processing, this needs to be resolved
	void determinedType(PosType value);

	// Many UTs can end up being bound to the same thing
	// Make sure all of them know about everything
	boolean enhance();

	// Copy the knowledge from another UnifiableType
	void sameAs(InputPosition pos, Type type);

	// Gather together all the info ready for resolution
	void collectInfo(ErrorReporter errors, DirectedAcyclicGraph<UnifiableType> dag);

	// acquire any UTs that have not already been considered, or else be acquired by ones that have
	void acquireOthers(List<UnifiableType> considered);

	// has this been acquired by somebody else?
	boolean isRedirected();

	UnifiableType redirectedTo();

	void acquire(UnifiableType ut);

	// expand all unions into their component parts for more easy merging and (later) unification
	void expandUnions();

	// look at all polymorphic types and consolidate their arguments
	void mergePolyVars();
}
