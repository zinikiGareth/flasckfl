if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.runner === 'undefined') {
  test.runner = function() {
  }
}

test.runner.x = function() {
	return 420;
}

test.runner._TestCard = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.runner.TestCard';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
  this._contracts['test.runner.SetState'] = test.runner.TestCard._C0.apply(this);
  this._contracts['test.runner.Echo'] = test.runner.TestCard._C1.apply(this);
  this.e = this._contracts['test.runner.Echo'];
}

test.runner.TestCard = function(v0) {
  "use strict";
  return new test.runner._TestCard(v0);
}

test.runner._TestCard.prototype._onReady = function(v0) {
  "use strict";
  return null;
}

test.runner.TestCard.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.runner.TestCard._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.runner.SetState';
}

test.runner.TestCard._C0 = function() {
  "use strict";
  return new test.runner.TestCard.__C0(this);
}

test.runner.TestCard.__C1 = function(v0) {
  "use strict";
  this._ctor = 'test.runner.TestCard._C1';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.runner.Echo';
}

test.runner.TestCard._C1 = function() {
  "use strict";
  return new test.runner.TestCard.__C1(this);
}

test.runner._TestCard.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.runner._TestCard.B1(new CardArea(parent, wrapper, this));
}

test.runner._TestCard.B1 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('hello, world');
  this._onAssign(this._card, 'sayHello', test.runner._TestCard.B1.prototype._setVariableFormats);
  test.runner._TestCard.B1.prototype._setVariableFormats.call(this);
  // this is something of a hack to get the tests to pass
  var ff = function(event) {};
  this._mydiv.setAttribute('onclick', 'this._area._wrapper.dispatchEvent(test.runner._TestCard.B1.prototype.handlers_1.call(this), event)');
}

test.runner._TestCard.B1.prototype = new TextArea();

test.runner._TestCard.B1.prototype.constructor = test.runner._TestCard.B1;

test.runner._TestCard.B1.prototype._setVariableFormats = function() {
  this._mydiv.setAttribute('class', join(FLEval.full(this.formats_0()), ' '));
}

test.runner.TestCard.__C0.prototype.setOff = function() {
  "use strict";
  var v0 = FLEval.closure(Assign, this._card, 'sayHello', false);
  return FLEval.closure(Cons, v0, Nil);
}

test.runner.TestCard.__C0.prototype.setOn = function() {
  "use strict";
  var v0 = FLEval.closure(Assign, this._card, 'sayHello', true);
  return FLEval.closure(Cons, v0, Nil);
}

test.runner.TestCard.__C1.prototype.saySomething = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (typeof v0 === 'string') {
    var v1 = FLEval.closure(Cons, v0, Nil);
    var v2 = FLEval.closure(Send, this._card.e, 'echoIt', v1);
    return FLEval.closure(Cons, v2, Nil);
  }
  return FLEval.error("test.runner.TestCard._C1.saySomething: case not handled");
}

test.runner.TestCard.prototype.styleIf = function(v0, v1) {
  "use strict";
  if (v1) {
    return v0;
  }
  return '';
}

test.runner._TestCard.prototype.echoHello = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, 'hello clicked', Nil);
  var v2 = FLEval.closure(Send, this.e, 'echoIt', v1);
  return FLEval.closure(Cons, v2, Nil);
}

test.runner._TestCard.B1.prototype.formats_0 = function() {
  "use strict";
  var v2 = FLEval.oclosure(this._card, FLEval.curry, test.runner.TestCard.prototype.styleIf, 2);
  var v0 = FLEval.closure(v2, 'show', this._card.sayHello);
  return FLEval.closure(Cons, v0, Nil);
}

test.runner._TestCard.B1.prototype.handlers_1 = function() {
  return test.runner._TestCard.prototype.echoHello;
}