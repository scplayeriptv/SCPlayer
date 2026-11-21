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

## إصلاح دخول Xtream (خطأ 404) ✅ (غير مختبر)
- الأعراض: Xtream بسيرفر `https://es.egys.gs` + user/pass بيرجّع "The server returned an error (404)" لأن `get.php` مش متاح على العنوان ده.
- الحل في `PlaylistRepository.download`: لو `get.php` فشل (404/403/فاضي) نجرّب `player_api.php` (فحص الدخول `user_info.auth`) ونبني M3U بنفسنا من `get_live_streams` + `get_vod_streams` (`data/XtreamApi.kt`)، ونجرّب بروتوكول تاني (http ⇄ https) لو مفيش بورت، وUser-Agent تاني (VLC).
- رسالة خطأ جديدة `err_xtream_404` (9 لغات) لو العنوان مش سيرفر Xtream أصلًا: تلمّح للبورت (مثل :8080) والاشتراك.
- حدود: المسلسلات (series) مش داخلة في النمط ده (محتاجة طلب لكل مسلسل). مجرّب على سيرفر حقيقي: لا.
- لا يوجد unit test لـ XtreamApi لأن `Uri`/`org.json` مش متاحين في اختبارات الـ JVM من غير إعداد إضافي (الـ CI بيشغّل testDebugUnitTest).

## إعادة تصميم الإعدادات (Settings) ✅ (غير مختبر)
- `ui/home/SettingsScreen.kt` على نفس تصميم الموكاب: على الشاشات العريضة لوحة أقسام يسار (عام / القفل الأبوي / التشغيل / دليل البرامج / القوائم / عن التطبيق) ولوحة صفوف يمين؛ على الموبايل أزرار أقسام أفقية + كارت صفوف.
- عام: اللغة، مسح الكاش (الحجم + أيقونة سلة)، البحث عن تحديث (أيقونة تحديث)، مسح المفضلة والسجل، استعادة الإعدادات. التشغيل: تشغيل تلقائي + تسريع الأجهزة (مفاتيح Switch بنفسجي).
- تشغيل تلقائي: لما يتفعّل، التطبيق يكمّل آخر قناة مرة واحدة عند كل فتح (`AutoPlaySession` + `LaunchedEffect` في HomeScreen). افتراضي: مقفول.
- تسريع الأجهزة: لو اتقفل، `MediaCodecSelector` في PlayerScreen بيقدّم مفكّكات البرمجيات (يفيد لو الصورة بتبوظ). افتراضي: شغّال. بيتطبّق عند فتح المشغّل.
- مفاتيح UserPrefs جديدة: auto_play, hw_decoding + `restoreDefaults()`. ترجمة ×9 لغات: set_sec_general/playback/guide, set_auto_play(+_sub), set_hw(+_sub), set_restore.
- ما اتعملش قسم Appearance/Theme لأن مفيش ثيم فاتح حاليًا (مش هنحط صف وهمي).

## شريط التحديث بالحجم ✅ (غير مختبر)
- نافذة التحديث بتعرض "حجم التحديث: X MB" قبل التحميل، وأثناءه "٪ — المُحمّل / الإجمالي MB" (الحجم من GitHub API). مفاتيح: update_size, downloading_size ×9.
- التحديث بالفروقات فقط (delta/bsdiff) لسه ما اتعملش: محتاج تعديل build.yml (توليد patch بين الـ APK السابق والجديد) + bspatch داخل التطبيق. مستني قرار المستخدم.

## تحديث بالفروقات (delta) ✅ (غير مختبر) — محتاج لصق build.yml الجديد يدويًا
- build.yml: بعد البناء ينزّل APK الإصدار المنشور حاليًا، ويطلّع `patch-<قديم>-to-<جديد>.bsdiff` بـ bsdiff4، وينشره مع الـ APK، ويكتب `sha256` في ملاحظات الإصدار. لو فشل الباتش يكمّل بالـ APK كامل.
- التطبيق: `data/update/BsPatch.kt` (BSDIFF40 + commons-compress للـ bz2). `Updater.check` يدوّر على باتش مطابق لبناء المستخدم الحالي؛ `download` ينزّل الباتش، يطبّقه على APK المثبّت، يتحقق من SHA-256، ولو أي حاجة فشلت يرجع للتحميل الكامل. النافذة بتكتب "(التغييرات فقط)".
- يشتغل بس لو المثبّت = البناء السابق مباشرة؛ غير كده تحميل كامل. اختبار: BsPatchTest.

## About + تشغيل/تحديث في الخلفية ✅ (غير مختبر)
- About: اسم المطوّر Mostafa Mahmoud + إيميل التواصل Sc.downloder@gmail.com (ضغطة تفتح تطبيق الإيميل). مفاتيح: about_developer, about_contact.
- تشغيل في الخلفية: `service/PlaybackService.kt` خدمة أمامية (mediaPlayback) بإشعار فيه زر Stop؛ بتشتغل لما المشغّل يفتح وتتقفل لما يتقفل. PlayerScreen مبقاش يعمل pause عند ON_STOP لو الخيار شغّال، وفيه audio focus + WAKE_MODE_NETWORK + إيقاف عند سحب السماعات. إعداد جديد في Settings → Playback: "Background Play" (افتراضي شغّال، مفتاح `bg_play`). ملاحظة: في الخلفية بيشتغل الصوت بس (الصورة بتوقف لحد ما ترجع للتطبيق).
- تحديث في الخلفية: `data/update/UpdateManager.kt` (حالة على مستوى التطبيق) + `service/UpdateService.kt` (dataSync) — التحميل بيكمّل حتى لو قفلت النافذة أو خرجت من التطبيق، مع إشعار بنسبة التحميل، وبعد ما يخلص إشعار "التحديث جاهز — اضغط للتثبيت". زر النافذة بقى "متابعة في الخلفية" أثناء التحميل.
- Manifest: FOREGROUND_SERVICE(+MEDIA_PLAYBACK,+DATA_SYNC), POST_NOTIFICATIONS (بيتطلب عند فتح التطبيق على أندرويد 13+), WAKE_LOCK.

## مسلسلات Xtream + بحث شامل + معلومات الحساب ✅ (غير مختبر)
- المسلسلات: لما get.php يفشل ونبني القائمة من player_api.php، بنضيف `get_series_categories` + `get_series` كعناصر برابط `.../seriesinfo/user/pass/<id>` (`isSeriesEntry`, `Kind.kt` بيصنّفها Series). الضغط على مسلسل بيفتح `SeriesScreen` (مواسم + حلقات من `get_series_info` عن طريق `data/XtreamClient.kt`)، والحلقة بتتشغّل بقائمة باقي حلقات الموسم (روابط `.../series/user/pass/<id>.<ext>`). لازم المستخدم يعمل Reload للقائمة عشان المسلسلات تظهر. القوائم اللي get.php شغّال فيها أصلًا فيها الحلقات في الـ M3U زي ما كانت.
- بحث شامل: Dest.Search (شريط جانبي + مربع في الرئيسية) بيدوّر في كل القنوات والأفلام والمسلسلات والراديو (حرفين على الأقل، لحد 300 نتيجة). البحث جوّه كل قسم كان موجود من قبل.
- معلومات الحساب: قسم Account في Settings (user_info من player_api.php): الحالة، تاريخ الانتهاء + الأيام المتبقية، الاتصالات الحالية/الأقصى، تجريبي/كامل. لقوائم Xtream بس.
- ترجمة ×9 (19 مفتاح جديد: acc_*, series_*, nav_search, search_prompt, set_sec_account).
- حدود: لا اختبار على سيرفر حقيقي؛ parseSeriesInfo بيفترض الشكل القياسي لـ get_series_info (episodes كـ object مفاتيحه أرقام المواسم أو array). مفيش JVM test لأن org.json/Uri مش متاحين في الـ JVM.

## الجزء A: استكمال المشاهدة + شريط تقدّم للأفلام ✅ (غير مختبر)
- `data/PositionStore.kt`: يحفظ موضع كل فيلم/حلقة (مفتاحه الرابط، آخر 150)، كل 10 ثواني أثناء التشغيل وعند تغيير العنصر/الإيقاف/الخروج. بيكمّل من الموضع لو أكبر من 10 ثواني، وبيمسحه لو فاضل أقل من 30 ثانية. للبث المباشر مفيش حفظ.
- PlayerScreen: MediaItem بـ mediaId = الرابط؛ شريط تقدّم (Slider) + الوقت + أزرار ⏪/⏩ 10 ثواني للأفلام والحلقات فقط (isVod: مش live والمدة ≥ دقيقة). ريموت التلفزيون: يمين/يسار = ±10 ثواني للـ VOD. زر "Reset favorites" بيمسح المواضع المحفوظة كمان. مفاتيح: player_back10, player_fwd10 ×9.

