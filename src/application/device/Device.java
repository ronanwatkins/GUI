package application.device;

import application.utilities.ADBUtil;
import application.utilities.Singleton;
import application.utilities.TelnetServer;
import application.applications.ApplicationTabController;
import application.automation.extras.GetTouchPositionController;
import application.monitor.MonitorTabController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import static application.utilities.ADBUtil.consoleCommand;

public class Device implements Singleton {
    private static final Logger Log = Logger.getLogger(Device.class.getName());

    private static Device instance = new Device();
    private String name = "";
    private int port;
    private boolean isEmulator;
    private AndroidApplication selectedApplication;
    private ObservableList<String> applicationNames;
    private double resolutionX;
    private double resolutionY;
    private double maxPositionX;
    private double maxPositionY;

    public static Device getInstance() {
        return instance;
    }

    private Device() {
        if(applicationNames == null)
            applicationNames = FXCollections.observableArrayList();
    }

    //Setters
    public void setApplicationNames(ObservableList<String> applicationNames) {
        this.applicationNames = applicationNames;
    }

    public void setSelectedApplication(AndroidApplication androidApplication) {
        this.selectedApplication = androidApplication;
    }

    public void setName(String name) {
        this.name = name;

        if(name.contains("emulator")) {
            isEmulator = true;
            try {
                port = Integer.parseInt(name.split("-")[1]);
            } catch (NumberFormatException nfe) {
                Log.error(nfe.getMessage());
            }
        } else {
            isEmulator = false;
        }
    }

    public void setResolution() {
        Log.info("");
        String[] response = consoleCommand("shell wm size").split(" ");
        String[] size = response[2].split("x");
        resolutionX = Double.parseDouble(size[0]);
        resolutionY = Double.parseDouble(size[1]);

        response = consoleCommand("shell \"getevent -il | grep ABS_MT_POSITION\"").split("\n");


        for(String res : response) {
            if(res.contains("ABS_MT_POSITION_X")) {
                String temp = res.split(",")[2];
                maxPositionX =  Double.parseDouble(temp.substring(temp.length()-5, temp.length()).trim());
            }
            if(res.contains("ABS_MT_POSITION_Y")) {
                String temp = res.split(",")[2];
                maxPositionY =  Double.parseDouble(temp.substring(temp.length()-5, temp.length()).trim());
            }
        }

        Log.info("resolution X: " + resolutionX);
        Log.info("resolution Y: " + resolutionY);

        Log.info("MaxX: " + maxPositionX);
        Log.info("MaxY: " + maxPositionY);
    }

    //Getters
    public String getName() {
        return name;
    }

    public AndroidApplication getSelectedApplication() {
        return selectedApplication;
    }

    public ObservableList<String> getApplicationNames() {
        return applicationNames;
    }

    public boolean isEmulator() {
        return isEmulator;
    }

    public double getResolutionX() {
        return resolutionX;
    }

    public double getResolutionY() {
        return resolutionY;
    }

    public double getMaxPositionX() {
        return maxPositionX;
    }

    public double getMaxPositionY() {
        return maxPositionY;
    }

    //Actions
    public int connectOverWifi() {
        consoleCommand("tcpip 5555");

        try {
            Thread.sleep(7000);
        } catch (InterruptedException ie) {
            Log.error(ie.getMessage());
        }

        String tempName = consoleCommand("shell ifconfig wlan0");
        tempName = tempName.split(" ")[2] + ":5555";

        String response = consoleCommand("connect " + tempName);

        if(response.startsWith("connected")) {
            this.name = tempName;
            return 0;
        }
        else if(response.startsWith("already")) {
            this.name = tempName;
            return 1;
        }
        else return 2;
    }

    public void handleNewConnection() {
        Log.info("handleNewConnection>> ");
        Log.info("isEmulator " + isEmulator);
        if(isEmulator) {
            try {
                TelnetServer.connect(port);
            } catch (NumberFormatException nfe) {
                Log.error(nfe.getMessage());
            }
        }

        Log.info("Starting monitor service for " + name);
        Platform.runLater(() -> {
            ApplicationTabController.getApplicationTabController().updateDeviceListView();
            MonitorTabController.getController().play();
            MonitorTabController.getController().updateDeviceListView();
        });

        Log.info("Getting screen resolution");
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                setResolution();
                return null;
            }
        };
        new Thread(task).start();
    }

    private static double xStart = 0.0;
    private static double yStart = 0.0;
    private static double xEnd = 0.0;
    private static double yEnd = 0.0;
    private AtomicBoolean swipeFlag = new AtomicBoolean(false);

    public void setSwipeFlag(Boolean flag) {
        swipeFlag.set(flag);

        if(flag) {
            xStart = 0.0;
            yStart = 0.0;
        }
    }

    private AtomicBoolean stopFlag = new AtomicBoolean(false);

    public void stopGettingCursorPosition() {
        stopFlag.set(true);
    }

    private GetTouchPositionController getTouchPositionController;

    public void getCursorPosition(GetTouchPositionController controller) {
        this.getTouchPositionController = controller;

        try {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String lineGlobal;
                    Integer decimal;
                    Process process = Runtime.getRuntime().exec(ADBUtil.getAdbPath() + " -s " + name + " shell getevent -lt");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    long startTime = 0;
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if(stopFlag.get()) {
                            stopFlag.set(false);
                            return null;
                        }

                        if(line.contains("ABS_MT_POSITION")) {
                            lineGlobal = line;
                            String position = line.substring(line.trim().length()-8).trim();
                            decimal = 0;
                            try {
                                decimal = Integer.parseInt(position, 16);
                            } catch (NumberFormatException nfe) {
                                Log.error(nfe.getMessage(), nfe);
                            }

                            try {
                                if(lineGlobal.contains("ABS_MT_POSITION_X")) {
                                    double x = decimal.doubleValue()*(resolutionX/maxPositionX);




                                    if(swipeFlag.get()) {
                                        if (xStart == 0.0) {
                                            xStart = x;
                                            getTouchPositionController.setXField(xStart);
                                            startTime = System.currentTimeMillis();
                                        } else {
                                            xEnd = x;
                                            getTouchPositionController.setXEndField(xEnd);
                                        }

                                        if(System.currentTimeMillis() - startTime > 2000) {
                                            startTime = 0;
                                            xStart = 0.0;
                                            yStart = 0.0;
                                        }
                                    } else {
                                        getTouchPositionController.setXField(x);
                                    }
                                }
                                else if(lineGlobal.contains("ABS_MT_POSITION_Y")) {
                                    double y = decimal.doubleValue()*(resolutionY/maxPositionY);

                                    if(swipeFlag.get()) {
                                        if (yStart == 0.0) {
                                            yStart = y;
                                            getTouchPositionController.setYField(yStart);
                                        } else {
                                            yEnd = y;
                                            getTouchPositionController.setYEndField(yEnd);
                                        }
                                    } else {
                                        getTouchPositionController.setYField(y);
                                    }
                                }
                            } catch (Exception ee) {
                                Log.error(ee.getMessage(), ee);
                            }
                        }
                    }

                    bufferedReader.close();
                    process.waitFor();

                    return null;
                }
            };
            new Thread(task).start();

        } catch (Exception ee) {
            Log.error(ee.getMessage(), ee);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
