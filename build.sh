#!/usr/bin/env bash
set -euo pipefail

# Project paths
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
LIB_DIR="$ROOT_DIR/lib"
TARGET_DIR="$ROOT_DIR/target"
CLASSES_DIR="$TARGET_DIR/classes"
JAR_FILE="$TARGET_DIR/MyFramework.jar"
MANIFEST_FILE="$TARGET_DIR/MANIFEST.MF"
DEST_TOMCAT_LIB="/opt/apache-tomcat-10.1.48/lib"
DEST_JAR="$DEST_TOMCAT_LIB/MyFramework.jar"

# Ensure tools
command -v javac >/dev/null 2>&1 || { echo "Error: javac not found. Install JDK." >&2; exit 1; }
command -v jar >/dev/null 2>&1 || { echo "Error: jar tool not found. Install JDK." >&2; exit 1; }

# Prepare directories
mkdir -p "$CLASSES_DIR"

# Classpath (Jakarta Servlet API for Tomcat 10)
# Prefer a jakarta.servlet-api jar in lib/, else try Tomcat's lib directory.
CP=""
if [[ -f "$LIB_DIR/servlet-api.jar" ]]; then
  CP="$LIB_DIR/servlet-api.jar"
elif [[ -f "$LIB_DIR/servlet-api.jar" ]]; then
  CP="$LIB_DIR/servlet-api.jar"
else
  # Try to find a jakarta.servlet-api-*.jar in Tomcat lib
  CANDIDATE=$(ls "$DEST_TOMCAT_LIB"/servlet-api-*.jar 2>/dev/null | head -n 1 || true)
  if [[ -n "${CANDIDATE:-}" && -f "$CANDIDATE" ]]; then
    CP="$CANDIDATE"
  fi
fi

if [[ -z "$CP" ]]; then
  echo "Error: Could not locate jakarta.servlet-api jar for compilation." >&2
  echo "Hints:" >&2
  echo "  - Copy your Tomcat's jakarta.servlet-api-<ver>.jar into $LIB_DIR as jakarta.servlet-api.jar" >&2
  echo "  - Or set DEST_TOMCAT_LIB correctly and ensure it contains jakarta.servlet-api-*.jar" >&2
  exit 1
fi

# Collect sources
SOURCES_LIST="$TARGET_DIR/sources.txt"
find "$SRC_DIR" -type f -name "*.java" > "$SOURCES_LIST"
if [[ ! -s "$SOURCES_LIST" ]]; then
  echo "Error: No Java sources found under $SRC_DIR" >&2
  exit 1
fi

# Compile
echo "Compiling sources..."
# Add all jars from lib/ to the classpath in addition to the servlet API
FULL_CP="$CP:$LIB_DIR/*"
javac -cp "$FULL_CP" -d "$CLASSES_DIR" @"$SOURCES_LIST"

# Manifest
cat > "$MANIFEST_FILE" <<EOF
Manifest-Version: 1.0
Created-By: build.sh

EOF

# Package JAR
echo "Packaging $JAR_FILE ..."
jar cfm "$JAR_FILE" "$MANIFEST_FILE" -C "$CLASSES_DIR" .

# Deploy to Tomcat lib (replace if exists)
echo "Copying framework JAR to $DEST_JAR (requires permissions if protected)..."
if cp -f "$JAR_FILE" "$DEST_JAR" 2>/dev/null; then
  echo "Copied to $DEST_JAR"
else
  echo "Direct copy failed (likely permissions). Retrying with sudo..."
  if command -v sudo >/dev/null 2>&1; then
    if sudo cp -f "$JAR_FILE" "$DEST_JAR"; then
      echo "Copied to $DEST_JAR with sudo"
    else
      echo "Warning: Failed to copy to $DEST_JAR. Please copy manually with sufficient permissions." >&2
    fi
  else
    echo "Warning: sudo not available. Please copy $JAR_FILE to $DEST_JAR manually with sufficient permissions." >&2
  fi
fi

# Also deploy framework dependencies (e.g. commons-beanutils, commons-logging) to Tomcat lib
for dep in "$LIB_DIR"/commons-beanutils-*.jar "$LIB_DIR"/commons-logging-*.jar; do
  if [[ -f "$dep" ]]; then
    dest_dep="$DEST_TOMCAT_LIB/$(basename "$dep")"
    echo "Copying dependency $(basename "$dep") to $dest_dep ..."
    if cp -f "$dep" "$dest_dep" 2>/dev/null; then
      echo "Copied $(basename "$dep") to $dest_dep"
    else
      if command -v sudo >/dev/null 2>&1; then
        if sudo cp -f "$dep" "$dest_dep"; then
          echo "Copied $(basename "$dep") to $dest_dep with sudo"
        else
          echo "Warning: Failed to copy $(basename "$dep") to $dest_dep. Please copy manually if needed." >&2
        fi
      else
        echo "Warning: sudo not available. Please copy $dep to $dest_dep manually with sufficient permissions." >&2
      fi
    fi
  fi
done

# Done
echo "Built JAR: $JAR_FILE"
