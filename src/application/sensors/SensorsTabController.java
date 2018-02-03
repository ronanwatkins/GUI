package application.sensors;

import application.TelnetServer;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class SensorsTabController implements Initializable {

    @FXML
    private Slider lightSlider;
    @FXML
    private Slider temperatureSlider;
    @FXML
    private Slider pressureSlider;
    @FXML
    private Slider proximitySlider;
    @FXML
    private Slider humiditySlider;

    @FXML
    private Label lightLabel;
    @FXML
    private Label temperatureLabel;
    @FXML
    private Label pressureLabel;
    @FXML
    private Label proximityLabel;
    @FXML
    private Label humidityLabel;

    @FXML
    private TextField magneticField1;
    @FXML
    private TextField magneticField2;
    @FXML
    private TextField magneticField3;

    @FXML
    private TextField accelerometerField1;
    @FXML
    private TextField accelerometerField2;
    @FXML
    private TextField accelerometerField3;


    private double magneticFieldVal1 = 0;
    private double magneticFieldVal2 = 0;
    private double magneticFieldVal3 = 0;

    private double accelerometerVal1 = 0;
    private double accelerometerVal2 = 0;
    private double accelerometerVal3 = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        lightSlider.valueProperty().addListener(
                (observable, oldvalue, newvalue) ->
                {
                    double lightValue = newvalue.doubleValue();
                    lightLabel.setText(String.format("%.2f",lightValue)+"");
                    TelnetServer.setSensor("light " + String.format("%.2f",lightValue));
                } );

        temperatureSlider.valueProperty().addListener(
                (observable, oldvalue, newvalue) ->
                {
                    double temperatureValue = newvalue.doubleValue();
                    temperatureLabel.setText(String.format("%.2f",temperatureValue)+"");
                    TelnetServer.setSensor("temperature " + String.format("%.2f",temperatureValue));
                } );

        pressureSlider.valueProperty().addListener(
                (observable, oldvalue, newvalue) ->
                {
                    double pressureValue = newvalue.doubleValue();
                    pressureLabel.setText(String.format("%.2f",pressureValue)+"");
                    TelnetServer.setSensor("pressure " + String.format("%.2f",pressureValue));
                } );

        proximitySlider.valueProperty().addListener(
                (observable, oldvalue, newvalue) ->
                {
                    double proximityValue = newvalue.doubleValue();
                    proximityLabel.setText(String.format("%.2f",proximityValue)+"");
                    TelnetServer.setSensor("proximity " + String.format("%.2f",proximityValue));
                } );

        humiditySlider.valueProperty().addListener(
                (observable, oldvalue, newvalue) ->
                {
                    double hummityValue = newvalue.doubleValue();
                    humidityLabel.setText(String.format("%.2f",hummityValue)+"");
                    TelnetServer.setSensor("humidity " + String.format("%.2f",hummityValue));
                } );

        magneticField1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                magneticFieldVal1 = Double.parseDouble(magneticField1.getText());
                TelnetServer.setSensor("magnetic-field " + magneticFieldVal1 + ":" + magneticFieldVal2 + ":" + magneticFieldVal3);
            }
        });

        magneticField2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                magneticFieldVal2 = Double.parseDouble(magneticField2.getText());
                TelnetServer.setSensor("magnetic-field " + magneticFieldVal1 + ":" + magneticFieldVal2 + ":" + magneticFieldVal3);
            }
        });

        magneticField3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                magneticFieldVal3 = Double.parseDouble(magneticField3.getText());
                TelnetServer.setSensor("magnetic-field " + magneticFieldVal1 + ":" + magneticFieldVal2 + ":" + magneticFieldVal3);
            }
        });

        accelerometerField1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                accelerometerVal1 = Double.parseDouble(accelerometerField1.getText());
                TelnetServer.setSensor("acceleration " + accelerometerVal1 + ":" + accelerometerVal2 + ":" + accelerometerVal3);
            }
        });

        accelerometerField2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                accelerometerVal2 = Double.parseDouble(accelerometerField2.getText());
                TelnetServer.setSensor("acceleration " + accelerometerVal1 + ":" + accelerometerVal2 + ":" + accelerometerVal3);
            }
        });

        accelerometerField3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                accelerometerVal3 = Double.parseDouble(accelerometerField3.getText());
                TelnetServer.setSensor("acceleration " + accelerometerVal1 + ":" + accelerometerVal2 + ":" + accelerometerVal3);
            }
        });
    }
}
