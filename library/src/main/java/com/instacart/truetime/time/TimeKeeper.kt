package com.instacart.truetime.time

import android.os.SystemClock
import com.instacart.truetime.CacheProvider
import com.instacart.truetime.TimeKeeperListener
import com.instacart.truetime.sntp.SntpResult
import java.time.Instant

// TODO: move android dependency to separate package
//  so we can make Truetime a pure kotlin library

/** TimeKeeper figures out how to give you the best time given all the info currently available. */
internal class TimeKeeper(
    private val listener: TimeKeeperListener,
    private val cacheProvider: CacheProvider,
) {

  fun save(ntpResult: SntpResult) {
    listener.storingTrueTime(ntpResult)
    cacheProvider.insert(ntpResult)
  }

  fun hasTheTime(): Boolean = cacheProvider.hasInfo()

  fun nowSafely(): Instant {
    return if (hasTheTime()) {
      nowTrueOnly()
    } else {
      listener.returningDeviceTime()
      Instant.now()
    }
  }

  fun nowTrueOnly(): Instant {
    if (!hasTheTime()) throw IllegalStateException("TrueTime was not initialized successfully yet")
    return now()
  }

  /** Given the available information provide the best known time */
  private fun now(): Instant {
    val ntpResult = cacheProvider.fetch()!!
    val savedSntpTime: Long = ntpResult.trueTime()
    val timeSinceBoot: Long = ntpResult.timeSinceBoot()
    val currentTimeSinceBoot: Long = SystemClock.elapsedRealtime()
    val trueTime = Instant.ofEpochMilli(savedSntpTime + (currentTimeSinceBoot - timeSinceBoot))
    listener.returningTrueTime(trueTime)
    return trueTime
  }
}
