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

public class StreamableError
    implements Streamable
{
    public function getCause () :String
    {
        return _cause;
    }

    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error("StreamableError is only used from server to client.");
    }

    public function readObject (ins :ObjectInputStream) :void
    {
        _cause = ins.readField(String) as String;
    }

    protected var _cause :String;
}
}
