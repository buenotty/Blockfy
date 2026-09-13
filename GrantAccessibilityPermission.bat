@echo off
echo Concedendo permissao de Acessibilidade ao Blockfy...
adb shell settings put secure enabled_accessibility_services com.buenotty.blockfy/com.robingebert.blokky.feature_accessibility.ReelsBlockAccessibilityService
adb shell settings put secure accessibility_enabled 1
echo Concluido!
pause