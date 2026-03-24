# disable obfuscation
-dontobfuscate

# preserve the line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# remove kotlin null checks
-processkotlinnullchecks remove
