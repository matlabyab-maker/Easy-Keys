# Easy Keys

Independent Android input-method project with Persian and English layouts.

## Build
Open this folder in Android Studio or push it to GitHub. GitHub Actions builds a debug APK on every branch push and via manual workflow dispatch.

## Enable
Install the APK, open Easy Keys, tap **فعال‌سازی صفحه‌کلید**, enable Easy Keys in Android settings, then tap **انتخاب صفحه‌کلید**.

## Notes
The File button opens Android's document picker and then the system share sheet. Undo/Redo track text entered through this keyboard in the current session. This initial source package has not been compiled or device-tested.
\n\n## GitHub Actions SDK setup\nThe workflow uses the runner's preinstalled `sdkmanager` directly and installs only `platform-tools`, `platforms;android-35`, and `build-tools;35.0.0`. It intentionally avoids the obsolete SDK `tools` package.\n
## Keyboard layout update

- The toolbar has one language switch (EN/FA), plus Undo, Redo, Past, Copy, File, and Answer.
- Answer and the Enter key use the focused editor's IME action when available, then fall back to an Enter key event.
- Persian layout includes ژ and ج.
- Arrow keys are on the right side of Space in left/right/up/down order.
- Bottom-row key widths are reduced while the row height remains 58dp.


## Source layout

The main Kotlin source files are stored in the repository-level `Source/` folder to keep frequently edited, text-heavy files easy to access. Android resources and the app module configuration remain under `app/`.
