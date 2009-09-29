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

package com.threerings.riposte.data;

import com.threerings.io.Streamable;

/**
 * An encapsulation of service call data, this is the main class to get sent over the wire.  Code
 * that uses Riposte should never create one of these.
 */
public class PostRequest
    implements Streamable
{
    /**
     * Get the id of the service being used in this request.
     *
     * @return the service id.
     */
    public int getServiceId ()
    {
        return _serviceId;
    }

    /**
     * Get the id of the method call being requested.
     *
     * @return the method id.
     */
    public int getMethodId ()
    {
        return _methodId;
    }

    /**
     * Get the args of the method sent from the client.  All args must either be basic Java types,
     * or must implement {@link com.threerings.io.Streamable}.
     *
     * @return the args for this defined RPC call
     */
    public Object[] getArgs ()
    {
        return _args;
    }

    @Override
    public String toString ()
    {
        return "PostRequest [service=" + _serviceId + ", method=" + _methodId + ", args=" +
            _args + "]";
    }

    protected int _serviceId;
    protected int _methodId;
    protected Object[] _args;
}
