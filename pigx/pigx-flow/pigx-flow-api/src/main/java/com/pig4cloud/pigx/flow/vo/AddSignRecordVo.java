package com.pig4cloud.pigx.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程进度中的动态加签事件。
 */
@Data
public class AddSignRecordVo {

	private Long id;

	private String addedByName;

	private List<String> targetUserNames;

	private String reason;

	private LocalDateTime createTime;

}
