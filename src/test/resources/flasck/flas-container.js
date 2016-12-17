/*
 * Copyright 2014 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Atmosphere.js
 * https://github.com/Atmosphere/atmosphere-javascript
 *
 * API reference
 * https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-API
 *
 * Highly inspired by
 * - Portal by Donghwan Kim http://flowersinthesand.github.io/portal/
 */
    var version = "2.2.3-javascript",
        atmosphere = {},
        guid,
        requests = [],
        callbacks = [],
        uuid = 0,
        hasOwn = Object.prototype.hasOwnProperty;

    atmosphere = {

        onError: function (response) {
        },
        onClose: function (response) {
        },
        onOpen: function (response) {
        },
        onReopen: function (response) {
        },
        onMessage: function (response) {
        },
        onReconnect: function (request, response) {
        },
        onMessagePublished: function (response) {
        },
        onTransportFailure: function (errorMessage, _request) {
        },
        onLocalMessage: function (response) {
        },
        onFailureToReconnect: function (request, response) {
        },
        onClientTimeout: function (request) {
        },

        /**
         * Creates an object based on an atmosphere subscription that exposes functions defined by the Websocket interface.
         *
         * @class WebsocketApiAdapter
         * @param {Object} request the request object to build the underlying subscription
         * @constructor
         */
        WebsocketApiAdapter: function (request) {
            var _socket, _adapter;

            /**
             * Overrides the onMessage callback in given request.
             *
             * @method onMessage
             * @param {Object} e the event object
             */
            request.onMessage = function (e) {
                _adapter.onmessage({data: e.responseBody});
            };

            /**
             * Overrides the onMessagePublished callback in given request.
             *
             * @method onMessagePublished
             * @param {Object} e the event object
             */
            request.onMessagePublished = function (e) {
                _adapter.onmessage({data: e.responseBody});
            };

            /**
             * Overrides the onOpen callback in given request to proxy the event to the adapter.
             *
             * @method onOpen
             * @param {Object} e the event object
             */
            request.onOpen = function (e) {
                _adapter.onopen(e);
            };

            _adapter = {
                close: function () {
                    _socket.close();
                },

                send: function (data) {
                    _socket.push(data);
                },

                onmessage: function (e) {
                },

                onopen: function (e) {
                },

                onclose: function (e) {
                },

                onerror: function (e) {

                }
            };
            _socket = new atmosphere.subscribe(request);

            return _adapter;
        },

        AtmosphereRequest: function (options) {

            /**
             * {Object} Request parameters.
             *
             * @private
             */
            var _request = {
                timeout: 300000,
                method: 'GET',
                headers: {},
                contentType: '',
                callback: null,
                url: '',
                data: '',
                suspend: true,
                maxRequest: -1,
                reconnect: true,
                maxStreamingLength: 10000000,
                lastIndex: 0,
                logLevel: 'info',
                requestCount: 0,
                fallbackMethod: 'GET',
                fallbackTransport: 'streaming',
                transport: 'long-polling',
                webSocketImpl: null,
                webSocketBinaryType: null,
                dispatchUrl: null,
                webSocketPathDelimiter: "@@",
                enableXDR: false,
                rewriteURL: false,
                attachHeadersAsQueryString: true,
                executeCallbackBeforeReconnect: false,
                readyState: 0,
                withCredentials: false,
                trackMessageLength: false,
                messageDelimiter: '|',
                connectTimeout: -1,
                reconnectInterval: 0,
                dropHeaders: true,
                uuid: 0,
                async: true,
                shared: false,
                readResponsesHeaders: false,
                maxReconnectOnClose: 0,
                enableProtocol: true,
                pollingInterval: 0,
                heartbeat: {
                    client: null,
                    server: null
                },
                ackInterval: 0,
                closeAsync: false,
                onError: function (response) {
                },
                onClose: function (response) {
                },
                onOpen: function (response) {
                },
                onMessage: function (response) {
                },
                onReopen: function (request, response) {
                },
                onReconnect: function (request, response) {
                },
                onMessagePublished: function (response) {
                },
                onTransportFailure: function (reason, request) {
                },
                onLocalMessage: function (request) {
                },
                onFailureToReconnect: function (request, response) {
                },
                onClientTimeout: function (request) {
                }
            };

            /**
             * {Object} Request's last response.
             *
             * @private
             */
            var _response = {
                status: 200,
                reasonPhrase: "OK",
                responseBody: '',
                messages: [],
                headers: [],
                state: "messageReceived",
                transport: "polling",
                error: null,
                request: null,
                partialMessage: "",
                errorHandled: false,
                closedByClientTimeout: false,
                ffTryingReconnect: false
            };

            /**
             * {websocket} Opened web socket.
             *
             * @private
             */
            var _websocket = null;

            /**
             * {SSE} Opened SSE.
             *
             * @private
             */
            var _sse = null;

            /**
             * {XMLHttpRequest, ActiveXObject} Opened ajax request (in case of http-streaming or long-polling)
             *
             * @private
             */
            var _activeRequest = null;

            /**
             * {Object} Object use for streaming with IE.
             *
             * @private
             */
            var _ieStream = null;

            /**
             * {Object} Object use for jsonp transport.
             *
             * @private
             */
            var _jqxhr = null;

            /**
             * {boolean} If request has been subscribed or not.
             *
             * @private
             */
            var _subscribed = true;

            /**
             * {number} Number of test reconnection.
             *
             * @private
             */
            var _requestCount = 0;

            /**
             * {boolean} If request is currently aborded.
             *
             * @private
             */
            var _abordingConnection = false;

            /**
             * A local "channel' of communication.
             *
             * @private
             */
            var _localSocketF = null;

            /**
             * The storage used.
             *
             * @private
             */
            var _storageService;

            /**
             * Local communication
             *
             * @private
             */
            var _localStorageService = null;

            /**
             * A Unique ID
             *
             * @private
             */
            var guid = atmosphere.util.now();

            /** Trace time */
            var _traceTimer;

            /** Key for connection sharing */
            var _sharingKey;

            // Automatic call to subscribe
            _subscribe(options);

            /**
             * Initialize atmosphere request object.
             *
             * @private
             */
            function _init() {
                _subscribed = true;
                _abordingConnection = false;
                _requestCount = 0;

                _websocket = null;
                _sse = null;
                _activeRequest = null;
                _ieStream = null;
            }

            /**
             * Re-initialize atmosphere object.
             *
             * @private
             */
            function _reinit() {
                _clearState();
                _init();
            }

            /**
             *
             * @private
             */
            function _verifyStreamingLength(ajaxRequest, rq) {
                // Wait to be sure we have the full message before closing.
                if (_response.partialMessage === "" && (rq.transport === 'streaming') && (ajaxRequest.responseText.length > rq.maxStreamingLength)) {
                    return true;
                }
                return false;
            }

            /**
             * Disconnect
             *
             * @private
             */
            function _disconnect() {
                if (_request.enableProtocol && !_request.firstMessage) {
                    var query = "X-Atmosphere-Transport=close&X-Atmosphere-tracking-id=" + _request.uuid;

                    atmosphere.util.each(_request.headers, function (name, value) {
                        var h = atmosphere.util.isFunction(value) ? value.call(this, _request, _request, _response) : value;
                        if (h != null) {
                            query += "&" + encodeURIComponent(name) + "=" + encodeURIComponent(h);
                        }
                    });

                    var url = _request.url.replace(/([?&])_=[^&]*/, query);
                    url = url + (url === _request.url ? (/\?/.test(_request.url) ? "&" : "?") + query : "");

                    var rq = {
                        connected: false
                    };
                    var closeR = new atmosphere.AtmosphereRequest(rq);
                    closeR.attachHeadersAsQueryString = false;
                    closeR.dropHeaders = true;
                    closeR.url = url;
                    closeR.contentType = "text/plain";
                    closeR.transport = 'polling';
                    closeR.method = 'GET';
                    closeR.data = '';
                    closeR.async = rq.closeAsync;
                    _pushOnClose("", closeR);
                }
            }

            /**
             * Close request.
             *
             * @private
             */
            function _close() {
                if (_request.logLevel === 'debug') {
                    atmosphere.util.debug("Closing");
                }
                _abordingConnection = true;
                if (_request.reconnectId) {
                    clearTimeout(_request.reconnectId);
                    delete _request.reconnectId;
                }

                if (_request.heartbeatTimer) {
                    clearTimeout(_request.heartbeatTimer);
                }

                _request.reconnect = false;
                _response.request = _request;
                _response.state = 'unsubscribe';
                _response.responseBody = "";
                _response.status = 408;
                _response.partialMessage = "";
                _invokeCallback();
                _disconnect();
                _clearState();
            }

            function _clearState() {
                _response.partialMessage = "";
                if (_request.id) {
                    clearTimeout(_request.id);
                }

                if (_request.heartbeatTimer) {
                    clearTimeout(_request.heartbeatTimer);
                }

                if (_ieStream != null) {
                    _ieStream.close();
                    _ieStream = null;
                }
                if (_jqxhr != null) {
                    _jqxhr.abort();
                    _jqxhr = null;
                }
                if (_activeRequest != null) {
                    _activeRequest.abort();
                    _activeRequest = null;
                }
                if (_websocket != null) {
                    if (_websocket.canSendMessage) {
                        _websocket.close();
                    }
                    _websocket = null;
                }
                if (_sse != null) {
                    _sse.close();
                    _sse = null;
                }
                _clearStorage();
            }

            function _clearStorage() {
                // Stop sharing a connection
                if (_storageService != null) {
                    // Clears trace timer
                    clearInterval(_traceTimer);
                    // Removes the trace
                    document.cookie = _sharingKey + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
                    // The heir is the parent unless unloading
                    _storageService.signal("close", {
                        reason: "",
                        heir: !_abordingConnection ? guid : (_storageService.get("children") || [])[0]
                    });
                    _storageService.close();
                }
                if (_localStorageService != null) {
                    _localStorageService.close();
                }
            }

            /**
             * Subscribe request using request transport. <br>
             * If request is currently opened, this one will be closed.
             *
             * @param {Object} Request parameters.
             * @private
             */
            function _subscribe(options) {
                _reinit();

                _request = atmosphere.util.extend(_request, options);
                // Allow at least 1 request
                _request.mrequest = _request.reconnect;
                if (!_request.reconnect) {
                    _request.reconnect = true;
                }
            }

            /**
             * Check if web socket is supported (check for custom implementation provided by request object or browser implementation).
             *
             * @returns {boolean} True if web socket is supported, false otherwise.
             * @private
             */
            function _supportWebsocket() {
                return _request.webSocketImpl != null || window.WebSocket || window.MozWebSocket;
            }

            /**
             * Check if server side events (SSE) is supported (check for custom implementation provided by request object or browser implementation).
             *
             * @returns {boolean} True if web socket is supported, false otherwise.
             * @private
             */
            function _supportSSE() {
                return window.EventSource;
            }

            /**
             * Open request using request transport. <br>
             * If request transport is 'websocket' but websocket can't be opened, request will automatically reconnect using fallback transport.
             *
             * @private
             */
            function _execute() {
                // Shared across multiple tabs/windows.
                if (_request.shared) {
                    _localStorageService = _local(_request);
                    if (_localStorageService != null) {
                        if (_request.logLevel === 'debug') {
                            atmosphere.util.debug("Storage service available. All communication will be local");
                        }

                        if (_localStorageService.open(_request)) {
                            // Local connection.
                            return;
                        }
                    }

                    if (_request.logLevel === 'debug') {
                        atmosphere.util.debug("No Storage service available.");
                    }
                    // Wasn't local or an error occurred
                    _localStorageService = null;
                }

                // Protocol
                _request.firstMessage = uuid == 0 ? true : false;
                _request.isOpen = false;
                _request.ctime = atmosphere.util.now();

                // We carry any UUID set by the user or from a previous connection.
                if (_request.uuid === 0) {
                    _request.uuid = uuid;
                }
                _response.closedByClientTimeout = false;

                if (_request.transport !== 'websocket' && _request.transport !== 'sse') {
                    _executeRequest(_request);

                } else if (_request.transport === 'websocket') {
                    if (!_supportWebsocket()) {
                        _reconnectWithFallbackTransport("Websocket is not supported, using request.fallbackTransport (" + _request.fallbackTransport
                            + ")");
                    } else {
                        _executeWebSocket(false);
                    }
                } else if (_request.transport === 'sse') {
                    if (!_supportSSE()) {
                        _reconnectWithFallbackTransport("Server Side Events(SSE) is not supported, using request.fallbackTransport ("
                            + _request.fallbackTransport + ")");
                    } else {
                        _executeSSE(false);
                    }
                }
            }

            function _local(request) {
                var trace, connector, orphan, name = "atmosphere-" + request.url, connectors = {
                    storage: function () {
                        function onstorage(event) {
                            if (event.key === name && event.newValue) {
                                listener(event.newValue);
                            }
                        }

                        if (!atmosphere.util.storage) {
                            return;
                        }

                        var storage = window.localStorage,
                            get = function (key) {
                                return atmosphere.util.parseJSON(storage.getItem(name + "-" + key));
                            },
                            set = function (key, value) {
                                storage.setItem(name + "-" + key, atmosphere.util.stringifyJSON(value));
                            };

                        return {
                            init: function () {
                                set("children", get("children").concat([guid]));
                                atmosphere.util.on(window, "storage", onstorage);
                                return get("opened");
                            },
                            signal: function (type, data) {
                                storage.setItem(name, atmosphere.util.stringifyJSON({
                                    target: "p",
                                    type: type,
                                    data: data
                                }));
                            },
                            close: function () {
                                var children = get("children");

                                atmosphere.util.off(window, "storage", onstorage);
                                if (children) {
                                    if (removeFromArray(children, request.id)) {
                                        set("children", children);
                                    }
                                }
                            }
                        };
                    },
                    windowref: function () {
                        var win = window.open("", name.replace(/\W/g, ""));

                        if (!win || win.closed || !win.callbacks) {
                            return;
                        }

                        return {
                            init: function () {
                                win.callbacks.push(listener);
                                win.children.push(guid);
                                return win.opened;
                            },
                            signal: function (type, data) {
                                if (!win.closed && win.fire) {
                                    win.fire(atmosphere.util.stringifyJSON({
                                        target: "p",
                                        type: type,
                                        data: data
                                    }));
                                }
                            },
                            close: function () {
                                // Removes traces only if the parent is alive
                                if (!orphan) {
                                    removeFromArray(win.callbacks, listener);
                                    removeFromArray(win.children, guid);
                                }
                            }

                        };
                    }
                };

                function removeFromArray(array, val) {
                    var i, length = array.length;

                    for (i = 0; i < length; i++) {
                        if (array[i] === val) {
                            array.splice(i, 1);
                        }
                    }

                    return length !== array.length;
                }

                // Receives open, close and message command from the parent
                function listener(string) {
                    var command = atmosphere.util.parseJSON(string), data = command.data;

                    if (command.target === "c") {
                        switch (command.type) {
                            case "open":
                                _open("opening", 'local', _request);
                                break;
                            case "close":
                                if (!orphan) {
                                    orphan = true;
                                    if (data.reason === "aborted") {
                                        _close();
                                    } else {
                                        // Gives the heir some time to reconnect
                                        if (data.heir === guid) {
                                            _execute();
                                        } else {
                                            setTimeout(function () {
                                                _execute();
                                            }, 100);
                                        }
                                    }
                                }
                                break;
                            case "message":
                                _prepareCallback(data, "messageReceived", 200, request.transport);
                                break;
                            case "localMessage":
                                _localMessage(data);
                                break;
                        }
                    }
                }

                function findTrace() {
                    var matcher = new RegExp("(?:^|; )(" + encodeURIComponent(name) + ")=([^;]*)").exec(document.cookie);
                    if (matcher) {
                        return atmosphere.util.parseJSON(decodeURIComponent(matcher[2]));
                    }
                }

                // Finds and validates the parent socket's trace from the cookie
                trace = findTrace();
                if (!trace || atmosphere.util.now() - trace.ts > 1000) {
                    return;
                }

                // Chooses a connector
                connector = connectors.storage() || connectors.windowref();
                if (!connector) {
                    return;
                }

                return {
                    open: function () {
                        var parentOpened;

                        // Checks the shared one is alive
                        _traceTimer = setInterval(function () {
                            var oldTrace = trace;
                            trace = findTrace();
                            if (!trace || oldTrace.ts === trace.ts) {
                                // Simulates a close signal
                                listener(atmosphere.util.stringifyJSON({
                                    target: "c",
                                    type: "close",
                                    data: {
                                        reason: "error",
                                        heir: oldTrace.heir
                                    }
                                }));
                            }
                        }, 1000);

                        parentOpened = connector.init();
                        if (parentOpened) {
                            // Firing the open event without delay robs the user of the opportunity to bind connecting event handlers
                            setTimeout(function () {
                                _open("opening", 'local', request);
                            }, 50);
                        }
                        return parentOpened;
                    },
                    send: function (event) {
                        connector.signal("send", event);
                    },
                    localSend: function (event) {
                        connector.signal("localSend", atmosphere.util.stringifyJSON({
                            id: guid,
                            event: event
                        }));
                    },
                    close: function () {
                        // Do not signal the parent if this method is executed by the unload event handler
                        if (!_abordingConnection) {
                            clearInterval(_traceTimer);
                            connector.signal("close");
                            connector.close();
                        }
                    }
                };
            }

            function share() {
                var storageService, name = "atmosphere-" + _request.url, servers = {
                    // Powered by the storage event and the localStorage
                    // http://www.w3.org/TR/webstorage/#event-storage
                    storage: function () {
                        function onstorage(event) {
                            // When a deletion, newValue initialized to null
                            if (event.key === name && event.newValue) {
                                listener(event.newValue);
                            }
                        }

                        if (!atmosphere.util.storage) {
                            return;
                        }

                        var storage = window.localStorage;

                        return {
                            init: function () {
                                // Handles the storage event
                                atmosphere.util.on(window, "storage", onstorage);
                            },
                            signal: function (type, data) {
                                storage.setItem(name, atmosphere.util.stringifyJSON({
                                    target: "c",
                                    type: type,
                                    data: data
                                }));
                            },
                            get: function (key) {
                                return atmosphere.util.parseJSON(storage.getItem(name + "-" + key));
                            },
                            set: function (key, value) {
                                storage.setItem(name + "-" + key, atmosphere.util.stringifyJSON(value));
                            },
                            close: function () {
                                atmosphere.util.off(window, "storage", onstorage);
                                storage.removeItem(name);
                                storage.removeItem(name + "-opened");
                                storage.removeItem(name + "-children");
                            }

                        };
                    },
                    // Powered by the window.open method
                    // https://developer.mozilla.org/en/DOM/window.open
                    windowref: function () {
                        // Internet Explorer raises an invalid argument error
                        // when calling the window.open method with the name containing non-word characters
                        var neim = name.replace(/\W/g, ""), container = document.getElementById(neim), win;

                        if (!container) {
                            container = document.createElement("div");
                            container.id = neim;
                            container.style.display = "none";
                            container.innerHTML = '<iframe name="' + neim + '" />';
                            document.body.appendChild(container);
                        }

                        win = container.firstChild.contentWindow;

                        return {
                            init: function () {
                                // Callbacks from different windows
                                win.callbacks = [listener];
                                // In IE 8 and less, only string argument can be safely passed to the function in other window
                                win.fire = function (string) {
                                    var i;

                                    for (i = 0; i < win.callbacks.length; i++) {
                                        win.callbacks[i](string);
                                    }
                                };
                            },
                            signal: function (type, data) {
                                if (!win.closed && win.fire) {
                                    win.fire(atmosphere.util.stringifyJSON({
                                        target: "c",
                                        type: type,
                                        data: data
                                    }));
                                }
                            },
                            get: function (key) {
                                return !win.closed ? win[key] : null;
                            },
                            set: function (key, value) {
                                if (!win.closed) {
                                    win[key] = value;
                                }
                            },
                            close: function () {
                            }
                        };
                    }
                };

                // Receives send and close command from the children
                function listener(string) {
                    var command = atmosphere.util.parseJSON(string), data = command.data;

                    if (command.target === "p") {
                        switch (command.type) {
                            case "send":
                                _push(data);
                                break;
                            case "localSend":
                                _localMessage(data);
                                break;
                            case "close":
                                _close();
                                break;
                        }
                    }
                }

                _localSocketF = function propagateMessageEvent(context) {
                    storageService.signal("message", context);
                };

                function leaveTrace() {
                    document.cookie = _sharingKey + "=" +
                        // Opera's JSON implementation ignores a number whose a last digit of 0 strangely
                        // but has no problem with a number whose a last digit of 9 + 1
                        encodeURIComponent(atmosphere.util.stringifyJSON({
                            ts: atmosphere.util.now() + 1,
                            heir: (storageService.get("children") || [])[0]
                        })) + "; path=/";
                }

                // Chooses a storageService
                storageService = servers.storage() || servers.windowref();
                storageService.init();

                if (_request.logLevel === 'debug') {
                    atmosphere.util.debug("Installed StorageService " + storageService);
                }

                // List of children sockets
                storageService.set("children", []);

                if (storageService.get("opened") != null && !storageService.get("opened")) {
                    // Flag indicating the parent socket is opened
                    storageService.set("opened", false);
                }
                // Leaves traces
                _sharingKey = encodeURIComponent(name);
                leaveTrace();
                _traceTimer = setInterval(leaveTrace, 1000);

                _storageService = storageService;
            }

            /**
             * @private
             */
            function _open(state, transport, request) {
                if (_request.shared && transport !== 'local') {
                    share();
                }

                if (_storageService != null) {
                    _storageService.set("opened", true);
                }

                request.close = function () {
                    _close();
                };

                if (_requestCount > 0 && state === 're-connecting') {
                    request.isReopen = true;
                    _tryingToReconnect(_response);
                } else if (_response.error == null) {
                    _response.request = request;
                    var prevState = _response.state;
                    _response.state = state;
                    var prevTransport = _response.transport;
                    _response.transport = transport;

                    var _body = _response.responseBody;
                    _invokeCallback();
                    _response.responseBody = _body;

                    _response.state = prevState;
                    _response.transport = prevTransport;
                }
            }

            /**
             * Execute request using jsonp transport.
             *
             * @param request {Object} request Request parameters, if undefined _request object will be used.
             * @private
             */
            function _jsonp(request) {
                // When CORS is enabled, make sure we force the proper transport.
                request.transport = "jsonp";

                var rq = _request, script;
                if ((request != null) && (typeof (request) !== 'undefined')) {
                    rq = request;
                }

                _jqxhr = {
                    open: function () {
                        var callback = "atmosphere" + (++guid);

                        function poll() {
                            var url = rq.url;
                            if (rq.dispatchUrl != null) {
                                url += rq.dispatchUrl;
                            }

                            var data = rq.data;
                            if (rq.attachHeadersAsQueryString) {
                                url = _attachHeaders(rq);
                                if (data !== '') {
                                    url += "&X-Atmosphere-Post-Body=" + encodeURIComponent(data);
                                }
                                data = '';
                            }

                            var head = document.head || document.getElementsByTagName("head")[0] || document.documentElement;

                            script = document.createElement("script");
                            script.src = url + "&jsonpTransport=" + callback;
                            script.clean = function () {
                                script.clean = script.onerror = script.onload = script.onreadystatechange = null;
                                if (script.parentNode) {
                                    script.parentNode.removeChild(script);
                                }
                            };
                            script.onload = script.onreadystatechange = function () {
                                if (!script.readyState || /loaded|complete/.test(script.readyState)) {
                                    script.clean();
                                }
                            };
                            script.onerror = function () {
                                script.clean();
                                rq.lastIndex = 0;

                                if (rq.openId) {
                                    clearTimeout(rq.openId);
                                }

                                if (rq.heartbeatTimer) {
                                    clearTimeout(rq.heartbeatTimer);
                                }

                                if (rq.reconnect && _requestCount++ < rq.maxReconnectOnClose) {
                                    _open('re-connecting', rq.transport, rq);
                                    _reconnect(_jqxhr, rq, request.reconnectInterval);
                                    rq.openId = setTimeout(function () {
                                        _triggerOpen(rq);
                                    }, rq.reconnectInterval + 1000);
                                } else {
                                    _onError(0, "maxReconnectOnClose reached");
                                }
                            };

                            head.insertBefore(script, head.firstChild);
                        }

                        // Attaches callback
                        window[callback] = function (msg) {
                            if (rq.reconnect) {
                                if (rq.maxRequest === -1 || rq.requestCount++ < rq.maxRequest) {
                                    // _readHeaders(_jqxhr, rq);

                                    if (!rq.executeCallbackBeforeReconnect) {
                                        _reconnect(_jqxhr, rq, rq.pollingInterval);
                                    }

                                    if (msg != null && typeof msg !== 'string') {
                                        try {
                                            msg = msg.message;
                                        } catch (err) {
                                            // The message was partial
                                        }
                                    }

                                    var skipCallbackInvocation = _trackMessageSize(msg, rq, _response);
                                    if (!skipCallbackInvocation) {
                                        _prepareCallback(_response.responseBody, "messageReceived", 200, rq.transport);
                                    }

                                    if (rq.executeCallbackBeforeReconnect) {
                                        _reconnect(_jqxhr, rq, rq.pollingInterval);
                                    }
                                } else {
                                    atmosphere.util.log(_request.logLevel, ["JSONP reconnect maximum try reached " + _request.requestCount]);
                                    _onError(0, "maxRequest reached");
                                }
                            }
                        };
                        setTimeout(function () {
                            poll();
                        }, 50);
                    },
                    abort: function () {
                        if (script && script.clean) {
                            script.clean();
                        }
                    }
                };

                _jqxhr.open();
            }

            /**
             * Build websocket object.
             *
             * @param location {string} Web socket url.
             * @returns {websocket} Web socket object.
             * @private
             */
            function _getWebSocket(location) {
                if (_request.webSocketImpl != null) {
                    return _request.webSocketImpl;
                } else {
                    if (window.WebSocket) {
                        return new WebSocket(location);
                    } else {
                        return new MozWebSocket(location);
                    }
                }
            }

            /**
             * Build web socket url from request url.
             *
             * @return {string} Web socket url (start with "ws" or "wss" for secure web socket).
             * @private
             */
            function _buildWebSocketUrl() {
                return _attachHeaders(_request, atmosphere.util.getAbsoluteURL(_request.webSocketUrl || _request.url)).replace(/^http/, "ws");
            }

            /**
             * Build SSE url from request url.
             *
             * @return a url with Atmosphere's headers
             * @private
             */
            function _buildSSEUrl() {
                var url = _attachHeaders(_request);
                return url;
            }

            /**
             * Open SSE. <br>
             * Automatically use fallback transport if SSE can't be opened.
             *
             * @private
             */
            function _executeSSE(sseOpened) {

                _response.transport = "sse";

                var location = _buildSSEUrl();

                if (_request.logLevel === 'debug') {
                    atmosphere.util.debug("Invoking executeSSE");
                    atmosphere.util.debug("Using URL: " + location);
                }

                if (sseOpened && !_request.reconnect) {
                    if (_sse != null) {
                        _clearState();
                    }
                    return;
                }

                try {
                    _sse = new EventSource(location, {
                        withCredentials: _request.withCredentials
                    });
                } catch (e) {
                    _onError(0, e);
                    _reconnectWithFallbackTransport("SSE failed. Downgrading to fallback transport and resending");
                    return;
                }

                if (_request.connectTimeout > 0) {
                    _request.id = setTimeout(function () {
                        if (!sseOpened) {
                            _clearState();
                        }
                    }, _request.connectTimeout);
                }

                _sse.onopen = function (event) {
                    _timeout(_request);
                    if (_request.logLevel === 'debug') {
                        atmosphere.util.debug("SSE successfully opened");
                    }

                    if (!_request.enableProtocol) {
                        if (!sseOpened) {
                            _open('opening', "sse", _request);
                        } else {
                            _open('re-opening', "sse", _request);
                        }
                    } else if (_request.isReopen) {
                        _request.isReopen = false;
                        _open('re-opening', _request.transport, _request);
                    }

                    sseOpened = true;

                    if (_request.method === 'POST') {
                        _response.state = "messageReceived";
                        _sse.send(_request.data);
                    }
                };

                _sse.onmessage = function (message) {
                    _timeout(_request);

                    if (!_request.enableXDR && message.origin && message.origin !== window.location.protocol + "//" + window.location.host) {
                        atmosphere.util.log(_request.logLevel, ["Origin was not " + window.location.protocol + "//" + window.location.host]);
                        return;
                    }

                    _response.state = 'messageReceived';
                    _response.status = 200;

                    message = message.data;
                    var skipCallbackInvocation = _trackMessageSize(message, _request, _response);

                    // https://github.com/remy/polyfills/blob/master/EventSource.js
                    // Since we polling.
                    /* if (_sse.URL) {
                     _sse.interval = 100;
                     _sse.URL = _buildSSEUrl();
                     } */

                    if (!skipCallbackInvocation) {
                        _invokeCallback();
                        _response.responseBody = '';
                        _response.messages = [];
                    }
                };

                _sse.onerror = function (message) {
                    clearTimeout(_request.id);

                    if (_request.heartbeatTimer) {
                        clearTimeout(_request.heartbeatTimer);
                    }

                    if (_response.closedByClientTimeout) return;

                    _invokeClose(sseOpened);
                    _clearState();

                    if (_abordingConnection) {
                        atmosphere.util.log(_request.logLevel, ["SSE closed normally"]);
                    } else if (!sseOpened) {
                        _reconnectWithFallbackTransport("SSE failed. Downgrading to fallback transport and resending");
                    } else if (_request.reconnect && (_response.transport === 'sse')) {
                        if (_requestCount++ < _request.maxReconnectOnClose) {
                            _open('re-connecting', _request.transport, _request);
                            if (_request.reconnectInterval > 0) {
                                _request.reconnectId = setTimeout(function () {
                                    _executeSSE(true);
                                }, _request.reconnectInterval);
                            } else {
                                _executeSSE(true);
                            }
                            _response.responseBody = "";
                            _response.messages = [];
                        } else {
                            atmosphere.util.log(_request.logLevel, ["SSE reconnect maximum try reached " + _requestCount]);
                            _onError(0, "maxReconnectOnClose reached");
                        }
                    }
                };
            }

            /**
             * Open web socket. <br>
             * Automatically use fallback transport if web socket can't be opened.
             *
             * @private
             */
            function _executeWebSocket(webSocketOpened) {

                _response.transport = "websocket";

                var location = _buildWebSocketUrl(_request.url);
                if (_request.logLevel === 'debug') {
                    atmosphere.util.debug("Invoking executeWebSocket");
                    atmosphere.util.debug("Using URL: " + location);
                }

                if (webSocketOpened && !_request.reconnect) {
                    if (_websocket != null) {
                        _clearState();
                    }
                    return;
                }

                _websocket = _getWebSocket(location);
                if (_request.webSocketBinaryType != null) {
                    _websocket.binaryType = _request.webSocketBinaryType;
                }

                if (_request.connectTimeout > 0) {
                    _request.id = setTimeout(function () {
                        if (!webSocketOpened) {
                            var _message = {
                                code: 1002,
                                reason: "",
                                wasClean: false
                            };
                            _websocket.onclose(_message);
                            // Close it anyway
                            try {
                                _clearState();
                            } catch (e) {
                            }
                            return;
                        }

                    }, _request.connectTimeout);
                }

                _websocket.onopen = function (message) {
                    _timeout(_request);

                    if (_request.logLevel === 'debug') {
                        atmosphere.util.debug("Websocket successfully opened");
                    }

                    var reopening = webSocketOpened;

                    if (_websocket != null) {
                        _websocket.canSendMessage = true;
                    }

                    if (!_request.enableProtocol) {
                        webSocketOpened = true;
                        if (reopening) {
                            _open('re-opening', "websocket", _request);
                        } else {
                            _open('opening', "websocket", _request);
                        }
                    }

                    if (_websocket != null) {
                        if (_request.method === 'POST') {
                            _response.state = "messageReceived";
                            _websocket.send(_request.data);
                        }
                    }
                };

                _websocket.onmessage = function (message) {
                    _timeout(_request);

                    // We only consider it opened if we get the handshake data
                    // https://github.com/Atmosphere/atmosphere-javascript/issues/74
                    if (_request.enableProtocol) {
                        webSocketOpened = true;
                    }

                    _response.state = 'messageReceived';
                    _response.status = 200;

                    message = message.data;
                    var isString = typeof (message) === 'string';
                    if (isString) {
                        var skipCallbackInvocation = _trackMessageSize(message, _request, _response);
                        if (!skipCallbackInvocation) {
                            _invokeCallback();
                            _response.responseBody = '';
                            _response.messages = [];
                        }
                    } else {
                        message = _handleProtocol(_request, message);
                        if (message === "")
                            return;

                        _response.responseBody = message;
                        _invokeCallback();
                        _response.responseBody = null;
                    }
                };

                _websocket.onerror = function (message) {
                    clearTimeout(_request.id);

                    if (_request.heartbeatTimer) {
                        clearTimeout(_request.heartbeatTimer);
                    }
                };

                _websocket.onclose = function (message) {
                    clearTimeout(_request.id);
                    if (_response.state === 'closed')
                        return;

                    var reason = message.reason;
                    if (reason === "") {
                        switch (message.code) {
                            case 1000:
                                reason = "Normal closure; the connection successfully completed whatever purpose for which " + "it was created.";
                                break;
                            case 1001:
                                reason = "The endpoint is going away, either because of a server failure or because the "
                                    + "browser is navigating away from the page that opened the connection.";
                                break;
                            case 1002:
                                reason = "The endpoint is terminating the connection due to a protocol error.";
                                break;
                            case 1003:
                                reason = "The connection is being terminated because the endpoint received data of a type it "
                                    + "cannot accept (for example, a text-only endpoint received binary data).";
                                break;
                            case 1004:
                                reason = "The endpoint is terminating the connection because a data frame was received that is too large.";
                                break;
                            case 1005:
                                reason = "Unknown: no status code was provided even though one was expected.";
                                break;
                            case 1006:
                                reason = "Connection was closed abnormally (that is, with no close frame being sent).";
                                break;
                        }
                    }

                    if (_request.logLevel === 'warn') {
                        atmosphere.util.warn("Websocket closed, reason: " + reason);
                        atmosphere.util.warn("Websocket closed, wasClean: " + message.wasClean);
                    }

                    if (_response.closedByClientTimeout) {
                        return;
                    }

                    _invokeClose(webSocketOpened);

                    _response.state = 'closed';

                    if (_abordingConnection) {
                        atmosphere.util.log(_request.logLevel, ["Websocket closed normally"]);
                    } else if (!webSocketOpened) {
                        _reconnectWithFallbackTransport("Websocket failed. Downgrading to Comet and resending");

                    } else if (_request.reconnect && _response.transport === 'websocket' && message.code !== 1001) {
                        _clearState();
                        if (_requestCount++ < _request.maxReconnectOnClose) {
                            _open('re-connecting', _request.transport, _request);
                            if (_request.reconnectInterval > 0) {
                                _request.reconnectId = setTimeout(function () {
                                    _response.responseBody = "";
                                    _response.messages = [];
                                    _executeWebSocket(true);
                                }, _request.reconnectInterval);
                            } else {
                                _response.responseBody = "";
                                _response.messages = [];
                                _executeWebSocket(true);
                            }
                        } else {
                            atmosphere.util.log(_request.logLevel, ["Websocket reconnect maximum try reached " + _request.maxReconnectOnClose]);
                            if (_request.logLevel === 'warn') {
                                atmosphere.util.warn("Websocket error, reason: " + message.reason);
                            }
                            _onError(0, "maxReconnectOnClose reached");
                        }
                    }
                };

                var ua = navigator.userAgent.toLowerCase();
                var isAndroid = ua.indexOf("android") > -1;
                if (isAndroid && _websocket.url === undefined) {
                    // Android 4.1 does not really support websockets and fails silently
                    _websocket.onclose({
                        reason: "Android 4.1 does not support websockets.",
                        wasClean: false
                    });
                }
            }

            function _handleProtocol(request, message) {

                var nMessage = message;
                if (request.transport === 'polling') return nMessage;

                if (atmosphere.util.trim(message).length !== 0 && request.enableProtocol && request.firstMessage) {
                    var pos = request.trackMessageLength ? 1 : 0;
                    var messages = message.split(request.messageDelimiter);

                    if (messages.length <= pos + 1) {
                        // Something went wrong, normally with IE or when a message is written before the
                        // handshake has been received.
                        return nMessage;
                    }

                    request.firstMessage = false;
                    request.uuid = atmosphere.util.trim(messages[pos]);

                    if (messages.length <= pos + 2) {
                        atmosphere.util.log('error', ["Protocol data not sent by the server. " +
                            "If you enable protocol on client side, be sure to install JavascriptProtocol interceptor on server side." +
                            "Also note that atmosphere-runtime 2.2+ should be used."]);
                    }

                    var interval = parseInt(atmosphere.util.trim(messages[pos + 1]), 10);
                    var paddingData = messages[pos + 2];

                    if (!isNaN(interval) && interval > 0) {
                        var _pushHeartbeat = function () {
                            _push(paddingData);
                            request.heartbeatTimer = setTimeout(_pushHeartbeat, interval);
                        };
                        request.heartbeatTimer = setTimeout(_pushHeartbeat, interval);
                    }

                    if (request.transport !== 'long-polling') {
                        _triggerOpen(request);
                    }
                    uuid = request.uuid;
                    nMessage = "";

                    // We have trailing messages
                    pos = request.trackMessageLength ? 4 : 3;
                    if (messages.length > pos + 1) {
                        for (var i = pos; i < messages.length; i++) {
                            nMessage += messages[i];
                            if (i + 1 !== messages.length) {
                                nMessage += request.messageDelimiter;
                            }
                        }
                    }

                    if (request.ackInterval !== 0) {
                        setTimeout(function () {
                            _push("...ACK...");
                        }, request.ackInterval);
                    }
                } else if (request.enableProtocol && request.firstMessage && atmosphere.util.browser.msie && +atmosphere.util.browser.version.split(".")[0] < 10) {
                    // In case we are getting some junk from IE
                    atmosphere.util.log(_request.logLevel, ["Receiving unexpected data from IE"]);
                } else {
                    _triggerOpen(request);
                }
                return nMessage;
            }

            function _timeout(_request) {
                clearTimeout(_request.id);
                if (_request.timeout > 0 && _request.transport !== 'polling') {
                    _request.id = setTimeout(function () {
                        _onClientTimeout(_request);
                        _disconnect();
                        _clearState();
                    }, _request.timeout);
                }
            }

            function _onClientTimeout(_request) {
                _response.closedByClientTimeout = true;
                _response.state = 'closedByClient';
                _response.responseBody = "";
                _response.status = 408;
                _response.messages = [];
                _invokeCallback();
            }

            function _onError(code, reason) {
                _clearState();
                clearTimeout(_request.id);
                _response.state = 'error';
                _response.reasonPhrase = reason;
                _response.responseBody = "";
                _response.status = code;
                _response.messages = [];
                _invokeCallback();
            }

            /**
             * Track received message and make sure callbacks/functions are only invoked when the complete message has been received.
             *
             * @param message
             * @param request
             * @param response
             */
            function _trackMessageSize(message, request, response) {
                message = _handleProtocol(request, message);
                if (message.length === 0)
                    return true;

                response.responseBody = message;

                if (request.trackMessageLength) {
                    // prepend partialMessage if any
                    message = response.partialMessage + message;

                    var messages = [];
                    var messageStart = message.indexOf(request.messageDelimiter);
                    while (messageStart !== -1) {
                        var str = message.substring(0, messageStart);
                        var messageLength = +str;
                        if (isNaN(messageLength))
                            throw new Error('message length "' + str + '" is not a number');
                        messageStart += request.messageDelimiter.length;
                        if (messageStart + messageLength > message.length) {
                            // message not complete, so there is no trailing messageDelimiter
                            messageStart = -1;
                        } else {
                            // message complete, so add it
                            messages.push(message.substring(messageStart, messageStart + messageLength));
                            // remove consumed characters
                            message = message.substring(messageStart + messageLength, message.length);
                            messageStart = message.indexOf(request.messageDelimiter);
                        }
                    }

                    /* keep any remaining data */
                    response.partialMessage = message;

                    if (messages.length !== 0) {
                        response.responseBody = messages.join(request.messageDelimiter);
                        response.messages = messages;
                        return false;
                    } else {
                        response.responseBody = "";
                        response.messages = [];
                        return true;
                    }
                } else {
                    response.responseBody = message;
                }
                return false;
            }

            /**
             * Reconnect request with fallback transport. <br>
             * Used in case websocket can't be opened.
             *
             * @private
             */
            function _reconnectWithFallbackTransport(errorMessage) {
                atmosphere.util.log(_request.logLevel, [errorMessage]);

                if (typeof (_request.onTransportFailure) !== 'undefined') {
                    _request.onTransportFailure(errorMessage, _request);
                } else if (typeof (atmosphere.util.onTransportFailure) !== 'undefined') {
                    atmosphere.util.onTransportFailure(errorMessage, _request);
                }

                _request.transport = _request.fallbackTransport;
                var reconnectInterval = _request.connectTimeout === -1 ? 0 : _request.connectTimeout;
                if (_request.reconnect && _request.transport !== 'none' || _request.transport == null) {
                    _request.method = _request.fallbackMethod;
                    _response.transport = _request.fallbackTransport;
                    _request.fallbackTransport = 'none';
                    if (reconnectInterval > 0) {
                        _request.reconnectId = setTimeout(function () {
                            _execute();
                        }, reconnectInterval);
                    } else {
                        _execute();
                    }
                } else {
                    _onError(500, "Unable to reconnect with fallback transport");
                }
            }

            /**
             * Get url from request and attach headers to it.
             *
             * @param request {Object} request Request parameters, if undefined _request object will be used.
             *
             * @returns {Object} Request object, if undefined, _request object will be used.
             * @private
             */
            function _attachHeaders(request, url) {
                var rq = _request;
                if ((request != null) && (typeof (request) !== 'undefined')) {
                    rq = request;
                }

                if (url == null) {
                    url = rq.url;
                }

                // If not enabled
                if (!rq.attachHeadersAsQueryString)
                    return url;

                // If already added
                if (url.indexOf("X-Atmosphere-Framework") !== -1) {
                    return url;
                }

                url += (url.indexOf('?') !== -1) ? '&' : '?';
                url += "X-Atmosphere-tracking-id=" + rq.uuid;
                url += "&X-Atmosphere-Framework=" + version;
                url += "&X-Atmosphere-Transport=" + rq.transport;

                if (rq.trackMessageLength) {
                    url += "&X-Atmosphere-TrackMessageSize=" + "true";
                }

                if (rq.heartbeat !== null && rq.heartbeat.server !== null) {
                    url += "&X-Heartbeat-Server=" + rq.heartbeat.server;
                }

                if (rq.contentType !== '') {
                    //Eurk!
                    url += "&Content-Type=" + (rq.transport === 'websocket' ? rq.contentType : encodeURIComponent(rq.contentType));
                }

                if (rq.enableProtocol) {
                    url += "&X-atmo-protocol=true";
                }

                atmosphere.util.each(rq.headers, function (name, value) {
                    var h = atmosphere.util.isFunction(value) ? value.call(this, rq, request, _response) : value;
                    if (h != null) {
                        url += "&" + encodeURIComponent(name) + "=" + encodeURIComponent(h);
                    }
                });

                return url;
            }

            function _triggerOpen(rq) {
                if (!rq.isOpen) {
                    rq.isOpen = true;
                    _open('opening', rq.transport, rq);
                } else if (rq.isReopen) {
                    rq.isReopen = false;
                    _open('re-opening', rq.transport, rq);
                }
            }

            /**
             * Execute ajax request. <br>
             *
             * @param request {Object} request Request parameters, if undefined _request object will be used.
             * @private
             */
            function _executeRequest(request) {
                var rq = _request;
                if ((request != null) || (typeof (request) !== 'undefined')) {
                    rq = request;
                }

                rq.lastIndex = 0;
                rq.readyState = 0;

                // CORS fake using JSONP
                if ((rq.transport === 'jsonp') || ((rq.enableXDR) && (atmosphere.util.checkCORSSupport()))) {
                    _jsonp(rq);
                    return;
                }

                if (atmosphere.util.browser.msie && +atmosphere.util.browser.version.split(".")[0] < 10) {
                    if ((rq.transport === 'streaming')) {
                        if (rq.enableXDR && window.XDomainRequest) {
                            _ieXDR(rq);
                        } else {
                            _ieStreaming(rq);
                        }
                        return;
                    }

                    if ((rq.enableXDR) && (window.XDomainRequest)) {
                        _ieXDR(rq);
                        return;
                    }
                }

                var reconnectF = function () {
                    rq.lastIndex = 0;
                    if (rq.reconnect && _requestCount++ < rq.maxReconnectOnClose) {
                        _response.ffTryingReconnect = true;
                        _open('re-connecting', request.transport, request);
                        _reconnect(ajaxRequest, rq, request.reconnectInterval);
                    } else {
                        _onError(0, "maxReconnectOnClose reached");
                    }
                };

                var disconnected = function () {
                    // Prevent onerror callback to be called
                    _response.errorHandled = true;
                    _clearState();
                    reconnectF();
                };

                if (rq.force || (rq.reconnect && (rq.maxRequest === -1 || rq.requestCount++ < rq.maxRequest))) {
                    rq.force = false;

                    var ajaxRequest = atmosphere.util.xhr();
                    ajaxRequest.hasData = false;

                    _doRequest(ajaxRequest, rq, true);

                    if (rq.suspend) {
                        _activeRequest = ajaxRequest;
                    }

                    if (rq.transport !== 'polling') {
                        _response.transport = rq.transport;

                        ajaxRequest.onabort = function () {
                            _invokeClose(true);
                        };

                        ajaxRequest.onerror = function () {
                            _response.error = true;
                            _response.ffTryingReconnect = true;
                            try {
                                _response.status = XMLHttpRequest.status;
                            } catch (e) {
                                _response.status = 500;
                            }

                            if (!_response.status) {
                                _response.status = 500;
                            }
                            if (!_response.errorHandled) {
                                _clearState();
                                reconnectF();
                            }
                        };
                    }

                    ajaxRequest.onreadystatechange = function () {
                        if (_abordingConnection) {
                            return;
                        }

                        _response.error = null;
                        var skipCallbackInvocation = false;
                        var update = false;

                        if (rq.transport === 'streaming' && rq.readyState > 2 && ajaxRequest.readyState === 4) {
                            _clearState();
                            reconnectF();
                            return;
                        }

                        rq.readyState = ajaxRequest.readyState;

                        if (rq.transport === 'streaming' && ajaxRequest.readyState >= 3) {
                            update = true;
                        } else if (rq.transport === 'long-polling' && ajaxRequest.readyState === 4) {
                            update = true;
                        }
                        _timeout(_request);

                        if (rq.transport !== 'polling') {
                            // MSIE 9 and lower status can be higher than 1000, Chrome can be 0
                            var status = 200;
                            if (ajaxRequest.readyState === 4) {
                                status = ajaxRequest.status > 1000 ? 0 : ajaxRequest.status;
                            }

                            if (status >= 300 || status === 0) {
                                disconnected();
                                return;
                            }

                            if ((!rq.enableProtocol || !request.firstMessage) && ajaxRequest.readyState === 2) {
                                // Firefox incorrectly send statechange 0->2 when a reconnect attempt fails. The above checks ensure that onopen is not called for these
                                // In that case, ajaxRequest.onerror will be called just after onreadystatechange is called, so we delay the trigger untill we are
                                // garantee the connection is well established.
                                if (atmosphere.util.browser.mozilla && _response.ffTryingReconnect) {
                                    _response.ffTryingReconnect = false;
                                    setTimeout(function () {
                                        if (!_response.ffTryingReconnect) {
                                            _triggerOpen(rq);
                                        }
                                    }, 500);
                                } else {
                                    _triggerOpen(rq);
                                }
                            }

                        } else if (ajaxRequest.readyState === 4) {
                            update = true;
                        }

                        if (update) {
                            var responseText = ajaxRequest.responseText;
                            _response.errorHandled = false;

                            // IE behave the same way when resuming long-polling or when the server goes down.
                            if (atmosphere.util.trim(responseText).length === 0 && rq.transport === 'long-polling') {
                                // For browser that aren't support onabort
                                if (!ajaxRequest.hasData) {
                                    disconnected();
                                } else {
                                    ajaxRequest.hasData = false;
                                }
                                return;
                            }
                            ajaxRequest.hasData = true;

                            _readHeaders(ajaxRequest, _request);

                            if (rq.transport === 'streaming') {
                                if (!atmosphere.util.browser.opera) {
                                    var message = responseText.substring(rq.lastIndex, responseText.length);
                                    skipCallbackInvocation = _trackMessageSize(message, rq, _response);

                                    rq.lastIndex = responseText.length;
                                    if (skipCallbackInvocation) {
                                        return;
                                    }
                                } else {
                                    atmosphere.util.iterate(function () {
                                        if (_response.status !== 500 && ajaxRequest.responseText.length > rq.lastIndex) {
                                            try {
                                                _response.status = ajaxRequest.status;
                                                _response.headers = atmosphere.util.parseHeaders(ajaxRequest.getAllResponseHeaders());

                                                _readHeaders(ajaxRequest, _request);

                                            } catch (e) {
                                                _response.status = 404;
                                            }
                                            _timeout(_request);

                                            _response.state = "messageReceived";
                                            var message = ajaxRequest.responseText.substring(rq.lastIndex);
                                            rq.lastIndex = ajaxRequest.responseText.length;

                                            skipCallbackInvocation = _trackMessageSize(message, rq, _response);
                                            if (!skipCallbackInvocation) {
                                                _invokeCallback();
                                            }

                                            if (_verifyStreamingLength(ajaxRequest, rq)) {
                                                _reconnectOnMaxStreamingLength(ajaxRequest, rq);
                                                return;
                                            }
                                        } else if (_response.status > 400) {
                                            // Prevent replaying the last message.
                                            rq.lastIndex = ajaxRequest.responseText.length;
                                            return false;
                                        }
                                    }, 0);
                                }
                            } else {
                                skipCallbackInvocation = _trackMessageSize(responseText, rq, _response);
                            }
                            var closeStream = _verifyStreamingLength(ajaxRequest, rq);

                            try {
                                _response.status = ajaxRequest.status;
                                _response.headers = atmosphere.util.parseHeaders(ajaxRequest.getAllResponseHeaders());

                                _readHeaders(ajaxRequest, rq);
                            } catch (e) {
                                _response.status = 404;
                            }

                            if (rq.suspend) {
                                _response.state = _response.status === 0 ? "closed" : "messageReceived";
                            } else {
                                _response.state = "messagePublished";
                            }

                            var isAllowedToReconnect = !closeStream && request.transport !== 'streaming' && request.transport !== 'polling';
                            if (isAllowedToReconnect && !rq.executeCallbackBeforeReconnect) {
                                _reconnect(ajaxRequest, rq, rq.pollingInterval);
                            }

                            if (_response.responseBody.length !== 0 && !skipCallbackInvocation)
                                _invokeCallback();

                            if (isAllowedToReconnect && rq.executeCallbackBeforeReconnect) {
                                _reconnect(ajaxRequest, rq, rq.pollingInterval);
                            }

                            if (closeStream) {
                                _reconnectOnMaxStreamingLength(ajaxRequest, rq);
                            }
                        }
                    };

                    try {
                        ajaxRequest.send(rq.data);
                        _subscribed = true;
                    } catch (e) {
                        atmosphere.util.log(rq.logLevel, ["Unable to connect to " + rq.url]);
                        _onError(0, e);
                    }

                } else {
                    if (rq.logLevel === 'debug') {
                        atmosphere.util.log(rq.logLevel, ["Max re-connection reached."]);
                    }
                    _onError(0, "maxRequest reached");
                }
            }

            function _reconnectOnMaxStreamingLength(ajaxRequest, rq) {
                _close();
                _abordingConnection = false;
                _reconnect(ajaxRequest, rq, 500);
            }

            /**
             * Do ajax request.
             *
             * @param ajaxRequest Ajax request.
             * @param request Request parameters.
             * @param create If ajax request has to be open.
             */
            function _doRequest(ajaxRequest, request, create) {
                // Prevent Android to cache request
                var url = request.url;
                if (request.dispatchUrl != null && request.method === 'POST') {
                    url += request.dispatchUrl;
                }
                url = _attachHeaders(request, url);
                url = atmosphere.util.prepareURL(url);

                if (create) {
                    ajaxRequest.open(request.method, url, request.async);
                    if (request.connectTimeout > 0) {
                        request.id = setTimeout(function () {
                            if (request.requestCount === 0) {
                                _clearState();
                                _prepareCallback("Connect timeout", "closed", 200, request.transport);
                            }
                        }, request.connectTimeout);
                    }
                }

                if (_request.withCredentials && _request.transport !== 'websocket') {
                    if ("withCredentials" in ajaxRequest) {
                        ajaxRequest.withCredentials = true;
                    }
                }

                if (!_request.dropHeaders) {
                    ajaxRequest.setRequestHeader("X-Atmosphere-Framework", atmosphere.util.version);
                    ajaxRequest.setRequestHeader("X-Atmosphere-Transport", request.transport);

                    if (ajaxRequest.heartbeat !== null && ajaxRequest.heartbeat.server !== null) {
                        ajaxRequest.setRequestHeader("X-Heartbeat-Server", ajaxRequest.heartbeat.server);
                    }

                    if (request.trackMessageLength) {
                        ajaxRequest.setRequestHeader("X-Atmosphere-TrackMessageSize", "true");
                    }
                    ajaxRequest.setRequestHeader("X-Atmosphere-tracking-id", request.uuid);

                    atmosphere.util.each(request.headers, function (name, value) {
                        var h = atmosphere.util.isFunction(value) ? value.call(this, ajaxRequest, request, create, _response) : value;
                        if (h != null) {
                            ajaxRequest.setRequestHeader(name, h);
                        }
                    });
                }

                if (request.contentType !== '') {
                    ajaxRequest.setRequestHeader("Content-Type", request.contentType);
                }
            }

            function _reconnect(ajaxRequest, request, reconnectInterval) {
                if (request.reconnect || (request.suspend && _subscribed)) {
                    var status = 0;
                    if (ajaxRequest && ajaxRequest.readyState > 1) {
                        status = ajaxRequest.status > 1000 ? 0 : ajaxRequest.status;
                    }
                    _response.status = status === 0 ? 204 : status;
                    _response.reason = status === 0 ? "Server resumed the connection or down." : "OK";

                    clearTimeout(request.id);
                    if (request.reconnectId) {
                        clearTimeout(request.reconnectId);
                        delete request.reconnectId;
                    }

                    if (reconnectInterval > 0) {
                        // For whatever reason, never cancel a reconnect timeout as it is mandatory to reconnect.
                        _request.reconnectId = setTimeout(function () {
                            _executeRequest(request);
                        }, reconnectInterval);
                    } else {
                        _executeRequest(request);
                    }
                }
            }

            function _tryingToReconnect(response) {
                response.state = 're-connecting';
                _invokeFunction(response);
            }

            function _ieXDR(request) {
                if (request.transport !== "polling") {
                    _ieStream = _configureXDR(request);
                    _ieStream.open();
                } else {
                    _configureXDR(request).open();
                }
            }

            function _configureXDR(request) {
                var rq = _request;
                if ((request != null) && (typeof (request) !== 'undefined')) {
                    rq = request;
                }

                var transport = rq.transport;
                var lastIndex = 0;
                var xdr = new window.XDomainRequest();

                var reconnect = function () {
                    if (rq.transport === "long-polling" && (rq.reconnect && (rq.maxRequest === -1 || rq.requestCount++ < rq.maxRequest))) {
                        xdr.status = 200;
                        _ieXDR(rq);
                    }
                };

                var rewriteURL = rq.rewriteURL || function (url) {
                    // Maintaining session by rewriting URL
                    // http://stackoverflow.com/questions/6453779/maintaining-session-by-rewriting-url
                    var match = /(?:^|;\s*)(JSESSIONID|PHPSESSID)=([^;]*)/.exec(document.cookie);

                    switch (match && match[1]) {
                        case "JSESSIONID":
                            return url.replace(/;jsessionid=[^\?]*|(\?)|$/, ";jsessionid=" + match[2] + "$1");
                        case "PHPSESSID":
                            return url.replace(/\?PHPSESSID=[^&]*&?|\?|$/, "?PHPSESSID=" + match[2] + "&").replace(/&$/, "");
                    }
                    return url;
                };

                // Handles open and message event
                xdr.onprogress = function () {
                    handle(xdr);
                };
                // Handles error event
                xdr.onerror = function () {
                    // If the server doesn't send anything back to XDR will fail with polling
                    if (rq.transport !== 'polling') {
                        _clearState();
                        if (_requestCount++ < rq.maxReconnectOnClose) {
                            if (rq.reconnectInterval > 0) {
                                rq.reconnectId = setTimeout(function () {
                                    _open('re-connecting', request.transport, request);
                                    _ieXDR(rq);
                                }, rq.reconnectInterval);
                            } else {
                                _open('re-connecting', request.transport, request);
                                _ieXDR(rq);
                            }
                        } else {
                            _onError(0, "maxReconnectOnClose reached");
                        }
                    }
                };

                // Handles close event
                xdr.onload = function () {
                };

                var handle = function (xdr) {
                    clearTimeout(rq.id);
                    var message = xdr.responseText;

                    message = message.substring(lastIndex);
                    lastIndex += message.length;

                    if (transport !== 'polling') {
                        _timeout(rq);

                        var skipCallbackInvocation = _trackMessageSize(message, rq, _response);

                        if (transport === 'long-polling' && atmosphere.util.trim(message).length === 0)
                            return;

                        if (rq.executeCallbackBeforeReconnect) {
                            reconnect();
                        }

                        if (!skipCallbackInvocation) {
                            _prepareCallback(_response.responseBody, "messageReceived", 200, transport);
                        }

                        if (!rq.executeCallbackBeforeReconnect) {
                            reconnect();
                        }
                    }
                };

                return {
                    open: function () {
                        var url = rq.url;
                        if (rq.dispatchUrl != null) {
                            url += rq.dispatchUrl;
                        }
                        url = _attachHeaders(rq, url);
                        xdr.open(rq.method, rewriteURL(url));
                        if (rq.method === 'GET') {
                            xdr.send();
                        } else {
                            xdr.send(rq.data);
                        }

                        if (rq.connectTimeout > 0) {
                            rq.id = setTimeout(function () {
                                if (rq.requestCount === 0) {
                                    _clearState();
                                    _prepareCallback("Connect timeout", "closed", 200, rq.transport);
                                }
                            }, rq.connectTimeout);
                        }
                    },
                    close: function () {
                        xdr.abort();
                    }
                };
            }

            function _ieStreaming(request) {
                _ieStream = _configureIE(request);
                _ieStream.open();
            }

            function _configureIE(request) {
                var rq = _request;
                if ((request != null) && (typeof (request) !== 'undefined')) {
                    rq = request;
                }

                var stop;
                var doc = new window.ActiveXObject("htmlfile");

                doc.open();
                doc.close();

                var url = rq.url;
                if (rq.dispatchUrl != null) {
                    url += rq.dispatchUrl;
                }

                if (rq.transport !== 'polling') {
                    _response.transport = rq.transport;
                }

                return {
                    open: function () {
                        var iframe = doc.createElement("iframe");

                        url = _attachHeaders(rq);
                        if (rq.data !== '') {
                            url += "&X-Atmosphere-Post-Body=" + encodeURIComponent(rq.data);
                        }

                        // Finally attach a timestamp to prevent Android and IE caching.
                        url = atmosphere.util.prepareURL(url);

                        iframe.src = url;
                        doc.body.appendChild(iframe);

                        // For the server to respond in a consistent format regardless of user agent, we polls response text
                        var cdoc = iframe.contentDocument || iframe.contentWindow.document;

                        stop = atmosphere.util.iterate(function () {
                            try {
                                if (!cdoc.firstChild) {
                                    return;
                                }

                                var res = cdoc.body ? cdoc.body.lastChild : cdoc;
                                var readResponse = function () {
                                    // Clones the element not to disturb the original one
                                    var clone = res.cloneNode(true);

                                    // If the last character is a carriage return or a line feed, IE ignores it in the innerText property
                                    // therefore, we add another non-newline character to preserve it
                                    clone.appendChild(cdoc.createTextNode("."));

                                    var text = clone.innerText;

                                    text = text.substring(0, text.length - 1);
                                    return text;

                                };

                                // To support text/html content type
                                if (!cdoc.body || !cdoc.body.firstChild || cdoc.body.firstChild.nodeName.toLowerCase() !== "pre") {
                                    // Injects a plaintext element which renders text without interpreting the HTML and cannot be stopped
                                    // it is deprecated in HTML5, but still works
                                    var head = cdoc.head || cdoc.getElementsByTagName("head")[0] || cdoc.documentElement || cdoc;
                                    var script = cdoc.createElement("script");

                                    script.text = "document.write('<plaintext>')";

                                    head.insertBefore(script, head.firstChild);
                                    head.removeChild(script);

                                    // The plaintext element will be the response container
                                    res = cdoc.body.lastChild;
                                }

                                if (rq.closed) {
                                    rq.isReopen = true;
                                }

                                // Handles message and close event
                                stop = atmosphere.util.iterate(function () {
                                    var text = readResponse();
                                    if (text.length > rq.lastIndex) {
                                        _timeout(_request);

                                        _response.status = 200;
                                        _response.error = null;

                                        // Empties response every time that it is handled
                                        res.innerText = "";
                                        var skipCallbackInvocation = _trackMessageSize(text, rq, _response);
                                        if (skipCallbackInvocation) {
                                            return "";
                                        }

                                        _prepareCallback(_response.responseBody, "messageReceived", 200, rq.transport);
                                    }

                                    rq.lastIndex = 0;

                                    if (cdoc.readyState === "complete") {
                                        _invokeClose(true);
                                        _open('re-connecting', rq.transport, rq);
                                        if (rq.reconnectInterval > 0) {
                                            rq.reconnectId = setTimeout(function () {
                                                _ieStreaming(rq);
                                            }, rq.reconnectInterval);
                                        } else {
                                            _ieStreaming(rq);
                                        }
                                        return false;
                                    }
                                }, null);

                                return false;
                            } catch (err) {
                                _response.error = true;
                                _open('re-connecting', rq.transport, rq);
                                if (_requestCount++ < rq.maxReconnectOnClose) {
                                    if (rq.reconnectInterval > 0) {
                                        rq.reconnectId = setTimeout(function () {
                                            _ieStreaming(rq);
                                        }, rq.reconnectInterval);
                                    } else {
                                        _ieStreaming(rq);
                                    }
                                } else {
                                    _onError(0, "maxReconnectOnClose reached");
                                }
                                doc.execCommand("Stop");
                                doc.close();
                                return false;
                            }
                        });
                    },

                    close: function () {
                        if (stop) {
                            stop();
                        }

                        doc.execCommand("Stop");
                        _invokeClose(true);
                    }
                };
            }

            /**
             * Send message. <br>
             * Will be automatically dispatch to other connected.
             *
             * @param {Object, string} Message to send.
             * @private
             */
            function _push(message) {

                if (_localStorageService != null) {
                    _pushLocal(message);
                } else if (_activeRequest != null || _sse != null) {
                    _pushAjaxMessage(message);
                } else if (_ieStream != null) {
                    _pushIE(message);
                } else if (_jqxhr != null) {
                    _pushJsonp(message);
                } else if (_websocket != null) {
                    _pushWebSocket(message);
                } else {
                    _onError(0, "No suspended connection available");
                    atmosphere.util.error("No suspended connection available. Make sure atmosphere.subscribe has been called and request.onOpen invoked before invoking this method");
                }
            }

            function _pushOnClose(message, rq) {
                if (!rq) {
                    rq = _getPushRequest(message);
                }
                rq.transport = "polling";
                rq.method = "GET";
                rq.withCredentials = false;
                rq.reconnect = false;
                rq.force = true;
                rq.suspend = false;
                rq.timeout = 1000;
                _executeRequest(rq);
            }

            function _pushLocal(message) {
                _localStorageService.send(message);
            }

            function _intraPush(message) {
                // IE 9 will crash if not.
                if (message.length === 0)
                    return;

                try {
                    if (_localStorageService) {
                        _localStorageService.localSend(message);
                    } else if (_storageService) {
                        _storageService.signal("localMessage", atmosphere.util.stringifyJSON({
                            id: guid,
                            event: message
                        }));
                    }
                } catch (err) {
                    atmosphere.util.error(err);
                }
            }

            /**
             * Send a message using currently opened ajax request (using http-streaming or long-polling). <br>
             *
             * @param {string, Object} Message to send. This is an object, string message is saved in data member.
             * @private
             */
            function _pushAjaxMessage(message) {
                var rq = _getPushRequest(message);
                _executeRequest(rq);
            }

            /**
             * Send a message using currently opened ie streaming (using http-streaming or long-polling). <br>
             *
             * @param {string, Object} Message to send. This is an object, string message is saved in data member.
             * @private
             */
            function _pushIE(message) {
                if (_request.enableXDR && atmosphere.util.checkCORSSupport()) {
                    var rq = _getPushRequest(message);
                    // Do not reconnect since we are pushing.
                    rq.reconnect = false;
                    _jsonp(rq);
                } else {
                    _pushAjaxMessage(message);
                }
            }

            /**
             * Send a message using jsonp transport. <br>
             *
             * @param {string, Object} Message to send. This is an object, string message is saved in data member.
             * @private
             */
            function _pushJsonp(message) {
                _pushAjaxMessage(message);
            }

            function _getStringMessage(message) {
                var msg = message;
                if (typeof (msg) === 'object') {
                    msg = message.data;
                }
                return msg;
            }

            /**
             * Build request use to push message using method 'POST' <br>. Transport is defined as 'polling' and 'suspend' is set to false.
             *
             * @return {Object} Request object use to push message.
             * @private
             */
            function _getPushRequest(message) {
                var msg = _getStringMessage(message);

                var rq = {
                    connected: false,
                    timeout: 60000,
                    method: 'POST',
                    url: _request.url,
                    contentType: _request.contentType,
                    headers: _request.headers,
                    reconnect: true,
                    callback: null,
                    data: msg,
                    suspend: false,
                    maxRequest: -1,
                    logLevel: 'info',
                    requestCount: 0,
                    withCredentials: _request.withCredentials,
                    async: _request.async,
                    transport: 'polling',
                    isOpen: true,
                    attachHeadersAsQueryString: true,
                    enableXDR: _request.enableXDR,
                    uuid: _request.uuid,
                    dispatchUrl: _request.dispatchUrl,
                    enableProtocol: false,
                    messageDelimiter: '|',
                    trackMessageLength: _request.trackMessageLength,
                    maxReconnectOnClose: _request.maxReconnectOnClose,
                    heartbeatTimer: _request.heartbeatTimer,
                    heartbeat: _request.heartbeat
                };

                if (typeof (message) === 'object') {
                    rq = atmosphere.util.extend(rq, message);
                }

                return rq;
            }

            /**
             * Send a message using currently opened websocket. <br>
             *
             */
            function _pushWebSocket(message) {
                var msg = atmosphere.util.isBinary(message) ? message : _getStringMessage(message);
                var data;
                try {
                    if (_request.dispatchUrl != null) {
                        data = _request.webSocketPathDelimiter + _request.dispatchUrl + _request.webSocketPathDelimiter + msg;
                    } else {
                        data = msg;
                    }

                    if (!_websocket.canSendMessage) {
                        atmosphere.util.error("WebSocket not connected.");
                        return;
                    }

                    _websocket.send(data);

                } catch (e) {
                    _websocket.onclose = function (message) {
                    };
                    _clearState();

                    _reconnectWithFallbackTransport("Websocket failed. Downgrading to Comet and resending " + message);
                    _pushAjaxMessage(message);
                }
            }

            function _localMessage(message) {
                var m = atmosphere.util.parseJSON(message);
                if (m.id !== guid) {
                    if (typeof (_request.onLocalMessage) !== 'undefined') {
                        _request.onLocalMessage(m.event);
                    } else if (typeof (atmosphere.util.onLocalMessage) !== 'undefined') {
                        atmosphere.util.onLocalMessage(m.event);
                    }
                }
            }

            function _prepareCallback(messageBody, state, errorCode, transport) {

                _response.responseBody = messageBody;
                _response.transport = transport;
                _response.status = errorCode;
                _response.state = state;

                _invokeCallback();
            }

            function _readHeaders(xdr, request) {
                if (!request.readResponsesHeaders) {
                    if (!request.enableProtocol) {
                        request.uuid = guid;
                    }
                }
                else {
                    try {

                        var tempUUID = xdr.getResponseHeader('X-Atmosphere-tracking-id');
                        if (tempUUID && tempUUID != null) {
                            request.uuid = tempUUID.split(" ").pop();
                        }
                    } catch (e) {
                    }
                }
            }

            function _invokeFunction(response) {
                _f(response, _request);
                // Global
                _f(response, atmosphere.util);
            }

            function _f(response, f) {
                switch (response.state) {
                    case "messageReceived":
                        _requestCount = 0;
                        if (typeof (f.onMessage) !== 'undefined')
                            f.onMessage(response);

                        if (typeof (f.onmessage) !== 'undefined')
                            f.onmessage(response);
                        break;
                    case "error":
                        if (typeof (f.onError) !== 'undefined')
                            f.onError(response);

                        if (typeof (f.onerror) !== 'undefined')
                            f.onerror(response);
                        break;
                    case "opening":
                        delete _request.closed;
                        if (typeof (f.onOpen) !== 'undefined')
                            f.onOpen(response);

                        if (typeof (f.onopen) !== 'undefined')
                            f.onopen(response);
                        break;
                    case "messagePublished":
                        if (typeof (f.onMessagePublished) !== 'undefined')
                            f.onMessagePublished(response);
                        break;
                    case "re-connecting":
                        if (typeof (f.onReconnect) !== 'undefined')
                            f.onReconnect(_request, response);
                        break;
                    case "closedByClient":
                        if (typeof (f.onClientTimeout) !== 'undefined')
                            f.onClientTimeout(_request);
                        break;
                    case "re-opening":
                        delete _request.closed;
                        if (typeof (f.onReopen) !== 'undefined')
                            f.onReopen(_request, response);
                        break;
                    case "fail-to-reconnect":
                        if (typeof (f.onFailureToReconnect) !== 'undefined')
                            f.onFailureToReconnect(_request, response);
                        break;
                    case "unsubscribe":
                    case "closed":
                        var closed = typeof (_request.closed) !== 'undefined' ? _request.closed : false;

                        if (!closed) {
                            if (typeof (f.onClose) !== 'undefined') {
                                f.onClose(response);
                            }

                            if (typeof (f.onclose) !== 'undefined') {
                                f.onclose(response);
                            }
                        }
                        _request.closed = true;
                        break;
                }
            }

            function _invokeClose(wasOpen) {
                if (_response.state !== 'closed') {
                    _response.state = 'closed';
                    _response.responseBody = "";
                    _response.messages = [];
                    _response.status = !wasOpen ? 501 : 200;
                    _invokeCallback();
                }
            }

            /**
             * Invoke request callbacks.
             *
             * @private
             */
            function _invokeCallback() {
                var call = function (index, func) {
                    func(_response);
                };

                if (_localStorageService == null && _localSocketF != null) {
                    _localSocketF(_response.responseBody);
                }

                _request.reconnect = _request.mrequest;

                var isString = typeof (_response.responseBody) === 'string';
                var messages = (isString && _request.trackMessageLength) ? (_response.messages.length > 0 ? _response.messages : ['']) : new Array(
                    _response.responseBody);
                for (var i = 0; i < messages.length; i++) {

                    if (messages.length > 1 && messages[i].length === 0) {
                        continue;
                    }
                    _response.responseBody = (isString) ? atmosphere.util.trim(messages[i]) : messages[i];

                    if (_localStorageService == null && _localSocketF != null) {
                        _localSocketF(_response.responseBody);
                    }

                    if (_response.responseBody.length === 0 && _response.state === "messageReceived") {
                        continue;
                    }

                    _invokeFunction(_response);

                    // Invoke global callbacks
                    if (callbacks.length > 0) {
                        if (_request.logLevel === 'debug') {
                            atmosphere.util.debug("Invoking " + callbacks.length + " global callbacks: " + _response.state);
                        }
                        try {
                            atmosphere.util.each(callbacks, call);
                        } catch (e) {
                            atmosphere.util.log(_request.logLevel, ["Callback exception" + e]);
                        }
                    }

                    // Invoke request callback
                    if (typeof (_request.callback) === 'function') {
                        if (_request.logLevel === 'debug') {
                            atmosphere.util.debug("Invoking request callbacks");
                        }
                        try {
                            _request.callback(_response);
                        } catch (e) {
                            atmosphere.util.log(_request.logLevel, ["Callback exception" + e]);
                        }
                    }
                }
            }

            this.subscribe = function (options) {
                _subscribe(options);
                _execute();
            };

            this.execute = function () {
                _execute();
            };

            this.close = function () {
                _close();
            };

            this.disconnect = function () {
                _disconnect();
            };

            this.getUrl = function () {
                return _request.url;
            };

            this.push = function (message, dispatchUrl) {
                if (dispatchUrl != null) {
                    var originalDispatchUrl = _request.dispatchUrl;
                    _request.dispatchUrl = dispatchUrl;
                    _push(message);
                    _request.dispatchUrl = originalDispatchUrl;
                } else {
                    _push(message);
                }
            };

            this.getUUID = function () {
                return _request.uuid;
            };

            this.pushLocal = function (message) {
                _intraPush(message);
            };

            this.enableProtocol = function (message) {
                return _request.enableProtocol;
            };

            this.request = _request;
            this.response = _response;
        }
    };

    atmosphere.subscribe = function (url, callback, request) {
        if (typeof (callback) === 'function') {
            atmosphere.addCallback(callback);
        }

        if (typeof (url) !== "string") {
            request = url;
        } else {
            request.url = url;
        }

        // https://github.com/Atmosphere/atmosphere-javascript/issues/58
        uuid = ((typeof (request) !== 'undefined') && typeof (request.uuid) !== 'undefined') ? request.uuid : 0;

        var rq = new atmosphere.AtmosphereRequest(request);
        rq.execute();

        requests[requests.length] = rq;
        return rq;
    };

    atmosphere.unsubscribe = function () {
        if (requests.length > 0) {
            var requestsClone = [].concat(requests);
            for (var i = 0; i < requestsClone.length; i++) {
                var rq = requestsClone[i];
                rq.close();
                clearTimeout(rq.response.request.id);

                if (rq.heartbeatTimer) {
                    clearTimeout(rq.heartbeatTimer);
                }
            }
        }
        requests = [];
        callbacks = [];
    };

    atmosphere.unsubscribeUrl = function (url) {
        var idx = -1;
        if (requests.length > 0) {
            for (var i = 0; i < requests.length; i++) {
                var rq = requests[i];

                // Suppose you can subscribe once to an url
                if (rq.getUrl() === url) {
                    rq.close();
                    clearTimeout(rq.response.request.id);

                    if (rq.heartbeatTimer) {
                        clearTimeout(rq.heartbeatTimer);
                    }

                    idx = i;
                    break;
                }
            }
        }
        if (idx >= 0) {
            requests.splice(idx, 1);
        }
    };

    atmosphere.addCallback = function (func) {
        if (atmosphere.util.inArray(func, callbacks) === -1) {
            callbacks.push(func);
        }
    };

    atmosphere.removeCallback = function (func) {
        var index = atmosphere.util.inArray(func, callbacks);
        if (index !== -1) {
            callbacks.splice(index, 1);
        }
    };

    atmosphere.util = {
        browser: {},

        parseHeaders: function (headerString) {
            var match, rheaders = /^(.*?):[ \t]*([^\r\n]*)\r?$/mg, headers = {};
            while (match = rheaders.exec(headerString)) {
                headers[match[1]] = match[2];
            }
            return headers;
        },

        now: function () {
            return new Date().getTime();
        },

        isArray: function (array) {
            return Object.prototype.toString.call(array) === "[object Array]";
        },

        inArray: function (elem, array) {
            if (!Array.prototype.indexOf) {
                var len = array.length;
                for (var i = 0; i < len; ++i) {
                    if (array[i] === elem) {
                        return i;
                    }
                }
                return -1;
            }
            return array.indexOf(elem);
        },

        isBinary: function (data) {
            // True if data is an instance of Blob, ArrayBuffer or ArrayBufferView 
            return /^\[object\s(?:Blob|ArrayBuffer|.+Array)\]$/.test(Object.prototype.toString.call(data));
        },

        isFunction: function (fn) {
            return Object.prototype.toString.call(fn) === "[object Function]";
        },

        getAbsoluteURL: function (url) {
            var div = document.createElement("div");

            // Uses an innerHTML property to obtain an absolute URL
            div.innerHTML = '<a href="' + url + '"/>';

            // encodeURI and decodeURI are needed to normalize URL between IE and non-IE,
            // since IE doesn't encode the href property value and return it - http://jsfiddle.net/Yq9M8/1/
            return encodeURI(decodeURI(div.firstChild.href));
        },

        prepareURL: function (url) {
            // Attaches a time stamp to prevent caching
            var ts = atmosphere.util.now();
            var ret = url.replace(/([?&])_=[^&]*/, "$1_=" + ts);

            return ret + (ret === url ? (/\?/.test(url) ? "&" : "?") + "_=" + ts : "");
        },

        trim: function (str) {
            if (!String.prototype.trim) {
                return str.toString().replace(/(?:(?:^|\n)\s+|\s+(?:$|\n))/g, "").replace(/\s+/g, " ");
            } else {
                return str.toString().trim();
            }
        },

        param: function (params) {
            var prefix, s = [];

            function add(key, value) {
                value = atmosphere.util.isFunction(value) ? value() : (value == null ? "" : value);
                s.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
            }

            function buildParams(prefix, obj) {
                var name;

                if (atmosphere.util.isArray(obj)) {
                    atmosphere.util.each(obj, function (i, v) {
                        if (/\[\]$/.test(prefix)) {
                            add(prefix, v);
                        } else {
                            buildParams(prefix + "[" + (typeof v === "object" ? i : "") + "]", v);
                        }
                    });
                } else if (Object.prototype.toString.call(obj) === "[object Object]") {
                    for (name in obj) {
                        buildParams(prefix + "[" + name + "]", obj[name]);
                    }
                } else {
                    add(prefix, obj);
                }
            }

            for (prefix in params) {
                buildParams(prefix, params[prefix]);
            }

            return s.join("&").replace(/%20/g, "+");
        },

        storage: function () {
            try {
                return !!(window.localStorage && window.StorageEvent);
            } catch (e) {
                //Firefox throws an exception here, see
                //https://bugzilla.mozilla.org/show_bug.cgi?id=748620
                return false;
            }
        },

        iterate: function (fn, interval) {
            var timeoutId;

            // Though the interval is 0 for real-time application, there is a delay between setTimeout calls
            // For detail, see https://developer.mozilla.org/en/window.setTimeout#Minimum_delay_and_timeout_nesting
            interval = interval || 0;

            (function loop() {
                timeoutId = setTimeout(function () {
                    if (fn() === false) {
                        return;
                    }

                    loop();
                }, interval);
            })();

            return function () {
                clearTimeout(timeoutId);
            };
        },

        each: function (obj, callback, args) {
            if (!obj) return;
            var value, i = 0, length = obj.length, isArray = atmosphere.util.isArray(obj);

            if (args) {
                if (isArray) {
                    for (; i < length; i++) {
                        value = callback.apply(obj[i], args);

                        if (value === false) {
                            break;
                        }
                    }
                } else {
                    for (i in obj) {
                        value = callback.apply(obj[i], args);

                        if (value === false) {
                            break;
                        }
                    }
                }

                // A special, fast, case for the most common use of each
            } else {
                if (isArray) {
                    for (; i < length; i++) {
                        value = callback.call(obj[i], i, obj[i]);

                        if (value === false) {
                            break;
                        }
                    }
                } else {
                    for (i in obj) {
                        value = callback.call(obj[i], i, obj[i]);

                        if (value === false) {
                            break;
                        }
                    }
                }
            }

            return obj;
        },

        extend: function (target) {
            var i, options, name;

            for (i = 1; i < arguments.length; i++) {
                if ((options = arguments[i]) != null) {
                    for (name in options) {
                        target[name] = options[name];
                    }
                }
            }

            return target;
        },
        on: function (elem, type, fn) {
            if (elem.addEventListener) {
                elem.addEventListener(type, fn, false);
            } else if (elem.attachEvent) {
                elem.attachEvent("on" + type, fn);
            }
        },
        off: function (elem, type, fn) {
            if (elem.removeEventListener) {
                elem.removeEventListener(type, fn, false);
            } else if (elem.detachEvent) {
                elem.detachEvent("on" + type, fn);
            }
        },

        log: function (level, args) {
            if (window.console) {
                var logger = window.console[level];
                if (typeof logger === 'function') {
                    logger.apply(window.console, args);
                }
            }
        },

        warn: function () {
            atmosphere.util.log('warn', arguments);
        },

        info: function () {
            atmosphere.util.log('info', arguments);
        },

        debug: function () {
            atmosphere.util.log('debug', arguments);
        },

        error: function () {
            atmosphere.util.log('error', arguments);
        },
        xhr: function () {
            try {
                return new window.XMLHttpRequest();
            } catch (e1) {
                try {
                    return new window.ActiveXObject("Microsoft.XMLHTTP");
                } catch (e2) {
                }
            }
        },
        parseJSON: function (data) {
            return !data ? null : window.JSON && window.JSON.parse ? window.JSON.parse(data) : new Function("return " + data)();
        },
        // http://github.com/flowersinthesand/stringifyJSON
        stringifyJSON: function (value) {
            var escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g, meta = {
                '\b': '\\b',
                '\t': '\\t',
                '\n': '\\n',
                '\f': '\\f',
                '\r': '\\r',
                '"': '\\"',
                '\\': '\\\\'
            };

            function quote(string) {
                return '"' + string.replace(escapable, function (a) {
                    var c = meta[a];
                    return typeof c === "string" ? c : "\\u" + ("0000" + a.charCodeAt(0).toString(16)).slice(-4);
                }) + '"';
            }

            function f(n) {
                return n < 10 ? "0" + n : n;
            }

            return window.JSON && window.JSON.stringify ? window.JSON.stringify(value) : (function str(key, holder) {
                var i, v, len, partial, value = holder[key], type = typeof value;

                if (value && typeof value === "object" && typeof value.toJSON === "function") {
                    value = value.toJSON(key);
                    type = typeof value;
                }

                switch (type) {
                    case "string":
                        return quote(value);
                    case "number":
                        return isFinite(value) ? String(value) : "null";
                    case "boolean":
                        return String(value);
                    case "object":
                        if (!value) {
                            return "null";
                        }

                        switch (Object.prototype.toString.call(value)) {
                            case "[object Date]":
                                return isFinite(value.valueOf()) ? '"' + value.getUTCFullYear() + "-" + f(value.getUTCMonth() + 1) + "-"
                                    + f(value.getUTCDate()) + "T" + f(value.getUTCHours()) + ":" + f(value.getUTCMinutes()) + ":" + f(value.getUTCSeconds())
                                    + "Z" + '"' : "null";
                            case "[object Array]":
                                len = value.length;
                                partial = [];
                                for (i = 0; i < len; i++) {
                                    partial.push(str(i, value) || "null");
                                }

                                return "[" + partial.join(",") + "]";
                            default:
                                partial = [];
                                for (i in value) {
                                    if (hasOwn.call(value, i)) {
                                        v = str(i, value);
                                        if (v) {
                                            partial.push(quote(i) + ":" + v);
                                        }
                                    }
                                }

                                return "{" + partial.join(",") + "}";
                        }
                }
            })("", {
                "": value
            });
        },

        checkCORSSupport: function () {
            if (atmosphere.util.browser.msie && !window.XDomainRequest && +atmosphere.util.browser.version.split(".")[0] < 11) {
                return true;
            } else if (atmosphere.util.browser.opera && +atmosphere.util.browser.version.split(".") < 12.0) {
                return true;
            }

            // KreaTV 4.1 -> 4.4
            else if (atmosphere.util.trim(navigator.userAgent).slice(0, 16) === "KreaTVWebKit/531") {
                return true;
            }
            // KreaTV 3.8
            else if (atmosphere.util.trim(navigator.userAgent).slice(-7).toLowerCase() === "kreatel") {
                return true;
            }

            // Force Android to use CORS as some version like 2.2.3 fail otherwise
            var ua = navigator.userAgent.toLowerCase();
            var isAndroid = ua.indexOf("android") > -1;
            if (isAndroid) {
                return true;
            }
            return false;
        }
    };

    guid = atmosphere.util.now();

    // Browser sniffing
    (function () {
        var ua = navigator.userAgent.toLowerCase(),
            match = /(chrome)[ \/]([\w.]+)/.exec(ua) ||
                /(webkit)[ \/]([\w.]+)/.exec(ua) ||
                /(opera)(?:.*version|)[ \/]([\w.]+)/.exec(ua) ||
                /(msie) ([\w.]+)/.exec(ua) ||
                /(trident)(?:.*? rv:([\w.]+)|)/.exec(ua) ||
                ua.indexOf("compatible") < 0 && /(mozilla)(?:.*? rv:([\w.]+)|)/.exec(ua) ||
                [];

        atmosphere.util.browser[match[1] || ""] = true;
        atmosphere.util.browser.version = match[2] || "0";

        // Trident is the layout engine of the Internet Explorer
        // IE 11 has no "MSIE: 11.0" token
        if (atmosphere.util.browser.trident) {
            atmosphere.util.browser.msie = true;
        }

        // The storage event of Internet Explorer and Firefox 3 works strangely
        if (atmosphere.util.browser.msie || (atmosphere.util.browser.mozilla && +atmosphere.util.browser.version.split(".")[0] === 1)) {
            atmosphere.util.storage = false;
        }
    })();

    atmosphere.util.on(window, "unload", function (event) {
        atmosphere.unsubscribe();
    });

    // Pressing ESC key in Firefox kills the connection
    // for your information, this is fixed in Firefox 20
    // https://bugzilla.mozilla.org/show_bug.cgi?id=614304
    atmosphere.util.on(window, "keypress", function (event) {
        if (event.charCode === 27 || event.keyCode === 27) {
            if (event.preventDefault) {
                event.preventDefault();
            }
        }
    });

    atmosphere.util.on(window, "offline", function () {
        if (requests.length > 0) {
            var requestsClone = [].concat(requests);
            for (var i = 0; i < requestsClone.length; i++) {
                var rq = requestsClone[i];
                rq.close();
                clearTimeout(rq.response.request.id);

                if (rq.heartbeatTimer) {
                    clearTimeout(rq.heartbeatTimer);
                }
            }
        }
    });

    atmosphere.util.on(window, "online", function () {
        if (requests.length > 0) {
            for (var i = 0; i < requests.length; i++) {
                requests[i].execute();
            }
        }
    });
