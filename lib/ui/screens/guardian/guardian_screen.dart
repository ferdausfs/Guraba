// lib/ui/screens/guardian/guardian_screen.dart
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mindful/core/extensions/ext_build_context.dart';
import 'package:mindful/core/extensions/ext_widget.dart';
import 'package:mindful/providers/guardian/guardian_provider.dart';
import 'package:mindful/ui/common/content_section_header.dart';
import 'package:mindful/ui/common/default_list_tile.dart';
import 'package:mindful/ui/common/scaffold_shell.dart';
import 'package:mindful/ui/common/sliver_tabs_bottom_padding.dart';
import 'package:mindful/ui/common/styled_text.dart';
import 'package:mindful/ui/screens/guardian/apps/tab_guardian_apps.dart';
import 'package:mindful/ui/screens/guardian/events/tab_guardian_events.dart';
import 'package:mindful/ui/screens/guardian/keywords/tab_guardian_keywords.dart';
import 'package:mindful/ui/screens/guardian/settings/tab_guardian_settings.dart';

class GuardianScreen extends ConsumerWidget {
  const GuardianScreen({super.key, this.initialTabIndex});

  final int? initialTabIndex;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final guardian = ref.watch(guardianProvider);

    return ScaffoldShell(
      initialTab: initialTabIndex ?? 0,
      items: [
        // ── Tab 1: Overview / Settings ──────────────────────────────────────
        NavbarItem(
          icon: FluentIcons.shield_20_regular,
          filledIcon: FluentIcons.shield_20_filled,
          titleText: 'Guardian',
          sliverBody: const TabGuardianSettings(),
        ),

        // ── Tab 2: Blocked Apps ─────────────────────────────────────────────
        NavbarItem(
          icon: FluentIcons.prohibited_20_regular,
          filledIcon: FluentIcons.prohibited_20_filled,
          titleText: 'Apps',
          sliverBody: const TabGuardianApps(),
        ),

        // ── Tab 3: Keywords ─────────────────────────────────────────────────
        NavbarItem(
          icon: FluentIcons.text_bullet_list_square_shield_20_regular,
          filledIcon: FluentIcons.text_bullet_list_square_shield_20_filled,
          titleText: 'Keywords',
          sliverBody: const TabGuardianKeywords(),
        ),

        // ── Tab 4: Block Events log ─────────────────────────────────────────
        NavbarItem(
          icon: FluentIcons.history_20_regular,
          filledIcon: FluentIcons.history_20_filled,
          titleText: 'Events',
          sliverBody: const TabGuardianEvents(),
        ),
      ],
    );
  }
}
