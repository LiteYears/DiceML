# DiceML Predictor

**An Android dice-history analyzer and probability research sandbox.**

DiceML Predictor records two-dice outcomes and compares theoretical probabilities with historical observations. Its analysis engine combines Bayesian estimation, frequency tests, transition analysis, a small learned model, and walk-forward evaluation in one native Android application.

> **Important:** Dice outcomes are random. Historical patterns and model outputs do not predict or guarantee future results. This project is an analysis and simulation tool, not betting advice.

## Analysis features

- **Two-dice outcome tracking** — records individual die values, sums, categories, timestamps, and user-entered result history.
- **Theoretical baseline** — calculates the combinatorial distribution for sums 2 through 12 and the Under (2–6), Seven, and Over (8–12) categories.
- **Bayesian estimation** — configurable prior strength, posterior category estimates, and 95% credible intervals.
- **Goodness-of-fit analysis** — chi-square diagnostics for category and sum distributions, with sample-size and significance reporting.
- **Rolling-window analysis** — recent frequencies, rolling mean, and comparison with the theoretical mean.
- **Transition analysis** — Markov transition counts and tests for evidence of dependence between successive categories.
- **Model experiments** — a compact neural model and ensemble components alongside Bayesian and sequence-based features.
- **Evaluation tools** — walk-forward backtesting, Brier score, log loss, calibration error, and Monte Carlo simulations.
- **Local history and checkpoints** — Room persistence for recorded rolls and saved model-weight checkpoints.
- **Data export** — export historical roll records as CSV.

## Outcome categories

The app groups two-dice sums into three categories:

| Category | Sum | Theoretical probability |
|---|---:|---:|
| Under | 2–6 | 15/36 (41.67%) |
| Seven | 7 | 6/36 (16.67%) |
| Over | 8–12 | 15/36 (41.67%) |

These are the mathematical probabilities for two independent, fair six-sided dice. Historical frequencies can differ by chance, especially in small samples.

## Statistical methodology

DiceML keeps several kinds of evidence visible rather than treating a model score as certainty:

1. **Baseline comparison:** compare observations with the known combinatorial distribution.
2. **Bayesian updating:** combine historical counts with configurable prior assumptions.
3. **Diagnostic testing:** report whether the available sample is large enough for inference and whether observed deviations are statistically significant.
4. **Temporal analysis:** inspect rolling windows and category transitions without assuming that a past sequence must repeat.
5. **Out-of-sample evaluation:** use walk-forward metrics to compare model outputs with theoretical and simple baseline methods.
6. **Simulation:** explore the range of possible outcomes under stated probability and payout assumptions.

A statistically unusual historical sample is not proof that a future roll is predictable. Any displayed confidence or expected-value estimate is conditional on the model and its assumptions.

## Technology

- **Platform:** Android
- **Language:** Kotlin
- **UI:** Jetpack Compose / Material 3
- **Persistence:** Room
- **Concurrency:** Kotlin Coroutines
- **Tests:** JUnit, Robolectric, and Android UI test dependencies

## Build

1. Clone the repository.
2. Open the project in Android Studio.
3. Allow Gradle sync to complete using the project's configured Android and Kotlin plugins.
4. Select the `app` run configuration and launch it on an emulator or Android device.

The project currently targets a modern Android SDK. Use the SDK and JDK versions requested by the Gradle configuration in the repository.

## Data and privacy

Roll history and model-weight checkpoints are stored in the app's local Room database. Review the app's export and device-backup behaviour before entering data you consider sensitive.

## Contributing

Focused bug reports and pull requests are welcome. Include sample data, expected calculations, actual output, and steps to reproduce statistical or UI issues.

## Disclaimer

This software is provided for educational, statistical, and simulation purposes. It does not provide guaranteed predictions, financial advice, or a reliable method for overcoming the randomness of dice games.
