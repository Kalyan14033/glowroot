/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.tests;

import java.lang.instrument.ClassFileTransformer;

import org.fest.reflect.core.Reflection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.glowroot.Containers;
import org.glowroot.container.AppUnderTest;
import org.glowroot.container.Container;
import org.glowroot.container.IgnoreOnJdk6;
import org.glowroot.weaving.WeavingClassFileTransformer;

/**
 * From http://docs.oracle.com/javase/7/docs/api/java/lang/invoke/MethodHandle.html
 * 
 * "Implementations may (or may not) create internal subclasses of MethodHandle"
 * 
 * When these internal subclasses of MethodHandle are created, they are passed to
 * {@link ClassFileTransformer#transform(ClassLoader, String, Class, java.security.ProtectionDomain, byte[])}
 * with null class name.
 * 
 * This test checks that
 * {@link WeavingClassFileTransformer#transform(ClassLoader, String, Class, java.security.ProtectionDomain, byte[])}
 * doesn't mind being passed null class names.
 * 
 * @author Trask Stalnaker
 * @since 0.5
 */
@RunWith(IgnoreOnJdk6.class)
public class MethodHandleWeavingTest {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = Containers.createJavaagentContainer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void shouldReadTraces() throws Exception {
        // given
        // when
        container.executeAppUnderTest(ShouldDefineAnonymousClass.class);
        // then
    }

    public static class ShouldDefineAnonymousClass implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            Class<?> methodHandlesClass = Reflection.type("java.lang.invoke.MethodHandles").load();

            Object lookup = Reflection.staticMethod("lookup")
                    .in(methodHandlesClass)
                    .invoke();

            Class<?> methodTypeClass = Reflection.type("java.lang.invoke.MethodType").load();

            Object methodType = Reflection.staticMethod("methodType")
                    .withParameterTypes(Class.class)
                    .in(methodTypeClass)
                    .invoke(String.class);

            Reflection.method("findVirtual")
                    .withParameterTypes(Class.class, String.class, methodTypeClass)
                    .in(lookup)
                    .invoke(Object.class, "toString", methodType);
        }
    }
}
