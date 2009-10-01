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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.samskivert.util.ObjectUtil;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.riposte.data.PostCodes;
import com.threerings.riposte.data.StreamableError;

import static com.threerings.riposte.Log.log;

/**
 * The main server-side class, PostManager is responsible for parsing requests from clients and
 * seeing that the proper {@link PostDispatcher} is notified of the service call.
 */
@Singleton
public class PostManager
{
    /** The name used in the @Named annotation for the constructor's injected dispatcher map. */
    public static final String DISPATCHERS = "RiposteDispatchers";

    /** The name used in the @Named annotation for the constructor's injected client version */
    public static final String CLIENT_VERSION = "RiposteClientVersion";

    /**
     * Constructor for PostManager. PostManager may be Guice injected, or can be constructed by
     * hand. If it is created by Guice, the protected _injector field will be injected by Guice, and
     * then every service method argument will have any transient {@link Inject} members added by
     * Guice after the object has been created.
     *
     * @param dispatchers A mapping of service id to the {@link PostDispatcher} that's responsible
     *                       for handling that service's RPC calls.
     * @param clientVersion {@link PostRequest} will send along a client-defined version string with
     *                         every request. If the client and server version strings don't match, a
     *                      {@link PostException} is called to notify the client that it is out of
     *                      date. If versioning is not required, client and server can both use null
     *                      for the version string.
     */
    @Inject
    public PostManager (@Named(DISPATCHERS) Map<Integer, PostDispatcher> dispatchers,
                        @Named(CLIENT_VERSION) String clientVersion)
    {
        _clientVersion = clientVersion;
        if (dispatchers == null) {
            _dispatchers = Maps.newHashMap();
        } else {
            _dispatchers = Maps.newHashMap(dispatchers);
        }
    }

    /**
     * Called by the servlet that is receiving Riposte RPC calls with the raw
     * {@link HttpServletRequest} and {@link HttpServletResponse}.  The service call data is read
     * from the {@link HttpServletRequest}, which is assumed to be binary data encapsulated in a
     * {@link PostRequest} at the top level.  The method response is written to the
     * {@link HttpServletResponse}, or a {@link StreamableError} is sent if an exception occurred.
     * If * the exception is a {@link PostException}, it is not logged on the server.  All other
     * Exceptions are both sent to the client and logged as warnings with the Riposte logger,
     * {@link com.threerings.riposte.Log}.
     *
     * @param req The servlet request.
     * @param rsp The servlet response.
     *
     * @throws IOException
     */
    public void doServiceCall (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        ObjectInputStream ois = new ObjectInputStream(req.getInputStream());
        ObjectOutputStream oos = new ObjectOutputStream(rsp.getOutputStream());

        try {
            sendResult(processServiceCall(ois), oos);

        } catch (PostException pe) {
            sendException(pe, oos);

        } catch (Exception e) {
            // accepting any exception so that Riposte can share services with
            // GWT
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
        int serviceId;
        int methodId;
        try {
            // require that they either both be null or that they .equals()
            String version = (String)ois.readObject();
            if (!ObjectUtil.equals(_clientVersion, version)) {
                log.warning("Version mismatch from client", "required", _clientVersion,
                    "supplied", version);
                throw new PostException(PostCodes.VERSION_MISMATCH);
            }

            serviceId = ois.readInt();
            methodId = ois.readInt();
        } catch (IOException ioe) {
            log.warning("Exception encountered streaming the pre-args values", ioe);
            throw new PostException(PostCodes.STREAMING_ERROR);
        } catch (ClassNotFoundException cnfe) {
            log.warning("Exception encountered streaming the pre-args values", cnfe);
            throw new PostException(PostCodes.STREAMING_ERROR);
        }

        Object[] args;
        try {
            args = (Object[]) ois.readObject();
        } catch (Exception e) {
            log.warning("Exception encountered streaming the args", "serviceId", serviceId,
                "methodId", methodId, e);
            throw new PostException(PostCodes.STREAMING_ERROR);
        }

        if (ois.available() != 0) {
            log.warning("PostRequest has extra bytes", "extra",
                    ois.available(), "serviceId", serviceId, "methodId", methodId);
            throw new PostException(PostCodes.STREAMING_ERROR);
        }

        PostDispatcher dispatcher = _dispatchers.get(serviceId);
        if (dispatcher == null) {
            log.warning("Dispatcher not found for service", "serviceId", serviceId);
            throw new PostException(PostCodes.STREAMING_ERROR);
        }

        if (_injector != null) {
            // Inject members into the args that were streamed over the wire, only if we're running
            // in a Guice environment (_injector will be null otherwise)
            for (Object arg : args) {
                _injector.injectMembers(arg);
            }
        }

        return dispatcher.dispatchRequest(methodId, args);
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

    protected Map<Integer, PostDispatcher> _dispatchers;
    protected String _clientVersion;

    @Inject
    protected Injector _injector;
}
