package org.jglue.cdiunit.internal.easymock;

import java.lang.reflect.Field;

import org.jglue.cdiunit.internal.DiscoveryExtension;

public class EasyMockDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void bootstrapExtensions(Context context) {
		try {
			Class.forName("org.easymock.EasyMockRunner");
			context.extension(new EasyMockExtension(), EasyMockDiscoveryExtension.class.getName());
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
				if (field.isAnnotationPresent(org.easymock.Mock.class)) {
					Class<?> type = field.getType();
					context.ignoreBean(type);
				}
			}
		} catch (NoClassDefFoundError ignore) {
			// no EasyMock
		}
	}

}
