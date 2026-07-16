# LetterLab release rules.

# Keep readable crash traces in Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: the library ships consumer rules, but pin our
# level-catalog models explicitly so R8 full mode can never strip the
# generated serializers that levels.json parsing depends on.
-keepclassmembers @kotlinx.serialization.Serializable class com.baldae.letterlab.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.baldae.letterlab.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
