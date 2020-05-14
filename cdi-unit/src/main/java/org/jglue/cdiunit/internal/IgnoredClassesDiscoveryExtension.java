package org.jglue.cdiunit.internal;

import org.jglue.cdiunit.IgnoredClasses;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Discover IgnoredClasses feature of CDI Unit.
 */
public class IgnoredClassesDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void process(Context context, Class<?> beanClass) {
		discover(context, beanClass.getAnnotation(IgnoredClasses.class));
	}

	@Override
	public void discover(Context context, Field field) {
		if (field.isAnnotationPresent(IgnoredClasses.class)) {
			context.ignoreBean(field.getGenericType());
		}
	}

	@Override
	public void discover(Context context, Method method) {
		if (method.isAnnotationPresent(IgnoredClasses.class)) {
			context.ignoreBean(method.getGenericReturnType());
		}
	}

	private void discover(Context context, IgnoredClasses ignoredClasses) {
		if (ignoredClasses == null) {
			return;
		}
		Arrays.stream(ignoredClasses.value()).forEach(context::ignoreBean);
		Arrays.stream(ignoredClasses.late()).forEach(context::ignoreBean);
	}

}
