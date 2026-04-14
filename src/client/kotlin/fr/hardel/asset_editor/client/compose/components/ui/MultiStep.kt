package fr.hardel.asset_editor.client.compose.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors

@Stable
class MultiStepState(val totalSteps: Int) {

    var currentStep by mutableIntStateOf(0)
        internal set

    val isFirst: Boolean get() = currentStep == 0
    val isLast: Boolean get() = currentStep == totalSteps - 1

    fun next() {
        if (!isLast) currentStep++
    }

    fun previous() {
        if (!isFirst) currentStep--
    }

    fun goTo(step: Int) {
        currentStep = step.coerceIn(0, totalSteps - 1)
    }
}

@Composable
fun rememberMultiStepState(totalSteps: Int): MultiStepState =
    remember(totalSteps) { MultiStepState(totalSteps) }

@Composable
fun MultiStep(
    state: MultiStepState,
    modifier: Modifier = Modifier,
    content: @Composable (currentStep: Int) -> Unit
) {
    Box(modifier = modifier) {
        content(state.currentStep)
    }
}

@Composable
fun MultiStepControl(
    state: MultiStepState,
    onDone: () -> Unit,
    previousLabel: String,
    nextLabel: String,
    doneLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        StepIndicator(currentStep = state.currentStep, totalSteps = state.totalSteps)
        if (!state.isFirst) {
            Button(
                text = previousLabel,
                variant = ButtonVariant.GHOST_BORDER,
                size = ButtonSize.SM,
                onClick = state::previous
            )
        }
        Button(
            text = if (state.isLast) doneLabel else nextLabel,
            variant = ButtonVariant.DEFAULT,
            size = ButtonSize.SM,
            onClick = if (state.isLast) onDone else state::next
        )
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { index ->
            val active = index == currentStep
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 20.dp else 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) StudioColors.Zinc200 else StudioColors.Zinc700)
            )
        }
    }
}
