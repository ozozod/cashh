# VAYVENE – Android (NFC POS) v3

Correcciones:
- Evitada la colisión de recursos: `Theme.Vayvene` vive solo en `values/themes.xml`.
- `AndroidManifest.xml` sin atributo `package` y sin `activity-alias` (ya no exige `exported`).
- `LoginActivity` sigue con `exported=true` (porque tiene intent-filter).

Abrir en Android Studio, ajustar `BuildConfig.BASE_URL` y ejecutar en un dispositivo con NFC.