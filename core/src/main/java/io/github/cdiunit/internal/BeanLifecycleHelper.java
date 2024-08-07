/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.cdiunit.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import static io.github.cdiunit.internal.ExceptionUtils.illegalInstantiation;

public final class BeanLifecycleHelper {

    public static void invokePostConstruct(Class<?> targetClass, Object target) throws Throwable {
        invokeLifecycleMethods(targetClass, PostConstruct.class, target);
    }

    public static void invokePreDestroy(Class<?> targetClass, Object target) throws Throwable {
        invokeLifecycleMethods(targetClass, PreDestroy.class, target);
    }

    private static void invokeLifecycleMethods(Class<?> targetClass, Class<? extends Annotation> a, Object target)
            throws Throwable {
        findLifecycleMethods(targetClass, a).forEach(m -> {
            try {
                m.setAccessible(true);
                m.invoke(target);
            } catch (IllegalAccessException e) {
                throw ExceptionUtils.asRuntimeException(e);
            } catch (InvocationTargetException e) {
                var cause = e.getCause();
                if (cause == null) {
                    cause = e;
                }
                throw ExceptionUtils.asRuntimeException(cause);
            }
        });
    }

    private static Collection<Method> findLifecycleMethods(Class<?> targetClass, Class<? extends Annotation> a) {
        var superClassSpliterator = new Spliterator<Class<?>>() {

            Class<?> aClass = targetClass;

            @Override
            public boolean tryAdvance(Consumer<? super Class<?>> action) {
                if (aClass == null) {
                    return false;
                }

                action.accept(aClass);
                aClass = aClass.getSuperclass();
                return true;
            }

            @Override
            public Spliterator<Class<?>> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return 0;
            }

            @Override
            public int characteristics() {
                return ORDERED | DISTINCT | NONNULL | IMMUTABLE;
            }
        };
        var superclasses = StreamSupport.stream(superClassSpliterator, false).collect(Collectors.toList());
        Collections.reverse(superclasses);
        return superclasses.stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(a))
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> void.class.equals(m.getReturnType()))
                .collect(Collectors.toList());
    }

    private BeanLifecycleHelper() throws IllegalAccessException {
        illegalInstantiation();
    }

}
