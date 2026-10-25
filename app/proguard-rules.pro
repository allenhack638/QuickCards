-keepattributes Signature
-keepattributes *Annotation*

# Room entities: column names come from field names, so keep them stable.
-keep @androidx.room.Entity class * { *; }

# The export envelope is the only class Gson (de)serializes by reflection;
# backup content is built with JsonObject and settings travel as a Map.
-keep class com.quickcards.app.core.security.ExportCrypto$Envelope { *; }
