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

package com.threerings.riposte.tools;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.velocity.VelocityContext;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.internal.Lists;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.GenUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.tools.ImportSet;
import com.threerings.presents.tools.InvocationTask;

import com.threerings.riposte.data.PostCodes;
import com.threerings.riposte.server.PostDispatcher;
import com.threerings.riposte.server.PostException;

/**
 * The ant task that will handle a bunch of code generation to make using Riposte much easier.
 */
public class GenRiposteTask extends InvocationTask
{
    public class PostServiceMethod extends ServiceMethod
    {
        public PostServiceMethod (Method method, ImportSet imports)
        {
            super(method, imports);

            Type[] types = method.getGenericParameterTypes();
            for (int ii = 0; ii < types.length; ii++) {
                if (types[ii] instanceof ParameterizedType) {
                    _parameterized.add(ii);
                }
            }
        }

        public boolean returnsVoid ()
        {
            return method.getReturnType() == Void.TYPE;
        }

        public boolean containsGenericParameters ()
        {
            return _parameterized.size() > 0;
        }

        public List<String> getGenericCasts ()
        {
            List<String> casts = Lists.newArrayList();
            Type[] types = method.getGenericParameterTypes();
            for (int idx : _parameterized) {
                casts.add(GenUtil.simpleName(types[idx]) + " arg" + idx + " = " +
                    super.unboxArgument(types[idx], idx, true) + ";");
            }
            return casts;
        }

        @Override protected String unboxArgument (Type type, int index, boolean listenerMode)
        {
            if (listenerMode && type instanceof ParameterizedType) {
                return "arg" + index;
            } else {
                return super.unboxArgument(type, index, listenerMode);
            }
        }

        protected List<Integer> _parameterized = Lists.newArrayList();
    }

    /**
     * Configures the path to our ActionScript source files.
     */
    public void setAsroot (File asroot)
    {
        _asroot = asroot;
    }

    @Override
    protected void processService (File source, Class<?> service)
        throws Exception
    {
        System.out.println("Processing " + service.getName() + "...");

        // verify that the service class name is as we expect it to be
        if (!service.getName().endsWith("Service")) {
            System.err.println("Cannot process '" + service.getName() + "':");
            System.err.println("Service classes must be named SomethingService.");
            return;
        }

        // make sure they have a service id specified early, so we can bail if they don't
        Field serviceIdField = service.getDeclaredField(SERVICE_ID_FIELD_NAME);
        int publicStaticFinal = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
        if (serviceIdField == null || serviceIdField.getModifiers() != publicStaticFinal ||
            serviceIdField.getType() != Integer.TYPE) {
            System.err.println("Services are required to have a field with the signature:");
            System.err.println("public static final int " + SERVICE_ID_FIELD_NAME);
            return;
        }
        int serviceId = serviceIdField.getInt(null);

        ServiceDescription desc = new ServiceDescription(service, serviceId);
        generateDispatcher(source, desc);
        generatePostService(source, desc);
        generateMarshaller(source, desc);
    }

    protected void generateDispatcher (File source, ServiceDescription sdesc)
        throws Exception
    {
        if (_verbose) {
            System.out.println("Generating dispatcher");
        }

        String name = StringUtil.replace(sdesc.sname, "Service", "");

        // start with the imports required by service methods
        ImportSet imports = sdesc.imports.clone();

        // add some classes required for all dispatchers
        imports.add(Inject.class);
        imports.add(PostCodes.class);
        imports.add(PostDispatcher.class);
        imports.add(PostException.class);

        // import classes contained in arrays
        imports.translateClassArrays();

        // get rid of primitives and java.lang types
        imports.removeGlobals();

        // get rid of arrays
        imports.removeArrays();

        // import Foo instead of Foo$Bar
        imports.swapInnerClassesForParents();

        // remove imports in our own package
        imports.removeSamePackage(sdesc.spackage);

        VelocityContext ctx = new VelocityContext();
        ctx.put("name", name);
        ctx.put("package", sdesc.spackage);
        ctx.put("methods", sdesc.methods);
        ctx.put("imports", imports.toList());

        StringWriter sw = new StringWriter();
        _velocity.mergeTemplate(DISPATCHER_TMPL, "UTF-8", ctx, sw);

        // determine the path to our dispatcher file
        String mpath = source.getPath();
        mpath = StringUtil.replace(mpath, "Service", "Dispatcher");

        writeFile(mpath, sw.toString());
    }

