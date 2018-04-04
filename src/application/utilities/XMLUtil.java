package application.utilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import application.location.KML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtil {
    private static Document document;
    private static Element rootElement;
    private AtomicBoolean isFileSaved = new AtomicBoolean(true);
    private boolean isKml = false;

    public XMLUtil() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            document = documentBuilder.newDocument();
            rootElement = document.createElement("global");
            document.appendChild(rootElement);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
    }

    public XMLUtil(boolean isKML) {
        this.isKml = isKML;

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            document = documentBuilder.newDocument();
            rootElement = document.createElement("kml");

            document.appendChild(rootElement);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
    }

    public void addElement(Map<String, Double> sensorValues) {
        Element stage = document.createElement("stage");
        rootElement.appendChild(stage);

        Element sensor;

        for(String key : sensorValues.keySet()) {
            sensor = document.createElement("sensor");
            sensor.setAttribute("type", key);

            Element value = document.createElement("value");
            value.appendChild(document.createTextNode(sensorValues.get(key)+""));
            sensor.appendChild(value);

            stage.appendChild(sensor);
        }
    }

    private String printFile(File file) {
        String content = "";
        try {
            List<String> fileContent = new ArrayList<>(Files.readAllLines(file.toPath()));
            //fileContent.set(0, fileContent.get(0).replace("</kml>mark>", "<Placemark>")); //Sometimes a tag would become malformed so I came up with this quick fix
            content = fileContent.get(0);
            Files.write(file.toPath(), fileContent, StandardCharsets.UTF_8);
        }catch (Exception ee) {
            ee.printStackTrace();
        } finally {
            return content;
        }
    }

    public void updateFile(File file, KML kml) {
        System.out.println("updateFile before >> " + printFile(file));
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);

            Element root = document.getDocumentElement();

            Node firstDocImportedNode = document.importNode(addKMLElement(kml), true);
            root.appendChild(firstDocImportedNode );

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);

            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (Exception ee) {
            ee.printStackTrace();
        } finally {
            isKml = false;
            System.out.println("updateFile after >> " + printFile(file));
            saveKMLFile(file);
        }
    }

    public Element addKMLElement(KML kml) {
        Element placemark = document.createElement("Placemark");
        rootElement.appendChild(placemark);

        Element name = document.createElement("name");
        name.appendChild(document.createTextNode(kml.getName()));
        placemark.appendChild(name);

        Element description = document.createElement("description");
        description.appendChild(document.createTextNode(kml.getDescription()));
        placemark.appendChild(description);

        Element point = document.createElement("Point");

        Element coordinates = document.createElement("coordinates");
        coordinates.appendChild(document.createTextNode(kml.getAllValues()));
        point.appendChild(coordinates);

        placemark.appendChild(point);

        return placemark;
    }

    public void updateFile(File file, ObservableList<KML> KMLCommands) {
        XMLUtil xmlUtil = new XMLUtil(true);

        for(KML kml : KMLCommands) {
            System.out.println("updateFile>> " + kml);
            xmlUtil.addKMLElement(kml);
        }

        isKml = false;
        saveKMLFile(file);
    }

    public void saveKMLFile(File file) {
        System.out.println("saveKMLFile before >> " + printFile(file));
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);

            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (TransformerException te) {
            te.printStackTrace();
        }
        System.out.println("saveKMLFile after >> " + printFile(file));
    }

    public void saveFile(File file) {
        isFileSaved.set(false);

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);

            StreamResult result = new StreamResult(file);

            Task task = new Task<Void>() {

                @Override
                public Void call() throws TransformerException {
                        transformer.transform(source, result);
                        return null;
                }
            };
            new Thread(task).start();

            task.setOnSucceeded(event -> {
                System.out.println("file saved");
                isFileSaved.set(true);
            });

            task.setOnFailed(event -> {
                System.out.println("file not saved");
                isFileSaved.set(false);
            });
        } catch (TransformerException te) {
            te.printStackTrace();
        }
    }

    public boolean isFileSavedYet() {
        return isFileSaved.get();
    }

    public HashMap<Integer, HashMap<String, Double>> loadXML(File file) {
        HashMap<Integer, HashMap<String, Double>> returnMap = new HashMap<>();

        Task task = new Task<Void>() {
            @Override
            public Void call() {
                try {
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(file);

                    document.getDocumentElement().normalize();

                    Element element;
                    String valueString;

                    NodeList nodeList = document.getElementsByTagName("stage");

                    HashMap<String, Double> sensorValues;

                    for (int i = 0; i < nodeList.getLength(); i++) { //looping through "stage"
                        Node node = nodeList.item(i);
                        sensorValues = new HashMap<>();
                        NodeList childList = node.getChildNodes();
                        for(int j=0; j<childList.getLength(); j++) { //looping through "sensor"
                            Node childNode = childList.item(j);

                            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                                element = (Element) childNode;

                                String type = element.getAttribute("type");
                                Double value = Double.parseDouble(element.getElementsByTagName("value").item(0).getTextContent());
                                valueString = String.format("%.2f", value);

                                sensorValues.put(type, Double.parseDouble(valueString));
                            }
                        }

                        if(i>0)
                            returnMap.put(i, sensorValues);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
        new Thread(task).run();

        return returnMap;
    }

    public void saveBatchCommands(ObservableList<String> batchCommands, File file) {
        Element command;

        for(String s : batchCommands) {
            command = document.createElement("command");
            command.appendChild(document.createTextNode(s));
            rootElement.appendChild(command);
        }

        saveFile(file);
    }

    public ObservableList<String> openBatchCommands(File file) {
        while (!isFileSaved.get()) {
            System.out.println("Waiting for file to be saved");
        }

        ObservableList<String> returnList = FXCollections.observableArrayList();

        Task task = new Task<Void>() {
            @Override
            public Void call() {
                try {
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(file);

                    document.getDocumentElement().normalize();

                    Element element;

                    NodeList nodeList = document.getElementsByTagName("command");

                    for (int i = 0; i < nodeList.getLength(); i++) { //looping through "command"
                        element = (Element) nodeList.item(i);
                        returnList.add(element.getTextContent());
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }

                return null;
            }
        };
        new Thread(task).run();

        return returnList;
    }

    public ObservableList<KML> openKMLCommands(File file) {
        ObservableList<KML> KMLList = FXCollections.observableArrayList();

        Task task = new Task<Void>() {
            @Override
            public Void call() {
                try {
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(file);

                    document.getDocumentElement().normalize();

                    Element element;

                    NodeList nodeList = document.getElementsByTagName("kml");

                    for (int i = 0; i < nodeList.getLength(); i++) { //looping through "Document"
                        Node node = nodeList.item(i);
                        NodeList childList = node.getChildNodes();
                        for(int j=0; j<childList.getLength(); j++) { //looping through "Placemark"
                            Node childNode = childList.item(j);
                            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                                element = (Element) childNode;

                                String name = (element.getElementsByTagName("name").item(0).getTextContent());
                                String description = (element.getElementsByTagName("description").item(0).getTextContent());

                                String[] values = (element.getElementsByTagName("Point").item(0).getTextContent()).split(",");
                                Double longitude = Double.parseDouble(values[0]);
                                Double latitude = Double.parseDouble(values[1]);
                                Double altitude = Double.parseDouble(values[2]);

                                KMLList.add(new KML(name, description, latitude, longitude, altitude));
                            }
                        }
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }

                return null;
            }
        };
        new Thread(task).run();

        return KMLList;
    }
}