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
