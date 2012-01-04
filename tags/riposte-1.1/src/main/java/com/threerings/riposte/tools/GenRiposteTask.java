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

package com.threerings.riposte.tools;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.samskivert.util.ComparableArrayList;
import com.threerings.presents.tools.GenUtil;
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
        public PostServiceMethod (Method method)
        {
            super(method);

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
                    unboxArgument(types[idx], idx, false) + ";");
            }
            return casts;
        }

        @Override protected String unboxArgument (Type type, int index, boolean listenerMode)
        {
            if (listenerMode && type instanceof ParameterizedType) {
                return "arg" + index;
            } else {
                String unboxed = GenUtil.unboxArgument(type, "args[" + index + "]");
                // hack to get around a breaking change in GenUtil
                if (unboxed.startsWith("this")) {
                    unboxed = "(" + GenUtil.simpleName(type) + ")args[" + index + "]";
                }
                return unboxed;
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
    public void processClass (File source, Class<?> service)
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
        String name = sdesc.sname.replace("Service", "");

        // start with the imports required by service methods
        ImportSet imports = sdesc.imports.clone();

        // add some classes required for all dispatchers
        imports.add("com.google.inject.Inject");
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

        // determine the path to our dispatcher file
        String mpath = source.getPath();
        mpath = mpath.replace("Service", "Dispatcher");

        writeFile(mpath, mergeTemplate(DISPATCHER_TMPL,
                                       "name", name,
                                       "package", sdesc.spackage,
                                       "methods", sdesc.methods,
                                       "imports", imports.toList()));
    }

    protected void generatePostService (File source, ServiceDescription sdesc)
        throws Exception
    {
        String sname = sdesc.sname;
        String name = sname.replace("Service", "");
        String spackage = sdesc.spackage.replace(".server", ".client");

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

        // make sure our post service directory exists
        String spath = spackage.replace('.', File.separatorChar);
        spath = spath.replace("/server/", "/client/");
        new File(_asroot + File.separator + spath).mkdirs();

        // generate the post service file
        String ampath = _asroot + File.separator + spath + File.separator + name + "PostService.as";
        writeFile(ampath, mergeTemplate(POST_SERVICE_TMPL,
                                        "name", name,
                                        "package", spackage,
                                        "methods", sdesc.methods,
                                        "imports", imports.toList()));
    }

    protected void generateMarshaller (File source, ServiceDescription sdesc)
        throws Exception
    {
        String sname = sdesc.sname;
        String name = sname.replace("Service", "");
        String mpackage = sdesc.spackage.replace(".server", ".data");
        String spackage = sdesc.spackage.replace(".server", ".client");

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

        // make sure our post service directory exists
        String spath = mpackage.replace('.', File.separatorChar);
        spath = spath.replace("/server/", "/data/");
        new File(_asroot + File.separator + spath).mkdirs();

        // generate the post service file
        String ampath = _asroot + File.separator + spath + File.separator + name + "Marshaller.as";
        writeFile(ampath, mergeTemplate(MARSHALLER_TMPL,
                                        "name", name,
                                        "package", mpackage,
                                        "methods", sdesc.methods,
                                        "imports", imports.toList(),
                                        "spackage", spackage,
                                        "serviceId", sdesc.serviceId));
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
                PostServiceMethod psm = new PostServiceMethod(m);
                psm.gatherImports(imports);
                methods.add(psm);
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
