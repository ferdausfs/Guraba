import 'package:flutter/material.dart';

@immutable
class DeviceInfoModel {
  final String manufacturer;
  final String model;
  final String androidVersion;
  final int sdkVersion;
  final String mindfulVersion;

  const DeviceInfoModel({
    required this.manufacturer,
    required this.model,
    required this.androidVersion,
    required this.sdkVersion,
    required this.mindfulVersion,
  });

  /// Alias for [mindfulVersion] after app rename to Guraba
  String get gurabaVersion => mindfulVersion;

  factory DeviceInfoModel.fromMap(Map<String, dynamic> map) {
    return DeviceInfoModel(
      manufacturer: map['manufacturer'] ?? '',
      model: map['model'] ?? '',
      androidVersion: map['androidVersion'] ?? '',
      sdkVersion: map['sdkVersion'] ?? 24,
      mindfulVersion: map['mindfulVersion'] ?? map['gurabaVersion'] ?? '',
    );
  }
}
