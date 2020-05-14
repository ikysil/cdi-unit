package org.jglue.cdiunit.internal;

import org.jglue.cdiunit.*;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Discover CDI Unit features:
 * <li>{@link AdditionalClasspaths}</li>
 * <li>{@link AdditionalPackages}</li>
 * <li>{@link AdditionalClasses}</li>
 * <li>{@link IgnoredClasses}</li>
 * <li>{@link ActivatedAlternatives}</li>
 * <li>meta annotations</li>
 */
public class CdiUnitDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void process(Context context, Class<?> beanClass) {
		discover(context, beanClass.getAnnotation(AdditionalClasspaths.class));
		discover(context, beanClass.getAnnotation(AdditionalPackages.class));
		discover(context, beanClass.getAnnotation(AdditionalClasses.class));
		discover(context, beanClass.getAnnotation(IgnoredClasses.class));
		discover(context, beanClass.getAnnotation(ActivatedAlternatives.class));
		discover(context, beanClass.getAnnotations());
		discover(context, beanClass.getGenericSuperclass());
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

	private void discover(Context context, AdditionalClasspaths additionalClasspaths) {
		if (additionalClasspaths == null) {
			return;
		}
		final List<Class<?>> baseClasses = Arrays.stream(additionalClasspaths.value()).collect(Collectors.toList());
		context.scanBeanArchives(baseClasses)
			.forEach(context::processBean);
	}

	private void discover(Context context, AdditionalPackages additionalPackages) {
		if (additionalPackages == null) {
			return;
		}
		final List<Class<?>> baseClasses = Arrays.stream(additionalPackages.value()).collect(Collectors.toList());
		context.scanPackages(baseClasses)
			.forEach(context::processBean);
	}

	private void discover(Context context, AdditionalClasses additionalClasses) {
		if (additionalClasses == null) {
			return;
		}
		Arrays.stream(additionalClasses.value()).forEach(context::processBean);
		Arrays.stream(additionalClasses.late()).forEach(context::processBean);
	}

	private void discover(Context context, IgnoredClasses ignoredClasses) {
		if (ignoredClasses == null) {
			return;
		}
		Arrays.stream(ignoredClasses.value()).forEach(context::ignoreBean);
		Arrays.stream(ignoredClasses.late()).forEach(context::ignoreBean);
	}

	private void discover(Context context, ActivatedAlternatives alternativeClasses) {
		if (alternativeClasses == null) {
			return;
		}
		for (Class<?> alternativeClass : alternativeClasses.value()) {
			context.processBean(alternativeClass);
			if (!isAlternativeStereotype(alternativeClass)) {
				context.enableAlternative(alternativeClass);
			}
		}
	}

	private static boolean isAlternativeStereotype(Class<?> c) {
		return c.isAnnotationPresent(Stereotype.class) && c.isAnnotationPresent(Alternative.class);
	}

	private void discover(Context context, Annotation[] annotations) {
		Arrays.stream(annotations)
			.filter(this::exceptCdiUnitAnnotations)
			.map(Annotation::annotationType)
			.forEach(context::processBean);
	}

	private boolean exceptCdiUnitAnnotations(Annotation annotation) {
		return !annotation.annotationType().getPackage().getName().equals("org.jglue.cdiunit");
	}

	private void discover(Context context, Type genericSuperclass) {
		Optional.ofNullable(genericSuperclass)
			.filter(o -> o != Object.class)
			.ifPresent(context::processBean);
	}

}
