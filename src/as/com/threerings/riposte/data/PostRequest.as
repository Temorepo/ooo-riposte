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

package com.threerings.riposte.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * An ecnapsulation of service call data, this is the main class to get sent over the wire.  Code
 * that uses Riposte should never create one of these.
 */
public class PostRequest
    implements Streamable
{
    public function PostRequest (serviceId :int, methodId :int, args :Array)
    {
        _serviceId = serviceId;
        _methodId = methodId;
        _args = args;
    }

    public function getServiceId () :int
    {
        return _serviceId;
    }

    public function getMethodId () :int
    {
        return _methodId;
    }

    public function getArgs () :Array
    {
        return _args;
    }

    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(_serviceId);
        out.writeInt(_methodId);
        out.writeField(_args);
    }

    public function readObject (ins :ObjectInputStream) :void
    {
        throw new ArgumentError("PostRequest is for client to server communications only");
    }

    protected var _serviceId :int;
    protected var _methodId :int;
    protected var _args :Array;
}
}
