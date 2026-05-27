import 'package:flutter/material.dart';

import 'pages/device_page.dart';
import 'pages/file_transfer_page.dart';
import 'pages/transfer_setting_page.dart';
import 'widgets/sidebar.dart';

void main() {
  runApp(const LinkDiskApp());
}

class LinkDiskApp extends StatelessWidget {
  const LinkDiskApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LinkDisk',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        fontFamily: 'Arial',
      ),
      home: const LinkDiskHomePage(),
    );
  }
}

class LinkDiskHomePage extends StatefulWidget {
  const LinkDiskHomePage({super.key});

  @override
  State<LinkDiskHomePage> createState() => _LinkDiskHomePageState();
}

class _LinkDiskHomePageState extends State<LinkDiskHomePage> {
  int currentIndex = 0;

  Widget get currentPage {
    switch (currentIndex) {
      case 0:
        return const DevicePage();
      case 1:
        return const TransferSettingPage();
      case 2:
        return const FileTransferPage();
      default:
        return const DevicePage();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          Sidebar(
            currentIndex: currentIndex,
            onChanged: (index) {
              setState(() {
                currentIndex = index;
              });
            },
          ),
          Expanded(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 180),
              child: KeyedSubtree(
                key: ValueKey(currentIndex),
                child: currentPage,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
