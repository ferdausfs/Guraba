/*
 * Guraba — Guardian Filter screen.
 *
 * Surfaces the merged Guardian-Shield features inside the Mindful UI:
 *   • Enable on-device AI NSFW filtering
 *   • User-gender preference (powers opposite-gender NSFW gate)
 *   • Detection thresholds (legacy, NSFW gate, gender, grid votes)
 *   • TFLite model import status
 *   • Recent block events
 *   • Quick link to PIN protection
 */
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/core/services/guardian_service.dart';
import 'package:guraba/providers/guardian/guardian_providers.dart';
import 'package:guraba/ui/screens/guardian/guardian_keywords_screen.dart';
import 'package:guraba/ui/screens/guardian/guardian_models_screen.dart';
import 'package:guraba/ui/screens/guardian/guardian_pin_screen.dart';
import 'package:guraba/ui/screens/guardian/guardian_events_screen.dart';

class GuardianFilterScreen extends ConsumerWidget {
  const GuardianFilterScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(guardianAiSettingsProvider);
    final models = ref.watch(guardianModelStatusProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Guardian Filter'),
        leading: const BackButton(),
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(guardianAiSettingsProvider);
          ref.invalidate(guardianModelStatusProvider);
        },
        child: settings.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => _ErrorBox(message: '$e'),
          data: (s) => ListView(
            padding: const EdgeInsets.symmetric(vertical: 12),
            children: [
              _SectionHeader('On-device AI filter'),
              SwitchListTile(
                secondary: const Icon(FluentIcons.shield_24_regular),
                title: const Text('Enable AI content filter'),
                subtitle: const Text(
                    'On-device NSFW detection using TFLite. No data leaves your phone.'),
                value: s.aiEnabled,
                onChanged: (v) async {
                  await GuardianService.instance.setAiEnabled(v);
                  ref.invalidate(guardianAiSettingsProvider);
                },
              ),
              ListTile(
                leading: const Icon(FluentIcons.person_24_regular),
                title: const Text('Your gender'),
                subtitle: Text(_genderLabel(s.userGender)),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _pickGender(context, ref, s.userGender),
              ),
              const Divider(height: 32),
              _SectionHeader('Detection thresholds'),
              _SliderTile(
                title: 'NSFW threshold (legacy model)',
                value: s.aiThreshold,
                onChangeEnd: (v) async {
                  await GuardianService.instance.setAiThreshold(v);
                  ref.invalidate(guardianAiSettingsProvider);
                },
              ),
              _SliderTile(
                title: 'NSFW gate threshold (dedicated model)',
                value: s.nsfwGateThreshold,
                onChangeEnd: (v) async {
                  await GuardianService.instance.setNsfwGateThreshold(v);
                  ref.invalidate(guardianAiSettingsProvider);
                },
              ),
              _SliderTile(
                title: 'Gender confidence threshold',
                value: s.genderThreshold,
                onChangeEnd: (v) async {
                  await GuardianService.instance.setGenderThreshold(v);
                  ref.invalidate(guardianAiSettingsProvider);
                },
              ),
              ListTile(
                leading: const Icon(FluentIcons.grid_24_regular),
                title: const Text('Grid vote count'),
                subtitle: Text(
                    '${s.gridVoteCount} of 6 regions must trigger to block borderline images.'),
                trailing: DropdownButton<int>(
                  value: s.gridVoteCount,
                  items: [1, 2, 3, 4, 5, 6]
                      .map((v) => DropdownMenuItem(value: v, child: Text('$v')))
                      .toList(),
                  onChanged: (v) async {
                    if (v == null) return;
                    await GuardianService.instance.setGridVoteCount(v);
                    ref.invalidate(guardianAiSettingsProvider);
                  },
                ),
              ),
              const Divider(height: 32),
              _SectionHeader('Modules'),
              models.maybeWhen(
                data: (m) => ListTile(
                  leading: const Icon(FluentIcons.brain_circuit_24_regular),
                  title: const Text('TFLite AI models'),
                  subtitle: Text(_modelSummary(m)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) => const GuardianModelsScreen())),
                ),
                orElse: () => const ListTile(
                  leading: Icon(FluentIcons.brain_circuit_24_regular),
                  title: Text('TFLite AI models'),
                  subtitle: Text('Loading…'),
                ),
              ),
              ListTile(
                leading: const Icon(FluentIcons.tag_24_regular),
                title: const Text('Keyword & regex rules'),
                subtitle: const Text('Block on-screen text patterns.'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => const GuardianKeywordsScreen())),
              ),
              ListTile(
                leading: const Icon(FluentIcons.history_24_regular),
                title: const Text('Block-event log'),
                subtitle: const Text('Recent blocks, with CSV export.'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => const GuardianEventsScreen())),
              ),
              ListTile(
                leading: const Icon(FluentIcons.lock_closed_24_regular),
                title: const Text('PIN protection'),
                subtitle: const Text('Prevent tampering with Guardian settings.'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => const GuardianPinScreen())),
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  String _genderLabel(String g) => switch (g) {
        'MALE' => 'Male — block female NSFW',
        'FEMALE' => 'Female — block male NSFW',
        _ => 'Not set — opposite-gender filter disabled'
      };

  String _modelSummary(GuardianModelStatus m) {
    final loaded = [
      if (m.legacyImported) 'legacy',
      if (m.nsfwImported) 'NSFW',
      if (m.genderImported) 'gender',
    ];
    if (loaded.isEmpty) return 'No models imported. Tap to import.';
    return 'Loaded: ${loaded.join(", ")}';
  }

  Future<void> _pickGender(BuildContext context, WidgetRef ref, String current) async {
    final result = await showModalBottomSheet<String>(
      context: context,
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.male),
              title: const Text('Male'),
              trailing: current == 'MALE' ? const Icon(Icons.check) : null,
              onTap: () => Navigator.pop(context, 'MALE'),
            ),
            ListTile(
              leading: const Icon(Icons.female),
              title: const Text('Female'),
              trailing: current == 'FEMALE' ? const Icon(Icons.check) : null,
              onTap: () => Navigator.pop(context, 'FEMALE'),
            ),
            ListTile(
              leading: const Icon(Icons.cancel_outlined),
              title: const Text('Not set / disable'),
              trailing: current == 'NONE' ? const Icon(Icons.check) : null,
              onTap: () => Navigator.pop(context, 'NONE'),
            ),
          ],
        ),
      ),
    );
    if (result != null) {
      await GuardianService.instance.setUserGender(result);
      ref.invalidate(guardianAiSettingsProvider);
    }
  }
}

