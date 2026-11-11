# SC Player – PROGRESS

تطبيق مشغّل IPTV (Android + Android TV) · `com.sc.player` (اسم مؤقت، اتأكد إنه فاضي على Google Play).
المشغّل فقط: المستخدم هو اللي يضيف قائمته (M3U / Xtream). التطبيق مفيهوش أي قنوات.

## الريبو والبناء (مهم عشان نكمل في أي وقت)
- الريبو: https://github.com/scplayeriptv/SCPlayer (Public) · الفرع `main`
- مفيش Gradle wrapper ومفيش Android Studio: البناء على **GitHub Actions** (`.github/workflows/build.yml`).
  أي Commit على `main` بيشغّل Build (~3 دقايق)، والـ APK بيتنزّل من **Actions → آخر Build → Artifacts → SCPlayer-debug-apk**.
- **طريقة التحديث (من غير Terminal ولا Codespace): ملف `update.zip`.**
  بنبعتلك `update.zip` فيه الملفات المتغيّرة بمساراتها من جذر الريبو (مثلاً `app/src/main/...`).
  ترفعه في **جذر الريبو** بالاسم ده بالظبط (ومن مهمة 4 هو بيحتوي فولدر `app/` كله + PROGRESS.md): الصفحة الرئيسية للريبو ← Add file ← Upload files ← Commit changes.
  الـ workflow بيفكّه لوحده فوق الملفات، ويمسح الـ zip، ويعمل Commit، وبعدين يبني الـ APK (Actions → آخر Build → Artifacts).
  الـ zip بيستبدل الملفات الموجودة ويضيف الجديدة؛ مبيحذفش ملفات (الحذف بيتعمل يدوي من GitHub).
  (احتياطي: أي ملف برضه يتعدّل بالرابط `https://github.com/scplayeriptv/SCPlayer/edit/main/<المسار>`.)
  ملاحظة: `build.gradle.kts` فيه اتنين: الجذر وجوه `app` — محتواهم مختلف.
- **التوقيع:** `app/debug.keystore` ثابت في الريبو (كلمة السر `android`، alias `androiddebugkey`) عشان التحديث يتثبّت فوق النسخة القديمة. مفتاح الإصدار لـ Google Play يتعمل لاحقًا ويتحفظ بعيد عن الريبو.
- بعد كل مهمة بنبعت zip بنسخة المشروع كاملة (للنسخ الاحتياطي والمتابعة في محادثة جديدة) + PROGRESS.md. الـ zip الكامل مش للرفع؛ اللي بيترفع هو `update.zip`.

## القرارات
- الاسم: **SC Player** · تطبيق منفصل (مش جزء من Simo Launcher).
- الواجهة: داكنة، RTL، ستايل زجاجي بتدرّج سماوي/بنفسجي. الألوان كلها في `ui/theme/Theme.kt`.
- التقنية: Kotlin + Jetpack Compose + Navigation + Coil (شعارات) + Media3/ExoPlayer (تشغيل).
- الربح (لاحقًا): إعلانات خفيفة + Premium.
- التصميم الأصلي (artifact) مبقاش بيتفتح، فالشاشات بتتبني من الوصف.

## الحالة
### ✅ الجزء 1 – الأساس وإضافة القائمة
- هيكل المشروع، Manifest (موبايل + TV)، الثيم، `M3uParser` + اختبارات، `PlaylistRepository`، شاشة إضافة قائمة (M3U / Xtream؛ QR مؤجّل)، GitHub Actions.
- اتصلّح: القفلان على الموبايل (Scroll متداخل في `AddPlaylistScreen`).

### ✅ الجزء 2 (جزئيًا) – الرئيسية
- شاشة Welcome to SC Player (2.8 ثانية) ← الرئيسية.
- الرئيسية: بحث، شرائح تصنيفات بالعدد، شبكة كروت بشعارات (Coil) وحرف بديل، إطار تركيز للريموت.

### ✅ الجزء 3 – المشغّل (اتكتب، لسه محتاج تجربة على جهاز)
- `ui/player/PlayerScreen.kt`: ExoPlayer على SurfaceView، ملء الشاشة + بالعرض، أزرار (رجوع / السابقة / تشغيل-إيقاف / التالية) بتختفي بعد 4 ثواني، لمسة تظهرها/تخفيها.
- ريموت التلفزيون: OK = إظهار الأزرار/إيقاف-تشغيل · فوق/PageUp = التالية · تحت/PageDown = السابقة.
- بيجرّب HLS تلقائيًا لو الرابط من غير امتداد وفشل، وفيه زر إعادة المحاولة.
- `data/PlayQueue.kt`: القنوات المعروضة (حسب التصنيف/البحث) هي قائمة التالي/السابق.

