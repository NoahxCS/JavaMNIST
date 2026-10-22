# MNIST Digit Classifier

A Java desktop app that recognises handwritten digits. You draw a digit (0-9) with the mouse on a
black canvas, press **Predict**, and the app shows the digit with a confidence percentage and saves
the result in a local SQLite database.

* **GUI:** Java Swing (one window)
* **Neural network:** Deeplearning4j (DL4J) 1.0.0-M2.1, `nd4j-native-platform` CPU backend
* **Database:** SQLite through JDBC (`org.xerial:sqlite-jdbc`), no ORM
* **Build:** Maven, Java 17 or newer

Network: 784 inputs -> 128 hidden units (ReLU) -> 10 outputs (softmax), Adam, batch size 64,
5 epochs. Test accuracy about **97.4 %**.

Code style is deliberately simple for the viva: plain classes, named listener classes, `for`/`while`
loops. No lambdas, streams, records or anonymous classes.

---

## Quick start

Open a terminal in this folder (the one with `pom.xml`).

```bash
mvn clean package
mvn exec:java "-Dexec.mainClass=com.college.mnist.model.ModelTrainer"   # once, trains the model
mvn exec:java "-Dexec.mainClass=com.college.mnist.Main"                 # run the app
```

> Keep the quotes around `-Dexec.mainClass=...`. Without them PowerShell splits the argument and
> Maven reports `Unknown lifecycle phase ".mainClass=..."`. On Linux/macOS the quotes are harmless.

* The **first** build downloads the DL4J/ND4J/SQLite jars from Maven Central (a few minutes).
* The **first** training run downloads MNIST (about 15 MB, internet needed) and caches it.
* Training takes well under a minute and writes `mnist_model.zip` in this folder.
* If `mnist_model.zip` is missing, the app shows a message telling you to run `ModelTrainer` first.
  The app itself never trains.

See [QUICKSTART.md](QUICKSTART.md) for the same steps with pre-checks and common fixes.

---

## Using the app

| Part of the window | What it does |
|---|---|
| Canvas (left, 280x280) | Draw in white on black. Brush is about 20 px, mouse or touch. |
| Preview (right) | The 28x28 image the network actually receives. |
| Result label | Only the answer, e.g. `Digit: 7   Confidence: 98.6%`. |
| **Predict** | Downsamples the drawing, runs the network, saves the row, refreshes the history. |
| **Clear** | Wipes the canvas and the result. The database history is kept. |
| `Recent: 7  3  1` | Last three predictions, newest first, read from the database. |
| **Show all history** | Expands a scrollable read-only table (`id`, `digit`, `confidence`, `time`). |

Draw the digit **thick** and roughly in the middle. The app warns you when the canvas is empty.

---

## Project layout

```
src/main/java/com/college/mnist/
├── Main.java                     starts the app
├── TestCases.java                8 sample test cases (console program)
├── model/
│   ├── DigitClassifier.java      interface (abstraction)
│   ├── Dl4jDigitClassifier.java  loads mnist_model.zip and predicts
│   ├── Prediction.java           digit + confidence (encapsulation)
│   └── ModelTrainer.java         trains once, own main()
├── db/
│   ├── DatabaseManager.java      Singleton, all JDBC code
│   └── HistoryRecord.java        one row of the history table
├── ui/
│   ├── MainFrame.java            the window and the buttons
│   ├── DrawingCanvas.java        280x280 drawing area
│   ├── CanvasMouseHandler.java   mouse/touch listener
│   ├── PreviewPanel.java         enlarged 28x28 view
│   └── HistoryPanel.java         recent digits strip + expandable table
├── util/
│   └── ImageProcessor.java       downsample() (10x10 block averaging)
└── exception/
    └── PredictionException.java  custom checked exception
```

Files created when you run the project (not source): `mnist_model.zip`, `digits.db`, `target/`.

---

## Test cases

```bash
mvn exec:java "-Dexec.mainClass=com.college.mnist.TestCases"
```

Prints eight cases in the format `Id | Description | Expected output | Pass/Fail`. All eight pass
with a trained model; the database and image-processing cases also pass without one.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Unknown lifecycle phase ".mainClass=..."` | Quote the property: `mvn exec:java "-Dexec.mainClass=com.college.mnist.Main"` (PowerShell issue). |
| "Model file mnist_model.zip was not found" | Run the `ModelTrainer` command above, from this folder. |
| Training fails on the first run | Needs internet once. If your machine blocks the download, see the cache note below. |
| App opens but the prediction is wrong | Draw thicker/larger and more centred; check the preview panel. |
| `mvn` cannot write to `~/.m2` | Use `mvn -s maven-settings-localrepo.xml ...` (see QUICKSTART.md). |
| After `mvn clean`, training downloads MNIST again | Expected: `clean` deletes `target/`, where the MNIST cache lives (the ND4J native libraries are re-unpacked from the jars, no download needed). |

DL4J caches are kept inside the project so nothing is written to your home folder:
`target/javacpp-cache` (ND4J native libraries unpacked from the jars) and `target/dl4j-data`
(the MNIST download). To use the usual shared locations instead, add
`-Dexec.jvmArgs="-Dorg.bytedeco.javacpp.cachedir=. -Dorg.deeplearning4j.resources.directory=."`.

---

## More documentation

[DOCUMENTATION.md](DOCUMENTATION.md) has the class table, mermaid class diagram, database schema,
architecture flowchart, the list of OOP concepts with the class that demonstrates each one, and the
eight test cases with expected results.
