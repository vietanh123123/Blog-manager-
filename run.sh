#!/bin/bash
# =============================================================
# Blog Backend — Build & Run Script (Linux / macOS)
# =============================================================
# This script replaces "mvn spring-boot:run".
# It manually compiles all Java files and runs the server.
#
# HOW MAVEN HELPED US BEFORE:
#   - Downloaded the PostgreSQL driver .jar automatically
#   - Compiled all Java source files in the right order
#   - Put everything on the classpath for us
#
# WHAT WE DO NOW:
#   - You download the PostgreSQL driver once (instructions below)
#   - javac compiles all .java files
#   - java runs the main class, with the driver on the classpath

set -e  # stop script immediately if any command fails

# ── Configuration ──────────────────────────────────────────────
MAIN_CLASS="com.blog.server.BlogServer"
SRC_DIR="src"
OUT_DIR="out"
LIB_DIR="lib"
PG_JAR="$LIB_DIR/postgresql-42.7.3.jar"

# ── Check Prerequisites ────────────────────────────────────────
echo "🔍 Checking prerequisites..."

if ! command -v javac &> /dev/null; then
    echo "❌ javac not found. Install Java 17+ JDK from https://adoptium.net/"
    exit 1
fi

JAVA_VER=$(javac -version 2>&1 | awk '{print $2}' | cut -d. -f1)
echo "   Java version: $JAVA_VER"
if [ "$JAVA_VER" -lt 17 ]; then
    echo "❌ Java 17+ required. Found version $JAVA_VER."
    exit 1
fi

# ── Check PostgreSQL Driver ────────────────────────────────────
if [ ! -f "$PG_JAR" ]; then
    echo ""
    echo "❌ PostgreSQL driver not found at: $PG_JAR"
    echo ""
    echo "   Download it with one of these commands:"
    echo ""
    echo "   Option 1 — curl:"
    echo "   curl -L -o $PG_JAR https://jdbc.postgresql.org/download/postgresql-42.7.3.jar"
    echo ""
    echo "   Option 2 — wget:"
    echo "   wget -O $PG_JAR https://jdbc.postgresql.org/download/postgresql-42.7.3.jar"
    echo ""
    echo "   Option 3 — Manual:"
    echo "   Download from https://jdbc.postgresql.org/download/ and place it in lib/"
    echo ""
    exit 1
fi

# ── Compile ────────────────────────────────────────────────────
echo "🔨 Compiling Java source files..."

# Create output directory if it doesn't exist
mkdir -p "$OUT_DIR"

# Find all .java files and compile them
# -cp lib/postgresql-*.jar  = put the PG driver on the classpath so imports resolve
# -d out                    = put .class files in the out/ directory
# $(find src -name "*.java") = compile every .java file we find
javac -cp "$PG_JAR" -d "$OUT_DIR" $(find "$SRC_DIR" -name "*.java")

echo "   ✅ Compilation successful."

# ── Copy Resources ─────────────────────────────────────────────
# Copy db.properties to the output dir so it's on the classpath at runtime
echo "📋 Copying resources..."
cp "$SRC_DIR/db.properties" "$OUT_DIR/"
echo "   ✅ Resources copied."

# ── Run ───────────────────────────────────────────────────────
echo ""
echo "🚀 Starting server..."
echo ""

# java -cp out:lib/postgresql-*.jar com.blog.server.BlogServer
#   -cp = classpath: our compiled classes (out/) + the PG driver jar
#   ':' separator = Linux/Mac (Windows uses ';' instead)
java -cp "$OUT_DIR:$PG_JAR" "$MAIN_CLASS"