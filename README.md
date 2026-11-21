# Easy Keys

Easy Keys is an independent Android keyboard project with Persian and English layouts.

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


## Recent keyboard updates
- Saved suggestions can be inserted with a tap and removed with a long press.
- The default Persian suggestion words can also be hidden from the suggestion strip.
- Export List opens Android's document picker so the suggestion list can be saved to a chosen folder.
- Persian mode types Persian digits; English mode types Latin digits.
- The bottom control row uses number-key-sized controls and a wider Space key.
- Shift tap opens an alternate-symbol layout; tapping a symbol returns to letters. Holding Shift toggles English letter case.
- The launcher icon is included in Android mipmap densities and each PNG is under 50 KB.
