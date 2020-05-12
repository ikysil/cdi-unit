/*
 *    Copyright 2011 Bryn Cooke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jglue.cdiunit.internal;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.*;
import org.jboss.weld.environment.se.WeldSEBeanRegistrant;
import org.jboss.weld.metadata.BeansXmlImpl;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jglue.cdiunit.*;
import org.jglue.cdiunit.internal.easymock.EasyMockExtension;
import org.jglue.cdiunit.internal.mockito.MockitoExtension;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.interceptor.Interceptor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;

public class WeldTestUrlDeployment implements Deployment {
	private final BeanDeploymentArchive beanDeploymentArchive;
	private ClasspathScanner scanner = new CachingClassGraphScanner(new DefaultBeanArchiveScanner());
	private Collection<Metadata<Extension>> extensions = new ArrayList<>();
	private static final Logger log = LoggerFactory.getLogger(WeldTestUrlDeployment.class);
	private Set<URL> cdiClasspathEntries = new HashSet<>();
	private final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();

	public WeldTestUrlDeployment(ResourceLoader resourceLoader, Bootstrap bootstrap, TestConfiguration testConfiguration) throws IOException {
		cdiClasspathEntries.addAll(scanner.getBeanArchives());
		BeansXml beansXml = createBeansXml();

		Context discoveryContext = new Context(testConfiguration);

		Set<String> discoveredClasses = new LinkedHashSet<>();
		Set<String> alternatives = new HashSet<>();
		discoveredClasses.add(testConfiguration.getTestClass().getName());
		Set<Class<?>> classesProcessed = new HashSet<>();
		Set<Class<?>> classesToIgnore = findMockedClassesOfTest(testConfiguration.getTestClass());

		discoveryContext.processBean(testConfiguration.getTestClass());

		ServiceLoader<DiscoveryExtension> discoveryExtensions = ServiceLoader.load(DiscoveryExtension.class);
		for (DiscoveryExtension extension: discoveryExtensions) {
			extension.bootstrapExtensions(discoveryContext);
		}

		testConfiguration.getAdditionalClasses().forEach(discoveryContext::processBean);

		while (true) {
			Optional<Class<?>> nextToProcess = discoveryContext.nextToProcess();
			if (!nextToProcess.isPresent()) {
				break;
			}
			final Class<?> c = nextToProcess.get();

			if ((isCdiClass(c) || Extension.class.isAssignableFrom(c)) && !classesProcessed.contains(c) && !c.isPrimitive()
				&& !classesToIgnore.contains(c)) {
				classesProcessed.add(c);
				if (!c.isAnnotation()) {
					discoveredClasses.add(c.getName());
				}
				if (Extension.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
					try {
//						extensions.add(createMetadata((Extension) c.getConstructor().newInstance(), c.getName()));
						extensions.add(createMetadata((Extension) c.newInstance(), c.getName()));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				if (c.isAnnotationPresent(Interceptor.class)) {
					beansXml.getEnabledInterceptors().add(createMetadata(c.getName(), c.getName()));
				}

				if (c.isAnnotationPresent(Decorator.class)) {
					beansXml.getEnabledDecorators().add(createMetadata(c.getName(), c.getName()));
				}

				if (isAlternativeStereotype(c)) {
					beansXml.getEnabledAlternativeStereotypes().add(createMetadata(c.getName(), c.getName()));
				}

				AdditionalClasses additionalClasses = c.getAnnotation(AdditionalClasses.class);
				if (additionalClasses != null) {
					Arrays.stream(additionalClasses.value()).forEach(discoveryContext::processBean);
					for (String lateBound : additionalClasses.late()) {
						discoveryContext.processBean(loadClass(lateBound));
					}
				}

				AdditionalClasspaths additionalClasspaths = c.getAnnotation(AdditionalClasspaths.class);
				if (additionalClasspaths != null) {
					URL[] urls = Arrays.stream(additionalClasspaths.value())
						.map(this::getClasspathURL)
						.toArray(URL[]::new);
					scanner.getClassNamesForClasspath(urls)
						.stream()
						.map(this::loadClass)
						.forEach(discoveryContext::processBean);
				}

				AdditionalPackages additionalPackages = c.getAnnotation(AdditionalPackages.class);
				if (additionalPackages != null) {
					for (Class<?> additionalPackage : additionalPackages.value()) {
						final String packageName = additionalPackage.getPackage().getName();
						URL url = getClasspathURL(additionalPackage);

						// It might be more efficient to scan all packageNames at once, but we
						// might pick up classes from a different package's classpath entry, which
						// would be a change in behaviour (but perhaps less surprising?).
						scanner.getClassNamesForPackage(packageName, url)
							.stream()
							.map(this::loadClass)
							.forEach(discoveryContext::processBean);
					}
				}

				IgnoredClasses ignoredClasses = c.getAnnotation(IgnoredClasses.class);
				if (ignoredClasses != null) {
					Collections.addAll(classesToIgnore, ignoredClasses.value());
					for (String lateBound : ignoredClasses.late()) {
						classesToIgnore.add(loadClass(lateBound));
					}
				}

				ActivatedAlternatives alternativeClasses = c.getAnnotation(ActivatedAlternatives.class);
				if (alternativeClasses != null) {
					for (Class<?> alternativeClass : alternativeClasses.value()) {
						discoveryContext.processBean(alternativeClass);
						if (!isAlternativeStereotype(alternativeClass)) {
							alternatives.add(alternativeClass.getName());
						}
					}
				}

				for (Annotation a : c.getAnnotations()) {
					if (!a.annotationType().getPackage().getName().equals("org.jglue.cdiunit")) {
						discoveryContext.processBean(a.annotationType());
					}
				}

				Type superClass = c.getGenericSuperclass();
				if (superClass != null && superClass != Object.class) {
					discoveryContext.processBean(superClass);
				}

				for (Field field : c.getDeclaredFields()) {
					if (field.isAnnotationPresent(IgnoredClasses.class)) {
						addClassesToProcess(classesToIgnore, field.getGenericType());
					}
					if (field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(Produces.class)) {
						discoveryContext.processBean(field.getGenericType());
					}
					if (field.getType().equals(Provider.class) || field.getType().equals(Instance.class)) {
						discoveryContext.processBean(field.getGenericType());
					}
				}
				for (Method method : c.getDeclaredMethods()) {
					if (method.isAnnotationPresent(IgnoredClasses.class)) {
						addClassesToProcess(classesToIgnore, method.getGenericReturnType());
					}
					if (method.isAnnotationPresent(Inject.class) || method.isAnnotationPresent(Produces.class)) {
						for (Type param : method.getGenericParameterTypes()) {
							discoveryContext.processBean(param);
						}
						// TODO PERF we might be adding classes which we already processed
						discoveryContext.processBean(method.getGenericReturnType());

					}
				}
			}

			discoveryContext.processed(c);
		}

		beansXml.getEnabledAlternativeStereotypes().add(
			createMetadata(ProducesAlternative.class.getName(), ProducesAlternative.class.getName()));

		for (String alternative : alternatives) {
			beansXml.getEnabledAlternativeClasses().add(createMetadata(alternative, alternative));
		}

		try {
			Class.forName("org.mockito.Mock");
			extensions.add(createMetadata(new MockitoExtension(), MockitoExtension.class.getName()));
		} catch (ClassNotFoundException ignore) {
		}

		try {
			Class.forName("org.easymock.EasyMockRunner");
			extensions.add(createMetadata(new EasyMockExtension(), EasyMockExtension.class.getName()));
		} catch (ClassNotFoundException ignore) {
		}

		extensions.add(createMetadata(new WeldSEBeanRegistrant(), WeldSEBeanRegistrant.class.getName()));

		extensions.addAll(discoveryContext.getExtensions());

		beanDeploymentArchive = new BeanDeploymentArchiveImpl("cdi-unit" + UUID.randomUUID(), discoveredClasses, beansXml);
		beanDeploymentArchive.getServices().add(ResourceLoader.class, resourceLoader);
		log.debug("CDI-Unit discovered:");
		for (String clazz : discoveredClasses) {
			if (!clazz.startsWith("org.jglue.cdiunit.internal.")) {
				log.debug(clazz);
			}
		}

	}

	private Class<?> loadClass(String name) {
		try {
			return getClass().getClassLoader().loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static <T> Metadata<T> createMetadata(T value, String location) {
		try {
			return new org.jboss.weld.bootstrap.spi.helpers.MetadataImpl<>(value, location);
		} catch (NoClassDefFoundError e) {
			// MetadataImpl moved to a new package in Weld 2.4, old copy removed in 3.0
			try {
				// If Weld < 2.4, the new package isn't there, so we try the old package.
				//noinspection unchecked
				Class<Metadata<T>> oldClass = (Class<Metadata<T>>) Class.forName("org.jboss.weld.metadata.MetadataImpl");
				Constructor<Metadata<T>> ctor = oldClass.getConstructor(Object.class, String.class);
				return ctor.newInstance(value, location);
			} catch (ReflectiveOperationException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private static Object annotatedDiscoveryMode() {
		try {
			return BeanDiscoveryMode.ANNOTATED;
		} catch (NoClassDefFoundError e) {
			// No such enum in Weld 1.x, but the constructor for BeansXmlImpl has fewer parameters so we don't need it
			return null;
		}
	}

	private static BeansXml createBeansXml() {
		try {
			// The constructor for BeansXmlImpl has added more parameters in newer Weld versions. The parameter list
			// is truncated in older version of Weld where the number of parameters is shorter, thus omitting the
			// newer parameters.
			Object[] initArgs = new Object[]{
				new ArrayList<Metadata<String>>(), new ArrayList<Metadata<String>>(),
				new ArrayList<Metadata<String>>(), new ArrayList<Metadata<String>>(), Scanning.EMPTY_SCANNING,
				// These were added in Weld 2.0:
				new URL("file:cdi-unit"), annotatedDiscoveryMode(), "cdi-unit",
				// isTrimmed: added in Weld 2.4.2 [WELD-2314]:
				false
			};
			Constructor<?> beansXmlConstructor = BeansXmlImpl.class.getConstructors()[0];
			return (BeansXml) beansXmlConstructor.newInstance(
				Arrays.copyOfRange(initArgs, 0, beansXmlConstructor.getParameterCount()));
		} catch (MalformedURLException | ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private void addClassesToProcess(Collection<Class<?>> classesToProcess, Type type) {
		if (type instanceof Class) {
			classesToProcess.add((Class<?>) type);
		}

		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			classesToProcess.add((Class<?>) ptype.getRawType());
			for (Type arg : ptype.getActualTypeArguments()) {
				addClassesToProcess(classesToProcess, arg);
			}
		}
	}

	private Set<Class<?>> findMockedClassesOfTest(Class<?> testClass) {
		Set<Class<?>> mockedClasses = new HashSet<>();

		try {
			for (Field field : testClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(Mock.class)) {
					Class<?> type = field.getType();
					mockedClasses.add(type);
				}
			}
		} catch (NoClassDefFoundError ignore) {
			// no Mockito
		}

		try {
			for (Field field : testClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(org.easymock.Mock.class)) {
					Class<?> type = field.getType();
					mockedClasses.add(type);
				}
			}
		} catch (NoClassDefFoundError ignore) {
			// no EasyMock
		}
		return mockedClasses;
	}

	private URL getClasspathURL(Class<?> clazz) {
		CodeSource codeSource = clazz.getProtectionDomain()
			.getCodeSource();
		return codeSource != null ? codeSource.getLocation() : null;
	}

	private boolean isCdiClass(Class<?> c) {
		URL location = getClasspathURL(c);
		return location != null && cdiClasspathEntries.contains(location);
	}

	private boolean isAlternativeStereotype(Class<?> c) {
		return c.isAnnotationPresent(Stereotype.class) && c.isAnnotationPresent(Alternative.class);
	}

	@Override
	public Iterable<Metadata<Extension>> getExtensions() {
		return extensions;
	}

	public List<BeanDeploymentArchive> getBeanDeploymentArchives() {
		return Collections.singletonList(beanDeploymentArchive);
	}

	public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> beanClass) {
		return beanDeploymentArchive;
	}

	public BeanDeploymentArchive getBeanDeploymentArchive(Class<?> beanClass) {
		return beanDeploymentArchive;
	}

	@Override
	public ServiceRegistry getServices() {
		return serviceRegistry;
	}

	private static class Context implements DiscoveryExtension.Context {

		private final TestConfiguration testConfiguration;

		private final Collection<Metadata<Extension>> extensions = new ArrayList<>();

		private final Set<Class<?>> classesToProcess = new LinkedHashSet<>();

		public Context(final TestConfiguration testConfiguration) {
			this.testConfiguration = testConfiguration;
		}

		@Override
		public TestConfiguration getTestConfiguration() {
			return testConfiguration;
		}

		public Optional<Class<?>> nextToProcess() {
			if (classesToProcess.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(classesToProcess.iterator().next());
		}

		public void processed(Class<?> c) {
			classesToProcess.remove(c);
		}

		@Override
		public void processBean(String className) {
			try {
				processBean(Class.forName(className));
			} catch (ClassNotFoundException ignore) {
			}
		}

		@Override
		public void processBean(Type type) {
			if (type instanceof Class) {
				classesToProcess.add((Class<?>) type);
			}

			if (type instanceof ParameterizedType) {
				ParameterizedType ptype = (ParameterizedType) type;
				classesToProcess.add((Class<?>) ptype.getRawType());
				for (Type arg : ptype.getActualTypeArguments()) {
					processBean(arg);
				}
			}
		}

		@Override
		public void processPackage(String className) {

		}

		@Override
		public void processPackage(Type type) {

		}

		@Override
		public void processClassPath(String className) {

		}

		@Override
		public void processClassPath(Type type) {

		}

		@Override
		public void ignoreBean(String className) {

		}

		@Override
		public void ignoreBean(Type type) {

		}

		@Override
		public void enableAlternative(String className) {

		}

		@Override
		public void enableAlternative(Type type) {

		}

		@Override
		public void enableDecorator(String className) {

		}

		@Override
		public void enableDecorator(Type type) {

		}

		@Override
		public void enableInterceptor(String className) {

		}

		@Override
		public void enableInterceptor(Type type) {

		}

		@Override
		public void enableAlternativeStereotype(String className) {

		}

		@Override
		public void enableAlternativeStereotype(Type type) {

		}

		@Override
		public void extension(Extension extension, String location) {
			extensions.add(createMetadata(extension, location));
		}

		public Collection<Metadata<Extension>> getExtensions() {
			return extensions;
		}

	}

}