class _SectionHeader extends StatelessWidget {
  final String text;
  const _SectionHeader(this.text);

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 4),
        child: Text(
          text,
          style: Theme.of(context).textTheme.titleSmall?.copyWith(
                color: Theme.of(context).colorScheme.primary,
              ),
        ),
      );
}

class _SliderTile extends StatefulWidget {
  final String title;
  final double value;
  final ValueChanged<double> onChangeEnd;
  const _SliderTile({
    required this.title,
    required this.value,
    required this.onChangeEnd,
  });

  @override
  State<_SliderTile> createState() => _SliderTileState();
}

class _SliderTileState extends State<_SliderTile> {
  late double _v = widget.value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(left: 8, top: 8),
            child: Text(widget.title),
          ),
          Row(
            children: [
              Expanded(
                child: Slider(
                  min: 0.10,
                  max: 0.95,
                  divisions: 17,
                  value: _v.clamp(0.10, 0.95),
                  onChanged: (v) => setState(() => _v = v),
                  onChangeEnd: widget.onChangeEnd,
                ),
              ),
              SizedBox(width: 52, child: Text(_v.toStringAsFixed(2))),
            ],
          ),
        ],
      ),
    );
  }
}

class _ErrorBox extends StatelessWidget {
  final String message;
  const _ErrorBox({required this.message});
  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Text('Guardian unavailable: $message'),
        ),
      );
}
