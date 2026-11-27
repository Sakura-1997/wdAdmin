package com.pig4cloud.pigx.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 动态加签后新建任务摘要。
 */
@Data
@AllArgsConstructor
public class AddSignTaskSummaryDto {

	private String taskId;

	private Long userId;

}