## ✅ المهمة 1 – دفعة التثبيت (اتنفّذت)
- `allowBackup="false"` · الـ parser بيقبل علامات التنصيص في اسم القناة (+ اختبار) · رسالة "اسم المستخدم أو كلمة المرور غلط" لـ Xtream · `BrandChip` سطر واحد.

## ✅ المهمة 2 – المفضلة وآخر قناة (اتكتبت)
- `data/UserPrefs.kt`: SharedPreferences (`sc_user`): المفضلة بالرابط، وآخر 12 قناة.
- الرئيسية: ☆/★ على كل كارت (أو ضغطة مطوّلة / OK مطوّل على الريموت) · شريحة "★ المفضلة" · صف "آخر ما شاهدته" بيظهر أول الشبكة (لما مفيش بحث/تصنيف).
- المفضلة والآخر بالرابط، يعني مشتركين بين القوائم.

## ✅ المهمة 3 – إدارة القوائم (اتكتبت)
- شاشة `ui/playlists/PlaylistsScreen.kt` (زرار "القوائم" في الرئيسية): فتح / تحديث (إعادة تحميل من الرابط) / حذف (بتأكيد) / "+ قائمة جديدة". بتعرض اسم السيرفر بس (من غير بيانات الدخول).
- `UserPrefs.activePlaylist()`: التطبيق بيفتح آخر قائمة استخدمتها.
- `PlaylistRepository`: `refresh(id)` و`delete(id)`، و`data/Errors.kt` فيه رسائل الأخطاء المشتركة.
- `AppNav.openHome(id)`: بيصفّر الـ back stack ويفتح الرئيسية (الرجوع منها بيقفل التطبيق).

## ✅ المهمة 4 – أداء القوائم الكبيرة (اتكتبت)
- التحميل بيتكتب مباشرة في ملف مؤقت (من غير ما القائمة كلها تتحمّل في الذاكرة) ثم بيتحلّل بالسطر وبيستبدل الملف القديم.
- `data/ChannelCache.kt`: كاش للقنوات بعد الـ parse (قائمتين بس) ← فتح الرئيسية فوري بعد أول مرة.
- `M3uParser.parse(Sequence<String>)` + اختبار 20 ألف قناة.
- الرئيسية: البحث بيستنى 250ms والفلترة في الخلفية (Dispatchers.Default).
- ملاحظة: `update.zip` من هنا بيحتوي على فولدر `app/` كله (عشان مفيش اعتماد على ترتيب الرفع). مبنحطش `.github` فيه أبدًا (التوكن مبيسمحش بتعديل workflows).

## ✅ المهمة 5 – دليل البرامج EPG (اتكتبت)
- `data/epg/`: `EpgParser` (XMLTV بالتدفّق، بيحتفظ بنافذة من -3 ساعات لـ +36 ساعة)، `XmltvTime` (+ اختبارات)، `EpgRepository` (تحميل + كاش `epg_<id>.tsv` + تحديث كل 6 ساعات)، `EpgStore` (الدليل في الذاكرة).
- مصدر الدليل: Xtream ← `http://host:port/xmltv.php?username=..&password=..` تلقائي · M3U ← `url-tvg` / `x-tvg-url` في أول سطر. (بيدعم .gz).
- مطابقة القنوات: بـ `tvg-id` الأول، وبعدها باسم القناة (display-name).
- الرئيسية: "الآن: ..." على الكارت + زرار "الدليل". شاشة `ui/guide/GuideScreen.kt`: القنوات المعروضة مع البرنامج الحالي (وشريط تقدّم) والجاي. المشغّل بيعرض الآن/بعده.
- ⚠️ لسه مجرّبتش على مزوّد حقيقي؛ ملفات XMLTV الكبيرة (50–100MB) ممكن تاخد دقيقة في أول تحميل (في الخلفية).
- ⏭️ مؤجّل: رابط EPG يدوي في الإعدادات (المهمة 6) · جدول زمني كامل (grid) بدل القايمة.

## ⏳ ملاحظات مؤجّلة
- المشغّل محتاج تجربة على قنوات حقيقية (HLS / TS) وعلى تلفزيون بالريموت.

## ⏭️ خطة المهام (كل مهمة بتخلص بـ zip + md جديدين)
1. ✅ دفعة التثبيت
2. ✅ المفضلة وآخر قناة
3. ✅ إدارة القوائم
4. ✅ أداء القوائم الكبيرة
5. ✅ EPG (XMLTV)
6. ⬜ الإعدادات + تحكم أبوي + QR
7. ⬜ الإعلانات + Premium + تجهيز Google Play

