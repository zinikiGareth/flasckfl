if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._ControlledCard = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.ControlledCard';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
  this._contracts['test.golden.SetState'] = test.golden.ControlledCard._C0.apply(this);
  this._contracts['test.golden.Echo'] = test.golden.ControlledCard._C1.apply(this);
  this.e = this._contracts['test.golden.Echo'];
}

test.golden._ControlledCard.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  return msgs.curr;
}

test.golden.ControlledCard = function(v0) {
  "use strict";
  return new test.golden._ControlledCard(v0);
}

test.golden.ControlledCard.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.ControlledCard._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.SetState';
}

test.golden.ControlledCard._C0 = function() {
  "use strict";
  return new test.golden.ControlledCard.__C0(this);
}

test.golden.ControlledCard.__C1 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.ControlledCard._C1';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.Echo';
}

test.golden.ControlledCard._C1 = function() {
  "use strict";
  return new test.golden.ControlledCard.__C1(this);
}

test.golden._ControlledCard.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._ControlledCard.B1(new CardArea(parent, wrapper, this));
}

test.golden._ControlledCard.B1 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('hello');
  this._onAssign(this._card, 'sayHello', test.golden._ControlledCard.B1.prototype._setVariableFormats);
  test.golden._ControlledCard.B1.prototype._setVariableFormats.call(this);
  test.golden._ControlledCard.B1.prototype._add_handlers.call(this);
}

test.golden._ControlledCard.B1.prototype = new TextArea();

test.golden._ControlledCard.B1.prototype.constructor = test.golden._ControlledCard.B1;

test.golden._ControlledCard.B1.prototype._setVariableFormats = function() {
  this._mydiv.setAttribute('class', join(FLEval.full(this.formats_0()), ' '));
}

test.golden._ControlledCard.B1.prototype._add_handlers = function() {
  this._mydiv['onclick'] = function(event) {
    this._area._wrapper.dispatchEvent(this._area.handlers_1(), event);
  }
}

test.golden._ControlledCard.B1.prototype.formats_0 = function() {
  "use strict";
  var v2 = FLEval.oclosure(this._card, FLEval.curry, test.golden.ControlledCard.prototype.styleIf, 2);
  var v0 = FLEval.closure(v2, 'show', this._card.sayHello);
  return FLEval.closure(Cons, v0, Nil);
}

test.golden.ControlledCard.__C0.prototype.setOff = function() {
  "use strict";
  var v0 = FLEval.closure(Assign, this._card, 'sayHello', false);
  return FLEval.closure(Cons, v0, Nil);
}

test.golden.ControlledCard.__C0.prototype.setOn = function() {
  "use strict";
  var v0 = FLEval.closure(Assign, this._card, 'sayHello', true);
  return FLEval.closure(Cons, v0, Nil);
}

test.golden.ControlledCard.__C1.prototype.saySomething = function(v0) {
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
  return FLEval.error("test.golden.ControlledCard._C1.saySomething: case not handled");
}

test.golden.ControlledCard.prototype.echoHello = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, 'hello clicked', Nil);
  var v2 = FLEval.closure(Send, this.e, 'echoIt', v1);
  return FLEval.closure(Cons, v2, Nil);
}

test.golden.ControlledCard.handlers_1 = function() {
  "use strict";
  return test.golden.ControlledCard.prototype.echoHello;
}

test.golden.ControlledCard.prototype.styleIf = function(v0, v1) {
  "use strict";
  if (v1) {
    return v0;
  }
  return '';
}

test.golden;
