package com.girbola.m2krypto;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import nu.pattern.OpenCV;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CryptoCrossword extends Application {

  static {
    // Ladataan OpenCV-natiivikirjasto (org.openpnp.opencv)
    OpenCV.loadShared();
  }

  private static final double CELL_SIZE = 40.0;

  private final Map<Integer, StringProperty> numberProperties = new HashMap<>();

  private int maxNumberLimit = 30;

  private Spinner<Integer> rowsSpinner;
  private Spinner<Integer> colsSpinner;
  private GridPane visualInputGrid;
  private TextArea rawTextArea;

  private SimpleStringProperty loadedFile = new SimpleStringProperty("");

  // References for visual grid synchronization
  private TextField[][] currentVisualFields;

  private boolean isProgrammaticUpdate = false;

  @Override
  public void start(Stage primaryStage) {

    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));

    StackPane centerPane = new StackPane();
    centerPane.setPadding(new Insets(10));

    VBox startPane = new VBox(10);
    startPane.setAlignment(Pos.CENTER);

    rawTextArea = new TextArea();
    rawTextArea.setPrefRowCount(25);
    rawTextArea.setStyle("-fx-font-family: monospace;");

    // ---------------------------------------------------------
    // TOP FILE TOOLBAR
    // ---------------------------------------------------------

    ToolBar fileToolBar = createFileToolBar(primaryStage);
    root.setTop(fileToolBar);

    // ---------------------------------------------------------
    // INPUT TABS
    // ---------------------------------------------------------

    VBox visualGridBuilderPane = createVisualGridBuilderPane();
    VBox keypadBuilderPane = createNumericGridInputPane();

    TabPane inputTabPane = new TabPane();

    Tab visualTab = new Tab("Visual Matrix Builder", visualGridBuilderPane);

    Tab keypadTab = new Tab("Keypad Builder", keypadBuilderPane);

    Tab textTab = new Tab("Raw CSV Text", rawTextArea);

    visualTab.setClosable(false);
    keypadTab.setClosable(false);
    textTab.setClosable(false);

    inputTabPane.getTabs().addAll(visualTab, keypadTab, textTab);

    // ---------------------------------------------------------
    // GENERATE BUTTON
    // ---------------------------------------------------------

    Button startButton = new Button("Generate Crossword Grid");

    startButton.setStyle("-fx-font-size: 14px;" + "-fx-font-weight: bold;" + "-fx-base: #2196F3;");

    startButton.setDefaultButton(true);

    startPane.getChildren().addAll(inputTabPane, startButton);

    centerPane.getChildren().add(startPane);

    root.setCenter(centerPane);

    // ---------------------------------------------------------
    // GENERATE CROSSWORD
    // ---------------------------------------------------------

    startButton.setOnAction(event -> {

      String input = rawTextArea.getText();

      List<List<Integer>> crosswordData = parseAndValidateInput(input);
      if (crosswordData == null || crosswordData.isEmpty()) {
        showErrorAlert("Invalid Input", "Please provide a valid matrix with equal row lengths containing non-negative integers.");
        return;
      }

      // -----------------------------------------------------
      // FIND MIN/MAX NUMBER
      // -----------------------------------------------------
      IntSummaryStatistics stats = crosswordData.stream().flatMap(Collection::stream).filter(val -> val > 0).mapToInt(Integer::intValue).summaryStatistics();
      if (stats.getCount() == 0) {
        showErrorAlert("Invalid Data", "Grid contains no printable number cells.");
        return;
      }

      // -----------------------------------------------------
      // CREATE LETTER PROPERTIES
      // -----------------------------------------------------
      for (int i = stats.getMin(); i <= stats.getMax(); i++) {
        numberProperties.putIfAbsent(i, new SimpleStringProperty(""));
      }

      // -----------------------------------------------------
      // COUNT HOW MANY TIMES EACH NUMBER OCCURS
      // -----------------------------------------------------

      Map<Integer, Integer> numberCounts = countNumbers(crosswordData);

      // -----------------------------------------------------
      // CREATE CROSSWORD GRID
      // -----------------------------------------------------

      GridPane gridPane = createCrosswordGrid(crosswordData);

      // -----------------------------------------------------
      // CREATE KEY BANK WITH COUNTS
      // -----------------------------------------------------

      VBox keyBank = createKeyBankPane(stats.getMin(), stats.getMax(), numberCounts);

      // -----------------------------------------------------
      // SCROLL PANES
      // -----------------------------------------------------

      ScrollPane kryptoScrollPane = new ScrollPane(gridPane);
      kryptoScrollPane.setFitToWidth(true);
      kryptoScrollPane.setFitToHeight(true);

      ScrollPane scrollPane = new ScrollPane(keyBank);
      scrollPane.setFitToWidth(true);
      scrollPane.setPrefWidth(320);

      // -----------------------------------------------------
      // PUT KEY BANK ON LEFT
      // -----------------------------------------------------

      root.setLeft(scrollPane);

      // Put crossword in center
      centerPane.getChildren().setAll(kryptoScrollPane);
    });

    // ---------------------------------------------------------
    // SCENE
    // ---------------------------------------------------------

    Scene scene = new Scene(root, 1150, 850);
    scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/girbola/m2krypto/crypto-crossword.css")).toExternalForm());

    primaryStage.setTitle("Kryptoristikko JavaFX");
    primaryStage.setScene(scene);
    primaryStage.show();
    //Path kryptoristikkoPath = Paths.get("/Users/gerbiloi/Downloads/krypto-grey.JPG");
    //Path kryptoristikkoPath = Paths.get("/Users/gerbiloi/Downloads/krypto-iphone.JPG");
    // krypto-iphone.JPG
    //getMatrix(kryptoristikkoPath);

  }

  private void getMatrix(Path path) {
    KryptoristikkoExtractorService service = new KryptoristikkoExtractorService();

    try {
      // Option A: Call with a String file path
      List<String> matrix = service.extractMatrix(path);

      // Option B: Call with a Path object
      // Path photoPath = Paths.get("photos", "kryptoristikko.jpg");
      // List<String> matrix = service.extractMatrix(photoPath);

      // Print each row of the matrix
      System.out.println("\"matrix\": [");
      for (int i = 0; i < matrix.size(); i++) {
        String comma = (i < matrix.size() - 1) ? "," : "";
        System.out.println("  \"" + matrix.get(i) + "\"" + comma);
      }
      System.out.println("]");

    } catch (Exception e) {
      System.err.println("Failed to extract matrix from image: " + e.getMessage());
      e.printStackTrace();
    }
  }
  // =============================================================
  // COUNT NUMBERS
  // =============================================================

  private Map<Integer, Integer> countNumbers(List<List<Integer>> crosswordData) {

    Map<Integer, Integer> counts = new HashMap<>();

    for (List<Integer> row : crosswordData) {

      for (int number : row) {

        // Ignore black cells (0)
        if (number > 0) {

          counts.merge(number, 1, Integer::sum);
        }
      }
    }

    return counts;
  }

  // =============================================================
  // FILE TOOLBAR
  // =============================================================

  private ToolBar createFileToolBar(Stage stage) {

    Button saveBtn = new Button("💾 Save Progress (.krypto)");

    Button loadBtn = new Button("📂 Load Progress (.krypto)");
    Label loadLabel = new Label("Loads the last saved state.");
    loadLabel.textProperty().bind(loadedFile);

    saveBtn.setOnAction(e -> {
      saveStateToFile(stage);

      });
    loadBtn.setOnAction(e -> loadStateFromFile(stage));

    return new ToolBar(saveBtn, loadBtn, loadLabel);
  }

  // =============================================================
  // SAVE
  // =============================================================

  private void saveStateToFile(Stage stage) {

    FileChooser fileChooser = new FileChooser();
    if( loadedFile.getValue() != null) {
      String substring = loadedFile.getValue().substring(loadedFile.getValue().lastIndexOf("/") + 1);
      substring = substring.replace(".krypto", "");
      fileChooser.setInitialFileName(substring);
    }

    fileChooser.setTitle("Save Crypto Crossword State");

    fileChooser.getExtensionFilters().addAll(

      new FileChooser.ExtensionFilter("Crypto Crossword (*.krypto, *.json)", "*.krypto", "*.json"),

      new FileChooser.ExtensionFilter("All Files", "*.*"));

    File file = fileChooser.showSaveDialog(stage);

    if (file == null) {
      return;
    }

    try {

      int rows = rowsSpinner.getValue();

      int cols = colsSpinner.getValue();

      String matrixText = rawTextArea.getText().trim();

      StringBuilder json = new StringBuilder();

      json.append("{\n");

      json.append("  \"rows\": ").append(rows).append(",\n");

      json.append("  \"cols\": ").append(cols).append(",\n");

      json.append("  \"maxLimit\": ").append(maxNumberLimit).append(",\n");

      json.append("  \"matrix\": [\n");

      String[] lines = matrixText.split("\\r?\\n");

      for (int i = 0; i < lines.length; i++) {
        json.append("    \"").append(lines[i].trim()).append("\"");
        if (i < lines.length - 1) {
          json.append(",");
        }
        json.append("\n");
      }

      json.append("  ],\n");
      json.append("  \"letters\": {\n");

      List<String> mappingEntries = new ArrayList<>();
      for (Map.Entry<Integer, StringProperty> entry : numberProperties.entrySet()) {
        String val = entry.getValue().get();
        if (val != null && !val.isEmpty()) {
          mappingEntries.add("    \"" + entry.getKey() + "\": \"" + val + "\"");
        }
      }
      json.append(String.join(",\n", mappingEntries));
      json.append("\n  }\n");
      json.append("}");
      Files.writeString(file.toPath(), json.toString());
      showInfoAlert("Saved Successfully", "State saved to:\n" + file.getAbsolutePath());
    } catch (IOException ex) {
      showErrorAlert("File Save Error", "Could not save file: " + ex.getMessage());
    }
  }

  // =============================================================
  // LOAD
  // =============================================================

  private void loadStateFromFile(Stage stage) {

    FileChooser fileChooser = new FileChooser();

    fileChooser.setTitle("Load Crypto Crossword State");

    fileChooser.getExtensionFilters().addAll(

      new FileChooser.ExtensionFilter("Crypto Crossword (*.krypto, *.json)", "*.krypto", "*.json"),

      new FileChooser.ExtensionFilter("All Files", "*.*"));

    File file = fileChooser.showOpenDialog(stage);

    if (file == null) {
      return;
    }
    loadedFile.set(file.toString());
    try {

      String content = Files.readString(file.toPath());

      int rows = parseJsonInt(content, "rows", 10);
      int cols = parseJsonInt(content, "cols", 10);
      int limit = parseJsonInt(content, "maxLimit", 30);

      this.maxNumberLimit = limit;
      this.rowsSpinner.getValueFactory().setValue(rows);
      this.colsSpinner.getValueFactory().setValue(cols);

      List<String> matrixLines = parseJsonArray(content, "matrix");
      String rawMatrix = String.join("\n", matrixLines);
      isProgrammaticUpdate = true;
      try {
        rawTextArea.setText(rawMatrix);
        rebuildInteractiveTextFieldGrid(visualInputGrid, rows, cols, rawTextArea);
        populateVisualGridFromText(matrixLines, rows, cols);
      } finally {
        isProgrammaticUpdate = false;
      }

      // Restore letters

      Map<String, String> letters = parseJsonObjectMap(content, "letters");

      for (Map.Entry<String, String> entry : letters.entrySet()) {
        try {
          int numKey = Integer.parseInt(entry.getKey());
          numberProperties.putIfAbsent(numKey, new SimpleStringProperty(""));
          numberProperties.get(numKey).set(entry.getValue());
        } catch (NumberFormatException ignored) {
        }
      }

      showInfoAlert("Loaded Successfully", "Crossword state loaded from:\n" + file.getAbsolutePath());

    } catch (Exception ex) {

      showErrorAlert("File Load Error", "Failed to parse crossword file: " + ex.getMessage());
    }
  }

  // =============================================================
  // JSON INTEGER
  // =============================================================

  private int parseJsonInt(String json, String key, int defaultVal) {

    String search = "\"" + key + "\":";

    int idx = json.indexOf(search);

    if (idx == -1) {
      return defaultVal;
    }

    int start = idx + search.length();
    int end = json.indexOf(",", start);
    if (end == -1) {
      end = json.indexOf("\n", start);
    }
    try {
      return Integer.parseInt(json.substring(start, end).trim());
    } catch (Exception e) {
      return defaultVal;
    }
  }

  // =============================================================
  // JSON ARRAY
  // =============================================================

  private List<String> parseJsonArray(String json, String key) {

    List<String> result = new ArrayList<>();
    String search = "\"" + key + "\": [";
    int idx = json.indexOf(search);
    if (idx == -1) {
      return result;
    }

    int start = idx + search.length();
    int end = json.indexOf("]", start);
    if (end == -1) {
      return result;
    }

    String arrayContent = json.substring(start, end);
    String[] lines = arrayContent.split("\\r?\\n");
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.endsWith(",")) {
        trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
      }

      if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
        trimmed = trimmed.substring(1, trimmed.length() - 1);
      }

      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }

    return result;
  }

  // =============================================================
  // JSON LETTER MAP
  // =============================================================

  private Map<String, String> parseJsonObjectMap(String json, String key) {

    Map<String, String> map = new HashMap<>();
    String search = "\"" + key + "\": {";
    int idx = json.indexOf(search);
    if (idx == -1) {
      return map;
    }

    int start = idx + search.length();
    int end = json.indexOf("}", start);
    if (end == -1) {
      return map;
    }

    String body = json.substring(start, end);
    String[] lines = body.split(",");
    for (String line : lines) {
      String[] kv = line.split(":");
      if (kv.length == 2) {
        String k = kv[0].trim().replace("\"", "");
        String v = kv[1].trim().replace("\"", "");
        if (!k.isEmpty()) {
          map.put(k, v);
        }
      }
    }

    return map;
  }

  // =============================================================
  // VISUAL GRID BUILDER
  // =============================================================

  private VBox createVisualGridBuilderPane() {

    VBox container = new VBox(10);
    container.setPadding(new Insets(10));
    container.setAlignment(Pos.CENTER);

    HBox configBox = new HBox(15);
    configBox.setAlignment(Pos.CENTER);

    Label rowsLabel = new Label("Rows:");

    rowsSpinner = new Spinner<>(1, 50, 23);
    rowsSpinner.setEditable(true);
    rowsSpinner.setPrefWidth(75);

    Label colsLabel = new Label("Cells per Line (Cols):");

    colsSpinner = new Spinner<>(1, 50, 17);
    colsSpinner.setEditable(true);
    colsSpinner.setPrefWidth(75);

    Button generateGridBtn = new Button("Create Visual Grid");
    generateGridBtn.setStyle("-fx-font-weight: bold;");

    configBox.getChildren().addAll(rowsLabel, rowsSpinner, colsLabel, colsSpinner, generateGridBtn);

    ScrollPane gridScrollPane = new ScrollPane();
    gridScrollPane.setFitToWidth(true);
    gridScrollPane.setFitToHeight(true);
    gridScrollPane.setPrefHeight(320);

    visualInputGrid = new GridPane();
    visualInputGrid.setHgap(4);
    visualInputGrid.setVgap(4);
    visualInputGrid.setAlignment(Pos.CENTER);
    visualInputGrid.setPadding(new Insets(10));

    gridScrollPane.setContent(visualInputGrid);

    generateGridBtn.setOnAction(e -> {
      int rows = rowsSpinner.getValue();
      int cols = colsSpinner.getValue();
      rebuildInteractiveTextFieldGrid(visualInputGrid, rows, cols, rawTextArea);
    });

    rebuildInteractiveTextFieldGrid(visualInputGrid, rowsSpinner.getValue(), colsSpinner.getValue(), rawTextArea);

    container.getChildren().addAll(configBox, gridScrollPane);

    return container;
  }

  // =============================================================
  // REBUILD VISUAL INPUT GRID
  // =============================================================

  private void rebuildInteractiveTextFieldGrid(GridPane grid, int rows, int cols, TextArea textArea) {

    grid.getChildren().clear();

    currentVisualFields = new TextField[rows][cols];

    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        final int curRow = r;
        final int curCol = c;

        TextField tf = new TextField("0");
        tf.setPrefSize(40, 35);
        tf.setAlignment(Pos.CENTER);
        tf.setStyle("-fx-font-weight: bold;");
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
          if (isProgrammaticUpdate || newVal == null) {
            return;
          }
          String sanitized = newVal.replaceAll("[^0-9]", "");
          if (!sanitized.equals(newVal)) {
            tf.setText(sanitized);
          } else {
            syncVisualGridToTextArea(currentVisualFields, rows, cols, textArea);
          }
        });

        tf.setOnKeyPressed(event -> {
          if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.ENTER) {
            moveFocus(currentVisualFields, curRow, curCol + 1, rows, cols);
          } else if (event.getCode() == KeyCode.LEFT) {
            moveFocus(currentVisualFields, curRow, curCol - 1, rows, cols);
          } else if (event.getCode() == KeyCode.DOWN) {
            moveFocus(currentVisualFields, curRow + 1, curCol, rows, cols);
          } else if (event.getCode() == KeyCode.UP) {
            moveFocus(currentVisualFields, curRow - 1, curCol, rows, cols);
          } else if (event.getCode() == KeyCode.BACK_SPACE && tf.getText().isEmpty()) {
            moveFocus(currentVisualFields, curRow, curCol - 1, rows, cols);
          }
        });

        currentVisualFields[r][c] = tf;

        grid.add(tf, c, r);
      }
    }

    if (!isProgrammaticUpdate) {
      syncVisualGridToTextArea(currentVisualFields, rows, cols, textArea);
    }
  }

  // =============================================================
  // POPULATE VISUAL GRID
  // =============================================================

  private void populateVisualGridFromText(List<String> matrixLines, int rows, int cols) {

    if (currentVisualFields == null) {
      return;
    }

    for (int r = 0; r < Math.min(rows, matrixLines.size()); r++) {

      String[] tokens = matrixLines.get(r).split(",");

      for (int c = 0; c < Math.min(cols, tokens.length); c++) {

        if (currentVisualFields[r][c] != null) {

          String val = tokens[c].trim();

          currentVisualFields[r][c].setText(val.isEmpty() ? "0" : val);
        }
      }
    }
  }

  // =============================================================
  // MOVE FOCUS
  // =============================================================

  private void moveFocus(TextField[][] fields, int targetRow, int targetCol, int totalRows, int totalCols) {

    if (targetCol >= totalCols) {

      targetCol = 0;
      targetRow++;

    } else if (targetCol < 0) {

      targetCol = totalCols - 1;

      targetRow--;
    }

    if (targetRow >= 0 && targetRow < totalRows && targetCol >= 0 && targetCol < totalCols) {

      fields[targetRow][targetCol].requestFocus();

      fields[targetRow][targetCol].selectAll();
    }
  }

  // =============================================================
  // SYNC VISUAL GRID -> TEXT
  // =============================================================

  private void syncVisualGridToTextArea(TextField[][] fields, int rows, int cols, TextArea textArea) {

    StringBuilder sb = new StringBuilder();

    for (int r = 0; r < rows; r++) {

      for (int c = 0; c < cols; c++) {

        String val = fields[r][c] != null ? fields[r][c].getText().trim() : "0";

        sb.append(val.isEmpty() ? "0" : val);

        if (c < cols - 1) {
          sb.append(",");
        }
      }

      if (r < rows - 1) {
        sb.append("\n");
      }
    }

    textArea.setText(sb.toString());
  }

  // =============================================================
  // KEYPAD
  // =============================================================

  private VBox createNumericGridInputPane() {

    VBox container = new VBox(10);

    container.setPadding(new Insets(10));

    container.setAlignment(Pos.CENTER);

    HBox maxLimitConfigBox = new HBox(10);

    maxLimitConfigBox.setAlignment(Pos.CENTER);

    Label limitLabel = new Label("Max Key Numbers / Letters:");

    limitLabel.setStyle("-fx-font-weight: bold;");

    Spinner<Integer> maxSpinner = new Spinner<>(1, 200, maxNumberLimit);

    maxSpinner.setEditable(true);
    maxSpinner.setPrefWidth(80);

    Button applyLimitBtn = new Button("Update Keypad");

    maxLimitConfigBox.getChildren().addAll(limitLabel, maxSpinner, applyLimitBtn);

    GridPane keypad = new GridPane();

    keypad.setHgap(4);
    keypad.setVgap(4);
    keypad.setAlignment(Pos.CENTER);

    rebuildKeypad(keypad, maxSpinner.getValue(), rawTextArea);

    applyLimitBtn.setOnAction(e -> {

      maxNumberLimit = maxSpinner.getValue();

      rebuildKeypad(keypad, maxNumberLimit, rawTextArea);
    });

    HBox toolbar = new HBox(10);

    toolbar.setAlignment(Pos.CENTER);

    Button newRowBtn = new Button("↵ New Row");

    Button removeLastBtn = new Button("⌫ Remove Last");

    Button clearAllBtn = new Button("Clear All");

    newRowBtn.setOnAction(e -> {

      if (!rawTextArea.getText().endsWith("\n") && !rawTextArea.getText().isEmpty()) {

        rawTextArea.appendText("\n");
      }
    });

    removeLastBtn.setOnAction(e -> removeLastNumberFromInput(rawTextArea));

    clearAllBtn.setOnAction(e -> rawTextArea.clear());

    toolbar.getChildren().addAll(newRowBtn, removeLastBtn, clearAllBtn);

    container.getChildren().addAll(maxLimitConfigBox, keypad, toolbar);

    return container;
  }

  // =============================================================
  // KEYPAD BUTTONS
  // =============================================================

  private void rebuildKeypad(GridPane keypad, int maxLimit, TextArea textArea) {

    keypad.getChildren().clear();

    int cols = 10;

    for (int i = 0; i <= maxLimit; i++) {

      final int num = i;

      Button btn = new Button(String.valueOf(num));

      btn.setPrefWidth(45);
      btn.setPrefHeight(35);

      if (num == 0) {

        btn.setStyle("-fx-background-color: #333;" + "-fx-text-fill: white;" + "-fx-font-weight: bold;");
      }

      btn.setOnAction(e -> appendNumberToCurrentRow(textArea, num));

      int row = i / cols;
      int col = i % cols;

      keypad.add(btn, col, row);
    }
  }

  // =============================================================
  // APPEND NUMBER
  // =============================================================

  private void appendNumberToCurrentRow(TextArea textArea, int num) {

    String content = textArea.getText();

    if (content.isEmpty() || content.endsWith("\n")) {

      textArea.appendText(String.valueOf(num));

    } else {

      textArea.appendText("," + num);
    }
  }

  // =============================================================
  // REMOVE LAST NUMBER
  // =============================================================

  private void removeLastNumberFromInput(TextArea textArea) {

    String content = textArea.getText();

    if (content.isEmpty()) {
      return;
    }

    int lastComma = content.lastIndexOf(",");

    int lastNewline = content.lastIndexOf("\n");

    if (lastComma > lastNewline) {

      textArea.setText(content.substring(0, lastComma));

    } else if (lastNewline != -1) {

      textArea.setText(content.substring(0, lastNewline));

    } else {

      textArea.clear();
    }
  }

  // =============================================================
  // PARSE INPUT
  // =============================================================

  private List<List<Integer>> parseAndValidateInput(String input) {

    if (input == null || input.isBlank()) {
      return null;
    }

    List<List<Integer>> grid = new ArrayList<>();

    String[] lines = input.split("\\r?\\n");

    int expectedColumns = -1;

    for (String line : lines) {

      String trimmed = line.trim();

      if (trimmed.isEmpty()) {
        continue;
      }

      String[] tokens = trimmed.split(",");

      if (expectedColumns == -1) {

        expectedColumns = tokens.length;

      } else if (tokens.length != expectedColumns) {

        return null;
      }

      List<Integer> row = new ArrayList<>(tokens.length);

      for (String token : tokens) {

        try {

          int val = Integer.parseInt(token.trim());

          if (val < 0) {
            return null;
          }

          row.add(val);

        } catch (NumberFormatException e) {

          return null;
        }
      }

      grid.add(row);
    }

    return grid;
  }

  // =============================================================
  // KEY BANK
  // =============================================================
  private VBox createKeyBankPane(int min, int max, Map<Integer, Integer> numberCounts) {

    VBox box = new VBox(10);

    box.setPadding(new Insets(15));

    box.getStyleClass().add("key-bank");

    Label title = new Label("🔐 Cipher Key");

    title.getStyleClass().add("key-bank-title");

    Label subtitle = new Label("Number  →  Letter  →  Frequency");

    subtitle.getStyleClass().add("subtle-label");

    box.getChildren().addAll(title, subtitle);

    GridPane grid = new GridPane();

    grid.setHgap(8);
    grid.setVgap(6);

    int index = 0;

    for (int i = min; i <= max; i++) {

      if (!numberProperties.containsKey(i)) {
        continue;
      }

      int count = numberCounts.getOrDefault(i, 0);

      HBox keyRow = new HBox(6);

      keyRow.setAlignment(Pos.CENTER_LEFT);

      keyRow.getStyleClass().add("key-row");

      Label numberLabel = new Label(String.valueOf(i));

      numberLabel.setMinWidth(25);

      numberLabel.getStyleClass().add("key-number");

      Label equalsLabel = new Label("=");

      equalsLabel.getStyleClass().add("subtle-label");

      TextField letterField = createLetterTextField();

      letterField.setPrefWidth(40);

      letterField.getStyleClass().add("key-letter");

      letterField.textProperty().bindBidirectional(numberProperties.get(i));

      Label countLabel = new Label("× " + count);

      countLabel.getStyleClass().add("key-count");

      countLabel.setTooltip(new Tooltip("Number " + i + " occurs " + count + " times"));

      keyRow.getChildren().addAll(numberLabel, equalsLabel, letterField, countLabel);

      int row = index % 15;
      int col = (index / 15) * 1;

      grid.add(keyRow, col, row);

      index++;
    }

    box.getChildren().add(grid);

    return box;
  }

  // =============================================================
  // CREATE CROSSWORD GRID
  // =============================================================

  private GridPane createCrosswordGrid(List<List<Integer>> data) {

    GridPane grid = new GridPane();

    grid.setHgap(1);
    grid.setVgap(1);

    grid.setAlignment(Pos.CENTER);

    grid.getStyleClass().add("crossword-grid");

    for (int r = 0; r < data.size(); r++) {

      List<Integer> row = data.get(r);

      for (int c = 0; c < row.size(); c++) {

        int cellNum = row.get(c);

        if (cellNum == 0) {

          Pane blackCell = new Pane();
          blackCell.setPrefSize(CELL_SIZE, CELL_SIZE);
          blackCell.getStyleClass().add("black-cell");

          grid.add(blackCell, c, r);

        } else {

          VBox cryptoCell = createCryptoCell(cellNum);

          grid.add(cryptoCell, c, r);
        }
      }
    }

    return grid;
  }

  // =============================================================
  // CREATE CRYPTO CELL
  // =============================================================

  private VBox createCryptoCell(int number) {

    VBox cellBox = new VBox();

    cellBox.setPrefSize(CELL_SIZE, CELL_SIZE);

    cellBox.setAlignment(Pos.TOP_CENTER);

    cellBox.getStyleClass().add("crypto-cell");

    Label numLabel = new Label(String.valueOf(number));

    numLabel.getStyleClass().add("crypto-number");

    HBox numWrapper = new HBox(numLabel);

    numWrapper.setAlignment(Pos.TOP_LEFT);

    TextField tf = createLetterTextField();

    tf.getStyleClass().add("crypto-letter");

    tf.textProperty().bindBidirectional(numberProperties.get(number));

    cellBox.getChildren().addAll(numWrapper, tf);

    return cellBox;
  }

  // =============================================================
  // LETTER FIELD
  // =============================================================

  private TextField createLetterTextField() {

    TextField tf = new TextField();

    tf.setAlignment(Pos.CENTER);

    tf.textProperty().addListener((obs, oldVal, newVal) -> {

      if (newVal == null || newVal.isEmpty()) {
        return;
      }

      String filtered = newVal.replaceAll("[^a-zA-ZÅÄÖåäö]", "");

      if (filtered.isEmpty()) {

        tf.setText("");

      } else {

        String singleUpper = filtered.substring(filtered.length() - 1).toUpperCase();

        if (!singleUpper.equals(newVal)) {

          tf.setText(singleUpper);
        }
      }
    });

    return tf;
  }

  // =============================================================
  // ALERTS
  // =============================================================

  private void showInfoAlert(String title, String content) {

    Alert alert = new Alert(Alert.AlertType.INFORMATION);

    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);

    alert.showAndWait();
  }

  private void showErrorAlert(String title, String content) {

    Alert alert = new Alert(Alert.AlertType.ERROR);

    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);

    alert.showAndWait();
  }

  // =============================================================
  // MAIN
  // =============================================================

  public static void main(String[] args) {
    launch(args);
  }
}
