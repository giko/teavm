EventTypes = {
    RECEIVE_VALUE : 0,
    INVOKE_METHOD : 5,
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
    chrome.debugger.attach(debuggee, "1.1", (function(callback) {
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
    var request = {
        objectId : teavmId,
        functionDeclaration : "__TeaVM_remote__.callMethod",
        arguments : args,
        returnByValue : false };
    chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
        sendValueBack(messageId, response);
    });
}
function receiveGetPropertyMessage(input) {
    var valueReader = new ValueReader(input);
    var messageId = input.readInt();
    var args = [valueReader.read(), valueReader.read()];
    var request = {
        objectId : teavmId,
        functionDeclaration : "__TeaVM_remote__.getProperty",
        arguments : args,
        returnByValue : false };
    chrome.debugger.sendCommand(debuggee, "Runtime.callFunctionOn", request, function(response) {
        sendValueBack(messageId, response);
    });
}
function sendValueBack(messageId, response) {
    var out = new OutputStream();
    var valueWriter = new ValueWriter(out);
    out.writeByte(EventTypes.RECEIVE_VALUE);
    out.writeInt(messageId);
    valueWriter.write(response.result);
    connection.send(out.getData());
    // TODO: add exception handling
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

function ValueReader(input) {
    this.input = input;
}
ValueReader.prototype.read = function() {
    switch (this.input.readByte()) {
        case ValueTypes.BOOLEAN:
            return { value : this.input.readByte() == 1 };
        case ValueTypes.GLOBAL:
            return { objectId : globalObjectId };
        case ValueTypes.INTEGER:
            return { value : this.input.readInt() };
        case ValueTypes.JAVA_OBJECT:
            throw "Java objects are not supported yet";
        case ValueTypes.NULL:
            return { value : null };
        case ValueTypes.NUMBER:
            return { value : this.input.readNumber() };
        case ValueTypes.OBJECT:
            return { objectId : objects[this.input.readInt()] };
        case ValueTypes.STRING:
            return { value : this.input.readUTF8() };
        case ValueTypes.UNDEFINED:
            return { value : undefined };
    }
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