## الجزء B: صوت/ترجمة/نسبة الصورة ✅ (غير مختبر)
- زر ⚙ في شريط المشغّل (أو زر Menu في الريموت) يفتح `PlayerOptionsDialog`: اختيار مسار الصوت (لو أكتر من مسار)، الترجمة (Off + المسارات المدمجة)، ونسبة الصورة Fit / Stretch / Zoom (محفوظة في UserPrefs `aspect`).
- إضافة dependency `media3-ui:1.4.1` عشان `SubtitleView` يعرض الترجمة فوق الفيديو. حجم السطح بيتحسب بـ BoxWithConstraints + requiredSize.
- مفاتيح ×9: player_options/audio/subtitles/off/track, player_aspect, aspect_fit/stretch/zoom.

## الجزء C: صورة داخل صورة (PiP) ✅ (غير مختبر)
- Manifest: `supportsPictureInPicture` + configChanges. `ui/player/PipState.kt`. MainActivity: `onUserLeaveHint` → يدخل PiP تلقائي لو المشغّل مفتوح والإعداد شغّال؛ `onPictureInPictureModeChanged` يخفي الأزرار أثناء PiP. زر 🖼 في المشغّل يدخل PiP يدويًا (لو الجهاز بيدعمه، مش على أغلب التلفزيونات).
- إعداد جديد Settings → Playback: "Picture-in-Picture" (افتراضي شغّال). مفاتيح: set_pip, set_pip_sub, player_pip ×9.
- ملاحظة: في PiP لو المستخدم قفل النافذة والتشغيل في الخلفية شغّال، الصوت بيكمّل.

## إصلاح: المسلسلات = 0 ✅ (غير مختبر)
- السبب: get.php عند أغلب سيرفرات Xtream بيرجّع القنوات والأفلام بس، فالمسلسلات (اللي كانت بتتضاف من player_api.php في حالة فشل get.php بس) ما كانتش بتظهر. دلوقتي لو القائمة Xtream ومفيش ولا مسلسل بعد get.php، بنجيب get_series/get_series_categories من player_api.php ونضيفهم (`addSeriesFromApi`, `writeSeriesEntries`). لازم Reload بعد التحديث.

## ميزة 1: تبديل القنوات من المشغّل ✅ (غير مختبر)
- `ui/player/ChannelListPanel.kt`: قائمة القنوات فوق الفيديو (زر ☰ في الشريط العلوي، أو سهم يسار على الريموت للبث المباشر). اختيار قناة يبدّل فورًا من غير ما تخرج.
- أرقام الريموت: كتابة رقم (لحد 4 خانات) = ترتيب القناة في القائمة الحالية، بيتنفّذ بعد 1.5 ثانية أو بـ OK. ChannelUp/ChannelDown = التالي/السابق. زر "↩" و Key.LastChannel = القناة السابقة.
- مفاتيح ×9: player_channels, player_last.

## ميزة 2: بوسترات وتفاصيل الأفلام والمسلسلات ✅ (غير مختبر)
- Movies/Series في ContentScreen بقت شبكة بوسترات (`Posters.kt`: PosterCard بشريط تقدّم المشاهدة). Live/Radio لسه قوائم.
- `MovieDetailsScreen`: بوستر + backdrop + سنة/مدة/تقييم/نوع/قصة/ممثلين/مخرج + تشغيل/كمّل من X + مفضلة + تريلر (يوتيوب). البيانات من `get_vod_info` عبر `XtreamClient.vodInfo` (من غير TMDB ولا مفتاح)؛ لو القائمة مش Xtream بيعرض الاسم والصورة بس.
- شاشة المسلسل بقى فيها رأس تفاصيل (`MediaHeader`) من info بتاع get_series_info. `parseMediaInfo`, `MediaInfo`.
- الفتح: openItem في HomeScreen: فيلم → تفاصيل، مسلسل → شاشته، غيره → تشغيل. "Continue Watching" في الرئيسية لسه بيشغّل مباشرة.
- مفاتيح ×9: det_resume, det_trailer, det_cast, det_director.

## إصلاح: قنوات لايف ظاهرة في قسم الأفلام ✅ (غير مختبر)
- السبب: `Kind.kt` كان بيخمّن من اسم المجموعة ("GO BOX CINEMA" فيها cinema) حتى لو الرابط بث مباشر. دلوقتي رابط البث المباشر (/live/ أو .ts أو .m3u8 أو host/user/pass/id) بيفضل Live؛ /movie/ و /series/ لسه الأقوى، والراديو من اسم المجموعة. تعديل KindTest + اختبار جديد.

## ميزة 3: بروفايلات ✅ (غير مختبر)
- `Profile(id,name,kids,avatar)` في UserPrefs.kt؛ لحد 6 بروفايلات. المفضلة والـ Recent منفصلة لكل بروفايل (`fav_<id>`, `recent_<id>`؛ الرئيسي "main" بيحافظ على المفاتيح القديمة).
- `ProfileScreens.kt`: شاشة "مين بيتفرج؟"، حوار تعديل (اسم/أفاتار/بروفايل أطفال/استخدم/حذف)، `ProfileChip` في الرئيسية.
- بروفايل الأطفال: بيخبّي المجموعات المقفولة حتى لو اتفتحت، والإعدادات والخروج منه محتاجين PIN لو متحدد.
- Settings → Profiles. مفاتيح ×9: set_sec_profiles, profile_*.
- محدودية: مواضع المشاهدة (PositionStore) والـ PIN والمجموعات المقفولة مشتركة بين الكل.

## ميزة 4: دليل برامج بجدول زمني + تذكيرات ✅ (غير مختبر)
- `GuideScreen.kt` اتعاد كتابته: شبكة قنوات × وقت (12 ساعة، من قبل الآن بنص ساعة)، خط أحمر للوقت الحالي، سكرول أفقي مشترك. ضغط على برنامج حالي = تشغيل القناة؛ غيره = تفاصيل + "ذكّرني" / "شاهد القناة".
- `ReminderStore` + `ReminderReceiver` + `BootReceiver`: إشعار قبل البرنامج بدقيقتين (AlarmManager غير دقيق، ممكن يتأخر دقائق) وبيتعاد جدولته بعد إعادة تشغيل الجهاز. زر 🔔 في الدليل بيعرض كل التذكيرات.
- محدودية: الضغط على الإشعار بيفتح التطبيق بس (من غير تشغيل القناة).
- مفاتيح ×9: guide_reminders, guide_remind, guide_remind_off, guide_reminder_set, guide_watch, guide_no_reminders, notif_reminder_title, notif_reminder_text.

## ميزة 5: صف "Continue Watching" في Android TV ✅ (غير مختبر)
- `data/WatchNext.kt`: بيكتب في Watch Next provider (`TvContract.WatchNextPrograms`، Android 8+ وأجهزة TV بس، أي فشل بيتتجاهل). بيتحدّث كل ~30 ثانية من `saveNow` في PlayerScreen، وبيتشال لما الفيلم/الحلقة تخلص أو يتقفل قبل 10 ثواني.
- الضغط على الكارت بيفتح التطبيق بـ Intent فيه (url, name, logo) → `MainActivity.readDeepLink` → `DeepLink.item` → HomeScreen بيشغّله (بعد اختيار البروفايل). MainActivity بقت `singleTop`.
- محدودية: بيشتغل للأفلام/الحلقات بس (مش البث المباشر)؛ لو الصف ما ظهرش على جهاز معيّن ممكن تكون الصلاحية/النظام (بيعتمد على launcher الجهاز).

## ميزة 6: Multi-view ✅ (غير مختبر)
- `ui/multi/MultiViewScreen.kt`، route "multi" في AppNav، وتايل جديد في الرئيسية (Multi-view). لحد 4 قنوات (شبكة 2×2) أو 2.
- كل خانة ExoPlayer مستقل؛ الصوت للخانة المختارة بس (🔊). ضغط على خانة فاضية = اختيار قناة (بحث)؛ على خانة غير مختارة = تختار صوتها؛ على المختارة = قائمة (غيّر/ملء الشاشة/شيل).
- محدودية: 4 فيديوهات مع بعض تقيلة على الأجهزة الضعيفة (المفكّكات). خانات Multi-view مش بتتحفظ بين الفتحات.
- مفاتيح ×9: multi_view, multi_pick, multi_change, multi_remove, multi_fullscreen, multi_add.

## ميزة 7: استرجاع تلقائي + البافر + إحصائيات ✅ (غير مختبر)
- إعادة اتصال تلقائي في PlayerScreen: عند خطأ تشغيل أو انقطاع بث (ENDED لبث مباشر/TS من غير مدة) بيحاول لحد 5 مرات بمهلة 2ث، 4ث، ... وبيظهر "Reconnecting n/5". العدّاد بيتصفّر بعد 20 ثانية تشغيل متواصل أو تغيير القناة. بعد آخر محاولة بتظهر شاشة الخطأ (إعادة/التالي).
- Settings → Playback: "Auto reconnect" (افتراضي شغّال)، "Buffer size" (صغير 5-15ث / عادي افتراضي Media3 / كبير 30-120ث، بالضغط بيتبدّل)، "Playback stats" (دقة/كودك/بت ريت/بافر/سرعة الشبكة/فريمات مفقودة فوق الفيديو، أعلى اليمين).
- الإعدادات بتتقرأ لما المشغّل يتفتح (غيّرها قبل ما تفتح قناة). مفاتيح ×9: set_reconnect(+_sub), set_buffer(+_sub), buffer_low/normal/high, set_stats(+_sub), player_reconnecting. restoreDefaults بيشملهم.

