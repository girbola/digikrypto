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
    TextArea textArea = new TextArea();
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
