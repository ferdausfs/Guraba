// lib/ui/screens/guardian/keywords/tab_guardian_keywords.dart
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mindful/core/extensions/ext_widget.dart';
import 'package:mindful/providers/guardian/guardian_provider.dart';
import 'package:mindful/ui/common/content_section_header.dart';
import 'package:mindful/ui/common/sliver_tabs_bottom_padding.dart';
import 'package:mindful/ui/common/styled_text.dart';

class TabGuardianKeywords extends ConsumerStatefulWidget {
  const TabGuardianKeywords({super.key});

  @override
  ConsumerState<TabGuardianKeywords> createState() => _TabGuardianKeywordsState();
}

class _TabGuardianKeywordsState extends ConsumerState<TabGuardianKeywords> {
  final _ctrl = TextEditingController();
  bool _isRegex = false;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _add() {
    final kw = _ctrl.text.trim();
    if (kw.isEmpty) return;
    ref.read(guardianProvider.notifier).addKeyword(kw, isRegex: _isRegex);
    _ctrl.clear();
    setState(() => _isRegex = false);
  }

  @override
  Widget build(BuildContext context) {
    final keywords = ref.watch(guardianProvider.select((s) => s.keywords));
    final theme = Theme.of(context);

    return CustomScrollView(
      physics: const BouncingScrollPhysics(),
      slivers: [
        const StyledText(
          'Add words or phrases to block. When detected on screen, the app will be closed immediately.',
        ).sliver,

        ContentSectionHeader(title: 'Add Keyword').sliver,

        // Input
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _ctrl,
                    decoration: InputDecoration(
                      hintText: _isRegex ? 'Enter regex pattern…' : 'Enter keyword…',
                      border: const OutlineInputBorder(),
                      isDense: true,
                      suffixIcon: IconButton(
                        icon: const Icon(FluentIcons.dismiss_20_regular),
                        onPressed: () => _ctrl.clear(),
                      ),
                    ),
                    onSubmitted: (_) => _add(),
                  ),
                ),
                const SizedBox(width: 8),
                FilledButton.icon(
                  onPressed: _add,
                  icon: const Icon(FluentIcons.add_20_regular),
                  label: const Text('Add'),
                ),
              ],
            ),
          ),
        ),

        // Regex toggle
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                Switch(value: _isRegex, onChanged: (v) => setState(() => _isRegex = v)),
                const SizedBox(width: 8),
                Text('Use as Regex', style: theme.textTheme.bodyMedium),
              ],
            ),
          ),
        ),

        // List
        ContentSectionHeader(title: 'Active Keywords (${keywords.length})').sliver,

        if (keywords.isEmpty)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                children: [
                  Icon(FluentIcons.text_bullet_list_square_shield_20_regular,
                      size: 48, color: theme.colorScheme.outline),
                  const SizedBox(height: 12),
                  Text('No keywords yet', style: theme.textTheme.bodyLarge),
                  const SizedBox(height: 4),
                  Text('Add words to block above', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
          )
        else
          SliverList(
            delegate: SliverChildBuilderDelegate(
              (_, i) {
                final kw = keywords[i];
                return ListTile(
                  leading: Icon(
                    kw.isRegex
                        ? FluentIcons.code_20_regular
                        : FluentIcons.text_bullet_list_20_regular,
                    color: theme.colorScheme.primary,
                  ),
                  title: Text(kw.keyword),
                  subtitle: Text(kw.isRegex ? 'Regex pattern' : 'Exact match'),
                  trailing: IconButton(
                    icon: Icon(FluentIcons.delete_20_regular,
                        color: theme.colorScheme.error),
                    onPressed: () =>
                        ref.read(guardianProvider.notifier).removeKeyword(kw.id),
                  ),
                );
              },
              childCount: keywords.length,
            ),
          ),

        const SliverTabsBottomPadding(),
      ],
    );
  }
}
