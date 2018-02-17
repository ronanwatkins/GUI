package application.commands;

import application.XMLUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;

public class CommandsTabController implements Initializable{

    private final String DIRECTORY = System.getProperty("user.dir") + "\\misc\\commands";

    @FXML
    private TextField commandField;

    @FXML
    private ListView<String> possibleCommandsListView;
    @FXML
    private ListView<String> commandsListView;
    @FXML
    private ListView<Integer> indexListView;
    @FXML
    private ListView<String> selectListView;
    @FXML
    private ListView<String> allCommandsListView;
    @FXML
    private ListView<String> runningCommandsListView;

    @FXML
    private Button addCommandButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button moveUpButton;
    @FXML
    private Button moveDownButton;
    @FXML
    private Button saveButton;

    @FXML
    private Button editButton;
    @FXML
    private Button deleteCommandsButton;
    @FXML
    private Button runCommandsButton;

    @FXML
    private Tab runBatchTab;
    @FXML
    private Tab createBatchTab;

    @FXML
    private TabPane tabPane;

    @FXML
    private ChoiceBox<Integer> indexBox;

    private HashMap<String, String> commandsMap;

    private int index = 0;

    private File editFile = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        RunBatchTab();

        createBatchTab.setOnSelectionChanged(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if(createBatchTab.isSelected()) {
                    System.out.println("Create Batch Tab Selected");

                    CreateBatchTab(editFile);
                }
            }
        });

        runBatchTab.setOnSelectionChanged(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if(runBatchTab.isSelected()) {
                    System.out.println("Run Batch Tab Selected");
                    RunBatchTab();
                }
            }
        });
    }

    private void CreateBatchTab(File file) {

        ObservableList<Integer> indexList;

        if(file != null) {
            saveButton.setDisable(false);
            XMLUtil xmlUtil = new XMLUtil();

            ObservableList<String> commands = xmlUtil.openBatchCommands(file);
            commandsListView.setItems(commands);

            indexList = FXCollections.observableArrayList();

            index = commands.size();

            for (int i = 0; i <= index; i++) {
                indexList.add(i);
            }

            indexBox.setItems(indexList);
            indexBox.setValue(index);

            indexListView.getItems().clear();
            for (int i = 0; i < indexBox.getValue(); i++) {
                indexListView.getItems().add(i);
            }

        } else {
            saveButton.setDisable(true);

            index = 0;
            indexList = FXCollections.observableArrayList(
                    index
            );

            indexListView.getItems().clear();
            commandsListView.getItems().clear();
        }

        String keyEvent = "shell input keyevent ";

        initCommandsMap();

        ObservableList<String> possibleCommands = FXCollections.observableArrayList(
                commandsMap.keySet()
        );

        deleteButton.setDisable(true);
        moveUpButton.setDisable(true);
        moveDownButton.setDisable(true);

        indexBox.setItems(indexList);
        indexBox.setValue(index);

        possibleCommandsListView.setItems(possibleCommands);

        possibleCommandsListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                commandField.setText(keyEvent + commandsMap.get(possibleCommandsListView.getSelectionModel().getSelectedItem()));
            }
        });

        indexBox.setItems(indexList);

        addCommandButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(!commandField.getText().isEmpty()) {
                    saveButton.setDisable(false);
                    commandsListView.getItems().add(indexBox.getValue(), commandField.getText());

                    index+=1;
                    indexList.add(index);
                    indexBox.setItems(indexList);
                    indexBox.setValue(index);

                    indexListView.getItems().clear();
                    for (int i = 0; i < indexBox.getValue(); i++) {
                        indexListView.getItems().add(i);
                    }
                }
            }
        });

        commandsListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    if (!commandsListView.getSelectionModel().getSelectedItem().isEmpty()) {
                        deleteButton.setDisable(false);
                        moveDownButton.setDisable(false);
                        moveUpButton.setDisable(false);
                    }
                } catch (NullPointerException npe) {
                    //npe.printStackTrace();
                }
            }
        });

        moveUpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int selectedItemIndex = commandsListView.getSelectionModel().getSelectedIndex();
                if(selectedItemIndex > 0) {
                    String selectedItemValue = commandsListView.getSelectionModel().getSelectedItem();
                    commandsListView.getItems().remove(selectedItemIndex);
                    commandsListView.getItems().add(selectedItemIndex - 1, selectedItemValue);

                    commandsListView.getSelectionModel().select(selectedItemIndex - 1);
                }
            }
        });

        moveDownButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int selectedItemIndex = commandsListView.getSelectionModel().getSelectedIndex();
                try {
                    if (!commandsListView.getItems().get(selectedItemIndex + 1).isEmpty()) {
                        String selectedItemValue = commandsListView.getSelectionModel().getSelectedItem();
                        commandsListView.getItems().remove(selectedItemIndex);
                        commandsListView.getItems().add(selectedItemIndex + 1, selectedItemValue);

                        commandsListView.getSelectionModel().select(selectedItemIndex + 1);
                    }
                } catch (IndexOutOfBoundsException e) {}
            }
        });

        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int commandsListViewIndex = commandsListView.getSelectionModel().getSelectedIndex();
                commandsListView.getItems().remove(commandsListViewIndex);

                indexList.remove(index);
                index -= 1;

                indexBox.setItems(indexList);
                indexBox.setValue(index);

                indexListView.getItems().clear();
                for (int i = 0; i < indexBox.getValue(); i++) {
                    indexListView.getItems().add(i);
                }
            }
        });

        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                XMLUtil xmlUtil = new XMLUtil();

                if(file != null) {
                    xmlUtil.saveBatchCommands(commandsListView.getItems(), file);
                    editFile = null;
                    tabPane.getSelectionModel().select(runBatchTab);
                } else {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Save Batch Commands");
                    dialog.setHeaderText("Enter Command Name");

                    Optional<String> result = dialog.showAndWait();
                    if (result.isPresent()) {
                        System.out.println(result.get());

                        xmlUtil.saveBatchCommands(commandsListView.getItems(), new File(DIRECTORY + "\\" + result.get() + ".xml"));
                        tabPane.getSelectionModel().select(runBatchTab);
                    }
                }

            }
        });


