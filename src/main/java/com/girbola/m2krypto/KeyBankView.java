package com.girbola.m2krypto;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Map;

public class KeyBankView {

  public GridPane createKeyBank(Map<Integer, StringProperty> numberProperties) {
    GridPane bankPane = new GridPane();
    bankPane.setHgap(10);
    bankPane.setVgap(5);
    bankPane.setPadding(new Insets(10));

    int col = 0;
    int row = 0;

    for (int i = 1; i <= 25; i++) {
      Label numLabel = new Label(i + " =");
      numLabel.setMinWidth(30);

      TextField letterField = new TextField();
      letterField.setPrefWidth(40);
      letterField.setAlignment(javafx.geometry.Pos.CENTER);

      // Sidotaan tekstikenttä kaksisuuntaisesti vastaavaan numeron propertyyn
      letterField.textProperty().bindBidirectional(numberProperties.get(i));

      // Rajoitetaan syöte yhteen merkkiin ja muutetaan kirjain automaattisesti isotiksi (yleinen käytäntö ristikossa)
      letterField.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && !newValue.isEmpty()) {
          String upper = newValue.substring(newValue.length() - 1).toUpperCase();
          if (!upper.equals(newValue)) {
            letterField.setText(upper);
          }
        }
      });

      bankPane.add(numLabel, col * 2, row);
      bankPane.add(letterField, col * 2 + 1, row);

      row++;
      if (row > 14) { // Jaetaan esim. kahteen sarakkeeseen (1-15 ja 16-30)
        row = 0;
        col++;
      }
    }

    return bankPane;
  }
}
