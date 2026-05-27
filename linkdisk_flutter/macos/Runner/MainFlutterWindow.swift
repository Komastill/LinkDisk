import Cocoa
import FlutterMacOS

class MainFlutterWindow: NSWindow {
  override func awakeFromNib() {
    let flutterViewController = FlutterViewController()
    let windowFrame = self.frame

    self.contentViewController = flutterViewController
    self.setFrame(windowFrame, display: true)

    // LinkDisk 桌面端最小窗口尺寸。
    // 防止窗口被缩得太小后出现 RenderFlex overflow，
    // 同时保证按钮、卡片和状态区不会被挤乱。
    self.minSize = NSSize(width: 1280, height: 820)
    self.setContentSize(NSSize(width: 1280, height: 820))
    self.center()

    RegisterGeneratedPlugins(registry: flutterViewController)

    super.awakeFromNib()
  }
}
