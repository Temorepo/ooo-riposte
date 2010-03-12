package com.threerings.riposte.server;

import com.samskivert.util.Logger;

/**
 * Thrown when a riposte method fails in processing.
 */
public class RiposteException extends Exception
{
    /**
     * Creates a RiposteException with the given message as formatted by {@link Logger#format}.  If
     * the final element in <code>args</code> is an instance of Throwable, it will be set as this
     * exception's initial cause.
     */
    public RiposteException (Object message, Object... args)
    {
        super(Logger.format(message, args));
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            initCause((Throwable)args[args.length - 1]);
        }
    }

    /**
     * Throws a RiposteException with the given message unless condition is true.
     */
    public static void unless (boolean condition, Object message, Object... args)
        throws RiposteException
    {
        if (!condition) {
            throw new RiposteException(message, args);
        }
    }
}
