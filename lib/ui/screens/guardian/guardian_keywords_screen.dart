/*
 * Guraba — Keyword / regex rules UI for the Guardian filter.
 */
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/core/services/guardian_service.dart';
import 'package:guraba/providers/guardian/guardian_providers.dart';

class GuardianKeywordsScreen extends ConsumerWidget {
  const GuardianKeywordsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(guardianKeywordRulesProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Keyword Rules'), leading: const BackButton()),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: const Text('Add rule'),
        onPressed: () => _showEditDialog(context, ref, null),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (rules) {
          if (rules.isEmpty) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text(
                  'No keyword rules yet. Tap "Add rule" to create one.\n\n'
                  'Use regex for advanced patterns; plain keywords match case-insensitively.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey),
                ),
              ),
            );
          }
          return ListView.separated(
            itemCount: rules.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (_, i) {
              final r = rules[i];
              return ListTile(
                leading: Icon(
                  r.isRegex ? Icons.code : Icons.text_fields,
                  color: r.enabled ? Colors.orange : Colors.grey,
                ),
                title: Text(
                  r.keyword,
                  style: TextStyle(
                    decoration: r.enabled ? null : TextDecoration.lineThrough,
                    fontFamily: r.isRegex ? 'monospace' : null,
                  ),
                ),
                subtitle: Text(r.isRegex ? 'Regex' : 'Keyword'),
                trailing: Switch(
                  value: r.enabled,
                  onChanged: (v) async {
                    final newList = [...rules];
                    newList[i] = GuardianKeywordRule(
                      keyword: r.keyword, isRegex: r.isRegex, enabled: v,
                    );
                    await GuardianService.instance.setKeywordRules(newList);
                    ref.invalidate(guardianKeywordRulesProvider);
                  },
                ),
                onLongPress: () async {
                  final newList = [...rules]..removeAt(i);
                  await GuardianService.instance.setKeywordRules(newList);
                  ref.invalidate(guardianKeywordRulesProvider);
                },
                onTap: () => _showEditDialog(context, ref, r),
              );
            },
          );
        },
      ),
    );
  }

  Future<void> _showEditDialog(
    BuildContext context,
    WidgetRef ref,
    GuardianKeywordRule? existing,
  ) async {
    final controller = TextEditingController(text: existing?.keyword ?? '');
    bool isRegex = existing?.isRegex ?? false;

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          title: Text(existing == null ? 'Add rule' : 'Edit rule'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: controller,
                autofocus: true,
                decoration: const InputDecoration(labelText: 'Keyword or regex'),
              ),
              CheckboxListTile(
                value: isRegex,
                onChanged: (v) => setState(() => isRegex = v ?? false),
                title: const Text('Treat as regex'),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Save')),
          ],
        ),
      ),
    );

    if (saved != true) return;
    final txt = controller.text.trim();
    if (txt.isEmpty) return;

    final current = await GuardianService.instance.getKeywordRules();
    final updated = [...current];
    final newRule = GuardianKeywordRule(keyword: txt, isRegex: isRegex, enabled: true);
    if (existing == null) {
      updated.add(newRule);
    } else {
      final idx = updated.indexWhere((e) => e.keyword == existing.keyword);
      if (idx >= 0) updated[idx] = newRule;
    }
    await GuardianService.instance.setKeywordRules(updated);
    ref.invalidate(guardianKeywordRulesProvider);
  }
}
