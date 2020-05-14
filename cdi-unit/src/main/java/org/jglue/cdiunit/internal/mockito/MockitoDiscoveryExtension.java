package org.jglue.cdiunit.internal.mockito;

import java.lang.reflect.Field;

import org.jglue.cdiunit.internal.DiscoveryExtension;
import org.mockito.Mock;

public class MockitoDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void bootstrapExtensions(Context context) {
		try {
			Class.forName("org.mockito.Mock");
			context.extension(new MockitoExtension(), MockitoDiscoveryExtension.class.getName());
		} catch (ClassNotFoundException ignore) {
		}
	}

	@Override
	public void process(Context context, Class<?> beanClass) {
		ignoreMockedBeans(context, beanClass);
	}

	private void ignoreMockedBeans(Context context, Class<?> beanClass) {
		try {
			for (Field field : beanClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(Mock.class)) {
					Class<?> type = field.getType();
					context.ignoreBean(type);
				}
			}
		} catch (NoClassDefFoundError ignore) {
			// no Mockito
		}
	}

}