package com.poterin.andorra;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class XMLUtil {
    public static String domToString(Document document, boolean formatted) throws Exception {
        return new String(domToBytes(document, formatted), "UTF-8");
    } // domToString

    public static byte[] domToBytes(Document document, boolean formatted) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");

        if (formatted) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        transformer.transform(new DOMSource(document), new StreamResult(bytes));

        return bytes.toByteArray();
    } // domToBytes

    public static String nodeToString(Node node)
        throws TransformerException, TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        StringWriter stringWriter = new StringWriter();

        transformer.transform(new DOMSource(node), new StreamResult(stringWriter));

        return stringWriter.toString();
    } // nodeToString

    public static Node findFirstNode(Node atNode, String nodeName) {
        NodeList childs = atNode.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            if (childs.item(i).getNodeName().equals(nodeName)) return childs.item(i);
        }
        return null;
    } // findFirstNode

    public static String getNodeText(Node atNode, String nodeName)
        throws UnsupportedEncodingException {
        Node n = findFirstNode(atNode, nodeName);
        if (n == null)
            return "";
        else
            n = n.getFirstChild();
        if (n == null)
            return "";
        else
            return new String(n.getNodeValue().getBytes("UTF-8"), "UTF-8");
    } // getNodeText

    public static Document parseFile(String fileName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(fileName));
    }

    public static Document parseString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    public static Document parseStream(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(inputStream);
    }

    public static Node getNodeByPath
        (Document document,
         Node currentNode,
         String path,
         boolean createIfNew) throws Exception {
        int begPos, endPos;
        String curNodeName;
        Node newNode, result;

        if (path.charAt(0) == '/') {
            result = document.getDocumentElement();
            begPos = 1;
        } else {
            result = currentNode;
            begPos = 0;
        }

        endPos = path.indexOf('/', begPos);
        if (endPos == -1 && begPos < path.length()) endPos = path.length();

        while (endPos > -1) {
            curNodeName = path.substring(begPos, endPos);

            if (curNodeName.equals(".."))
                if (result != null)
                    newNode = result.getParentNode();
                else
                    newNode = null;
            else
                newNode = findFirstNode(result, curNodeName);

            if (newNode == null) {
                if (createIfNew && result != null)
                    newNode = result.appendChild(document.createElement(curNodeName));
                else
                    throw new Exception("Path '" + path + "' not found");
            }

            result = newNode;
            begPos = endPos + 1;
            endPos = path.indexOf('/', begPos);
            if (endPos == -1 && begPos < path.length()) endPos = path.length();
        } // while

        return result;
    } // getNodeByPath

    public static void dropVoidTexts(Element element) throws Exception {
        NodeList nl = element.getChildNodes();

        for (int i = nl.getLength() - 1; i >= 0; i--) {
            Node n = nl.item(i);

            switch (n.getNodeType()) {
                case Node.TEXT_NODE: {
                    String nodeValue = n.getNodeValue();
                    boolean isVoid = true;
                    for (int j = 0; j < nodeValue.length(); j++) {
                        char ch = nodeValue.charAt(j);
                        if (!(ch == '\t' || ch == ' ' || ch == '\n' || ch == '\r')) {
                            isVoid = false;
                            break;
                        }
                    } // for
                    if (isVoid) element.removeChild(n);
                    break;
                } // TEXT_NODE

                case Node.ELEMENT_NODE: {
                    dropVoidTexts((Element) n);
                    break;
                } // ELEMENT_NODE
            } // switch
        } // for
    } // dropVoidTexts;

    public static class CommonNamespaceContext implements NamespaceContext {

        private Map<String, String> map;

        public CommonNamespaceContext() {
            map = new HashMap<String, String>();
        }

        public void setNamespaceURI(String prefix, String namespaceURI) {
            map.put(prefix, namespaceURI);
        }

        public String getNamespaceURI(String prefix) {
            return map.get(prefix);
        }

        public String getPrefix(String namespaceURI) {

            Set keys = map.keySet();

            for (Iterator i = keys.iterator(); i.hasNext(); ) {
                String prefix = (String) i.next();
                String uri = map.get(prefix);
                if (uri.equals(namespaceURI)) return prefix;
            }

            return null;
        }

        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }
    } // CommonNamespaceContext

    public static Element findElement(Node node, String path) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XMLUtil.CommonNamespaceContext nsContext = new XMLUtil.CommonNamespaceContext();
        nsContext.setNamespaceURI("xsl", "http://www.w3.org/1999/XSL/Transform");
        xpath.setNamespaceContext(nsContext);

        return (Element) xpath.evaluate(path, node, XPathConstants.NODE);
    }

    public static Document newDocument(final String rootName) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        document.appendChild(document.createElement(rootName));
        return document;
    } // newDocument

    public static void removeAllChilds(Node node)
    {
        NodeList childs = node.getChildNodes();

        for(int i = 0; i < childs.getLength(); i++) {
            if (childs.item(i).hasChildNodes()) removeAllChilds(childs.item(i));
            node.removeChild(childs.item(i));
        }
    }

} // XMLUtil
