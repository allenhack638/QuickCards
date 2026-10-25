package com.quickcards.app.core.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupProtectionTest {

    private val strict = BackupSettings.Protection(
        appLockEnabled = true,
        lockOnLeave = true,
        appLockAfterSec = 30,
        copyWindowSec = 30,
        clipClearSec = 30,
        maskNumbers = true,
        notifEnabled = false
    )

    private val loose = BackupSettings.Protection(
        appLockEnabled = false,
        lockOnLeave = false,
        appLockAfterSec = 0,
        copyWindowSec = 120,
        clipClearSec = 0,
        maskNumbers = false,
        notifEnabled = true
    )

    @Test
    fun aLooseBackupCannotWeakenAStrictPhone() {
        assertEquals(strict, strict.stricterWith(loose))
    }

    @Test
    fun aStrictBackupTightensALoosePhone() {
        assertEquals(strict, loose.stricterWith(strict))
    }

    @Test
    fun neverIsTheWeakestTimeout() {
        assertEquals(60, BackupSettings.Protection.shorterTimeout(0, 60))
        assertEquals(60, BackupSettings.Protection.shorterTimeout(60, 0))
        assertEquals(10, BackupSettings.Protection.shorterTimeout(10, 600))
        assertEquals(0, BackupSettings.Protection.shorterTimeout(0, 0))
    }
}
