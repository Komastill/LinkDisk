import 'dart:async';
import 'dart:collection';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:file_picker/file_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'transfer_task.dart';
import 'transfer_manager.dart';
import 'tcp_server.dart';
import 'tcp_client.dart';
import 'udp_listener.dart';
import 'device_page.dart';
import 'transferring_page.dart';
import 'setting_page.dart';
import 'side_navigation.dart';
import 'transfer_file_item.dart';

class MainFrame extends StatefulWidget {
  const MainFrame({super.key});

  @override
  State<MainFrame> createState() => _MainFrameState();
}

class _MainFrameState extends State<MainFrame> {
  final GlobalKey<NavigatorState> _navKey = GlobalKey();
  String _activePage = "device";

  final TransferManager _transferManager = TransferManager.instance;
  final Set<String> _connectedDevices = {};
  final Map<String, String> _deviceNameMap = {};
  final Map<String, String> _devicePlatformMap = {};
  final Map<String, int> _receiveRowIndexMap = {};
  final Map<int, String> _transferRowKeyMap = {};
  final Set<String> _cancelledTransferKeys = {};
  final LinkedHashMap<String, File> _selectedRootMap = LinkedHashMap();

  List<TransferFileItem>? _selectedItems;

  final Map<String, Widget> _pages = {};

  @override
  void initState() {
    super.initState();
    _initPages();
    _startTcpServer();
    _startUdpDiscovery();
  }

  void _initPages() {
    _pages["device"] = DevicePage(
      displayProvider: _getDeviceDisplayText,
    );
    _pages["transfer"] = const TransferringPage();
    _pages["settings"] = const SettingPage();
  }

  String _displayText(String text) {
    return text.replaceAll(".", "\u2024");
  }

  String _displayIp(String ip) => _displayText(ip);

  String _formatFileSize(int size) {
    if (size < 1000) return '$size B';
    double v = size / 1000;
    if (v < 1000) return '${v.toStringAsFixed(2)} KB';
    v /= 1000;
    if (v < 1000) return '${v.toStringAsFixed(2)} MB';
    v /= 1000;
    return '${v.toStringAsFixed(2)} GB';
  }

  String _getDeviceDisplayText(String ip) {
    final deviceName = _deviceNameMap[ip] ?? "未知设备";
    final platform = _devicePlatformMap[ip] ?? "未知平台";
    final connected = _connectedDevices.contains(ip) ? "已连接" : "未连接";
    final trusted = AuthManager().isTrusted(ip) ? "已信任" : "未信任";
    return "$deviceName [${_displayIp(ip)}] $platform $connected $trusted";
  }

  void _showDeviceStatus(String msg) {
    (_pages["device"] as DevicePage).setStatusMessage(msg);
    (_pages["settings"] as SettingPage).setStatusMessage(msg);
  }

  void _showTransferStatus(String msg) {
    (_pages["transfer"] as TransferringPage).setStatusMessage(msg);
    (_pages["settings"] as SettingPage).setStatusMessage(msg);
  }

  void _showSettingsStatus(String msg) {
    (_pages["settings"] as SettingPage).setStatusMessage(msg);
  }

  int _addTransferRow(String dir, String name, String ip, int size, String status, int progress) {
    return (_pages["transfer"] as TransferringPage).addTransferRow(
      dir,
      _displayText(name),
      _displayIp(ip),
      _displayText(_formatFileSize(size)),
      status,
      progress,
    );
  }

  void _updateTransferRow(int idx, String status, int progress) {
    (_pages["transfer"] as TransferringPage).updateTransferRow(idx, status, progress);
  }

  Future<void> _startTcpServer() async {
    TcpServer.startServer((ip) {
      bool allow = false;
      WidgetsBinding.instance.addPostFrameCallback((_) async {
        await showDialog(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text("连接请求"),
            content: Text("设备 ${_displayIp(ip)} 请求连接，是否允许？"),
            actions: [
              TextButton(
                onPressed: () {
                  allow = false;
                  Navigator.pop(ctx);
                },
                child: const Text("拒绝"),
              ),
              TextButton(
                onPressed: () {
                  allow = true;
                  Navigator.pop(ctx);
                },
                child: const Text("允许"),
              ),
            ],
          ),
        );
        setState(() {});
      });
      return allow;
    }, (clientIp, fileName, savePath, fileSize) {
      final idx = _addTransferRow("接收", fileName, clientIp, fileSize.toInt(), "接收中", 0);
      _receiveRowIndexMap["$clientIp|$savePath"] = idx;
      _showTransferStatus("开始接收：$fileName");
    }, (clientIp, fileName, savePath, fileSize, received, progress) {
      final idx = _receiveRowIndexMap["$clientIp|$savePath"];
      if (idx != null) _updateTransferRow(idx, "接收中", progress);
    }, (clientIp, fileName, savePath, fileSize) {
      final idx = _receiveRowIndexMap["$clientIp|$savePath"];
      if (idx != null) _updateTransferRow(idx, "完成", 100);
      _showTransferStatus("接收完成：$fileName");
    });
  }

  void _startUdpDiscovery() {
    UdpListener.startListening((ip, name, platform) {
      setState(() {
        _deviceNameMap[ip] = name;
        _devicePlatformMap[ip] = platform;
        final devicePage = _pages["device"] as DevicePage;
        if (!devicePage.deviceList.contains(ip)) {
          devicePage.deviceList.add(ip);
        }
      });
      _showDeviceStatus("发现设备：$name [$ip] $platform");
    });
  }

  @override
  void dispose() {
    UdpListener.stopListening();
    TcpServer.stopServer();
    _transferManager.shutdown();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          SideNavigation(
            active: _activePage,
            onDevice: () => setState(() => _activePage = "device"),
            onTransfer: () => setState(() => _activePage = "transfer"),
            onSettings: () => setState(() => _activePage = "settings"),
          ),
          Expanded(child: _pages[_activePage]!),
        ],
      ),
    );
  }
}