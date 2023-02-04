package presentation

import Constants
import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import domain.playground.*
import kotlinx.coroutines.*
import presentation.adapters.*

fun Container.playgroundBlock(
    col: Int,
    row: Int,
    power: Int,
    animationState: PlayBlockAnimationState,
    playgroundAnimationState: AnimationState,
    targetPower: Int? = null,
    collapsingState: PlaygroundBlock.ChangingState? = null,
    movingState: PlaygroundBlock.ChangingState? = null,
    onNewBlockAnimationFinished: () -> Unit,
    onCollapseBlockAnimationFinished: () -> Unit,
    onMoveBlockAnimationFinished: () -> Unit
) =
    UIPlaygroundBlock(
        col = col,
        row = row,
        power = power,
        animationState = animationState,
        playgroundAnimationState = playgroundAnimationState,
        targetPower = targetPower,
        collapsingState = collapsingState,
        movingState = movingState,
        onNewBlockAnimationFinished = onNewBlockAnimationFinished,
        onCollapseBlockAnimationFinished = onCollapseBlockAnimationFinished,
        onMoveBlockAnimationFinished = onMoveBlockAnimationFinished,
    ).addTo(this)

class UIPlaygroundBlock(
    var col: Int,
    var row: Int,
    var power: Int,
    var animationState: PlayBlockAnimationState,
    var playgroundAnimationState: AnimationState,
    var targetPower: Int? = null,
    var collapsingState: PlaygroundBlock.ChangingState? = null,
    var movingState: PlaygroundBlock.ChangingState? = null,
    var onNewBlockAnimationFinished: () -> Unit,
    var onCollapseBlockAnimationFinished: () -> Unit,
    var onMoveBlockAnimationFinished: () -> Unit
) : Container() {
    val cellSize: Double = SizeAdapter.cellSize
    val playgroundBlock = this
    init {

        position(
            x = getXPosition(animationState),
            y = getYPosition(animationState)
        )

        container {
            block(power = power, cellSize = cellSize)
            // text(animationState.toString())
        }

        if (animationState == PlayBlockAnimationState.BOTTOM) {
            launchImmediately(Dispatchers.Default) {
                animate {
                    parallel {
                        moveTo(
                            view = playgroundBlock,
                            x = getXPosition(PlayBlockAnimationState.PLACED),
                            y = getYPosition(PlayBlockAnimationState.PLACED),
                            time = TimeSpan(Constants.Playground.ANIMATION_TIME)
                        )
                    }
                    block {
                        onNewBlockAnimationFinished()
                    }
                }
            }
        }
    }

    private fun getYPosition(animationState: PlayBlockAnimationState): Double {
        val cellSizeWithMargin = cellSize + SizeAdapter.horizontalPlaygroundColumnMarginValue
        return when (animationState) {
            PlayBlockAnimationState.BOTTOM ->
                Constants.Playground.ROW_COUNT * cellSizeWithMargin
            PlayBlockAnimationState.COLLAPSED ->
                (collapsingState?.targetRow ?: row) * cellSizeWithMargin
            PlayBlockAnimationState.MOVED -> {
                (movingState?.targetRow ?: row) * cellSizeWithMargin
            }

            else -> row * cellSizeWithMargin
        }
    }

    private fun getXPosition(animationState: PlayBlockAnimationState): Double {
        val columnSize = SizeAdapter.columnSize
        val margin = SizeAdapter.horizontalPlaygroundColumnMarginValue
        return when (animationState) {
            PlayBlockAnimationState.COLLAPSED -> (collapsingState?.targetCol ?: col) * columnSize + margin
            PlayBlockAnimationState.MOVED -> (movingState?.targetCol ?: col) * columnSize + margin
            else -> col * columnSize + margin
        }
    }

    fun collapseIfNeeded() {
        collapsingState?.let {
            // draw current block and target on top. Then current will fade out so the target will be seen
            block(power = targetPower ?: power, cellSize = cellSize)
            val blockToVanish = block(power, cellSize = cellSize)

            launchImmediately(Dispatchers.Default) {
                animate {
                    parallel {
                        moveTo(
                            view = playgroundBlock,
                            x = getXPosition(PlayBlockAnimationState.COLLAPSED),
                            y = getYPosition(PlayBlockAnimationState.COLLAPSED),
                            time = TimeSpan(Constants.Playground.ANIMATION_TIME),
                        )
                        hide(
                            view = blockToVanish,
                            time = TimeSpan(Constants.Playground.ANIMATION_TIME),
                        )
                    }
                    block {
                        if (col != it.targetCol || row != it.targetRow) {
                            playgroundBlock.removeFromParent()
                        }
                        onCollapseBlockAnimationFinished()
                    }
                }
            }
        }
    }

    fun moveIfNeeded() {
        movingState?.let {
            launchImmediately(Dispatchers.Default) {
                animate {
                    parallel {
                        moveTo(
                            view = playgroundBlock,
                            x = getXPosition(PlayBlockAnimationState.MOVED),
                            y = getYPosition(PlayBlockAnimationState.MOVED),
                            time = TimeSpan(Constants.Playground.ANIMATION_TIME),
                        )
                    }
                    block {
                        onMoveBlockAnimationFinished()
                    }
                }
            }
        }
    }
}
