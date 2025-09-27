---
# 📊 Talkie-Talkie Database & App

Proyecto de tesis (2024) que combina el desarrollo de una **aplicación Android** con la construcción de una **base de datos en Firebase** y un módulo de **análisis de datos en Python**.  
El objetivo fue diseñar un sistema para **almacenar, consultar y visualizar información de usuarios en tiempo real**, integrando tecnologías móviles y de ciencia de datos.
---

## 🚀 Tecnologías usadas

- **Android (Gradle, Java)**
- **Firebase** (almacenamiento y autenticación)
- **Python** (pandas, numpy, matplotlib, jupyter)

## 📂 Estructura del repositorio
- android_app/ → Proyecto Android (Gradle, app/, gradle/, build.gradle…)
- analysis/ → Notebooks Jupyter ( Analisis_BaseD.ipynb)
- visualization/ → Scripts Python (VisualizBD.py)
- data/ → Datos de ejemplo 
- results/ → Gráficas y outputs generados
- docs/ → Documentación, PDF de tesis y capturas
- config/ → Archivos de configuración ( Firebase)

## ⚙️ Cómo ejecutar el proyecto

### 🔹 Android (app)
1. Abrir la carpeta `android_app/` en **Android Studio**.  
2. Sincronizar dependencias (Gradle se descarga automáticamente).  
3. Compilar y ejecutar en emulador o dispositivo físico:  
   ```bash
   ./gradlew assembleDebug

### 🔹 Python (análisis y visualización)

1. Crear un entorno virtual:
   ```bash
   python -m venv .venv

2. Activar el entorno:
   - Windows:
     ```bash
     .venv\Scripts\activate
     ```
   - Linux/Mac:
     ```bash
     source .venv/bin/activate
     ```
3. Instalar dependencias:
   ```bash
   pip install -r requirements.txt

4. Abrir el notebook principal:
   ```bash
   jupyter notebook analysis/Analisis_BaseD.ipynb


---