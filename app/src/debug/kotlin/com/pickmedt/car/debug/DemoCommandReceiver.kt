package com.pickmedt.car.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pickmedt.car.app.PickmeApplication

/**
 * 터미널 시연 명령 수신기 (debug 빌드 전용). scripts/demo.sh가 adb broadcast로 보낸다.
 * 매니페스트에서 DUMP 권한을 요구해 adb(shell)만 보낼 수 있다. 결과 문구는 broadcast 결과 데이터로 돌려준다.
 *
 *   adb shell am broadcast -n com.pickmedt.car/.debug.DemoCommandReceiver -a com.pickmedt.car.DEMO --es cmd accept
 */
class DemoCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PickmeApplication
        val cmd = intent.getStringExtra(EXTRA_CMD).orEmpty()
        val arg = intent.getStringExtra(EXTRA_ARG)
        // adb 출력은 data="…" 한 줄로 읽으므로 줄바꿈을 \n 문자로 바꿔 보낸다 (demo.sh가 되돌린다)
        resultData = app.container.demoCommands.run(cmd, arg).replace("\n", "\\n")
    }

    private companion object {
        const val EXTRA_CMD = "cmd"
        const val EXTRA_ARG = "arg"
    }
}
