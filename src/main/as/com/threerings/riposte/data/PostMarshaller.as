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

package com.threerings.riposte.data {

import com.threerings.riposte.client.PostClient;
import com.threerings.riposte.client.PostService;

/**
 * Client-side Marshallers for specific services should extend this class and implement the service
 * in question.  Marshallers will typically be generated automatically by the ant genriposte task.
 */
public class PostMarshaller
    implements PostService
{
    /**
     * Called by the PostClient when this marshaller is registered.
     */
    public function init (client :PostClient) :void
    {
        _client = client;
    }

    /**
     * Posts an RPC request to the server with the given args.  If there are any PostListener
     * args, they are not sent along to the server; instead, they're registered on the client to
     * receive callbacks with the RPC call finishes.
     */
    protected function sendRequest (methodId :int, args :Array) :void
    {
        _client.sendRequest(getServiceId(), methodId, args);
    }

    /**
     * Client-side marshallers are responsible for knowing their own service id, and for reporting
     * it here.
     */
    public function getServiceId () :int
    {
        throw new Error("getServiceId() is abstract in PostMarshaller");
    }

    protected var _client :PostClient;
}
}
