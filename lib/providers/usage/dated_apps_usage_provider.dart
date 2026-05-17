import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:guraba/core/services/drift_db_service.dart';
import 'package:guraba/core/utils/date_time_utils.dart';
import 'package:guraba/models/usage_model.dart';
import 'package:guraba/providers/usage/todays_apps_usage_provider.dart';

/// A future provider that manages a map of Package and its usage for the selected day.
final appsUsageProvider =
    FutureProvider.family<Map<String, UsageModel>, DateTime>(
  (ref, selectedDay) => selectedDay == dateToday
      ? Future.value(ref.watch(todaysAppsUsageProvider).value ?? {})
      : DriftDbService.instance.driftDb.dynamicRecordsDao.fetchDatedAppsUsage(
          selectedDay: selectedDay,
        ),
);
