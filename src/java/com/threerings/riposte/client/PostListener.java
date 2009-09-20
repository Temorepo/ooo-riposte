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

package com.threerings.riposte.client;

/**
 * All Riposte service calls receive a PostListener optional argument at the end to allow the
 * calling entity to find out about return values and failures.<br/><br/>
 *
 * All exceptions through by service methods are caught, with the message sent along the wire to
 * requestFailed.
 */
public interface PostListener
{
    /**
     * Called to report request failure.  If the post services system detects failure of any kind.
     * it will report it via this callback.  Particular services may also make use of this
     * callback to report failures of their own, or they may opt to define more specific failure
     * callbacks.
     */
    void requestFailed (String cause);
}
