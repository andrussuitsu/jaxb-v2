/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.bind.api;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.bind.api.impl.NameConverter;
import com.sun.xml.bind.v2.model.annotation.RuntimeAnnotationReader;
import com.sun.xml.bind.v2.model.nav.Navigator;

/**
 * {@link JAXBContext} enhanced with JAXB RI specific functionalities.
 *
 * <p>
 * <b>Subject to change without notice</b>.
 * 
 * @since 2.0 EA1
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public abstract class JAXBRIContext extends JAXBContext {
    protected JAXBRIContext() {}

    /**
     * Creates a new {@link JAXBRIContext}.
     *
     * <p>
     * {@link JAXBContext#newInstance(Class[]) JAXBContext.newInstance()} methods may
     * return other JAXB providers that are not compatible with the JAX-RPC RI.
     * This method guarantees that the JAX-WS RI will finds the JAXB RI.
     *
     * @param classes
     *      Classes to be bound. See {@link JAXBContext#newInstance(Class[])} for the meaning.
     * @param typeRefs
     *      See {@link #TYPE_REFERENCES} for the meaning of this parameter.
     *      Can be null.
     * @param defaultNamespaceRemap
     *      See {@link #DEFAULT_NAMESPACE_REMAP} for the meaning of this parameter.
     *      Can be null (and should be null for ordinary use of JAXB.)
     * @param c14nSupport
     *      See {@link #CANONICALIZATION_SUPPORT} for the meaning of this parameter.
     */
    public static JAXBRIContext newInstance(@NotNull Class[] classes, @Nullable Collection<TypeReference> typeRefs, @Nullable String defaultNamespaceRemap, boolean c14nSupport ) throws JAXBException {
        try {
            Class c = Class.forName("com.sun.xml.bind.v2.ContextFactory");
            Method method = c.getMethod("createContext",Class[].class,Collection.class,String.class,boolean.class, RuntimeAnnotationReader.class);
            Object o = method.invoke(null,classes,typeRefs,defaultNamespaceRemap,c14nSupport,null);
            return (JAXBRIContext)o;
        } catch (ClassNotFoundException e) {
            throw new JAXBException(e);
        } catch (NoSuchMethodException e) {
            throw new JAXBException(e);
        } catch (IllegalAccessException e) {
            throw new JAXBException(e);
        } catch (InvocationTargetException e) {
            Throwable te = e.getTargetException();
            if(te instanceof JAXBException)
                throw (JAXBException)te;
            if(te instanceof RuntimeException)
                throw (RuntimeException)te;
            if(te instanceof Error)
                throw (Error)te;
            throw new JAXBException(e);
        }
    }

    /**
     * If the given object is bound to an element in XML by JAXB,
     * returns the element name.
     *
     * @return null
     *      if the object is not bound to an element.
     * @throws JAXBException
     *      if the object is not known to this context.
     *
     * @since 2.0 EA1
     */
    public abstract @Nullable QName getElementName(@NotNull Object o) throws JAXBException;

    /**
     * Creates a mini-marshaller/unmarshaller that can process a {@link TypeReference}.
     *
     * @return
     *      null if the specified reference is not given to {@link JAXBRIContext#newInstance}.
     *
     * @since 2.0 EA1
     */
    public abstract Bridge createBridge(@NotNull TypeReference ref);

    /**
     * Creates a new {@link BridgeContext} instance.
     *
     * @return
     *      always a valid non-null instance.
     *
     * @since 2.0 EA1
     */
    public abstract @NotNull BridgeContext createBridgeContext();

    /**
     * Gets a {@link RawAccessor} for the specified element property of the specified wrapper bean class.
     *
     * <p>
     * This method is designed to assist the JAX-RPC RI fill in a wrapper bean (in the doc/lit/wrap mode.)
     * In the said mode, a wrapper bean is supposed to only have properties that match elements,
     * and for each element that appear in the content model there's one property.
     *
     * <p>
     * Therefore, this method takes a wrapper bean and a tag name that identifies a property
     * on the given wrapper bean, then returns a {@link RawAccessor} that allows the caller
     * to set/get a value from the property of the bean.
     *
     * <p>
     * This method is not designed for a performance. The caller is expected to cache the result.
     *
     * @param <B>
     *      type of the wrapper bean
     * @param <V>
     *      type of the property of the bean
     * @return
     *      always return non-null valid accessor object.
     * @throws JAXBException
     *      if the specified wrapper bean is not bound by JAXB, or if it doesn't have an element property
     *      of the given name.
     *
     * @since 2.0 EA1
     */
    public abstract <B,V> RawAccessor<B,V> getElementPropertyAccessor( Class<B> wrapperBean, String nsUri, String localName )
            throws JAXBException;

    /**
     * Gets the namespace URIs statically known to this {@link JAXBContext}.
     *
     * <p>
     * When JAXB is used to marshal into sub-trees, it declares
     * these namespace URIs at each top-level element that it marshals.
     *
     * To avoid repeated namespace declarations at sub-elements, the application
     * may declare those namespaces at a higher level.
     *
     * @return
     *      always non-null.
     *
     * @since 2.0 EA2
     */
    public abstract @NotNull List<String> getKnownNamespaceURIs();


    /**
     * Generates the schema documents from the model.
     *
     * <p>
     * The caller can use the additionalElementDecls parameter to
     * add element declarations to the generate schema.
     * For example, if the JAX-RPC passes in the following entry:
     *
     * {foo}bar -> DeclaredType for java.lang.String
     *
     * then JAXB generates the following element declaration (in the schema
     * document for the namespace "foo")"
     *
     * &lt;xs:element name="bar" type="xs:string" />
     *
     * This can be used for generating schema components necessary for WSDL.
     *
     * @param outputResolver
     *      this object controls the output to which schemas
     *      will be sent.
     *
     * @throws IOException
     *      if {@link SchemaOutputResolver} throws an {@link IOException}.
     */
    public abstract void generateSchema(@NotNull SchemaOutputResolver outputResolver) throws IOException;

    /**
     * Returns the name of the XML Type bound to the
     * specified Java type.
     *
     * @param tr
     *      must not be null. This must be one of the {@link TypeReference}s specified
     *      in the {@link JAXBRIContext#newInstance} method.
     *
     * @throws IllegalArgumentException
     *      if the parameter is null or not a part of the {@link TypeReference}s specified
     *      in the {@link JAXBRIContext#newInstance} method.
     *
     * @return null
     *      if the referenced type is an anonymous and therefore doesn't have a name. 
     */
    public abstract QName getTypeName(@NotNull TypeReference tr);

    /**
     * Gets the build information of the JAXB runtime.
     *
     * @return
     *      may be null, if the runtime is loaded by a class loader that doesn't support
     *      the access to the manifest informatino.
     */
    public abstract @NotNull String getBuildId();

    /**
     * Generates the episode file that represents the binding known to this {@link JAXBContext},
     * so that XJC can later do separate compilation.
     *
     * <p>
     * Episode file is really just a JAXB customization file, except that currently
     * we use the RI-specific SCD to refer to schema components.
     *
     * @param output
     *      This receives the generated episode file.
     *
     * @since 2.1
     */
    public abstract void generateEpisode(Result output);

    /**
     * Computes a Java identifier from a local name.
     *
     * <p>
     * This method faithfully implements the name mangling rule as specified in the JAXB spec.
     *
     * <p>
     * In JAXB, a collision with a Java reserved word (such as "return") never happens.
     * Accordingly, this method may return an identifier that collides with reserved words.
     *
     * <p>
     * Use {@link JJavaName#isJavaIdentifier(String)} to check for such collision.
     *
     * @return
     *      Typically, this method returns "nameLikeThis".
     *
     * @see JJavaName#isJavaIdentifier(String)
     */
    public static @NotNull String mangleNameToVariableName(@NotNull String localName) {
        return NameConverter.standard.toVariableName(localName);
    }

    /**
     * Computes a Java class name from a local name.
     *
     * <p>
     * This method faithfully implements the name mangling rule as specified in the JAXB spec.
     *
     * @return
     *      Typically, this method returns "NameLikeThis".
     */
    public static @NotNull String mangleNameToClassName(@NotNull String localName) {
        return NameConverter.standard.toClassName(localName);
    }

    /**
     * Gets the parameterization of the given base type.
     *
     * <p>
     * For example, given the following
     * <pre><xmp>
     * interface Foo<T> extends List<List<T>> {}
     * interface Bar extends Foo<String> {}
     * </xmp></pre>
     * This method works like this:
     * <pre><xmp>
     * getBaseClass( Bar, List ) = List<List<String>
     * getBaseClass( Bar, Foo  ) = Foo<String>
     * getBaseClass( Foo<? extends Number>, Collection ) = Collection<List<? extends Number>>
     * getBaseClass( ArrayList<? extends BigInteger>, List ) = List<? extends BigInteger>
     * </xmp></pre>
     *
     * @param type
     *      The type that derives from {@code baseType}
     * @param baseType
     *      The class whose parameterization we are interested in.
     * @return
     *      The use of {@code baseType} in {@code type}.
     *      or null if the type is not assignable to the base type.
     * @since 2.0 FCS
     */
    public static @Nullable Type getBaseType(@NotNull Type type, @NotNull Class baseType) {
        return Navigator.REFLECTION.getBaseClass(type,baseType);
    }


    /**
     * The property that you can specify to {@link JAXBContext#newInstance}
     * to reassign the default namespace URI to something else at the runtime.
     *
     * <p>
     * The value of the property is {@link String}, and it is used as the namespace URI
     * that succeeds the default namespace URI.
     *
     * @since 2.0 EA1
     */
    public static final String DEFAULT_NAMESPACE_REMAP = "com.sun.xml.bind.defaultNamespaceRemap";

    /**
     * The property that you can specify to {@link JAXBContext#newInstance}
     * to put additional JAXB type references into the {@link JAXBContext}.
     *
     * <p>
     * The value of the property is {@link Collection}&lt;{@link TypeReference}>.
     * Those {@link TypeReference}s can then be used to create {@link Bridge}s.
     *
     * <p>
     * This mechanism allows additional element declarations that were not a part of
     * the schema into the created {@link JAXBContext}.
     *
     * @since 2.0 EA1
     */
    public static final String TYPE_REFERENCES = "com.sun.xml.bind.typeReferences";

    /**
     * The property that you can specify to {@link JAXBContext#newInstance}
     * and {@link Marshaller#setProperty(String, Object)}
     * to enable the c14n marshalling support in the {@link JAXBContext}.
     *
     * @see C14nSupport_ArchitectureDocument
     * @since 2.0 EA2
     */
    public static final String CANONICALIZATION_SUPPORT = "com.sun.xml.bind.c14n";

    /**
     * The property that you can specify to {@link JAXBContext#newInstance}
     * to use alternative {@link RuntimeAnnotationReader} implementation.
     *
     * @since 2.1 EA2
     */
    public static final String ANNOTATION_READER = RuntimeAnnotationReader.class.getName();

    /**
     * Marshaller/Unmarshaller property to enable XOP processing.
     *
     * @since 2.0 EA2
     */
    public static final String ENABLE_XOP = "com.sun.xml.bind.XOP";
}