/* jshint eqnull:true, noarg:true, noempty:true, eqeqeq:true, evil:true, laxbreak:true, undef:true, browser:true, indent:false, maxerr:50 */

var login_html =
"<form id='loginForm' class='form-horizontal full-page-form'>\n" +
"	<fieldset id='loginFields'>\n" +
"		<legend>Ziniki</legend>\n" +
"		<!--\n" +
"		{{#if guardOnDuty}}\n" +
"		<div class='alert alert-info'>{{guardOnDuty.label}}</div>\n" +
"		{{/if}}\n" +
"		-->\n" +
"		<div class='form-group'>\n" +
"			<label class='ziniki-control-label'>Username</label>\n" +
"			<div class='ziniki-controls'><input type='text' id='username' focused=true></div>\n" +
"		</div>\n" +
"		<div class='form-group'>\n" +
"			<label class='ziniki-control-label'>Password</label>\n" +
"			<div class='ziniki-controls'><input type='password' id='password'></div>\n" +
"		</div>\n" +
"		<div>\n" +
"			<div class='ziniki-unlabeled-controls'><button id='logInButton' type='button' class='btn btn-default' onclick='zinikiLogin()'>Log in</button></div>\n" +
"		</div>\n" +
"		<div class='form-group'>\n" +
"			<div id='loginError' class='ziniki-unlabeled-controls has-error' style='display: none'><span class='help-block'>Login failed</span></div>\n" +
"		</div>\n" +
"		<div class='form-group-spacer-medium'>\n" +
"		</div>\n" +
"		<!--\n" +
"		<div class='form-group'>\n" +
"			{{view 'identityProviders' label='Or sign in with' actionVerb='Log in' activityName='login'}}\n" +
"		</div>\n" +
"		-->\n" +
"		<!--\n" +
"		{{#if userApprovalOfRegistration}}\n" +
"		<div class='form-group'>\n" +
"			<div class='ziniki-unlabeled-controls-wide'>\n" +
"				<div class='alert alert-warning'>\n" +
"					{{userApprovalOfRegistration.credentialName}} hasn't yet been registered with Ziniki.\n" +
"					{{#if registrationPromptsAreAllowed}}\n" +
"					Would you like to register it now?\n" +
"					<button id='userApprovalOfRegistrationYes' class='btn btn-warning' type='button' {{action 'acceptApprovalOfRegistration'}}>Yes</button>\n" +
"					{{/if}}\n" +
"				</div>\n" +
"			</div>\n" +
"		</div>\n" +
"		{{/if}}\n" +
"		-->\n" +
"		<!--\n" +
"		{{#if registrationPromptsAreAllowed}}\n" +
"		<div class='form-group-spacer-small'>\n" +
"		</div>\n" +
"		<div class='form-group'>\n" +
"			<div class='ziniki-unlabeled-controls'>\n" +
"				{{#if userApprovalOfRegistration}}\n" +
"				You can also {{#link-to 'register' id='register'}}register{{/link-to}} a different account, or a new username.\n" +
"				{{else}}\n" +
"				Or you can {{#link-to 'register' id='register'}}register{{/link-to}} a new account.\n" +
"				{{/if}}\n" +
"			</div>\n" +
"		</div>\n" +
"		{{/if}}\n" +
"		-->\n" +
"	</fieldset>\n" +
"</form>\n" +
"";

