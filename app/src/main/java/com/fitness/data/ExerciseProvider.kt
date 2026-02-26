package com.fitness.data

import com.fitness.model.Exercise

object ExerciseProvider {
    val exercises = listOf(
        Exercise(
            id = "squat",
            name = "深蹲 (Squat)",
            gifResPath = "exercises/squat.gif",
            description = "双脚与肩同宽，背部挺直，臀部向后坐，模拟坐下的动作。主要锻炼大腿前侧和臀部。",
            targetMuscle = "股四头肌, 臀大肌"
        ),
        Exercise(
            id = "pushup",
            name = "俯卧撑 (Push-up)",
            gifResPath = "exercises/pushup.gif",
            description = "双手比肩略宽，身体保持直线，屈肘使胸部接近地面。主要锻炼胸大肌、三角肌前束和肱三头肌。",
            targetMuscle = "胸大肌, 肱三头肌"
        ),
        Exercise(
            id = "benchpress",
            name = "杠铃卧推 (Bench Press)",
            gifResPath = "exercises/benchpress.gif",
            description = "仰卧在凳子上，双手握杠铃，垂直推起。经典的胸肌力量训练。",
            targetMuscle = "胸大肌"
        ),
        Exercise(
            id = "pullup",
            name = "引体向上 (Pull-up)",
            gifResPath = "exercises/pullup.gif",
            description = "双手握杠，向上拉起身体直至下巴超过单杠。主要锻炼背部肌群。",
            targetMuscle = "背阔肌"
        )
    )
}
