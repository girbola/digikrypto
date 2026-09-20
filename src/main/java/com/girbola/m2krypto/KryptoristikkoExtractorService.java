package com.girbola.m2krypto;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.file.Path;
import java.util.*;

public class KryptoristikkoExtractorService {

  private static final double BLACK_CELL_THRESHOLD = 50.0;
  private static final int ROW_Y_TOLERANCE = 12;
  private static final int INSET_PX = 3;

  private final Tesseract tesseract;

  static {
    // Load OpenCV native binaries safely
    nu.pattern.OpenCV.loadLocally();
  }

  public KryptoristikkoExtractorService() {
    tesseract = new Tesseract();
    // Point to your local tessdata folder path if needed:
    tesseract.setDatapath("/Users/gerbiloi/Documents/tessdata/");

    // Force Tesseract to treat cropped images as a single word/digit cluster
    tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_WORD); // PSM 8

    // Restrict OCR strictly to numeric digits 0-9
    tesseract.setVariable("tessedit_char_whitelist", "0123456789"); //
  }

  public List<String> extractMatrix(Path photoPath) throws Exception {
    return extractMatrix(photoPath.toAbsolutePath().toString());
  }

  public List<String> extractMatrix(String photoFilePath) throws Exception {
    Mat src = Imgcodecs.imread(photoFilePath);
    if (src.empty()) {
      throw new IllegalArgumentException("Failed to load image from path: " + photoFilePath);
    }

    try {
      // 1. Grayscale and Adaptive Binarization
      Mat gray = new Mat();
      Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

      Mat thresh = new Mat();
      Imgproc.adaptiveThreshold(gray, thresh, 255,
        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 15, 2);

      // 2. Find contours representing grid cell boxes
      List<MatOfPoint> contours = new ArrayList<>();
      Mat hierarchy = new Mat();
      Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

      List<Rect> cellRects = new ArrayList<>();
      for (MatOfPoint c : contours) {
        Rect rect = Imgproc.boundingRect(c);
        // Filter out outer boundaries and tiny noise
        if (rect.width > 15 && rect.height > 15 && Math.abs(rect.width - rect.height) < 10) {
          cellRects.add(rect);
        }
      }

      // 3. Structure grid bounding boxes into 2D rows
      List<List<Rect>> grid = organizeIntoGrid(cellRects);

      // 4. Extract digits per cell
      List<String> matrixRows = new ArrayList<>();

      for (List<Rect> row : grid) {
        List<String> rowValues = new ArrayList<>();

        for (Rect cellRect : row) {
          Mat cellMat = new Mat(gray, cellRect);
          double avgIntensity = Core.mean(cellMat).val[0];

          // Assign 0 for dark/black cells
          if (avgIntensity < BLACK_CELL_THRESHOLD) {
            rowValues.add("0");
          } else {
            // Crop inner cell to remove black box boundaries
            Rect paddedRect = cropInnerCell(cellRect, src.cols(), src.rows());
            Mat croppedCell = new Mat(gray, paddedRect);

            // Scale up small cells for higher Tesseract OCR accuracy
            Mat scaledCell = new Mat();
            Imgproc.resize(croppedCell, scaledCell, new Size(croppedCell.width() * 2, croppedCell.height() * 2));

            BufferedImage bufferedImage = matToBufferedImage(scaledCell);
            String recognizedNumber = parseCellNumber(bufferedImage);

            rowValues.add(recognizedNumber);
          }
        }
        matrixRows.add(String.join(",", rowValues));
      }

      return matrixRows;

    } finally {
      src.release();
    }
  }

  private List<List<Rect>> organizeIntoGrid(List<Rect> rects) {
    rects.sort(Comparator.comparingInt(r -> r.y));
    List<List<Rect>> grid = new ArrayList<>();
    if (rects.isEmpty()) return grid;

    List<Rect> currentRow = new ArrayList<>();
    int rowY = rects.get(0).y;

    for (Rect r : rects) {
      if (Math.abs(r.y - rowY) > ROW_Y_TOLERANCE) {
        currentRow.sort(Comparator.comparingInt(rect -> rect.x));
        grid.add(new ArrayList<>(currentRow));
        currentRow.clear();
        rowY = r.y;
      }
      currentRow.add(r);
    }
    if (!currentRow.isEmpty()) {
      currentRow.sort(Comparator.comparingInt(rect -> rect.x));
      grid.add(currentRow);
    }
    return grid;
  }

  private Rect cropInnerCell(Rect cell, int maxW, int maxH) {
    int x = Math.max(0, cell.x + INSET_PX);
    int y = Math.max(0, cell.y + INSET_PX);
    int w = Math.min(maxW - x, cell.width - (INSET_PX * 2));
    int h = Math.min(maxH - y, cell.height - (INSET_PX * 2));
    return new Rect(x, y, w, h);
  }

  private String parseCellNumber(BufferedImage img) {
    try {
      String text = tesseract.doOCR(img);
      text = text.replaceAll("[^0-9]", "").trim();
      return text.isEmpty() ? "0" : text;
    } catch (TesseractException e) {
      return "0";
    }
  }

  private BufferedImage matToBufferedImage(Mat mat) {
    int type = BufferedImage.TYPE_BYTE_GRAY;
    if (mat.channels() > 1) {
      type = BufferedImage.TYPE_3BYTE_BGR;
    }
    BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
    mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
    return image;
  }
}
