package io.github.cdiunit.internal.junit4;

import java.util.function.Consumer;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.environment.se.Weld;
import org.junit.runners.model.Statement;

import io.github.cdiunit.internal.TestConfiguration;
import io.github.cdiunit.internal.WeldHelper;

public class WeldLifecycle extends Statement {

    private final Statement base;
    private final TestConfiguration testConfiguration;
    private final Object target;
    private final Consumer<BeanManager> beanManagerConsumer;

    private Weld weld;
    private BeanManager beanManager;
    private CreationalContext<Object> creationalContext;
    private InjectionTarget<Object> injectionTarget;

    public WeldLifecycle(Statement base, TestConfiguration testConfiguration, Object target,
            Consumer<BeanManager> beanManagerConsumer) {
        this.base = base;
        this.testConfiguration = testConfiguration;
        this.target = target;
        this.beanManagerConsumer = beanManagerConsumer;
    }

    @Override
    public void evaluate() throws Throwable {
        try {
            initialize();
            beanManagerConsumer.accept(beanManager);
            base.evaluate();
        } finally {
            shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private void initialize() {
        weld = WeldHelper.configureWeld(testConfiguration);

        var container = weld.initialize();
        beanManager = container.getBeanManager();
        creationalContext = beanManager.createCreationalContext(null);
        var annotatedType = beanManager.createAnnotatedType(testConfiguration.getTestClass());
        injectionTarget = (InjectionTarget<Object>) beanManager.getInjectionTargetFactory(annotatedType)
                .createInjectionTarget(null);
        injectionTarget.inject(target, creationalContext);
    }

    private void shutdown() {
        if (creationalContext != null) {
            creationalContext.release();
        }
        if (weld != null) {
            weld.shutdown();
        }
    }

}