## Task 8 — تحديث داخل التطبيق ✅ (غير مختبر على GitHub بعد)
- الـ workflow بينشر Release اسمه `latest` فيه `SCPlayer.apk` مع كل Build، ورقم الـ Build = versionCode.
- `app/build.gradle.kts`: versionCode من `-PappVersionCode` (= github.run_number).
- `data/update/Updater.kt` (فحص/تحميل/تثبيت) + `ui/update/UpdatePrompt.kt` (نافذة تحديث مرة كل تشغيل، تظهر في HomeScreen).
- Manifest: REQUEST_INSTALL_PACKAGES + FileProvider + `res/xml/file_paths.xml`.
- خطوات المستخدم: (1) الصق build.yml الجديد مرة واحدة من صفحة التعديل على GitHub، (2) ارفع update.zip، (3) ثبّت أول APK يدويًا (احذف القديم مرة)، (4) بعدها التحديثات من داخل التطبيق.
- تنبيه: الـ Release عام، ولازم تعطيل الـ updater و REQUEST_INSTALL_PACKAGES قبل Google Play.
## الجاي: Task 6 (إعدادات، قفل أبوي، QR، EPG يدوي، زر فحص التحديث) ثم Task 7 (إعلانات/Premium/Play).

## Task 9 — إعادة تصميم الواجهة حسب الـ mockup (المرحلة 1) ✅ (غير مختبر على GitHub)
- ثيم بنفسجي جديد + اتجاه LTR + شعار `SCLogo` (رسم Canvas أصلي) + سبلاش بالشعار.
- `HomeScreen.kt` بقى هيكل: شريط جانبي (عرض ≥720dp / TV) أو شريط سفلي (موبايل) + أقسام Home / Live TV / Movies / Series / Radio / Catch Up(قريبًا) / Playlist / Settings / About / Exit.
- `Dashboard.kt`: Hero + 4 تايلز بعدادات + Continue Watching. `ContentScreen.kt`: تصنيفات + قائمة قنوات + لوحة تفاصيل (Now/Next) على ≥1100dp. `SettingsScreen.kt`: Playlists, Clear Cache, Check for Updates, Reset Favorites, About.
- `data/Kind.kt`: تصنيف Live/Movies/Series/Radio من مسار Xtream (/movie/, /series/) أو اسم المجموعة.
- ملاحظات: صور الـ mockup (شخصيات محمية) لم تُستخدم؛ الـ Hero رسم تجريدي. الشاشات القديمة (Playlists/Guide/Add/Player) لسه نصوصها عربي.
## المراحل الجاية: (2) ترجمة/لغة EN-AR وإعادة تصميم Add Server وPlaylists وGuide وPlayer بنفس الستايل، (3) Task 6: Parental Control, QR, EPG يدوي, Playback settings، (4) Task 7: Premium/إعلانات/Play.

## Task 9 — المرحلة 2: تعدد اللغات ✅ (غير مختبر على GitHub)
- 9 لغات: English (الافتراضي)، العربية، Español، Français، Deutsch، Italiano، Português، Русский، Türkçe — ملفات `res/values-xx/strings.xml` (كل النصوص هناك، مفيش نصوص ثابتة في الكود غير "SC Player"/"LIVE").
- الاختيار من Settings → Language (أو "System default")؛ بيتحفظ في `sc_user/lang` وبيعمل recreate. `data/Language.kt` (قائمة اللغات + LocaleHelper.wrap) و`MainActivity.attachBaseContext`.
- اتجاه الواجهة (RTL/LTR) بيتحدد من اللغة تلقائيًا في `Theme.kt`.
- `friendlyError(ctx, e)` بقت بتاخد Context. اسم المجموعة الافتراضي من الـ parser لسه "أخرى".
- لإضافة لغة: انسخ values/strings.xml لـ values-xx وترجمه، وأضف السطر في `appLanguages`.
## الجاي: إعادة تصميم Add Server/Playlists/Guide/Player بنفس ستايل الـ mockup، ثم Task 6 (Parental Control, QR, EPG يدوي).

## Task 9 — الجزء A: Add Server ✅ (غير مختبر)
- شريط علوي (زر رجوع لو جاي من القوائم، أو الشعار) + رسم سيرفر Canvas + حقول بخلفية زجاجية + زر "✓ Add". AppNav بيمرّر `onBack`.
## باقي الأجزاء: B Playlists · C Guide · D Player

## Task 9 — الجزء B: Playlists ✅ (غير مختبر)
- `BackButton` مشترك في Components.kt، شبكة كروت (Adaptive 340dp)، أيقونة 📡 وشارة "Current" وإطار بنفسجي للقائمة النشطة.
## باقي الأجزاء: C Guide · D Player