var popover_html =
"	<div id='flasck_popover_chrome'>\n" +
"		<a style='text-decoration: none;' onclick='FlasckComponents.closePopover()'>X</a>\n" +
"	</div>\n" +
"	<div id='flasck_popover_div'>\n" +
"	</div>\n" +
"";

var toolbar_html =
"  	<div class='toolbar'>\n" +
"  		<span id='currentUser' class='logged-in-user'></span>\n" +
"  		<button class='toolbar-button' onclick='FlasckComponents.logout()'>log out</button>\n" +
"  	</div>\n" +
"";

FlasckComponents = {};

FlasckComponents.provideLogin = function(div) {
	var dialog = div.ownerDocument.createElement("dialog");
	dialog.id = 'flasck_login';
	dialog.innerHTML = login_html;
	div.appendChild(dialog);
}

FlasckComponents.providePopover = function(div) {
	var dialog = div.ownerDocument.createElement("dialog");
	dialog.id = 'flasck_popover';
	dialog.innerHTML = popover_html;
	div.appendChild(dialog);
}

FlasckComponents.currentCard = null;

FlasckComponents.popoverCard = function(postbox, services, card) {
	var popover = doc.getElementById('flasck_popover_div');
	popover.innerHTML = '';
	var card = Flasck.createCard(postbox, popover, { mode: 'overlay', explicit: card }, services);
	FlasckComponents.currentCard = card
	var p1 = doc.getElementById('flasck_popover');
	if (!p1.open)
		p1.showModal();
	return card;
}

