package com.example.superaiagents.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止工具（作用是让自主规划智能体能够合理地中断）
 */
public class TerminateTool {

    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            "When you have finished all the tasks, call this tool to end the work.
            """)
//    在满足请求时或助手无法继续执行任务时终止互动。
//    当你完成所有任务后，调用此工具以结束工作。
    public String doTerminate() {
        return "任务结束";
    }
}