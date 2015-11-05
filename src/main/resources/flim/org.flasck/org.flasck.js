if (typeof org === 'undefined') {
  org = function() {
  }
}

if (typeof org.flasck === 'undefined') {
  org.flasck = function() {
  }
}

org.flasck._ChangeEvent = function(v0) {
  "use strict";
  this._ctor = 'org.flasck.ChangeEvent';
  if (v0) {
    if (v0.index) {
      this.index = v0.index;
    }
    if (v0.id) {
      this.id = v0.id;
    }
    if (v0.value) {
      this.value = v0.value;
    }
  }
  else {
  }
}

org.flasck.ChangeEvent = function(v0, v1, v2) {
  "use strict";
  return new org.flasck._ChangeEvent({index: v0, id: v1, value: v2});
}

org.flasck._DropEvent = function(v0) {
  "use strict";
  this._ctor = 'org.flasck.DropEvent';
  if (v0) {
    if (v0.file) {
      this.file = v0.file;
    }
  }
  else {
  }
}

org.flasck.DropEvent = function(v0) {
  "use strict";
  return new org.flasck._DropEvent({file: v0});
}

org.flasck;
