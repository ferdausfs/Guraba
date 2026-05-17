/*
 *
 *  * Copyright (c) 2024 Guraba (https://github.com/akaMrNagar/Guraba)
 *  * Author : Pawan Nagar (https://github.com/akaMrNagar)
 *  *
 *  * This source code is licensed under the GPL-2.0 license license found in the
 *  * LICENSE file in the root directory of this source tree.
 *
 */

import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/config/app_constants.dart';
import 'package:guraba/core/enums/item_position.dart';
import 'package:guraba/core/extensions/ext_build_context.dart';
import 'package:guraba/core/extensions/ext_num.dart';
import 'package:guraba/core/extensions/ext_widget.dart';
import 'package:guraba/core/services/method_channel_service.dart';
import 'package:guraba/providers/system/guraba_settings_provider.dart';
import 'package:guraba/ui/common/content_section_header.dart';
import 'package:guraba/ui/common/default_list_tile.dart';
import 'package:guraba/ui/common/scaffold_shell.dart';
import 'package:guraba/ui/common/sliver_app_version_info.dart';
import 'package:guraba/ui/common/sliver_tabs_bottom_padding.dart';
import 'package:guraba/ui/screens/change_logs/data/change_logs_data.dart';
import 'package:guraba/ui/screens/change_logs/widgets/change_log_card.dart';
import 'package:guraba/ui/transitions/default_effects.dart';

class ChangeLogsScreen extends ConsumerStatefulWidget {
  const ChangeLogsScreen({super.key});

  @override
  ConsumerState<ConsumerStatefulWidget> createState() =>
      _ChangeLogsScreenState();
}

class _ChangeLogsScreenState extends ConsumerState<ChangeLogsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => ref.read(gurabaSettingsProvider.notifier).updateAppVersion(),
    );
  }

  @override
  Widget build(BuildContext context) {
    /// Includes version strings and changelogs
    final logs = ChangeLogsData().getFormattedChangeLogs();

    return ScaffoldShell(
      items: [
        NavbarItem(
          icon: FluentIcons.slide_text_20_regular,
          filledIcon: FluentIcons.slide_text_20_filled,
          titleText: context.locale.changelog_tile_title,
          sliverBody: CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              /// Version
              const SliverAppVersionInfo(),

              24.vSliverBox,

              /// Change log
              DefaultListTile(
                isPrimary: true,
                leadingIcon: FluentIcons.slide_text_20_regular,
                titleText: context.locale.full_changelog_tile_title,
                subtitleText: context.locale.redirected_to_github_subtitle,
                trailing: const Icon(FluentIcons.chevron_right_20_regular),
                onPressed: () => MethodChannelService.instance.launchUrl(
                  AppConstants.githubChangeLogUrl(
                      MethodChannelService.instance.deviceInfo.gurabaVersion),
                ),
              ).sliver,
              12.vSliverBox,

              /// Change logs
              SliverList.separated(
                itemCount: logs.length,
                separatorBuilder: (context, index) => logs[index] is String
                    ? ContentSectionHeader(title: logs[index]).animateOnce(
                        ref: ref,
                        uniqueKey: "changeLog.versions.$index",
                        effects: DefaultEffects.transitionIn,
                        delay: (100 * index).ms,
                      )
                    : 0.vBox,
                itemBuilder: (context, index) {
                  final item = logs[index];
                  final position = index == 0
                      ? ItemPosition.top
                      : index == (logs.length - 1)
                          ? ItemPosition.bottom
                          : _resolvePositionWithIndex(index, logs);

                  return item is ChangeLog
                      ? ChangeLogCard(
                          position: position,
                          changeLog: item,
                        ).animateOnce(
                          ref: ref,
                          uniqueKey: "changeLog.logs.$index",
                          effects: DefaultEffects.transitionIn,
                          delay: (100 * index).ms,
                        )
                      : 0.vBox;
                },
              ),

              /// Padding
              const SliverTabsBottomPadding(),
            ],
          ),
        )
      ],
    );
  }

  ItemPosition _resolvePositionWithIndex(
    int currentIndex,
    List<dynamic> logs,
  ) =>
      logs[currentIndex - 1] is String
          ? ItemPosition.top
          : logs[currentIndex + 1] is String
              ? ItemPosition.bottom
              : ItemPosition.mid;
}
