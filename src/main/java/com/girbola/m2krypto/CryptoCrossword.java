package com.girbola.m2krypto;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class CryptoCrossword extends Application {

  // Luodaan tietovarasto numeroille 1-30 (StringProperty mahdollistaa automaattisen päivityksen)
  private final Map<Integer, StringProperty> numberProperties = new HashMap<>();

  @Override
  public void start(Stage primaryStage) {
    // Alustetaan propertyt numeroille 1-30 tyhjiksi
    for (int i = 1; i <= 30; i++) {
      numberProperties.put(i, new SimpleStringProperty(""));
    }

    // Pääasetteluna BorderPane: vasemmalla kirjainpankki, keskellä ristikko
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));

    // 1. Luodaan kirjainpankki (1-30)
/*    VBox keyBank = new VBox();
    ScrollPane scrollPane = new ScrollPane(keyBank);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefWidth(220);
    root.setLeft(scrollPane);
*/
    // 2. Esimerkki ristikon datasta (0 = musta ruutu, muut = kryptonumeroita)
    List<List<Integer>> crosswordData = new ArrayList<>();

    // Keskialueelle ristikko (keskitettynä)
    StackPane centerPane = new StackPane();
    centerPane.setPadding(new Insets(10));

    VBox startPane = new VBox();
    TextArea textArea = new TextArea("0,0,0,0,0,0,0,0,1,2,3,3,4,0,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,2,0,20,0,9,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,22,24,17,17,12,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,24,0,17,0,13,0,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,8,25,18,8,3,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,0,14,0,4,0,17,17,5,8,9,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,0,17,19,2,23,12,9,0,2,0,11,0,1,0,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,0,0,2,0,2,0,5,4,24,11,2,15,2,0,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,0,25,23,17,9,22,8,0,4,0,18,14,0,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,0,11,0,1,0,14,8,8,9,3,17,14,9,0,0,0,0,0,0,0,0\n" +
      "0,0,0,0,0,0,0,0,0,15,25,1,19,8,0,14,0,25,0,4,0\n" +
      "0,0,0,0,0,0,0,0,0,2,0,2,0,20,17,9,3,11,4,6,17,0,0,0,0,0,0,0,0\n" +
      "0,9,0,1,0,18,4,9,9,2,3,2,0,0,8,0,7,0,6,0\n" +
      "12,13,3,4,11,10,0,3,21,0,9,4,21,23,2,2,11,4,3\n" +
      "0,4,0,12,0,18,14,8,8,23,4,3,0,2,4,0,4,0,25\n" +
      "16,24,3,13,11,10,0,23,0,17,0,17,25,14,8,7,4,4,9,25\n" +
      "0,12,0,11,0,0,15,17,9,9,17,0,0,2,0,2,0,2,0,19\n" +
      "5,17,14,4,23,3,10,0,16,0,21,17,14,1,2,23,8,3,3,2\n" +
      "25,0,8,0,13,0,14,17,23,23,16,0,17,0,19,0,23,0,13,0\n" +
      "12,17,9,13,3,16,10,0,3,0,5,0,3,14,17,17,23,2,3,2\n" +
      "12,0,7,0,3,0,3,20,4,3,3,17,14,0,11,4,4,7,4,3\n" +
      "4,14,8,23,4,22,0,2,0,2,4,23,8,2,0,11,0,4,0,17\n" +
      "0,2,0,8,0,24,25,11,11,25,0,9,0,9,5,17,23,22,17,14\n" +
      "8,11,4,7,4,2,0,17,11,4,4,3,25,0,17,0,4,0,4\n" +
      "0,6,0,20,3,2,9,12,25,0,23,0,3,25,23,3,4,15,2\n");
    Button startButton = new Button("Start");
    startButton.setOnAction(event -> {
      Platform.runLater(() -> {

        centerPane.getChildren().clear();
        //crosswordData
        crosswordData.clear();

        boolean isValid = isValidData(textArea.getText());

        if (!isValid) {
          System.err.println("Invalid data format:\n" + textArea.getText());
          return;
        }

        //List<List<Integer>> crosswordData = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
          new StringReader(textArea.getText())
        );

        String line;

        try {
          while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
              continue;
            }

            List<Integer> row = Arrays.stream(line.split(","))
              .map(String::trim)
              .map(Integer::parseInt)
              .toList();

            crosswordData.add(row);
          }

        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        System.out.println(crosswordData);
        GridPane gridPane2 = createCrosswordGrid(crosswordData);

        centerPane.getChildren().add(gridPane2);

        String input = textArea.getText();

        IntSummaryStatistics stats = Arrays.stream(input.split("[,\\s]+"))
          .filter(s -> !s.isBlank())
          .mapToInt(Integer::parseInt)
          .summaryStatistics();

        System.out.println("Minimum: " + stats.getMin());
        System.out.println("Maximum: " + stats.getMax());

        VBox keyBank = createKeyBankPane(stats.getMin(), stats.getMax());
        ScrollPane scrollPane = new ScrollPane(keyBank);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(220);
        root.setLeft(scrollPane);

      });
    });

    startPane.getChildren().addAll(textArea, startButton);

    centerPane.getChildren().add(startPane);

    root.setCenter(centerPane);

    // Ikkunan asetukset
    Scene scene = new Scene(root, 900, 600);
    primaryStage.setTitle("Kryptoristikko JavaFX");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static boolean isValidData(String input) {

    List<String> lines = input.lines()
      .map(String::trim)
      .filter(line -> !line.isEmpty())
      .toList();

    if (lines.isEmpty()) {
      return false;
    }

    int expectedLength = -1;

    for (String line : lines) {

      // Only integers separated by commas
      if (!line.matches("\\d+(\\s*,\\s*\\d+)*")) {
        return false;
      }

      String[] values = line.split(",");
      int length = values.length;

      // First line defines the required row length
      if (expectedLength == -1) {
        expectedLength = length;
      }

      // Every row must have the same length
      if (length != expectedLength) {
        return false;
      }

      // Values must be between 0 and 30
      for (String value : values) {
        try {
          int number = Integer.parseInt(value.trim());

          if (number < 0 || number > 30) {
            return false;
          }

        } catch (NumberFormatException e) {
          return false;
        }
      }
    }

    return true;
  }

  // Luodaan kirjainpankki paneeli (numerot 1-30 ja niiden tekstikentät)
  private VBox createKeyBankPane(int min, int max) {
    VBox box = new VBox(5);
    box.setPadding(new Insets(5));
    box.getChildren().add(new Label("Kirjainpankki (1-30):"));

    GridPane grid = new GridPane();
    grid.setHgap(5);
    grid.setVgap(5);

    for (int i = min; i <= max; i++) {
      Label lbl = new Label(i + " =");
      lbl.setMinWidth(30);

      TextField tf = new TextField();
      tf.setPrefWidth(40);
      tf.setAlignment(Pos.CENTER);

      // Sidotaan tekstikenttä kaksisuuntaisesti vastaavaan numeron propertyyn
      tf.textProperty().bindBidirectional(numberProperties.get(i));

      // Rajoitetaan syöte yhteen merkkiin ja muutetaan se automaattisesti isoksi
      tf.textProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal != null && !newVal.isEmpty()) {
          String upper = newVal.substring(newVal.length() - 1).toUpperCase();
          if (!upper.equals(newVal)) {
            tf.setText(upper);
          }
        }
      });

      int row = (i - 1) % 15;
      int col = (i - 1) / 15; // Jaetaan kahteen sarakkeeseen (1-15 ja 16-30)

      grid.add(lbl, col * 2, row);
      grid.add(tf, col * 2 + 1, row);
    }

    box.getChildren().add(grid);
    return box;
  }

  // Rakennetaan itse ristikko kaksiulotteisen listan perusteella
  private GridPane createCrosswordGrid(List<List<Integer>> data) {
    GridPane grid = new GridPane();
    grid.setHgap(2);
    grid.setVgap(2);
    grid.setAlignment(Pos.CENTER);

    for (int r = 0; r < data.size(); r++) {
      List<Integer> row = data.get(r);
      for (int c = 0; c < row.size(); c++) {
        int cellNum = row.get(c);

        if (cellNum == 0) {
          // Musta/tyhjä ruutu
          Pane blackCell = new Pane();
          blackCell.setPrefSize(45, 45);
          blackCell.setStyle("-fx-background-color: black;");
          grid.add(blackCell, c, r);
        } else {
          // Kryptoruutu, jossa numero nurkassa ja tekstikenttä keskellä
          VBox cryptoCell = createCryptoCell(cellNum);
          grid.add(cryptoCell, c, r);
        }
      }
    }
    return grid;
  }

  // Yksittäisen kryptoruudun luonti
  private VBox createCryptoCell(int number) {
    VBox cellBox = new VBox();
    cellBox.setPrefSize(45, 45);
    cellBox.setAlignment(Pos.CENTER);
    cellBox.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 1;");

    // Pieni numero ruudun yläreunassa
    Label numLabel = new Label(String.valueOf(number));
    numLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666;");

    // Kirjaimen syöttökenttä ruudussa
    TextField tf = new TextField();
    tf.setAlignment(Pos.CENTER);
    tf.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0;");

    // Sido samaan propertyyn kirjainpankin kanssa!
    // Muutos täällä päivittää pankin, muutos pankissa päivittää tämän ruudun.
    tf.textProperty().bindBidirectional(numberProperties.get(number));

    // Rajoitetaan syöte yhteen isoon kirjaimeen
    tf.textProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && !newVal.isEmpty()) {
        String upper = newVal.substring(newVal.length() - 1).toUpperCase();
        if (!upper.equals(newVal)) {
          tf.setText(upper);
        }
      }
    });

    cellBox.getChildren().addAll(numLabel, tf);
    return cellBox;
  }

}
