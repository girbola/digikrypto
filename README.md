# KryptoGrid Studio (JavaFX Crypto Crossword)

**KryptoGrid Studio** is an interactive JavaFX desktop application designed to build, edit, and solve code-based crypto crosswords (*Kryptoristikko*). It offers flexible grid generation methods, dynamic letter-to-number mapping, and full state saving and loading.

---

## Key Features

* 🧩 **Interactive Crossword Solver**: Type a letter into any numbered cell or letter bank entry to instantly update all corresponding numbers across the entire crossword matrix in real time.
* ✏️ **Visual Matrix Builder**: Define custom grid dimensions (rows × columns per line) and design crosswords directly through an interactive keyboard-navigable cell grid.
* ⌨️ **Numeric Keypad Editor**: Quickly append numeric values and black cells (`0`) to your matrix using a dynamic numeric keypad (supporting customizable max number limits).
* 📄 **Raw CSV View**: Directly inspect, copy, paste, or manually modify raw comma-separated crossword data.
* 💾 **Session State Saving & Loading**: Save unfinished crosswords to `.krypto` (JSON) format, preserving grid layout, dimensions, max number settings, and current letter solutions.

---

## Prerequisites

* **Java JDK 17** or higher
* **JavaFX SDK 17+** (if not bundled with your JDK)
