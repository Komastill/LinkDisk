import 'dart:io';

class PlatformUtils {
  static bool get isWindows => Platform.isWindows;
  static bool get isAndroid => Platform.isAndroid;

  static bool isSystemProtectPath(String targetPath) {
    if (!isWindows) return false;
    final paths = [
      r"C:\Windows",
      r"C:\Program Files",
      r"C:\Program Files (x86)",
      r"C:\Users\Administrator",
      r"C:\Users\Public",
      r"C:\$Recycle.Bin",
      r"C:\System Volume Information",
      r"C:\ProgramData",
    ];
    String lower = targetPath.toLowerCase();
    return paths.any((p) => lower.startsWith(p.toLowerCase()));
  }
}