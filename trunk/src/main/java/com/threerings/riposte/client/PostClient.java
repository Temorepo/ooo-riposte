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

package com.threerings.riposte.client;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.riposte.data.StreamableError;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.threerings.riposte.Log.log;

/**
 * The main client class.
 */
public class PostClient
{
    public PostClient (String serviceAddress)
    {
        this(serviceAddress, null, false, null);
    }

    /**
     * Create a new PostClient for processing RPC calls to the Java server.
     *
     * @param serviceAddress The fully qualified HTTP address to connect to the server at.
     * @param version The version string to send along with RPC requests.  If this version is null
     *                an error will be thrown on the server unless the server is also configured
     *                with a null version.
     */
    public PostClient (String serviceAddress, String version, boolean useAuthCode,
        PostListener defaultListener)
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
    public void shutdown (boolean allowQueueToComplete)
    {
        if (!_shutdown) {
            _shutdown = true;
            if (!allowQueueToComplete) {
                _queue.clear();
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
    public <T extends PostService> void registerService (Class<T> clazz, T marshaller)
    {
        marshaller.init(this);
        _services.put(clazz, marshaller);
    }

    /**
     * Configure a batch of service marshallers.
     *
     * @param services A map of PostService extension interface to the marshaller that implements
     *                 it.
     */
    public void registerServices (Map<Class<? extends PostService>, PostService> services)
    {
        for (Map.Entry<Class<? extends PostService>, PostService> entry : services.entrySet()) {
            Class<? extends PostService> service = entry.getKey();
            PostService marshaller = entry.getValue();
            if (service.isInstance(marshaller)) {
                marshaller.init(this);
                _services.put(service, marshaller);
            }
        }
    }

    /**
     * Set the auth code to send with each request, if this PostClient was configured to use it
     * in the constrcutor.
     *
     * @param authCode The auth code to send
     */
    public void setAuthCode (String authCode)
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
    @SuppressWarnings("unchecked")
    public <T extends PostService> T getService (Class<T> clazz)
    {
        return (T) _services.get(clazz);
    }

    /**
     * Get the implementor of the given service.
     *
     * @throws Error if the service has not been registered with this client.
     */
    public <T extends PostService> T requireService (Class<T> clazz)
    {
        T marshaller = getService(clazz);
        if (marshaller == null) {
            throw new RuntimeException(clazz + " isn't available.  I can't bear to go on.");
        }
        return marshaller;
    }

    /**
     * This method should only be called by subclasses of PostMarshaller.
     * @private
     */
    public void sendRequest (int serviceId, int methodId, Object[] args)
    {
        final List<PostListener> listeners = Lists.newArrayList();
        List<Object> others = Lists.newArrayList();
        for (Object arg : args) {
            if (arg instanceof PostListener) {
                listeners.add((PostListener)arg);
            } else {
                others.add(arg);
            }
        }
        if (listeners.size() == 0 && _defaultListener != null) {
            listeners.add(_defaultListener);
        }

        if (_shutdown) {
            listenersFailed(listeners, "PostClient has been shut down");
            return;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        try {
            oos.writeUTF(_version);
            if (_useAuthCode) {
                oos.writeUTF(getAuthCode(serviceId, methodId, args));
            }
            oos.writeInt(serviceId);
            oos.writeInt(methodId);
        } catch (IOException ioe) {
            log.warning("Exception encountered writing pre-args values", ioe);
        }
        final byte[] bytes = bos.toByteArray();

        try {
            oos.writeObject(others.toArray(new Object[others.size()]));
        } catch (IOException ioe) {
            log.warning("Exception encountered writing args", ioe);
        }

        _queue.add(new Runnable() {
            public void run () {
                try {
                    HttpURLConnection conn =
                        (HttpURLConnection) new URL(_serviceAddress).openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    BufferedOutputStream out =
                        new BufferedOutputStream(conn.getOutputStream());
                    out.write(bytes);
                    out.flush();
                    out.close();

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    InputStream in = conn.getInputStream();
                    int count;
                    byte[] buf = new byte[1024];
                    while (-1 < (count = in.read(buf))) {
                        bos.write(buf, 0, count);
                    }
                    requestComplete(listeners, bos.toByteArray());

                } catch (IOException ioe) {
                    log.warning("IOException processing request", ioe);
                    listenersFailed(listeners, ioe.getMessage());

                } finally {
                    _postIsPending = false;
                    maybeSendNextRequest();
                }
            }
        });
        maybeSendNextRequest();
    }

    protected String getAuthCode (int serviceId, int methodId, Object[] args)
    {
        return _authCode;
    }

    protected void maybeSendNextRequest ()
    {
        if (_postIsPending || _queue.size() == 0) {
            return;
        }
        _postIsPending = true;
        _queue.remove(0).run();
    }

    protected void requestComplete (List<PostListener> listeners, byte[] bytes)
    {
        if (bytes == null) {
            log.warning("Return value from RPC call is null");
            listenersFailed(listeners, "Return value from invocation call is null");
            return;
        }

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object result;

        try {
            result = ois.readObject();

        } catch (ClassNotFoundException e) {
            log.warning("Exception from readObject", e);
            listenersFailed(listeners, e.getMessage());
            return;
        } catch (IOException e) {
            log.warning("Exception from readObject", e);
            listenersFailed(listeners, e.getMessage());
            return;
        }

        if (result instanceof StreamableError) {
            listenersFailed(listeners, ((StreamableError)result).getCause());
        } else {
            listenersProcessed(listeners, result);
        }
    }

    // TODO: perhaps find a way to type this. Might need to add a Class<Foo> instance to the
    // call structure for service methods so we can typecheck the result here. At least if a person
    // sticks to generated code, the implicit casts should be safe.
    @SuppressWarnings("unchecked")
    protected static void listenersProcessed (List<PostListener> listeners, Object result)
    {
        for (PostListener listener : listeners) {
            if (listener instanceof PostResultListener<?>) {
                ((PostResultListener)listener).requestProcessed(result);
            }
        }
    }

    protected static void listenersFailed (List<PostListener> listeners, String cause)
    {
        for (PostListener listener : listeners) {
            listener.requestFailed(cause);
        }
    }

    protected String _serviceAddress;
    protected Map<Class<? extends PostService>, PostService> _services = Maps.newHashMap();
    protected String _version = "";
    protected boolean _useAuthCode;
    protected String _authCode = "";
    protected List<Runnable> _queue = Lists.newArrayList();
    protected boolean _postIsPending;
    protected PostListener _defaultListener;
    protected boolean _shutdown;
}
