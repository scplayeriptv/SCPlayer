# SC Player – PROGRESS

تطبيق مشغّل IPTV (Android + Android TV) · `com.sc.player` (اسم مؤقت، اتأكد إنه فاضي على Google Play).
المشغّل فقط: المستخدم هو اللي يضيف قائمته (M3U / Xtream). التطبيق مفيهوش أي قنوات.

## القرارات
- الاسم: **SC Player** · تطبيق منفصل (مش جزء من Simo Launcher).
- الواجهة: داكنة، **بالعرض (landscape)**، RTL، ستايل زجاجي بتدرّج سماوي/بنفسجي. الألوان هتتغير لاحقًا (كلها في `ui/theme/Theme.kt`).
- التصميم المعتمد (5 شاشات): إضافة قائمة، الرئيسية، المشغّل، دليل EPG، الإعدادات.
  رابط التصميم: https://claude.ai/artifact/PJDWLBiuiakRoQ34eibCny
- التقنية: Kotlin + Jetpack Compose + Navigation. المشغّل (Media3) هييجي في جزء 3.
- الربح (لاحقًا): إعلانات خفيفة + Premium.
- مفيش Gradle wrapper: البناء على GitHub Actions (`.github/workflows/build.yml`) بيطلّع APK debug كـ artifact.

## الحالة
### ✅ الجزء 1 – الأساس وإضافة القائمة (تم)
- [x] هيكل المشروع (Gradle KTS, minSdk 24, compileSdk 34) + Manifest (موبايل + TV Leanback) + أيقونة وبانر
- [x] الثيم والمكوّنات (`ui/theme`)
- [x] `M3uParser` + اختبارات وحدة (`M3uParserTest`)
- [x] `PlaylistRepository`: تحميل وتخزين القائمة (ملف محلي) + رابط Xtream
- [x] شاشة إضافة قائمة (M3U / Xtream؛ QR مؤجّل) مع رسائل أخطاء
- [x] شاشة رئيسية مؤقتة بتعرض القنوات للتأكد إن التدفق شغال
- [x] GitHub Actions workflow

⚠️ الكود لسه **ما اتبنيش**: مفيش إنترنت في بيئة الكتابة. أول خطوة: ارفعه على GitHub وشوف الـ Actions. لو فيه أخطاء بناء، صلّحها الأول.

### ⏭️ الجزء 2 – الرئيسية الحقيقية
- [ ] بانر القناة، كروت القنوات، تصنيفات، بحث، المفضلة (حسب التصميم)
- [ ] صور الشعارات (Coil)، تنقّل بالريموت (focus) للـ TV
### ⏭️ الجزء 3 – المشغّل (Media3/ExoPlayer)
### ⏭️ الجزء 4 – EPG (XMLTV)
### ⏭️ الجزء 5 – الإعدادات + تحكم أبوي + QR
### ⏭️ الجزء 6 – الإعلانات + Premium + تجهيز Google Play
