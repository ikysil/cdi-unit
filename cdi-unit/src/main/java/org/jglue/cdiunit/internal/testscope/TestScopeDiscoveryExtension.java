package org.jglue.cdiunit.internal.testscope;

import org.jglue.cdiunit.internal.DiscoveryExtension;

public class TestScopeDiscoveryExtension implements DiscoveryExtension {

	@Override
	public void bootstrapExtensions(Context context) {
		context.extension(new TestScopeExtension(context.getTestConfiguration()), TestScopeDiscoveryExtension.class.getName());
	}

}
