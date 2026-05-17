/*
 *
 *  * Copyright (c) 2024 Guraba (https://github.com/akaMrNagar/Guraba)
 *  * Author : Pawan Nagar (https://github.com/akaMrNagar)
 *  *
 *  * This source code is licensed under the GPL-2.0 license license found in the
 *  * LICENSE file in the root directory of this source tree.
 *
 */

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/providers/notifications/notification_settings_provider.dart';
import 'package:guraba/ui/common/sliver_distracting_apps_list.dart';

class SliverBatchedAppsList extends ConsumerWidget {
  const SliverBatchedAppsList({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final batchedApps =
        ref.watch(notificationSettingsProvider.select((v) => v.batchedApps));

    return SliverDistractingAppsList(
      distractingApps: batchedApps,
      onSelectionChanged: (package, isSelected) => ref
          .read(notificationSettingsProvider.notifier)
          .batchUnBatchApp(package, isSelected),
    );
  }
}
