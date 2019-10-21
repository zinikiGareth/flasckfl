package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.StructDefn;

public interface UnifiableType extends Type {
	// particularly for the pattern-matching case, but also if an expression is created which returns this type,
	// say that this slot can be represented by a particular struct defn
	// In this case, we allow the struct defn to be further constrained on its fields
	StructTypeConstraints canBeStruct(StructDefn sd);

	// We can represent the notion that a variable is typed in an argument
	void canBeType(Type ofType);

	// This makes the statement that whatever the ultimate type is, it cannot be "bigger than" or "different to" incorporator
	// e.g. if it is incorporated by List, it can be Nil, Cons or List, but it cannot be Number
	// if it is incorporated by Nil, it cannot be Cons, List or Number
	void incorporatedBy(InputPosition pos, Type incorporator);

	// This says that the value is returned (somewhere? From the top level?)
	// Anyway, the upshot is that it is almost undoubtedly used more than once (it must have come from somewhere)
	// and thus needs to be polymorphic
	void isReturned();

	// Ultimately we need to come up with some description of what this type is
	// It could be "Top" (Any), it could be a polymorphic var (eg A) or it could be a concrete type (such as Number)
	// It could also be something more complex, such as List[A]
	Type resolve();

	// We conclude that this is being used in a function application and as such must be a function
	// able to be applied to these types
	Application canBeAppliedTo(List<Type> results);
}