## ميزة 9: ثيمات وألوان ✅ (غير مختبر)
- `ui/theme/Theme.kt`: `AppTheme` (Violet الافتراضي، Ocean، Emerald، Sunset، Rose، AMOLED أسود) + `ThemeState.current` (Compose state). الألوان Bg/Panel/Cyan/Violet/Blue/BrandBrush بقت getters بتقرأ الثيم الحالي، فكل الشاشات بتتحدّث فورًا من غير ما نلمس باقي الملفات.
- Settings → General → صف "Theme" بدواير ملوّنة (`ThemePicker.kt`)، بيتحفظ في `UserPrefs.theme()` وبيتحمّل في `MainActivity.onCreate`. تينتات الـ focus الثابتة (A855F7) اتحوّلت للثيم.
- محدودية: مربّعات الرئيسية (Live=بنفسجي، Movies=أزرق...) والشعار ولوجو شاشة الإضافة ليهم ألوانهم الثابتة ومش بيتغيّروا؛ استعادة الافتراضي مش بترجّع الثيم. مفاتيح ×9: set_theme, theme_violet/ocean/emerald/sunset/rose/amoled.

## ميزة 10: Onboarding + حركات ✅ (غير مختبر)
- `ui/onboarding/OnboardingScreen.kt`: 4 صفحات (HorizontalPager) بتظهر أول تشغيل بس بعد الـ Splash (قوايمك/Live-Movies-Series/الدليل والتذكيرات/بروفايلات-Multi-view-ثيمات). تخطّي/التالي/يلا نبدأ، نقط مؤشر، إيموجي بيطفو (infinite transition)، تلاشي/تصغير للصفحات الجانبية. زر الرجوع يرجّع صفحة. الفلاج `UserPrefs.onboarded()`.
- AppNav: `afterIntro()` (نفس منطق الـ Splash القديم)، وNavHost بقى فيه fade بين كل الشاشات (260ms/180ms).
- محدودية: المستخدمين الحاليين هيشوفوا الـ onboarding مرة واحدة بعد التحديث. مفاتيح ×9: onb_next/skip/start، onb1..4_title/body.

## إصلاح Build 39: علامة `&` في strings.xml (onb3_title في values و values-de) لازم تتكتب `&amp;` — اتصلّحت، وكل ملفات الـ strings اتفحصت إنها XML سليم (parse).

## ميزة 11: Google Cast ✅ (غير مختبر)
- Dependencies جديدة: media3-cast 1.4.1، play-services-cast-framework 21.5.0، mediarouter 1.7.0. `cast/CastOptionsProvider.kt` (Default Media Receiver — من غير تسجيل) + meta-data في الـ Manifest.
- `cast/CastHelper.kt`: بيلفّ CastContext/CastPlayer (null على TV أو من غير Play Services → الزر مش بيظهر) + `CastDialog` بقايمة الأجهزة بتاعتنا (من MediaRouter) لأن MediaRouteButton بيحتاج ثيم AppCompat والتطبيق مش كده.
- في المشغّل: زر 📡 في الشريط العلوي → اختيار جهاز → الفيديو المحلي بيتوقف والـ Cast بيكمّل من نفس الموضع (للأفلام)، ولوحة "بيعرض على X" فيها Play/Pause وإيقاف. تغيير القناة أثناء الـ Cast بيبعت القناة الجديدة. إيقاف الـ Cast أو الخروج من المشغّل بيقفل الجلسة ويرجّع التشغيل المحلي.
- روابط Xtream المباشرة `.../live/.../id.ts` بتتحوّل لـ `.m3u8` للـ Cast (الـ Chromecast مش بيشغّل TS خام). MIME: m3u8/mkv/webm/mp4 حسب الامتداد.
- محدودية: الـ Chromecast بيفتح الرابط بنفسه (لازم يوصل للسيرفر وما يكونش مربوط بـ IP واحد)، بعض السيرفرات بتمنعه؛ مفيش seek أثناء الـ Cast؛ الصيغ/الكودكات غير المدعومة (مثلاً H.265/AC3 على بعض الأجهزة) ممكن ما تشتغلش؛ بعد Cast للـ VOD الرجوع للمحلي بيكمّل من آخر موضع محلي مش موضع الـ Cast. غير مختبر على جهاز Cast حقيقي.
- مفاتيح ×9: cast_title, cast_none, cast_casting, cast_stop.

## تنظيم الإعدادات ✅ (غير مختبر)
- SettingRow: لو القيمة أطول من 18 حرف بتنزل تحت العنوان بدل ما تعصر العنوان (حالة "Reset Favorites & Recents" و"Installed build…").
- عناوين مجموعات صغيرة (`GroupLabel`): General → المظهر / الصيانة / إعادة الضبط؛ Playback → التشغيل / الأداء / التشخيص. مفاتيح ×9: set_grp_*.
- ThemePicker: العنوان والاسم في سطر واحد، والدواير الـ6 موزّعة بالتساوي (SpaceBetween، 38dp) ومبقتش بتتقطع.

## تنظيم الرئيسية (موبايل) ✅ (غير مختبر)
- Launcher (narrow): Live (150dp) + Movies/Series (112dp)، ثم مجموعتين بعناوين صغيرة: TOOLS (Radio/Guide/Multi-view/Search) و MANAGE (Playlist/Reload/Settings/Exit) بنفس حجم Mini (92dp) — كل الأسماء بتتكتب في سطرين بدل الاقتطاع ("Multi-view"، "Change Playlist").
- شارة Radio بتختفي لما العدد 0 (موبايل وواسع). مفاتيح ×9: grp_tools, grp_manage. تخطيط الشاشات الواسعة (TV) ما اتغيّرش غير الشارة.

## ميزة 8: أداء القوايم الكبيرة ✅ (غير مختبر)
- `Channel.kind()` بقى بيتخزّن (كاش داخلي في الـ Channel) بدل ما يعمل lowercase + 3 regex مع كل صف وكل إعادة رسم؛ وبيتسخّن في خيط الخلفية وقت تحميل القايمة (`PlaylistRepository.channels`).
- `EpgStore.forChannel` بيخزّن الاسم المُطبَّع ونتيجة البحث لكل قناة (بيتبطّل لما بيانات الدليل تتغيّر) — الصفوف ما بقتش تعيد حساب `normalizeName` كل مرة.
- `M3uParser`: شلت الـ regex + map لكل سطر EXTINF واستبدلتها بقراءة الخصائص بـ indexOf (مع التحقق من حدود الاسم وعدم حساسية الحروف) — أسرع في ملفات بعشرات الآلاف.
- اختبارات جديدة: خصائص بحروف كبيرة/مفتاح مشابه (xtvg-id)، وثبات كاش kind.
- محدودية: لسه الـ parse بيتعمل من الملف في كل تشغيل بارد؛ لو لسه بطيء نعمل كاش ثنائي على القرص. مش قايس الأرقام قبل/بعد (مفيش جهاز هنا).

## شاشة إضافة السيرفر (Xtream) على شكل فورم السيرفر ✅ (غير مختبر)
- `AddPlaylistScreen.kt`: تاب Xtream بقى الافتراضي، وحقوله في عمود واحد بالترتيب: اسم القايمة → Username → Password (زر 👁/🙈 لإظهار الباسورد) → Server Address، وزر الإضافة تحت. الـ IME: Next بين الحقول و Done في آخر حقل بيضيف مباشرة لو البيانات كاملة (مفيد للريموت/الكيبورد).
- في الشاشات الواسعة (TV/تابلت) تحت الشعار زر "📋 القوائم المحفوظة" (add_list_playlists ×9) بيرجّع لقايمة الـ playlists (يظهر لما نكون جايين من القايمة).
- تابات M3U و QR زي ما هي. مفيش استخدام لأي شعار أو اسم تطبيق تاني.

