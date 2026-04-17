// Electron 主进程代码
const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const dgram = require('dgram');

// ========== 新增：扫描电脑所有磁盘 ==========
function getDiskDrives() {
  try {
    // Windows系统扫描所有磁盘（C:/ D:/ E:/...）
    const drives = [];
    // 遍历A-Z字母，检测是否是有效磁盘
    for (let i = 67; i <= 90; i++) { // 67=C, 90=Z
      const driveLetter = String.fromCharCode(i) + ':/';
      try {
        // 检测磁盘是否存在
        fs.accessSync(driveLetter);
        drives.push({
          letter: String.fromCharCode(i),
          path: driveLetter,
          // C盘默认读用户目录（避免权限问题），其他盘读根目录
          safePath: i === 67 ? `${process.env.USERPROFILE}/` : driveLetter
        });
      } catch (e) {
        // 磁盘不存在，跳过
        continue;
      }
    }
    return drives;
  } catch (e) {
    console.error('扫描磁盘失败：', e);
    // 兜底返回常用磁盘
    return [
      { letter: 'C', path: 'C:/', safePath: `${process.env.USERPROFILE}/` },
      { letter: 'D', path: 'D:/', safePath: 'D:/' }
    ];
  }
}

// 创建窗口
function createWindow() {
  const win = new BrowserWindow({
    width: 1000,
    height: 700,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
    }
  });

  // 加载你的网页
  win.loadFile('index.html');
  // 打开开发者工具（方便调试）
  win.webContents.openDevTools();

  // 窗口加载完成后，发送所有磁盘信息给前端
  win.webContents.on('did-finish-load', () => {
    const drives = getDiskDrives();
    win.webContents.send('disk-list', drives);
  });
}

// 当Electron准备好时创建窗口
app.whenReady().then(() => {
  createWindow();

  // ========== 局域网UDP发现（核心！）==========
  const udpServer = dgram.createSocket('udp4');
  const PORT = 54321;

  // 监听其他设备
  udpServer.on('message', (msg, rinfo) => {
    const deviceName = msg.toString();
    // 给前端发送设备信息
    BrowserWindow.getAllWindows()[0].webContents.send('device-found', {
      name: deviceName,
      ip: rinfo.address
    });
  });

  // 绑定端口
  udpServer.bind(PORT, () => {
    udpServer.setBroadcast(true);
  });

  // 监听前端的广播请求
  ipcMain.on('broadcast-self', (event, data) => {
    const client = dgram.createSocket('udp4');
    const message = Buffer.from(data.name);
    // 广播自己的信息（局域网所有设备都能收到）
    client.send(message, 0, message.length, PORT, '255.255.255.255');
  });

  // ========== 读取本地真实文件（优化C盘权限）==========
  ipcMain.on('read-local-files', (event, targetPath) => {
    try {
      // 读取指定路径的文件
      const files = fs.readdirSync(targetPath, { withFileTypes: true });
      const fileList = files.map(file => {
        const fullPath = path.join(targetPath, file.name);
        return {
          name: file.name,
          type: file.isDirectory() ? 'folder' : 'file',
          size: file.isFile() ? fs.statSync(fullPath).size : 0
        };
      });
      // 返回给前端
      event.reply('local-files', fileList);
    } catch (e) {
      event.reply('local-files', [{ name: '❌ 访问失败：' + e.message, type: 'file', size: 0 }]);
    }
  });
});

// 关闭所有窗口时退出（Mac除外）
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
//& "D:\electron-v28.0.0-win32-x64\electron.exe" "D:\Seliana\LinkDisk\src\main\webapp\electron-main.js"