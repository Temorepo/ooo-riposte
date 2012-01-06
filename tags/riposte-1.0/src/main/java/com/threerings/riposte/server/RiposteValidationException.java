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

package com.threerings.riposte.server;

import com.samskivert.util.Logger;

/**
 * Thrown when a riposte method fails in processing.
 */
public class RiposteValidationException extends Exception
{
    /**
     * Creates a RiposteValidationException with the given message as formatted by
     * {@link Logger#format}. If the final element in <code>args</code> is an instance of
     * Throwable, it will be set as this exception's initial cause.
     */
    public RiposteValidationException (Object message, Object... args)
    {
        super(Logger.format(message, args));
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            initCause((Throwable)args[args.length - 1]);
        }
    }
}