## تبسيط الرئيسية + أفقي + مقدمة جديدة ✅ (غير مختبر)
- **الرئيسية**: فضلت Live / Movies / Series + Reload / Settings / Exit بس. Radio و Guide و Multi-view و Search اتنقلوا لقسم جديد في Settings اسمه "Tools" (set_sec_tools ×9) عن طريق `onTool` من HomeScreen. Change Playlist موجود في Settings → Playlists.
- **أفقي**: `MainActivity` بقى `screenOrientation="sensorLandscape"` (للتراجع: شيل السطر من الـ Manifest). وHomeScreen بيستخدم التخطيط الواسع (`wide`) في أي وضع أفقي (مش بس ≥720dp): Live (أكبر) + Movies + Series بجنب بعض، وعمود Reload/Settings/Exit. التخطيط الرأسي القديم اتبسّط برضه (3 مربعات صغيرة).
- **المقدمة (SplashScreen)**: ~3.2 ثانية: هالة ضوء + اللوجو بيظهر بنبضة (spring)، وحروف "SC Player" بتتجمّع من مواضع/زوايا/أحجام عشوائية (staggered 90ms، تدرّج لوني حسب الثيم)، لمعة بتعدّي على الكلمة، وبعدين "IPTV PLAYER" بتظهر تحتها. الألوان بتتبع الثيم المختار.
- ملاحظات: "Continue Watching" مش ظاهرة في التخطيط الواسع (كانت في الرأسي بس) — ممكن نضيفها كصف تحت. الفورمز في الوضع الأفقي بتتقفل شوية بالكيبورد (بتتسكرول).

## ضبط العرض الأفقي على الموبايل ✅ (غير مختبر)
- مربعات الرئيسية: عمود Reload/Settings/Exit أعرض (weight 1.35)، أيقونة 38dp وخط 15sp وفراغات أقل — عشان "Reload" و"Settings" يظهروا كاملين.
- HomeScreen: لما ارتفاع الشاشة < 480dp (موبايل أفقي) الـ Sidebar بيتحوّل لشريط أيقونات ضيق (64dp، `RailItem`) بدل 240dp.
- Settings: التخطيط ثلاثي الأجزاء بقى بس لما العرض ≥ 900dp والارتفاع ≥ 480dp (تلفزيون/تابلت)؛ على الموبايل الأفقي بقى تابات فوق + كارت واحد عريض (من غير عنوان كبير لو الارتفاع < 480) — اتحلّت مشكلة "Lan-gua-ge" ودواير الثيم المقطوعة.

## دفعة: صف الرئيسية + مؤقّت النوم + نسخ احتياطي ✅ (غير مختبر)
- **صف "Continue Watching · Favorites"** في الرئيسية الأفقية: `quick = recents + المفضلة (بلا تكرار، حتى 24)` من HomeScreen، و`MiniCard` في Launcher (لوجو + اسم + ★). الضغط بيفتح `openItem` (فيلم → تفاصيل، مسلسل → مواسم). على الشاشات القصيرة (<480dp) مربعات Live/Movies/Series بتتحوّل لأسلوب Card عشان الصف يلحق.
- **مؤقّت النوم** في المشغّل: زر ⏲ في الشريط العلوي → Off/15/30/45/60/90 دقيقة؛ بيظهر ⏲ Nm، وعند الانتهاء بيوقّف التشغيل + الـ Cast ويرجع للخلف. (بيشتغل مع قياس `elapsedRealtime`، والشاشة لازم تفضل شغّالة داخل المشغّل.)
- **نسخ احتياطي/استعادة** (Settings → General → Maintenance): `BackupManager` بيصدّر JSON فيه `sc_user` (المفضلة/البروفايلات/الإعدادات/الثيم/PIN) و`sc_positions` عبر Storage Access Framework (CreateDocument/OpenDocument). الاستعادة بتستبدل الحالي وبتعمل `recreate()`. القوائم نفسها مش داخلة (فيها بيانات الدخول)؛ الملف نفسه فيه روابط مفضلتك فاحتفظ بيه خاص.
- مفاتيح ×9: set_backup(+_sub), set_restore(+_sub), backup_done/failed, restore_done/failed, sleep_title/off/min.

## إصلاح Build 47: مفتاح مكرر `set_restore` (كان موجود أصلًا لـ "Restore Defaults"). اتغيّر مفتاح النسخة الاحتياطية لـ `set_backup_restore` (+`_sub`). فحصت كل strings: مفيش مفاتيح مكررة، وكل `R.string.*` مستخدم له تعريف، والـ XML سليم.

## شاشة إضافة القائمة بعرض الشاشة ✅ (غير مختبر)
- `AddPlaylistScreen.kt`: شلت اللوحة اليسرى (رسم السيرفر) والتقسيم لعمودين. دلوقتي: شريط علوي فيه (رجوع) + لوجو + **"SC Player"** بتدرّج الثيم وتحته "Add your playlist"، وتحته الفورم بعرض الشاشة كلها (كارت زجاجي بهوامش 20dp، سكرول رأسي).
- لو العرض ≥ 600dp (موبايل أفقي/تابلت/TV): حقول Xtream في صفّين (اسم | Username) و(Password | Server)، وM3U: الرابط (2) | الاسم (1). غير كده عمود واحد.
- دوال `Brand` و`ServerIllustration` لسه في الملف بس مش مستخدمة (ممكن تتشال).

## إصلاح Build 48: `SettingsScreen.kt:141 Unresolved reference 'prefs'` — لانچر الاستعادة كان بيستخدم `prefs` قبل تعريفه؛ اتغيّر لـ `UserPrefs(ctx).theme()`.

## تنظيف القائمة الجانبية
- القائمة الجانبية بقت فيها الرئيسية + الإعدادات + الخروج بس. Live/Movies/Series بتتفتح من الصفحة الرئيسية، وRadio/Search/Guide/Multi-view/Playlists/About موجودين في الإعدادات.

## تبديل القناة السريع + بحث صوتي
- المشغّل: سحب لفوق = القناة التالية، لتحت = السابقة (للبث المباشر بس). زر ↩ للقناة السابقة وأرقام الريموت كانوا موجودين.
- البحث: زر 🎤 جوه خانة البحث بيفتح التعرف الصوتي بتاع النظام (من غير صلاحية ميكروفون). لو الجهاز مفيهوش خدمة صوت (بعض التلفزيونات) بيظهر تنبيه. الاستعلام <queries> اتضاف للـ Manifest.

## تصميم الرئيسية الجديد (Cinematic Hub)
- اتشال صف Continue Watching/Favorites ورقم النسخة من الرئيسية (النسخة لسه في About).
- فوق: شريحة البروفايل يسار، اللوجو + اسم القائمة في النص، ويمين 3 أزرار دائرية زجاجية (Reload / Settings / Exit). Reload بيلف وهو بيحمّل.
- تحت: 3 لوحات كبيرة (Live أكبر شوية، Movies، Series). كل لوحة: خلفية متحركة بكرات ضوء، أيقونة عملاقة شفافة كعلامة مائية، ميدالية مضيئة، عدّاد بتوهّج، شريط بيطول مع الـ focus، سهم زجاجي، وميل 3D مع اللمس. Live عليها وسم LIVE بينبض.
- الملف: ui/home/Launcher.kt (HubPanel, DockButton). التخطيط الطولي القديم لسه موجود كما هو بدون صف الاستمرار.

### Fix: Launcher.kt — Unresolved reference 'animateFloat' (ناقص import androidx.compose.animation.core.animateFloat)

## تصميم القوائم الداخلية (Live / Movies / Series / Radio)
- ContentScreen: العنوان بتدرّج الثيم + عدّاد، خانة بحث كبسولة زجاجية (SearchPill) فيها 🔍 وزر مسح ✕.
- لوحة التصنيفات: حاوية زجاجية مدوّرة، الصف المحدد بتدرّج الثيم وشريط جانبي، العدّاد في كبسولة صغيرة.
- ChannelRow: لوجو في بلاطة، شريط تقدّم للبرنامج الحالي (EPG)، تدرّج وتوهّج عند الـ focus.
- PosterCard: العنوان فوق البوستر على تدرّج داكن، توهّج وحد متدرّج عند الـ focus، شريط تقدّم المشاهدة بلون الثيم.
- الشريط الجانبي: أيقونات Material بدل الإيموجي (كان زر الخروج بيظهر مربع فاضي)، التحديد بتوهّج، وزر الخروج أحمر.

### Fix: HomeScreen.kt — Unresolved reference ExitToApp/Home/Settings/Info (أيقونات Material لازم import، مش بتشتغل بالاسم الكامل)

## إعادة تصميم شاشات Live/Movies/Series على نمط المرجع
- لوحة تصنيفات على الشمال بطول الشاشة: خانة بحث في التصنيفات + All + Favorites + المجموعات (الصف المحدد بتدرّج الثيم).
- على اليمين لوحة بخلفية متدرّجة ملوّنة (Cyan→Violet→داكن) فيها عنوان التصنيف المحدد + العدد + بحث + Guide، وتحتها شبكة بوسترات أصغر (≈5 أعمدة على الموبايل) بالعنوان فوق البوستر.
- Live بنفس التصميم لكن بصفوف القنوات. الملف: ContentScreen.kt (CategoryPanel).
- "Continue Watching / Recently Added" كتصنيفات مش متضافة (محتاجة تتبّع إضافي).