FlasckComponents.closePopover = function() {
	document.getElementById("flasck_popover").close();
	FlasckComponents.currentCard.dispose();
}

FlasckComponents.provideToolbar = function(div) {
	var bar = div.ownerDocument.createElement("div");
	bar.innerHTML = toolbar_html;
	div.appendChild(bar);
}

FlasckComponents.logout = function() {
	console.log("logging out ...");
	var user = doc.getElementById('username');
	var pwd = doc.getElementById('password');
	user.value = '';
	pwd.value = '';
	new FlasckServices.CredentialsService(doc, postbox).logout();
}
		

FlasckServices = {};

// Not really a service
FlasckServices.RenderService = function(postbox) {
	this.postbox = postbox;
	return this;
}

FlasckServices.TimerService = function(postbox) {
	this.postbox = postbox;
	return this;
}

FlasckServices.TimerService.prototype.process = function(message) {
	this.requestTicks(message.args[0], message.args[1], message.args[2]);
}

FlasckServices.TimerService.prototype.requestTicks = function(handler, amount) {
	var self = this;
//	console.log("Add timer for handler", handler, amount);
//	console.log("interval should be every " + amount + "s");
	setInterval(function() {
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'onTick', args:[] });
	}, 1000);
}

FlasckServices.WindowService = function(postbox) {
	this.postbox = postbox;
	return this;
}

