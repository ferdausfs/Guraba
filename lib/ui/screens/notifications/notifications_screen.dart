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
import 'package:guraba/core/extensions/ext_build_context.dart';
import 'package:guraba/ui/common/scaffold_shell.dart';
import 'package:guraba/ui/screens/notifications/notifications_timeline/search_notification_button.dart';
import 'package:guraba/ui/screens/notifications/notifications_timeline/tab_notifications_timeline.dart';
import 'package:guraba/ui/screens/notifications/todays_notifications/tab_todays_notifications.dart';

class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({
    super.key,
    this.initialTabIndex,
  });

  final int? initialTabIndex;

  @override
  Widget build(BuildContext context) {
    return ScaffoldShell(
      items: [
        NavbarItem(
          icon: FluentIcons.alert_badge_20_regular,
          filledIcon: FluentIcons.alert_badge_20_filled,
          titleText: context.locale.notifications_tab_title,
          sliverBody: const TabTodaysNotifications(),
        ),
        NavbarItem(
          icon: FluentIcons.history_20_regular,
          filledIcon: FluentIcons.history_20_filled,
          titleText: context.locale.timeline_tab_title,
          actions: const [SearchNotificationButton()],
          sliverBody: const TabNotificationTimeline(),
        ),
      ],
    );
  }
}
