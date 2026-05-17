/*
 * Guraba — Guardian block-event log UI.
 */
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/core/services/guardian_service.dart';
import 'package:guraba/providers/guardian/guardian_providers.dart';

class GuardianEventsScreen extends ConsumerWidget {
  const GuardianEventsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(guardianBlockEventsProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Block events'),
        leading: const BackButton(),
        actions: [
          IconButton(
            tooltip: 'Export CSV',
            icon: const Icon(Icons.ios_share),
            onPressed: () async {
              final path = await GuardianService.instance.exportBlockEventsCsv();
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                  content: Text(path == null ? 'Nothing to export' : 'CSV saved: $path'),
                ));
              }
            },
          ),
          IconButton(
            tooltip: 'Clear log',
            icon: const Icon(Icons.delete_outline),
            onPressed: () async {
              await GuardianService.instance.clearBlockEvents();
              ref.invalidate(guardianBlockEventsProvider);
            },
          ),
        ],
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (events) {
          if (events.isEmpty) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text(
                  'No blocks recorded yet.\nWhen the Guardian filter intercepts content, '
                  'it will appear here.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey),
                ),
              ),
            );
          }
          return ListView.separated(
            itemCount: events.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (_, i) {
              final e = events[events.length - 1 - i]; // newest first
              return ListTile(
                title: Text(e.packageName),
                subtitle: Text('${e.reason} — ${e.detail}'),
                trailing: Text(_formatTime(e.timestamp), style: const TextStyle(fontSize: 12)),
              );
            },
          );
        },
      ),
    );
  }

  String _formatTime(int ts) {
    final d = DateTime.fromMillisecondsSinceEpoch(ts);
    String two(int v) => v.toString().padLeft(2, '0');
    return '${d.year}-${two(d.month)}-${two(d.day)} ${two(d.hour)}:${two(d.minute)}';
  }
}
