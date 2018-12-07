if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._TestCard = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.TestCard';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden._TestCard.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  this.data = FLEval.full(test.golden._TestCard.prototype.inits_data.apply(this, [msgs]));
  return msgs.curr;
}

test.golden.TestCard = function(v0) {
  "use strict";
  return new test.golden._TestCard(v0);
}

test.golden._TestCard.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._TestCard.B1(new CardArea(parent, wrapper, this));
}

test.golden._TestCard.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b2 = new test.golden._TestCard.B2(this);
  var b5 = new test.golden._TestCard.B5(this);
}

test.golden._TestCard.B1.prototype = new DivArea();

test.golden._TestCard.B1.prototype.constructor = test.golden._TestCard.B1;

test.golden._TestCard.B2 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b3 = new test.golden._TestCard.B3(this);
}

test.golden._TestCard.B2.prototype = new DivArea();

test.golden._TestCard.B2.prototype.constructor = test.golden._TestCard.B2;

test.golden._TestCard.B3 = function(parent) {
  DivArea.call(this, parent, 'a');
  if (!parent) return;
  var b4 = new test.golden._TestCard.B4(this);
  test.golden._TestCard.B3.prototype._add_handlers.call(this);
}

test.golden._TestCard.B3.prototype = new DivArea();

test.golden._TestCard.B3.prototype.constructor = test.golden._TestCard.B3;

test.golden._TestCard.B4 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('click here');
}

test.golden._TestCard.B4.prototype = new TextArea();

test.golden._TestCard.B4.prototype.constructor = test.golden._TestCard.B4;

test.golden._TestCard.B3.prototype._add_handlers = function() {
  this._mydiv['onclick'] = function(event) {
    this._area._wrapper.dispatchEvent(this._area.handlers_0(), event);
  }
}

test.golden._TestCard.B5 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b6 = new test.golden._TestCard.B6(this);
}

test.golden._TestCard.B5.prototype = new DivArea();

test.golden._TestCard.B5.prototype.constructor = test.golden._TestCard.B5;

test.golden._TestCard.B6 = function(parent) {
  DivArea.call(this, parent, 'svg', 'http://www.w3.org/2000/svg');
  if (!parent) return;
  test.golden._TestCard.B6.prototype._setAttr_1.call(this);
  test.golden._TestCard.B6.prototype._setAttr_2.call(this);
  var b7 = new test.golden._TestCard.B7(this);
}

test.golden._TestCard.B6.prototype = new DivArea();

test.golden._TestCard.B6.prototype.constructor = test.golden._TestCard.B6;

test.golden._TestCard.B6.prototype._setAttr_1 = function() {
  var attr = FLEval.full(this.teas_1());
  if (attr && !(attr instanceof FLError)) {
    this._mydiv.setAttribute('width', attr);
  }
}

test.golden._TestCard.B6.prototype._setAttr_2 = function() {
  var attr = FLEval.full(this.teas_2());
  if (attr && !(attr instanceof FLError)) {
    this._mydiv.setAttribute('height', attr);
  }
}

test.golden._TestCard.B7 = function(parent) {
  D3Area.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'data', D3Area.prototype._onUpdate);
}

test.golden._TestCard.B7.prototype = new D3Area();

test.golden._TestCard.B7.prototype.constructor = test.golden._TestCard.B7;

test.golden._TestCard.B6.prototype.teas_1 = function() {
  "use strict";
  return '400';
}

test.golden._TestCard.B6.prototype.teas_2 = function() {
  "use strict";
  return '250';
}

test.golden.TestCard.prototype._d3_chart_enter_rect = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, 'rect', Nil);
  var v2 = FLEval.closure(D3Action, 'append', v1);
  return FLEval.closure(Cons, v2, Nil);
}

test.golden.TestCard.prototype._d3_chart_enter_text = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, 'text', Nil);
  var v2 = FLEval.closure(D3Action, 'append', v1);
  return FLEval.closure(Cons, v2, Nil);
}

