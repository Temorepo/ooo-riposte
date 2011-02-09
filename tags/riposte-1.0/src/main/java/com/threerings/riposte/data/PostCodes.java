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

package com.threerings.riposte.data;

/**
 * A collection of translation strings sent when things go wrong at a low level.
 */
public class PostCodes
{
    /** This code is dispatched when something broke during streaming. */
    public static final String STREAMING_ERROR = "e.streaming_error";

    /** This code is dispatched when there is a version mismatch between client and server. */
    public static final String VERSION_MISMATCH = "e.version_mismatch";

    /** This code is dispatched when a method was not found. */
    public static final String METHOD_NOT_FOUND = "e.method_not_found";
}