## Task 9 — الجزء D: Player ✅ (غير مختبر)
- زر رجوع دائري، وأزرار تحكم دائرية (⏮ / ⏸▶ كبير بتدرّج بنفسجي / ⏭) بدل أزرار النص. زرّا Retry/Next في شاشة الخطأ زي ما هما.
## باقي: C Guide

## Task 9 — الجزء C: Guide ✅ (غير مختبر)
- زر رجوع دائري، شبكة كروت (Adaptive 420dp) بشعار القناة، شريط تقدّم بنفسجي. بكده كل أجزاء Task 9 (A–D) خلصت.
## الجاي: Task 6 (Parental Control, QR, EPG يدوي, Playback settings) ثم Task 7 (Premium/إعلانات/Play).

## Task 6A — القفل الأبوي + رابط EPG يدوي ✅ (غير مختبر)
- Settings → Parental Control: إنشاء PIN (4 أرقام، SHA-256 محلي) ثم اختيار تصنيفات مقفولة؛ التصنيفات المقفولة بتختفي من كل القوائم (Home/Live/Movies/Series/Radio/Recents) لحد "Unlock for this session" (`ParentalSession`، بيتصفّر لما التطبيق يتقفل). تغيير/حذف الـ PIN من نفس النافذة. `ui/home/ParentalDialogs.kt`.
- Settings → Custom guide link: رابط XMLTV لكل قائمة (`UserPrefs.epgOverride`)؛ `EpgRepository.epgUrl` بيفضّله، وبيمسح الكاش ويعيد التحميل.
- 22 مفتاح ترجمة جديد في الـ 9 لغات.
- حدود: القفل محلي وبيتخطّى بمسح بيانات التطبيق؛ إعدادات التشغيل (Playback) لسه مش معمولة.
## الجاي: 6B مسح QR، ثم Task 7.

## Task 6B — مسح QR ✅ (غير مختبر)
- تبويب Scan QR في Add Server: زر يفتح ماسح Google Code Scanner (`play-services-code-scanner:16.1.0`، من غير صلاحية كاميرا). لو الـ QR فيه رابط http بيتعبّى حقل الرابط ويرجع لتبويب M3U للمراجعة والإضافة؛ غير كده رسالة خطأ.
- محتاج Google Play services؛ على أجهزة TV/بدون GMS بتظهر رسالة "غير متاح".
- 3 مفاتيح ترجمة جديدة في الـ 9 لغات.
## الجاي: Task 7 (Premium/إعلانات/Google Play) + إعدادات التشغيل لو عايز.

## إصلاح تخطيط Add Server على الموبايل ✅ (غير مختبر)
- من سكرين شوت الموبايل: الحقول كانت جنب بعض ومتزنقة، وتبويب "Xtream Codes" متقطّع، وزر الإضافة بيتكسر سطرين.
- الحل: على الشاشة الضيقة الحقول تحت بعض بعرض كامل، رأس مضغوط (رسمة صغيرة + الاسم جنبها)، تبويب Xtream اسمه "Xtream"، والزر سطر واحد (ellipsis، 16sp).
- لسه محتاج سكرين شوتات لباقي الشاشات على الموبايل (Home / Live TV / Settings) لمراجعتها.

## الرئيسية بشكل Launcher ثلاثي الأبعاد ✅ (غير مختبر)
- `ui/home/Launcher.kt` بدل Dashboard: مربع Live كبير + Movies/Series/Radio/Guide + عمود أزرار (Change Playlist / Reload / Settings / Exit) على الشاشات العريضة؛ على الموبايل بالطول: Live كبير ثم 2×2 ثم صف أزرار صغيرة + Continue Watching.
- تأثير 3D: وجه متدرّج بلمعة زجاجية وحافة مضيئة، سُمك سفلي وظل ملوّن، ميل perspective ناحية اللمس (rotationX/Y)، وتكبير وإطار أبيض مع focus الريموت.
- الرئيسية من غير شريط جانبي/سفلي؛ باقي الشاشات فيها الشريط وفيه Home للرجوع. زر Reload بيحدّث القائمة من المصدر.
- اعتمادية جديدة: `material-icons-extended` (أيقونات TV/Movie/Radio...). مفاتيح ترجمة: change_playlist, tile_reload, reloading, home_playlist.
- مش منسوخ من أي تطبيق: التصميم والألوان والشعار بتاعتنا؛ "Account/تاريخ الانتهاء" مش موجودين لأن مفيش API لحساب المستخدم لسه.

## إصلاح Build 29: `matchParentSize` اتعمله import غلط في Launcher.kt (هو عضو في BoxScope ومش بيتعمله import) — اتشال الـ import.
