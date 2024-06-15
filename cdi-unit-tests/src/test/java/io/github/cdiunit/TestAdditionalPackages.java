package io.github.cdiunit;

import javax.inject.Inject;

import io.github.cdiunit.packagetest.PackageImpl;
import io.github.cdiunit.packagetest.PackageInterface;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@AdditionalPackages(PackageInterface.class)
@RunWith(CdiRunner.class)
public class TestAdditionalPackages {

	@Inject
	private PackageInterface p;

	@Test
	public void testResolvedPackage() {
		Assert.assertTrue(p instanceof PackageImpl);
	}


}