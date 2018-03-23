package application.commands;

import application.ADBUtil;
import application.Main;
import application.XMLUtil;
import application.commands.CommandsTabController;
import application.commands.CreateBatchTabController;
import application.utilities.Utilities;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunBatchTabController implements Initializable {

    @FXML
    private ListView<String> selectListView;
    @FXML
    private ListView<String> allCommandsListView;
    @FXML
    private ListView<String> runningCommandsListView;

    @FXML
    private Button editButton;
    @FXML
    private Button deleteCommandsButton;
    @FXML
    private Button runCommandsButton;
    @FXML
    public Button stopCommandsButton;

    @FXML
    private ComboBox<String> runTypeComboBox;

    @FXML
    private CheckBox stopOnFailureCheckBox;

    @FXML
    private Tab createBatchTab;

    @FXML
    private TabPane tabPane;

    @FXML
    private CreateBatchTabController createBatchTabController;

    private Task<Void> runCommandsTask;
    private Thread runCommandsThread;

    private ObservableList<String> commandFilesList;

    private File editFile = null;
    private File directory = null;

    private AtomicBoolean pauseFlag = new AtomicBoolean(false);
    private AtomicBoolean wasPaused = new AtomicBoolean(false);

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        initializeButtons();

        editFile = null;
        runTypeComboBox.getSelectionModel().select(0);
        stopCommandsButton.setDisable(true);
        deleteCommandsButton.setDisable(true);
        editButton.setDisable(true);
        runCommandsButton.setDisable(true);

        commandFilesList = FXCollections.observableArrayList();

        directory = new File(CommandsTabController.DIRECTORY);

        try {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                commandFilesList.add(file.getName().replace(".xml", ""));
            }
        } catch (NullPointerException npe) {
            //npe.printStackTrace();
        }

        selectListView.setItems(commandFilesList);

        try {
            if (!selectListView.getSelectionModel().getSelectedItem().isEmpty())
                refreshCommandsList();
        } catch (Exception e) {}

        selectListView.setOnMouseClicked(event -> {
            try {
                if (!selectListView.getSelectionModel().getSelectedItem().isEmpty()) {
                    deleteCommandsButton.setDisable(false);
                    editButton.setDisable(false);
                    runCommandsButton.setDisable(false);
                    stopCommandsButton.fire();

                    refreshCommandsList();
                }
            } catch (NullPointerException npe) {}
        });
    }

    @FXML
    private void handleDeleteCommandsButtonClicked(ActionEvent event) {
        String fileName = selectListView.getSelectionModel().getSelectedItem();
        int fileIndex = selectListView.getSelectionModel().getSelectedIndex();
        File fileToDelete = new File(directory.getAbsolutePath() + "\\" + fileName + ".xml");
        if(fileToDelete.delete()) {
            System.out.println("File deleted");
            commandFilesList.remove(fileIndex);

            refreshCommandsList();
        }

    }

    @FXML
    private void handleRunCommandsButtonClicked(ActionEvent event) {
        if(runCommandsTask != null) {
            if(runCommandsTask.isRunning()) {
                if (!pauseFlag.get()) {
                    synchronized (pauseFlag) {
                        pauseFlag.set(true);
                    }

                    Utilities.setImage("/resources/play.png", runCommandsButton);
                } else {
                    synchronized (pauseFlag) {
                        pauseFlag.set(false);
                        pauseFlag.notify();
                    }

                    Utilities.setImage("/resources/pause.png", runCommandsButton);
                    wasPaused.set(true);
                }
            }
        }

        if(!wasPaused.get() && !pauseFlag.get()) {
            Utilities.setImage("/resources/pause.png", runCommandsButton);
            System.out.println("Starting new batch commands");
            runningCommandsListView.getItems().clear();
            stopCommandsButton.setDisable(false);

            ObservableList<String> commandsList = allCommandsListView.getItems();

            int startIndex;
            if(runTypeComboBox.getSelectionModel().isSelected(1) || runTypeComboBox.getSelectionModel().isSelected(2))
                startIndex = allCommandsListView.getSelectionModel().getSelectedIndex();
            else
                startIndex = 0;

            int endIndex;
            if(runTypeComboBox.getSelectionModel().isSelected(2))
                endIndex = startIndex+1;
            else
                endIndex = allCommandsListView.getItems().size();

            runCommandsTask = new Task<Void>() {
                @Override
                protected Void call() {
                    int index = startIndex;
                    for (String command : commandsList.subList(startIndex, endIndex)) {

                        if (pauseFlag.get()) {
                            synchronized (pauseFlag) {
                                while (pauseFlag.get()) {
                                    try {
                                        System.out.println("WAITING.....");
                                        pauseFlag.wait();
                                    } catch (InterruptedException e) {
                                        System.out.println("waiting.....");
                                        Thread.currentThread().interrupt();
                                        return null;
                                    }
                                }
                            }
                        }

                        String result = ADBUtil.consoleCommand(formatCommand(command).split(" "));
                        if(stopOnFailureCheckBox.isSelected() && (isError(result)))
                            Platform.runLater(() -> stopCommandsButton.fire());

                        if (result.isEmpty())
                            result = "Command completed successfully";

                        final String newResult = result;
                        final int newIndex = index++;

                        Platform.runLater(() -> {
                            allCommandsListView.getSelectionModel().select(newIndex);

                            if (allCommandsListView.getSelectionModel().getSelectedIndex() > 10) {
                                allCommandsListView.scrollTo(allCommandsListView.getSelectionModel().getSelectedIndex());
                            }

                            runningCommandsListView.getItems().add(newResult);
                            int selectedIndex = allCommandsListView.getSelectionModel().getSelectedIndex() - startIndex;
                            runningCommandsListView.getSelectionModel().select(selectedIndex);
                            runningCommandsListView.scrollTo(selectedIndex);
                        });
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ie) {
                            return null;
                        }
                    }
                    return null;
                }
            };

            runCommandsTask.setOnSucceeded(event1 -> {
                Utilities.setImage("/resources/play.png", runCommandsButton);
                wasPaused.set(false);
                pauseFlag.set(false);
            });

            runCommandsTask.setOnFailed(event1 -> {
                Utilities.setImage("/resources/play.png", runCommandsButton);
                wasPaused.set(false);
                pauseFlag.set(false);
            });

            runCommandsThread = new Thread(runCommandsTask);
            runCommandsThread.start();
        }
    }

    @FXML
    private void handleStopCommandsButtonClicked(ActionEvent event) {
        stopCommandsButton.setDisable(true);
        runCommandsThread.interrupt();
        runCommandsTask.cancel();
        wasPaused.set(false);
        pauseFlag.set(false);
        Utilities.setImage("/resources/play.png", runCommandsButton);
    }

    @FXML
    private void handleEditButtonClicked(ActionEvent event) {
        String fileName = selectListView.getSelectionModel().getSelectedItem();
        editFile = new File(directory.getAbsolutePath() + "\\" + fileName + ".xml");
        //createBatchTab(editFile);
       // createBatchTabController.init(editFile);

        CreateBatchTabController.setEditFile(editFile);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/commands/CommandsTab.fxml"));

        ObservableList<Node> children = Main.getChildren();
        TabPane tabPane = (TabPane) children.get(0); //Main tab pane

        ObservableList<Tab> tabs = tabPane.getTabs();
        Tab thisTab = tabs.get(5); //Commands tab

        thisTab.

        TabPane myTapPane = thisTab.getTabPane();

        myTapPane.getSelectionModel().select(1);

       // ObservableList<Node> cunt = thisTab.

     //   ObservableList<Tab> nowHey = thisTab.getTabPane().getTabs();

     //   System.out.println(nowHey);
     //   nowHey.get(1).getTabPane().getSelectionModel().select(1);
        //System.out.println("0: " + cunt.get(0).);
       // System.out.println("cunt: " + cunt);
      //  TabPaneSkin tabPaneSkin = (TabPaneSkin) cunt.get(0);


    //    TabPane newTapPane = (TabPane) cunt.get(0);
    //    newTapPane.getSelectionModel().select(1);

//        for(Node node : children)
//            System.out.println("Children: " + node);
//
//        try {
//            //loader.load();
//
//            Parent root = loader.load();
//
//            Stage stage = new Stage();
//            stage.initModality(Modality.APPLICATION_MODAL);
//            stage.setTitle("Record Inputs");
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }

       // CommandsTabController commandsTabController = loader.getController();
       // System.out.println("commandsTabController: " + commandsTabController);
       // commandsTabController.tabPane.getSelectionModel().select(createBatchTab);

      //  commandsTabController.createBatchTab.getTabPane().getSelectionModel().select(1);

    }

    private void initializeButtons() {
        Utilities.setImage("/resources/play.png", runCommandsButton);
        Utilities.setImage("/resources/stop.png", stopCommandsButton);

        Utilities.setImage("/resources/edit.png", editButton);
        Utilities.setImage("/resources/delete.png", deleteCommandsButton);
    }

    private void refreshCommandsList() {
        XMLUtil xmlUtil = new XMLUtil();

        String commandName = selectListView.getSelectionModel().getSelectedItem();
        System.out.println("Command name: " + commandName);
        if(commandName != null) {
            ObservableList<String> batchCommands = xmlUtil.openBatchCommands(new File(CommandsTabController.DIRECTORY + "\\" + commandName + ".xml"));
            allCommandsListView.setItems(batchCommands);
        } else {
            allCommandsListView.setItems(null);
            editButton.setDisable(true);
            deleteCommandsButton.setDisable(true);
            runCommandsButton.setDisable(true);
        }
    }

    private String formatCommand(String command) {
        if(command.startsWith("shell input text")) {
            String tempCommand = command.substring(17);
            //System.out.println("TempCommand: " + tempCommand);

            StringBuilder temp2 = new StringBuilder();
            for(char ch : tempCommand.toCharArray()) {
                String temp = ch+"";
                if(!Character.isAlphabetic(ch) && ch != ' ')
                    temp = "\\" + temp;

                temp2.append(temp);
            }

            tempCommand = temp2.toString();
            //tempCommand = tempCommand.replaceAll("[^a-zA-Z0-9 ]", "\\[^a-zA-Z0-9]");
            tempCommand = tempCommand.replace(" ", "%s");

            //System.out.println("TempCommand now: " + tempCommand);
            tempCommand  = "\"" + tempCommand + "\"";

            command = "shell input text " + tempCommand;
        }

        //System.out.print("Command: " + command);
        return command;
    }

    private boolean isError(String input) {
        return (input.startsWith("Failure") || input.startsWith("Error") || input.startsWith("error") || input.startsWith("** No activities found"));
    }


}