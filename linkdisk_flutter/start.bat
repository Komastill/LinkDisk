@echo off
echo ==============================================
echo    Flutter 国内镜像 一键启动
echo ==============================================
echo.

:: 国内镜像
set PUB_HOSTED_URL=https://pub.flutter-io.cn
set FLUTTER_STORAGE_BASE_URL=https://storage.flutter-io.cn

echo 已切换国内镜像
echo.

flutter pub get
echo.
echo --- 开始运行 ---
flutter run -d windows

:: 运行完停住，不闪退
echo.
echo --- 运行结束，按任意键关闭窗口 ---
pause