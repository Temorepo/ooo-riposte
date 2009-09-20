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

public class PostCodes
{
    /** This code is dispatched when a service method is not found on the server. */
    public static final String METHOD_NOT_FOUND = "m.method_not_found";

    /** This code is dispatched when the user is not logged in. */
    public static final String USER_NOT_AUTHED = "m.user_not_authed";

    /** This code is dispatched when something broke during streaming. */
    public static final String STREAMING_ERROR = "m.streaming_error";
}
