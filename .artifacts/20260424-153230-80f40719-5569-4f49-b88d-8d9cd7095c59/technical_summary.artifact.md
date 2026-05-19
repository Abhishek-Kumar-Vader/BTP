# Odia InScript Keyboard: Technical Architecture & Implementation Deep-Dive

## 1. Project Abstract & Objectives
The **Odia InScript Keyboard** is a specialized Android Input Method Editor (IME) designed to provide a native, high-performance typing experience for the Odia language (Unicode block U+0B00–U+0B7F). The primary objective is to implement the Government of India's **InScript standard** within a modern, reactive framework.

### Core Functionalities:
- **Full Odia Script Support**: Complete mapping of vowels, consonants, matras (dependent vowels), and conjuncts.
- **Dynamic Mode Switching**: Seamless transition between Odia (Alpha), Numeric, and Symbol layouts.
- **Intelligent Shift States**: Three-stage shift logic (Off, One-Shot, Caps Lock) to handle shifted character variations.
- **Haptic Feedback**: Low-latency tactile response optimized for high-speed typing.
- **Future-Ready ML Integration**: Built-in hooks for offline next-word prediction and auto-correction.

---

## 2. System Architecture
The application adheres to a strict **MVVM (Model-View-ViewModel)** architectural pattern, modified to suit the unique lifecycle requirements of an Android `InputMethodService`.

### Directory Structure & Responsibility Mapping:
- `com.example.odiakeyboard.ime`: **Entry Point & Service Layer**. Manages the IME lifecycle and bridges Android's `InputConnection` with the ViewModel.
- `com.example.odiakeyboard.viewmodel`: **Business Logic & State Management**. Contains the "Brain" of the keyboard, decoupled from Android UI components.
- `com.example.odiakeyboard.ui`: **Presentation Layer**. Implements the UI using Jetpack Compose, driven entirely by the ViewModel's state.
- `com.example.odiakeyboard.model`: **Data Structures**. Defines the immutable state and key data models.
- `com.example.odiakeyboard.layout`: **Domain Data**. Encapsulates the static InScript layout mapping.

---

## 3. UI & Presentation Layer (Jetpack Compose)
The UI is built using **Jetpack Compose**, leveraging a unidirectional data flow (UDF) model.

### UI State Management & Hoisting:
- **State Flow**: The `KeyboardViewModel` exposes a single `StateFlow<KeyboardState>`. The `KeyboardScreen` consumes this state using `collectAsStateWithLifecycle()`, ensuring efficiency and lifecycle awareness.
- **State Hoisting**: Events (key presses, long presses) are hoisted from individual `KeyboardKey` components up to the `KeyboardViewModel`.
- **Composition Strategy**: The `OdiaInputMethodService` uses `ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool` to manage the lifecycle of the `ComposeView` within the IME window, preventing memory leaks.

### Component Breakdown:
- `KeyboardScreen`: The root container that organizes the suggestion strip, character rows, and functional keys (Shift, Space, Backspace).
- `KeyboardKey`: A highly optimized custom component that handles visual rendering and interaction logic, including long-press detection for rapid deletion.

---

## 4. Core Business Logic & Input Processing
The core logic resides in `KeyboardViewModel`, which acts as an intermediary between the user's touch events and the text editor.

### The Input Pipeline:
1. **Event Capture**: User taps a key in the Compose UI.
2. **ViewModel Processing**: `onKeyPressed(key)` determines the action (commit text, delete, toggle mode).
3. **Shift Logic**:
   - `OFF`: Commits the base character (e.g., କ).
   - `ONE_SHOT`: Commits the shifted character (e.g., ଖ) and reverts to `OFF`.
   - `CAPS_LOCK`: Commits shifted characters until manually toggled off.
4. **InputConnection Bridge**: The ViewModel calls an `InputConnectionHandler` interface. The implementation in `OdiaInputMethodService` executes the actual `commitText` or `deleteSurroundingText` calls on the active `InputConnection`.

---

## 5. Data Layer & Repository Pattern
While Phase 1 is primarily logic-driven, the architecture includes a formal data layer to support future expansion.

### Suggestion Repository:
- **`SuggestionRepository` Interface**: Defines the contract for fetching suggestions based on the `currentWordBuffer`.
- **Decoupling**: The ViewModel does not know *how* suggestions are generated (ML, dictionary, or n-grams), only that it receives a `Flow<List<String>>`.
- **Offline First**: The architecture specifies that all data processing must occur on-device (via `Dispatchers.Default`) to ensure privacy and low latency.

---

## 6. Advanced Integrations: IME Lifecycle & Haptics
### IME Lifecycle Management:
Unlike standard Activities, IMEs have a persistent background presence.
- **`ImeLifecycleOwner`**: A custom implementation of `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`. This allows the `KeyboardViewModel` to survive configuration changes (like orientation) while correctly disposing when the service is destroyed.
- **Window Management**: Lifecycle owners are manually attached to the window's `decorView` to support Compose-internal components like Dialogs.

### Haptic Feedback Engine:
- Uses `VibratorManager` (API 31+) and `VibrationEffect` to provide high-precision tactile feedback.
- **Efficiency**: Haptics are triggered via a callback from the UI layer to the Service, keeping the ViewModel "pure" and testable.

---

## 7. Build Configuration & Tech Stack
- **Target SDK**: 35 (Android 15)
- **Minimum SDK**: 26 (Android 8.0)
- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose (BOM 2026.02.01)
- **Async Pattern**: Kotlin Coroutines & Flow
- **Architecture**: MVVM with UDF
- **Testing Stack**: JUnit 5, MockK, Turbine (for Flow testing), and Compose UI Test.

---

## 8. Final Year Project: Technical Justification
### Why MVVM?
Standard IME development often leads to massive Service classes (God Objects). MVVM separates the Android-specific Service from the keyboard logic, enabling **100% unit test coverage** of character mapping and shift logic without an emulator.

### Why Jetpack Compose?
Traditional XML-based `KeyboardViews` are deprecated and inflexible. Compose allows for **dynamic layout adjustments** (e.g., resizing for tablets, switching between InScript and Phonetic layouts) with minimal code overhead and high performance.
