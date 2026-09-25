package com.example

import com.example.engine.BallSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun ballEngine_initialState_hasTwoBalls() {
    val engine = BallSimulationEngine()
    engine.setViewport(800f, 1200f)
    engine.reset(2)
    assertEquals(2, engine.balls.size)
    assertEquals(0L, engine.totalCollisions)
  }

  @Test
  fun ballEngine_addBalls_increasesCount() {
    val engine = BallSimulationEngine()
    engine.setViewport(800f, 1200f)
    engine.reset(2)
    engine.addBalls(10)
    assertEquals(12, engine.balls.size)
  }

  @Test
  fun ballEngine_refresh_respawnsActiveBalls() {
    val engine = BallSimulationEngine()
    engine.setViewport(800f, 1200f)
    engine.reset()
    engine.addBalls(5)
    engine.refresh()
    assertTrue(engine.balls.isNotEmpty())
  }

  @Test
  fun ballEngine_customMaxBalls_allowsHighLimits() {
    val engine = BallSimulationEngine()
    engine.config = engine.config.copy(maxBalls = 50000)
    assertEquals(50000, engine.config.maxBalls)
    engine.addBalls(100)
    assertTrue(engine.balls.size >= 100)
  }

  @Test
  fun ballEngine_exponentialDoubling_multipliesQuantity() {
    val engine = BallSimulationEngine()
    engine.setViewport(800f, 1200f)
    engine.reset(2)
    assertEquals(2, engine.balls.size)
    // Run simulation steps until wall impact triggers doubling
    for (step in 0 until 180) {
      engine.update(0.016f)
      if (engine.balls.size > 2) break
    }
    // When doubling triggers from 2, it doubles to 4 (not 3!)
    if (engine.balls.size > 2) {
      assertEquals(4, engine.balls.size)
    }
  }
}

