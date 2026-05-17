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
import 'package:guraba/core/enums/item_position.dart';
import 'package:guraba/config/hero_tags.dart';
import 'package:guraba/providers/restrictions/wellbeing_provider.dart';
import 'package:guraba/ui/common/default_list_tile.dart';
import 'package:guraba/ui/common/default_slide_to_remove.dart';
import 'package:guraba/ui/common/rounded_container.dart';
import 'package:guraba/ui/transitions/default_hero.dart';

class WebsiteTile extends ConsumerWidget {
  const WebsiteTile({
    super.key,
    required this.websitehost,
    required this.isRemovable,
    this.position,
  });

  final String websitehost;
  final ItemPosition? position;
  final bool isRemovable;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return DefaultHero(
      tag: HeroTags.websiteTileTag(websitehost),
      child: DefaultSlideToRemove(
        key: Key(websitehost),
        position: position ?? ItemPosition.none,
        removable: isRemovable,
        onDismiss: () => ref
            .read(wellBeingProvider.notifier)
            .insertRemoveBlockedSite(websitehost, false),
        child: DefaultListTile(
          position: ItemPosition.fit,
          margin: const EdgeInsets.all(0),
          leading: RoundedContainer(
            width: 14,
            height: 14,
            color: isRemovable
                ? Theme.of(context).colorScheme.secondary
                : Theme.of(context).colorScheme.error,
          ),
          titleText: websitehost,
        ),
      ),
    );
  }
}
