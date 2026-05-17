/*
 *
 *  * Copyright (c) 2024 Guraba (https://github.com/akaMrNagar/Guraba)
 *  * Author : Pawan Nagar (https://github.com/akaMrNagar)
 *  *
 *  * This source code is licensed under the GPL-2.0 license license found in the
 *  * LICENSE file in the root directory of this source tree.
 *
 */

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/core/services/drift_db_service.dart';
import 'package:guraba/core/utils/date_time_utils.dart';
import 'package:guraba/providers/focus/dated_focus_provider.dart';

final lifetimeFocusProvider = FutureProvider<Duration>(
  (ref) {
    /// Listen to todays focus changes
    ref.watch(datedFocusProvider(dateToday));

    return DriftDbService.instance.driftDb.dynamicRecordsDao
        .fetchLifetimeSessionsDuration();
  },
);
