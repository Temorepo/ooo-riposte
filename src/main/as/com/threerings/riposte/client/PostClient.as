//
// $Id$
//
// Riposte library - an RPC library for binary data over HTTP POST
// Copyright (C) 2009-2011 Nathan Curtis
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.riposte.client {

import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;
import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.Boxed;
import com.threerings.util.ClassUtil;
import com.threerings.util.Log;
import com.threerings.util.Map;
import com.threerings.util.Maps;

import com.threerings.riposte.data.PostMarshaller;
import com.threerings.riposte.data.StreamableError;

/**
 * The main client class.
 */
public class PostClient
{
    /**
     * Create a new PostClient for processing RPC calls to the Java server.
     *
     * @param serviceAddress The fully qualified HTTP address to connect to the server at.
     * @param version The version string to send along with RPC requests.  If this version is null
     *                an error will be thrown on the server unless the server is also configured
     *                with a null version.
     */
    public function PostClient (serviceAddress :String, version :String = null,
                                useAuthCode :Boolean = false, defaultListener :PostListener = null)
    {
        _serviceAddress = serviceAddress;
        _version = version;
        _useAuthCode = useAuthCode;
        _defaultListener = defaultListener;
    }

    /**
     * Shuts down the PostClient. Any service requests made after shutdown will fail immediately.
     *
     * @param allowQueueToComplete if true, allows the queue of pending service requests to
     * continue processing.
     */
    public function shutdown (allowQueueToComplete :Boolean) :void
    {
        if (!_shutdown) {
            _shutdown = true;
            if (!allowQueueToComplete) {
                _queue = [];
            }
        }
    }

    /**
     * Configure a service marshaller.
     *
     * @param clazz The service class that the marshaller implements.  The service class should be
     *              an extension of PostService.
     * @param marshaller The class that implements the given server.
     */
    public function registerService (clazz :Class, marshaller :PostMarshaller) :void
    {
        if (!ClassUtil.isAssignableAs(PostService, clazz)) {
            throw new ArgumentError(ClassUtil.getClassName(clazz) + " does not extend PostService");
        }
        if (!(marshaller is clazz)) {
            throw new ArgumentError(ClassUtil.getClassName(marshaller) + " does not implement " +
                ClassUtil.getClassName(clazz));
        }

        marshaller.init(this);
        _services.put(clazz, marshaller);
    }

    /**
     * Set the auth code to send with each request, if this PostClient was configured to use it
     * in the constrcutor.
     *
     * @param authCode The auth code to send
     */
    public function setAuthCode (authCode :String) :void
    {
        if (!_useAuthCode) {
            log.warning("This PostClient was not configured to use auth codes, this auth code" +
                " will be ignored");
            return;
        }

        _authCode = authCode;
    }

    /**
     * Get the implementor of the given service.
     */
    public function getService (clazz :Class) :*
    {
        return _services.get(clazz);
    }

    /**
     * Get the implementor of the given service.
     *
     * @throws Error if the service has not been registered with this client.
     */
    public function requireService (clazz :Class) :*
    {
        var isvc :PostService = getService(clazz);
        if (isvc == null) {
            throw new Error(clazz + " isn't available.  I can't bear to go on.");
        }
        return isvc;
    }

    /**
     * This method should only be called by subclasses of PostMarshaller.
     * @private
     */
    public function sendRequest (serviceId :int, methodId :int, args :Array) :void
    {
        var listeners :Array = [];
        for (var ii :int = 0; ii < args.length; ii++) {
            if (args[ii] is PostListener) {
                listeners.push(args.splice(ii, 1)[0]);
                ii--;
            }
        }
        if (listeners.length == 0 && _defaultListener != null) {
            listeners.push(_defaultListener);
        }

        if (_shutdown) {
            listenersFailed(listeners, "PostClient has been shut down");
            return;
        }

        var url :URLRequest = new URLRequest(_serviceAddress);
        url.method = URLRequestMethod.POST;

        var bytes :ByteArray = new ByteArray();
        var oos :ObjectOutputStream = new ObjectOutputStream(bytes);
        oos.writeUTF(_version);
        if (_useAuthCode) {
            oos.writeUTF(_authCode);
        }
        oos.writeInt(serviceId);
        oos.writeInt(methodId);
        oos.writeObject(args);
        url.data = bytes;

        _queue.push(function () :void {
            try {
                var loader :URLLoader = new URLLoader(url);
                function eventListener (event :Event) :void {
                    loader.removeEventListener(Event.COMPLETE, eventListener);
                    loader.removeEventListener(IOErrorEvent.IO_ERROR, eventListener);

                    if (event is IOErrorEvent) {
                        listenersFailed(listeners, (event as IOErrorEvent).text);
                    } else {
                        loaderComplete(listeners, event);
                    }
                    _postIsPending = false;
                    maybeSendNextRequest();

                };
                loader.addEventListener(Event.COMPLETE, eventListener);
                loader.addEventListener(IOErrorEvent.IO_ERROR, eventListener);
                loader.dataFormat = URLLoaderDataFormat.BINARY;

            } catch (err :Error) {
                listenersFailed(listeners, err.toString());
                _postIsPending = false;
                maybeSendNextRequest();
            }
        });
        maybeSendNextRequest();
    }

    protected function maybeSendNextRequest() :void
    {
        if (_postIsPending || _queue.length == 0) {
            return;
        }
        _queue.shift()();
        _postIsPending = true;
    }

    protected function loaderComplete (listeners :Array, event :Event) :void
    {
        var loader :URLLoader = event.target as URLLoader;
        var bytes :ByteArray = loader.data as ByteArray;
        if (bytes == null) {
            log.warning("Return value from RPC call is null");
            listenersFailed(listeners, "Return value from invocation call is null");
            return;
        }

        var ois :ObjectInputStream = new ObjectInputStream(bytes);
        var result :Object;
        try {
            result = ois.readObject();
            if (bytes.position < bytes.length) {
                log.warning(
                    "extra bytes on end of request", "num", (bytes.length - bytes.position));
            }

        } catch (err :Error) {
            log.warning("Error from readObject", err);
            listenersFailed(listeners, err.toString());
            return;
        }

        if (result is StreamableError) {
            listenersFailed(listeners, (result as StreamableError).getCause());

        } else {
            if (result is Boxed) {
                result = Boxed(result).unbox();
            }
            listenersProcessed(listeners, result);
        }
    }

    protected static function listenersProcessed (listeners :Array, result :Object) :void
    {
        for each (var listener :PostListener in listeners) {
            if (listener is PostConfirmListener) {
                (listener as PostConfirmListener).requestProcessed();

            } else if (listener is PostResultListener) {
                (listener as PostResultListener).requestProcessed(result);
            }
        }
    }

    protected static function listenersFailed (listeners :Array, cause :String) :void
    {
        for each (var listener :PostListener in listeners) {
            listener.requestFailed(cause);
        }
    }

    protected var _serviceAddress :String;
    // PostService doesn't extend Hashable, but these are all singletons, so a DictionaryMap
    // is fine.
    protected var _services :Map = Maps.newMapOf(Class);
    protected var _version :String = "";
    protected var _useAuthCode :Boolean;
    protected var _authCode :String = "";
    protected var _queue :Array = [];
    protected var _postIsPending :Boolean;
    protected var _defaultListener :PostListener;
    protected var _shutdown :Boolean;

    private static const log :Log = Log.getLog(PostClient);
}
}
