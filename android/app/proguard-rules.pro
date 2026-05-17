-keep class androidx.lifecycle.DefaultLifecycleObserver
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# TensorFlow Lite / GPU delegate
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.lite.**