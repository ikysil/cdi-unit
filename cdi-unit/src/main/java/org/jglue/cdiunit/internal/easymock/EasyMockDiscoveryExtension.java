package org.jglue.cdiunit.internal.easymock;

import java.lang.reflect.Field;

import org.jglue.cdiunit.internal.DiscoveryExtension;

public class EasyMockDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void bootstrap(BootstrapDiscoveryContext bdc) {
		bdc.discoverExtension(this::discoverCdiExtension);
		bdc.discoverClass(this::discoverClass);
	}

	private void discoverCdiExtension(Context context) {
		try {
			Class.forName("org.easymock.EasyMockRunner");
			context.extension(new EasyMockExtension(), EasyMockDiscoveryExtension.class.getName());
		} catch (ClassNotFoundException ignore) {
		}
	}

	private void discoverClass(Context context, Class<?> cls) {
		ignoreMockedBeans(context, cls);
	}

	private void ignoreMockedBeans(Context context, Class<?> beanClass) {
		// FIXME - migrate to discoverField
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
