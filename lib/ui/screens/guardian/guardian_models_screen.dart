/*
 * Guraba — TFLite Model Manager UI.
 */
import 'package:file_picker/file_picker.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/core/services/guardian_service.dart';
import 'package:guraba/providers/guardian/guardian_providers.dart';

class GuardianModelsScreen extends ConsumerWidget {
  const GuardianModelsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(guardianModelStatusProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('AI Models'), leading: const BackButton()),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (m) => ListView(
          padding: const EdgeInsets.symmetric(vertical: 12),
          children: [
            _ModelTile(
              name: GuardianModels.legacy,
              label: 'Legacy combined NSFW classifier',
              imported: m.legacyImported,
              sizeBytes: m.legacyBytes,
            ),
            _ModelTile(
              name: GuardianModels.nsfw,
              label: 'Dedicated NSFW gate',
              imported: m.nsfwImported,
              sizeBytes: m.nsfwBytes,
            ),
            _ModelTile(
              name: GuardianModels.gender,
              label: 'Male/female classification',
              imported: m.genderImported,
              sizeBytes: m.genderBytes,
            ),
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text(
                'Models are stored privately in the app\'s filesDir. They are validated against '
                'the TFL3 header on import. The GPU delegate is used when supported, with a 2-thread '
                'CPU fallback.',
                style: TextStyle(color: Colors.grey),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ModelTile extends ConsumerWidget {
  final String name;
  final String label;
  final bool imported;
  final int sizeBytes;

  const _ModelTile({
    required this.name,
    required this.label,
    required this.imported,
    required this.sizeBytes,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ListTile(
      leading: Icon(
        imported ? FluentIcons.checkmark_circle_24_filled : FluentIcons.dismiss_circle_24_regular,
        color: imported ? Colors.green : Colors.grey,
      ),
      title: Text(label),
      subtitle: Text(
        imported
            ? '$name — ${_formatSize(sizeBytes)}'
            : '$name — not imported',
      ),
      trailing: PopupMenuButton<String>(
        onSelected: (v) async {
          if (v == 'import') {
            final res = await FilePicker.platform.pickFiles(
              type: FileType.any,
              allowMultiple: false,
              withData: false,
            );
            final path = res?.files.single.path;
            if (path == null) return;
            final uri = Uri.file(path).toString();
            final ok = await GuardianService.instance
                .importModel(uri: uri, modelName: name);
            ref.invalidate(guardianModelStatusProvider);
            if (context.mounted) {
              ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                content: Text(ok ? 'Imported $name' : 'Import failed'),
              ));
            }
          } else if (v == 'delete') {
            await GuardianService.instance.deleteModel(name);
            ref.invalidate(guardianModelStatusProvider);
          }
        },
        itemBuilder: (_) => const [
          PopupMenuItem(value: 'import', child: Text('Import .tflite…')),
          PopupMenuItem(value: 'delete', child: Text('Delete')),
        ],
      ),
    );
  }

  static String _formatSize(int b) {
    if (b < 1024) return '$b B';
    if (b < 1024 * 1024) return '${(b / 1024).toStringAsFixed(1)} KB';
    if (b < 1024 * 1024 * 1024) return '${(b / 1024 / 1024).toStringAsFixed(1)} MB';
    return '${(b / 1024 / 1024 / 1024).toStringAsFixed(2)} GB';
  }
}
