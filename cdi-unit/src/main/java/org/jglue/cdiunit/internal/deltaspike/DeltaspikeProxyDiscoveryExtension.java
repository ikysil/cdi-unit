package org.jglue.cdiunit.internal.deltaspike;

import org.jglue.cdiunit.deltaspike.SupportDeltaspikeProxy;
import org.jglue.cdiunit.internal.DiscoveryExtension;

import java.util.Collections;

public class DeltaspikeProxyDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void process(Context context, Class<?> beanClass) {
		if (beanClass != SupportDeltaspikeProxy.class) {
			return;
		}
		try {
			// support for DeltaSpike 1.7.x and 1.8.x
			final Class<?> baseClass = Class.forName("org.apache.deltaspike.proxy.impl.invocation.InterceptorLookup");
			processBeanArchive(context, baseClass);
		} catch (ClassNotFoundException e) {
		}
		try {
			// support for DeltaSpike 1.9.x
			final Class<?> baseClass = Class.forName("org.apache.deltaspike.proxy.spi.invocation.DeltaSpikeProxyInvocationHandler");
			processBeanArchive(context, baseClass);
		} catch (ClassNotFoundException e) {
		}
	}

	private void processBeanArchive(Context context, Class<?> baseClass) {
		context.scanBeanArchives(Collections.singleton(baseClass))
			.forEach(context::processBean);
	}

}
