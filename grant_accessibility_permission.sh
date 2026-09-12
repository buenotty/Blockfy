#!/bin/bash
echo "==================================================="
echo "  Blockfy - Concedendo Permissões e Modo Invisível"
echo "==================================================="

adb shell settings put secure enabled_accessibility_services com.buenotty.blockfy/com.robingebert.blokky.feature_accessibility.ReelsBlockAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell pm grant com.buenotty.blockfy android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant com.buenotty.blockfy android.permission.PACKAGE_USAGE_STATS

echo ""
echo "✅ Permissão de Acessibilidade concedida!"
echo "✅ Proteção Bancária Invisível (WRITE_SECURE_SETTINGS) ativada!"
echo "Agora o Blockfy pausa e religa sozinho sem alertas em bancos digitais."
