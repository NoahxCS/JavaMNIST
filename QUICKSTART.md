# Quick guide

Everything in one place, in order. Total time on a fresh machine: about 5 minutes (mostly downloads).

---

## 0. Pre-checks

Run these two commands in the project folder. Both must print a version.

```bash
java -version     # need 17 or newer
mvn -version      # need 3.6 or newer
```

You also need an internet connection for the first build (Maven downloads the DL4J, ND4J and SQLite
jars) and for the first training run (MNIST, about 15 MB).

---

## 1. Build

```bash
mvn clean package
```

Expected: `BUILD SUCCESS`.

> **Windows / PowerShell:** always put quotes around `-Dexec...` properties, otherwise PowerShell
> splits the argument and Maven says `Unknown lifecycle phase ".mainClass=..."`.
> ```bash
> mvn exec:java "-Dexec.mainClass=com.college.mnist.Main"
> ```
> On Linux/macOS the quotes change nothing, so they are safe to keep.

---

## 2. Train the model (once)

```bash
mvn exec:java "-Dexec.mainClass=com.college.mnist.model.ModelTrainer"
```

What happens:

1. DL4J downloads MNIST the first time (about 15 MB) and caches it.
2. The network trains: 784 -> 128 (ReLU) -> 10 (softmax), Adam, batch size 64, 5 epochs.
3. The score is printed every 100 batches, then the test accuracy (about 97 %).
4. `mnist_model.zip` is written in the project folder.

Want a fast smoke test instead of full training?

```bash
mvn exec:java "-Dexec.mainClass=com.college.mnist.model.ModelTrainer" "-Dexec.args=1"
```

One epoch gives about 94 % accuracy in a few seconds and also produces `mnist_model.zip`.

---

## 3. Run the app

```bash
mvn exec:java "-Dexec.mainClass=com.college.mnist.Main"
```

* Draw a digit in the black square (white strokes, about 20 px thick, mouse or touch).
* Press **Predict** to see `Digit: N   Confidence: ..%` and the 28x28 preview.
* The bottom strip shows the last three predictions; **Show all history** expands the full table.
* The results are stored in `digits.db`, created automatically.

---

## 4. Test cases (optional)

```bash
mvn exec:java "-Dexec.mainClass=com.college.mnist.TestCases"
```

Prints `Id | Description | Expected output | Pass/Fail` for eight cases and a summary line. Case 7
(the real prediction) needs `mnist_model.zip`; the rest run without a model.

---

## 5. Common problems

| Message / symptom | Cause | Fix |
|---|---|---|
| `Unknown lifecycle phase ".mainClass=..."` | PowerShell split the unquoted `-D` argument | Add quotes: `"-Dexec.mainClass=com.college.mnist.Main"` |
| `Model file mnist_model.zip was not found` | Training not done, or you are in the wrong folder | Run step 2 in the folder that contains `pom.xml` |
| `Could not resolve dependencies` / connection errors | No internet, or a proxy blocking Maven Central | Connect to the internet and retry; on a locked-down machine see the `~/.m2` note below |
| `Target directory ... must exist` | An old DL4J cache path was configured but is missing | Delete `target/` and run again; the project creates `target/dl4j-data` itself |
| Training stops with a download error | The MNIST mirror is unreachable | Retry, or copy the MNIST files into `target/dl4j-data/data/MNIST` |
| Prediction looks wrong | Stroke is too thin, too small or off-centre | Draw thicker, larger, centred; watch the preview panel |
| `mvn` cannot write to `~/.m2` | Read-only home folder (lab machines, sandboxes) | `mvn -s maven-settings-localrepo.xml <goal>` — keeps jars in `.m2repo/` inside the project |
| After `mvn clean` training downloads MNIST again | `clean` deletes `target/`, where DL4J's cache lives | Expected behaviour, or move the cache to a shared folder: `-Dexec.jvmArgs="-Dorg.deeplearning4j.resources.directory=."` |

---

## 6. Cheat sheet

| Command | Purpose |
|---|---|
| `mvn clean package` | Build everything |
| `mvn exec:java "-Dexec.mainClass=com.college.mnist.model.ModelTrainer"` | Train and save `mnist_model.zip` |
| `mvn exec:java "-Dexec.mainClass=com.college.mnist.Main"` | Start the GUI |
| `mvn exec:java "-Dexec.mainClass=com.college.mnist.TestCases"` | Run the 8 test cases |
| `mvn -s maven-settings-localrepo.xml <goal>` | Same, but with the local jar repository inside the project |

Files you will see after running: `mnist_model.zip` (trained network), `digits.db` (history),
`target/` (build output and DL4J caches). Deleting `digits.db` just clears the history; deleting
`mnist_model.zip` means you have to train again.
