/*
 *
 *  * Copyright (c) 2024 Guraba (https://github.com/akaMrNagar/Guraba)
 *  * Author : Pawan Nagar (https://github.com/akaMrNagar)
 *  *
 *  * This source code is licensed under the GPL-2.0 license license found in the
 *  * LICENSE file in the root directory of this source tree.
 *
 */

import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/config/navigation/app_routes.dart';
import 'package:guraba/config/app_themes.dart';
import 'package:guraba/config/navigation/app_routes_observer.dart';
import 'package:guraba/config/navigation/navigation_service.dart';
import 'package:guraba/l10n/generated/app_localizations.dart';
import 'package:guraba/providers/system/guraba_settings_provider.dart';

class GurabaApp extends ConsumerWidget {
  const GurabaApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode =
        ref.watch(gurabaSettingsProvider.select((v) => v.themeMode));

    final accentColor =
        ref.watch(gurabaSettingsProvider.select((v) => v.accentColor));

    final localeCode =
        ref.watch(gurabaSettingsProvider.select((v) => v.localeCode));

    final useAmoledDark =
        ref.watch(gurabaSettingsProvider.select((v) => v.useAmoledDark));

    final useDynamicColors =
        ref.watch(gurabaSettingsProvider.select((v) => v.useDynamicColors));

    return DynamicColorBuilder(
      builder: (light, dark) {
        /// Apply transparent color to system ui background
        WidgetsBinding.instance.addPostFrameCallback(
          (timeStamp) => SystemChrome.setSystemUIOverlayStyle(
            const SystemUiOverlayStyle(
              systemNavigationBarContrastEnforced: true,
              systemNavigationBarDividerColor: Colors.transparent,
              systemNavigationBarColor: Colors.transparent,
            ),
          ),
        );

        return MaterialApp(
          debugShowCheckedModeBanner: false,

          /// Themes
          themeAnimationCurve: Curves.ease,
          themeMode: ThemeMode.values[themeMode.index],
          darkTheme: AppTheme.darkTheme(
            isAmoled: useAmoledDark,
            seedColor: useDynamicColors
                ? dark?.primary
                : AppTheme.materialColors[accentColor],
          ),
          theme: AppTheme.lightTheme(
            seedColor: useDynamicColors
                ? light?.primary
                : AppTheme.materialColors[accentColor],
          ),

          /// Localization
          locale: Locale(localeCode),
          supportedLocales: AppLocalizations.supportedLocales,
          localizationsDelegates: const [
            AppLocalizations.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],

          /// Navigation
          initialRoute: AppRoutes.rootSplashPath,
          routes: AppRoutes.routes,
          navigatorKey: NavigationService.navigatorKey,
          navigatorObservers: [AppRoutesObserver.instance],
        );
      },
    );
  }
}
