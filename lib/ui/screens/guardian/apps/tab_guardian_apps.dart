// lib/ui/screens/guardian/apps/tab_guardian_apps.dart
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mindful/core/extensions/ext_widget.dart';
import 'package:mindful/models/app_info.dart';
import 'package:mindful/providers/apps/apps_info_provider.dart';
import 'package:mindful/providers/guardian/guardian_provider.dart';
import 'package:mindful/ui/common/application_icon.dart';
import 'package:mindful/ui/common/content_section_header.dart';
import 'package:mindful/ui/common/default_list_tile.dart';
import 'package:mindful/ui/common/sliver_tabs_bottom_padding.dart';
import 'package:mindful/ui/common/styled_text.dart';

class TabGuardianApps extends ConsumerStatefulWidget {
  const TabGuardianApps({super.key});

  @override
  ConsumerState<TabGuardianApps> createState() => _TabGuardianAppsState();
}

class _TabGuardianAppsState extends ConsumerState<TabGuardianApps> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final guardian = ref.watch(guardianProvider);
    final notifier = ref.read(guardianProvider.notifier);
    final appsAsync = ref.watch(appsInfoProvider);

    return appsAsync.when(
      loading: () => const CustomScrollView(
        slivers: [SliverFillRemaining(child: Center(child: CircularProgressIndicator()))],
      ),
      error: (e, _) => CustomScrollView(
        slivers: [SliverFillRemaining(child: Center(child: Text('Error: $e')))],
      ),
      data: (appsMap) {
        final blockedPkgs = guardian.blockedApps.map((a) => a.packageName).toSet();
        final whitelistedPkgs = guardian.whitelistedApps.map((a) => a.packageName).toSet();

        final allApps = appsMap.values
            .where((a) => _query.isEmpty ||
                a.name.toLowerCase().contains(_query.toLowerCase()) ||
                a.packageName.toLowerCase().contains(_query.toLowerCase()))
            .toList()
          ..sort((a, b) => a.name.compareTo(b.name));

        final blocked = allApps.where((a) => blockedPkgs.contains(a.packageName)).toList();
        final whitelisted = allApps.where((a) => whitelistedPkgs.contains(a.packageName)).toList();
        final rest = allApps
            .where((a) =>
                !blockedPkgs.contains(a.packageName) &&
                !whitelistedPkgs.contains(a.packageName))
            .toList();

        return CustomScrollView(
          physics: const BouncingScrollPhysics(),
          slivers: [
            // Search bar
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                child: TextField(
                  decoration: const InputDecoration(
                    hintText: 'Search apps…',
                    prefixIcon: Icon(FluentIcons.search_20_regular),
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                  onChanged: (v) => setState(() => _query = v),
                ),
              ),
            ),

            if (blocked.isNotEmpty) ...[
              ContentSectionHeader(title: '🚫 Blocked (${blocked.length})').sliver,
              SliverList(
                delegate: SliverChildBuilderDelegate(
                  (_, i) => _AppTile(
                    app: blocked[i],
                    status: _AppStatus.blocked,
                    onBlock: () => notifier.unblockApp(blocked[i].packageName),
                    onWhitelist: () => notifier.whitelistApp(blocked[i].packageName, blocked[i].name),
                  ),
                  childCount: blocked.length,
                ),
              ),
            ],

            if (whitelisted.isNotEmpty) ...[
              ContentSectionHeader(title: '✅ Whitelisted (${whitelisted.length})').sliver,
              SliverList(
                delegate: SliverChildBuilderDelegate(
                  (_, i) => _AppTile(
                    app: whitelisted[i],
                    status: _AppStatus.whitelisted,
                    onBlock: () => notifier.blockApp(whitelisted[i].packageName, whitelisted[i].name),
                    onWhitelist: () => notifier.removeWhitelist(whitelisted[i].packageName),
                  ),
                  childCount: whitelisted.length,
                ),
              ),
            ],

            ContentSectionHeader(title: 'All Apps (${rest.length})').sliver,
            SliverList(
              delegate: SliverChildBuilderDelegate(
                (_, i) => _AppTile(
                  app: rest[i],
                  status: _AppStatus.normal,
                  onBlock: () => notifier.blockApp(rest[i].packageName, rest[i].name),
                  onWhitelist: () => notifier.whitelistApp(rest[i].packageName, rest[i].name),
                ),
                childCount: rest.length,
              ),
            ),

            const SliverTabsBottomPadding(),
          ],
        );
      },
    );
  }
}

enum _AppStatus { normal, blocked, whitelisted }

class _AppTile extends StatelessWidget {
  const _AppTile({
    required this.app,
    required this.status,
    required this.onBlock,
    required this.onWhitelist,
  });

  final AppInfo app;
  final _AppStatus status;
  final VoidCallback onBlock;
  final VoidCallback onWhitelist;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    Color? tileColor;
    if (status == _AppStatus.blocked) tileColor = theme.colorScheme.errorContainer.withOpacity(0.3);
    if (status == _AppStatus.whitelisted) tileColor = theme.colorScheme.primaryContainer.withOpacity(0.3);

    return Container(
      color: tileColor,
      child: ListTile(
        leading: ApplicationIcon(appInfo: app, size: 20),
        title: Text(app.name, maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Text(app.packageName, maxLines: 1, overflow: TextOverflow.ellipsis, style: theme.textTheme.bodySmall),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Block / Unblock
            IconButton(
              tooltip: status == _AppStatus.blocked ? 'Unblock' : 'Block',
              icon: Icon(
                status == _AppStatus.blocked
                    ? FluentIcons.checkmark_circle_20_regular
                    : FluentIcons.prohibited_20_regular,
                color: status == _AppStatus.blocked
                    ? theme.colorScheme.primary
                    : theme.colorScheme.error,
              ),
              onPressed: onBlock,
            ),
            // Whitelist / Remove whitelist
            IconButton(
              tooltip: status == _AppStatus.whitelisted ? 'Remove whitelist' : 'Whitelist (always allow)',
              icon: Icon(
                status == _AppStatus.whitelisted
                    ? FluentIcons.shield_dismiss_20_regular
                    : FluentIcons.shield_checkmark_20_regular,
                color: theme.colorScheme.primary,
              ),
              onPressed: onWhitelist,
            ),
          ],
        ),
      ),
    );
  }
}
