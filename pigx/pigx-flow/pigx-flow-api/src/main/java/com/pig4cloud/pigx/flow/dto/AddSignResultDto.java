package com.pig4cloud.pigx.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 动态加签结果。
 */
@Data
@AllArgsConstructor
public class AddSignResultDto {

	private Long recordId;

	private List<AddSignTaskSummaryDto> tasks;

}
