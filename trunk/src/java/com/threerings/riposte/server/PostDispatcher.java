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

package com.threerings.riposte.server;

/**
 * Riposte Service dispatchers must implement this method.  Given the method id and args array,
 * they are responsible for calling the correct service method on the Service implementor.<br/><br/>
 *
 * The implementors of these interfaces are not usually written by hand - they are usually generated
 * by the genriposte ant task, defined in {@link com.threerings.riposte.tools.GenRiposteTask}.  If
 * this method is used, the service interface is required to have a service id defined in a public
 * static string:<br/><br/>
 *
 * {@code public static final int SERVICE_ID = 1;}
 */
public interface PostDispatcher
{
    /**
     * Called by the RiposteManager when a service call for this service arrives at the server.
     *
     * @param methodId The id of the method being called.  The dispatcher is responsible for
     *                 maintaining a mapping of method id to service method.
     * @param args The args for the method being called.
     *
     * @return If this method returns a value, return it here.  If not, return null.
     *
     * @throws Exception If the service method throws an exception, let it pass through here.
     *                   {@link PostManager} will catch the exception and send a
     *                   {@link com.threerings.riposte.data.StreamableError} to the client with the
     *                   exception's message.
     */
    public Object dispatchRequest (int methodId, Object[] args)
        throws Exception;

    /**
     * Called by RiposteManager to increase the accuracy of error logs in a generic
     * catch (Exception) block
     */
    public String getMethodName (int methodId);
}
