#FLASCK - Functional Compiler for Cards#

This is a compiler for a state/transformation based functional language for cards.  One day, there will be a full set of documentation here, but for now the best that can be offered is the set of notes that have been collected along the way at the [GitHub Documentation Site][1].

##Basic Philosophy##

This work emerges from the work that Chris Tse's team have done on cards in JavaScript with advanced frameworks at [cardstack.io][2].  The main distinction is that instead of trying to build cards with a combination of JavaScript, a JavaScript framework and a card library, this language (and it's [companion runtime][3] with [GitHub experimental notes][4]) attempts to provide a view of cards that is simple and consistent.

Each card is defined as a combination of _state_, _template_ and _code_.

The **state** defines data that can be stored on the card and is essentially transactional and based on a "clock" or "gating" mechanism, much like JavaScript, where the values move between states at the end of each execution cycle.

The **template** defines HTML-like constructs that are based solely on the values in the state and are defined declaratively.  The runtime figures out the correct delta at the end of each execution cycle between the rendering of the old state and the rendering of the new state and makes the appropriate updates to the display.

The **code** portion consists of all the logic to handle state changes.  Mainly this consists of supporting _contracts_ in which the containing environment can provide data to, or make requests of, the card; and handling _events_ in the JavaScript sense.  These methods ultimately cause actions to take place, either updating the card state or making requests of the containing environment.

##Compilation Targets##

The main compilation target is JavaScript with the JavaScript runtime in [flasjs][3].

Currently, there is code which amounts to a proto-backend to compile directly to Java bytecodes to support native Android development.

There are plans to support generation of Swift code to generate native code for iOS.

##Architecture##

As a compiler, the overall architecture is a mapping from source files to binaries (JS, JVM and Swift)

Within that, there are three main phases or stages:

* Parsing, which consists of indentation blocking, individual statement parsing, and removing syntactic sugar
* Resolving, which consists of resolving scoped names to unique names and converting methods to functions
* Generating, which consists of: generating structures, converting functions to intermediate form, dependency analysis, typechecking, handling currying and generating bytecodes.

##Development Cycle##

After a number of different approaches, I think the correct approach to adding new runtime features is first to get a "normal" test case working with "normal" guide code in FLASJvm/test.runner and then to ensure that can be generated from src/test/resources/cards/test.runner/jvm.fl.

Within that, smaller chunks of code can be generated using the mockgen library (see eg GenTestsForStructs and src/test/resources/genstructs).

Individual features should then be tested by writing sample programs and their corresponding unit tests (.ut) which proves that programs work in the intended way.

[1]: http://zinikigareth.github.io/flasckfl/
[2]: http://cardstack.io/#architecture
[3]: https://github.com/zinikiGareth/flasjs
[4]: http://zinikigareth.github.io/flasjs/
