package org.jglue.cdiunit.internal;

import java.lang.reflect.Type;
import java.util.Collection;

import javax.enterprise.inject.spi.Extension;

public interface DiscoveryExtension {

	default void bootstrapExtensions(Context context) {
	}

	default void process(Context context, Class<?> beanClass) {
	}

	interface Context {

		TestConfiguration getTestConfiguration();

		void processBean(String className);

		void processBean(Type type);

		void processPackage(String className);

		void processPackage(Type type);

		void processClassPath(String className);

		void processClassPath(Type type);

		void ignoreBean(String className);

		void ignoreBean(Type type);

		void enableAlternative(String className);

		void enableAlternative(Type type);

		void enableDecorator(String className);

		void enableDecorator(Type type);

		void enableInterceptor(String className);

		void enableInterceptor(Type type);

		void enableAlternativeStereotype(String className);

		void enableAlternativeStereotype(Type type);

		void extension(Extension extension, String location);

	}

}
