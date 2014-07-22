EventTypes = {
    RECEIVE_VALUE : 0,
    RECEIVE_EXCEPTION : 1,
    RECEIVE_JAVA_CLASS_INFO : 2,
    CREATE_ARRAY : 3,
    INVOKE_METHOD : 5,
    INSTANTIATE_CLASS : 6,
    GET_JAVA_CLASS_INFO : 7,
    GET_PROPERTY : 8,
    SET_PROPERTY : 9
}
ValueTypes = {
    OBJECT : 0,
    GLOBAL : 1,
    NUMBER : 2,
    INTEGER : 3,
    STRING : 4,
    BOOLEAN : 5,
    UNDEFINED : 6,
    NULL : 7,
    JAVA_OBJECT : 8
}

var connection = null;
var objects = {};
var objectIds = {};
var objectIdGenerator = 0;
var javaObjects = {};
var debuggee = null;
var globalObjectId = null;
var teavmId = null;

chrome.browserAction.onClicked.addListener(function(tab) {
    injectContentScript(tab);
    attachDebugger(tab);
});
function connectToServer() {
    connection = new WebSocket("ws://localhost:8888/");
    connection.binaryType = "arraybuffer";
    connection.onmessage = function(event) {
        receiveMessage(event.data);
    }
}
function injectContentScript(tab) {
    chrome.tabs.executeScript(tab.id, { file: "content.js" });
}
function attachDebugger(tab) {
    debuggee = { tabId : tab.id };
    chrome.debugger.attach(debuggee, "1.0", (function(callback) {
        chrome.debugger.sendCommand(debuggee, "Debugger.enable", {}, callback);
    }).bind(null, (function(callback) {
        chrome.debugger.sendCommand(debuggee, "Runtime.evaluate", { expression : "window" },
                function(response) {
            globalObjectId = response.result.objectId;
            callback && callback();
        });
    }).bind(null, (function(callback) {
        chrome.debugger.sendCommand(debuggee, "Runtime.evaluate",
                { expression : "window.__TeaVM_remote__" }, function(response) {
            teavmId = response.result.objectId;
            callback && callback();
        });
    }).bind(null, connectToServer))));
}

chrome.debugger.onEvent.addListener(function(source, method, params) {
    switch (method) {
        case "Debugger.paused":
            onPause(source);
            break;
    }
});

function receiveMessage(data) {
    var input = new InputStream(new Int8Array(data));
    switch (input.readByte()) {
        case EventTypes.INVOKE_METHOD:
            receiveInvokeMethodMessage(input);
            break;
        case EventTypes.GET_PROPERTY:
            receiveGetPropertyMessage(input);
            break;
    }
}
function receiveInvokeMethodMessage(input) {
    var valueReader = new ValueReader(input);
    var args = [];
    var messageId = input.readInt();
    args.push(valueReader.read());
    args.push(valueReader.read());
    var argsCount = input.readByte();
    for (var i = 0; i < argsCount; ++i) {
        args.push(valueReader.read());
    }
    Future.resolveAll(args, function(args) {
        var request = {
            objectId : teavmId,
            functionDeclaration : "__TeaVM_remote__.callMethod",
            arguments : args,
            returnByValue : false };
        chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request,
                function(response) {
            sendValueBack(messageId, response);
        });
    });
}
function receiveGetPropertyMessage(input) {
    var valueReader = new ValueReader(input);
    var messageId = input.readInt();
    var args = [valueReader.read(), valueReader.read()];
    Future.resolveAll(args, function(args) {
        var request = {
            objectId : teavmId,
            functionDeclaration : "__TeaVM_remote__.getProperty",
            arguments : args,
            returnByValue : false };
        chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request,
                function(response) {
            sendValueBack(messageId, response);
        });
    });
}
function sendValueBack(messageId, response) {
    var out = new OutputStream();
    var valueWriter = new ValueWriter(out);
    if (!response.wasThrown) {
        out.writeByte(EventTypes.RECEIVE_VALUE);
        out.writeInt(messageId);
        valueWriter.write(response.result);
    } else {
        out.writeByte(EventTypes.RECEIVE_EXCEPTION);
        out.writeInt(messageId);
        out.writeUTF8(response.result.description);
    }
    connection.send(out.getData());
}

