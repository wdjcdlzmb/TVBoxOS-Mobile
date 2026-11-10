# Cling uses reflection on service/model classes
-keep class org.fourthline.cling.** { *; }
-keep class org.seamless.** { *; }
-dontwarn org.fourthline.cling.**
-dontwarn org.seamless.**
-dontwarn javax.servlet.**
-dontwarn org.eclipse.jetty.**