package org.jglue.cdiunit.internal;

import javax.enterprise.inject.spi.Extension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

public interface DiscoveryExtension {

	default void bootstrapExtensions(Context context) {
	}

	default void process(Context context, Class<?> beanClass) {
	}

	default void discover(Context context, Field field) {
	}

	default void discover(Context context, Method method) {
	}

	interface Context {

		TestConfiguration getTestConfiguration();

		void processBean(String className);

		void processBean(Type type);

		void ignoreBean(String className);

		void ignoreBean(Type type);

		void enableAlternative(String className);

		void enableAlternative(Class<?> alternativeClass);

		void enableDecorator(String className);

		void enableDecorator(Class<?> decoratorClass);

		void enableInterceptor(String className);

		void enableInterceptor(Class<?> interceptorClass);

		void enableAlternativeStereotype(String className);

		void enableAlternativeStereotype(Class<?> alternativeStereotypeClass);

		void extension(Extension extension, String location);

		Collection<Class<?>> scanPackages(Collection<Class<?>> baseClasses);

		Collection<Class<?>> scanBeanArchives(Collection<Class<?>> baseClasses);

	}

}
