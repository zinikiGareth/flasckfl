package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;

public interface UnifiableType extends Type {
	
	// This makes the statement that whatever the ultimate type is, it cannot be "bigger than" or "different to" incorporator
	// e.g. if it is incorporated by List, it can be Nil, Cons or List, but it cannot be Number
	// if it is incorporated by Nil, it cannot be Cons, List or Number
	void incorporatedBy(InputPosition pos, Type incorporator);
	
	// Ultimately we need to come up with some description of what this type is
	// It could be "Top" (Any), it could be a polymorphic var (eg A) or it could be a concrete type (such as Number)
	// It could also be something more complex, such as List[A]
	Type resolve();
}
