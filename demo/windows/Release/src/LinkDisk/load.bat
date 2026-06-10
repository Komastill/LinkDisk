@echo off
setlocal enabledelayedexpansion

:: 1. 自动定位到项目根目录（src 的上一级）
cd /d "%~dp0\..\.."

:: 2. 清理旧的编译文件（可选，也可以删掉这行）
if exist bin rmdir /s /q bin
mkdir bin

:: 3. 编译所有包下的 Java 文件
javac -d bin -encoding UTF-8 ^
src\LinkDisk\model\*.java ^
src\LinkDisk\network\*.java ^
src\LinkDisk\ui\*.java

:: 4. 运行主类（全限定类名）
if %errorlevel% equ 0 (
    echo 编译成功，正在启动程序...
    java -cp bin LinkDisk.ui.MainFrame
) else (
    echo 编译失败，请检查错误信息！
)

pause