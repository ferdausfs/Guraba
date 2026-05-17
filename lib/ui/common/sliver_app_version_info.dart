import 'package:flutter/material.dart';
import 'package:guraba/core/extensions/ext_num.dart';
import 'package:guraba/core/services/drift_db_service.dart';
import 'package:guraba/core/services/method_channel_service.dart';
import 'package:guraba/ui/common/styled_text.dart';
import 'package:sliver_tools/sliver_tools.dart';

class SliverAppVersionInfo extends StatelessWidget {
  const SliverAppVersionInfo({super.key});

  @override
  Widget build(BuildContext context) {
    final appVersion = MethodChannelService.instance.deviceInfo.mindfulVersion;

    return MultiSliver(
      children: [
        /// App version
        StyledText(
          appVersion,
          fontWeight: FontWeight.bold,
          fontSize: 14,
          isSubtitle: true,
          height: 1,
        ),
        2.vBox,

        /// Database version
        StyledText(
          "db-v${DriftDbService.instance.driftDb.schemaVersion}",
          fontWeight: FontWeight.bold,
          fontSize: 14,
          isSubtitle: true,
          height: 1,
        ),
      ],
    );
  }
}
