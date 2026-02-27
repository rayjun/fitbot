package com.fitness.data

import com.fitness.model.Exercise

object ExerciseProvider {
    val exercises = listOf(
        Exercise(
            id = "benchpress",
            name = "杠铃卧推",
            gifResPath = "exercises/benchpress.gif",
            description = "仰卧在凳子上，垂直推起杠铃。",
            targetMuscle = "胸大肌",
            category = "胸部"
        ),
        Exercise(
            id = "pushup",
            name = "俯卧撑",
            gifResPath = "exercises/pushup.gif",
            description = "标准的自重胸部训练。",
            targetMuscle = "胸大肌",
            category = "胸部"
        ),
        Exercise(
            id = "incline_press",
            name = "上斜卧推",
            gifResPath = "exercises/benchpress.gif",
            description = "侧重胸肌上部。",
            targetMuscle = "胸大肌上束",
            category = "胸部"
        ),
        Exercise(
            id = "pullup_v2",
            name = "引体向上",
            gifResPath = "exercises/pushup.gif", // 临时使用 pushup 占位
            description = "背部王牌动作。",
            targetMuscle = "背阔肌",
            category = "背部"
        ),
        Exercise(
            id = "row",
            name = "划船",
            gifResPath = "exercises/benchpress.gif",
            description = "增加背部厚度。",
            targetMuscle = "中背部",
            category = "背部"
        ),
        Exercise(
            id = "squat",
            name = "深蹲",
            gifResPath = "exercises/squat.gif",
            description = "全能练腿动作。",
            targetMuscle = "股四头肌",
            category = "腿部"
        ),
        Exercise(
            id = "lunge",
            name = "箭步蹲",
            gifResPath = "exercises/squat.gif",
            description = "单腿力量训练。",
            targetMuscle = "臀腿部",
            category = "腿部"
        ),
        Exercise(
            id = "plank",
            name = "平板支撑",
            gifResPath = "exercises/squat.gif",
            description = "核心稳定性训练。",
            targetMuscle = "腹横肌",
            category = "核心"
        ),
        Exercise(
            id = "burpee",
            name = "波比跳",
            gifResPath = "exercises/pushup.gif",
            description = "高强度全身训练。",
            targetMuscle = "全身",
            category = "全身"
        )
    )
    
    val categories = listOf("全部") + exercises.map { it.category }.distinct()
}
