package com.sun.xml.bind.v2.runtime;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.sun.xml.bind.api.AccessorException;
import com.sun.xml.bind.v2.model.runtime.RuntimeLeafInfo;
import com.sun.xml.bind.v2.runtime.property.Unmarshaller;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingEventHandler;

import org.xml.sax.SAXException;

/**
 * {@link JaxBeanInfo} implementation for immutable leaf classes.
 *
 * <p>
 * Leaf classes are always bound to a text and they are often immutable.
 * The JAXB spec allows this binding for a few special Java classes plus
 * type-safe enums.
 *
 * <p>
 * This implementation obtains necessary information from {@link RuntimeLeafInfo}.
 *
 * @author Kohsuke Kawaguchi
 */
final class LeafBeanInfoImpl<BeanT> extends JaxBeanInfo<BeanT> {

    private final Unmarshaller.Handler unmarshaller;

    private final Transducer<BeanT> xducer;

    public LeafBeanInfoImpl(JAXBContextImpl grammar, RuntimeLeafInfo li) {
        super(grammar,li,li.getClazz(),li.getTypeName(),false,true);

        xducer = li.getTransducer();

        unmarshaller = new Unmarshaller.RawTextHandler(
            Unmarshaller.ERROR,Unmarshaller.REVERT_TO_PARENT) {
            public void processText(UnmarshallingContext context, CharSequence s) throws SAXException {
                try {
                    context.setTarget(xducer.parse(s));
                } catch( Exception e ) {
                    handleParseConversionException(context,e);
                }
            }
        };
    }

    public final String getElementNamespaceURI(BeanT _) {
        throw new UnsupportedOperationException();
    }

    public final String getElementLocalName(BeanT _) {
        throw new UnsupportedOperationException();
    }

    public BeanT createInstance(UnmarshallingContext context) {
        throw new UnsupportedOperationException();
    }

    public final boolean reset(BeanT bean, UnmarshallingContext context) {
        return false;
    }

    public final String getId(BeanT bean, XMLSerializer target) {
        return null;
    }

    public final void serializeBody(BeanT bean, XMLSerializer w) throws SAXException, IOException, XMLStreamException {
        // most of the times leaves are printed as leaf element/attribute property,
        // so this code is only used for example when you have multiple XmlElement on a property
        // and some of them are leaves. Hence this doesn't need to be super-fast.
        try {
            w.text(xducer.print(bean),null);
        } catch (AccessorException e) {
            w.reportError(null,e);
        }
    }

    public final void serializeAttributes(BeanT bean, XMLSerializer target) {
        // noop
    }

    public final void serializeRoot(BeanT bean, XMLSerializer target) throws SAXException, IOException, XMLStreamException {
        serializeBody(bean,target);
    }

    public final void serializeURIs(BeanT bean, XMLSerializer target) throws SAXException {
        // TODO: maybe we should create another LeafBeanInfoImpl class for
        // context-dependent xducers?
        if(xducer.useNamespace()) {
            try {
                xducer.declareNamespace(bean,target);
            } catch (AccessorException e) {
                target.reportError(null,e);
            }
        }
    }

    public final UnmarshallingEventHandler getUnmarshaller(boolean root) {
        return unmarshaller;
    }

    public Transducer<BeanT> getTransducer() {
        return xducer;
    }
}