test.golden.TestCard.prototype._d3init_chart = function() {
  "use strict";
  var v0 = FLEval.closure(FLEval.tuple, 'y', test.golden.TestCard.prototype._gen_5);
  var v1 = FLEval.closure(FLEval.tuple, 'x', test.golden.TestCard.prototype._gen_4);
  var v2 = FLEval.closure(FLEval.tuple, 'width', test.golden.TestCard.prototype._gen_3);
  var v3 = FLEval.closure(FLEval.tuple, 'height', test.golden.TestCard.prototype._gen_2);
  var v4 = FLEval.closure(FLEval.tuple, 'fill', test.golden.TestCard.prototype._gen_1);
  var v5 = FLEval.closure(Cons, v4, Nil);
  var v6 = FLEval.closure(Cons, v3, v5);
  var v7 = FLEval.closure(Cons, v2, v6);
  var v8 = FLEval.closure(Cons, v1, v7);
  var v9 = FLEval.closure(Cons, v0, v8);
  var v10 = FLEval.closure(FLEval.tuple, 'rect', v9);
  var v11 = FLEval.closure(FLEval.tuple, 'y', test.golden.TestCard.prototype._gen_14);
  var v12 = FLEval.closure(FLEval.tuple, 'x', test.golden.TestCard.prototype._gen_13);
  var v13 = FLEval.closure(FLEval.tuple, 'width', test.golden.TestCard.prototype._gen_12);
  var v14 = FLEval.closure(FLEval.tuple, 'textAnchor', test.golden.TestCard.prototype._gen_11);
  var v15 = FLEval.closure(FLEval.tuple, 'text', test.golden.TestCard.prototype._gen_10);
  var v16 = FLEval.closure(FLEval.tuple, 'height', test.golden.TestCard.prototype._gen_9);
  var v17 = FLEval.closure(FLEval.tuple, 'fontSize', test.golden.TestCard.prototype._gen_8);
  var v18 = FLEval.closure(FLEval.tuple, 'fontFamily', test.golden.TestCard.prototype._gen_7);
  var v19 = FLEval.closure(FLEval.tuple, 'fill', test.golden.TestCard.prototype._gen_6);
  var v20 = FLEval.closure(Cons, v19, Nil);
  var v21 = FLEval.closure(Cons, v18, v20);
  var v22 = FLEval.closure(Cons, v17, v21);
  var v23 = FLEval.closure(Cons, v16, v22);
  var v24 = FLEval.closure(Cons, v15, v23);
  var v25 = FLEval.closure(Cons, v14, v24);
  var v26 = FLEval.closure(Cons, v13, v25);
  var v27 = FLEval.closure(Cons, v12, v26);
  var v28 = FLEval.closure(Cons, v11, v27);
  var v29 = FLEval.closure(FLEval.tuple, 'text', v28);
  var v30 = FLEval.closure(Cons, v29, Nil);
  var v31 = FLEval.closure(Cons, v10, v30);
  var v32 = FLEval.closure(Cons, test.golden.TestCard.prototype._d3_chart_enter_text, Nil);
  var v33 = FLEval.closure(Cons, test.golden.TestCard.prototype._d3_chart_enter_rect, v32);
  var v34 = FLEval.closure(Assoc, 'enter', v33, NilMap);
  var v35 = FLEval.closure(Assoc, 'layout', v31, v34);
  return FLEval.closure(Assoc, 'data', test.golden.TestCard.prototype._gen_15, v35);
}

test.golden.TestCard.prototype._gen_1 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return 'red';
  }
  return FLEval.error("test.golden.TestCard._gen_1: case not handled");
}

test.golden.TestCard.prototype._gen_10 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return FLEval.closure(FLEval.field, v0, 'data');
  }
  return FLEval.error("test.golden.TestCard._gen_10: case not handled");
}

test.golden.TestCard.prototype._gen_11 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return 'middle';
  }
  return FLEval.error("test.golden.TestCard._gen_11: case not handled");
}

test.golden.TestCard.prototype._gen_12 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return 20;
  }
  return FLEval.error("test.golden.TestCard._gen_12: case not handled");
}

test.golden.TestCard.prototype._gen_13 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'idx');
    var v2 = FLEval.closure(FLEval.mul, 22, v1);
    return FLEval.closure(FLEval.plus, v2, 10);
  }
  return FLEval.error("test.golden.TestCard._gen_13: case not handled");
}

test.golden.TestCard.prototype._gen_14 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'data');
    var v2 = FLEval.closure(FLEval.mul, 10, v1);
    var v3 = FLEval.closure(FLEval.minus, 200, v2);
    return FLEval.closure(FLEval.plus, v3, 14);
  }
  return FLEval.error("test.golden.TestCard._gen_14: case not handled");
}

test.golden.TestCard.prototype._gen_15 = function() {
  "use strict";
  return this.data;
}

test.golden.TestCard.prototype._gen_2 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'data');
    return FLEval.closure(FLEval.mul, 10, v1);
  }
  return FLEval.error("test.golden.TestCard._gen_2: case not handled");
}

test.golden.TestCard.prototype._gen_3 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return 20;
  }
  return FLEval.error("test.golden.TestCard._gen_3: case not handled");
}

test.golden.TestCard.prototype._gen_4 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'idx');
    return FLEval.closure(FLEval.mul, 22, v1);
  }
  return FLEval.error("test.golden.TestCard._gen_4: case not handled");
}

test.golden.TestCard.prototype._gen_5 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'data');
    var v2 = FLEval.closure(FLEval.mul, 10, v1);
    return FLEval.closure(FLEval.minus, 200, v2);
  }
  return FLEval.error("test.golden.TestCard._gen_5: case not handled");
}

test.golden.TestCard.prototype._gen_6 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return 'black';
  }
  return FLEval.error("test.golden.TestCard._gen_6: case not handled");
}

test.golden.TestCard.prototype._gen_7 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return 'sans-serif';
  }
  return FLEval.error("test.golden.TestCard._gen_7: case not handled");
}

test.golden.TestCard.prototype._gen_8 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    return '11px';
  }
  return FLEval.error("test.golden.TestCard._gen_8: case not handled");
}

test.golden.TestCard.prototype._gen_9 = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'D3Element')) {
    var v1 = FLEval.closure(FLEval.field, v0, 'data');
    return FLEval.closure(FLEval.mul, 10, v1);
  }
  return FLEval.error("test.golden.TestCard._gen_9: case not handled");
}

test.golden.TestCard.handlers_0 = function() {
  "use strict";
  return test.golden.TestCard.prototype.rejig;
}

test.golden._TestCard.prototype.inits_data = function(msgs) {
  "use strict";
  var v0 = FLEval.closure(Cons, 16, Nil);
  var v1 = FLEval.closure(Cons, 2, v0);
  var v2 = FLEval.closure(Cons, 8, v1);
  var ret = FLEval.closure(Cons, 5, v2);
  return ret;
}

test.golden._TestCard.prototype.rejig = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Cons, 9, Nil);
  var v2 = FLEval.closure(Cons, 11, v1);
  var v3 = FLEval.closure(Cons, 7, v2);
  var v4 = FLEval.closure(Cons, 4, v3);
  var v5 = FLEval.closure(Assign, this, 'data', v4);
  return FLEval.closure(Cons, v5, Nil);
}

test.golden;
