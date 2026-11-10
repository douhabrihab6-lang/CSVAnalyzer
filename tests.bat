@echo off
REM ══════════════════════════════════════════════════════════════
REM  CSV Analyzer — Script de tests unitaires
REM  Auteurs : Rihab Douhab & Soumia El Halla — EMSI 4IIR
REM ══════════════════════════════════════════════════════════════
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set MAVEN_HOME=C:\tools\apache-maven-3.9.6
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

cd /d "%~dp0"
echo Lancement des tests JUnit 5...
mvn test
pause
