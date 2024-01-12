/*******************************************************************************
 * Copyright (c) 2011, 2024 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.internal.core.model.description.helpers;

import org.eclipse.wb.core.databinding.xsd.component.Component;
import org.eclipse.wb.core.databinding.xsd.component.ContextFactory;
import org.eclipse.wb.core.databinding.xsd.component.Creation;
import org.eclipse.wb.core.databinding.xsd.component.ExposingRuleType;
import org.eclipse.wb.core.databinding.xsd.component.ExposingRulesType;
import org.eclipse.wb.core.databinding.xsd.component.MorphingType;
import org.eclipse.wb.core.databinding.xsd.component.TagType;
import org.eclipse.wb.core.databinding.xsd.component.TypeParameterType;
import org.eclipse.wb.core.databinding.xsd.component.TypeParametersType;
import org.eclipse.wb.internal.core.model.description.AbstractInvocationDescription;
import org.eclipse.wb.internal.core.model.description.ComponentDescription;
import org.eclipse.wb.internal.core.model.description.ComponentDescriptionKey;
import org.eclipse.wb.internal.core.model.description.ConfigurablePropertyDescription;
import org.eclipse.wb.internal.core.model.description.ConstructorDescription;
import org.eclipse.wb.internal.core.model.description.CreationDescription;
import org.eclipse.wb.internal.core.model.description.CreationInvocationDescription;
import org.eclipse.wb.internal.core.model.description.GenericPropertyDescription;
import org.eclipse.wb.internal.core.model.description.IDescriptionProcessor;
import org.eclipse.wb.internal.core.model.description.ParameterDescription;
import org.eclipse.wb.internal.core.model.description.ToolkitDescription;
import org.eclipse.wb.internal.core.model.description.factory.FactoryMethodDescription;
import org.eclipse.wb.internal.core.model.description.internal.AbstractConfigurableDescription;
import org.eclipse.wb.internal.core.model.description.resource.ClassResourceInfo;
import org.eclipse.wb.internal.core.model.description.resource.ResourceInfo;
import org.eclipse.wb.internal.core.model.description.rules.ConfigurableObjectListParameterRule;
import org.eclipse.wb.internal.core.model.description.rules.ConfigurableObjectParameterRule;
import org.eclipse.wb.internal.core.model.description.rules.ConfigurablePropertyRule;
import org.eclipse.wb.internal.core.model.description.rules.ConstructorRule;
import org.eclipse.wb.internal.core.model.description.rules.CreationTagRule;
import org.eclipse.wb.internal.core.model.description.rules.CreationTypeParametersRule;
import org.eclipse.wb.internal.core.model.description.rules.ExposingRulesRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodOrderDefaultRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodOrderMethodRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodOrderMethodsRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodOrderMethodsSignatureRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodPropertyRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodSinglePropertyRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodTagRule;
import org.eclipse.wb.internal.core.model.description.rules.MethodsOperationRule;
import org.eclipse.wb.internal.core.model.description.rules.ModelClassRule;
import org.eclipse.wb.internal.core.model.description.rules.MorphingNoInheritRule;
import org.eclipse.wb.internal.core.model.description.rules.MorphingTargetRule;
import org.eclipse.wb.internal.core.model.description.rules.ObjectCreateRule;
import org.eclipse.wb.internal.core.model.description.rules.ParameterEditorRule;
import org.eclipse.wb.internal.core.model.description.rules.ParameterTagRule;
import org.eclipse.wb.internal.core.model.description.rules.PropertyAccessRule;
import org.eclipse.wb.internal.core.model.description.rules.PropertyCategoryRule;
import org.eclipse.wb.internal.core.model.description.rules.PropertyDefaultRule;
import org.eclipse.wb.internal.core.model.description.rules.PropertyEditorRule;
import org.eclipse.wb.internal.core.model.description.rules.PropertyGetterRule;
import org.eclipse.wb.internal.core.model.description.rules.PropertyTagRule;
import org.eclipse.wb.internal.core.model.description.rules.PublicFieldPropertiesRule;
import org.eclipse.wb.internal.core.model.description.rules.SetClassPropertyRule;
import org.eclipse.wb.internal.core.model.description.rules.SetListedPropertiesRule;
import org.eclipse.wb.internal.core.model.description.rules.StandardBeanPropertiesAdvancedRule;
import org.eclipse.wb.internal.core.model.description.rules.StandardBeanPropertiesHiddenRule;
import org.eclipse.wb.internal.core.model.description.rules.StandardBeanPropertiesNoDefaultValueRule;
import org.eclipse.wb.internal.core.model.description.rules.StandardBeanPropertiesNormalRule;
import org.eclipse.wb.internal.core.model.description.rules.StandardBeanPropertiesPreferredRule;
import org.eclipse.wb.internal.core.model.description.rules.StandardBeanPropertiesRule;
import org.eclipse.wb.internal.core.model.description.rules.StandardBeanPropertyTagRule;
import org.eclipse.wb.internal.core.model.description.rules.ToolkitRule;
import org.eclipse.wb.internal.core.utils.ast.AstEditor;
import org.eclipse.wb.internal.core.utils.ast.AstNodeUtils;
import org.eclipse.wb.internal.core.utils.ast.AstParser;
import org.eclipse.wb.internal.core.utils.ast.DomGenerics;
import org.eclipse.wb.internal.core.utils.check.Assert;
import org.eclipse.wb.internal.core.utils.exception.DesignerException;
import org.eclipse.wb.internal.core.utils.exception.ICoreExceptionConstants;
import org.eclipse.wb.internal.core.utils.execution.ExecutionUtils;
import org.eclipse.wb.internal.core.utils.execution.RunnableEx;
import org.eclipse.wb.internal.core.utils.external.ExternalFactoriesHelper;
import org.eclipse.wb.internal.core.utils.jdt.core.CodeUtils;
import org.eclipse.wb.internal.core.utils.reflect.ClassMap;
import org.eclipse.wb.internal.core.utils.reflect.IntrospectionHelper;
import org.eclipse.wb.internal.core.utils.reflect.ReflectionUtils;
import org.eclipse.wb.internal.core.utils.state.EditorState;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jface.resource.ImageDescriptor;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.Rule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.impl.NoOpLog;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;

/**
 * Helper for accessing descriptions of components -
 * {@link ComponentDescription}.
 *
 * @author scheglov_ke
 * @coverage core.model.description
 */
