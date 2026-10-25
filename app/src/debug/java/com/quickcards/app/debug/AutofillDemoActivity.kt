package com.quickcards.app.debug

import android.app.Activity
import android.os.Bundle
import com.quickcards.app.R

/** Debug-only checkout form so the autofill service can be exercised without a second app. */
class AutofillDemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.autofill_demo)
    }
}