FlasckServices.WindowService.prototype.process = function(message) {
	"use strict";
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this, message.args);
}

FlasckServices.WindowService.prototype.closePopup = function() {
	"use strict";
	var elt = document.getElementById("flasck_popover");
	if (elt)
		elt.close();
}

FlasckServices.WindowService.prototype.requestFullScreen = function() {
	"use strict";
	var body = doc.getElementsByTagName("html")[0];
	body.webkitRequestFullScreen();
}

FlasckServices.WindowService.prototype.leaveFullScreen = function() {
	"use strict";
	var body = doc.getElementsByTagName("html")[0];
	doc.webkitCancelFullScreen();
}

FlasckServices.CentralStore = {
	keyValue: {_hack: 'keyvalue', _localMapping: {}},
	personae: {_hack: 'personae'},
	crokeys: {_hack: 'crokeys'}
};

FlasckServices.CentralStore.realId = function(id) {
	if (id[0] != '_' || id[1] != '_')
		return id;
	return FlasckServices.CentralStore.keyValue._localMapping[id];
}

FlasckServices.CentralStore.keyValue.merge = function(it) {
	if (this[it.id] === null || this[it.id] === undefined) {
		this[it.id] = it;
		return;
	}
	var already = this[it.id];
	for (var p in it)
		if (it.hasOwnProperty(p))
			already[p] = it[p];
}

