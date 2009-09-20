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

package com.threerings.riposte.client {

public class PostAdapter
    implements PostListener
{
    /**
     * Construct an PostAdapter that will call the specified
     * function on error.
     */
    public function PostAdapter (failedFunc :Function)
    {
        _failedFunc = failedFunc;
    }

    // documentation inherited from interface InvocationListener
    public function requestFailed (cause :String) :void
    {
        _failedFunc(cause);
    }

    /** The Function to call when we've recevied our failure response. */
    protected var _failedFunc :Function;
}
}