function InputStream(data) {
    this.data = data;
    this.index = 0;
}
InputStream.prototype.readByte = function() {
    return this.data[this.index++];
}
InputStream.prototype.readShort = function() {
    var a = this.data[this.index++];
    var b = this.data[this.index++];
    var r = ((a & 0xFF) << 8) | (b & 0xFF);
    return (r << 16) >> 16;
}
InputStream.prototype.readInt = function() {
    var a = this.data[this.index++];
    var b = this.data[this.index++];
    var c = this.data[this.index++];
    var d = this.data[this.index++];
    return ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((c & 0xFF) << 8) | (d & 0xFF);
}
InputStream.prototype.readNumber = function() {
    var data = new Float64Array(this.data, this.index, 8);
    var value = data[0];
    this.index += 8;
    return value;
}
InputStream.prototype.readUTF8 = function() {
    var length = this.readShort();
    var str = "";
    for (var i = 0; i < length; ++i) {
        var code;
        var b = this.readByte();
        if ((b & 0x80) == 0x00) {
            code = b;
        } else if ((b & 0xE0) == 0xC0) {
            code = ((b & 0x1F) << 6) | (this.readByte() & 0x3F);
        } else if ((b & 0xF0) == 0xE0) {
            code = ((b & 0x1F) << 12) | ((this.readByte() & 0x3F) << 6) | (this.readByte() & 0x3F);
        }
        str += String.fromCharCode(code);
    }
    return str;
};

function OutputStream() {
    this.buffer = new ArrayBuffer(64);
    this.data = new Int8Array(this.buffer);
    this.index = 0;
}
OutputStream.prototype.alloc = function(size) {
    if (this.index + size >= this.data.length) {
        var newSize = Math.max((this.index + size) * 2, this.data.length);
        this.buffer = new ArrayBuffer(newSize);
        var newData = new Int8Array(this.buffer);
        for (var i = 0; i < index; ++i) {
            newData[i] = this.data[i];
        }
        this.data = newData;
    }
}
OutputStream.prototype.writeByte = function(value) {
    this.alloc(1);
    this.data[this.index++] = value;
}
OutputStream.prototype.writeShort = function(value) {
    this.alloc(2);
    this.data[this.index++] = (value >> 8) & 0xFF;
    this.data[this.index++] = (value >> 0) & 0xFF;
}
OutputStream.prototype.writeInt = function(value) {
    this.alloc(4);
    this.data[this.index++] = (value >> 24) & 0xFF;
    this.data[this.index++] = (value >> 16) & 0xFF;
    this.data[this.index++] = (value >> 8) & 0xFF;
    this.data[this.index++] = (value >> 0) & 0xFF;
}
OutputStream.prototype.writeNumber = function(value) {
    this.alloc(8);
    var data = new Float64Array(this.buffer, this.index, 8);
    data[0] = value;
    this.index += 8;
}
OutputStream.prototype.writeUTF8 = function(value) {
    var length = 2;
    for (var i = 0; i < value.length; ++i) {
        var c = value.charCodeAt(i);
        if (c < 0x0080) {
            length += 1;
        } else if (c < 0x0800) {
            length += 2;
        } else {
            length += 3;
        }
    }
    this.alloc(length);
    this.data[this.index++] = (value.length >> 8) & 0xFF;
    this.data[this.index++] = (value.length >> 0) & 0xFF;
    for (var i = 0; i < value.length; ++i) {
        var c = value.charCodeAt(i);
        if (c < 0x0080) {
            this.data[this.index++] = c & 0x7F;
        } else if (c < 0x0800) {
            this.data[this.index++] = 0xC0 | ((c >> 6) & 0x1F);
            this.data[this.index++] = 0x80 | (c & 0x3F);
        } else {
            this.data[this.index++] = 0xC0 | ((c >> 12) & 0x1F);
            this.data[this.index++] = 0x80 | ((c >> 6) & 0x3F);
            this.data[this.index++] = 0x80 | (c & 0x3F);
        }
    }
}
OutputStream.prototype.getData = function() {
    var resultBuffer = new ArrayBuffer(this.index);
    var resultArray = new Int8Array(resultBuffer);
    for (var i = 0; i < this.index; ++i) {
        resultArray[i] = this.data[i];
    }
    return resultBuffer;
}