FlasckServices.CentralStore.unpackPayload = function(store, payload) {
	var main = payload._main;
	for (var k in payload) {
		if (k[0] !== '_' && payload.hasOwnProperty(k)) {
			if (!main)
				main = k;
			var l = payload[k];
			if (l instanceof Array) {
				for (var i=0;i<l.length;i++) {
					var it = l[i];
					if (!it._ctor)
						it._ctor = main;
					store[it.id] = it;
				}
			}
		}
	}
	return payload[main][0];
}

FlasckServices.KeyValueService = function(postbox) {
	this.postbox = postbox;
	this.store = FlasckServices.CentralStore.keyValue;
	this.nextLocal = 1;
	return this;
}

FlasckServices.KeyValueService.prototype.process = function(message) {
//	console.log("received message", message);
	"use strict";
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this, message.args);
}

FlasckServices.KeyValueService.prototype.create = function(type, handler) {
	var self = this;
	var id = '__' + (self.nextLocal++)
	var letMeCreate = { _ctor: type, id: id };
	self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'itemCreated', args:[letMeCreate]});
	// NOTE: we should now close "handler.chan" because it has served its purpose
	

	var zinchandler = function (msg) {
		console.log("kv received", msg, "from Ziniki for local id", id);
		var obj = FlasckServices.CentralStore.unpackPayload(self.store, msg.payload);
		self.store._localMapping[id] = obj.id;
		// still to do:
		// 3. notify my KV client (not the handler) of an ID change
	};

	var payload = {};
	payload[type] = [{}];

	var resource = 'create/' + type;
	var req = ZinikiConn.req.invoke(resource, zinchandler);
	req.setPayload(payload);
	req.send();
}

