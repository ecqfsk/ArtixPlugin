#!/usr/bin/env bash
# ============================================================
#  ArtixCloud Plugin — Build Script
#  Requisito: Java 21+ e Maven 3.8+ instalados no PATH
# ============================================================

set -e

echo ""
echo "  =============================================  "
echo "    ArtixCloud Plugin — Build v1.0.0"
echo "  =============================================  "
echo ""

# Verifica Java 21+
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 21 ] 2>/dev/null; then
  echo "[ERRO] Java 21 ou superior é necessário. Versão detectada: $JAVA_VER"
  exit 1
fi

echo "[OK] Java $JAVA_VER detectado."
echo "[INFO] Baixando dependências e compilando..."
mvn clean package -q
echo ""
echo "[SUCESSO] Build concluído!"
echo "[ARQUIVO] target/ArtixCloud-1.0.0.jar"
echo ""
echo " Copie o JAR para a pasta plugins/ do seu servidor Paper 1.21.1"
echo " Configure config.yml com seu token da API ArtixCloud."
echo ""