//        ScrollBar scrollBarOne;
//        ScrollBar scrollBarTwo;
//
//
//        // lookup after scene rendering....
//        indexListView.setStyle(".scroll-bar:vertical");
//        commandsListView.setStyle(".scroll-bar:vertical");
//        System.out.println(indexListView.lookup(".scroll-bar:vertical"));
//        System.out.println(commandsListView.lookup(".scroll-bar:vertical"));
//        scrollBarOne = (ScrollBar) indexListView.lookup(".scroll-bar:vertical");
//        scrollBarTwo = (ScrollBar) commandsListView.lookup(".scroll-bar:vertical");
//
//        scrollBarOne.valueProperty().bindBidirectional(scrollBarTwo.valueProperty());
    }

    private void RunBatchTab() {

        editFile = null;
        deleteCommandsButton.setDisable(true);
        editButton.setDisable(true);
        runCommandsButton.setDisable(true);


        
        ObservableList<String> commandFilesList = FXCollections.observableArrayList();

        File directory = new File(DIRECTORY);
        for(File file : directory.listFiles()) {
            commandFilesList.add(file.getName().replace(".xml", ""));
        }

        selectListView.setItems(commandFilesList);

        try {
            if (!selectListView.getSelectionModel().getSelectedItem().isEmpty())
                refreshCommandsList();
        } catch (Exception e) {}

        selectListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    if (!selectListView.getSelectionModel().getSelectedItem().isEmpty()) {
                        deleteCommandsButton.setDisable(false);
                        editButton.setDisable(false);
                        runCommandsButton.setDisable(false);

                        refreshCommandsList();
                    }
                } catch (NullPointerException npe) {}
            }
        });

        editButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String fileName = selectListView.getSelectionModel().getSelectedItem();
                editFile = new File(directory.getAbsolutePath() + "\\" + fileName + ".xml");
                CreateBatchTab(editFile);
                tabPane.getSelectionModel().select(createBatchTab);
            }
        });

        deleteCommandsButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String fileName = selectListView.getSelectionModel().getSelectedItem();
                int fileIndex = selectListView.getSelectionModel().getSelectedIndex();
                File fileToDelete = new File(directory.getAbsolutePath() + "\\" + fileName + ".xml");
                if(fileToDelete.delete()) {
                    System.out.println("File deleted");
                    commandFilesList.remove(fileIndex);

                    refreshCommandsList();
                }
            }
        });

    }

    private void refreshCommandsList() {
        XMLUtil xmlUtil = new XMLUtil();
        String commandName = selectListView.getSelectionModel().getSelectedItem();
        ObservableList<String> batchCommands = xmlUtil.openBatchCommands(new File(DIRECTORY + "\\" + commandName + ".xml"));

        allCommandsListView.setItems(batchCommands);
    }

    private void initCommandsMap() {
        commandsMap = new HashMap<>();
        commandsMap.put("Back", "KEYCODE_BACK");
        commandsMap.put("Home", "KEYCODE_HOME");
        commandsMap.put("List Open Apps", "KEYCODE_APP_SWITCH");
        commandsMap.put("Volume Up", "KEYCODE_VOLUME_UP");
        commandsMap.put("Volume Down", "KEYCODE_VOLUME_DOWN");
        commandsMap.put("Power", "KEYCODE_POWER");
        commandsMap.put("Tab", "KEYCODE_TAB");
        commandsMap.put("Enter", "KEYCODE_ENTER");
    }
}