// TODO: remove all the duplication in all these methods
FlasckServices.KeyValueService.prototype.typed = function(type, id, handler) {
	"use strict";
	var self = this;
	if (self.store.hasOwnProperty(id)) {
		var obj = self.store[id];
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		return;
	}

	var resource = 'typedObject/' + type + '/' + id;

	if (self.store.hasOwnProperty(resource)) {
		var obj = self.store[resource];
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		return;
	}
	var zinchandler = function (msg) {
		if (msg.error)
			console.log("error:", msg.error);
		else {
			var obj = FlasckServices.CentralStore.unpackPayload(self.store, msg.payload);
			self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		}
	};

	ZinikiConn.req.subscribe(resource, zinchandler).send();
}

FlasckServices.KeyValueService.prototype.unprojected = function(id, handler) {
	"use strict";
	var self = this;
	var resource = 'unprojected/' + id;
	
	
	/* This code, while a good idea, has the inherent flaw that it assumes that a given object
	 * will always have the same type, which is certainly not true in the face of envelopes
	if (self.store.hasOwnProperty(id)) {
		var obj = self.store[id];
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		return;
	}
	*/

	// This test seems safe, but I'm not sure that we're putting things back here in terms of resources ...
	if (self.store.hasOwnProperty(resource)) {
		var obj = self.store[resource];
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		return;
	}
	var zinchandler = function (msg) {
		if (msg.error)
			console.log("error:", msg.error);
		else {
			var obj = FlasckServices.CentralStore.unpackPayload(self.store, msg.payload);
			self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		}
	};

	ZinikiConn.req.subscribe(resource, zinchandler).send();
}

FlasckServices.KeyValueService.prototype.resource = function(resource, handler) {
	"use strict";
	var self = this;
	console.log("self =", self, "subscribe to", resource);

	if (self.store.hasOwnProperty(resource)) {
		var obj = self.store[resource];
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		return;
	}
	var zinchandler = function (msg) {
		console.log("kv received", msg, "from Ziniki");
		if (msg.error)
			console.log("error:", msg.error);
		else {
			var obj = FlasckServices.CentralStore.unpackPayload(self.store, msg.payload);
			self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		}
	};
	ZinikiConn.req.subscribe(resource, zinchandler).send();
}

FlasckServices.KeyValueService.prototype.save = function(obj) {
	"use strict";
	var self = this;
	var cvobj = {};
	for (var x in obj) {
		if (obj.hasOwnProperty(x) && x[0] != '_' && !(obj[x] instanceof Array) && typeof obj[x] !== 'object')
			cvobj[x] = obj[x];
	}
	var id = FlasckServices.CentralStore.realId(obj.id);
	if (!id) {
		// in this case, we haven't yet seen a real id back from Ziniki (or possibly it was so long ago that we've forgotten about it)
		// what we need to do is to park this record somewhere waiting for the rewrite event to occur and when it does, turn around and save the object
		// in the meantime, we should at least cache this value locally and notify other local clients ... when that code is written
		console.log("difficult case still to be handled ... see comment");
		return;
	}
	obj.id = id;
	var payload = {};
	payload[obj._ctor] = [cvobj];
	console.log("saving payload", JSON.stringify(payload));
	ZinikiConn.req.invoke("update/" + obj._ctor + "/" + obj.id).setPayload(payload).send();
}

FlasckServices.CrosetService = function(postbox) {
	this.postbox = postbox;
	this.store = FlasckServices.CentralStore.crokeys;
	return this;
}

// TODO: we should probably have a resource that is like croset/{id}/updates
// that tells us about things that we are interested in.
// It possibly is simply enough to say "range", I don't know ...

FlasckServices.CrosetService.prototype.process = function(message) {
	"use strict";
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this, message.args);
}

FlasckServices.CrosetService.prototype.get = function(crosetId, after, count, handler) {
	"use strict";
	var self = this;
	var zinchandler = function (msg) {
		console.log("croset received", msg, "from Ziniki");
		if (msg.action === 'replace' || msg.action === 'insert') {
			var obj = FlasckServices.CentralStore.unpackPayload(self.store, msg.payload);
			self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
		} else if (msg.action === 'remove') {
			var obj = FlasckServices.CentralStore.unpackPayload(self.store, msg.payload);
			self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'remove', args:[obj]});
		} else
			throw new Error("Cannot handle croset update action: " + msg.payload.action);
	};
	ZinikiConn.req.subscribe("croset/" + crosetId + "/get/" + after + "/" + count, zinchandler).send();
}

FlasckServices.CrosetService.prototype.insert = function(crosetId, key, objId) {
	"use strict";
	var self = this;
	var sendId = FlasckServices.CentralStore.realId(objId);
	if (!sendId) {
		// I think in this case we need to try and remember that we should do this and get a callback
		// when we have the real ID.  This gets more complex as time goes by, because you end up changing
		// the thing you inserted and everything, so you probably need to consolidate those changes
		// for now, just keep retrying until it is here :-)
		console.log("We don't have a real id for this yet; deferred case handling needed");
		setTimeout(function() { self.insert(crosetId, key, objId); }, 150);
		return;
	}
	var croset = this.store[crosetId];
	if (!croset)
		throw new Error("There is no croset for " + crosetId);
	// de dup
	for (var i=0;i<croset.keys.length;i++) {
		if (croset.keys[i].id === sendId)
			return; // it's a duplicate
	}
	var payload = {};
	payload['org.ziniki.ID'] = [{id: sendId}];
	ZinikiConn.req.invoke("croset/" + crosetId + "/insertAround/" + key).setPayload(payload).send();
}

FlasckServices.CrosetService.prototype.move = function(crosetId, objId, fromKey, toKey) {
	"use strict";
	var self = this;
	var sendId = FlasckServices.CentralStore.realId(objId);
	if (!sendId) {
		// I think in this case we need to try and remember that we should do this and get a callback
		// when we have the real ID.  This gets more complex as time goes by, because you end up changing
		// the thing you inserted and everything, so you probably need to consolidate those changes
		// for now, just keep retrying until it is here :-)
		console.log("We don't have a real id for this yet; deferred case handling needed");
		setTimeout(function() { self.move(crosetId, objId, fromKey, toKey); }, 150);
		return;
	}
	var croset = this.store[crosetId];
	if (!croset)
		throw new Error("There is no croset for" + crosetId);
	ZinikiConn.req.invoke("croset/" + crosetId + "/move/" + sendId + "/" + fromKey +"/" + toKey).send();
}

FlasckServices.CrosetService.prototype.delete = function(crosetId, key, objId) {
	"use strict";
	var self = this;
	var sendId = FlasckServices.CentralStore.realId(objId);
	if (!sendId) {
		// I think in this case we need to try and remember that we should do this and get a callback
		// when we have the real ID.  This gets more complex as time goes by, because you end up changing
		// the thing you inserted and everything, so you probably need to consolidate those changes
		// for now, just keep retrying until it is here :-)
		console.log("We don't have a real id for this yet; deferred case handling needed");
		setTimeout(function() { self.delete(crosetId, key, objId); }, 150);
		return;
	}
	var croset = this.store[crosetId];
	if (!croset)
		throw new Error("There is no croset for" + crosetId);
	ZinikiConn.req.invoke("croset/" + crosetId + "/delete/" + key).send();
}


FlasckServices.ContentService = function(postbox) {
	this.postbox = postbox;
//	this.store = FlasckServices.CentralStore.crokeys;
	return this;
}

FlasckServices.ContentService.prototype.process = function(message) {
	"use strict";
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this, message.args);
}

FlasckServices.ContentService.prototype.upload = function(to, file) {
	"use strict";
	var form = new FormData();
	form.append("file", file);
	var request = new XMLHttpRequest();
	request.open("POST", to);
	request.send(form);
	// TODO: handle error recovery & transmission issues
}

FlasckServices.ContentService.prototype.readHTML = function(cid, h) {
	"use strict";
	var self = this;
	
	var csStateChange = function(obj) {
  		if (this.readyState == 4) {
  			var type = this.getResponseHeader("content-type");
			console.log("well,", type, this.responseText);
			self.postbox.deliver(h.chan, {from: self._myAddr, method: 'load', args:[type, this.responseText]});
  		}
	}
	
	var contentLink = function(msg) {
		var link = msg.payload["String"][0].value;
		if (link) {
            console.log("link = " + link);

            var csXHRr = new XMLHttpRequest();
            csXHRr.onreadystatechange = csStateChange;
            csXHRr.open("GET", link, true);
            csXHRr.send();
  		}
	}

	ZinikiConn.req.subscribe("content/" + cid + "/get", contentLink).send();
}

FlasckServices.PersonaService = function(postbox) {
	"use strict";
	this.postbox = postbox;
	this.store = FlasckServices.CentralStore.personae;
	return this;
}

