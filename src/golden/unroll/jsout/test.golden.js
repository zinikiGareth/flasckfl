if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Unroll = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Unroll';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden.Unroll = function(v0) {
  "use strict";
  return new test.golden._Unroll(v0);
}

test.golden._Unroll.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._Unroll.B1(new CardArea(parent, wrapper, this));
}

test.golden._Unroll.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b2 = new test.golden._Unroll.B2(this);
  var b4 = new test.golden._Unroll.B4(this);
}

test.golden._Unroll.B1.prototype = new DivArea();

test.golden._Unroll.B1.prototype.constructor = test.golden._Unroll.B1;

test.golden._Unroll.B2 = function(parent) {
  DivArea.call(this, parent, 'p');
  if (!parent) return;
  test.golden._Unroll.B2.prototype._setAttr_1.call(this);
  var b3 = new test.golden._Unroll.B3(this);
  this._mydiv.className = 'styled hello';
}

test.golden._Unroll.B2.prototype = new DivArea();

test.golden._Unroll.B2.prototype.constructor = test.golden._Unroll.B2;

test.golden._Unroll.B2.prototype._setAttr_1 = function() {
  var attr = FLEval.full(this._card.teas_0());
  if (attr && !(attr instanceof FLError)) {
    this._mydiv.setAttribute('title', attr);
  }
}

test.golden._Unroll.B3 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('hello');
}

test.golden._Unroll.B3.prototype = new TextArea();

test.golden._Unroll.B3.prototype.constructor = test.golden._Unroll.B3;

test.golden._Unroll.B4 = function(parent) {
  DivArea.call(this, parent, 'p');
  if (!parent) return;
  test.golden._Unroll.B4.prototype._setAttr_1.call(this);
  var b5 = new test.golden._Unroll.B5(this);
  this._mydiv.className = 'styled goodbye';
}

test.golden._Unroll.B4.prototype = new DivArea();

test.golden._Unroll.B4.prototype.constructor = test.golden._Unroll.B4;

test.golden._Unroll.B4.prototype._setAttr_1 = function() {
  var attr = FLEval.full(this._card.teas_1());
  if (attr && !(attr instanceof FLError)) {
    this._mydiv.setAttribute('title', attr);
  }
}

test.golden._Unroll.B5 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('goodbye');
}

test.golden._Unroll.B5.prototype = new TextArea();

test.golden._Unroll.B5.prototype.constructor = test.golden._Unroll.B5;

test.golden.Unroll.prototype.teas_0 = function() {
  "use strict";
  return 'hello';
}

test.golden.Unroll.prototype.teas_1 = function() {
  "use strict";
  return 'goodbye';
}

test.golden;