## تصنيفات Continue Watching و Recently Added
- Continue Watching: أفلام/مسلسلات = اللي لسه بيتكمّل (من PositionStore.inProgress، الأحدث أول). Live/Radio = آخر المشاهَد (UserPrefs.recents). بتظهر بس لو فيها عناصر.
- Recently Added: SeenStore بيسجّل أول ظهور لكل رابط (ملف ثنائي seen_<النوع>.bin بهاش الرابط). أول تحميل أو تغيير القائمة كلها (>40% جديد) = baseline. بيعرض آخر 14 يوم، والأحدث أول، وبحد أقصى 200؛ ولو مفيش جديد بيعرض آخر 30 عنصر في القائمة.
- ملاحظة: التصنيف مش هيبان "جديد" فعلًا إلا بعد ما تعمل Reload بعد ما القائمة تتحدّث.
- نص جديد: cat_recent (9 لغات). الملفات: data/SeenStore.kt, PositionStore.kt, ui/home/ContentScreen.kt.

## 3 ميزات: كاش ثنائي + تنظيف القائمة + تفاصيل أغنى
- كاش ثنائي (PlaylistRepository): playlist_<id>.bin فيه القنوات + نوع كل قناة، بيتطابق بتاريخ تعديل ملف الـ M3U؛ بيتكتب بعد التحميل/أول parse وبيتمسح مع الحذف. فتح التطبيق بيقراه بدل ما يحلّل M3U.
- تنظيف القائمة: قسم جديد "Clean-up" في الإعدادات: إخفاء المكرر (نفس الرابط أو نفس الاسم+المجموعة) وإخفاء مجموعات (حوار بحث + checkbox). محفوظ في UserPrefs (hidden_groups, dedupe) وبيتطبّق في HomeScreen.visible على كل الشاشات.
- تفاصيل الفيلم: خلفية احتياطية (البوستر) لو مفيش backdrop، وصف "أعمال مشابهة" (من نفس القائمة المفتوحة) بيفتح الفيلم التاني.
- الملفات: PlaylistRepository.kt, UserPrefs.kt, HomeScreen.kt, SettingsScreen.kt, CleanupDialogs.kt (جديد), MovieDetailsScreen.kt + 8 نصوص × 9 لغات.

### Fix: MovieDetailsScreen.kt — Unresolved reference 'items' (دوال LazyListScope لازم import: lazy.items)

## زر تخطي المقدمة (المسلسلات)
- في المشغّل: لو الحلقة مسلسل (Kind.Series) وبتتشغّل (مش Cast) وفي أول 5 دقايق وبعد ثانية 3، بيظهر زر "تخطي المقدمة (+85ث)" تحت يمين. بيقفز المدة المختارة ويختفي لحد الحلقة اللي بعدها. على الريموت: Enter والأزرار مخفية = تخطي.
- الإعدادات > Playback: "تخطي المقدمة" بيبدّل بين مقفول/60/85/90/120 ثانية (UserPrefs.introSkip، الافتراضي 85، بيرجع للافتراضي مع Restore Defaults).
- نصوص جديدة × 9 لغات: player_skip_intro, set_intro(+_sub/_off/_sec).
- ملاحظة: المدة ثابتة مش مكتشفة تلقائيًا من الحلقة.

## اختبار القنوات (Check channels)
- الإعدادات > Clean-up: صف "اختبار القنوات" (بيعرض عدد الميتة) + مفتاح "إخفاء القنوات الميتة".
- الحوار: اختيار النطاق (كل القنوات المباشرة / المفضلة بس) ← تقدّم (4 اختبارات متوازية) ← نتيجة شغالة/ميتة/غير معروفة، وزر "إخفاء الميتة" بيشغّل الإخفاء.
- ChannelChecker: بيفتح البث ويقرا أول 188 بايت (مهلة اتصال 5ث وقراءة 6ث، محاولتين). ميتة = فشل اتصال/قراءة، 404/410، أو 5xx (عدا 509)، أو رد فاضي. غير معروفة = 401/403/429/509 أو رد HTML (غالبًا حد اتصالات الحساب).
- النتيجة محفوظة في UserPrefs.dead_urls ومفتاح hide_dead؛ HomeScreen.visible بيفلتر بيهم. إعادة الاختبار بتحدّث النطاق اللي اتفحص بس (وبتشمل الميتة المخفية).
- محدود بالقنوات المباشرة (Live). النصوص: 13 مفتاح × 9 لغات. ملفات: data/ChannelChecker.kt, CleanupDialogs.kt, SettingsScreen.kt, HomeScreen.kt, UserPrefs.kt.

## Fix: زر Reload "مش بيعمل حاجة" (مفيش رد فعل ظاهر)
- كان بيحدّث فعلًا بس من غير أي إشارة: أيقونة صغيرة بتلف والنتيجة/الفشل مخفيين. دلوقتي: شاشة تحميل فوق الرئيسية ("Reloading…" + دايرة) بتمنع اللمس لحد ما يخلص، وبعدها Toast: "اتحدّثت القائمة (N قناة)" أو "فشل التحديث: <السبب>".
- حماية من الضغط المتكرر (if !reloading). نصوص جديدة: reload_done, reload_failed × 9 لغات.
- لو طلع "فشل التحديث" ابعتلي السبب اللي في الرسالة.

## شاشات Live/Movies/Series على نمط المرجع (IBP-style) بألوان الثيم
- شريط علوي: Home | Live TV | Movies | Series (الحالي بلون الثيم) + خانة بحث في القائمة + اللوجو. الشريط الجانبي القديم اتشال من الشاشات دي (Radio لسه بيستخدمه).
- يسار: لوحة بخلفية داكنة: أيقونة النوع + "↩ Back" (للرئيسية) + بحث في التصنيفات + الصفوف (Continue Watching، All، Favorites، Recently Added، ثم المجموعات). على الموبايل (ارتفاع <480dp) الأيقونة الكبيرة بتتصغّر جنب Back.
- يمين: خلفية متدرّجة من ألوان الثيم، وفوقها قائمة ترتيب منسدلة (Default / A–Z / Z–A / Recently added) + زر Guide (Live) + "الاسم (العدد)" على اليمين، وتحتها الشبكة/القنوات.
- نصوص جديدة: sort_default/az/za/added × 9 لغات. ملفات: ContentScreen.kt (TopNav, SortMenu, CategoryPanel), HomeScreen.kt.
- الوضع الطولي (portrait) فضل على التصميم القديم.

## إضافة القائمة من الموبايل (QR) — تلفزيون + موبايل
- (اتشال: تقرير الأخطاء/CrashReporter بناءً على طلبك.)
- شاشة Add Playlist: تاب جديد "من الموبايل". الجهاز بيشغّل خدمة HTTP صغيرة على الشبكة المحلية (بورت 8765–8785) برمز عشوائي في الرابط، وبيعرض QR + العنوان (http://IP:PORT/TOKEN).
- الموبايل (نفس الواي فاي) يفتح الرابط من الكاميرا أو المتصفح ← صفحة فورم (Xtream/M3U، بلغة التطبيق، RTL للعربي) ← "إرسال" ← الجهاز بيستقبل ويضيف القائمة بنفس مسار الإضافة العادي (vm.addXtream/addM3u) وبيفتحها.
- الخدمة بتتقفل أول ما تسيب التاب/الشاشة. المحتوى مش بيطلع للنت: شبكة محلية بس. حد أقصى 8KB للطلب، ومحاولة بالرمز بس.
- مكتبة جديدة: com.google.zxing:core:3.5.3 (توليد QR). ملفات: data/PhoneAddServer.kt, ui/add/PhoneAddPanel.kt, AddPlaylistScreen.kt, build.gradle.kts + 11 نص × 9 لغات.
- محدودية: لازم الجهازين على نفس الشبكة، وبعض الراوترات بتعزل الأجهزة (AP isolation) فمش هتشتغل. الصفحة HTTP عادي (من غير تشفير) فبيانات القائمة بتتنقل داخل البيت بس.

## إعادة تصميم الإعدادات (Settings)
- تنظيم جديد في 4 مجموعات (فواصل في القائمة الجانبية): [المظهر، التشغيل] — [التنظيف، الدليل، القوائم] — [البروفايلات، الرقابة الأبوية، الحساب] — [الأدوات، البيانات والنسخ، حول].
- القسم "General" بقى للمظهر بس (اللغة + الثيم). "Playback" فيه التشغيل/الأداء/التشخيص في كروت. قسم جديد "Data & backup": مسح الكاش، نسخ احتياطي، استرجاع، Reset، Restore defaults. "About" فيه فحص التحديثات + معلومات التطبيق.
- شكل جديد: كروت مستديرة (SettingsCard) بصفوف فيها أيقونة داخل مربع ملوّن (لون لكل قسم)، فواصل رفيعة مزاحة، عنوان صفحة بأيقونة كبيرة، ورأس القائمة فيه رقم الـ build. الشاشات الصغيرة/الموبايل: نفس الكروت تحت شريط الأقسام. دعم الريموت (focus) زي ما كان.
- Restore defaults بقى بيرجّع كمان مدة تخطي المقدمة.
- نص جديد × 9 لغات: set_sec_data (ترجمة آلية). ملف: ui/home/SettingsScreen.kt.
- مش متجرّب: لسه محتاج البيلد الأخضر وتجربة على الجهاز.

## ساعة + تاريخ + انتهاء الاشتراك في الرئيسية
- شريط جديد تحت الهيدر (وفي الموبايل تحت البروفايل): ساعة كبيرة وتاريخ اليوم (بلغة التطبيق، بيتحدّث كل 10ث) + كبسولة "ينتهي في" بتاريخ الانتهاء والأيام المتبقية.
- لون الكبسولة: أخضر (>30 يوم) / أصفر (8–30) / أحمر (≤7 أو منتهي). "غير محدود" لو الحساب من غير تاريخ.
- بتظهر بس لو القائمة Xtream والسيرفر رد (بيتجاب من player_api في الخلفية)؛ لو M3U عادي بيظهر الساعة بس.
- مفيش نصوص جديدة (بنستخدم acc_expires/acc_days_left/acc_expired/acc_unlimited). ملفات: Launcher.kt, HomeScreen.kt. مش متجرّب لحد البيلد الأخضر.

## تعديل: مكان الانتهاء + عكس الكلمة في السبلاش
- كبسولة انتهاء الاشتراك بقت تحت على شمال الشاشة الفعلي (حتى مع العربي RTL)، والساعة والتاريخ فضلوا تحت الهيدر. في الوضع الطولي الكبسولة تحت آخر صف.
- السبلاش: كلمة "SC Player" كانت بتظهر معكوسة ("reyalP CS") مع RTL لأن الحروف في Row بتتعكس. اتثبّت اتجاه الصف LTR. ملفات: splash/SplashScreen.kt, home/Launcher.kt. مش متجرّب لحد البيلد.

## تطبيق ستايل "Shadeed" (أسود + أحمر) — على مراحل
خطة المراحل: 1) الثيم + الخلفية ✅ (دي) — 2) الرئيسية (Live كبير + Movies/Series + صف أزرار + شريط الانتهاء/الساعة فوق) — 3) شاشة Live (3 أعمدة: تصنيفات | قنوات بأرقام | معاينة + أزرار) — 4) الأفلام/المسلسلات (شبكة + Order by) — 5) الإعدادات كشبكة أزرار 4×N.
- مرحلة 1: ثيم جديد "Crimson" (خلفية سودا + موجات حمراء سفلية) بيظهر في الإعدادات > المظهر كدايرة سابعة. نص theme_crimson × 9 لغات. ملفات: theme/Theme.kt, ThemePicker.kt. مش متجرّب.
- ملحوظة: بنستلهم التخطيط والألوان بس، من غير لوجو أو اسم التطبيق الأصلي.

