package org.jglue.cdiunit.internal.mockito;

import org.jglue.cdiunit.internal.DiscoveryExtension;

public class MockitoDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void bootstrapExtensions(Context context) {
		try {
			Class.forName("org.mockito.Mock");
			context.extension(new MockitoExtension(), MockitoDiscoveryExtension.class.getName());
		} catch (ClassNotFoundException ignore) {
		}
	}

}
