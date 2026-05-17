/*
 * Guraba — PIN setup/verification screen for the Guardian module.
 */
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/core/services/guardian_service.dart';
import 'package:guraba/providers/guardian/guardian_providers.dart';

class GuardianPinScreen extends ConsumerStatefulWidget {
  const GuardianPinScreen({super.key});
  @override
  ConsumerState<GuardianPinScreen> createState() => _GuardianPinScreenState();
}

class _GuardianPinScreenState extends ConsumerState<GuardianPinScreen> {
  final _ctrl = TextEditingController();
  String _msg = '';

  @override
  Widget build(BuildContext context) {
    final pinSetAsync = ref.watch(guardianPinSetProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('PIN Protection'), leading: const BackButton()),
      body: pinSetAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (isSet) => Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                isSet
                    ? 'PIN is currently SET. Verify or clear below.'
                    : 'No PIN is set. Choose a 4–6 digit PIN to protect Guardian settings.',
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const SizedBox(height: 24),
              TextField(
                controller: _ctrl,
                obscureText: true,
                keyboardType: TextInputType.number,
                maxLength: 6,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  labelText: 'PIN (4–6 digits)',
                ),
              ),
              if (_msg.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(_msg, style: const TextStyle(color: Colors.redAccent)),
              ],
              const SizedBox(height: 16),
              if (!isSet)
                FilledButton(
                  onPressed: _setPin,
                  child: const Text('Set PIN'),
                )
              else ...[
                FilledButton(
                  onPressed: _verifyPin,
                  child: const Text('Verify PIN'),
                ),
                const SizedBox(height: 8),
                OutlinedButton(
                  onPressed: _clearPin,
                  child: const Text('Clear PIN'),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _setPin() async {
    final pin = _ctrl.text.trim();
    if (pin.length < 4 || pin.length > 6) {
      setState(() => _msg = 'PIN must be 4–6 digits');
      return;
    }
    final ok = await GuardianService.instance.setPin(pin);
    setState(() => _msg = ok ? 'PIN saved.' : 'Failed to save PIN.');
    if (ok) _ctrl.clear();
    ref.invalidate(guardianPinSetProvider);
  }

  Future<void> _verifyPin() async {
    final res = await GuardianService.instance.verifyPin(_ctrl.text.trim());
    setState(() {
      _msg = switch (res) {
        GuardianPinSuccess()      => '✅ Correct.',
        GuardianPinNotSet()       => 'No PIN is set.',
        GuardianPinWrong(:final remaining) =>
          '❌ Wrong. $remaining attempts left.',
        GuardianPinLockedOut(:final msRemaining) =>
          '⏳ Locked. Try again in ${(msRemaining / 60000).ceil()} min.',
      };
    });
    _ctrl.clear();
  }

  Future<void> _clearPin() async {
    await GuardianService.instance.clearPin();
    setState(() => _msg = 'PIN cleared.');
    _ctrl.clear();
    ref.invalidate(guardianPinSetProvider);
  }
}