## مرحلة 2 (ستايل Shadeed): الرئيسية
- الشاشة العريضة: فوق: اللوجو | كبسولة زجاجية (انتهاء الاشتراك + اسم القائمة + البروفايل) | ساعة كبيرة وتاريخ. تحت: مربع Live كبير، وجنبه Movies و Series فوق، وصف 4 أزرار: Settings / Playlist / Reload / Exit.
- مع ثيم Crimson ألوان المربعات درجات حمرا/سودا؛ مع باقي الثيمات نفس الألوان القديمة.
- كبسولة الانتهاء اتنقلت من تحت الشمال للكبسولة العلوية (زي المرجع). الوضع الطولي (موبايل portrait) فضل زي ما هو.
- مفيش نصوص جديدة. ملف: home/Launcher.kt. مش متجرّب. المرحلة الجاية: شاشة Live (3 أعمدة).

## مرحلة 3 (ستايل Shadeed): شاشة Live
- 3 أعمدة على الشاشة العريضة: التصنيفات | القنوات (برقم تسلسلي قبل اللوجو) | لوحة كبيرة على اليمين: مربع 16:9 (لوجو القناة + علامة LIVE)، اسم القناة، المجموعة، البرنامج الحالي بشريط تقدّم، التالي، وأزرار: تشغيل / مفضلة / الدليل. اللوحة بتظهر دايمًا في Live (قبل كده كانت بس على عرض ≥1100dp). لمس/ضغط مربع المعاينة بيشغّل القناة.
- ملحوظة: مفيش فيديو معاينة حي جوه المربع لسه (محتاج مشغّل مدمج؛ ممكن نعمله كمرحلة 3ب). الأفلام/المسلسلات زي ما هي (مرحلة 4).
- ChannelRow: بارامتر number جديد. ملفات: ContentScreen.kt, Widgets.kt. مفيش نصوص جديدة. مش متجرّب.

## مرحلة 4 (ستايل Shadeed): الأفلام والمسلسلات
- التخطيط (شريط علوي + تصنيفات يسار + Order by + الشبكة) كان موجود من قبل؛ المرحلة دي ضبطت الشكل: مع ثيم Crimson خلفية الشبكة بقت سودا مع لمسة حمرا خفيفة بدل التدرّج الملوّن (ونفس الحاجة للوضع الصغير).
- بوستر من غير صورة: بقى مربع غامق بلوجو التطبيق (بدل الحرف الأول على تدرّج ملوّن) في كل الثيمات.
- ملفات: ContentScreen.kt, Posters.kt. مفيش نصوص جديدة. مش متجرّب. الجاية: الإعدادات كشبكة أزرار (مرحلة 5).

## مرحلة 5 (ستايل Shadeed): الإعدادات كشبكة أزرار
- على أي شاشة عرضها ≥600dp (الموبايل الأفقي والتابلت والـ TV): العنوان "Settings" في النص وتحته شبكة أزرار (عدد الأعمدة تلقائي ~4): كل زر أيقونة + عنوان + قيمة صغيرة. المفاتيح (تشغيل تلقائي، تشغيل بالخلفية، PiP، Hardware، إعادة الاتصال، الإحصائيات، منع التكرار، إخفاء الميتة) بتتبدّل بالضغط ومعاها نقطة خضرا/رمادي. الأزرار اللي بتدوّر قيم (Buffer، تخطي المقدمة) بتغيّر القيمة المكتوبة تحت العنوان.
- أزرار بتفتح نافذة: "General" (اللغة + الثيم)، "Profiles"، "Account" (بنفس الصفوف اللي عملناها). الباقي (القوائم، الدليل، الرقابة، إخفاء المجموعات، اختبار القنوات، الأدوات، الكاش، النسخ الاحتياطي، الـ Reset، التحديث، حول) بيفتح حواراته الأصلية مباشرة.
- الشاشات الأضيق من 600dp (موبايل طولي) فضلت على شريط الأقسام + الكروت.
- مفيش نصوص جديدة. ملف: SettingsScreen.kt. مش متجرّب.

## فيكس: المحتوى مش داخل في الشاشة (موبايل أفقي، ارتفاع <480dp)
- شاشة Live: لوحة المعاينة على اليمين بقت بتتظبط على الشاشة القصيرة من غير سكرول: مربع المعاينة بياخد المساحة المتبقية، واسم القناة سطر واحد، والبرنامج الحالي وشريطه، وصف واحد فيه (تشغيل | ★ | الدليل). (على الشاشات الطويلة نفس اللوحة الكبيرة القديمة.)
- شاشة المسلسل: على الشاشة القصيرة بقت عمودين — يسار: زر الرجوع + البوستر والبيانات والمواسم، يمين: الحلقات بطول الشاشة كلها (قبل كده كانت بتتحشر في شريط صغير تحت). مفيش شريط عنوان علوي في الوضع ده.
- ملفات: ContentScreen.kt, SeriesScreen.kt. مفيش نصوص جديدة. مش متجرّب.

## قائمة Live على طريقة المرجع + مشغّل معاينة صغير
- على الشاشة العريضة/الأفقية، Live بقت 3 أعمدة مسطّحة: تصنيفات (اسم + عدد، المحدد بلون ذهبي) | قنوات (رقم + لوجو + اسم، الصفوف سودا مربّعة) | يمين: مشغّل معاينة صغير فوق، تحته اسم القناة والبرنامج الحالي/التالي، وتحت أزرار: الدليل / مفضلة / ترتيب.
- المعاينة: أول لمسة على قناة (أو التحريك عليها بالريموت) = تشتغل في المشغّل الصغير بعد ~0.6 ثانية؛ لمسة تانية على نفس القناة (أو الضغط على المشغّل الصغير، أو OK) = ملء الشاشة. ضغطة طويلة = مفضلة. المشغّل بيتحرر لما تسيب الشاشة وبيتوقف لما التطبيق يروح للخلفية.
- بتشتغل أول قناة تلقائيًا أول ما تفتح Live (بصوت) — لو مش عايزها قولي أخليها صامتة أو بتبدأ بعد اختيار.
- شلت من Live العريضة: بحث التصنيفات وزر Back (فيه Home في الشريط العلوي). الأفلام/المسلسلات زي ما هي. الوضع الطولي زي ما هو.
- ملفات: جديد ui/home/MiniPlayer.kt + ContentScreen.kt (LiveBrowser). مفيش نصوص جديدة. مش متجرّب: أداء المعاينة وتشغيل أكتر من قناة ورا بعض محتاج تجربة على جهازك.

