#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]); layer=Path(__file__).parent
src=root/'app/src/main/java/com/droiddeck/launcher'; activity=src/'SessionActivity.kt'; manifest=root/'app/src/main/AndroidManifest.xml'
for name in ('PcLayout.kt','PcLayoutEditorActivity.kt'):
    (src/'input'/name).write_text((layer/name).read_text(encoding='utf-8'),encoding='utf-8')
s=activity.read_text(encoding='utf-8')
needle='    private var screenKeys: ScreenKeys? = null\n'
insert='    private var screenKeys: ScreenKeys? = null\n    private var pcLayoutView: com.droiddeck.launcher.input.PcLayoutView? = null\n    private var pcLayoutButton: android.widget.TextView? = null\n'
if 'pcLayoutView:' not in s:
    if needle not in s: raise SystemExit('screenKeys field anchor missing')
    s=s.replace(needle,insert,1)
needle='''            root.addView(keys)\n        }\n        keyboard = KeyboardHost(this).also { host ->\n'''
insert='''            root.addView(keys)\n        }\n\n        // DroidDeck-PC configurable PC control layer.\n        val pcProfile = intent.getStringExtra("app_id") ?: intent.getStringExtra("program") ?: "default"\n        val pcView = com.droiddeck.launcher.input.PcLayoutView(\n            this, pcProfile, false,\n            onKey = { code, down -> if (CompositorHost.isStarted) sendGuestKey(code, down) },\n            pointer = pointerListener,\n            onDirectTouch = { event -> onTouchEvent(event) },\n        )\n        pcView.visibility = View.GONE\n        pcLayoutView = pcView\n        root.addView(pcView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))\n\n        val pcButton = android.widget.TextView(this).apply {\n            text = "PC"; textSize = 12f; gravity = android.view.Gravity.CENTER\n            setTextColor(android.graphics.Color.WHITE); setBackgroundColor(0xAA20242C.toInt())\n            setPadding(18, 10, 18, 10); isClickable = true\n            setOnClickListener { pcView.visibility = if (pcView.visibility == View.VISIBLE) View.GONE else View.VISIBLE }\n            setOnLongClickListener {\n                startActivity(android.content.Intent(this@SessionActivity, com.droiddeck.launcher.input.PcLayoutEditorActivity::class.java).putExtra("profile", pcProfile))\n                true\n            }\n        }\n        pcLayoutButton = pcButton\n        root.addView(pcButton, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {\n            gravity = android.view.Gravity.TOP or android.view.Gravity.END\n            topMargin = (12 * resources.displayMetrics.density).toInt(); marginEnd = topMargin\n        })\n\n        keyboard = KeyboardHost(this).also { host ->\n'''
if 'DroidDeck-PC configurable PC control layer' not in s:
    if needle not in s: raise SystemExit('keyboard anchor missing')
    s=s.replace(needle,insert,1)
activity.write_text(s,encoding='utf-8')
m=manifest.read_text(encoding='utf-8')
if '.input.PcLayoutEditorActivity' not in m:
    needle='        <activity android:name=".QuestXrActivity"'
    if needle not in m: raise SystemExit('manifest anchor missing')
    m=m.replace(needle,'        <activity android:name=".input.PcLayoutEditorActivity" android:exported="false" android:screenOrientation="landscape" />\n'+needle,1)
manifest.write_text(m,encoding='utf-8')
print('pc layer applied')
