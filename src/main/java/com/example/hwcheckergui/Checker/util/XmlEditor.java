package com.example.hwcheckergui.Checker.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class XmlEditor {

    private static Element getPlugin(Document doc, String packageMainClass) {
        Element plugin = doc.createElement("plugin");
        //////////////////
        Element groupId = doc.createElement("groupId");
        groupId.appendChild(doc.createTextNode("org.apache.maven.plugins"));
        ////////////////
        Element artifactId = doc.createElement("artifactId");
        artifactId.appendChild(doc.createTextNode("maven-assembly-plugin"));
        ////////////////
        Element configuration = doc.createElement("configuration");
        Element descriptorRefs = doc.createElement("descriptorRefs");
        Element descriptorRef = doc.createElement("descriptorRef");
        descriptorRef.appendChild(doc.createTextNode("jar-with-dependencies"));
        descriptorRefs.appendChild(descriptorRef);
        Element archive = doc.createElement("archive");
        Element manifest = doc.createElement("manifest");
        Element mainClass = doc.createElement("mainClass");
        mainClass.appendChild(doc.createTextNode(packageMainClass));
        manifest.appendChild(mainClass);
        archive.appendChild(manifest);
        configuration.appendChild(descriptorRefs);
        configuration.appendChild(archive);
        Element executions = doc.createElement("executions");
        Element execution = doc.createElement("execution");
        Element id = doc.createElement("id");
        id.appendChild(doc.createTextNode("make-assembly"));
        Element phase = doc.createElement("phase");
        phase.appendChild(doc.createTextNode("package"));
        Element goals = doc.createElement("goals");
        Element goal = doc.createElement("goal");
        goal.appendChild(doc.createTextNode("single"));
        goals.appendChild(goal);
        execution.appendChild(id);
        execution.appendChild(phase);
        execution.appendChild(goals);
        executions.appendChild(execution);
        ///////////////////
        plugin.appendChild(groupId);
        plugin.appendChild(artifactId);
        plugin.appendChild(configuration);
        plugin.appendChild(executions);
        return plugin;
    }

    private static Element getDependency(Document doc) {
        Element dependency = doc.createElement("dependency");
        //////////////////
        Element groupId = doc.createElement("groupId");
        groupId.appendChild(doc.createTextNode("org.apache.maven.plugins"));
        ////////////////
        Element artifactId = doc.createElement("artifactId");
        artifactId.appendChild(doc.createTextNode("maven-assembly-plugin"));
        ////////////////
        Element version = doc.createElement("version");
        version.appendChild(doc.createTextNode("3.4.2"));
        ////////////////
        Element type = doc.createElement("type");
        type.appendChild(doc.createTextNode("maven-plugin"));
        ////////////////
        dependency.appendChild(groupId);
        dependency.appendChild(artifactId);
        dependency.appendChild(version);
        dependency.appendChild(type);
        return dependency;
    }

    public static void execute(File f, String packageMainClass) {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String pomPath = null;
        boolean readSuccessful = false;
        Document doc = null;

        try (InputStream is = new FileInputStream(f)) {

            DocumentBuilder db = dbf.newDocumentBuilder();

            doc = db.parse(is);

            NodeList pluginNodeList = doc.getElementsByTagName("plugin");

            if (doc.getElementsByTagName("build").getLength() == 0) {
                // add build
                Element build = doc.createElement("build");
                doc.getElementsByTagName("project").item(0).appendChild(build);
            }
            NodeList buildNodeList = doc.getElementsByTagName("build");
            if (buildNodeList.item(0).getChildNodes().getLength() == 0) {
                // add plugins
                Element plugins = doc.createElement("plugins");
                doc.getElementsByTagName("build").item(0).appendChild(plugins);
            }

            NodeList pluginsNodeList = doc.getElementsByTagName("plugins");

            if (pluginsNodeList.item(0).getChildNodes().getLength() == 0) { // if plugins has no plugin -> add plugin
                // add plugin
                Element plugin = getPlugin(doc, packageMainClass);
                //////////////////
                doc.getElementsByTagName("plugins").item(0).appendChild(plugin);
            } else {
                boolean requiredPluginExists = false;
                boolean sameGroupIdArtifactIdExist = false;
                Element sameGroupIdArtifactId = null;
                Element requiredPlugin = getPlugin(doc, packageMainClass);
                NodeList pluginList = pluginsNodeList.item(0).getChildNodes();
                for (int i = 0; i < pluginList.getLength(); i++) {
                    Node plugin = pluginList.item(i);
                    ///
                    NodeList pluginData = plugin.getChildNodes();
                    String groupIdValue = null;
                    String artifactIdValue = null;
                    for (int j = 0; j < pluginData.getLength(); j++) {
                        Node pluginJ = pluginData.item(j);
                        if (pluginJ.getNodeName().equals("groupId")) {
                            groupIdValue = pluginJ.getTextContent();
                        }
                        if (pluginJ.getNodeName().equals("artifactId")) {
                            artifactIdValue = pluginJ.getTextContent();
                        }
                    }
                    if (("org.apache.maven.plugins".equals(groupIdValue)) && ("maven-assembly-plugin".equals(artifactIdValue))) {
                        sameGroupIdArtifactIdExist = true;
                        sameGroupIdArtifactId = (Element) plugin;
                    }
                    ///
                    if (requiredPlugin.isEqualNode(plugin)) {
                        requiredPluginExists = true;
                        break;
                    }
                }

                if (!requiredPluginExists) {
                    if (sameGroupIdArtifactIdExist) { // delete sameGroupIdArtifactId plugin
                        doc.getElementsByTagName("plugins").item(0).removeChild(sameGroupIdArtifactId);
                    }
                    doc.getElementsByTagName("plugins").item(0).appendChild(requiredPlugin);
                }

            }

            if (doc.getElementsByTagName("dependencies").getLength() == 0) {
                // add dependencies
                Element dependencies = doc.createElement("dependencies");
                doc.getElementsByTagName("project").item(0).appendChild(dependencies);
            }

            NodeList dependenciesNodeList = doc.getElementsByTagName("dependencies");
            if (doc.getElementsByTagName("dependency").getLength() == 0) {
                // add dependency
                Element dependency = getDependency(doc);
                doc.getElementsByTagName("dependencies").item(0).appendChild(dependency);
            } else {
                boolean requiredDependencyExists = false;
                boolean sameGroupIdArtifactIdExist = false;
                Element sameGroupIdArtifactId = null;
                Element requiredDependency = getDependency(doc);
                NodeList dependencyList = dependenciesNodeList.item(0).getChildNodes();
                for (int i = 0; i < dependencyList.getLength(); i++) {
                    Node dependency = dependencyList.item(i);
                    ///
                    NodeList dependencyData = dependency.getChildNodes();
                    String groupIdValue = null;
                    String artifactIdValue = null;
                    for (int j = 0; j < dependencyData.getLength(); j++) {
                        Node dependencyJ = dependencyData.item(j);
                        if (dependencyJ.getNodeName().equals("groupId")) {
                            groupIdValue = dependencyJ.getTextContent();
                        }
                        if (dependencyJ.getNodeName().equals("artifactId")) {
                            artifactIdValue = dependencyJ.getTextContent();
                        }
                    }
                    if (("org.apache.maven.plugins".equals(groupIdValue)) && ("maven-assembly-plugin".equals(artifactIdValue))) {
                        sameGroupIdArtifactIdExist = true;
                        sameGroupIdArtifactId = (Element) dependency;
                    }
                    ///
                    if (requiredDependency.isEqualNode(dependency)) {
                        requiredDependencyExists = true;
                        break;
                    }
                }

                if (!requiredDependencyExists) {
                    if (sameGroupIdArtifactIdExist) { // delete sameGroupIdArtifactId plugin
                        doc.getElementsByTagName("dependencies").item(0).removeChild(sameGroupIdArtifactId);
                    }
                    doc.getElementsByTagName("dependencies").item(0).appendChild(requiredDependency);
                }

            }
            readSuccessful = true;

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        if (readSuccessful) {
            try (FileOutputStream output = new FileOutputStream(f.getAbsolutePath())) {
                writeXml(doc, output);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException | TransformerException e) {
                throw new RuntimeException(e);
            }
        }

    }

    // write doc to output stream
    private static void writeXml(Document doc, OutputStream output) throws TransformerException, UnsupportedEncodingException {
        System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);

    }
}
