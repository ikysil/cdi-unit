package org.jglue.cdiunit.internal;

import javax.decorator.Decorator;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Extension;
import javax.interceptor.Interceptor;

import java.lang.reflect.Modifier;

/**
 * Discover standard CDI features - extensions, interceptors, decorators, and alternative stereotypes.
 */
public class CdiDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void process(Context context, Class<?> beanClass) {
		discoverExtensions(context, beanClass);
		discoverInterceptors(context, beanClass);
		discoverDecorators(context, beanClass);
		discoverAlternativeStereotype(context, beanClass);
	}

	private void discoverExtensions(Context context, Class<?> beanClass) {
		if (Extension.class.isAssignableFrom(beanClass) && !Modifier.isAbstract(beanClass.getModifiers())) {
			try {
				context.extension((Extension) beanClass.getConstructor().newInstance(), beanClass.getName());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void discoverDecorators(Context context, Class<?> beanClass) {
		if (beanClass.isAnnotationPresent(Interceptor.class)) {
			context.enableInterceptor(beanClass);
		}
	}

	private void discoverInterceptors(Context context, Class<?> beanClass) {
		if (beanClass.isAnnotationPresent(Decorator.class)) {
			context.enableDecorator(beanClass);
		}
	}

	private void discoverAlternativeStereotype(Context context, Class<?> beanClass) {
		if (isAlternativeStereotype(beanClass)) {
			context.enableAlternativeStereotype(beanClass);
		}
	}

	private static boolean isAlternativeStereotype(Class<?> c) {
		return c.isAnnotationPresent(Stereotype.class) && c.isAnnotationPresent(Alternative.class);
	}

}