    protected void generatePostService (File source, ServiceDescription sdesc)
        throws Exception
    {
        if (_verbose) {
            System.out.println("Generating PostService");
        }

        String sname = sdesc.sname;
        String name = StringUtil.replace(sname, "Service", "");
        String spackage = StringUtil.replace(sdesc.spackage, ".server", ".client");

        // start with the service method imports
        ImportSet imports = sdesc.imports.clone();

        // replace inner classes with action script equivalents
        imports.translateInnerClasses();

        // allow primitive types in service methods
        imports.replace("[B", "flash.utils.ByteArray");
        imports.replace("[I", "com.threerings.io.TypedArray");

        if (imports.removeAll("[L*") > 0) {
            imports.add("com.threerings.io.TypedArray");
        }

        // get rid of java.lang stuff and any remaining primitives
        imports.removeGlobals();

        // get rid of remaining arrays
        imports.removeArrays();

        // remove imports in our own package
        imports.removeSamePackage(spackage);

        VelocityContext ctx = new VelocityContext();
        ctx.put("name", name);
        ctx.put("package", spackage);
        ctx.put("methods", sdesc.methods);
        ctx.put("imports", imports.toList());

        // make sure our post service directory exists
        String spath = spackage.replace('.', File.separatorChar);
        spath = StringUtil.replace(spath, "/server/", "/client/");
        new File(_asroot + File.separator + spath).mkdirs();

        // generate the post service file
        String ampath = _asroot + File.separator + spath + File.separator + name + "PostService.as";
        StringWriter sw = new StringWriter();
        _velocity.mergeTemplate(POST_SERVICE_TMPL, "UTF-8", ctx, sw);
        writeFile(ampath, sw.toString());
    }

    protected void generateMarshaller (File source, ServiceDescription sdesc)
        throws Exception
    {
        if (_verbose) {
            System.out.println("Generating Marshaller");
        }

        String sname = sdesc.sname;
        String name = StringUtil.replace(sname, "Service", "");
        String mpackage = StringUtil.replace(sdesc.spackage, ".server", ".data");
        String spackage = StringUtil.replace(sdesc.spackage, ".server", ".client");

        // start with the service method imports
        ImportSet imports = sdesc.imports.clone();

        // replace inner classes with action script equivalents
        imports.translateInnerClasses();

        // replace primitive types with OOO types (required for unboxing)
        imports.replace("byte", "com.threerings.util.Byte");
        imports.replace("int", "com.threerings.util.Integer");
        imports.replace("boolean", "com.threerings.util.langBoolean");
        imports.replace("[B", "flash.utils.ByteArray");
        imports.replace("float", "com.threerings.util.Float");
        imports.replace("[I", "com.threerings.io.TypedArray");

        // get rid of java.lang stuff and any remaining primitives
        imports.removeGlobals();

        if (imports.removeAll("[L*") > 0) {
            imports.add("com.threerings.io.TypedArray");
        }

        // get rid of remaining arrays
        imports.removeArrays();

        // remove imports in our own package
        imports.removeSamePackage(mpackage);

        VelocityContext ctx = new VelocityContext();
        ctx.put("name", name);
        ctx.put("package", mpackage);
        ctx.put("methods", sdesc.methods);
        ctx.put("imports", imports.toList());
        ctx.put("spackage", spackage);
        ctx.put("serviceId", sdesc.serviceId);

        // make sure our post service directory exists
        String spath = mpackage.replace('.', File.separatorChar);
        spath = StringUtil.replace(spath, "/server/", "/data/");
        new File(_asroot + File.separator + spath).mkdirs();

        // generate the post service file
        String ampath = _asroot + File.separator + spath + File.separator + name + "Marshaller.as";
        StringWriter sw = new StringWriter();
        _velocity.mergeTemplate(MARSHALLER_TMPL, "UTF-8", ctx, sw);
        writeFile(ampath, sw.toString());
    }

    /** Rolls up everything needed for the generate* methods. */
    protected class ServiceDescription
    {
        public Class<?> service;
        public int serviceId;
        public String sname;
        public String spackage;
        public ImportSet imports = new ImportSet();
        public ComparableArrayList<PostServiceMethod> methods =
            new ComparableArrayList<PostServiceMethod>();

        public ServiceDescription (Class<?> serviceClass, int serviceId)
        {
            service = serviceClass;
            this.serviceId = serviceId;
            sname = service.getSimpleName();
            spackage = service.getPackage().getName();

            // look through and locate our service methods, also locating any
            // custom InvocationListener derivations along the way
            Method[] methdecls = service.getDeclaredMethods();
            for (Method m : methdecls) {
                // service interface methods must be public
                if (!Modifier.isPublic(m.getModifiers())) {
                    continue;
                }
                if (_verbose) {
                    System.out.println("Adding " + m + ", imports are " +
                        StringUtil.toString(imports));
                }
                methods.add(new PostServiceMethod(m, imports));
                if (_verbose) {
                    System.out.println("Added " + m + ", imports are " +
                        StringUtil.toString(imports));
                }
            }
            methods.sort();
        }

        /**
         * Checks if any of the service method arguments are listener types.
         */
        public boolean hasAnyListenerArgs ()
        {
            return Iterables.any(methods, new Predicate<PostServiceMethod>() {
                public boolean apply (PostServiceMethod sm) {
                    return !sm.listenerArgs.isEmpty();
                }
            });
        }

        /**
         * Constructs a union of the imports of the service methods and all listener methods.
         */
        public ImportSet constructAllImports ()
        {
            return imports.clone();
        }
    }

    protected static final String SERVICE_ID_FIELD_NAME = "SERVICE_ID";
    protected static final String DISPATCHER_TMPL =
        "com/threerings/riposte/tools/dispatcher.tmpl";
    protected static final String POST_SERVICE_TMPL =
        "com/threerings/riposte/tools/postService.tmpl";
    protected static final String MARSHALLER_TMPL =
        "com/threerings/riposte/tools/marshaller.tmpl";

    protected File _asroot;
}
