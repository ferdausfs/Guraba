// lib/ui/screens/guardian/events/tab_guardian_events.dart
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mindful/core/extensions/ext_widget.dart';
import 'package:mindful/core/services/guardian_service.dart';
import 'package:mindful/providers/guardian/guardian_provider.dart';
import 'package:mindful/ui/common/content_section_header.dart';
import 'package:mindful/ui/common/sliver_tabs_bottom_padding.dart';
import 'package:mindful/ui/common/styled_text.dart';

class TabGuardianEvents extends ConsumerWidget {
  const TabGuardianEvents({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final events = ref.watch(guardianProvider.select((s) => s.recentEvents));
    final notifier = ref.read(guardianProvider.notifier);
    final theme = Theme.of(context);

    return CustomScrollView(
      physics: const BouncingScrollPhysics(),
      slivers: [
        const StyledText('Recent content blocks by Guardian Shield.').sliver,

        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                Expanded(
                  child: ContentSectionHeader(title: 'Block History (${events.length})'),
                ),
                if (events.isNotEmpty)
                  TextButton.icon(
                    icon: const Icon(FluentIcons.delete_20_regular),
                    label: const Text('Clear'),
                    onPressed: () async {
                      await notifier.clearEvents();
                    },
                  ),
                IconButton(
                  icon: const Icon(FluentIcons.arrow_sync_20_regular),
                  tooltip: 'Refresh',
                  onPressed: () => notifier.refreshEvents(),
                ),
              ],
            ),
          ),
        ),

        if (events.isEmpty)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(48),
              child: Column(
                children: [
                  Icon(FluentIcons.shield_checkmark_20_regular,
                      size: 56, color: theme.colorScheme.primary),
                  const SizedBox(height: 16),
                  Text('All clear!', style: theme.textTheme.headlineSmall),
                  const SizedBox(height: 4),
                  Text('No blocks recorded yet.',
                      style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.outline)),
                ],
              ),
            ),
          )
        else
          SliverList(
            delegate: SliverChildBuilderDelegate(
              (_, i) => _EventTile(event: events[i]),
              childCount: events.length,
            ),
          ),

        const SliverTabsBottomPadding(),
      ],
    );
  }
}

class _EventTile extends StatelessWidget {
  const _EventTile({required this.event});
  final GuardianBlockEvent event;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final icon = _iconFor(event.reason);
    final color = _colorFor(event.reason, theme);

    return ListTile(
      leading: CircleAvatar(
        backgroundColor: color.withOpacity(0.15),
        child: Icon(icon, color: color, size: 20),
      ),
      title: Text(
        event.packageName.split('.').last,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_reasonLabel(event.reason),
              style: TextStyle(color: color, fontWeight: FontWeight.w600)),
          if (event.matchedTerm.isNotEmpty)
            Text('Match: ${event.matchedTerm}',
                style: theme.textTheme.bodySmall),
        ],
      ),
      trailing: Text(
        _formatTime(event.timestamp),
        style: theme.textTheme.bodySmall,
      ),
      isThreeLine: event.matchedTerm.isNotEmpty,
    );
  }

  IconData _iconFor(String reason) => switch (reason) {
    'KEYWORD_MATCH' => FluentIcons.text_bullet_list_square_shield_20_regular,
    'AI_DETECTION' => FluentIcons.brain_circuit_20_regular,
    'SCHEDULE_BLOCKED' => FluentIcons.timer_20_regular,
    _ => FluentIcons.prohibited_20_regular,
  };

  Color _colorFor(String reason, ThemeData theme) => switch (reason) {
    'KEYWORD_MATCH' => Colors.orange,
    'AI_DETECTION' => theme.colorScheme.error,
    'SCHEDULE_BLOCKED' => Colors.blue,
    _ => theme.colorScheme.primary,
  };

  String _reasonLabel(String reason) => switch (reason) {
    'APP_BLOCKED' => '🚫 App Blocked',
    'KEYWORD_MATCH' => '📝 Keyword Match',
    'AI_DETECTION' => '🤖 AI Detection',
    'SCHEDULE_BLOCKED' => '⏱ Schedule Block',
    'MANUAL' => '✋ Manual Block',
    _ => reason,
  };

  String _formatTime(DateTime dt) {
    final now = DateTime.now();
    final diff = now.difference(dt);
    if (diff.inMinutes < 1) return 'just now';
    if (diff.inHours < 1) return '${diff.inMinutes}m ago';
    if (diff.inDays < 1) return '${diff.inHours}h ago';
    return '${diff.inDays}d ago';
  }
}
