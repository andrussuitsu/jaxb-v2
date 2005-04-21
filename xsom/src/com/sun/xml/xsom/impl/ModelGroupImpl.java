/*
 * @(#)$Id: ModelGroupImpl.java,v 1.2 2005-04-21 16:42:13 kohsuke Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.xml.xsom.impl;

import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSTermFunction;
import com.sun.xml.xsom.visitor.XSTermFunctionWithParam;
import com.sun.xml.xsom.visitor.XSTermVisitor;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

public class ModelGroupImpl extends ComponentImpl implements XSModelGroup, Ref.Term
{
    public ModelGroupImpl( SchemaImpl owner, AnnotationImpl _annon, Locator _loc, ForeignAttributesImpl _fa,
        Compositor _compositor, ParticleImpl[] _children ) {
        
        super(owner,_annon,_loc,_fa);
        this.compositor = _compositor;
        this.children = _children;
        
        if(compositor==null)
            throw new IllegalArgumentException();
        for( int i=children.length-1; i>=0; i-- )
            if(children[i]==null)
                throw new IllegalArgumentException();
    }
    
    private final ParticleImpl[] children;
    public ParticleImpl getChild( int idx ) { return children[idx]; }
    public int getSize() { return children.length; }
    
    public ParticleImpl[] getChildren() { return children; }

    
    private final Compositor compositor;
    public Compositor getCompositor() { return compositor; }
    
    
    public void redefine(ModelGroupDeclImpl oldMG) {
        for (ParticleImpl p : children)
            p.redefine(oldMG);
    }



    public boolean isWildcard()                 { return false; }
    public boolean isModelGroupDecl()           { return false; }
    public boolean isModelGroup()               { return true; }
    public boolean isElementDecl()              { return false; }

    public XSWildcard asWildcard()              { return null; }
    public XSModelGroupDecl asModelGroupDecl()  { return null; }
    public XSModelGroup asModelGroup()          { return this; }
    public XSElementDecl asElementDecl()        { return null; }



    public void visit( XSVisitor visitor ) {
        visitor.modelGroup(this);
    }
    public void visit( XSTermVisitor visitor ) {
        visitor.modelGroup(this);
    }
    public Object apply( XSTermFunction function ) {
        return function.modelGroup(this);
    }

    public <T,P> T apply(XSTermFunctionWithParam<T, P> function, P param) {
        return function.modelGroup(this,param);
    }

    public Object apply( XSFunction function ) {
        return function.modelGroup(this);
    }

    // Ref.Term implementation
    public XSTerm getTerm() { return this; }
}
