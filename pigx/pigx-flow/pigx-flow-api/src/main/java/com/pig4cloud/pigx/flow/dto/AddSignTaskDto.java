package com.pig4cloud.pigx.flow.dto;

import lombok.Data;

import java.util.List;

/**
 * 动态加签请求。
 */
@Data
public class AddSignTaskDto {

	/** 当前审批任务 ID。 */
	private String taskId;

	/** 本批次新增的审批人。 */
	private List<Long> targetUserIdList;

	/** 加签原因。 */
	private String reason;

}