## فيكس: المشغّل الصغير + كثافة القائمة
- المشغّل الصغير كان بيطلع علامة ⚠: ناقصه نفس منطق المشغّل الكامل — لو الرابط من غير امتداد واضح (UnrecognizedInputFormat) بيجرّب كـ HLS، وبيعيد المحاولة مرتين لأي خطأ تاني قبل ما يستسلم. لو لسه ⚠ بعد كده يبقى غالبًا حد اتصالات الحساب (اشتراكك بيسمح باتصال واحد وبيتفتح تاني) أو القناة نفسها واقعة.
- قائمة Live (الشاشة القصيرة): خطوط 10–11sp وصفوف مضغوطة (لوجو 20dp، حشو 2–5dp، فواصل 1dp) والشريط العلوي نزل لـ 38dp والبحث 30dp — عشان يبان قنوات كتير زي الصورة المرجع. الشاشات الطويلة بتستخدم أحجام أكبر شوية.
- ملفات: MiniPlayer.kt, ContentScreen.kt. مش متجرّب.

## الأفلام والمسلسلات بنفس كثافة Live
- التصنيفات (يسار) بقت مسطّحة زي Live: صفوف سودا، اسم + عدد، المحدد بالذهبي، خط 11sp وعرض 160dp على الشاشة القصيرة. (اتشال بحث التصنيفات وزر Back والأيقونة؛ Home في الشريط العلوي.)
- شبكة البوسترات: أقل عرض للبوستر 72dp على الشاشة القصيرة (كان 92) ومسافات أصغر → حوالي 8 أعمدة بدل 5؛ عنوان البوستر 8sp والنجمة أصغر وزوايا أصغر. زر الترتيب وعنوان "(العدد)" أصغر.
- الشاشات الطويلة: أحجام وسط (بوستر 116dp). ملفات: ContentScreen.kt, Posters.kt. مش متجرّب.

## mpv كمشغّل اختياري جنب ExoPlayer
- إعدادات > التشغيل > "محرّك التشغيل": ExoPlayer (الافتراضي) ⇄ mpv (ضغطة تبدّل). بيتطبّق على أي تشغيل جديد (live/أفلام/مسلسلات). Restore defaults بيرجّعه ExoPlayer.
- شاشة mpv جديدة (ui/player/MpvPlayerScreen.kt): ملء الشاشة بالعرض، لمس لإظهار/إخفاء الأزرار، تشغيل/إيقاف، سابق/تالي، شريط تقدّم + وقت للأفلام، استئناف من آخر موضع، إعادة اتصال تلقائي (3 مرات)، وزر "ExoPlayer" للتبديل فورًا. ريموت: OK = إيقاف/تشغيل، يمين/يسار = ±10ث للأفلام، فوق/تحت = القناة التالية/السابقة.
- المكتبة: dev.jdtech.mpv:libmpv:0.5.1 (Maven Central). الغلاف (data/MpvEngine.kt) بيستدعيها بالـ reflection عشان لو الواجهة اختلفت أو المكتبة ماشتغلتش → toast ويكمل بـ ExoPlayer تلقائي من غير ما التطبيق يقع.
- مهم: ما قدرتش أقرأ واجهة المكتبة من هنا، فأسماء الدوال (create/init/attachSurface/command/get/setProperty…) مبنية على اللي أعرفه — محتاجة تجربة على جهازك. لو mpv ما اشتغلش هتشوف الرسالة "mpv غير متاح" ويكمل ExoPlayer.
- حجم الـ APK هيكبر (مكتبات native): قصرنا المعماريات على arm64-v8a و armeabi-v7a (مفيش x86/محاكي). وأضفت tools:overrideLibrary في المانيفست احتياطًا لو الـ minSdk بتاع المكتبة أعلى من 24.
- غير موجود لسه في mpv: تسجيلات صوت/ترجمة، نسبة العرض، تخطي المقدمة، قائمة القنوات الجانبية، PiP، Cast (دول لسه في ExoPlayer بس).
- نصوص جديدة × 9 لغات (ترجمة آلية): set_player_engine, set_player_engine_sub, engine_exo, engine_mpv, mpv_unavailable. ملفات: build.gradle.kts, AndroidManifest.xml, UserPrefs.kt, PlayerScreen.kt, SettingsScreen.kt + الجديدين. مش متجرّب.

## فيكس بيلد (mpv)
- الخطأ: نص mpv_unavailable في الإنجليزي والفرنسي فيه apostrophe متشفّر غلط (&apos;) فـ aapt رفضه. اتصلّح بـ \' (باقي اللغات ما فيهاش apostrophe).

## تحسين شاشة mpv (من لقطة الشاشة)
- mpv اشتغل على Live. المشاكل اللي ظهرت: شريط التقدّم كان بيظهر للبث المباشر (مدة 4 ثواني من نافذة HLS) → دلوقتي بيظهر بس للأفلام (مدة ≥ 60 ثانية) والتقديم ±10ث بالريموت كمان للأفلام بس.
- أزرار التحكم والشريط اتثبّتت LTR (كان RTL بيعكس ترتيب سابق/تالي واتجاه الشريط).
- ملف: ui/player/MpvPlayerScreen.kt. مش متجرّب.

## اختيارات تكبير الشاشة (Aspect / Zoom)
- ExoPlayer: قائمة خيارات الصوت/الترجمة/الأبعاد فيها دلوقتي 9 أوضاع بدل 3: Fit / Stretch / Zoom (قص الأطراف) + نسب ثابتة 16:9 و4:3 و21:9 + تكبير 125% و150% و175% (من الصورة الأصلية مع قص الزيادة). بيتحفظ ويتطبّق على كل القنوات.
- mpv: زر "⛶ الوضع" في الشريط العلوي بيلفّ على نفس الأوضاع التسعة (video-aspect-override / panscan / keepaspect / video-zoom) ويشارك نفس الإعداد المحفوظ.
- مفيش نصوص جديدة (الأرقام والنسب حروف ثابتة). ملفات: PlayerScreen.kt, PlayerOptions.kt, MpvPlayerScreen.kt. مش متجرّب.

## زرار التكبير (ExoPlayer)
- زرار "⛶ + الوضع" في صف التحكم السفلي بعد التالي؛ كل ضغطة تبدّل بين 9 أوضاع وتتحفظ، وتظهر لافتة وسط الشاشة بإسم الوضع لمدة 1.3 ثانية.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## زرار تكبير مودرن + ثيمات بخلفيات + سبلاش ثانيتين
- زرار التكبير: كبسولة زجاج متدرّجة بأيقونة أركان مرسومة وحدّ بلون الثيم؛ وصف الصف السفلي في ExoPlayer بقى LTR ثابت (زي mpv).
- 6 ثيمات جديدة بخلفيات مرسومة: Aurora(شفق) / Starry(نجوم) / Neon(شبكة نيون) / Gold(أشعة ذهبية) / Forest(تلال) / Dusk(غروب) — من الإعدادات > الثيم (الصف بقى يتحرك أفقياً).
- السبلاش بقى ثانيتين (SPLASH_MS=2000 + تسريع أنيميشن الحروف واللمعة).
- أسماء الثيمات الجديدة إنجليزي بس لحد ما تتترجم.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## زرارين ±10 ثواني دايمًا ظاهرين
- ExoPlayer: ⏪/⏩ بقوا ظاهرين في الفيديو واللايف (بيتحرّكوا لو البث بيدعم السحب/تايم شيفت، وإلا مفيش تأثير). mpv: زرارين "⏪ 10" و"10 ⏩" جنب التشغيل.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## بار التشغيل LTR + سبلاش ثانية
- شريط التقدم في ExoPlayer بقى من الشمال لليمين (الوقت الحالي شمال والمدة يمين) حتى في الواجهة العربي.
- السبلاش بقى ثانية واحدة (SPLASH_MS=1000 + أنيميشن أسرع).
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## شاشة تحديث المحتوى المتتابعة
- ملف جديد ReloadOverlay.kt: بعد تنزيل القائمة بتنوّر الأقسام واحد ورا التاني (لايف ← أفلام ← مسلسلات ← دليل البرامج): القسم الشغال دايرة بتلف ونبض، ولما يخلص بينوّر بلون الثيم مع علامة ✓ وعدده.
- التنزيل نفسه ملف واحد (M3U)، فالتتابع بعد التنزيل بيعرض عدد كل قسم (~0.5 ثانية لكل قسم) وبعدين بيحدّث دليل البرامج فعليًا (force).
- نصوص جديدة إنجليزي بس: reload_stage_download, reload_guide.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## تنظيم الإعدادات (7 أقسام بدل 11 + شبكة من 35 زرار)
- الأقسام: عام (لغة + ثيم) / التشغيل (+ الأداء والتشخيص) / المحتوى والقوائم (القوائم + دليل البرامج + التنظيف) / الملفات الشخصية (+ الرقابة الأبوية) / الحساب / البيانات والنسخ الاحتياطي / حول (+ التحديثات).
- اتشال قسم "الأدوات" (راديو/دليل/متعدد/بحث) لأنه مكرر في الشاشة الرئيسية.
- الشاشة العريضة: 7 مربعات كبيرة كل واحد بيفتح قسمه في نافذة وفيه ملخص الحالة؛ الضيقة: شريط أقسام بنفس الـ 7.
- نص جديد: set_sec_content (إنجليزي بس).
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## أنيميشن ناعم + صور أسرع
- انتقال الشاشات: انزلاق خفيف + تلاشي (بيحترم العربي/الإنجليزي)، والسبلاش→الرئيسية تلاشي بس.
- بوستر الأفلام: تكبير الفوكس بقى متحرك (160ms) بدل ما يقفز.
- محمّل صور Coil عام (MainActivity.installImageLoader): كاش ذاكرة 25% + كاش قرص 300MB، respectCacheHeaders=false (السيرفرات اللي بتبعت no-cache كانت بتعيد التحميل كل مرة)، اتصالات متوازية (16 للسيرفر الواحد)، RGB565 لتوفير الذاكرة، ظهور بتلاشي 220ms.
- لسه ماتجربش لحد ما الـ build يبقى أخضر. أول فتح لشاشة أفلام لسه بينزّل الصور، لكن المرات اللي بعدها فورية من الكاش.

