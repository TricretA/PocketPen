package helium314.keyboard

import helium314.keyboard.handwriting.InkGeometry
import helium314.keyboard.handwriting.InkJudge
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InkJudgeTest {
    private fun geometry(strokes: Int, width: Float = 80f, height: Float = 40f, closed: Boolean = false) =
        InkGeometry(strokes, width, height, 200f, closed)

    @Test fun blankTextIsNeverText() {
        assertFalse(InkJudge.isText("  ", false, geometry(3)))
    }

    @Test fun wordsWithManyStrokesAreTextEvenIfShapeLooksConfident() {
        assertTrue(InkJudge.isText("hello", true, geometry(5)))
    }

    @Test fun threeLetterResultIsTextEvenWithOneStroke() {
        assertTrue(InkJudge.isText("and", true, geometry(1)))
    }

    @Test fun singleStrokeConfidentShapeIsRejected() {
        assertFalse(InkJudge.isText("o", true, geometry(1, 60f, 60f, closed = true)))
    }

    @Test fun smallSingleLetterIsAccepted() {
        assertTrue(InkJudge.isText("o", false, geometry(1, 30f, 30f, closed = true)))
    }

    @Test fun bigClosedLoopIsRejectedAsShape() {
        assertFalse(InkJudge.isText("o", false, geometry(1, 150f, 150f, closed = true)))
    }

    @Test fun clearGapBetweenTopTwoScoresIsConfident() {
        assertTrue(InkJudge.isConfident(listOf(0.1f, 0.9f, 1.2f)))
    }

    @Test fun closeScoresAreNotConfident() {
        assertFalse(InkJudge.isConfident(listOf(0.50f, 0.55f)))
    }

    @Test fun missingScoresAreNotConfident() {
        assertFalse(InkJudge.isConfident(listOf(null, null)))
        assertFalse(InkJudge.isConfident(listOf(0.1f)))
    }
}
