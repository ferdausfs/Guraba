// lib/ui/screens/guardian/settings/tab_guardian_settings.dart
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mindful/core/extensions/ext_build_context.dart';
import 'package:mindful/core/extensions/ext_widget.dart';
import 'package:mindful/providers/guardian/guardian_provider.dart';
import 'package:mindful/ui/common/content_section_header.dart';
import 'package:mindful/ui/common/default_list_tile.dart';
import 'package:mindful/ui/common/sliver_tabs_bottom_padding.dart';
import 'package:mindful/ui/common/styled_text.dart';

class TabGuardianSettings extends ConsumerWidget {
  const TabGuardianSettings({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final g = ref.watch(guardianProvider);
    final notifier = ref.read(guardianProvider.notifier);

    return CustomScrollView(
      physics: const BouncingScrollPhysics(),
      slivers: [
        // ── Info ─────────────────────────────────────────────────────────────
        const StyledText(
          'Guardian Shield monitors apps and content in real time, blocking harmful material using keyword rules and optional AI detection.',
        ).sliver,

        // ── Main Toggle ───────────────────────────────────────────────────────
        ContentSectionHeader(title: 'Protection').sliver,

        DefaultListTile(
          leadingIcon: FluentIcons.shield_20_regular,
          titleText: 'Enable Protection',
          subtitleText: g.protectionEnabled
              ? 'Guardian is actively monitoring'
              : 'Guardian is paused',
          switchValue: g.protectionEnabled,
          onPressed: () => notifier.toggleProtection(!g.protectionEnabled),
        ).sliver,

        DefaultListTile(
          enabled: g.protectionEnabled,
          leadingIcon: FluentIcons.text_bullet_list_square_shield_20_regular,
          titleText: 'Keyword Filter',
          subtitleText: 'Block based on text keywords on screen',
          switchValue: g.keywordFilter,
          onPressed: () => notifier.toggleKeywordFilter(!g.keywordFilter),
        ).sliver,

        // ── AI Detection ──────────────────────────────────────────────────────
        ContentSectionHeader(title: 'AI Detection (TFLite)').sliver,

        DefaultListTile(
          enabled: g.protectionEnabled,
          leadingIcon: FluentIcons.brain_circuit_20_regular,
          titleText: 'AI Content Detection',
          subtitleText: 'Uses on-device TFLite model to detect NSFW images',
          switchValue: g.aiDetection,
          onPressed: () => notifier.toggleAiDetection(!g.aiDetection),
        ).sliver,

        if (g.aiDetection) ...[
          DefaultListTile(
            leadingIcon: FluentIcons.gauge_20_regular,
            titleText: 'AI Sensitivity',
            subtitleText:
                'Threshold: ${(g.aiThreshold * 100).toInt()}%  (lower = more strict)',
            trailing: _ThresholdSlider(
              value: g.aiThreshold,
              onChanged: (v) => notifier.setAiThreshold(v),
            ),
          ).sliver,
        ],

        // ── Gender filter ─────────────────────────────────────────────────────
        ContentSectionHeader(title: 'Gender Filter').sliver,

        DefaultListTile(
          leadingIcon: FluentIcons.person_20_regular,
          titleText: 'User Gender',
          subtitleText:
              'Filter opposite-gender NSFW content.\nCurrent: ${g.userGender}',
          trailing: _GenderDropdown(
            value: g.userGender,
            onChanged: (v) => notifier.setGender(v),
          ),
        ).sliver,

        // ── Temp block ────────────────────────────────────────────────────────
        ContentSectionHeader(title: 'Temporary Block').sliver,

        DefaultListTile(
          leadingIcon: FluentIcons.timer_20_regular,
          titleText: 'Block Duration',
          subtitleText:
              'How long an app is temp-blocked after repeated AI hits',
          trailing: _MinutesDropdown(
            value: g.tempBlockMins,
            onChanged: (v) => notifier.setTempBlockMins(v),
          ),
        ).sliver,

        const SliverTabsBottomPadding(),
      ],
    );
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

class _ThresholdSlider extends StatelessWidget {
  const _ThresholdSlider({required this.value, required this.onChanged});
  final double value;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 120,
      child: Slider(
        min: 0.3,
        max: 0.95,
        divisions: 13,
        value: value,
        onChanged: onChanged,
      ),
    );
  }
}

class _GenderDropdown extends StatelessWidget {
  const _GenderDropdown({required this.value, required this.onChanged});
  final String value;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButton<String>(
      value: value,
      underline: const SizedBox(),
      items: const [
        DropdownMenuItem(value: 'NONE', child: Text('None')),
        DropdownMenuItem(value: 'MALE', child: Text('Male')),
        DropdownMenuItem(value: 'FEMALE', child: Text('Female')),
      ],
      onChanged: (v) { if (v != null) onChanged(v); },
    );
  }
}

class _MinutesDropdown extends StatelessWidget {
  const _MinutesDropdown({required this.value, required this.onChanged});
  final int value;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButton<int>(
      value: value,
      underline: const SizedBox(),
      items: [5, 10, 15, 30, 60].map((m) =>
        DropdownMenuItem(value: m, child: Text('$m min'))
      ).toList(),
      onChanged: (v) { if (v != null) onChanged(v); },
    );
  }
}
