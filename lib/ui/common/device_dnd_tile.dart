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
import 'package:guraba/core/enums/item_position.dart';
import 'package:guraba/core/extensions/ext_build_context.dart';
import 'package:guraba/core/services/method_channel_service.dart';
import 'package:guraba/ui/common/default_list_tile.dart';

class DeviceDndTile extends StatelessWidget {
  const DeviceDndTile({super.key, this.position});

  final ItemPosition? position;

  @override
  Widget build(BuildContext context) {
    return DefaultListTile(
      position: position,
      leading: const Icon(FluentIcons.alert_off_20_regular),
      titleText: context.locale.dnd_settings_tile_title,
      subtitleText: context.locale.dnd_settings_tile_subtitle,
      trailing: const Icon(FluentIcons.chevron_right_20_filled),
      onPressed: () => MethodChannelService.instance.openDeviceDndSettings(),
    );
  }
}
