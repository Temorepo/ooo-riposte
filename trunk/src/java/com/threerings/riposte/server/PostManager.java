//
// $Id$
//
// Riposte library - an RPC library for binary data over HTTP POST
// Copyright (C) 2009 Nathan Curtis
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

package com.threerings.riposte.server;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.riposte.data.PostCodes;
import com.threerings.riposte.data.PostRequest;
import com.threerings.riposte.data.StreamableError;

import static com.threerings.riposte.Log.log;

@Singleton
public class PostManager
{
    @Inject
    public PostManager (Map<Integer, PostDispatcher> dispatchers,
        @Named("ClientVersion") String clientVersion)
    {
        _clientVersion = clientVersion;
        if (dispatchers == null) {
            _dispatchers = Maps.newHashMap();
        } else {
            _dispatchers = Maps.newHashMap(dispatchers);
        }
    }

    public void registerDispatcher (int serviceId, PostDispatcher dispatcher)
    {
        _dispatchers.put(serviceId, dispatcher);
    }

    public void doServiceCall (ObjectInputStream ois, ObjectOutputStream oos)
        throws IOException
    {
        try {
            sendResult(processServiceCall(ois), oos);

        } catch (PostException pe) {
            sendException(pe, oos);

        } catch (Exception e) {
            // accepting any exception so that Riposte can share services with GWT
            log.error("doServiceCall failure", e);
            sendException(e, oos);

        } finally {
            ois.close();
            oos.close();
        }
    }

    protected Object processServiceCall (ObjectInputStream ois)
        throws Exception
    {
        PostRequest request = new PostRequest();
        try {
            ois.readBareObject(request);
        } catch (Exception e) {
            log.warning("Exception encountered streaming the PostRequest", e);
            throw new PostException(PostCodes.STREAMING_ERROR);
        }

        if (ois.available() != 0) {
            log.warning("PostRequest has extra bytes", "extra", ois.available(), "request",
                request);
            throw new PostException(PostCodes.STREAMING_ERROR);
        }

        // require that they either both be null or that they .equals()
        String reqVersion = request.getVersion();
        if ((_clientVersion == null && reqVersion != null) || !_clientVersion.equals(reqVersion)) {
            log.warning("Version mismatch from client", "required", _clientVersion, "supplied",
                reqVersion);
            throw new PostException(VERSION_MISMATCH);
        }

        int serviceId = request.getServiceId();
        PostDispatcher dispatcher = _dispatchers.get(serviceId);
        if (dispatcher == null) {
            log.warning("Dispatcher not found for service", "serviceId", serviceId);
            throw new PostException(PostCodes.STREAMING_ERROR);
        }

        Object[] args = request.getArgs();
        if (_injector != null) {
            // Inject members into the args that were streamed over the wire, only if we're running
            // in a Guice environment (_injector will be null otherwise)
            for (Object arg : args) {
                _injector.injectMembers(arg);
            }
        }

        return dispatcher.dispatchRequest(request.getMethodId(), args);
    }

    protected void sendResult (Object result, ObjectOutputStream oos)
        throws IOException
    {
        oos.writeObject(result);
    }

    protected void sendException (Exception e, ObjectOutputStream oos)
        throws IOException
    {
        oos.writeObject(new StreamableError(e.getMessage()));
    }

    protected static final String VERSION_MISMATCH =
        "You are connecting with an out of date client.  Please refresh and try again.";

    protected Map<Integer, PostDispatcher> _dispatchers;
    protected String _clientVersion;

    @Inject protected Injector _injector;
}