## تسريع التحديث من داخل التطبيق
- التنزيل بقى بـ 4 اتصالات متوازية (HTTP Range) بدل اتصال واحد، مع إعادة محاولة لكل جزء، وبيرجع للتنزيل العادي تلقائيًا لو السيرفر مش بيدعم Range.
- الباتش (الفروقات) لسه بيتنزّل لو متاح، ولو مش متاح بينزّل الـ APK كامل.
- ملاحظة: سبب كبر الحجم هو مكتبة mpv (ملفات native لمعماريتين). لو الجهاز كله 64 بت ممكن نشيل armeabi-v7a ونصغّر الـ APK بشكل كبير.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## تشخيص الاتصال (المرحلة 1 من طلب: تسجيل/رجوع للبرنامج + تشخيص)
- ملفات جديدة: data/Diagnostics.kt (الفحص) و ui/home/DiagnosticsDialog.kt (الشاشة).
- موجودة في الإعدادات > "المحتوى والقوائم" > "تشخيص الاتصال". 6 خطوات: شبكة الجهاز، الإنترنت، DNS للسيرفر، السيرفر، الاشتراك (Xtream: حالة/انتهاء/اتصالات)، سرعة بث فعلي لقناة لايف (ميجابت/ث). بتطلّع خلاصة: المشكلة من الجهاز/النت/DNS/السيرفر/الاشتراك/سرعة الخط/الاتصالات مشغولة.
- نصوص عربي + إنجليزي. أُضيف إذن ACCESS_NETWORK_STATE.
- المرحلة 2 (لسه): الرجوع للبرنامج اللي فات (Catch-up/timeshift) ثم التسجيل — محتاجين نعرف السيرفر بيدعم tv_archive.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## الرجوع للبرنامج اللي فات (Catch-up) — المرحلة 2أ
- في مشغل ExoPlayer للقنوات اللايف (روابط Xtream بس) ظهر زرار ⏪ فوق: بيفتح قائمة برامج آخر 7 أيام (من get_simple_data_table) مجمّعة باليوم؛ البرامج اللي السيرفر مسجّلها (has_archive) بتتشغّل من أولها عبر رابط timeshift، والباقي باهت.
- لو شغّال بث قديم يظهر 🔴 للرجوع للبث المباشر، ويتصفّر لما تبدّل القناة. لو السيرفر مش بيدعم بتظهر رسالة.
- حدود: رابط timeshift ملف TS مستمر، فممكن ما يظهرش شريط تقدّم/سحب. mpv ماخدش الميزة دي لسه.
- نصوص عربي + إنجليزي. لسه ماتجربش لحد ما الـ build يبقى أخضر.
- لسه: التسجيل (حفظ برنامج فات أو تسجيل البث الحالي لملف).

## التحميل والتسجيل (الجزء التاني)
- ملفات جديدة: data/Downloads.kt (DownloadStore + Downloads)، service/DownloadService.kt (خدمة أمامية، اتنين في نفس الوقت)، ui/downloads/DownloadsScreen.kt (شاشة "التحميلات").
- فتح الشاشة: مربع "التحميلات" في الرئيسية. بتعرض تقدّم وحجم، تشغيل (من الملف المحلي)، إيقاف مؤقت/استكمال (للأفلام بـ HTTP Range)، "إيقاف وحفظ" للتسجيلات، حذف.
- زرار تحميل في: تفاصيل الفيلم (chip)، كل حلقة مسلسل (⬇)، وفي قائمة البرامج اللي فاتت (⬇ بجنب ▶ — بيحفظ البرنامج من الأرشيف).
- تسجيل البث: زرار ⏺ في مشغل ExoPlayer للقنوات Xtream اللايف؛ اختيار المدة (30 د / ساعة / ساعتين / لحد ما أوقفه). مش بيدعم HLS (m3u8) — بيطلّع رسالة.
- الملفات: Android/data/com.sc.player/files/Movies/SCPlayer (بتتمسح مع حذف التطبيق). إشعار التقدّم عبر قناة "Downloads".
- حدود: لسه من غير زرار تحميل في mpv، ولا جدولة تسجيل لوقت قادم، ولا مشاركة الملف لخارج التطبيق.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## إصلاح التحميل + تسريع بدء التشغيل
- التحميل كان بيعلّم "تم" على ملف 200KB بس: السيرفر غالبًا رجّع رد صغير/صفحة بدل الفيلم (كنت باعت User-Agent بتاع VLC بدل SCPlayer/0.1 زي المشغل). دلوقتي: نفس User-Agent المشغل، رفض الردود text/json/m3u8 للأفلام، وأي ملف أصغر من 2MB بيتعلّم "فشل" ويتمسح، وسبب الفشل (HTTP كذا / type / small) بيظهر في سطر الحالة.
- تسريع التشغيل: بافر البدء في الوضع العادي بقى 1 ثانية بدل 2.5 (والصغير 0.5)، prioritizeTime، ومستخرج TS بيقبل keyframes من غير IDR (يقلّل الشاشة السودا في بث IPTV)؛ نفس الشيء للمشغل الصغير.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## إصلاح التحميل (2): Range دايمًا + استكمال تلقائي
- التحميل دلوقتي بيطلب Range: bytes=N- من أول طلب ويكمّل تلقائيًا من آخر نقطة لو السيرفر قطع الاتصال بدري (3 محاولات من غير تقدّم قبل ما يفشل)، ويتعامل مع السيرفر اللي بيتجاهل Range. لو الخدمة نفسها ماقدرتش تشتغل بيطلع سبب الفشل في سطر الحالة ("service: ...").
- لسه ماتجربش. لو فشل، اللي بيفرق: السطر اللي تحت اسم الفيلم في شاشة التحميلات (HTTP/type/small/stalled/service).

## تحميل الأفلام HLS (m3u8)
- التشخيص من شاشة التحميلات: السيرفر بيرجّع الفيلم كـ HLS (type: application/vnd.apple.mpegurl) مش ملف مباشر، فكان بيترفض.
- اتضاف تحميل HLS كامل في DownloadService: بيختار أعلى جودة من الـ master، بينزّل القطع ويلصقها في ملف واحد (يدعم EXT-X-MAP وتشفير AES-128)، تقدّم تقديري، إيقاف مؤقت واستكمال من آخر قطعة (حقل segs جديد)، وتسجيل لايف HLS (بيجيب القطع الجديدة لحد المدة/الإيقاف). SAMPLE-AES مش مدعوم (بيظهر enc:).
- حدود: الملف الناتج TS داخل اسم .mp4 (بيشتغل بالمشغل) وممكن شريط السحب يبقى تقريبي.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.

## كبسولة الانتهاء ثابتة + أزرار تشغيل بلون محايد
- كبسولة تاريخ/أيام انتهاء السيرفر في الرئيسية: آخر قيمة بتتحفظ (ذاكرة + SharedPreferences "expiry_cache") وبتظهر من أول فريم عند الرجوع من الإعدادات؛ بتتحدّث بعدها بصمت.
- أزرار المشغل (ExoPlayer + mpv): استبدلت الإيموجي الملوّنة (اللي كانت برتقالي على كل الثيمات) بأيقونات Material بيضاء على زجاج معتم/شفاف، بلون محايد مريح للعين على كل الثيمات؛ الزرار الكبير (تشغيل/إيقاف) زجاج فاتح بحد أبيض، والفوكس أبيض مع تكبير ناعم؛ زرار التسجيل والبث المباشر بس أحمر.
- لسه ماتجربش لحد ما الـ build يبقى أخضر.
