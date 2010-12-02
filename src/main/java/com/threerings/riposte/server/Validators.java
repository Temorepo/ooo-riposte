package com.threerings.riposte.server;

public class Validators
{
    public static void checkNotNull (Object value, String message, Object... args)
        throws RiposteValidationException
    {
        check(value != null, message, args);
    }

    /**
     * Throws a RiposteValidationException with the given message unless condition is true.
     */
    public static void check (boolean condition, String message, Object... args)
        throws RiposteValidationException
    {
        if (!condition) {
            throw new RiposteValidationException(message, args);
        }
    }

}
