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
 * A Streamable class to encapsulate an error that occurred on the server.  Service methods should
 * not create one of these - Any exception thrown from a service method will result in a
 * StreamableError being created and sent back to the client.
 */
public class StreamableError
    implements Streamable
{
    public StreamableError ()
    {
        // Zero-arg used in deserialization
    }

    public StreamableError (String cause)
    {
        _cause = cause;
    }

    /**
     * Get the cause of this error.  Usually, this is Exception.getMessage();
     *
     * @return the cause.
     */
    public String getCause ()
    {
        return _cause;
    }

    public void setCause (String cause)
    {
        _cause = cause;
    }

    protected String _cause;
}
