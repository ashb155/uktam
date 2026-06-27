# IndicOffline

IndicOffline is a powerful, entirely offline Android application for real-time speech-to-text (ASR) transcription and machine translation between Indic languages. Built with modern Android development practices, it runs state-of-the-art AI models directly on your device—ensuring complete privacy and zero reliance on cloud APIs.

## Why IndicOffline?
* **100% Offline & Private:** No audio recordings or text transcripts ever leave your device. Every single piece of processing—from speech recognition to large language model (LLM) translation—happens locally on your smartphone's silicon.
* **Made for India, by Indian AI:** Instead of relying on generic global models, IndicOffline is strictly built around models researched and trained specifically for Indian languages. By utilizing **Sarvam AI** and **AI4Bharat**, the app captures the nuances, dialects, and grammatical complexities of languages far better than generic cloud APIs.
* **Custom Quantized for Mobile:** Running multi-billion parameter AI models on a phone requires immense compute. IndicOffline uses models that were **custom-quantized** (GGUF/ONNX) specifically for this project. This hands-on optimization drastically reduces the memory footprint and battery consumption, allowing them to run smoothly on standard smartphone hardware while maintaining near-perfect accuracy.
* **Zero Latency / No Internet Required:** Perfect for remote areas, low-connectivity zones, or traveling. Once the models are downloaded, you never need Wi-Fi or mobile data to translate again.

## Key Features

* **Real-time Offline Speech Recognition:** Powered by **Sherpa-ONNX** using the [**AI4Bharat IndicConformer**](https://huggingface.co/ai4bharat/IndicConformer) model for high-speed, local transcription.
* **On-Device Translation:** Leverages the [**Sarvam Translate**](https://huggingface.co/sarvamai/sarvam-translate) model (from Sarvam AI) running via `llama.cpp` through JNI for highly accurate offline machine translation.
* **Native Text-to-Speech (TTS):** Automatically speaks the translated text using Android's native offline TTS engine.
* **Premium UI/UX:** Built entirely with **Jetpack Compose**, featuring:
  * Sleek, high-contrast Dark and Light modes.
  * Haptic feedback for tactile interactions (`Vibration` and `TouchApp` integrations).
  * Smooth transition animations and dynamic chat bubbles.
* **In-App Model Management:** Built-in downloader to fetch and unpack the necessary AI models directly onto the device's local storage.

## Technology Stack

* **Language:** Kotlin, C++
* **UI Framework:** Jetpack Compose, Material Design 3
* **Architecture:** MVVM (Model-View-ViewModel) with `StateFlow` and Coroutines.
* **AI & Machine Learning:**
  * [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) for Automatic Speech Recognition (ASR) running the [**AI4Bharat IndicConformer**](https://huggingface.co/ai4bharat/IndicConformer).
  * [llama.cpp](https://github.com/ggerganov/llama.cpp) (via custom JNI bindings) running [**Sarvam Translate**](https://huggingface.co/sarvamai/sarvam-translate) for offline translation.
* **Build System:** Gradle (Kotlin DSL), CMake for native C++ compilation.

## Getting Started

### Prerequisites

* Android Studio (Koala or newer recommended)
* Android SDK Minimum API Level: 26 (Android 8.0)
* Android NDK & CMake (Required for building the `llama.cpp` JNI bindings)

### Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/IndicOffline.git
   cd IndicOffline
   ```

2. **Sync Gradle & NDK**
   Open the project in Android Studio. Ensure that your SDK Manager has the NDK (Side by side) and CMake installed. Gradle will automatically sync and build the C++ bindings via the `CMakeLists.txt`.

3. **Model Preparation**
   The application requires the ONNX models for ASR and the `.gguf` file for Llama. 
   - Upon launching the app for the first time, you will be greeted by the `DownloadScreen`. 
   - Ensure you have an active internet connection for this initial step so the app can download and extract the required AI models to your device's internal storage.

4. **Run the App**
   Connect a physical Android device (emulators may struggle with local LLM inference without hardware acceleration) and click **Run**.


## Contributing

Contributions, issues, and feature requests are welcome! 
If you plan to implement major features (such as adding support for new Indic languages or swapping out the underlying LLM), please open an issue first to discuss the proposed changes.

## License

This project is licensed under the [MIT License](LICENSE).
