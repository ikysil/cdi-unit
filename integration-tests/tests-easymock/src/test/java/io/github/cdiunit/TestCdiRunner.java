package io.github.cdiunit;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(CdiRunner.class)
public class TestCdiRunner {

    @Inject
    AInterface a;

    @Produces
    @Mock
    AInterface aMock;

    @Test
    public void testStart() {
        assertThat(a).isEqualTo(aMock);
    }

}