function Future(value) {
    if (value === undefined) {
        this.value = null;
        this.error = null;
        this.calculated = false;
        this.callbacks = [];
    } else {
        this.value = value;
        this.error = null;
        this.calculated = true;
        this.callbacks = null;
    }
}
Future.prototype.resolve = function(callback, error) {
    if (this.calculated) {
        if (!this.error) {
            callback(this.value);
        } else {
            error(this.error);
        }
    } else {
        this.callbacks.push({ callback : callback, error : error });
    }
}
Future.prototype.set = function(value) {
    if (this.calculated) {
        throw "Value already calculated";
    }
    this.value = value;
    this.calculated = true;
    for (var i = 0; i < this.callbacks.length; ++i) {
        this.callbacks[i].callback(value);
    }
    this.callbacks = null;
}
Future.prototype.error = function(error) {
    if (this.calculated) {
        throw "Value already calculated";
    }
    this.error = error;
    this.calculated = true;
    for (var i = 0; i < this.callbacks.length; ++i) {
        var errorHandler = this.callbacks[i].error;
        if (errorHandler) {
            errorHandler(error);
        }
    }
    this.callbacks = null;
}
Future.resolveAll = function(futures, callback, error) {
    var left = futures.length;
    var values = [];
    var stopped = false;
    for (var i = 0; i < futures.length; ++i) {
        values.push(null);
        futures[i].resolve((function(index) {
            return function(value) {
                if (stopped) {
                    return;
                }
                values[index] = value;
                if (--left == 0) {
                    callback(values);
                }
            };
        })(i), function(err) {
            if (stopped) {
                return;
            }
            stopped = true;
            error(err);
        });
    }
}

function ValueReader(input) {
    this.input = input;
}
ValueReader.prototype.read = function() {
    switch (this.input.readByte()) {
        case ValueTypes.BOOLEAN:
            return new Future({ value : this.input.readByte() == 1 });
        case ValueTypes.GLOBAL:
            return new Future({ objectId : globalObjectId });
        case ValueTypes.INTEGER:
            return new Future({ value : this.input.readInt() });
        case ValueTypes.JAVA_OBJECT:
            return this.readJavaObject();
        case ValueTypes.NULL:
            return new Future({ value : null });
        case ValueTypes.NUMBER:
            return new Future({ value : this.input.readNumber() });
        case ValueTypes.OBJECT:
            return new Future({ objectId : objects[this.input.readInt()] });
        case ValueTypes.STRING:
            return new Future({ value : this.input.readUTF8() });
        case ValueTypes.UNDEFINED:
            return new Future({ value : undefined });
    }
}
ValueReader.prototype.readJavaObject = function() {
    var f = new Future();
    f.error("Java object not supported yet");
}

function ValueWriter(output) {
    this.output = output;
}
ValueWriter.prototype.write = function(value) {
    if (value.value) {
        if (value.value === null) {
            this.output.writeByte(ValueTypes.NULL);
            return;
        }
        switch (typeof value.value) {
            case "boolean":
                this.output.writeByte(ValueTypes.BOOLEAN);
                this.output.writeByte(value ? 1 : 0);
                break;
            case "number":
                if (value == (value | 0)) {
                    this.output.writeByte(ValueTypes.INTEGER);
                    this.output.writeInt(value);
                } else {
                    this.output.writeByte(ValueTypes.NUMBER);
                    this.output.writeNumber(value);
                }
                break;
            case "undefined":
                this.output.writeByte(ValueTypes.UNDEFINED);
                break;
            case "string":
                this.output.writeByte(ValueTypes.STRING);
                this.output.writeUTF8(value);
                break;
        }
    } else {
        this.writeObject(value.objectId);
    }
}
ValueWriter.prototype.writeObject = function(objectId) {
    if (objectId == globalObjectId) {
        this.output.writeByte(ValueTypes.GLOBAL);
    } else {
        this.output.writeByte(ValueTypes.OBJECT);
        var integerId = objectIds[objectId];
        if (integerId === undefined) {
            integerId = ++objectIdGenerator;
            objectIds[objectId] = integerId;
            objects[integerId] = objectId;
        }
        this.output.writeInt(integerId);
    }
}