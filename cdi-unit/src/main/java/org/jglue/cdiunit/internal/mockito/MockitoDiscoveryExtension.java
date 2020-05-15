package org.jglue.cdiunit.internal.mockito;

import java.lang.reflect.Field;

import org.jglue.cdiunit.internal.DiscoveryExtension;
import org.mockito.Mock;

public class MockitoDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void bootstrap(BootstrapDiscoveryContext bdc) {
		bdc.discoverExtension(this::discoverCdiExtension);
		bdc.discoverClass(this::discoverClass);
	}

	private void discoverCdiExtension(Context context) {
		try {
			Class.forName("org.mockito.Mock");
			context.extension(new MockitoExtension(), MockitoDiscoveryExtension.class.getName());
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