public final class ComponentDescriptionHelper {
	////////////////////////////////////////////////////////////////////////////
	//
	// Constructor
	//
	////////////////////////////////////////////////////////////////////////////
	private ComponentDescriptionHelper() {
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Access
	//
	////////////////////////////////////////////////////////////////////////////
	private static final ClassMap<ComponentDescription> m_getDescription_Class = ClassMap.create();

	/**
	 * Returns the factory-method specific {@link ComponentDescription}.
	 * <p>
	 * Sometimes you want to add specific tweaks for component, when it is created
	 * using factory. For example - hide some properties, copy factory properties,
	 * etc.
	 */
	public static ComponentDescription getDescription(AstEditor editor,
			FactoryMethodDescription factoryMethodDescription) throws Exception {
		Class<?> componentClass = factoryMethodDescription.getReturnClass();
		// prepare description key
		ComponentDescriptionKey key;
		{
			String signature = factoryMethodDescription.getSignature();
			String signatureUnix = StringUtils.replaceChars(signature, "(,)", "___");
			Class<?> declaringClass = factoryMethodDescription.getDeclaringClass();
			ComponentDescriptionKey declaringKey = new ComponentDescriptionKey(declaringClass);
			key = new ComponentDescriptionKey(componentClass, declaringKey, signatureUnix);
		}
		// get key specific description
		return getKeySpecificDescription(editor, componentClass, key);
	}

	/**
	 * @param editor          the {@link AstEditor} in context of which we work now.
	 * @param hostDescription the {@link ComponentDescription} which child component
	 *                        we expose.
	 * @param method          the {@link Method} that exposes component.
	 *
	 * @return the {@link ComponentDescription} of component exposed using given
	 *         {@link Method}.
	 * @throws Exception if no {@link ComponentDescription} can be found.
	 */
	public static ComponentDescription getDescription(AstEditor editor, ComponentDescription hostDescription,
			Method method) throws Exception {
		Class<?> componentClass = method.getReturnType();
		// prepare description key
		ComponentDescriptionKey key;
		{
			String suffix = method.getName();
			key = new ComponentDescriptionKey(componentClass, hostDescription.getKey(), suffix);
		}
		// get key specific description
		return getKeySpecificDescription(editor, componentClass, key);
	}

	/**
	 * @param editor          the {@link AstEditor} in context of which we work now.
	 * @param hostDescription the {@link ComponentDescription} that has method with
	 *                        given parameter.
	 * @param parameter       the {@link SingleVariableDeclaration} parameter that
	 *                        considered as component.
	 *
	 * @return the {@link ComponentDescription} of component represented by given
	 *         {@link SingleVariableDeclaration}.
	 * @throws Exception if no {@link ComponentDescription} can be found.
	 */
	public static ComponentDescription getDescription(AstEditor editor, ComponentDescription hostDescription,
			SingleVariableDeclaration parameter) throws Exception {
		// prepare parameter Class
		Class<?> parameterClass;
		{
			String parameterClassName = AstNodeUtils.getFullyQualifiedName(parameter.getType(), true);
			parameterClass = EditorState.get(editor).getEditorLoader().loadClass(parameterClassName);
		}
		// prepare suffix
		String suffix;
		{
			MethodDeclaration methodDeclaration = (MethodDeclaration) parameter.getParent();
			String signature = AstNodeUtils.getMethodSignature(methodDeclaration);
			String signatureUnix = StringUtils.replaceChars(signature, "(,)", "___");
			int parameterIndex = DomGenerics.parameters(methodDeclaration).indexOf(parameter);
			suffix = signatureUnix + "." + parameterIndex;
		}
		// prepare DescriptionInfo's for parameter in inheritance hierarchy
		List<ClassResourceInfo> additionalDescriptions = new ArrayList<>();
		{
			Class<?> hostComponentClass = hostDescription.getComponentClass();
			List<Class<?>> types = ReflectionUtils.getSuperHierarchy(hostComponentClass);
			Collections.reverse(types);
			for (Class<?> type : types) {
				ComponentDescriptionKey hostKey = new ComponentDescriptionKey(type);
				// prepare specific ResourceInfo
				ResourceInfo resourceInfo;
				{
					EditorState state = EditorState.get(editor);
					ILoadingContext context = EditorStateLoadingContext.get(state);
					String descriptionPath = hostKey.getName() + "." + suffix + ".wbp-component.xml";
					resourceInfo = DescriptionHelper.getResourceInfo(context, type, descriptionPath);
				}
				// add specific DescriptionInfo
				if (resourceInfo != null) {
					ClassResourceInfo descriptionInfo = new ClassResourceInfo(parameterClass, resourceInfo);
					additionalDescriptions.add(descriptionInfo);
				}
			}
		}
		// get key specific description
		if (additionalDescriptions.isEmpty()) {
			return getDescription(editor, parameterClass);
		} else {
			ComponentDescriptionKey key = new ComponentDescriptionKey(parameterClass, hostDescription.getKey(), suffix);
			return getDescription0(editor, key, additionalDescriptions);
		}
	}

	/**
	 * @param editor         the {@link AstEditor} in context of which we work now.
	 * @param componentClass the {@link Class} of component to get description.
	 *
	 * @return the {@link ComponentDescription} of component with given
	 *         {@link Class}.
	 * @throws Exception if no {@link ComponentDescription} can be found.
	 */
	public static ComponentDescription getDescription(AstEditor editor, Class<?> componentClass) throws Exception {
		ComponentDescription description = m_getDescription_Class.get(componentClass);
		if (description == null) {
			description = getDescription0(editor, componentClass);
			m_getDescription_Class.put(componentClass, description);
		}
		return description;
	}

	/**
	 * Implementation for {@link #getDescription(AstEditor, Class)}.
	 */
	private static ComponentDescription getDescription0(AstEditor editor, Class<?> componentClass) throws Exception {
		// we should use component class that can be loaded, for example ignore
		// anonymous classes
		for (;; componentClass = componentClass.getSuperclass()) {
			String componentClassName = componentClass.getName();
			// stop if not an inner class
			int index = componentClassName.indexOf('$');
			if (index == -1) {
				break;
			}
			// stop if anonymous implementation of some interface
			if (componentClass.getInterfaces().length != 0) {
				break;
			}
			// stop if not an anonymous class
			String innerPart = componentClassName.substring(index + 1);
			if (!StringUtils.isNumeric(innerPart)) {
				break;
			}
		}
		// OK, get description
		ComponentDescriptionKey key = new ComponentDescriptionKey(componentClass);
		return getDescription0(editor, key, Collections.emptyList());
	}

	/**
	 * @param editor             the {@link AstEditor} in context of which we work
	 *                           now.
	 * @param componentClassName the name of {@link Class} of component to get
	 *                           description.
	 *
	 * @return the {@link ComponentDescription} of component with given
	 *         {@link Class}.
	 * @throws Exception if no {@link ComponentDescription} can be found.
	 */
	public static ComponentDescription getDescription(AstEditor editor, String componentClassName) throws Exception {
		Class<?> componentClass = EditorState.get(editor).getEditorLoader().loadClass(componentClassName);
		return getDescription(editor, componentClass);
	}

	/**
	 * @return the {@link ComponentDescription} that is specific to given
	 *         {@link ComponentDescriptionKey}, if exists, or just
	 *         {@link ComponentDescription} for given component {@link Class}.
	 */
	private static ComponentDescription getKeySpecificDescription(AstEditor editor, Class<?> componentClass,
			ComponentDescriptionKey key) throws Exception {
		// prepare optional key-specific ResourceInfo
		ResourceInfo resourceInfo;
		{
			EditorState state = EditorState.get(editor);
			ILoadingContext context = EditorStateLoadingContext.get(state);
			String descriptionPath = key.getName() + ".wbp-component.xml";
			resourceInfo = DescriptionHelper.getResourceInfo(context, componentClass, descriptionPath);
		}
		// if no key-specific, use pure type description
		if (resourceInfo == null) {
			return getDescription(editor, componentClass);
		}
		// OK, get key-specific description
		ClassResourceInfo descriptionInfo = new ClassResourceInfo(componentClass, resourceInfo);
		return getDescription0(editor, key, List.of(descriptionInfo));
	}

	/**
	 * @param editor                     the {@link AstEditor} in context of which
	 *                                   we work now.
	 * @param key                        the {@link ComponentDescriptionKey} of
	 *                                   requested {@link ComponentDescription}.
	 * @param additionalDescriptionInfos additional {@link ClassResourceInfo}'s to
	 *                                   parse after {@link ClassResourceInfo}'s
	 *                                   collected for component {@link Class}. May
	 *                                   be empty, but not <code>null</code>.
	 *
	 * @return the {@link ComponentDescription} of component with given
	 *         {@link Class}.
	 * @throws Exception if no {@link ComponentDescription} can be found.
	 */
	private static ComponentDescription getDescription0(AstEditor editor, ComponentDescriptionKey key,
			List<ClassResourceInfo> additionalDescriptionInfos) throws Exception {
		EditorState state = EditorState.get(editor);
		ILoadingContext context = EditorStateLoadingContext.get(state);
		Class<?> componentClass = key.getComponentClass();
		//
		try {
			// prepare result description
			ComponentDescription componentDescription = new ComponentDescription(key);
			addConstructors(editor.getJavaProject(), componentDescription);
			componentDescription.setBeanInfo(ReflectionUtils.getBeanInfo(componentClass));
			componentDescription.setBeanDescriptor(new IntrospectionHelper(componentClass).getBeanDescriptor());
			// prepare list of description resources, from generic to specific
			LinkedList<ClassResourceInfo> descriptionInfos;
			{
				descriptionInfos = new LinkedList<>();
				DescriptionHelper.addDescriptionResources(descriptionInfos, context, componentClass);
				Assert.isTrueException(!descriptionInfos.isEmpty(), ICoreExceptionConstants.DESCRIPTION_NO_DESCRIPTIONS,
						componentClass.getName());
				// at last append additional description resource
				descriptionInfos.addAll(additionalDescriptionInfos);
			}
			// prepare Digester
			Digester digester;
			{
				System.setProperty("org.apache.commons.logging.LogFactory",
						"org.apache.commons.logging.impl.LogFactoryImpl");
				digester = new Digester();
				digester.setLogger(new NoOpLog());
				addRules(digester, editor, componentClass);
			}
			// read descriptions from generic to specific
			for (ClassResourceInfo descriptionInfo : descriptionInfos) {
				ResourceInfo resourceInfo = descriptionInfo.resource;
				// read next description
				{
					componentDescription.setCurrentClass(descriptionInfo.clazz);
					digester.push(componentDescription);
					// do parse
					InputStream is = resourceInfo.getURL().openStream();
					try {
						digester.parse(is);
					} finally {
						IOUtils.closeQuietly(is);
					}
					JAXBContext jaxbContext = ContextFactory.createContext();
					Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
					Component component = (Component) jaxbUnmarshaller.unmarshal(resourceInfo.getURL());
					process(componentDescription, component, editor);
				}
				// clear parts that can not be inherited
				if (descriptionInfo.clazz != componentClass) {
					componentDescription.clearCreations();
					componentDescription.setDescription(null);
				}
			}
			// set toolkit
			if (componentDescription.getToolkit() == null) {
				for (int i = descriptionInfos.size() - 1; i >= 0; i--) {
					ClassResourceInfo descriptionInfo = descriptionInfos.get(i);
					ToolkitDescription toolkit = descriptionInfo.resource.getToolkit();
					if (toolkit != null) {
						componentDescription.setToolkit(toolkit);
						break;
					}
				}
				Assert.isTrueException(componentDescription.getToolkit() != null,
						ICoreExceptionConstants.DESCRIPTION_NO_TOOLKIT, componentClass.getName());
			}
			// icon, default creation
			setIcon(context, componentDescription, componentClass);
			configureDefaultCreation(componentDescription);
			// final operations
			{
				Assert.isNotNull(componentDescription.getModelClass());
				componentDescription.joinProperties();
			}
			// add to caches
			if (key.isPureComponent() && !"true".equals(componentDescription.getParameter("dontCacheDescription"))
					&& shouldCacheDescriptions_inPackage(descriptionInfos.getLast(), componentClass)) {
				componentDescription.setCached(true);
			}
			// mark for caching presentation
			if (shouldCachePresentation(descriptionInfos.getLast(), componentClass)) {
				componentDescription.setPresentationCached(true);
			}
			// use processors
			for (IDescriptionProcessor processor : getDescriptionProcessors()) {
				processor.process(editor, componentDescription);
			}
			// well, we have result
			return componentDescription;
		} catch (SAXParseException e) {
			throw new DesignerException(ICoreExceptionConstants.DESCRIPTION_LOAD_ERROR, e.getException(),
					componentClass.getName());
		}
	}

	/**
	 * Configures default {@link CreationDescription} with valid source.
	 */
	private static void configureDefaultCreation(ComponentDescription componentDescription) {
		Class<?> componentClass = componentDescription.getComponentClass();
		// prepare shortest constructor
		Constructor<?> constructor = ReflectionUtils.getShortestConstructor(componentClass);
		if (constructor == null) {
			return;
		}
		// set default creation
		String source = getDefaultConstructorInvocation(constructor);
		CreationDescription creationDefault = new CreationDescription(componentDescription, null, null);
		creationDefault.setSource(source);
		componentDescription.setCreationDefault(creationDefault);
	}

	/**
	 * TODO move into {@link ReflectionUtils}.
	 *
	 * @return the source for creating {@link Object} using given
	 *         {@link Constructor} with values default for type of each argument.
	 */
	public static String getDefaultConstructorInvocation(Constructor<?> constructor) {
		// prepare Class
		Class<?> componentClass = constructor.getDeclaringClass();
		String componentClassName = ReflectionUtils.getCanonicalName(componentClass);
		// prepare arguments
		String arguments;
		{
			StringBuilder buffer = new StringBuilder();
			for (Class<?> parameter : constructor.getParameterTypes()) {
				String parameterName = ReflectionUtils.getCanonicalName(parameter);
				buffer.append(AstParser.getDefaultValue(parameterName));
				buffer.append(", ");
			}
			arguments = StringUtils.removeEnd(buffer.toString(), ", ");
		}
		// prepare source
		return "new " + componentClassName + "(" + arguments + ")";
	}

	/**
	 * Sets icon for {@link ComponentDescription}.
	 *
	 * @param context              the {@link EditorState} to access environment.
	 * @param componentDescription the {@link ComponentDescription} to set icon for.
	 * @param currentClass         the {@link Class} to check for icon.
	 */
	private static void setIcon(ILoadingContext context, ComponentDescription componentDescription,
			Class<?> currentClass) throws Exception {
		if (currentClass != null) {
			// check current Class
			if (componentDescription.getIcon() == null) {
				ImageDescriptor icon = DescriptionHelper.getIcon(context, currentClass);
				if (icon != null) {
					componentDescription.setIcon(icon);
					return;
				}
			}
			// check interfaces
			for (Class<?> interfaceClass : currentClass.getInterfaces()) {
				if (componentDescription.getIcon() == null) {
					setIcon(context, componentDescription, interfaceClass);
				}
			}
			// check super Class
			if (componentDescription.getIcon() == null) {
				setIcon(context, componentDescription, currentClass.getSuperclass());
			}
		}
	}

	/**
	 * Ensures that {@link AbstractInvocationDescription} is fully initialized. See
	 * {@link AbstractInvocationDescription#setInitialized(boolean)} for more
	 * information.
	 */
	public static void ensureInitialized(final IJavaProject javaProject,
			final AbstractInvocationDescription methodDescription) {
		if (!methodDescription.isInitialized()) {
			methodDescription.setInitialized(true);
			// do initialize
			ExecutionUtils.runIgnore(new RunnableEx() {
				@Override
				public void run() throws Exception {
					IMethod method = CodeUtils.findMethod(javaProject, methodDescription.getDeclaringClass().getName(),
							methodDescription.getSignature());
					if (method != null) {
						String[] parameterNames = method.getParameterNames();
						for (ParameterDescription parameter : methodDescription.getParameters()) {
							if (parameter.getName() == null) {
								int parameterIndex = parameter.getIndex();
								String parameterName = parameterNames[parameterIndex];
								parameter.setName(parameterName);
							}
						}
					}
				}
			});
		}
	}

	/**
	 * Adds {@link ConstructorDescription} for given {@link ComponentDescription}.
	 */
	private static void addConstructors(IJavaProject javaProject, ComponentDescription componentDescription)
			throws Exception {
		Class<?> componentClass = componentDescription.getComponentClass();
		for (Constructor<?> constructor : componentClass.getDeclaredConstructors()) {
			constructor.setAccessible(true);
			ConstructorDescription constructorDescription = new ConstructorDescription(componentClass);
			// add parameter descriptions of constructor
			for (Class<?> parameterType : constructor.getParameterTypes()) {
				addParameter(constructorDescription, parameterType);
			}
			// OK, add constructor description
			constructorDescription.postProcess();
			componentDescription.addConstructor(constructorDescription);
		}
	}

	private static void addParameter(AbstractInvocationDescription description, Class<?> parameterType)
			throws Exception {
		ParameterDescription parameterDescription = new ParameterDescription();
		parameterDescription.setType(parameterType);
		description.addParameter(parameterDescription);
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Rules
	//
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Fills the given {@link ComponentDescription} with the data from the given
	 * {@link Component} model. The component corresponds to a single
	 * {@link wbp-component.xml} file, which has been read via JAXB.
	 */
	private static void process(ComponentDescription componentDescription, Component component, AstEditor editor)
			throws Exception {
		EditorState state = EditorState.get(editor);
		ILoadingContext context = EditorStateLoadingContext.get(state);
		acceptSafe(componentDescription, component.getToolkit(), new ToolkitRule());
		acceptSafe(componentDescription, component.getModel(), new ModelClassRule());
		// component order
		{
			acceptSafe(componentDescription, component.getOrder(), ComponentDescription::setOrder);
		}
		// creations
		{
			for (Creation creation : component.getCreation()) {
				CreationDescription creationDescription = getCreationDescription(componentDescription, creation,
						context);
				addCreationRules(creationDescription, creation);
				for (TagType tagType : creation.getTag()) {
					acceptSafe(creationDescription, tagType, new CreationTagRule());
				}
				TypeParametersType typeParameters = creation.getTypeParameters();
				if (typeParameters != null) {
					for (TypeParameterType typeParameter : typeParameters.getTypeParameter()) {
						acceptSafe(creationDescription, typeParameter, new CreationTypeParametersRule());
					}
				}
				componentDescription.addCreation(creationDescription);
			}
			Creation creationDefault = component.getCreationDefault();
			if (creationDefault != null) {
				CreationDescription creationDescription = getCreationDescription(componentDescription, creationDefault,
						context);
				addCreationRules(creationDescription, creationDefault);
				for (TagType tagType : creationDefault.getTag()) {
					acceptSafe(creationDescription, tagType, new CreationTagRule());
				}
				componentDescription.setCreationDefault(creationDescription);
			}
		}
		// morphing targets
		{
			MorphingType morphingTargets = component.getMorphTargets();
			if (morphingTargets != null) {
				MorphingType morphingType = component.getMorphTargets();
				acceptSafe(componentDescription, morphingType.getNoInherit(), new MorphingNoInheritRule());
				for (MorphingType.MorphTarget morphTarget : morphingType.getMorphTarget()) {
					acceptSafe(componentDescription, morphTarget, new MorphingTargetRule(state));
				}
			}
		}
		// description text
		{
			acceptSafe(componentDescription, component.getDescription(), ComponentDescription::setDescription);
		}
		// exposed children
		{
			if (component.getExposingRules() != null) {
				ExposingRulesType exposingRules = component.getExposingRules();
				for (JAXBElement<ExposingRuleType> exposingRule : exposingRules.getExcludeOrInclude()) {
					acceptSafe(componentDescription, exposingRule, new ExposingRulesRule());
				}
			}
		}
	}

	/**
	 * Adds {@link Rule}'s required for component description parsing.
	 */
	private static void addRules(Digester digester, AstEditor editor, Class<?> componentClass) {
		EditorState state = EditorState.get(editor);
		// standard bean properties
		{
			digester.addRule("component/standard-bean-properties", new StandardBeanPropertiesRule());
			digester.addRule("component/properties-preferred", new StandardBeanPropertiesPreferredRule());
			digester.addRule("component/properties-normal", new StandardBeanPropertiesNormalRule());
			digester.addRule("component/properties-advanced", new StandardBeanPropertiesAdvancedRule());
			digester.addRule("component/properties-hidden", new StandardBeanPropertiesHiddenRule());
			digester.addRule("component/properties-noDefaultValue", new StandardBeanPropertiesNoDefaultValueRule());
			digester.addRule("component/property-tag", new StandardBeanPropertyTagRule());
			{
				String pattern = "component/method-single-property";
				digester.addRule(pattern, new MethodSinglePropertyRule());
				addPropertyConfigurationRules(digester, state, pattern);
			}
			digester.addRule("component/method-property", new MethodPropertyRule(editor.getJavaProject()));
		}
		// public field properties
		{
			digester.addRule("component/public-field-properties", new PublicFieldPropertiesRule());
		}
		// constructors
		{
			String pattern = "component/constructors/constructor";
			digester.addRule(pattern, new ConstructorRule());
			digester.addSetProperties(pattern);
			addParametersRules(digester, pattern + "/parameter", state);
		}
		// methods
		{
			String pattern = "component/methods/method";
			digester.addRule(pattern, new MethodRule());
			digester.addRule(pattern,
					new SetListedPropertiesRule(new String[] { "order" }, new String[] { "orderSpecification" }));
			digester.addRule(pattern + "/tag", new MethodTagRule());
			addParametersRules(digester, pattern + "/parameter", state);
		}
		// method order
		{
			String pattern = "component/method-order";
			digester.addRule(pattern + "/default", new MethodOrderDefaultRule());
			digester.addRule(pattern + "/method", new MethodOrderMethodRule());
			digester.addRule(pattern + "/methods", new MethodOrderMethodsRule());
			digester.addRule(pattern + "/methods/s", new MethodOrderMethodsSignatureRule());
		}
		// methods-exclude, methods-include
		{
			digester.addRule("component/methods/methods-include", new MethodsOperationRule(true));
			digester.addRule("component/methods/methods-exclude", new MethodsOperationRule(false));
		}
		// untyped parameters
		{
			String pattern = "component/parameters/parameter";
			digester.addCallMethod(pattern, "addParameter", 2);
			digester.addCallParam(pattern, 0, "name");
			digester.addCallParam(pattern, 1);
		}
		addPropertiesRules(digester, state);
		addConfigurablePropertiesRules(digester, state);
	}

	/**
	 * Adds {@link Rule}'s for changing {@link GenericPropertyDescription}'s.
	 */
	private static void addPropertiesRules(Digester digester, EditorState state) {
		String propertyAccessPattern = "component/property";
		digester.addRule(propertyAccessPattern, new PropertyAccessRule());
		addPropertyConfigurationRules(digester, state, propertyAccessPattern);
	}

	/**
	 * Adds {@link Rule}'s for configuring {@link GenericPropertyDescription} on
	 * stack.
	 */
	private static void addPropertyConfigurationRules(Digester digester, EditorState state,
			String propertyAccessPattern) {
		// category
		{
			String pattern = propertyAccessPattern + "/category";
			digester.addRule(pattern, new PropertyCategoryRule());
		}
		// editor
		{
			String pattern = propertyAccessPattern + "/editor";
			digester.addRule(pattern, new PropertyEditorRule(state));
			addConfigurableObjectParametersRules(digester, pattern);
		}
		// defaultValue
		{
			String pattern = propertyAccessPattern + "/defaultValue";
			ClassLoader classLoader = state.getEditorLoader();
			digester.addRule(pattern, new PropertyDefaultRule(classLoader));
		}
		// getter
		{
			String pattern = propertyAccessPattern + "/getter";
			digester.addRule(pattern, new PropertyGetterRule());
		}
		// tag
		{
			String pattern = propertyAccessPattern + "/tag";
			digester.addRule(pattern, new PropertyTagRule());
		}
	}

	/**
	 * Adds {@link Rule}'s for adding {@link ConfigurablePropertyDescription}'s.
	 */
	private static void addConfigurablePropertiesRules(Digester digester, EditorState state) {
		String pattern = "component/add-property";
		digester.addRule(pattern, new ConfigurablePropertyRule());
		addConfigurableObjectParametersRules(digester, pattern);
	}

	/**
	 * Adds {@link Rule}'s for configuring {@link AbstractConfigurableDescription}.
	 */
	private static void addConfigurableObjectParametersRules(Digester digester, String pattern) {
		digester.addRule(pattern + "/parameter", new ConfigurableObjectParameterRule());
		digester.addRule(pattern + "/parameter-list", new ConfigurableObjectListParameterRule());
	}

	/**
	 * Adds {@link Rule}'s for parsing {@link CreationDescription}'s.
	 */
	private static void addCreationRules(CreationDescription creationDescription, Creation creation) throws Exception {
		// description
		{
			acceptSafe(creationDescription, creation.getDescription(), CreationDescription::setDescription);
		}
		// source
		{
			acceptSafe(creationDescription, creation.getSource(), CreationDescription::setSource);
		}
		// invocation
		{
			for (Creation.Invocation invocation : creation.getInvocation()) {
				CreationInvocationDescription invocationDescription = new CreationInvocationDescription();
				invocationDescription.setSignature(invocation.getSignature());
				// arguments
				invocationDescription.setArguments(invocation.getContent());
				// add
				creationDescription.addInvocation(invocationDescription);
			}
		}
		// untyped parameters
		{
			for (Creation.Parameter parameter : creation.getParameter()) {
				creationDescription.addParameter(parameter.getName(), parameter.getContent());
			}
		}
	}

	/**
	 * Adds {@link Rule}'s for parsing {@link ParameterDescription}'s.
	 */
	static void addParametersRules(Digester digester, String pattern, EditorState state) {
		ClassLoader classLoader = state.getEditorLoader();
		//
		digester.addRule(pattern, new ObjectCreateRule(ParameterDescription.class));
		digester.addRule(pattern, new SetClassPropertyRule(classLoader, "type"));
		digester.addRule(pattern, new SetListedPropertiesRule(
				new String[] { "name", "defaultSource", "parent", "child", "property", "parent2", "child2" }));
		digester.addSetNext(pattern, "addParameter");
		// editors
		{
			String editorPattern = pattern + "/editor";
			digester.addRule(editorPattern, new ParameterEditorRule(state));
			addConfigurableObjectParametersRules(digester, editorPattern);
		}
		// tags
		digester.addRule(pattern + "/tag", new ParameterTagRule());
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Utils
	//
	////////////////////////////////////////////////////////////////////////////
	private static boolean shouldCacheDescriptions_inPackage(ClassResourceInfo descriptionInfo, Class<?> componentClass)
			throws Exception {
		return hasMarkerFileForPackage(descriptionInfo, componentClass, ".wbp-cache-descriptions");
	}

	private static boolean shouldCachePresentation(ClassResourceInfo descriptionInfo, Class<?> componentClass)
			throws Exception {
		if (descriptionInfo.clazz == componentClass) {
			Bundle bundle = descriptionInfo.resource.getBundle();
			if (bundle != null) {
				return bundle.getEntry("wbp-meta/.wbp-cache-presentations") != null;
			}
		}
		return false;
	}

	/**
	 * Checks if package of given class has marker file with given name.
	 *
	 * @param descriptionInfo the first {@link ClassResourceInfo} for
	 *                        <code>*.wbp-component.xml</code>, in "to superclass"
	 *                        direction. We look into its {@link Bundle} for caching
	 *                        marker.
	 *
	 * @return <code>true</code> if package with given class has "cache enabled"
	 *         marker.
	 */
	private static boolean hasMarkerFileForPackage(ClassResourceInfo descriptionInfo, Class<?> componentClass,
			String markerFileName) throws Exception {
		ResourceInfo resourceInfo = descriptionInfo.resource;
		if (resourceInfo.getBundle() != null) {
			String packageName = CodeUtils.getPackage(componentClass.getName());
			String markerName = packageName.replace('.', '/') + "/" + markerFileName;
			return DescriptionHelper.getResourceInfo(null, resourceInfo.getBundle(), markerName) != null;
		}
		return false;
	}

	/**
	 * @return the instances of {@link IDescriptionProcessor}.
	 */
	public static List<IDescriptionProcessor> getDescriptionProcessors() {
		return ExternalFactoriesHelper.getElementsInstances(IDescriptionProcessor.class,
				"org.eclipse.wb.core.descriptionProcessors", "processor");
	}

	private static CreationDescription getCreationDescription(ComponentDescription componentDescription,
			Creation creation, ILoadingContext context) throws Exception {
		// prepare creation
		String id = creation.getId();
		String name = creation.getName();
		CreationDescription creationDescription = new CreationDescription(componentDescription, id, name);
		// set optional specific icon
		if (id != null) {
			Class<?> componentClass = componentDescription.getComponentClass();
			String suffix = "_" + id;
			creationDescription.setIcon(DescriptionHelper.getIcon(context, componentClass, suffix));
		}
		// OK, configured creation
		return creationDescription;
	}

	/**
	 * Null-safe invocation of {@link FailableBiConsumer#accept(Object, Object)}, in
	 * order to better handle optional model parameters. Does nothing if
	 * {@code model} is {@code null}.
	 */
	private static <U, T> void acceptSafe(U description, T model, FailableBiConsumer<U, T, ?> consumer)
			throws Exception {
		if (model == null) {
			return;
		}
		consumer.accept(description, model);
	}

	@Deprecated
	@FunctionalInterface
	/**
	 * @deprecated Going to be removed by Commons Lang3 FailableBiConsumer
	 */
	public static interface FailableBiConsumer<T, U, E extends Exception> {
		void accept(T t, U u) throws E;
	}
}
