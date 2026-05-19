# Odia Keyboard

Odia Keyboard is a custom Android Input Method Editor designed to provide a highly responsive and intelligent Odia language typing experience. The application features context-aware word suggestions, a native Kotlin spell correction engine, and a modern user interface built entirely with Jetpack Compose. All machine learning inference and text processing are performed locally on the device to ensure absolute user privacy and zero typing latency.

## Core Features

The keyboard offers native Odia typing with full support for complex grapheme clusters and nukta characters. The predictive text system combines multiple engines to deliver accurate suggestions. As the user types, a prefix engine searches a frequency-ordered dictionary for completions. If no valid prefix is found, the system automatically falls back to a custom phonetic spell corrector.

The spell correction engine utilizes a weighted Levenshtein distance algorithm specifically tuned for Odia. It groups phonetically similar characters, such as different sibilants or nasals, to apply discounted substitution penalties. This allows the keyboard to accurately understand common linguistic typos. Furthermore, a spacebar autocorrect heuristic automatically replaces misspelled words with the top phonetic correction when the user finishes typing a word.

For next-word prediction, the keyboard utilizes an N-gram engine for rapid bigram lookups. When bigrams do not yield enough suggestions, a TensorFlow Lite LSTM inference engine analyzes the previous five words to generate contextually relevant predictions.

## Architecture

The project is structured into distinct, decoupled layers.

The Input Method Service handles the keyboard lifecycle and communicates directly with the target application's input field using an InputConnectionHandler.

The Machine Learning and Suggestions package acts as the intelligence hub. The OdiaSuggestionRepository routes input through the PrefixEngine, OdiaSpellCorrector, NgramEngine, and LstmInferenceEngine based on the current typing context. ModelAssetLoader handles the background loading of dictionaries, N-gram probabilities, and the TensorFlow Lite model.

The ViewModel layer acts as the single source of truth for the UI state. The KeyboardViewModel manages shift states, keyboard modes, word history buffers, and executes the autocorrect logic.

The UI layer is built with Jetpack Compose and Material Design 3, providing a responsive and dynamic layout for both the keys and the suggestion strip.

## Technology Stack

The application is written entirely in Kotlin. The UI leverages Jetpack Compose. Machine learning features are powered by TensorFlow Lite. State management is handled through Kotlin Coroutines, StateFlow, and Jetpack ViewModel architecture. The testing suite includes JUnit 5 for unit tests and MockK for dependency mocking.

## Requirements

The project targets Android SDK 35 (Android 15) and requires a minimum SDK of 26 (Android 8.0).