FlasckServices.PersonaService.prototype.process = function(message) {
//	console.log("received message", message);
	"use strict";
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this, message.args);
}

FlasckServices.PersonaService.prototype.forApplication = function(appl, type, handler) {
	"use strict";
	var self = this;
	var resource = 'personafor/' + appl +'/' + type;
	console.log("self =", self, "subscribe to", resource);

	var zinchandler = function (msg) {
		console.log("kv received", msg, "from Ziniki");
		var main = msg.payload._main;
		for (var k in msg.payload) {
			if (k[0] !== '_' && msg.payload.hasOwnProperty(k)) {
				if (!main)
					main = k;
				var l = msg.payload[k];
				if (l instanceof Array) {
					for (var i=0;i<l.length;i++) {
						var it = l[i];
						if (!it._ctor)
							it._ctor = main;
						self.store[it.id] = it;
					}
				}
			}
		}
		var obj = msg.payload[main][0];
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'update', args:[obj]});
	};
	ZinikiConn.req.subscribe(resource, zinchandler).send();
}

FlasckServices.PersonaService.prototype.save = function(obj) {
	"use strict";
	var cvobj = {};
	for (var x in obj) {
		if (obj.hasOwnProperty(x) && x[0] != '_' && !(obj[x] instanceof Array) && typeof obj[x] !== 'object')
			cvobj[x] = obj[x];
	}
	var payload = {};
	payload[obj._ctor] = [cvobj];
	console.log("saving payload", JSON.stringify(payload));
	ZinikiConn.req.invoke("updatePersona/" + obj._ctor + "/" + obj.id).setPayload(payload).send();
}

FlasckServices.CredentialsService = function(document, postbox) {
	this.doc = document;
	this.postbox = postbox;
	return this;
}

FlasckServices.CredentialsService.prototype.process = function(message) {
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this.service, message.args);
}

FlasckServices.CredentialsService.prototype.logout = function() {
	console.log("logout");
	var self = this;
	localStorage.removeItem("zintoken");
	this.doc.getElementById("flasck_login").showModal();
}

FlasckServices.QueryService = function(postbox) {
	this.postbox = postbox;
	this.store = FlasckServices.CentralStore.keyValue;
	this.crokeys = FlasckServices.CentralStore.crokeys;
	return this;
}

FlasckServices.QueryService.prototype.process = function(message) {
//	console.log("received message", message);
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this, message.args);
}

FlasckServices.QueryService.prototype.scan = function(index, type, options, handler) {
	"use strict";
//	console.log("scan", index, type, options, handler);
	var self = this;
	var zinchandler = function(msg) {
	    console.log("scan received msg:", msg);
		if (msg.error) {
			console.log("error on scan", msg.error);
			throw new Error(msg.error);
		}
	    var payload = msg.payload;
	    if (!payload || !payload['Crokeys']) {
	    	console.log("returning because payload = ", payload, " is null or has no type", type);
	    	return;
	    }
		var main = msg.payload._main;
		var crokeys = { _ctor: 'Crokeys', keys: [] };
		for (var k in msg.payload) {
			if (k[0] !== '_' && msg.payload.hasOwnProperty(k)) {
				if (!main)
					main = k;
				if (main !== 'Crokeys')
					throw new Error("I was expecting crokeys ...");
				var l = msg.payload[k];
				if (k == 'Crokeys') {
					var ck = l[0];
					if (ck.keytype !== 'crindex' && ck.keytype !== 'natural')
						throw new Error("can't handle key type " + ck.keytype);
					crokeys.id = ck.id;
					crokeys.keytype = ck.keytype;
					crokeys.keys = ck.keys;
				} else { // sideload actual objects
					if (l instanceof Array) {
						for (var i=0;i<l.length;i++) {
							var it = l[i];
							it._ctor = k;
							self.store.merge(it)
						}
					}
				}
			}
		}
		self.crokeys[crokeys.id] = crokeys;
		self.postbox.deliver(handler.chan, {from: self._myAddr, method: 'keys', args:[crokeys]});
	}
	var req = ZinikiConn.req.subscribe(index, zinchandler);
	var idx;
	for (var k in options) {
		if (options.hasOwnProperty(k))
			req.setOption(k, options[k]);
	}
	req.send();
}

FlasckServices.YoyoService = function(postbox) {
	this.postbox = postbox;
}

FlasckServices.YoyoService.prototype.process = function(message) {
	"use strict";
	var meth = this[message.method];
	if (!meth)
		throw new Error("There is no method '" + message.method +"'");
	meth.apply(this, message.args);
}

FlasckServices.YoyoService.prototype.get = function(id, handler) {
	"use strict";
	var self = this;
	var zinchandler = function(msg) {
//		console.log("yoyo received", msg, "from Ziniki for", id);
		var rp = msg.payload['Card'][0];
		self.postbox.deliver(handler.chan, { from: self._myAddr, method: 'showCard', args: [{_ctor:'Card', explicit: rp.explicit, loadId: rp.loadId}] });
	}
	
	var req = ZinikiConn.req.invoke('invoke/org.ziniki.builtin.1/flasck/getYoyo', zinchandler);
	var payload = {}
	payload['org.ziniki.flasck.FlasckOpArgs'] = [{yoyo: id}]; 
	req.setPayload(payload);
	req.send();
}

FlasckServices.provideAll = function(document, postbox, services) {
	"use strict";
	Flasck.provideService(postbox, services, "org.ziniki.Timer", new FlasckServices.TimerService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.Window", new FlasckServices.WindowService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.Render", new FlasckServices.RenderService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.Credentials", new FlasckServices.CredentialsService(document, postbox));
	Flasck.provideService(postbox, services, "org.ziniki.KeyValue", new FlasckServices.KeyValueService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.CrosetContract", new FlasckServices.CrosetService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.ContentContract", new FlasckServices.ContentService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.Persona", new FlasckServices.PersonaService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.Query", new FlasckServices.QueryService(postbox));
	Flasck.provideService(postbox, services, "org.ziniki.Yoyo", new FlasckServices.YoyoService(postbox));
}
window.Zinc = {}

var connpool = {};

Zinc.Config = {
  hbTimeout: 90000,
  reconnInterval: 2500
};

function ZincError(message) {
  this.name = "ZincError";
  this.message = message;
  this.stack = (new Error()).stack;
}
ZincError.prototype = new Error();

/* A Connection represents a single, multiplexed, physical channel between here and a server at a URI.
 * Once created, it is the job of the connection to keep everything connected as much as possible and to use the Requestors to reconnect.
 */
function Connection(uri) {
  var self = this;
  this.isBroken = false;
  this.sentEstablish = false;
  this.requestors = [];
  // ----
  var atmoreq = new atmosphere.AtmosphereRequest({
    url: uri
  });
  atmoreq.url = uri;
  atmoreq.transport = 'websocket';
  atmoreq.fallbackTransport = 'long-polling';
  atmoreq.onOpen = function() {
    self.connect();
  };
  atmoreq.onMessage = function(msg) {
//  	console.log("onMessage " + msg.status + ": " + msg.responseBody);
    if (!msg || !msg.status || msg.status != 200 || !msg.responseBody)
      console.log("invalid message received", msg);
    self.processIncoming(msg.responseBody);
  };
  // ---
  atmoreq.onError = function(e) {
    console.log("saw error " + new Date());
    self.isBroken = true;
    self.sentEstablish = false;
    self.onresponse = {}; // throw away all existing "waiting for response" handlers
    self.reconnecting = setTimeout(function() {
      if (self.isBroken) {
        console.log("attempting to restore connection");
        self.atmo = atmosphere.subscribe(atmoreq);
        self.connect();
        self.isBroken = false;
      }
    }, 2500);
  };
  atmoreq.logLevel = 'debug';
  this.atmo = atmosphere.subscribe(atmoreq);
  this.nextId = 0;
  this.onresponse = {};
  this.dispatch = {};
  this.heartbeatInterval = setInterval(function() {
    if (self.sentEstablish) {
//      console.log("sending heartbeat");
      self.atmo.push(JSON.stringify({"request":{"method":"heartbeat"}}));
    } else
      console.log("timer fired with nothing to do");
  }, Zinc.Config.hbTimeout);
}

Connection.prototype.connect = function() {
  var self = this;
//  console.log("sending establish");
  var msg = {"request":{"method":"establish"}};
  self.atmo.push(JSON.stringify(msg));
  self.sentEstablish = true;
  this.requestors.forEach(function(reqr) {
  	reqr.connected();
  });
}

Connection.prototype.disconnect = function() {
  clearInterval(this.heartbeatInterval);
  atmosphere.unsubscribe();
}

Connection.prototype.nextHandler = function(handler) {
  var ret = ++this.nextId;
  this.dispatch[ret] = handler;
  return ret;
}

Connection.prototype.processIncoming = function(json) {
  var msg = JSON.parse(json);
  if (msg.requestid) {
    if (this.onresponse[msg.requestid]) {
      this.onresponse[msg.requestid](msg);
      delete this.onresponse[msg.requestid];
    }
  } else if (msg.subscription) {
    if (this.onresponse[msg.subscription]) {
      this.onresponse[msg.subscription](msg);
      delete this.onresponse[msg.subscription];
    }
    // Handle ongoing data input
    if (!this.dispatch[msg.subscription]) {
      console.log("received message for closed handle " + msg.subscription);
      return;
    }
    this.dispatch[msg.subscription](msg);
  }
}

/* A Requestor represents a logical connection to a remote endpoint.  Multiple Requestors can be multiplexed across a single physical Connection */
function Requestor(uri) {
  this.uri = uri;
  this.connect = [];
  this.subscriptions = [];
  this.pending = [];
  // this.retryable = []; // this would be for things that need to retry until acknowledged after break
  
  if (connpool[uri]) {
    this.conn = connpool[uri];
    this.conn.requestors.push(this);
  } else {
    this.conn = new Connection(uri);
    connpool[uri] = this.conn;
    this.conn.requestors.push(this);
  }
}

Requestor.prototype.connected = function() {
  var self = this;
  var conn = this.conn;
  this.fullyConnected = false;
  this.delayCount = 0;
  this.connect.forEach(function(r) {
    if (r.wait)
      self.delayCount++;
    var req = r.req;
    if (req instanceof Function)
      req = req(self);
    else
      conn.atmo.push(JSON.stringify(req.msg));
  });
  if (this.delayCount === 0)
    this.resubscribe();
}

Requestor.prototype.delayConnectivity = function() {
  this.delayCount++;
}

Requestor.prototype.advanceConnectivity = function() {
  this.delayCount--;
  if (this.delayCount === 0)
    this.resubscribe();
}

Requestor.prototype.resubscribe = function() {
  var conn = this.conn;
  this.subscriptions.forEach(function(s) {
    conn.atmo.push(JSON.stringify(s.msg));
  });
  this.pending.forEach(function(p) {
    conn.atmo.push(JSON.stringify(p.msg));
  });
  if (this.conn.sentEstablish)
    this.fullyConnected = true;
}

Requestor.prototype.subscribe = function(resource, handler) {
  if (!handler)
    throw "subscribe requires a handler";
  var req = new MakeRequest(this, "subscribe", handler);
  req.req.resource = resource;
  return req;
}

Requestor.prototype.create = function(resource, handler) {
  var req = new MakeRequest(this, "create", handler);
  req.req.resource = resource;
  return req;
}

Requestor.prototype.invoke = function(resource, handler) {
  var req = new MakeRequest(this, "invoke", handler);
  req.req.resource = resource;
  return req;
}

Requestor.prototype.cancelAnySubscriptionTo = function(resource) {
  this.cancelMatchingSubscriptions(function(request) {
    return request.req.resource === resource;
  });
}

Requestor.prototype.cancelAllSubscriptions = function() {
  this.cancelMatchingSubscriptions(function(request) {
    return true;
  });
}

Requestor.prototype.cancelMatchingSubscriptions = function(predicate) {
  var requestsToCancel = [];
  for (var id in this.conn.openSubscriptions)
    if (this.conn.openSubscriptions.hasOwnProperty(id))
    {
      var request = this.conn.openSubscriptions[id];
      if (predicate(request))
        requestsToCancel.push(request);
    }
  for (var request of requestsToCancel)
    request.unsubscribe();
}

Requestor.prototype.sendJson = function(request, id, json) {
  if (this.conn.sentEstablish)
    this.conn.atmo.push(JSON.stringify(json));
  else
    this.pending.push(JSON.stringify(json));
}

// TODO: for the general case, we want to consider that "request" can also be a function.
// In this case, we process request and hold off on doing ANYTHING else until request is completed without any descendants.
// It's not clear what is meant by descendant here.
Requestor.prototype.onConnect = function(request, waitForCompletion) {
  this.connect.push({req: request, wait: waitForCompletion});
  if (this.conn.sentEstablish) {
    if (request instanceof Function)
      request = request(this);
    this.conn.atmo.push(JSON.stringify(request.msg));
  }
}

Requestor.prototype.beginSubscribing = function(request) {
  this.subscriptions.push(request);
  if (this.fullyConnected)
    this.conn.atmo.push(JSON.stringify(request.msg));
}

Requestor.prototype.disconnect = function() {
  this.conn.disconnect();
}

Requestor.prototype.toString = function() {
  return "Requestor[" + this.conn.req.url + "]";
}

function MakeRequest(requestor, method, handler) {
  this.requestor = requestor;
  this.handler = handler;
  this.req = {"method": method};
  this.msg = {"request": this.req};
  if (method === 'subscribe')
    this.msg.subscription = this.requestor.conn.nextHandler(handler);
  this.method = method;
}

MakeRequest.prototype.getHandler = function () {
  return this.handler;
}

MakeRequest.prototype.setOption = function(opt, val) {
  if (!this.req.options)
    this.req.options = {};
  if (this.req.options[opt])
    throw "Option " + opt + " is already set";
  this.req.options[opt] = val;
  return this;
}

MakeRequest.prototype.setPayload = function(json) {
  if (this.msg.payload)
    throw "Cannot set the payload more than once";
  this.msg.payload = json;
  return this;
}

MakeRequest.prototype.send = function() {
  if (this.msg.subscription) {
    this.requestor.beginSubscribing(this);
  } else {
    this.msg.requestid = ++this.requestor.conn.nextId;
    if (this.handler)
      this.requestor.conn.onresponse[this.msg.requestid] = this.handler;
    this.requestor.sendJson(this, this.msg.requestid, this.msg);
  }
}

MakeRequest.prototype.onConnect = function() {
  if (this.msg.subscription)
    throw new Error("Subscriptions are automatically reconnected");
  this.msg.requestid = ++this.requestor.conn.nextId;
    if (this.handler)
      this.requestor.conn.onresponse[this.msg.requestid] = this.handler;
  this.requestor.onConnect(this);
}

MakeRequest.prototype.unsubscribe = function() {
  if (!this.msg.subscription)
    throw "There is no subscription to unsubscribe"
  this.conn.sendJson({subscription: this.msg.subscription, request: {method: "unsubscribe"}});
  delete this.conn.openSubscriptions[this.msg.subscription];
}

Zinc.newRequestor = function(uri) {
  return new Requestor(uri);
}

