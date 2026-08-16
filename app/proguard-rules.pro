# Keep Room entities
-keep class com.pixeldialer.app.data.** { *; }

# Keep InCallService / CallScreeningService (telecom framework binds these by reflection-like mechanisms)
-keep class com.pixeldialer.app.telecom.** { *; }
