org = function() {
}

org.flasck = function() {
}

org.flasck._ChangeEvent = function(v0) {
  "use strict";
  this._ctor = 'org.flasck.ChangeEvent';
  if (v0) {
    if (v0.index) {
      this.index = v0.index;
    }
    if (v0.value) {
      this.value = v0.value;
    }
    if (v0.label) {
      this.label = v0.label;
    }
  }
  else {
  }
}

org.flasck.ChangeEvent = function(v0, v1, v2) {
  "use strict";
  return new org.flasck._ChangeEvent({index: v0, value: v1, label: v2});
}

FLEval.registerStruct('org.flasck.ChangeEvent', org.flasck.ChangeEvent, org.flasck._ChangeEvent);

org.flasck;
