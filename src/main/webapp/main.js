/**
 * 
 */
const deviceList = document.getElementById('device-list');
const fileList = document.getElementById('file-list');
const pathBar = document.getElementById('path-bar');

const { ipcRenderer } = require('electron');

let currentPath = '';
let allDisks = [];

// 初始化左侧标题（只加1次）
function initSidebarHeader() {
  const header = document.createElement('div');
  header.className = 'device-header';
  header.innerText = '此电脑';
  deviceList.appendChild(header);
}

// 渲染磁盘列表
ipcRenderer.on('disk-list', (e, disks) => {
  allDisks = disks;
  deviceList.innerHTML = ''; // 清空所有内容
  initSidebarHeader(); // 只加1次此电脑标题

  disks.forEach(disk => {
    const item = document.createElement('div');
    item.className = 'disk-item';
    item.innerHTML = `
      <span class="disk-letter">💽</span>
      <span>${disk.letter} 盘</span>
    `;
    item.onclick = () => {
      // 清空所有激活态
      document.querySelectorAll('.disk-item').forEach(i => i.classList.remove('active'));
      item.classList.add('active');
      
      // 强制重置为磁盘根目录
      currentPath = disk.safePath;
      // 标准化路径格式（避免 D:/$RECYCLE.BIN/ 这类异常）
      if (!currentPath.endsWith('/')) currentPath += '/';
      pathBar.textContent = `当前路径：${currentPath}`;
      loadFiles(currentPath);
    };
    deviceList.appendChild(item);
  });

  // 默认加载第一个磁盘
  if (disks.length > 0) {
    currentPath = disks[0].safePath;
    if (!currentPath.endsWith('/')) currentPath += '/';
    pathBar.textContent = `当前路径：${currentPath}`;
    loadFiles(currentPath);
    document.querySelector('.disk-item')?.classList.add('active');
  }
});

// 加载文件
function loadFiles(targetPath) {
  fileList.innerHTML = '<div style="width:260px; padding:14px; color:#888;">加载中...</div>';
  ipcRenderer.send('read-local-files', targetPath);
}

// 核心：判断是否是磁盘根目录（绝对准确）
function isDiskRoot(path) {
  // 匹配 C:/ D:/ E:/ 这类纯根目录
  const rootRegex = /^[A-Z]:\/$/;
  return rootRegex.test(path) || allDisks.some(disk => disk.safePath === path);
}

// 渲染文件
ipcRenderer.on('local-files', (e, files) => {
  fileList.innerHTML = '';

  // 只有非根目录才显示返回按钮
  if (!isDiskRoot(currentPath)) {
    const back = document.createElement('div');
    back.className = 'back-btn';
    back.innerHTML = '⬅️ 返回上一级';
    back.onclick = () => {
      // 拆分路径并回退
      const pathArr = currentPath.split('/').filter(Boolean);
      if (pathArr.length <= 1) {
        // 已经到磁盘根目录，直接重置
        const currentDisk = allDisks.find(d => currentPath.startsWith(d.letter + ':/'));
        if (currentDisk) {
          currentPath = currentDisk.safePath;
          if (!currentPath.endsWith('/')) currentPath += '/';
        }
      } else {
        pathArr.pop();
        currentPath = pathArr.join('/') + '/';
      }
      
      pathBar.textContent = `当前路径：${currentPath}`;
      loadFiles(currentPath);
    };
    fileList.appendChild(back);
  }

  // 渲染文件/文件夹（过滤系统隐藏文件夹）
files.forEach(file => {
  // 过滤掉系统文件夹：$RECYCLE.BIN、System Volume Information 等
  const systemFolders = ['$RECYCLE.BIN', 'System Volume Information', 'Recycled'];
  if (file.type === 'folder' && systemFolders.includes(file.name)) {
    return; // 跳过系统文件夹，不渲染
  }

  const item = document.createElement('div');
  item.className = 'file-item';

  const icon = file.type === 'folder' ? '📁' : '📄';
  const size = file.type === 'file'
    ? (file.size > 1024 * 1024
      ? (file.size / (1024 * 1024)).toFixed(1) + ' MB'
      : (file.size / 1024).toFixed(1) + ' KB')
    : '';

  item.innerHTML = `
    <div class="file-icon">${icon}</div>
    <div class="file-info">
      <div class="file-name">${file.name}</div>
      <div class="file-size">${size}</div>
    </div>
  `;

  item.ondblclick = () => {
    if (file.type === 'folder') {
      currentPath += file.name + '/';
      pathBar.textContent = `当前路径：${currentPath}`;
      loadFiles(currentPath);
    } else {
      alert(`预览文件：\n${currentPath}${file.name}`);
    }
  };
  fileList.appendChild(item);
});
});

// 局域网广播
ipcRenderer.send('broadcast-self', { name: '我的PC' });