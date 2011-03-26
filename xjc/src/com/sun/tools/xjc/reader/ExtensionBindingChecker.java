/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.tools.xjc.reader;

import java.util.StringTokenizer;

import com.sun.tools.xjc.Options;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * This filter checks jaxb:extensionBindingPrefix and
 * pass/filter extension bindings.
 * 
 * <p>
 * This filter also remembers enabled extension namespaces
 * and filters out any extension namespaces that doesn't belong
 * to those. The net effect is that disabled customizations
 * will never pass through this filter.
 *
 * <p>
 * Note that we can't just filter out all foreign namespaces,
 * as we need to use user-defined tags in documentations to generate javadoc.
 * 
 * <p>
 * The class needs to know the list of extension binding namespaces
 * that the RI recognizes.
 * To add new URI, modify the isSupportedExtension method.
 *
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public final class ExtensionBindingChecker extends AbstractExtensionBindingChecker {

    /**
     * Number of the elements encountered. Used to detect the root element.
     */
    private int count=0;

    public ExtensionBindingChecker(String schemaLanguage, Options options, ErrorHandler handler) {
        super(schemaLanguage, options, handler);
    }

    /**
     * Returns true if the elements with the given namespace URI
     * should be blocked by this filter.
     */
    private boolean needsToBePruned( String uri ) {
        if( uri.equals(schemaLanguage) )
            return false;
        if( uri.equals(Const.JAXB_NSURI) )
            return false;
        if( enabledExtensions.contains(uri) )
            return false;
        
        // we don't need to prune something unless
        // the rest of the processor recognizes it as something special.
        // this allows us to send the documentation and other harmless
        // foreign XML fragments, which may be picked up as documents.
        return isRecognizableExtension(uri);
    }


    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        count=0;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
        
        if(!isCutting()) {
            String v = atts.getValue(Const.JAXB_NSURI,"extensionBindingPrefixes");
            if(v!=null) {
                if(count!=0)
                    // the binding attribute is allowed only at the root level.
                    error( Messages.ERR_UNEXPECTED_EXTENSION_BINDING_PREFIXES.format() );

                if(!allowExtensions)
                    error( Messages.ERR_VENDOR_EXTENSION_DISALLOWED_IN_STRICT_MODE.format() );

                // then remember the associated namespace URIs.
                StringTokenizer tokens = new StringTokenizer(v);
                while(tokens.hasMoreTokens()) {
                    String prefix = tokens.nextToken();
                    String uri = nsSupport.getURI(prefix);
                    if( uri==null )
                        // undeclared prefix
                        error( Messages.ERR_UNDECLARED_PREFIX.format(prefix) );
                    else
                        checkAndEnable(uri);
                }
            }
            
            if( needsToBePruned(namespaceURI) ) {
                // start pruning the tree. Call the super class method directly.
                if( isRecognizableExtension(namespaceURI) ) {
                    // but this is a supported customization.
                    // isn't the user forgetting @jaxb:extensionBindingPrefixes?
                    warning( Messages.ERR_SUPPORTED_EXTENSION_IGNORED.format(namespaceURI) );
                }
                startCutting();
            } else
                verifyTagName(namespaceURI, localName, qName);
        }
        
        count++;
        super.startElement(namespaceURI, localName, qName, atts);
    }
}
